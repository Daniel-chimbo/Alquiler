package pe.edu.upeu.AlquileresBackend.exception.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;

/** Cuerpo de error uniforme para todas las respuestas 4xx/5xx. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDTO(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors) {
}
