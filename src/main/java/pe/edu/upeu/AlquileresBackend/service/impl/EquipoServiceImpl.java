package pe.edu.upeu.AlquileresBackend.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.AlquileresBackend.dto.EquipoRequestDTO;
import pe.edu.upeu.AlquileresBackend.dto.EquipoResponseDTO;
import pe.edu.upeu.AlquileresBackend.entity.Categoria;
import pe.edu.upeu.AlquileresBackend.entity.Equipo;
import pe.edu.upeu.AlquileresBackend.exception.RecursoNoEncontradoException;
import pe.edu.upeu.AlquileresBackend.exception.ReglaNegocioException;
import pe.edu.upeu.AlquileresBackend.exception.SolicitudInvalidaException;
import pe.edu.upeu.AlquileresBackend.repository.AlquilerRepository;
import pe.edu.upeu.AlquileresBackend.repository.CategoriaRepository;
import pe.edu.upeu.AlquileresBackend.repository.EquipoRepository;
import pe.edu.upeu.AlquileresBackend.repository.EquipoSpecifications;
import pe.edu.upeu.AlquileresBackend.service.service.EquipoService;

@Service
public class EquipoServiceImpl implements EquipoService {

    private static final Logger log = LoggerFactory.getLogger(EquipoServiceImpl.class);

    /** Lista blanca de campos de orden: el cliente no puede ordenar por un atributo arbitrario. */
    private static final Map<String, String> CAMPOS_ORDEN = Map.of(
            "nombre", "nombre",
            "tarifa", "tarifaDiaria",
            "unidades", "unidadesDisponibles");

    private final EquipoRepository equipoRepository;
    private final CategoriaRepository categoriaRepository;
    private final AlquilerRepository alquilerRepository;

    public EquipoServiceImpl(EquipoRepository equipoRepository, CategoriaRepository categoriaRepository,
                             AlquilerRepository alquilerRepository) {
        this.equipoRepository = equipoRepository;
        this.categoriaRepository = categoriaRepository;
        this.alquilerRepository = alquilerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoResponseDTO> listar() {
        return equipoRepository.findAll(Sort.by("id")).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EquipoResponseDTO buscarPorId(Long id) {
        return toResponse(obtener(id));
    }

    @Override
    @Transactional
    public EquipoResponseDTO registrar(EquipoRequestDTO request) {
        Categoria categoria = obtenerCategoria(request.categoriaId());
        if (equipoRepository.existsByCodigo(request.codigo())) {
            throw new ReglaNegocioException("Ya existe un equipo con el código " + request.codigo());
        }
        Equipo equipo = new Equipo();
        copiar(request, equipo, categoria);
        equipo.setEstado(request.estado() == null ? Boolean.TRUE : request.estado());
        equipo = equipoRepository.save(equipo);
        log.info("Equipo registrado: id={}, codigo={}", equipo.getId(), equipo.getCodigo());
        return toResponse(equipo);
    }

    @Override
    @Transactional
    public EquipoResponseDTO actualizar(Long id, EquipoRequestDTO request) {
        Equipo equipo = obtener(id);
        Categoria categoria = obtenerCategoria(request.categoriaId());
        if (equipoRepository.existsByCodigoAndIdNot(request.codigo(), id)) {
            throw new ReglaNegocioException("Ya existe otro equipo con el código " + request.codigo());
        }
        copiar(request, equipo, categoria);
        if (request.estado() != null) {
            equipo.setEstado(request.estado());
        }
        log.info("Equipo actualizado: id={}", id);
        return toResponse(equipo);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Equipo equipo = obtener(id);
        if (alquilerRepository.contarDetallesConEquipo(id) > 0) {
            throw new ReglaNegocioException("No se puede eliminar el equipo " + equipo.getCodigo()
                    + " porque figura en alquileres registrados");
        }
        equipoRepository.delete(equipo);
        log.info("Equipo eliminado: id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoResponseDTO> listarPorCategoria(Long categoriaId) {
        obtenerCategoria(categoriaId);
        return equipoRepository.findByCategoriaIdOrderByNombreAsc(categoriaId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoResponseDTO> buscar(String nombre, Long categoriaId, BigDecimal tarifaMax,
                                          Boolean disponible, String orden, String dir) {
        String clave = orden == null || orden.isBlank() ? "nombre" : orden.trim().toLowerCase();
        String campo = CAMPOS_ORDEN.get(clave);
        if (campo == null) {
            throw new SolicitudInvalidaException("Campo de orden no válido: '" + orden
                    + "'. Valores permitidos: nombre, tarifa, unidades");
        }
        String direccion = dir == null || dir.isBlank() ? "asc" : dir.trim().toLowerCase();
        if (!direccion.equals("asc") && !direccion.equals("desc")) {
            throw new SolicitudInvalidaException("Dirección de orden no válida: '" + dir
                    + "'. Valores permitidos: asc, desc");
        }
        Sort sort = Sort.by(Sort.Direction.fromString(direccion), campo).and(Sort.by("codigo"));
        return equipoRepository
                .findAll(EquipoSpecifications.conFiltros(nombre, categoriaId, tarifaMax, disponible), sort)
                .stream().map(this::toResponse).toList();
    }

    private Equipo obtener(Long id) {
        return equipoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo", id));
    }

    private Categoria obtenerCategoria(Long categoriaId) {
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría", categoriaId));
    }

    private void copiar(EquipoRequestDTO request, Equipo equipo, Categoria categoria) {
        equipo.setCodigo(request.codigo());
        equipo.setNombre(request.nombre().trim());
        equipo.setTarifaDiaria(request.tarifaDiaria());
        equipo.setUnidadesDisponibles(request.unidadesDisponibles());
        equipo.setCategoria(categoria);
    }

    private EquipoResponseDTO toResponse(Equipo e) {
        return new EquipoResponseDTO(e.getId(), e.getCodigo(), e.getNombre(), e.getTarifaDiaria(),
                e.getUnidadesDisponibles(), e.getEstado(),
                e.getCategoria().getId(), e.getCategoria().getNombre());
    }
}
