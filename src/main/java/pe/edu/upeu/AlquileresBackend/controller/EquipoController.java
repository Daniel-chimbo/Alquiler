package pe.edu.upeu.AlquileresBackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.AlquileresBackend.dto.EquipoRequestDTO;
import pe.edu.upeu.AlquileresBackend.dto.EquipoResponseDTO;
import pe.edu.upeu.AlquileresBackend.enums.CondicionEquipo;
import pe.edu.upeu.AlquileresBackend.service.service.EquipoService;

@RestController
@RequestMapping("/api/v1/equipos")
@Tag(name = "Equipos", description = "CRUD y búsqueda de equipos")
public class EquipoController {

    private final EquipoService equipoService;

    public EquipoController(EquipoService equipoService) {
        this.equipoService = equipoService;
    }

    @GetMapping
    @Operation(summary = "Lista los equipos")
    public ResponseEntity<List<EquipoResponseDTO>> listar() {
        return ResponseEntity.ok(equipoService.listar());
    }

    @GetMapping("/buscar")
    @Operation(summary = "Busca equipos con filtros opcionales y combinables (sin paginación)")
    public ResponseEntity<List<EquipoResponseDTO>> buscar(
            @Parameter(description = "Contiene, sin distinguir mayúsculas") @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) BigDecimal tarifaMax,
            @Parameter(description = "true = unidades disponibles > 0") @RequestParam(required = false) Boolean disponible,
            @Parameter(description = "NUEVO | BUENO | REGULAR") @RequestParam(required = false) CondicionEquipo condicion,
            @Parameter(description = "nombre | tarifa | unidades") @RequestParam(required = false, defaultValue = "nombre") String orden,
            @Parameter(description = "asc | desc") @RequestParam(required = false, defaultValue = "asc") String dir) {
        return ResponseEntity.ok(equipoService.buscar(nombre, categoriaId, tarifaMax, disponible, condicion, orden, dir));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un equipo por id")
    public ResponseEntity<EquipoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(equipoService.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Registra un equipo (404 si la categoría no existe)")
    public ResponseEntity<EquipoResponseDTO> registrar(@Valid @RequestBody EquipoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipoService.registrar(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un equipo")
    public ResponseEntity<EquipoResponseDTO> actualizar(@PathVariable Long id,
                                                        @Valid @RequestBody EquipoRequestDTO request) {
        return ResponseEntity.ok(equipoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un equipo sin alquileres (409 si figura en alquileres)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        equipoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
