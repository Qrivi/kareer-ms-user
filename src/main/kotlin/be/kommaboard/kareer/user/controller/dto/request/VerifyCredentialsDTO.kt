package be.kommaboard.kareer.user.controller.dto.request

import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank

data class VerifyCredentialsDTO(

    @JsonProperty("email")
    @get:NotBlank(message = "{VerifyCredentialsDTO.email.NotBlank}")
    @get:Email(message = "{VerifyCredentialsDTO.email.Email}")
    val email: String?,

    @JsonProperty("password")
    @get:NotBlank(message = "{VerifyCredentialsDTO.password.NotBlank}")
    val password: String?,
)
