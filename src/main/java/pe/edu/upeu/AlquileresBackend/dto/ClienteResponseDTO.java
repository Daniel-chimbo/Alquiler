package pe.edu.upeu.AlquileresBackend.dto;

import pe.edu.upeu.AlquileresBackend.enums.TipoCliente;

public record ClienteResponseDTO(
        Long id,
        String codigo,
        String documento,
        String nombre,
        String email,
        String telefono,
        TipoCliente tipo,
        Boolean estado) {
}
