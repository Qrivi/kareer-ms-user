package be.kommaboard.kareer.user.controller.dto.request

import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank

data class VerifyCredentialsDTO(

    @get:NotBlank(message = "{VerifyCredentialsDTO.email.NotBlank}")
    @get:Email(message = "{VerifyCredentialsDTO.email.Email}")
    val email: String?,

    @get:NotBlank(message = "{VerifyCredentialsDTO.password.NotBlank}")
    val password: String?,
)
