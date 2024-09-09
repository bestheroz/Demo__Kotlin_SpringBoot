package com.github.bestheroz.standard.config

import com.github.bestheroz.standard.common.authenticate.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
) {
    @Bean
    @Throws(java.lang.Exception::class)
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf({ obj: CsrfConfigurer<HttpSecurity?> -> obj.disable() })
            .cors({ cors: CorsConfigurer<HttpSecurity?> ->
                cors.configurationSource(
                    corsConfigurationSource(),
                )
            })
            .sessionManagement(
                { session: SessionManagementConfigurer<HttpSecurity?> ->
                    session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS,
                    )
                },
            ).authorizeHttpRequests(
                { auth ->
                    auth
                        .requestMatchers(HttpMethod.GET, *GET_PUBLIC)
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, *POST_PUBLIC)
                        .permitAll()
                        .anyRequest()
                        .authenticated()
                },
            ).addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): org.springframework.web.cors.CorsConfigurationSource {
        val configuration: CorsConfiguration = CorsConfiguration()

        configuration.addAllowedOrigin("http://localhost:8081")
        configuration.addAllowedHeader("*")
        configuration.addAllowedMethod("*")
        configuration.setAllowCredentials(true)

        val source =
            org.springframework.web.cors
                .UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    companion object {
        val GET_PUBLIC: Array<String> =
            arrayOf(
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/webjars/**",
                "/favicon.ico",
                "/api/v1/health/**",
                "/api/v1/notices",
                "/api/v1/notices/{id}",
                "/api/v1/admins/check-login-id",
                "/api/v1/admins/renew-token",
                "/api/v1/users/check-login-id",
                "/api/v1/users/renew-token",
            )
        val POST_PUBLIC: Array<String> =
            arrayOf(
                "/api/v1/admins/login",
                "/api/v1/users/login",
            )
    }
}
