package be.kommaboard.kareer.user.controller

import be.kommaboard.kareer.common.dto.ErrorDTO
import be.kommaboard.kareer.common.dto.ResponseDTO
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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.Valid

@RestController
@RequestMapping("users/v1")
class UserController(
    private val userService: UserService,
) {

    @GetMapping("/info")
    fun sayHello(): ResponseEntity<String> {
        return ResponseEntity.ok("Hello from user microservice")
    }

    @GetMapping("/{uuid}")
    fun tempGetUser(
        @PathVariable uuid: String
    ): ResponseEntity<ResponseDTO> {
        val user = userService.getUserByUuid(UUID.fromString(uuid))

        return UserDTO(user).toResponseEntity()
    }

    @PostMapping
    fun tempCreateUser(
        @Valid @RequestBody dto: CreateUserDTO,
        validation: BindingResult,
    ): ResponseEntity<ResponseDTO> {
        if (validation.hasErrors())
            return ErrorDTO(validation).toResponseEntity(HttpStatus.BAD_REQUEST)

        val user = userService.createUser(
            email = dto.email!!,
            password = dto.password!!,
            name = dto.name!!,
            alias = dto.alias,
            // TODO companyUuid: retrieve using authorization, copy from manager
            role = User.Role.USER,
        )

        return UserDTO(user).toResponseEntity(HttpStatus.CREATED)
    }
}
