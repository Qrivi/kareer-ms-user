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
import be.kommaboard.kareer.common.trimOrNullIfBlank
import be.kommaboard.kareer.common.util.HttpHeadersBuilder
import be.kommaboard.kareer.user.UserConfig
import be.kommaboard.kareer.user.lib.dto.request.CreateUserDTO
import be.kommaboard.kareer.user.lib.dto.request.VerifyCredentialsDTO
import be.kommaboard.kareer.user.lib.dto.response.UserDTO
import be.kommaboard.kareer.user.proxy.OrganizationProxy
import be.kommaboard.kareer.user.repository.entity.User
import be.kommaboard.kareer.user.service.UserService
import be.kommaboard.kareer.user.service.exception.OrganizationDoesNotExistException
import feign.FeignException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mapping.PropertyReferenceException
import org.springframework.data.util.ClassTypeInformation
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

@RestController
@RequestMapping("/users/v1")
class UserController(
    private val userConfig: UserConfig,
    private val userService: UserService,
    private val organizationProxy: OrganizationProxy,
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

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
            .body(ListDTO(users.map(User::toDTO)))
    }

    @Operation(
        summary = "Get users mathing filter",
        description = "Returns the users matching the filters passed as request parameters. `page` and `size` function as offset and limit. Requires `ADMIN` or `MANAGER` roles. If a `MANAGER` makes the request, the `organizationUuid` value is overwritten by the manager's organization's UUID.",
        responses = [ApiResponse(responseCode = "200")],
    )
    @GetMapping
    fun getUsers(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @RequestParam emailPart: String?,
        @RequestParam organizationUuid: String?,
        @RequestParam role: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam sort: String?,
        request: HttpServletRequest,
    ): ResponseEntity<ListDTO<UserDTO>> {
        logger.info("Handling GET /users/v1 [getUsers] for {}", consumerId)
        authorizationCheck(consumerId, userConfig.consumerId, consumerRole, Role.ADMIN, Role.MANAGER)

        if (page < 0 || size < 1)
            throw InvalidPageOrSizeException()

        if (!sort.isNullOrBlank() && sort.contains("password")) // Disable sorting on password
            throw PropertyReferenceException("password", ClassTypeInformation.from(User::class.java), listOf())

        val usersPage = userService.getPagedUsers(
            pageRequest = if (sort.isNullOrBlank()) PageRequest.of(page, size, Sort.unsorted()) else PageRequest.of(page, size, Sort.by(*sort.split(',').toTypedArray())),
            emailPart = emailPart.trimOrNullIfBlank(),
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
            .body(ListDTO(usersPage.content.map(User::toDTO), usersPage))
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
            .body(user.toDTO())
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
                uuid = dto.organizationUuid.toString(),
            )
        } catch (e: FeignException) {
            throw OrganizationDoesNotExistException()
        }

        val user = userService.createUser(
            organizationUuid = organization.uuid,
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
            .body(user.toDTO())
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
            .body(user.toDTO())
    }
}
