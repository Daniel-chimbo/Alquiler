package pe.edu.upeu.AlquileresBackend.service.service;

import pe.edu.upeu.AlquileresBackend.dto.ClienteRequestDTO;
import pe.edu.upeu.AlquileresBackend.dto.ClienteResponseDTO;
import pe.edu.upeu.AlquileresBackend.service.generic.CrudService;

public interface ClienteService extends CrudService<ClienteRequestDTO, ClienteResponseDTO, Long> {
}
