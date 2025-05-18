package org.stapledon.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SwaggerConfig implements WebMvcConfigurer {
    @Override
    public void addViewControllers(final ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/swagger-ui.html");
        // use setStatusCode(HttpStatus.XYZ) for any custom status code if required, e.g. MOVED_PERMANENTLY
        registry.addRedirectViewController("/swagger-ui", "/swagger-ui/index.html");
        // any other alias
    }

    @Bean
    public OpenAPI comicCacherOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Comic Cacher API")
                        .description("Web comic downloader and viewer application")
                        .version("1.2.0")
                        .license(new License().name("MIT License")));
    }
}
