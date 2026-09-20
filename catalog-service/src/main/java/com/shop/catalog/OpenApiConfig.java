package com.shop.catalog;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ecommerceCatalogOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("E-commerce Catalog API")
                        .version("v1")
                        .description("REST API for product categories, products, variants, and catalog images. Image uploads accept JPEG, PNG, and WEBP up to 5 MB.")
                        .contact(new Contact()
                                .name("E-commerce Platform Team")))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
