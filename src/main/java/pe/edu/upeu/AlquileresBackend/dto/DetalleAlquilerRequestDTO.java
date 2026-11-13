package pe.edu.upeu.AlquileresBackend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DetalleAlquilerRequestDTO(
        @NotNull(message = "El equipo es obligatorio")
        Long equipoId,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        Integer cantidad) {
}
