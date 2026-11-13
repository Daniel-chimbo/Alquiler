package pe.edu.upeu.AlquileresBackend.dto;

import java.math.BigDecimal;
import pe.edu.upeu.AlquileresBackend.enums.CondicionEquipo;

/** DTO plano: expone la categoría solo por id y nombre (sin objeto anidado ni referencias circulares). */
public record EquipoResponseDTO(
        Long id,
        String codigo,
        String nombre,
        BigDecimal tarifaDiaria,
        Integer unidadesDisponibles,
        Boolean estado,
        CondicionEquipo condicion,
        Long categoriaId,
        String categoriaNombre) {
}
