package pe.edu.upeu.AlquileresBackend.repository;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import pe.edu.upeu.AlquileresBackend.entity.Equipo;

/** Filtros opcionales y combinables de RF-06: solo se agrega el predicado si el parámetro llega. */
public final class EquipoSpecifications {

    private EquipoSpecifications() {
    }

    public static Specification<Equipo> conFiltros(String nombre, Long categoriaId,
                                                   BigDecimal tarifaMax, Boolean disponible) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            if (nombre != null && !nombre.isBlank()) {
                predicados.add(cb.like(cb.lower(root.get("nombre")),
                        "%" + nombre.trim().toLowerCase() + "%"));
            }
            if (categoriaId != null) {
                predicados.add(cb.equal(root.get("categoria").get("id"), categoriaId));
            }
            if (tarifaMax != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("tarifaDiaria"), tarifaMax));
            }
            if (disponible != null) {
                predicados.add(disponible
                        ? cb.greaterThan(root.get("unidadesDisponibles"), 0)
                        : cb.equal(root.get("unidadesDisponibles"), 0));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }
}
