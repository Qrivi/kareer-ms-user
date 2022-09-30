package be.kommaboard.kareer.user.controller

import be.kommaboard.kareer.authorization.InternalHttpHeaders
import be.kommaboard.kareer.authorization.Role
import be.kommaboard.kareer.authorization.authorizationCheck
import be.kommaboard.kareer.authorization.exception.InvalidCredentialsException
import be.kommaboard.kareer.authorization.toRole
import be.kommaboard.kareer.authorization.toUuid
import be.kommaboard.kareer.common.dto.ListDTO
import be.kommaboard.kareer.common.exception.InvalidPageOrSizeException
import be.kommaboard.kareer.common.exception.RequestValidationException
import be.kommaboard.kareer.common.toSort
import be.kommaboard.kareer.common.trimOrNullIfBlank
import be.kommaboard.kareer.common.util.HttpHeadersBuilder
import be.kommaboard.kareer.storage.lib.dto.request.CreateFileReferenceDTO
import be.kommaboard.kareer.storage.lib.dto.response.UrlDTO
import be.kommaboard.kareer.user.UserConfig
import be.kommaboard.kareer.user.lib.dto.request.CreateUserDTO
import be.kommaboard.kareer.user.lib.dto.request.UpdateUserDTO
import be.kommaboard.kareer.user.lib.dto.request.VerifyCredentialsDTO
import be.kommaboard.kareer.user.lib.dto.response.UserDTO
import be.kommaboard.kareer.user.proxy.OrganizationProxy
import be.kommaboard.kareer.user.proxy.StorageProxy
import be.kommaboard.kareer.user.repository.entity.User
import be.kommaboard.kareer.user.service.UserService
import be.kommaboard.kareer.user.service.exception.OrganizationDoesNotExistException
import feign.FeignException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers.organization
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.mapping.PropertyReferenceException
import org.springframework.data.util.ClassTypeInformation
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.Base64Utils
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

@RestController
@RequestMapping("/users/v1")
class UserController(
    private val userConfig: UserConfig,
    private val userService: UserService,
    private val organizationProxy: OrganizationProxy,
    private val storageProxy: StorageProxy,
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    // region User

    @Operation(hidden = true)
    @GetMapping("/all")
    fun getAllUsers(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        request: HttpServletRequest,
    ): ResponseEntity<ListDTO<UserDTO>> {
        logger.info("Handling GET /users/v1/all [getAllUsers] for {}", consumerId)
        authorizationCheck(consumerId, userConfig.consumerId, consumerRole)

        val users = userService.getAllUsers()

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(ListDTO(users.map { it.toRichDTO() }))
    }

    @Operation(
        summary = "Get users matching filter",
        description = "Returns the users matching the filters passed as request parameters. `page` and `size` function as offset and limit. Requires `ADMIN` or `MANAGER` roles. If a `MANAGER` makes the request, the `organizationUuid` value is overwritten by the manager's organization's UUID.",
        responses = [ApiResponse(responseCode = "200")],
    )
    @GetMapping
    fun getUsers(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @RequestParam keywords: String?,
        @RequestParam organizationUuid: String?,
        @RequestParam role: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "creationDate") sort: String,
        request: HttpServletRequest,
    ): ResponseEntity<ListDTO<UserDTO>> {
        logger.info("Handling GET /users/v1 [getUsers] for {}", consumerId)
        authorizationCheck(consumerId, userConfig.consumerId, consumerRole, Role.ADMIN, Role.MANAGER)

        if (page < 0 || size < 1)
            throw InvalidPageOrSizeException()

        if (sort.contains("password")) // Disable sorting on password
            throw PropertyReferenceException("password", ClassTypeInformation.from(User::class.java), listOf())

        val usersPage = userService.getPagedUsers(
            pageRequest = PageRequest.of(page, size, sort.toSort()),
            keywords = keywords.trimOrNullIfBlank(),
            organizationUuid = if (Role.MANAGER.matches(consumerRole)) userService.getUserByUuid(consumerId.toUuid()).organizationUuid else organizationUuid.trimOrNullIfBlank()?.toUuid(),
            role = role.trimOrNullIfBlank()?.toRole(),
        )

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(
                HttpHeadersBuilder()
                    .contentLanguage()
                    .link(request, usersPage)
                    .build()
            )
            .body(ListDTO(usersPage.content.map { it.toRichDTO() }, usersPage))
    }

    @Operation(
        summary = "Get a user by their UUID",
        description = "Gets the user whose UUID matches the path variable. The `MANAGER` role can only request other users that are part of the same organization, whereas the `USER` role can only request their own details.",
        responses = [ApiResponse(responseCode = "200")],
    )
    @GetMapping("/{uuid}")
    fun getUser(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @PathVariable uuid: String,
    ): ResponseEntity<UserDTO> {
        logger.info("Handling GET /users/v1/{uuid} [getUser] for {}", consumerId)
        authorizationCheck(consumerId, userConfig.consumerId, consumerRole, Role.ADMIN, Role.MANAGER, Role.USER)

        if (Role.USER.matches(consumerRole) && uuid != consumerId)
            throw InvalidCredentialsException()

        val user = userService.getUserByUuid(uuid.toUuid())

        if (Role.MANAGER.matches(consumerRole)) {
            val manager = userService.getUserByUuid(consumerId.toUuid())

            if (manager.organizationUuid != user.organizationUuid)
                throw InvalidCredentialsException()
        }

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(user.toRichDTO())
    }

    @Operation(
        summary = "Create a new user",
        description = "Creates a new user. If using the `ADMIN` role, this bypasses the invitation system. Endpoint is also used internally when registering via auth-ms.",
        responses = [ApiResponse(responseCode = "201")],
    )
    @PostMapping
    fun createUser(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @Valid @RequestBody dto: CreateUserDTO,
        validation: BindingResult,
    ): ResponseEntity<UserDTO> {
        logger.info("Handling POST /users/v1 [createUser] for {}", consumerId)
        authorizationCheck(consumerId, userConfig.consumerId, consumerRole, Role.ADMIN)

        if (validation.hasErrors())
            throw RequestValidationException(validation)

        val organization = try {
            organizationProxy.getOrganization(
                consumerRole = Role.SYSTEM.name,
                consumerId = userConfig.consumerId!!,
                uuid = dto.organizationUuid!!,
            )
        } catch (e: FeignException) {
            throw OrganizationDoesNotExistException()
        }

        val user = userService.createUser(
            organizationUuid = organization.uuid,
            organizationName = organization.name,
            email = dto.email!!,
            password = dto.password!!,
            lastName = dto.lastName!!,
            firstName = dto.firstName!!,
            nickname = dto.nickname,
            role = dto.role?.toRole() ?: Role.USER,
        )

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(user.toDTO(null, null))
    }

    @Operation(
        summary = "Update a user",
        description = "Updates an user's details. The `ADMIN` role can edit all users, `MANAGER` role can edit users belonging to their organization, and `USER` role can only edit their own details.",
        responses = [ApiResponse(responseCode = "200")],
    )
    @PatchMapping("/{uuid}")
    fun updateUser(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @PathVariable uuid: String,
        @Valid @RequestBody dto: UpdateUserDTO,
        validation: BindingResult,
        request: HttpServletRequest,
    ): ResponseEntity<UserDTO> {
        logger.info("Handling PATCH /users/v1/{uuid} [updateUser] for {}", consumerId)
        authorizationCheck(consumerId, userConfig.consumerId, consumerRole, Role.ADMIN, Role.MANAGER, Role.USER)

        // Get the user details, for later
        val user = userService.getUserByUuid(uuid.toUuid())

        // If we are trying to update a user other than ourselves, we need to check who's performing the request first
        if (uuid != consumerId) {
            // Normal user's can only edit their own details
            if (Role.USER.matches(consumerRole) || !Role.USER.matches(dto.role ?: Role.USER.name))
                throw InvalidCredentialsException()
            // And managers can only edit their own or their employees' details
            if (Role.MANAGER.matches(consumerRole)) {
                val manager = userService.getUserByUuid(consumerId.toUuid())
                if (manager.organizationUuid != user.organizationUuid)
                    throw InvalidCredentialsException()
            }
        }

        // If we are trying to update the role to anything but USER...
        if (dto.role != null && !Role.USER.matches(dto.role!!)) {
            // ... then the requester must be a MANAGER or higher
            if (!Role.MANAGER.matches(consumerRole) && !Role.ADMIN.matches(consumerRole) && !Role.SYSTEM.matches(consumerRole))
                throw InvalidCredentialsException()
        }

        // Make sure the request body is valid
        if (validation.hasErrors())
            throw RequestValidationException(validation)

        // Get the organization's name
        val organizationName = try {
            organizationProxy.getOrganization(
                consumerRole = Role.SYSTEM.name,
                consumerId = userConfig.consumerId!!,
                uuid = user.organizationUuid.toString(),
            ).name
        } catch (e: FeignException) {
            null
        }

        val updatedUser = userService.updateUser(
            uuid = uuid.toUuid(),
            organizationName = organizationName,
            email = dto.email,
            password = dto.password,
            lastName = dto.lastName,
            firstName = dto.firstName,
            nickname = dto.nickname,
            role = dto.role?.toRole()
        )

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(updatedUser.toRichDTO())
    }

    @Operation(hidden = true)
    @PostMapping("/verify")
    fun verifyUserCredentials(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @Valid @RequestBody dto: VerifyCredentialsDTO,
        validation: BindingResult,
    ): ResponseEntity<UserDTO> {
        logger.info("Handling POST /users/v1/verify [verifyUserCredentials] for {}", consumerId)
        authorizationCheck(consumerId, userConfig.consumerId, consumerRole)

        if (validation.hasErrors())
            throw RequestValidationException(validation)

        val user = userService.getUserByEmailAndPassword(
            email = dto.email!!,
            password = dto.password!!
        )

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(user.toDTO(null, null))
    }

    // endregion
    // region User avatar and banner

    @Operation(
        summary = "Update a user's avatar",
        description = "Updates a user's avatar. The `ADMIN` role can edit all users, `MANAGER` role can edit users belonging to their organization, and `USER` role can only edit their own details.",
        responses = [ApiResponse(responseCode = "200")],
    )
    @PutMapping("/{uuid}/avatar", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateUserAvatar(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @RequestPart file: MultipartFile,
        @PathVariable uuid: String,
    ): ResponseEntity<UrlDTO> {
        logger.info("Handling PUT /users/v1/{uuid}/avatar [updateUserAvatar] for {}", consumerId)
        authorizationCheck(consumerId, userConfig.consumerId, consumerRole, Role.ADMIN, Role.MANAGER, Role.USER)

        // Get the user details, for later
        val user = userService.getUserByUuid(uuid.toUuid())

        // If we are trying to update a user other than ourselves, we need to check who's performing the request first
        if (uuid != consumerId) {
            // Normal user's can only edit their own details
            if (Role.USER.matches(consumerRole))
                throw InvalidCredentialsException()
            // And managers can only edit their own or their employees' details
            if (Role.MANAGER.matches(consumerRole)) {
                val manager = userService.getUserByUuid(consumerId.toUuid())
                if (manager.organizationUuid != user.organizationUuid)
                    throw InvalidCredentialsException()
            }
        }

        // TODO Add file type/size checks

        // Upload new avatar and get its reference
        val fileReference = storageProxy.createFileReference(
            consumerRole = Role.SYSTEM.name,
            consumerId = userConfig.consumerId!!,
            dto = CreateFileReferenceDTO(
                content = Base64Utils.encodeToString(file.bytes),
                contentType = file.contentType,
            )
        )

        // Remove the old avatar from storage
        userService.getUserByUuid(uuid.toUuid()).avatarReference?.let {
            storageProxy.deleteFileReference(
                consumerRole = Role.SYSTEM.name,
                consumerId = userConfig.consumerId!!,
                id = it,
            )
        }

        // Get the organization's name
        val organizationName = try {
            organizationProxy.getOrganization(
                consumerRole = Role.SYSTEM.name,
                consumerId = userConfig.consumerId!!,
                uuid = user.organizationUuid.toString(),
            ).name
        } catch (e: FeignException) {
            null
        }

        // Update the user details
        userService.updateUser(
            uuid = uuid.toUuid(),
            organizationName = organizationName,
            avatarReference = fileReference.id,
        )

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(fileReference.toUrlDTO())
    }

    @Operation(
        summary = "Update an user's banner",
        description = "Updates an user's banner. The `ADMIN` role can edit all users, `MANAGER` role can edit users belonging to their organization, and `USER` role can only edit their own details.",
        responses = [ApiResponse(responseCode = "200")],
    )
    @PutMapping("/{uuid}/banner", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateUserBanner(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @RequestPart file: MultipartFile,
        @PathVariable uuid: String,
    ): ResponseEntity<UrlDTO> {
        logger.info("Handling PUT /users/v1/{uuid}/banner [updateUserBanner] for {}", consumerId)
        authorizationCheck(consumerId, userConfig.consumerId, consumerRole, Role.ADMIN, Role.MANAGER, Role.USER)

        // Get the user details, for later
        val user = userService.getUserByUuid(uuid.toUuid())

        // If we are trying to update a user other than ourselves, we need to check who's performing the request first
        if (uuid != consumerId) {
            // Normal user's can only edit their own details
            if (Role.USER.matches(consumerRole))
                throw InvalidCredentialsException()
            // And managers can only edit their own or their employees' details
            if (Role.MANAGER.matches(consumerRole)) {
                val manager = userService.getUserByUuid(consumerId.toUuid())
                if (manager.organizationUuid != user.organizationUuid)
                    throw InvalidCredentialsException()
            }
        }

        // TODO Add file type/size checks

        // Upload new banner and get its reference
        val fileReference = storageProxy.createFileReference(
            consumerRole = Role.SYSTEM.name,
            consumerId = userConfig.consumerId!!,
            dto = CreateFileReferenceDTO(
                content = Base64Utils.encodeToString(file.bytes),
                contentType = file.contentType,
            )
        )

        // Remove the old banner from storage
        userService.getUserByUuid(uuid.toUuid()).bannerReference?.let {
            storageProxy.deleteFileReference(
                consumerRole = Role.SYSTEM.name,
                consumerId = userConfig.consumerId!!,
                id = it,
            )
        }

        // Get the organization's name
        val organizationName = try {
            organizationProxy.getOrganization(
                consumerRole = Role.SYSTEM.name,
                consumerId = userConfig.consumerId!!,
                uuid = user.organizationUuid.toString(),
            ).name
        } catch (e: FeignException) {
            null
        }

        // Update the user details
        userService.updateUser(
            uuid = uuid.toUuid(),
            organizationName = organizationName,
            bannerReference = fileReference.id,
        )

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(fileReference.toUrlDTO())
    }

    // endregion

    private fun User.toRichDTO(): UserDTO {
        val avatarUrl = this.avatarReference?.let { id ->
            storageProxy.getFileReference(
                consumerRole = Role.SYSTEM.name,
                consumerId = userConfig.consumerId!!,
                id = id,
            ).url
        }
        val bannerUrl = this.bannerReference?.let { id ->
            storageProxy.getFileReference(
                consumerRole = Role.SYSTEM.name,
                consumerId = userConfig.consumerId!!,
                id = id,
            ).url
        }
        return this.toDTO(avatarUrl, bannerUrl)
    }
}
