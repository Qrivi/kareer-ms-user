package be.kommaboard.kareer.user

import be.kommaboard.kareer.user.lib.constraint.NotCommon
import be.kommaboard.kareer.user.lib.constraint.NotSimple
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.URL
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "kareer.config")
data class KareerConfig(

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

    @get:NotBlank(message = "register-url cannot be blank.")
    @get:URL(message = "register-url must be valid.")
    var registerUrl: String? = null,
)
