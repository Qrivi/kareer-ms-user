package be.kommaboard.kareer.user

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "kareer")
data class UserConfig (

    var salt: String = "\$2a\$12\$s2o5YgwkqXj90ArJV7SYbe",

    var adminEmail: String = "kommaboard@kristofdewil.de",
    var adminPassword: String = "admin",

    var confirmEmailTTL: Long = 7L,
    var resetPasswordTTL: Long = 1L,
)
