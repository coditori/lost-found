 package com.example.lostfound.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Lost & Found API",
        version = "1.0.0",
        description = """
            Simple Lost & Found Service API for managing lost items and claims.
            
            ## Features
            - Browse and search lost items
            - Create claims for lost items
            - Upload files containing lost item records (Admin only)
            - Manage claims (Admin only)
            
            ## Authentication
            Uses HTTP Basic Authentication. Provide username and password.
            
            ## Roles
            - **USER**: Browse items and create claims
            - **ADMIN**: Upload files and manage all claims
            """,
        contact = @Contact(
            name = "Lost & Found Service",
            email = "support@lostfound.example.com"
        ),
        license = @License(
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = {
        @Server(
            url = "http://localhost:9095",
            description = "Development Server"
        )
    }
)
@SecurityScheme(
    name = "basicAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "basic",
    description = "HTTP Basic Authentication"
)
public class OpenApiConfig {}