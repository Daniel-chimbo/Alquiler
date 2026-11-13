package pe.edu.upeu.AlquileresBackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * La fecha de inicio solo se valida aquí como obligatoria: el rango (hoy .. hoy+60)
 * es la regla RN-01 y vive en el servicio, por eso responde 409 y no 400.
 */
public record AlquilerRequestDTO(
        @NotNull(message = "El cliente es obligatorio")
        Long clienteId,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio,

        @NotNull(message = "Los días son obligatorios")
        @Min(value = 1, message = "El alquiler es de 1 a 30 días")
        @Max(value = 30, message = "El alquiler es de 1 a 30 días")
        Integer dias,

        @Size(max = 150, message = "El lugar del evento no puede superar 150 caracteres")
        String lugarEvento,

        @NotEmpty(message = "El alquiler debe incluir al menos un equipo")
        List<@Valid @NotNull DetalleAlquilerRequestDTO> detalles) {
}
