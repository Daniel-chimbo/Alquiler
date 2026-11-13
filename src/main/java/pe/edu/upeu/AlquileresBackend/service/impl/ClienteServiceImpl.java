package pe.edu.upeu.AlquileresBackend.service.impl;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.AlquileresBackend.dto.ClienteRequestDTO;
import pe.edu.upeu.AlquileresBackend.dto.ClienteResponseDTO;
import pe.edu.upeu.AlquileresBackend.entity.Cliente;
import pe.edu.upeu.AlquileresBackend.exception.RecursoNoEncontradoException;
import pe.edu.upeu.AlquileresBackend.exception.ReglaNegocioException;
import pe.edu.upeu.AlquileresBackend.repository.AlquilerRepository;
import pe.edu.upeu.AlquileresBackend.repository.ClienteRepository;
import pe.edu.upeu.AlquileresBackend.service.service.ClienteService;

@Service
public class ClienteServiceImpl implements ClienteService {

    private static final Logger log = LoggerFactory.getLogger(ClienteServiceImpl.class);

    private final ClienteRepository clienteRepository;
    private final AlquilerRepository alquilerRepository;

    public ClienteServiceImpl(ClienteRepository clienteRepository, AlquilerRepository alquilerRepository) {
        this.clienteRepository = clienteRepository;
        this.alquilerRepository = alquilerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> listar() {
        return clienteRepository.findAll(Sort.by("id")).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorId(Long id) {
        return toResponse(obtener(id));
    }

    @Override
    @Transactional
    public ClienteResponseDTO registrar(ClienteRequestDTO request) {
        if (clienteRepository.existsByCodigo(request.codigo())) {
            throw new ReglaNegocioException("Ya existe un cliente con el código " + request.codigo());
        }
        if (clienteRepository.existsByDocumento(request.documento())) {
            throw new ReglaNegocioException("Ya existe un cliente con el documento " + request.documento());
        }
        Cliente cliente = new Cliente();
        copiar(request, cliente);
        cliente.setEstado(request.estado() == null ? Boolean.TRUE : request.estado());
        cliente = clienteRepository.save(cliente);
        log.info("Cliente registrado: id={}, codigo={}", cliente.getId(), cliente.getCodigo());
        return toResponse(cliente);
    }

    @Override
    @Transactional
    public ClienteResponseDTO actualizar(Long id, ClienteRequestDTO request) {
        Cliente cliente = obtener(id);
        if (clienteRepository.existsByCodigoAndIdNot(request.codigo(), id)) {
            throw new ReglaNegocioException("Ya existe otro cliente con el código " + request.codigo());
        }
        if (clienteRepository.existsByDocumentoAndIdNot(request.documento(), id)) {
            throw new ReglaNegocioException("Ya existe otro cliente con el documento " + request.documento());
        }
        copiar(request, cliente);
        if (request.estado() != null) {
            cliente.setEstado(request.estado());
        }
        log.info("Cliente actualizado: id={}", id);
        return toResponse(cliente);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = obtener(id);
        if (alquilerRepository.contarPorCliente(id) > 0) {
            throw new ReglaNegocioException("No se puede eliminar el cliente " + cliente.getCodigo()
                    + " porque tiene alquileres registrados");
        }
        clienteRepository.delete(cliente);
        log.info("Cliente eliminado: id={}", id);
    }

    private Cliente obtener(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", id));
    }

    private void copiar(ClienteRequestDTO request, Cliente cliente) {
        cliente.setCodigo(request.codigo());
        cliente.setDocumento(request.documento());
        cliente.setNombre(request.nombre().trim());
        cliente.setEmail(request.email());
        cliente.setTelefono(request.telefono());
        cliente.setTipo(request.tipo());
    }

    private ClienteResponseDTO toResponse(Cliente c) {
        return new ClienteResponseDTO(c.getId(), c.getCodigo(), c.getDocumento(), c.getNombre(),
                c.getEmail(), c.getTelefono(), c.getTipo(), c.getEstado());
    }
}
