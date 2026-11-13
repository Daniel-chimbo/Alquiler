package pe.edu.upeu.AlquileresBackend.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.AlquileresBackend.entity.Equipo;

public interface EquipoRepository extends JpaRepository<Equipo, Long>, JpaSpecificationExecutor<Equipo> {

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    boolean existsByCategoriaId(Long categoriaId);

    List<Equipo> findByCategoriaIdOrderByNombreAsc(Long categoriaId);

    /**
     * Lee el equipo bloqueando su fila (SELECT ... FOR UPDATE) hasta el fin de la transacción.
     * Así dos alquileres simultáneos de las últimas unidades no pueden descontar ambos.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Equipo e where e.id = :id")
    Optional<Equipo> findByIdParaActualizar(@Param("id") Long id);
}
