package be.kommaboard.kareer.user.controller.dto.request

import be.kommaboard.kareer.user.controller.dto.validation.NotCommon
import be.kommaboard.kareer.user.controller.dto.validation.NotSimple
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

data class CreateUserDTO(

    @get:NotBlank(message = "{CreateUserDTO.email.NotBlank}")
    @get:Email(message = "{CreateUserDTO.email.Email}")
    @get:Size(message = "{CreateUserDTO.email.Size}", max = 100)
    val email: String?,

    @get:NotBlank(message = "{CreateUserDTO.password.NotBlank}")
    @get:NotCommon(message = "{CreateUserDTO.password.NotCommon}")
    @get:NotSimple(message = "{CreateUserDTO.password.NotSimple}")
    @get:Size(message = "{CreateUserDTO.password.size}", min = 8)
    val password: String?,

    @get:NotBlank(message = "{CreateUserDTO.name.NotBlank}")
    @get:Size(message = "{CreateUserDTO.name.Size}", max = 100)
    val name: String?,

    @get:Size(message = "{CreateUserDTO.alias.Size}", max = 100)
    val alias: String?,
)
