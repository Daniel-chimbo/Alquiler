package pe.edu.upeu.AlquileresBackend.exception;

/** Parámetro de consulta inválido (orden, dirección, periodo): se traduce a 400. */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
