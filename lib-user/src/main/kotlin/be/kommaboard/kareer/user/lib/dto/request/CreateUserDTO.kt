package be.kommaboard.kareer.user.lib.dto.request

import be.kommaboard.kareer.user.lib.constraint.NotCommon
import be.kommaboard.kareer.user.lib.constraint.NotSimple
import java.time.ZonedDateTime
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Past
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

data class CreateUserDTO(

    val inviteUuid: String?,

    val organizationUuid: String?,

    @get:Pattern(message = "{CreateUserDTO.role.Pattern}", regexp = "^(user|manager)$", flags = [Pattern.Flag.CASE_INSENSITIVE])
    val role: String?,

    @get:Size(message = "{CreateUserDTO.slug.Size}", max = 50)
    @get:Pattern(message = "{CreateUserDTO.slug.Pattern}", regexp = "^[\\w.]+\$")
    val slug: String?,

    @get:NotBlank(message = "{CreateUserDTO.email.NotBlank}")
    @get:Size(message = "{CreateUserDTO.email.Size}", max = 100)
    @get:Email(message = "{CreateUserDTO.email.Email}")
    val email: String?,

    @get:Size(message = "{CreateUserDTO.phone.Size}", max = 20)
    @get:Pattern(message = "{CreateUserDTO.phone.Pattern}", regexp = "^(00|\\+)\\d+\$")
    val phone: String?,

    @get:NotBlank(message = "{CreateUserDTO.password.NotBlank}")
    @get:Size(message = "{CreateUserDTO.password.size}", min = 8)
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

    @get:NotBlank(message = "{CreateUserDTO.title.NotBlank}")
    @get:Size(message = "{CreateUserDTO.title.Size}", max = 100)
    val title: String?,

    @get:Past(message = "{CreateUserDTO.birthday.Past}")
    val birthday: ZonedDateTime?
)
