package pe.edu.upeu.AlquileresBackend.dto;

import java.math.BigDecimal;

/**
 * Proyección del reporte RF-07. Se construye directamente en la consulta JPQL
 * (SELECT new ...) con GROUP BY, sin recorrer findAll() en Java.
 */
public record EquipoAlquiladoDTO(
        String codigo,
        String equipo,
        Long unidadesAlquiladas,
        BigDecimal importeBruto) {
}
