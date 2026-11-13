package pe.edu.upeu.AlquileresBackend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import pe.edu.upeu.AlquileresBackend.enums.EstadoAlquiler;

public record AlquilerResponseDTO(
        Long id,
        LocalDateTime fechaRegistro,
        LocalDate fechaInicio,
        Integer dias,
        Long clienteId,
        String clienteCodigo,
        String clienteNombre,
        String lugarEvento,
        EstadoAlquiler estado,
        LocalDateTime fechaDevolucion,
        Integer totalUnidades,
        BigDecimal subtotal,
        BigDecimal descuento,
        BigDecimal montoTotal,
        List<DetalleAlquilerResponseDTO> detalles) {
}
