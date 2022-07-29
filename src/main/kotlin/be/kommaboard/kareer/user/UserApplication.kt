package be.kommaboard.kareer.user

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Bean
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver
import java.util.Locale

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
class UserApplication

fun main(args: Array<String>) {
    runApplication<UserApplication>(*args)
}
