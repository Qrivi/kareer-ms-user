package be.kommaboard.kareer.user.lib.dto.request

import be.kommaboard.kareer.user.lib.constraint.NotCommon
import be.kommaboard.kareer.user.lib.constraint.NotSimple
import java.time.ZonedDateTime
import javax.validation.constraints.Email
import javax.validation.constraints.Past
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

data class UpdateUserDTO(

    @get:Pattern(message = "{UpdateUserDTO.role.Pattern}", regexp = "^(user|manager)$", flags = [Pattern.Flag.CASE_INSENSITIVE])
    val role: String?,

    @get:Size(message = "{UpdateUserDTO.slug.Size}", max = 50)
    @get:Pattern(message = "{UpdateUserDTO.slug.Pattern}", regexp = "^[\\w.]+\$")
    val slug: String?,

    @get:Pattern(regexp = "^(?!\\s*$).+", message = "{UpdateUserDTO.email.NotBlank}")
    @get:Size(message = "{UpdateUserDTO.email.Size}", max = 100)
    @get:Email(message = "{UpdateUserDTO.email.Email}")
    val email: String?,

    @get:Size(message = "{UpdateUserDTO.phone.Size}", max = 20)
    @get:Pattern(message = "{UpdateUserDTO.phone.Pattern}", regexp = "^(00|\\+)\\d+\$")
    val phone: String?,

    @get:Pattern(regexp = "^(?!\\s*$).+", message = "{UpdateUserDTO.password.NotBlank}")
    @get:Size(message = "{UpdateUserDTO.password.size}", min = 8)
    @get:NotCommon(message = "{UpdateUserDTO.password.NotCommon}")
    @get:NotSimple(message = "{UpdateUserDTO.password.NotSimple}")
    val password: String?,

    @get:Pattern(regexp = "^(?!\\s*$).+", message = "{UpdateUserDTO.lastName.NotBlank}")
    @get:Size(message = "{UpdateUserDTO.lastName.Size}", max = 100)
    val lastName: String?,

    @get:Pattern(regexp = "^(?!\\s*$).+", message = "{UpdateUserDTO.firstName.NotBlank}")
    @get:Size(message = "{UpdateUserDTO.firstName.Size}", max = 100)
    val firstName: String?,

    @get:Size(message = "{UpdateUserDTO.nickname.Size}", max = 100)
    val nickname: String?,

    @get:Pattern(regexp = "^(?!\\s*$).+", message = "{UpdateUserDTO.title.NotBlank}")
    @get:Size(message = "{UpdateUserDTO.title.Size}", max = 100)
    val title: String?,

    @get:Past(message = "{UpdateUserDTO.birthday.Past}")
    val birthday: ZonedDateTime?,
)
