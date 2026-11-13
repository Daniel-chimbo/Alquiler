package pe.edu.upeu.AlquileresBackend.dto;

import java.math.BigDecimal;

public record DetalleAlquilerResponseDTO(
        Long id,
        Long equipoId,
        String equipoCodigo,
        String equipoNombre,
        Integer cantidad,
        BigDecimal tarifaDiaria,
        BigDecimal importe) {
}
