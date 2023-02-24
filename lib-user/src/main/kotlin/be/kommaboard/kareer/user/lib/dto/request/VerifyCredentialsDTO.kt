package be.kommaboard.kareer.user.lib.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class VerifyCredentialsDTO(

    @get:NotBlank(message = "{VerifyCredentialsDTO.email.NotBlank}")
    @get:Email(message = "{VerifyCredentialsDTO.email.Email}")
    val email: String?,

    @get:NotBlank(message = "{VerifyCredentialsDTO.password.NotBlank}")
    val password: String?,
)
