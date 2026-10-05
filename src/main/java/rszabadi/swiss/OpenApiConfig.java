package rszabadi.swiss;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("Vehicle Inventory API")
                .version("0.1.0")
                .description("REST API for a used-car dealer's vehicle inventory (sample data only)."));
    }
}