package pe.edu.upeu.AlquileresBackend.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record EquipoRequestDTO(
        @NotBlank(message = "El código es obligatorio")
        @Pattern(regexp = "^EQP-\\d{3}$", message = "El código debe tener el formato EQP-000")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
        String nombre,

        @NotNull(message = "La tarifa diaria es obligatoria")
        @Positive(message = "La tarifa diaria debe ser mayor que cero")
        @Digits(integer = 8, fraction = 2, message = "La tarifa admite hasta 8 enteros y 2 decimales")
        BigDecimal tarifaDiaria,

        @NotNull(message = "Las unidades disponibles son obligatorias")
        @Min(value = 0, message = "Las unidades disponibles no pueden ser negativas")
        Integer unidadesDisponibles,

        Boolean estado,

        @NotNull(message = "La categoría es obligatoria")
        Long categoriaId) {
}
