package com.kw.ebook.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger（OpenAPI 3）文件配置。
 *
 * <p>啟動後訪問：
 * <ul>
 *   <li>UI：http://localhost:8080/swagger-ui.html</li>
 *   <li>JSON：http://localhost:8080/v3/api-docs</li>
 * </ul></p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ebookOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("電子書借閱系統 API")
                .description("書籍瀏覽、詳情、借閱與歸還等介面文件")
                .version("1.0.0")
                .contact(new Contact().name("kw")));
    }
}
