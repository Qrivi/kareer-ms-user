package be.kommaboard.kareer.user

import be.kommaboard.kareer.user.controller.dto.validation.NotCommon
import be.kommaboard.kareer.user.controller.dto.validation.NotSimple
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.Email
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

@Validated
@ConfigurationProperties(prefix = "kareer")
class UserConfig(

    @get:NotBlank(message = "consumer-id cannot be blank.")
    var consumerId: String? = null,

    @get:NotBlank(message = "salt cannot be blank.")
    @get:Pattern(message = "salt must be valid BCrypt salt.", regexp = "^\\\$2a\\\$\\d+\\\$[\\w\\.]+\$")
    var salt: String? = null,

    @get:NotBlank(message = "admin-email cannot be blank.")
    @get:Email(message = "admin-email must be valid.")
    var adminEmail: String? = null,

    @get:NotBlank(message = "admin-password cannot be blank.")
    @get:NotSimple(message = "admin-password cannot be too simple.")
    @get:NotCommon(message = "admin-password cannot be too common.")
    @get:Size(message = "admin-password must be 8 characters or more.", min = 8)
    var adminPassword: String? = null,

    @get:NotNull(message = "confirm-email-ttl cannot be blank.")
    @get:Min(message = "confirm-email-ttl must be at least 1 hr.", value = 1L)
    var confirmEmailTtl: Long? = null,

    @get:NotNull(message = "reset-password-ttl cannot be blank.")
    @get:Min(message = "reset-password-ttl must be at least 1 hr.", value = 1L)
    var resetPasswordTtl: Long? = null,
)
