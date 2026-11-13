package pe.edu.upeu.AlquileresBackend.service.service;

import java.math.BigDecimal;
import java.util.List;
import pe.edu.upeu.AlquileresBackend.dto.EquipoRequestDTO;
import pe.edu.upeu.AlquileresBackend.dto.EquipoResponseDTO;
import pe.edu.upeu.AlquileresBackend.enums.CondicionEquipo;
import pe.edu.upeu.AlquileresBackend.service.generic.CrudService;

public interface EquipoService extends CrudService<EquipoRequestDTO, EquipoResponseDTO, Long> {

    List<EquipoResponseDTO> listarPorCategoria(Long categoriaId);

    List<EquipoResponseDTO> buscar(String nombre, Long categoriaId, BigDecimal tarifaMax,
                                   Boolean disponible, CondicionEquipo condicion, String orden, String dir);
}
