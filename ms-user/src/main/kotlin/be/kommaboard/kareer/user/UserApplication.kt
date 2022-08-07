package be.kommaboard.kareer.user

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.support.ReloadableResourceBundleMessageSource

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(UserConfig::class)
class UserApplication {

    @Bean
    fun messageSource(): MessageSource = ReloadableResourceBundleMessageSource().apply {
        setUseCodeAsDefaultMessage(true)
        setDefaultEncoding(Charsets.UTF_8.name())
        setBasenames(
            "classpath:common_messages",
            "classpath:user_messages",
            "classpath:messages",
        )
    }
}

fun main(args: Array<String>) {
    runApplication<UserApplication>(*args)
}
