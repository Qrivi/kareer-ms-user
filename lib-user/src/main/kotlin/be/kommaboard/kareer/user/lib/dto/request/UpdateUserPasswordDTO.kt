package be.kommaboard.kareer.user.lib.dto.request

import be.kommaboard.kareer.user.lib.constraint.NotCommonPassword
import be.kommaboard.kareer.user.lib.constraint.NotSimplePassword
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class UpdateUserPasswordDTO(

    @get:Pattern(message = "{UpdateUserPasswordDTO.currentPassword.NotBlank}", regexp = "^(?!\\s*$).+")
    val currentPassword: String?,

    @get:Pattern(message = "{UpdateUserPasswordDTO.newPassword.NotBlank}", regexp = "^(?!\\s*$).+")
    @get:Size(message = "{UpdateUserPasswordDTO.newPassword.size}", min = 8)
    @get:NotCommonPassword(message = "{UpdateUserPasswordDTO.newPassword.NotCommonPassword}")
    @get:NotSimplePassword(message = "{UpdateUserPasswordDTO.newPassword.NotSimplePassword}")
    val newPassword: String?,
)
