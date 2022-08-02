package be.kommaboard.kareer.user.controller

import be.kommaboard.kareer.common.dto.ResponseDTO
import be.kommaboard.kareer.common.dto.buildEntity
import be.kommaboard.kareer.common.dto.response.ErrorDTO
import be.kommaboard.kareer.common.exception.InvalidCredentialsException
import be.kommaboard.kareer.common.exception.OutOfPagesException
import be.kommaboard.kareer.common.exception.RequestValidationException
import be.kommaboard.kareer.common.security.InternalHttpHeaders
import be.kommaboard.kareer.common.security.Role
import be.kommaboard.kareer.common.toRole
import be.kommaboard.kareer.common.toUuid
import be.kommaboard.kareer.user.UserConfig
import be.kommaboard.kareer.user.controller.dto.request.CreateUserDTO
import be.kommaboard.kareer.user.controller.dto.request.VerifyCredentialsDTO
import be.kommaboard.kareer.user.controller.dto.response.UserDTO
import be.kommaboard.kareer.user.repository.entity.User
import be.kommaboard.kareer.user.service.UserService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mapping.PropertyReferenceException
import org.springframework.data.util.ClassTypeInformation
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
@RequestMapping("/v1")
class UserController(
    private val userConfig: UserConfig,
    private val userService: UserService,
) {

    @GetMapping("/all")
    fun getAllUsers(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        request: HttpServletRequest,
    ): ResponseEntity<*> {
        if (!Role.SYSTEM.matches(consumerRole) || userConfig.consumerId != consumerId)
            throw InvalidCredentialsException()

        val users = userService.getAllUsers()

        return users.map(::UserDTO).buildEntity()
    }

    @GetMapping
    fun getUsers(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @RequestParam email: String?,
        @RequestParam company: String?,
        @RequestParam role: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam sort: String?,
        request: HttpServletRequest,
    ): ResponseEntity<List<ResponseDTO>> {
        if (!Role.SYSTEM.matches(consumerRole) && !Role.ADMIN.matches(consumerRole))
            throw InvalidCredentialsException()

        if (sort.equals("password")) // Disable sorting on password
            throw PropertyReferenceException("password", ClassTypeInformation.from(User::class.java), listOf())

        // TODO allow MANAGER too but overwrite companyuuid with user.companyuuid

        val results = userService.getPagedUsers(
            pageRequest = if (sort.isNullOrBlank()) PageRequest.of(page, size) else PageRequest.of(page, size, Sort.by(*sort.split(',').toTypedArray())),
            email = if (email.isNullOrBlank()) null else email.trim(),
            companyUuid = if (company.isNullOrBlank()) null else company.toUuid(),
            role = if (role.isNullOrBlank()) null else role.toRole(),
        )

        if (results.totalPages <= page)
            throw OutOfPagesException()

        return results.content.map(::UserDTO).buildEntity(request, results)
    }

    @GetMapping("/{uuid}")
    fun getUser(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @PathVariable uuid: String,
        request: HttpServletRequest,
    ): ResponseEntity<ResponseDTO> {
        if (!Role.SYSTEM.matches(consumerRole) || userConfig.consumerId != consumerId)
            throw InvalidCredentialsException()

        val user = userService.getUserByUuid(uuid.toUuid())

        return UserDTO(user).buildEntity()
    }

    @PostMapping
    fun createUser(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @Valid @RequestBody dto: CreateUserDTO,
        validation: BindingResult,
        request: HttpServletRequest,
    ): ResponseEntity<ResponseDTO> {
        if (!Role.SYSTEM.matches(consumerRole) || userConfig.consumerId != consumerId)
            throw InvalidCredentialsException()

        if (validation.hasErrors())
            throw RequestValidationException(validation)

        val user = userService.createUser(
            email = dto.email!!,
            password = dto.password!!,
            name = dto.name!!,
            alias = dto.alias,
            // TODO companyUuid: retrieve using authorization, copy from manager
            role = Role.USER,
        )

        return UserDTO(user).buildEntity()
    }

    @PostMapping("/verify")
    fun verifyUserCredentials(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @Valid @RequestBody dto: VerifyCredentialsDTO,
        validation: BindingResult,
        request: HttpServletRequest,
    ): ResponseEntity<ResponseDTO> {
        if (!Role.SYSTEM.matches(consumerRole) || userConfig.consumerId != consumerId)
            throw InvalidCredentialsException()

        if (validation.hasErrors())
            throw RequestValidationException(validation)

        val user = userService.getUserByEmailAndPassword(
            email = dto.email!!,
            password = dto.password!!
        )

        return UserDTO(user).buildEntity()
    }
}
