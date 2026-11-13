package pe.edu.upeu.AlquileresBackend.service.service;

import java.util.List;
import pe.edu.upeu.AlquileresBackend.dto.AlquilerRequestDTO;
import pe.edu.upeu.AlquileresBackend.dto.AlquilerResponseDTO;
import pe.edu.upeu.AlquileresBackend.dto.EquipoAlquiladoDTO;
import pe.edu.upeu.AlquileresBackend.enums.EstadoAlquiler;

public interface AlquilerService {

    AlquilerResponseDTO registrar(AlquilerRequestDTO request);

    AlquilerResponseDTO buscarPorId(Long id);

    List<AlquilerResponseDTO> listar();

    List<AlquilerResponseDTO> listarPorCliente(Long clienteId, EstadoAlquiler estado);

    AlquilerResponseDTO devolver(Long id);

    List<EquipoAlquiladoDTO> reporteEquiposAlquilados(String periodo, Long categoriaId);
}
