package be.kommaboard.kareer.user.lib.dto.request

import jakarta.validation.constraints.NotBlank

data class VerifyPasswordDTO(

    @get:NotBlank(message = "{VerifyPasswordDTO.password.NotBlank}")
    val password: String?,
)
