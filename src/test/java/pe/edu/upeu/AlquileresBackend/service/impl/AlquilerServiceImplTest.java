package pe.edu.upeu.AlquileresBackend.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.AlquileresBackend.dto.AlquilerRequestDTO;
import pe.edu.upeu.AlquileresBackend.dto.AlquilerResponseDTO;
import pe.edu.upeu.AlquileresBackend.dto.DetalleAlquilerRequestDTO;
import pe.edu.upeu.AlquileresBackend.dto.IngresoCategoriaDTO;
import pe.edu.upeu.AlquileresBackend.entity.Alquiler;
import pe.edu.upeu.AlquileresBackend.entity.Cliente;
import pe.edu.upeu.AlquileresBackend.entity.DetalleAlquiler;
import pe.edu.upeu.AlquileresBackend.entity.Equipo;
import pe.edu.upeu.AlquileresBackend.enums.EstadoAlquiler;
import pe.edu.upeu.AlquileresBackend.enums.TipoCliente;
import pe.edu.upeu.AlquileresBackend.exception.RecursoNoEncontradoException;
import pe.edu.upeu.AlquileresBackend.exception.ReglaNegocioException;
import pe.edu.upeu.AlquileresBackend.exception.SolicitudInvalidaException;
import pe.edu.upeu.AlquileresBackend.repository.AlquilerRepository;
import pe.edu.upeu.AlquileresBackend.repository.CategoriaRepository;
import pe.edu.upeu.AlquileresBackend.repository.ClienteRepository;
import pe.edu.upeu.AlquileresBackend.repository.EquipoRepository;

/** Pruebas unitarias de las reglas RN-01 a RN-04 sin base de datos (repositorios simulados). */
@ExtendWith(MockitoExtension.class)
class AlquilerServiceImplTest {

    @Mock
    private AlquilerRepository alquilerRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private EquipoRepository equipoRepository;
    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private AlquilerServiceImpl service;

    private Cliente cliente;
    private LocalDate fechaF;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setCodigo("CLI-001");
        cliente.setNombre("Eventos Misti SAC");
        cliente.setTipo(TipoCliente.EMPRESA);
        cliente.setEstado(true);
        fechaF = LocalDate.now().plusDays(7);
    }

    private Equipo equipo(long id, String codigo, String tarifa, int unidades) {
        Equipo e = new Equipo();
        e.setId(id);
        e.setCodigo(codigo);
        e.setNombre("Equipo " + codigo);
        e.setTarifaDiaria(new BigDecimal(tarifa));
        e.setUnidadesDisponibles(unidades);
        e.setEstado(true);
        return e;
    }

    private void clienteSinVigentes() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(alquilerRepository.contarPorClienteYEstado(1L, EstadoAlquiler.VIGENTE)).thenReturn(0L);
    }

    @Test
    @DisplayName("CP-06: importes por días, subtotal 2190.00 sin descuento y unidades descontadas")
    void registraAlquilerYCalculaImportes() {
        clienteSinVigentes();
        Equipo eqp001 = equipo(1, "EQP-001", "80.00", 10);
        Equipo eqp003 = equipo(3, "EQP-003", "35.00", 20);
        Equipo eqp009 = equipo(9, "EQP-009", "25.00", 16);
        when(equipoRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(eqp001));
        when(equipoRepository.findByIdParaActualizar(3L)).thenReturn(Optional.of(eqp003));
        when(equipoRepository.findByIdParaActualizar(9L)).thenReturn(Optional.of(eqp009));
        when(alquilerRepository.save(any(Alquiler.class))).thenAnswer(inv -> inv.getArgument(0));

        AlquilerResponseDTO r = service.registrar(new AlquilerRequestDTO(1L, fechaF, 3, "Cerro Juli", List.of(
                new DetalleAlquilerRequestDTO(1L, 4),
                new DetalleAlquilerRequestDTO(3L, 6),
                new DetalleAlquilerRequestDTO(9L, 8))));

        assertEquals(18, r.totalUnidades());
        assertEquals(new BigDecimal("2190.00"), r.subtotal());
        assertEquals(new BigDecimal("0.00"), r.descuento());
        assertEquals(new BigDecimal("2190.00"), r.montoTotal());
        assertEquals(new BigDecimal("960.00"), r.detalles().get(0).importe());
        assertEquals(6, eqp001.getUnidadesDisponibles());
        assertEquals(14, eqp003.getUnidadesDisponibles());
        assertEquals(8, eqp009.getUnidadesDisponibles());
        assertEquals(EstadoAlquiler.VIGENTE, r.estado());
    }

    @Test
    @DisplayName("CP-10 / RN-04: 7 días o más aplica 15 % de descuento (1680.00 - 252.00 = 1428.00)")
    void aplicaDescuentoSemanal() {
        clienteSinVigentes();
        Equipo eqp002 = equipo(2, "EQP-002", "120.00", 4);
        when(equipoRepository.findByIdParaActualizar(2L)).thenReturn(Optional.of(eqp002));
        when(alquilerRepository.save(any(Alquiler.class))).thenAnswer(inv -> inv.getArgument(0));

        AlquilerResponseDTO r = service.registrar(new AlquilerRequestDTO(1L, fechaF, 7, null,
                List.of(new DetalleAlquilerRequestDTO(2L, 2))));

        assertEquals(new BigDecimal("1680.00"), r.subtotal());
        assertEquals(new BigDecimal("252.00"), r.descuento());
        assertEquals(new BigDecimal("1428.00"), r.montoTotal());
        assertEquals(2, eqp002.getUnidadesDisponibles());
    }

    @Test
    @DisplayName("RN-01: cliente inactivo → ReglaNegocioException y no se guarda nada")
    void rechazaClienteInactivo() {
        cliente.setEstado(false);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        assertThrows(ReglaNegocioException.class, () -> service.registrar(new AlquilerRequestDTO(
                1L, fechaF, 2, null, List.of(new DetalleAlquilerRequestDTO(1L, 1)))));
        verify(alquilerRepository, never()).save(any());
    }

    @Test
    @DisplayName("RN-01: fecha de inicio de ayer → ReglaNegocioException")
    void rechazaFechaPasada() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> service.registrar(
                new AlquilerRequestDTO(1L, LocalDate.now().minusDays(1), 2, null,
                        List.of(new DetalleAlquilerRequestDTO(1L, 1)))));
        assertTrue(ex.getMessage().startsWith("RN-01"));
    }

    @Test
    @DisplayName("RN-02: cantidad mayor a las unidades disponibles → ReglaNegocioException sin guardar")
    void rechazaStockInsuficiente() {
        clienteSinVigentes();
        when(equipoRepository.findByIdParaActualizar(7L)).thenReturn(Optional.of(equipo(7, "EQP-007", "90.00", 8)));
        when(equipoRepository.findByIdParaActualizar(4L)).thenReturn(Optional.of(equipo(4, "EQP-004", "150.00", 0)));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> service.registrar(
                new AlquilerRequestDTO(1L, fechaF, 2, null, List.of(
                        new DetalleAlquilerRequestDTO(7L, 2),
                        new DetalleAlquilerRequestDTO(4L, 1)))));
        assertTrue(ex.getMessage().startsWith("RN-02"));
        verify(alquilerRepository, never()).save(any());
    }

    @Test
    @DisplayName("RN-03: cliente con 2 alquileres vigentes → ReglaNegocioException")
    void rechazaMasDeDosVigentes() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(alquilerRepository.contarPorClienteYEstado(1L, EstadoAlquiler.VIGENTE)).thenReturn(2L);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> service.registrar(
                new AlquilerRequestDTO(1L, fechaF, 1, null, List.of(new DetalleAlquilerRequestDTO(6L, 1)))));
        assertTrue(ex.getMessage().startsWith("RN-03"));
        verify(equipoRepository, never()).findByIdParaActualizar(any());
    }

    @Test
    @DisplayName("RN-04: equipo repetido en dos líneas → ReglaNegocioException")
    void rechazaEquipoRepetido() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> service.registrar(
                new AlquilerRequestDTO(1L, fechaF, 2, null, List.of(
                        new DetalleAlquilerRequestDTO(7L, 1),
                        new DetalleAlquilerRequestDTO(7L, 2)))));
        assertTrue(ex.getMessage().startsWith("RN-04"));
    }

    @Test
    @DisplayName("SC-A: cliente inexistente → 404 sin consultar sus alquileres")
    void alquileresDeClienteInexistente() {
        when(clienteRepository.existsById(99L)).thenReturn(false);

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.listarPorCliente(99L, EstadoAlquiler.VIGENTE));
        verify(alquilerRepository, never()).listarPorCliente(any(), any());
    }

    @Test
    @DisplayName("SC-A: devuelve los alquileres del cliente en el orden del repositorio y con el filtro de estado")
    void alquileresDeClienteConFiltro() {
        Alquiler reciente = new Alquiler();
        reciente.setId(2L);
        reciente.setCliente(cliente);
        reciente.setFechaInicio(fechaF.plusDays(5));
        reciente.setDias(1);
        reciente.setEstado(EstadoAlquiler.VIGENTE);
        AlquilerServiceImpl.calcularTotales(reciente);
        when(clienteRepository.existsById(1L)).thenReturn(true);
        when(alquilerRepository.listarPorCliente(1L, EstadoAlquiler.VIGENTE)).thenReturn(List.of(reciente));

        List<AlquilerResponseDTO> r = service.listarPorCliente(1L, EstadoAlquiler.VIGENTE);

        assertEquals(1, r.size());
        assertEquals(2L, r.get(0).id());
        assertEquals("CLI-001", r.get(0).clienteCodigo());
    }

    private Alquiler alquilerVigente(int dias, Equipo... equiposYCantidad) {
        Alquiler a = new Alquiler();
        a.setId(1L);
        a.setCliente(cliente);
        a.setDias(dias);
        a.setEstado(EstadoAlquiler.VIGENTE);
        for (Equipo e : equiposYCantidad) {
            DetalleAlquiler d = new DetalleAlquiler();
            d.setEquipo(e);
            d.setCantidad(e.getId() == 2L ? 2 : 1);
            d.setTarifaDiaria(e.getTarifaDiaria());
            d.setImporte(AlquilerServiceImpl.calcularImporte(d.getCantidad(), e.getTarifaDiaria(), dias));
            a.agregarDetalle(d);
        }
        AlquilerServiceImpl.calcularTotales(a);
        return a;
    }

    @Test
    @DisplayName("SC-B: retirar un equipo repone unidades y recalcula subtotal, descuento y monto")
    void retirarEquipoRecalculaTotales() {
        Equipo eqp002 = equipo(2, "EQP-002", "120.00", 2);
        Equipo eqp007 = equipo(7, "EQP-007", "90.00", 7);
        Alquiler a = alquilerVigente(7, eqp002, eqp007);
        assertEquals(new BigDecimal("1963.50"), a.getMontoTotal());
        when(alquilerRepository.buscarConDetalles(1L)).thenReturn(Optional.of(a));
        when(equipoRepository.findByIdParaActualizar(7L)).thenReturn(Optional.of(eqp007));

        AlquilerResponseDTO r = service.retirarEquipo(1L, 7L);

        assertEquals(1, r.detalles().size());
        assertEquals(2, r.totalUnidades());
        assertEquals(new BigDecimal("1680.00"), r.subtotal());
        assertEquals(new BigDecimal("252.00"), r.descuento());
        assertEquals(new BigDecimal("1428.00"), r.montoTotal());
        assertEquals(8, eqp007.getUnidadesDisponibles());
    }

    @Test
    @DisplayName("SC-B: no se puede dejar un alquiler sin equipos → 409 sin reponer unidades")
    void retirarUltimoEquipoDevuelve409() {
        Equipo eqp002 = equipo(2, "EQP-002", "120.00", 2);
        when(alquilerRepository.buscarConDetalles(1L)).thenReturn(Optional.of(alquilerVigente(7, eqp002)));

        assertThrows(ReglaNegocioException.class, () -> service.retirarEquipo(1L, 2L));
        assertEquals(2, eqp002.getUnidadesDisponibles());
    }

    @Test
    @DisplayName("SC-B: equipo que no figura en el alquiler → 404")
    void retirarEquipoQueNoFigura() {
        when(alquilerRepository.buscarConDetalles(1L))
                .thenReturn(Optional.of(alquilerVigente(2, equipo(2, "EQP-002", "120.00", 2))));

        assertThrows(RecursoNoEncontradoException.class, () -> service.retirarEquipo(1L, 9L));
    }

    @Test
    @DisplayName("SC-C: periodo sin alquileres → lista vacía, con el rango [inicio del mes, inicio del mes siguiente)")
    void ingresosPorCategoriaSinDatos() {
        LocalDateTime desde = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 2, 1, 0, 0);
        when(alquilerRepository.reporteIngresosPorCategoria(desde, hasta)).thenReturn(List.of());

        List<IngresoCategoriaDTO> r = service.reporteIngresosPorCategoria("2026-01");

        assertTrue(r.isEmpty());
        verify(alquilerRepository).reporteIngresosPorCategoria(desde, hasta);
    }

    @Test
    @DisplayName("SC-C: periodo con formato inválido → 400 sin consultar la base")
    void ingresosPorCategoriaPeriodoInvalido() {
        assertThrows(SolicitudInvalidaException.class, () -> service.reporteIngresosPorCategoria("11-2026"));
        verify(alquilerRepository, never()).reporteIngresosPorCategoria(any(), any());
    }

    @Test
    @DisplayName("RN-02: devolver repone unidades y devolver dos veces → ReglaNegocioException")
    void devolverReponeUnidadesYNoPermiteRepetir() {
        Equipo eqp001 = equipo(1, "EQP-001", "80.00", 6);
        Alquiler alquiler = new Alquiler();
        alquiler.setId(1L);
        alquiler.setCliente(cliente);
        alquiler.setDias(3);
        alquiler.setEstado(EstadoAlquiler.VIGENTE);
        DetalleAlquiler d = new DetalleAlquiler();
        d.setEquipo(eqp001);
        d.setCantidad(4);
        d.setTarifaDiaria(new BigDecimal("80.00"));
        d.setImporte(new BigDecimal("960.00"));
        alquiler.agregarDetalle(d);
        AlquilerServiceImpl.calcularTotales(alquiler);
        when(alquilerRepository.buscarConDetalles(1L)).thenReturn(Optional.of(alquiler));

        AlquilerResponseDTO r = service.devolver(1L);

        assertEquals(EstadoAlquiler.DEVUELTO, r.estado());
        assertEquals(10, eqp001.getUnidadesDisponibles());
        assertThrows(ReglaNegocioException.class, () -> service.devolver(1L));
        assertEquals(10, eqp001.getUnidadesDisponibles());
    }
}
