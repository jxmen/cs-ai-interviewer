package dev.jxmen.cs.ai.interviewer.common.config

import dev.jxmen.cs.ai.interviewer.common.utils.JpaMemberArgumentResolver
import dev.jxmen.cs.ai.interviewer.common.utils.MemberArgumentResolver
import dev.jxmen.cs.ai.interviewer.persistence.mapper.MemberMapper
import dev.jxmen.cs.ai.interviewer.persistence.port.output.MemberQueryRepository
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val memberQueryRepository: MemberQueryRepository,
    private val memberMapper: MemberMapper,
) : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry
            .addMapping("/**")
            .allowedOrigins(
                "http://localhost:3000",
                "https://cs-ai-interviewer-web.vercel.app",
                "https://cs-ai.jxmen.dev",
            ).allowCredentials(true)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS")
            .allowedHeaders("*")
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(JpaMemberArgumentResolver(memberQueryRepository = memberQueryRepository))
        resolvers.add(
            MemberArgumentResolver(memberQueryRepository = memberQueryRepository, memberMapper = memberMapper),
        )
    }
}
