package pe.edu.upeu.AlquileresBackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.AlquileresBackend.dto.AlquilerResponseDTO;
import pe.edu.upeu.AlquileresBackend.dto.ClienteRequestDTO;
import pe.edu.upeu.AlquileresBackend.dto.ClienteResponseDTO;
import pe.edu.upeu.AlquileresBackend.enums.EstadoAlquiler;
import pe.edu.upeu.AlquileresBackend.service.service.AlquilerService;
import pe.edu.upeu.AlquileresBackend.service.service.ClienteService;

@RestController
@RequestMapping("/api/v1/clientes")
@Tag(name = "Clientes", description = "Registro, consulta, actualización y listado de clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final AlquilerService alquilerService;

    public ClienteController(ClienteService clienteService, AlquilerService alquilerService) {
        this.clienteService = clienteService;
        this.alquilerService = alquilerService;
    }

    @GetMapping
    @Operation(summary = "Lista los clientes")
    public ResponseEntity<List<ClienteResponseDTO>> listar() {
        return ResponseEntity.ok(clienteService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un cliente por id")
    public ResponseEntity<ClienteResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscarPorId(id));
    }

    @GetMapping("/{id}/alquileres")
    @Operation(summary = "Alquileres del cliente del más reciente al más antiguo por fecha de inicio (404 si el cliente no existe)")
    public ResponseEntity<List<AlquilerResponseDTO>> listarAlquileres(
            @PathVariable Long id,
            @Parameter(description = "Filtro opcional: VIGENTE | DEVUELTO") @RequestParam(required = false) EstadoAlquiler estado) {
        return ResponseEntity.ok(alquilerService.listarPorCliente(id, estado));
    }

    @PostMapping
    @Operation(summary = "Registra un cliente (código y documento únicos)")
    public ResponseEntity<ClienteResponseDTO> registrar(@Valid @RequestBody ClienteRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.registrar(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un cliente")
    public ResponseEntity<ClienteResponseDTO> actualizar(@PathVariable Long id,
                                                         @Valid @RequestBody ClienteRequestDTO request) {
        return ResponseEntity.ok(clienteService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un cliente sin alquileres (409 si tiene alquileres)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        clienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
