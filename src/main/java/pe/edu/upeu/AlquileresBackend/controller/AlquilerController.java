package pe.edu.upeu.AlquileresBackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.AlquileresBackend.dto.AlquilerRequestDTO;
import pe.edu.upeu.AlquileresBackend.dto.AlquilerResponseDTO;
import pe.edu.upeu.AlquileresBackend.service.service.AlquilerService;

@RestController
@RequestMapping("/api/v1/alquileres")
@Tag(name = "Alquileres", description = "Operación cabecera–detalle y devolución")
public class AlquilerController {

    private final AlquilerService alquilerService;

    public AlquilerController(AlquilerService alquilerService) {
        this.alquilerService = alquilerService;
    }

    @PostMapping
    @Operation(summary = "Registra un alquiler con sus equipos (atómico; 409 si falla una regla)")
    public ResponseEntity<AlquilerResponseDTO> registrar(@Valid @RequestBody AlquilerRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alquilerService.registrar(request));
    }

    @GetMapping
    @Operation(summary = "Lista los alquileres con sus detalles")
    public ResponseEntity<List<AlquilerResponseDTO>> listar() {
        return ResponseEntity.ok(alquilerService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un alquiler con sus detalles")
    public ResponseEntity<AlquilerResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(alquilerService.buscarPorId(id));
    }

    @PatchMapping("/{id}/devolver")
    @Operation(summary = "Registra la devolución y repone las unidades (409 si no está VIGENTE)")
    public ResponseEntity<AlquilerResponseDTO> devolver(@PathVariable Long id) {
        return ResponseEntity.ok(alquilerService.devolver(id));
    }

    @DeleteMapping("/{id}/equipos/{equipoId}")
    @Operation(summary = "Retira un equipo de un alquiler VIGENTE, repone sus unidades y recalcula los totales "
            + "(409 si el alquiler quedaría sin equipos)")
    public ResponseEntity<AlquilerResponseDTO> retirarEquipo(@PathVariable Long id, @PathVariable Long equipoId) {
        return ResponseEntity.ok(alquilerService.retirarEquipo(id, equipoId));
    }
}
