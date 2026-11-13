package pe.edu.upeu.AlquileresBackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.AlquileresBackend.entity.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    boolean existsByDocumento(String documento);

    boolean existsByDocumentoAndIdNot(String documento, Long id);
}
