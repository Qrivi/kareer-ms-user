package be.kommaboard.kareer.user

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.validation.Validator
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver
import java.util.Locale

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
class UserApplication {

    @Bean
    fun localeResolver(): LocaleResolver {
        val resolver = AcceptHeaderLocaleResolver()
        resolver.supportedLocales = listOf(
            Locale("en"),
            Locale("nl"),
        )
        resolver.defaultLocale = resolver.supportedLocales.first()
        return resolver
    }

    @Bean
    fun messageSource(): MessageSource {
        val source = ReloadableResourceBundleMessageSource()
        source.setBasenames("classpath:messages")
        source.setDefaultEncoding(Charsets.UTF_8.name())
        source.setUseCodeAsDefaultMessage(true)
        return source
    }

    @Bean
    fun getValidator(): Validator {
        val validator = LocalValidatorFactoryBean()
        validator.setValidationMessageSource(messageSource())
        return validator
    }
}

fun main(args: Array<String>) {
    runApplication<UserApplication>(*args)
}
