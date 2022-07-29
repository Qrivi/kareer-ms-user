package be.kommaboard.kareer.user.controller

import be.kommaboard.kareer.common.response.dto.ErrorDTO
import be.kommaboard.kareer.common.response.dto.ResponseDTO
import be.kommaboard.kareer.common.security.InternalHttpHeaders
import be.kommaboard.kareer.common.security.Role
import be.kommaboard.kareer.user.controller.dto.request.CreateUserDTO
import be.kommaboard.kareer.user.controller.dto.response.UserDTO
import be.kommaboard.kareer.user.repository.entity.User
import be.kommaboard.kareer.user.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

@RestController
@RequestMapping("/users/v1")
class UserController(
    private val userService: UserService,
) {

    @GetMapping("/{uuid}")
    fun getUser(
        @RequestHeader(value = InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(value = InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @PathVariable uuid: String,
        request: HttpServletRequest,
    ): ResponseEntity<ResponseDTO> {
        if(!Role.SYSTEM.matches(consumerRole))
            return ErrorDTO.Unauthorized(ErrorDTO.Unauthorized.Reason.INVALID_CREDENTIALS, request).buildEntity()

        val user = userService.getUserByUuid(UUID.fromString(uuid))

        return UserDTO(user).buildEntity()
    }

    @PostMapping
    fun createUser(
        @RequestHeader(value = InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(value = InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @Valid @RequestBody dto: CreateUserDTO,
        validation: BindingResult,
    ): ResponseEntity<ResponseDTO> {
        if (validation.hasErrors())
            return ErrorDTO.BadRequest(validation).buildEntity()

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
}
