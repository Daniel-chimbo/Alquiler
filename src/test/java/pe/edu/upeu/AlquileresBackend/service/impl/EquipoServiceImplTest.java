package pe.edu.upeu.AlquileresBackend.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.AlquileresBackend.dto.EquipoRequestDTO;
import pe.edu.upeu.AlquileresBackend.entity.Equipo;
import pe.edu.upeu.AlquileresBackend.exception.RecursoNoEncontradoException;
import pe.edu.upeu.AlquileresBackend.exception.ReglaNegocioException;
import pe.edu.upeu.AlquileresBackend.exception.SolicitudInvalidaException;
import pe.edu.upeu.AlquileresBackend.repository.AlquilerRepository;
import pe.edu.upeu.AlquileresBackend.repository.CategoriaRepository;
import pe.edu.upeu.AlquileresBackend.repository.EquipoRepository;

@ExtendWith(MockitoExtension.class)
class EquipoServiceImplTest {

    @Mock
    private EquipoRepository equipoRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private AlquilerRepository alquilerRepository;

    @InjectMocks
    private EquipoServiceImpl service;

    @Test
    @DisplayName("CP-04: categoría inexistente → 404 sin insertar")
    void registrarConCategoriaInexistente() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.registrar(new EquipoRequestDTO(
                "EQP-099", "Equipo de prueba", new BigDecimal("10.00"), 1, true, null, 99L)));
        verify(equipoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Eliminar un equipo con alquileres → 409")
    void eliminarEquipoConAlquileres() {
        Equipo e = new Equipo();
        e.setId(1L);
        e.setCodigo("EQP-001");
        when(equipoRepository.findById(1L)).thenReturn(Optional.of(e));
        when(alquilerRepository.contarDetallesConEquipo(1L)).thenReturn(3L);

        assertThrows(ReglaNegocioException.class, () -> service.eliminar(1L));
        verify(equipoRepository, never()).delete(any(Equipo.class));
    }

    @Test
    @DisplayName("Búsqueda: campo de orden no permitido → 400")
    void buscarConOrdenInvalido() {
        assertThrows(SolicitudInvalidaException.class,
                () -> service.buscar(null, null, null, null, null, "precio; drop table", "asc"));
    }
}
