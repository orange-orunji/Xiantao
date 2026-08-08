package com.xiantao.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("闲淘 API 接口文档")
                        .description("闲淘二手闲置交易平台后端接口说明")
                        .contact(new Contact().name("xiantao"))
                        .version("1.0.0"));
    }
}
