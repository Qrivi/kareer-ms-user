package be.kommaboard.kareer.user.lib.dto.request

import be.kommaboard.kareer.user.lib.constraint.NotCommon
import be.kommaboard.kareer.user.lib.constraint.NotSimple
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
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
    @get:NotCommon(message = "{CreateUserDTO.password.NotCommon}")
    @get:NotSimple(message = "{CreateUserDTO.password.NotSimple}")
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
    val slug: String?,

    @get:Valid
    @get:NotNull(message = "{CreateUserDTO.details.NotNull}")
    val details: CreateUserDetailsDTO?,
)
