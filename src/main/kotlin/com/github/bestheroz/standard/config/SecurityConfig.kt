package com.github.bestheroz.standard.config

import com.github.bestheroz.standard.common.authenticate.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            cors { configurationSource = corsConfigurationSource() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            authorizeHttpRequests {
                authorize(HttpMethod.GET, "/swagger-ui.html", permitAll)
                authorize(HttpMethod.GET, "/swagger-ui/**", permitAll)
                authorize(HttpMethod.GET, "/v3/api-docs/**", permitAll)
                authorize(HttpMethod.GET, "/webjars/**", permitAll)
                authorize(HttpMethod.GET, "/favicon.ico", permitAll)
                authorize(HttpMethod.GET, "/api/v1/health/**", permitAll)
                authorize(HttpMethod.GET, "/api/v1/notices", permitAll)
                authorize(HttpMethod.GET, "/api/v1/notices/{id}", permitAll)
                authorize(HttpMethod.GET, "/api/v1/admins/check-login-id", permitAll)
                authorize(HttpMethod.GET, "/api/v1/admins/renew-token", permitAll)
                authorize(HttpMethod.GET, "/api/v1/users/check-login-id", permitAll)
                authorize(HttpMethod.GET, "/api/v1/users/renew-token", permitAll)
                authorize(HttpMethod.POST, "/api/v1/admins/login", permitAll)
                authorize(HttpMethod.POST, "/api/v1/users/login", permitAll)
                authorize(anyRequest, authenticated)
            }
            addFilterBefore<UsernamePasswordAuthenticationFilter>(jwtAuthenticationFilter)
        }
        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration =
            CorsConfiguration().apply {
                addAllowedOrigin("http://localhost:3000")
                addAllowedHeader("*")
                addAllowedMethod("*")
                allowCredentials = true
                exposedHeaders = listOf("Content-Disposition")
            }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
