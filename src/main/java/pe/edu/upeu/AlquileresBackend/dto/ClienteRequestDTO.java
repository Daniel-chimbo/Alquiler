package pe.edu.upeu.AlquileresBackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.edu.upeu.AlquileresBackend.enums.TipoCliente;

public record ClienteRequestDTO(
        @NotBlank(message = "El código es obligatorio")
        @Pattern(regexp = "^CLI-\\d{3}$", message = "El código debe tener el formato CLI-000")
        String codigo,

        @NotBlank(message = "El documento es obligatorio")
        @Pattern(regexp = "^(\\d{8}|\\d{11})$", message = "El documento debe tener 8 u 11 dígitos")
        String documento,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
        String nombre,

        @Email(message = "El email no tiene un formato válido")
        @Size(max = 120, message = "El email no puede superar 120 caracteres")
        String email,

        @Pattern(regexp = "^\\d{6,15}$", message = "El teléfono debe tener entre 6 y 15 dígitos")
        String telefono,

        @NotNull(message = "El tipo es obligatorio (PERSONA o EMPRESA)")
        TipoCliente tipo,

        Boolean estado) {
}
