package be.kommaboard.kareer.user.lib.dto.request

import be.kommaboard.kareer.user.lib.constraint.NotCommonPassword
import be.kommaboard.kareer.user.lib.constraint.NotSimplePassword
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class UpdateUserDTO(

    @get:Pattern(message = "{UpdateUserDTO.role.Pattern}", regexp = "^(user|manager)$", flags = [Pattern.Flag.CASE_INSENSITIVE])
    val role: String?,

    // TODO Add status?

    @get:Pattern(message = "{UpdateUserDTO.email.NotBlank}", regexp = "^(?!\\s*$).+")
    @get:Size(message = "{UpdateUserDTO.email.Size}", max = 100)
    @get:Email(message = "{UpdateUserDTO.email.Email}")
    val email: String?,

    @get:Pattern(message = "{UpdateUserDTO.lastName.NotBlank}", regexp = "^(?!\\s*$).+")
    @get:Size(message = "{UpdateUserDTO.lastName.Size}", max = 100)
    val lastName: String?,

    @get:Pattern(message = "{UpdateUserDTO.firstName.NotBlank}", regexp = "^(?!\\s*$).+")
    @get:Size(message = "{UpdateUserDTO.firstName.Size}", max = 100)
    val firstName: String?,

    @get:Size(message = "{UpdateUserDTO.nickname.Size}", max = 100)
    val nickname: String?,

    @get:Size(message = "{UpdateUserDTO.slug.Size}", max = 50)
    @get:Pattern(message = "{UpdateUserDTO.slug.Pattern}", regexp = "^\\s*\$|^[\\w.]+\$")
    val slug: String?,
)
