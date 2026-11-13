package pe.edu.upeu.AlquileresBackend.dto;

public record CategoriaResponseDTO(
        Long id,
        String nombre,
        String descripcion,
        Boolean estado) {
}
