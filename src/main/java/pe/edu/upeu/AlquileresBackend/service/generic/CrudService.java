package pe.edu.upeu.AlquileresBackend.service.generic;

import java.util.List;

/**
 * Contrato CRUD genérico basado en DTO: los controladores nunca reciben ni devuelven entidades.
 *
 * @param <Q> DTO de entrada (request)
 * @param <R> DTO de salida (response)
 * @param <ID> tipo del identificador
 */
public interface CrudService<Q, R, ID> {

    List<R> listar();

    R buscarPorId(ID id);

    R registrar(Q request);

    R actualizar(ID id, Q request);

    void eliminar(ID id);
}
