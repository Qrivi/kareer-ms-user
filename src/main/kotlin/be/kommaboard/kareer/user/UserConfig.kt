package be.kommaboard.kareer.user

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "kareer")
class UserConfig {

    val salt = "0VKvg9UVp1WDcUKLPH7o2Vp9RSeLXIVQ"

    val adminEmail = "kommaboard@kristofdewil.de"
    val adminPassword = "admin"

    val confirmEmailTTL = 7L
    val resetPasswordTTL = 1L
}
