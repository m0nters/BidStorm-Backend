package com.taitrinh.online_auction.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Online Auction API", version = "1.0", description = "RESTful API for Online Auction Platform", contact = @Contact(name = "Online Auction Team", email = "support@onlineauction.com")), servers = {
        @Server(description = "Local Development Server", url = "http://localhost:${server.port}")
})
@SecurityScheme(name = "Bearer Authentication", type = SecuritySchemeType.HTTP, bearerFormat = "JWT", scheme = "bearer", in = SecuritySchemeIn.HEADER, description = "JWT authentication token. Use the access token received from login endpoint.")
public class OpenApiConfig {
}
