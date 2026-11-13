package pe.edu.upeu.AlquileresBackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.AlquileresBackend.dto.CategoriaRequestDTO;
import pe.edu.upeu.AlquileresBackend.dto.CategoriaResponseDTO;
import pe.edu.upeu.AlquileresBackend.dto.EquipoResponseDTO;
import pe.edu.upeu.AlquileresBackend.service.service.CategoriaService;
import pe.edu.upeu.AlquileresBackend.service.service.EquipoService;

@RestController
@RequestMapping("/api/v1/categorias")
@Tag(name = "Categorías", description = "CRUD de categorías de equipo")
public class CategoriaController {

    private final CategoriaService categoriaService;
    private final EquipoService equipoService;

    public CategoriaController(CategoriaService categoriaService, EquipoService equipoService) {
        this.categoriaService = categoriaService;
        this.equipoService = equipoService;
    }

    @GetMapping
    @Operation(summary = "Lista las categorías")
    public ResponseEntity<List<CategoriaResponseDTO>> listar() {
        return ResponseEntity.ok(categoriaService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una categoría por id")
    public ResponseEntity<CategoriaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaService.buscarPorId(id));
    }

    @GetMapping("/{id}/equipos")
    @Operation(summary = "Lista los equipos de una categoría")
    public ResponseEntity<List<EquipoResponseDTO>> listarEquipos(@PathVariable Long id) {
        return ResponseEntity.ok(equipoService.listarPorCategoria(id));
    }

    @PostMapping
    @Operation(summary = "Registra una categoría (nombre único sin distinguir mayúsculas ni espacios)")
    public ResponseEntity<CategoriaResponseDTO> registrar(@Valid @RequestBody CategoriaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.registrar(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza una categoría")
    public ResponseEntity<CategoriaResponseDTO> actualizar(@PathVariable Long id,
                                                           @Valid @RequestBody CategoriaRequestDTO request) {
        return ResponseEntity.ok(categoriaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina una categoría sin equipos (409 si tiene equipos)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        categoriaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
