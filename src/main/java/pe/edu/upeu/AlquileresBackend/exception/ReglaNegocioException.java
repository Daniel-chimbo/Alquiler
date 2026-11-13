package pe.edu.upeu.AlquileresBackend.exception;

/**
 * Violación de una regla de negocio: se traduce a 409.
 * Es RuntimeException, por eso provoca rollback de la transacción (@Transactional).
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
