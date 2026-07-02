package com.caliarena

import com.caliarena.domain.token.Sha256TokenEncoder
import com.caliarena.domain.user.UsersDomainConfig
import com.caliarena.http.AuthenticatedUserArgumentResolver
import com.caliarena.http.AuthenticationInterceptor
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.Clock
import java.time.Duration

@Configuration
class PipelineConfigurer(
    private val authenticatedUserArgumentResolver: AuthenticatedUserArgumentResolver,
    private val authenticationInterceptor: AuthenticationInterceptor,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(authenticationInterceptor)
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(authenticatedUserArgumentResolver)
    }
}

@SpringBootApplication(scanBasePackages = ["com.caliarena"])
class App {
    @Bean
    fun tokenEncoder() = Sha256TokenEncoder()

    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder()

    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun usersDomainConfig() =
        UsersDomainConfig(
            tokenSizeInBytes = 256 / 8,
            tokenTtl = Duration.ofHours(24),
            tokenRollingTtl = Duration.ofHours(1),
            maxTokensPerUser = 3,
        )
}

fun main() {
    runApplication<App>()
}
