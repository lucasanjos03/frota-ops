package br.edu.cesmac.frota_ops.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registra o {@link AuthInterceptor} em todas as rotas, exceto as públicas: as telas de
 * login e cadastro, os recursos estáticos e a página de erro do contêiner.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login",
                        "/cadastro",
                        "/css/**",
                        "/js/**",
                        "/favicon.ico",
                        "/error");
    }
}
