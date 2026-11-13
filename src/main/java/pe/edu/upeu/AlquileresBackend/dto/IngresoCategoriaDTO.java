package pe.edu.upeu.AlquileresBackend.dto;

import java.math.BigDecimal;

/**
 * SC-C: proyección del reporte de ingresos por categoría. Se construye en la
 * consulta JPQL (SELECT new ...) con GROUP BY, sin bucles en Java.
 */
public record IngresoCategoriaDTO(
        Long categoriaId,
        String categoria,
        Long numeroAlquileres,
        Long unidades,
        BigDecimal importeBruto) {
}
