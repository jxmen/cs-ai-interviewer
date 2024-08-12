package dev.jxmen.cs.ai.interviewer.global.config.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.springframework.web.filter.OncePerRequestFilter

@Suppress("ktlint:standard:chain-method-continuation") // NOTE: 활성화시 오히려 가독성이 저하되어 비활성화
@EnableWebSecurity
@Configuration
class SecurityConfig(
    @Value("\${spring.profiles.active:default}") // NOTE: 값이 없을시 콜론 뒤에 기본값 지정 가능
    private val activeProfile: String,
) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        if (activeProfile == "local" || activeProfile == "default") {
            // set h2 console to be accessible
            http
                .headers {
                    it.frameOptions { it.disable() }
                }
        }

        http
            .sessionManagement {
                // 세션 유지하지 않도록 설정
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .csrf { it.disable() }
            .authorizeHttpRequests {
                // NOTE: TokenFilter에서 처리하기 때문에 별다른 설정 없음
                it.anyRequest().permitAll()
            }
            .oauth2Login { } // NOTE: TokenFilter에서 처리하기 때문에 별다른 설정 없음
            .exceptionHandling {
                // 예외 발생시 커스텀 응답 반환
                it.authenticationEntryPoint(setCustomResponseAuthenticationEntryPoint())
            }
            .addFilterBefore(tokenFilter(), BasicAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun setCustomResponseAuthenticationEntryPoint(): AuthenticationEntryPoint = SetCustomResponseAuthenticationEntryPoint()

    @Bean
    fun tokenFilter(): OncePerRequestFilter = TokenFilter()
}
