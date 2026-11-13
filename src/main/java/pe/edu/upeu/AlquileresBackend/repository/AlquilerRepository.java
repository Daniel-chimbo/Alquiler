package pe.edu.upeu.AlquileresBackend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.AlquileresBackend.dto.EquipoAlquiladoDTO;
import pe.edu.upeu.AlquileresBackend.dto.IngresoCategoriaDTO;
import pe.edu.upeu.AlquileresBackend.entity.Alquiler;
import pe.edu.upeu.AlquileresBackend.enums.EstadoAlquiler;

public interface AlquilerRepository extends JpaRepository<Alquiler, Long> {

    /** RN-03: conteo agregado de alquileres de un cliente en un estado (SELECT COUNT). */
    @Query("select count(a) from Alquiler a where a.cliente.id = :clienteId and a.estado = :estado")
    long contarPorClienteYEstado(@Param("clienteId") Long clienteId, @Param("estado") EstadoAlquiler estado);

    /** Para impedir eliminar un equipo que ya figura en algún alquiler (409). */
    @Query("select count(d) from DetalleAlquiler d where d.equipo.id = :equipoId")
    long contarDetallesConEquipo(@Param("equipoId") Long equipoId);

    @Query("select count(a) from Alquiler a where a.cliente.id = :clienteId")
    long contarPorCliente(@Param("clienteId") Long clienteId);

    /** Cabecera + cliente + detalles + equipo en una sola consulta (evita N+1 y LazyInitialization). */
    @Query("""
            select distinct a from Alquiler a
              join fetch a.cliente
              left join fetch a.detalles d
              left join fetch d.equipo
            where a.id = :id
            """)
    Optional<Alquiler> buscarConDetalles(@Param("id") Long id);

    @Query("""
            select distinct a from Alquiler a
              join fetch a.cliente
              left join fetch a.detalles d
              left join fetch d.equipo
            order by a.id
            """)
    List<Alquiler> listarConDetalles();

    /**
     * SC-A: alquileres de un cliente, del más reciente al más antiguo por fecha de inicio.
     * El estado es un filtro opcional: si llega null, no se filtra.
     */
    @Query("""
            select distinct a from Alquiler a
              join fetch a.cliente c
              left join fetch a.detalles d
              left join fetch d.equipo
            where c.id = :clienteId
              and (:estado is null or a.estado = :estado)
            order by a.fechaInicio desc, a.id desc
            """)
    List<Alquiler> listarPorCliente(@Param("clienteId") Long clienteId, @Param("estado") EstadoAlquiler estado);

    /**
     * RF-07: reporte agregado con GROUP BY y proyección construida en la consulta.
     * Considera los alquileres registrados en el periodo, vigentes o devueltos.
     */
    @Query("""
            select new pe.edu.upeu.AlquileresBackend.dto.EquipoAlquiladoDTO(
                     e.codigo, e.nombre, sum(d.cantidad), sum(d.importe))
              from DetalleAlquiler d
              join d.alquiler a
              join d.equipo e
             where a.fechaRegistro >= :desde
               and a.fechaRegistro < :hasta
               and (:categoriaId is null or e.categoria.id = :categoriaId)
             group by e.codigo, e.nombre
             order by sum(d.cantidad) desc, e.codigo asc
            """)
    List<EquipoAlquiladoDTO> reporteEquiposAlquilados(@Param("desde") LocalDateTime desde,
                                                      @Param("hasta") LocalDateTime hasta,
                                                      @Param("categoriaId") Long categoriaId);

    /**
     * SC-C: ingresos por categoría de los alquileres registrados en el periodo.
     * count(distinct a.id) cuenta cada alquiler una sola vez aunque tenga varios equipos de la categoría.
     */
    @Query("""
            select new pe.edu.upeu.AlquileresBackend.dto.IngresoCategoriaDTO(
                     c.id, c.nombre, count(distinct a.id), sum(d.cantidad), sum(d.importe))
              from DetalleAlquiler d
              join d.alquiler a
              join d.equipo e
              join e.categoria c
             where a.fechaRegistro >= :desde
               and a.fechaRegistro < :hasta
             group by c.id, c.nombre
             order by sum(d.importe) desc, c.nombre asc
            """)
    List<IngresoCategoriaDTO> reporteIngresosPorCategoria(@Param("desde") LocalDateTime desde,
                                                          @Param("hasta") LocalDateTime hasta);
}
