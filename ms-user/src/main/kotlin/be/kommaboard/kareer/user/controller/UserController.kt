package be.kommaboard.kareer.user.controller

import be.kommaboard.kareer.authorization.InternalHttpHeaders
import be.kommaboard.kareer.authorization.Role
import be.kommaboard.kareer.authorization.authorizationCheck
import be.kommaboard.kareer.authorization.exception.InvalidCredentialsException
import be.kommaboard.kareer.authorization.isRole
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
import be.kommaboard.kareer.user.KareerConfig
import be.kommaboard.kareer.user.lib.dto.request.CreateUserDTO
import be.kommaboard.kareer.user.lib.dto.request.EditUserDetailsSkillsDTO
import be.kommaboard.kareer.user.lib.dto.request.UpdateUserDTO
import be.kommaboard.kareer.user.lib.dto.request.UpdateUserDetailsDTO
import be.kommaboard.kareer.user.lib.dto.request.VerifyCredentialsDTO
import be.kommaboard.kareer.user.lib.dto.request.VerifyPasswordDTO
import be.kommaboard.kareer.user.lib.dto.response.UserDTO
import be.kommaboard.kareer.user.proxy.OrganizationProxy
import be.kommaboard.kareer.user.proxy.StorageProxy
import be.kommaboard.kareer.user.repository.entity.Invitation
import be.kommaboard.kareer.user.repository.entity.User
import be.kommaboard.kareer.user.service.UserService
import be.kommaboard.kareer.user.service.exception.InvalidInviteException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.mapping.PropertyReferenceException
import org.springframework.data.util.TypeInformation
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.DeleteMapping
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
import java.util.Base64

@RestController
@RequestMapping("/users/v1")
class UserController(
    private val kareerConfig: KareerConfig,
    private val userService: UserService,
    private val organizationProxy: OrganizationProxy,
    private val storageProxy: StorageProxy,
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    // region User and UserDetails

    @Operation(hidden = true)
    @GetMapping("/all")
    fun getAllUsers(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        request: HttpServletRequest,
    ): ResponseEntity<ListDTO<UserDTO>> {
        logger.info("Handling GET /users/v1/all [getAllUsers] for {}", consumerId)
        authorizationCheck(consumerId, kareerConfig.consumerId, consumerRole)

        val users = userService.getAllUsers()

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(ListDTO(users.map { it.toDTO() }))
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
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "creationDate") sort: String,
        @RequestParam(defaultValue = "false") skipStorage: Boolean,
        request: HttpServletRequest,
    ): ResponseEntity<ListDTO<UserDTO>> {
        logger.info("Handling GET /users/v1 [getUsers] for {}", consumerId)
        authorizationCheck(consumerId, kareerConfig.consumerId, consumerRole, Role.ADMIN, Role.MANAGER, Role.USER)

        // TODO Add filter on status

        if (page < 1 || size < 1) {
            throw InvalidPageOrSizeException()
        }

        if (sort.contains("password")) {
            // Disable sorting on password
            throw PropertyReferenceException("password", TypeInformation.of(User::class.java), listOf())
        }

        val usersPage = userService.getPagedUsers(
            pageRequest = PageRequest.of(page - 1, size, sort.toSort()),
            keywords = keywords.trimOrNullIfBlank(),
            organizationUuid = if (consumerRole.isRole(Role.ADMIN)) organizationUuid.trimOrNullIfBlank()?.toUuid() else userService.getUserByUuid(consumerId.toUuid()).details!!.organizationUuid,
            role = role.trimOrNullIfBlank()?.toRole(),
        )

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(
                HttpHeadersBuilder()
                    .contentLanguage()
                    .link(request, usersPage)
                    .build(),
            )
            .body(ListDTO(usersPage.content.map { if (skipStorage) it.toDTO() else it.toRichDTO() }, usersPage))
    }

    @Operation(
        summary = "Get a user by their UUID or slug",
        description = "Gets the user whose UUID or slug matches the path variable. The `MANAGER` role can only request other users that are part of the same organization, whereas the `USER` role can only request their own details.",
        responses = [ApiResponse(responseCode = "200")],
    )
    @GetMapping("/{uuidOrSlug}")
    fun getUser(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @PathVariable uuidOrSlug: String,
        @RequestParam(defaultValue = "false") skipStorage: Boolean,
    ): ResponseEntity<UserDTO> {
        logger.info("Handling GET /users/v1/{uuid} [getUser] for {}", consumerId)
        authorizationCheck(consumerId, kareerConfig.consumerId, consumerRole, Role.ADMIN, Role.MANAGER, Role.USER)

        val user = if (uuidOrSlug.contains("-")) userService.getUserByUuid(uuidOrSlug.toUuid()) else userService.getUserBySlug(uuidOrSlug)

        // If the consumer is a regular user or manager, they can only retrieve user data of users belonging to their organization
        if (consumerRole.isRole(Role.MANAGER, Role.USER) && userService.getUserByUuid(consumerId.toUuid()).details!!.organizationUuid != user.details!!.organizationUuid) {
            throw InvalidCredentialsException()
        }

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(if (skipStorage) user.toDTO() else user.toRichDTO())
    }

    fun createAdmin(): ResponseEntity<UserDTO> {
        // TODO implement 👽
        throw NotImplementedError()
    }

    @Operation(
        summary = "Create a new user",
        description = "Creates a new user. Endpoint is used internally when registering via auth-ms.",
        responses = [ApiResponse(responseCode = "201")],
    )
    @PostMapping
    fun createUser(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @Valid @RequestBody
        dto: CreateUserDTO,
        validation: BindingResult,
    ): ResponseEntity<UserDTO> {
        logger.info("Handling POST /users/v1 [createUser] for {}", consumerId)
        authorizationCheck(consumerId, kareerConfig.consumerId, consumerRole)

        if (validation.hasErrors()) {
            throw RequestValidationException(validation)
        }

        // Get the invite details if we were passed an invitation UUID
        val invitation = userService.getInviteByUuid(dto.invitationUuid!!.toUuid())

        // Only PENDING invites can be used to create a new user
        if (invitation.status != Invitation.Status.PENDING) {
            throw InvalidInviteException()
        }

        // Assuming this won't throw a FeignException because the inviter should always belong to an existing organization
        val organization = organizationProxy.getOrganization(
            consumerRole = Role.SYSTEM.name,
            consumerId = kareerConfig.consumerId!!,
            uuid = invitation.inviter.details!!.organizationUuid.toString(),
        )

        // Create the user
        val user = userService.createUser(
            dto = dto,
            invitation = invitation,
            organization = organization,
        )

        // Prevent an invitation from being used multiple times
        userService.updateInviteStatus(invitation, Invitation.Status.ACCEPTED)

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(user.toDTO())
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
        @RequestParam(defaultValue = "false") skipStorage: Boolean,
        @Valid @RequestBody
        dto: UpdateUserDTO,
        validation: BindingResult,
    ): ResponseEntity<UserDTO> {
        logger.info("Handling PATCH /users/v1/{uuid} [updateUser] for {}", consumerId)
        authorizationCheck(consumerId, kareerConfig.consumerId, consumerRole, Role.ADMIN, Role.MANAGER, Role.USER)

        // Get the user details, for later
        val user = getUserIfPermitted(uuid, consumerId, consumerRole)

        // If we are trying to update the role to anything but USER...
        if (dto.role != null && !dto.role.isRole(Role.USER)) {
            // ... then the requester must be a MANAGER or higher
            if (consumerRole.isRole(Role.USER)) {
                throw InvalidCredentialsException()
            }
        }

        // Make sure the request body is valid
        if (validation.hasErrors()) {
            throw RequestValidationException(validation)
        }

        val updatedUser = userService.updateUser(
            user = user,
            dto = dto,
        )

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(if (skipStorage) updatedUser.toDTO() else updatedUser.toRichDTO())
    }

    @Operation(
        summary = "Update a user's details",
        description = "Updates an user's details. The `ADMIN` role can edit all users, `MANAGER` role can edit users belonging to their organization, and `USER` role can only edit their own details.",
        responses = [ApiResponse(responseCode = "200")],
    )
    @PatchMapping("/{uuid}/details")
    fun updateUserDetails(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @PathVariable uuid: String,
        @RequestParam(defaultValue = "false") skipStorage: Boolean,
        @Valid @RequestBody
        dto: UpdateUserDetailsDTO,
        validation: BindingResult,
    ): ResponseEntity<UserDTO> {
        logger.info("Handling PATCH /users/v1/{uuid}/details [updateUserDetails] for {}", consumerId)
        authorizationCheck(consumerId, kareerConfig.consumerId, consumerRole, Role.ADMIN, Role.MANAGER, Role.USER)

        // Get the user details, for later
        val user = getUserIfPermitted(uuid, consumerId, consumerRole)

        // Make sure the request body is valid
        if (validation.hasErrors()) {
            throw RequestValidationException(validation)
        }

        val updatedUser = userService.updateUserDetails(
            user = user,
            dto = dto,
        )

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(if (skipStorage) updatedUser.toDTO() else updatedUser.toRichDTO())
    }

    @Operation(
        summary = "Verify a user's password",
        description = "Returns the user's details if the password is correct. This endpoint can be used in a verification flow, eg. before allowing a user to change their password.",
        responses = [ApiResponse(responseCode = "200")],
    )
    @PostMapping("/{uuid}/verify")
    fun verifyUserPassword(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @PathVariable uuid: String,
        @Valid @RequestBody
        dto: VerifyPasswordDTO,
        validation: BindingResult,
    ): ResponseEntity<UserDTO> {
        logger.info("Handling POST /users/v1/{uuid}/verify [verifyUserPassword] for {}", consumerId)
        authorizationCheck(consumerId, kareerConfig.consumerId, consumerRole, Role.ADMIN, Role.MANAGER, Role.USER)

        if (uuid != consumerId) {
            throw InvalidCredentialsException()
        }

        if (validation.hasErrors()) {
            throw RequestValidationException(validation)
        }

        val user = userService.getUserByUuidAndPassword(
            uuid = consumerId.toUuid(),
            password = dto.password!!,
        )

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(user.toDTO())
    }

    @Operation(hidden = true)
    @PostMapping("/verify")
    fun verifyUserCredentials(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @Valid @RequestBody
        dto: VerifyCredentialsDTO,
        validation: BindingResult,
    ): ResponseEntity<UserDTO> {
        logger.info("Handling POST /users/v1/verify [verifyUserCredentials] for {}", consumerId)
        authorizationCheck(consumerId, kareerConfig.consumerId, consumerRole)

        if (validation.hasErrors()) {
            throw RequestValidationException(validation)
        }

        val user = userService.getUserByEmailAndPassword(
            email = dto.email!!,
            password = dto.password!!,
        )

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(user.toDTO())
    }

    // endregion
    // region UserDetails skills

    @Operation(
        summary = "Append to user's skills",
        description = "Appends entries to a user's skills. The `ADMIN` role can edit all users, `MANAGER` role can edit users belonging to their organization, and `USER` role can only edit their own details.",
        responses = [ApiResponse(responseCode = "201")],
    )
    @PostMapping("/{uuid}/details/skills")
    fun addUserDetailsSkills(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @PathVariable uuid: String,
        @RequestParam(defaultValue = "false") skipStorage: Boolean,
        @Valid @RequestBody
        dto: EditUserDetailsSkillsDTO,
        validation: BindingResult,
    ): ResponseEntity<UserDTO> {
        logger.info("Handling POST /users/v1/{uuid}/details/skills [addUserDetailsSkills] for {}", consumerId)
        authorizationCheck(consumerId, kareerConfig.consumerId, consumerRole, Role.ADMIN, Role.MANAGER, Role.USER)

        val user = getUserIfPermitted(uuid, consumerId, consumerRole)

        if (validation.hasErrors()) {
            throw RequestValidationException(validation)
        }

        val updatedUser = userService.appendUserDetailsSkills(
            user = user,
            dto = dto,
        )

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(if (skipStorage) updatedUser.toDTO() else updatedUser.toRichDTO())
    }

    @Operation(
        summary = "Remove from user's skills",
        description = "Removes entries to a user's skills. The `ADMIN` role can edit all users, `MANAGER` role can edit users belonging to their organization, and `USER` role can only edit their own details.",
        responses = [ApiResponse(responseCode = "200")],
    )
    @DeleteMapping("/{uuid}/details/skills")
    fun removeUserDetailsSkills(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @PathVariable uuid: String,
        @RequestParam(defaultValue = "false") skipStorage: Boolean,
        @Valid @RequestBody
        dto: EditUserDetailsSkillsDTO,
        validation: BindingResult,
    ): ResponseEntity<UserDTO> {
        logger.info("Handling DELETE /users/v1/{uuid}/details/skills [removeUserDetailsSkills] for {}", consumerId)
        authorizationCheck(consumerId, kareerConfig.consumerId, consumerRole, Role.ADMIN, Role.MANAGER, Role.USER)

        val user = getUserIfPermitted(uuid, consumerId, consumerRole)

        if (validation.hasErrors()) {
            throw RequestValidationException(validation)
        }

        val updatedUser = userService.removeUserDetailsSkills(
            user = user,
            dto = dto,
        )

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(if (skipStorage) updatedUser.toDTO() else updatedUser.toRichDTO())
    }

    // endregion
    // region User avatar and banner

    @Operation(
        summary = "Replace a user's avatar",
        description = "Replaces a user's avatar. The `ADMIN` role can edit all users, `MANAGER` role can edit users belonging to their organization, and `USER` role can only edit their own details.",
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
        authorizationCheck(consumerId, kareerConfig.consumerId, consumerRole, Role.ADMIN, Role.MANAGER, Role.USER)

        val user = getUserIfPermitted(uuid, consumerId, consumerRole)

        // TODO Add file type/size checks

        // Upload new avatar and get its reference
        val fileReference = storageProxy.createFileReference(
            consumerRole = Role.SYSTEM.name,
            consumerId = kareerConfig.consumerId!!,
            dto = CreateFileReferenceDTO(
                content = Base64.getEncoder().encodeToString(file.bytes),
                contentType = file.contentType,
            ),
        )

        // Remove the old avatar from storage
        userService.getUserByUuid(uuid.toUuid()).avatarReference?.let {
            storageProxy.deleteFileReference(
                consumerRole = Role.SYSTEM.name,
                consumerId = kareerConfig.consumerId!!,
                id = it,
            )
        }

        // Update the user details
        userService.updateUserAvatar(
            user = user,
            reference = fileReference.id,
        )

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(fileReference.toUrlDTO())
    }

    @Operation(
        summary = "Delete a user's avatar",
        description = "Deletes a user's avatar. The `ADMIN` role can edit all users, `MANAGER` role can edit users belonging to their organization, and `USER` role can only edit their own details.",
        responses = [ApiResponse(responseCode = "200")],
    )
    @DeleteMapping("/{uuid}/avatar")
    fun deleteUserAvatar(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @PathVariable uuid: String,
    ): ResponseEntity<Unit> {
        logger.info("Handling DELETE /users/v1/{uuid}/avatar [deleteUserAvatar] for {}", consumerId)
        authorizationCheck(consumerId, kareerConfig.consumerId, consumerRole, Role.ADMIN, Role.MANAGER, Role.USER)

        val user = getUserIfPermitted(uuid, consumerId, consumerRole)

        val response = storageProxy.deleteFileReference(
            consumerRole = Role.SYSTEM.name,
            consumerId = kareerConfig.consumerId!!,
            id = userService.getUserByUuid(uuid.toUuid()).avatarReference ?: "<0>", // I am lazy: I will just return the 404 or the error from ms-storage
        )
        userService.updateUserAvatar(
            user = user,
            reference = null,
        )
        return response
    }

    @Operation(
        summary = "Replace a user's banner",
        description = "Replaces an user's banner. The `ADMIN` role can edit all users, `MANAGER` role can edit users belonging to their organization, and `USER` role can only edit their own details.",
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
        authorizationCheck(consumerId, kareerConfig.consumerId, consumerRole, Role.ADMIN, Role.MANAGER, Role.USER)

        val user = getUserIfPermitted(uuid, consumerId, consumerRole)

        // TODO Add file type/size checks

        // Upload new banner and get its reference
        val fileReference = storageProxy.createFileReference(
            consumerRole = Role.SYSTEM.name,
            consumerId = kareerConfig.consumerId!!,
            dto = CreateFileReferenceDTO(
                content = Base64.getEncoder().encodeToString(file.bytes),
                contentType = file.contentType,
            ),
        )

        // Remove the old banner from storage
        userService.getUserByUuid(uuid.toUuid()).bannerReference?.let {
            storageProxy.deleteFileReference(
                consumerRole = Role.SYSTEM.name,
                consumerId = kareerConfig.consumerId!!,
                id = it,
            )
        }

        // Update the user details
        userService.updateUserBanner(
            user = user,
            reference = fileReference.id,
        )

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(fileReference.toUrlDTO())
    }

    @Operation(
        summary = "Delete a user's banner",
        description = "Deletes a user's banner. The `ADMIN` role can edit all users, `MANAGER` role can edit users belonging to their organization, and `USER` role can only edit their own details.",
        responses = [ApiResponse(responseCode = "200")],
    )
    @DeleteMapping("/{uuid}/banner")
    fun deleteUserBanner(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @PathVariable uuid: String,
    ): ResponseEntity<Unit> {
        logger.info("Handling DELETE /users/v1/{uuid}/banner [deleteUserBanner] for {}", consumerId)
        authorizationCheck(consumerId, kareerConfig.consumerId, consumerRole, Role.ADMIN, Role.MANAGER, Role.USER)

        val user = getUserIfPermitted(uuid, consumerId, consumerRole)

        val response = storageProxy.deleteFileReference(
            consumerRole = Role.SYSTEM.name,
            consumerId = kareerConfig.consumerId!!,
            id = userService.getUserByUuid(uuid.toUuid()).bannerReference ?: "<0>", // I am lazy: I will just return the 404 or the error from ms-storage
        )
        userService.updateUserBanner(
            user = user,
            reference = null,
        )
        return response
    }

    // endregion
    // region User preferences

    @Operation(
        summary = "Replace a user's preferences",
        description = "Replaces a user's preferences. There are no constraints, but the idea is to store key-value pairs clients can then use to read and persist user preferences.",
        responses = [ApiResponse(responseCode = "200")],
    )
    @PutMapping("/{uuid}/preferences")
    fun updateUserPreferences(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @RequestBody body: String?,
        @PathVariable uuid: String,
    ): ResponseEntity<String> {
        logger.info("Handling PUT /users/v1/{uuid}/preferences [updateUserPreferences] for {}", consumerId)
        authorizationCheck(consumerId, kareerConfig.consumerId, consumerRole, Role.ADMIN, Role.MANAGER, Role.USER)

        if (uuid != consumerId) {
            throw InvalidCredentialsException()
        }

        userService.updateUserPreferences(
            user = userService.getUserByUuid(uuid.toUuid()),
            preferences = body,
        )

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(body)
    }

    // endregion

    private fun getUserIfPermitted(userUuid: String, consumerId: String, consumerRole: String): User {
        val user = userService.getUserByUuid(userUuid.toUuid())

        // If we are trying to update a user other than ourselves, we need to check who's performing the request first
        if (userUuid != consumerId) {
            // Normal user's can only edit their own details
            if (consumerRole.isRole(Role.USER)) {
                throw InvalidCredentialsException()
            }
            // And managers can only edit their own or their employees' details
            if (consumerRole.isRole(Role.MANAGER)) {
                val manager = userService.getUserByUuid(consumerId.toUuid())
                if (manager.details!!.organizationUuid != user.details?.organizationUuid) {
                    throw InvalidCredentialsException()
                }
            }
        }
        return user
    }

    private fun User.toRichDTO(): UserDTO {
        val avatarUrl = this.avatarReference?.let { id ->
            storageProxy.getFileReference(
                consumerRole = Role.SYSTEM.name,
                consumerId = kareerConfig.consumerId!!,
                id = id,
            ).url
        }
        val bannerUrl = this.bannerReference?.let { id ->
            storageProxy.getFileReference(
                consumerRole = Role.SYSTEM.name,
                consumerId = kareerConfig.consumerId!!,
                id = id,
            ).url
        }
        return this.toDTO(avatarUrl, bannerUrl)
    }
}
