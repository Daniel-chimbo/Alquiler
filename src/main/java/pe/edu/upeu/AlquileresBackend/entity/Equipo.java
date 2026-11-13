package pe.edu.upeu.AlquileresBackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.AlquileresBackend.enums.CondicionEquipo;

@Entity
@Table(name = "equipos")
@Getter
@Setter
@NoArgsConstructor
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 7)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "tarifa_diaria", nullable = false, precision = 10, scale = 2)
    private BigDecimal tarifaDiaria;

    @Column(name = "unidades_disponibles", nullable = false)
    private Integer unidadesDisponibles;

    @Column(name = "estado", nullable = false)
    private Boolean estado;

    /**
     * SC-D. La columna admite null solo para que ddl-auto=update pueda agregarla a una tabla
     * con filas; la aplicación siempre guarda un valor (BUENO por defecto).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "condicion", length = 10)
    private CondicionEquipo condicion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @PrePersist
    void prePersist() {
        fechaCreacion = LocalDateTime.now();
        if (estado == null) estado = true;
        if (condicion == null) condicion = CondicionEquipo.BUENO;
    }

    @PreUpdate
    void preUpdate() {
        fechaModificacion = LocalDateTime.now();
    }
}
