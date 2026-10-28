package com.example.materialstudio.config;

import com.example.materialstudio.web.CurrentUserArgumentResolver;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 兼容两种接入方式：
 * 1) 前端经 Next.js rewrites 同源反代（默认，无需 CORS）；
 * 2) 前端直连 Java（按 app.cors.allowed-origins 放开带凭据跨域）。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserArgumentResolver;
    private final AppProperties props;

    public WebConfig(CurrentUserArgumentResolver currentUserArgumentResolver, AppProperties props) {
        this.currentUserArgumentResolver = currentUserArgumentResolver;
        this.props = props;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(props.cors().allowedOrigins().toArray(String[]::new))
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
