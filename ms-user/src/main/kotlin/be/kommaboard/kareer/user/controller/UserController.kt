package be.kommaboard.kareer.user.controller

import be.kommaboard.kareer.authorization.InternalHttpHeaders
import be.kommaboard.kareer.authorization.Role
import be.kommaboard.kareer.authorization.authorizationCheck
import be.kommaboard.kareer.authorization.exception.InvalidCredentialsException
import be.kommaboard.kareer.authorization.toRole
import be.kommaboard.kareer.authorization.toUuid
import be.kommaboard.kareer.common.exception.OutOfPagesException
import be.kommaboard.kareer.common.exception.RequestValidationException
import be.kommaboard.kareer.common.orNullIfBlank
import be.kommaboard.kareer.common.util.HttpHeadersBuilder
import be.kommaboard.kareer.user.UserConfig
import be.kommaboard.kareer.user.lib.dto.request.CreateUserDTO
import be.kommaboard.kareer.user.lib.dto.request.VerifyCredentialsDTO
import be.kommaboard.kareer.user.lib.dto.response.UserDTO
import be.kommaboard.kareer.user.repository.entity.User
import be.kommaboard.kareer.user.service.UserService
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
) {

    @GetMapping("/all")
    fun getAllUsers(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        request: HttpServletRequest,
    ): ResponseEntity<List<UserDTO>> {
        authorizationCheck(consumerId, userConfig.consumerId, consumerRole)

        val users = userService.getAllUsers()

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(users.map(User::toDTO))
    }

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
    ): ResponseEntity<List<UserDTO>> {
        authorizationCheck(consumerId, userConfig.consumerId, consumerRole, Role.ADMIN, Role.MANAGER)

        if (!sort.isNullOrBlank() && sort.contains("password")) // Disable sorting on password
            throw PropertyReferenceException("password", ClassTypeInformation.from(User::class.java), listOf())

        val results = userService.getPagedUsers(
            pageRequest = if (sort.isNullOrBlank()) PageRequest.of(page, size, Sort.unsorted()) else PageRequest.of(page, size, Sort.by(*sort.split(',').toTypedArray())),
            emailPart = emailPart.orNullIfBlank()?.trim(),
            organizationUuid = if (Role.MANAGER.matches(consumerRole)) userService.getUserByUuid(consumerId.toUuid()).organizationUuid else organizationUuid.orNullIfBlank()?.toUuid(),
            role = role.orNullIfBlank()?.toRole(),
        )

        if (results.totalPages <= page)
            throw OutOfPagesException()

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(
                HttpHeadersBuilder()
                    .contentLanguage()
                    .link(request, results)
                    .build()
            )
            .body(results.content.map(User::toDTO))
    }

    @GetMapping("/{uuid}")
    fun getUser(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @PathVariable uuid: String,
        request: HttpServletRequest,
    ): ResponseEntity<UserDTO> {
        authorizationCheck(consumerId, userConfig.consumerId, consumerRole, Role.ADMIN, Role.MANAGER, Role.USER)

        if (Role.USER.matches(consumerRole) && uuid != consumerId)
            throw InvalidCredentialsException()

        val user = userService.getUserByUuid(uuid.toUuid())

        if (Role.MANAGER.matches(consumerRole)) {
            val manager = userService.getUserByUuid(consumerId.toUuid())

            if (manager.organizationUuid == null || user.organizationUuid == null || manager.organizationUuid != user.organizationUuid)
                throw InvalidCredentialsException()
        }

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(user.toDTO())
    }

    @PostMapping
    fun createUser(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @Valid @RequestBody dto: CreateUserDTO,
        validation: BindingResult,
        request: HttpServletRequest,
    ): ResponseEntity<UserDTO> {
        authorizationCheck(consumerId, userConfig.consumerId, consumerRole)

        if (validation.hasErrors())
            throw RequestValidationException(validation)

        val user = userService.createUser(
            email = dto.email!!,
            password = dto.password!!,
            fullName = dto.fullName!!,
            shortName = dto.shortName,
            // TODO organizationUuid: retrieve using authorization, copy from manager?
            role = Role.USER,
        )

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(user.toDTO())
    }

    @PostMapping("/verify")
    fun verifyUserCredentials(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @Valid @RequestBody dto: VerifyCredentialsDTO,
        validation: BindingResult,
        request: HttpServletRequest,
    ): ResponseEntity<UserDTO> {
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
