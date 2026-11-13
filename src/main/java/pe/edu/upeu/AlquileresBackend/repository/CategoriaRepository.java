package pe.edu.upeu.AlquileresBackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.AlquileresBackend.entity.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    /** Unicidad sin distinguir mayúsculas ni espacios: el parámetro llega ya normalizado (trim + minúsculas). */
    @Query("select count(c) from Categoria c where lower(trim(c.nombre)) = :nombre")
    long contarPorNombreNormalizado(@Param("nombre") String nombreNormalizado);

    @Query("select count(c) from Categoria c where lower(trim(c.nombre)) = :nombre and c.id <> :id")
    long contarPorNombreNormalizadoEnOtra(@Param("nombre") String nombreNormalizado, @Param("id") Long id);
}
