package pe.edu.upeu.AlquileresBackend.service.impl;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.AlquileresBackend.dto.CategoriaRequestDTO;
import pe.edu.upeu.AlquileresBackend.dto.CategoriaResponseDTO;
import pe.edu.upeu.AlquileresBackend.entity.Categoria;
import pe.edu.upeu.AlquileresBackend.exception.RecursoNoEncontradoException;
import pe.edu.upeu.AlquileresBackend.exception.ReglaNegocioException;
import pe.edu.upeu.AlquileresBackend.repository.CategoriaRepository;
import pe.edu.upeu.AlquileresBackend.repository.EquipoRepository;
import pe.edu.upeu.AlquileresBackend.service.service.CategoriaService;

@Service
public class CategoriaServiceImpl implements CategoriaService {

    private static final Logger log = LoggerFactory.getLogger(CategoriaServiceImpl.class);

    private final CategoriaRepository categoriaRepository;
    private final EquipoRepository equipoRepository;

    public CategoriaServiceImpl(CategoriaRepository categoriaRepository, EquipoRepository equipoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.equipoRepository = equipoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponseDTO> listar() {
        return categoriaRepository.findAll(Sort.by("id")).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaResponseDTO buscarPorId(Long id) {
        return toResponse(obtener(id));
    }

    @Override
    @Transactional
    public CategoriaResponseDTO registrar(CategoriaRequestDTO request) {
        String nombre = limpiar(request.nombre());
        if (categoriaRepository.contarPorNombreNormalizado(nombre.toLowerCase()) > 0) {
            throw new ReglaNegocioException("Ya existe una categoría con el nombre '" + nombre + "'");
        }
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setDescripcion(request.descripcion());
        categoria.setEstado(request.estado() == null ? Boolean.TRUE : request.estado());
        categoria = categoriaRepository.save(categoria);
        log.info("Categoría registrada: id={}, nombre={}", categoria.getId(), categoria.getNombre());
        return toResponse(categoria);
    }

    @Override
    @Transactional
    public CategoriaResponseDTO actualizar(Long id, CategoriaRequestDTO request) {
        Categoria categoria = obtener(id);
        String nombre = limpiar(request.nombre());
        if (categoriaRepository.contarPorNombreNormalizadoEnOtra(nombre.toLowerCase(), id) > 0) {
            throw new ReglaNegocioException("Ya existe otra categoría con el nombre '" + nombre + "'");
        }
        categoria.setNombre(nombre);
        categoria.setDescripcion(request.descripcion());
        if (request.estado() != null) {
            categoria.setEstado(request.estado());
        }
        log.info("Categoría actualizada: id={}", id);
        return toResponse(categoria);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = obtener(id);
        if (equipoRepository.existsByCategoriaId(id)) {
            throw new ReglaNegocioException("No se puede eliminar la categoría '" + categoria.getNombre()
                    + "' porque tiene equipos asociados");
        }
        categoriaRepository.delete(categoria);
        log.info("Categoría eliminada: id={}", id);
    }

    private Categoria obtener(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría", id));
    }

    /** Quita espacios en los extremos y colapsa los espacios internos repetidos. */
    private String limpiar(String nombre) {
        return nombre.trim().replaceAll("\\s+", " ");
    }

    private CategoriaResponseDTO toResponse(Categoria c) {
        return new CategoriaResponseDTO(c.getId(), c.getNombre(), c.getDescripcion(), c.getEstado());
    }
}
