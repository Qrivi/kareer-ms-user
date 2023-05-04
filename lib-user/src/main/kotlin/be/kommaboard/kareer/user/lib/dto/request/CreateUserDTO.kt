package be.kommaboard.kareer.user.lib.dto.request

import be.kommaboard.kareer.user.lib.constraint.NotBlacklistedSlug
import be.kommaboard.kareer.user.lib.constraint.NotCommonPassword
import be.kommaboard.kareer.user.lib.constraint.NotSimplePassword
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CreateUserDTO(

    @get:NotBlank(message = "{CreateUserDTO.invitationUuid.NotBlank}")
    @get:Pattern(message = "{CreateUserDTO.invitationUuid.Pattern}", regexp = "^\\w{8}-\\w{4}-\\w{4}-\\w{4}-\\w{12}$")
    val invitationUuid: String?,

    @get:NotBlank(message = "{CreateUserDTO.email.NotBlank}")
    @get:Size(message = "{CreateUserDTO.email.Size}", max = 100)
    @get:Email(message = "{CreateUserDTO.email.Email}")
    val email: String?,

    @get:NotBlank(message = "{CreateUserDTO.password.NotBlank}")
    @get:Size(message = "{CreateUserDTO.password.Size}", min = 8)
    @get:NotCommonPassword(message = "{CreateUserDTO.password.NotCommonPassword}")
    @get:NotSimplePassword(message = "{CreateUserDTO.password.NotSimplePassword}")
    val password: String?,

    @get:NotBlank(message = "{CreateUserDTO.lastName.NotBlank}")
    @get:Size(message = "{CreateUserDTO.lastName.Size}", max = 100)
    val lastName: String?,

    @get:NotBlank(message = "{CreateUserDTO.firstName.NotBlank}")
    @get:Size(message = "{CreateUserDTO.firstName.Size}", max = 100)
    val firstName: String?,

    @get:Size(message = "{CreateUserDTO.nickname.Size}", max = 100)
    val nickname: String?,

    @get:Size(message = "{CreateUserDTO.slug.Size}", max = 50)
    @get:Pattern(message = "{CreateUserDTO.slug.Pattern}", regexp = "^[\\w.]+\$")
    @get:NotBlacklistedSlug(message = "{CreateUserDTO.slug.NotBlacklistedSlug}")
    val slug: String?,

    @get:Valid
    val details: CreateUserDetailsDTO?,
)
