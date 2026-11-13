package pe.edu.upeu.AlquileresBackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.AlquileresBackend.dto.EquipoAlquiladoDTO;
import pe.edu.upeu.AlquileresBackend.service.service.AlquilerService;

@RestController
@RequestMapping("/api/v1/reportes")
@Tag(name = "Reportes", description = "Reportes agregados")
public class ReporteController {

    private final AlquilerService alquilerService;

    public ReporteController(AlquilerService alquilerService) {
        this.alquilerService = alquilerService;
    }

    @GetMapping("/equipos-alquilados")
    @Operation(summary = "Unidades e importe bruto por equipo de los alquileres registrados en un periodo")
    public ResponseEntity<List<EquipoAlquiladoDTO>> equiposAlquilados(
            @Parameter(description = "Periodo AAAA-MM", example = "2026-11") @RequestParam String periodo,
            @RequestParam(required = false) Long categoriaId) {
        return ResponseEntity.ok(alquilerService.reporteEquiposAlquilados(periodo, categoriaId));
    }
}
