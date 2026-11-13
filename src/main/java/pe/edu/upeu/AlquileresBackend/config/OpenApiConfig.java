package pe.edu.upeu.AlquileresBackend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI alquileresOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Alquileres API")
                .version("v1")
                .description("EquipAndes - alquiler de equipos de sonido, proyección e iluminación para eventos"));
    }
}
