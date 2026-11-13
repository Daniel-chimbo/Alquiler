package pe.edu.upeu.AlquileresBackend.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.AlquileresBackend.dto.AlquilerRequestDTO;
import pe.edu.upeu.AlquileresBackend.dto.AlquilerResponseDTO;
import pe.edu.upeu.AlquileresBackend.dto.DetalleAlquilerRequestDTO;
import pe.edu.upeu.AlquileresBackend.dto.DetalleAlquilerResponseDTO;
import pe.edu.upeu.AlquileresBackend.dto.EquipoAlquiladoDTO;
import pe.edu.upeu.AlquileresBackend.entity.Alquiler;
import pe.edu.upeu.AlquileresBackend.entity.Cliente;
import pe.edu.upeu.AlquileresBackend.entity.DetalleAlquiler;
import pe.edu.upeu.AlquileresBackend.entity.Equipo;
import pe.edu.upeu.AlquileresBackend.enums.EstadoAlquiler;
import pe.edu.upeu.AlquileresBackend.exception.RecursoNoEncontradoException;
import pe.edu.upeu.AlquileresBackend.exception.ReglaNegocioException;
import pe.edu.upeu.AlquileresBackend.exception.SolicitudInvalidaException;
import pe.edu.upeu.AlquileresBackend.repository.AlquilerRepository;
import pe.edu.upeu.AlquileresBackend.repository.CategoriaRepository;
import pe.edu.upeu.AlquileresBackend.repository.ClienteRepository;
import pe.edu.upeu.AlquileresBackend.repository.EquipoRepository;
import pe.edu.upeu.AlquileresBackend.service.service.AlquilerService;

/**
 * Operación cabecera–detalle. Aquí viven las reglas RN-01 a RN-04.
 * registrar() y devolver() son @Transactional: si cualquier regla lanza una
 * ReglaNegocioException (RuntimeException), Spring hace rollback y no se guarda
 * ni la cabecera, ni los detalles, ni los cambios de unidades.
 */
@Service
public class AlquilerServiceImpl implements AlquilerService {

    private static final Logger log = LoggerFactory.getLogger(AlquilerServiceImpl.class);

    static final int MAX_DIAS_ANTICIPACION = 60;     // RN-01
    static final int MAX_ALQUILERES_VIGENTES = 2;    // RN-03
    static final int DIAS_PARA_DESCUENTO = 7;        // RN-04
    static final BigDecimal TASA_DESCUENTO = new BigDecimal("0.15");
    private static final int ESCALA = 2;
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private final AlquilerRepository alquilerRepository;
    private final ClienteRepository clienteRepository;
    private final EquipoRepository equipoRepository;
    private final CategoriaRepository categoriaRepository;

    public AlquilerServiceImpl(AlquilerRepository alquilerRepository, ClienteRepository clienteRepository,
                               EquipoRepository equipoRepository, CategoriaRepository categoriaRepository) {
        this.alquilerRepository = alquilerRepository;
        this.clienteRepository = clienteRepository;
        this.equipoRepository = equipoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Override
    @Transactional
    public AlquilerResponseDTO registrar(AlquilerRequestDTO request) {
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", request.clienteId()));

        // RN-01: cliente activo y fecha de inicio entre hoy y hoy + 60 días
        if (!Boolean.TRUE.equals(cliente.getEstado())) {
            throw new ReglaNegocioException("RN-01: el cliente " + cliente.getCodigo() + " está inactivo");
        }
        LocalDate hoy = LocalDate.now();
        if (request.fechaInicio().isBefore(hoy)) {
            throw new ReglaNegocioException("RN-01: la fecha de inicio no puede ser anterior a hoy (" + hoy + ")");
        }
        if (request.fechaInicio().isAfter(hoy.plusDays(MAX_DIAS_ANTICIPACION))) {
            throw new ReglaNegocioException("RN-01: la fecha de inicio no puede ser posterior a "
                    + MAX_DIAS_ANTICIPACION + " días desde hoy");
        }

        // RN-04: un alquiler no repite equipos
        Set<Long> equiposVistos = new HashSet<>();
        for (DetalleAlquilerRequestDTO d : request.detalles()) {
            if (!equiposVistos.add(d.equipoId())) {
                throw new ReglaNegocioException("RN-04: el equipo con id " + d.equipoId()
                        + " está repetido en el alquiler");
            }
        }

        // RN-03: máximo 2 alquileres VIGENTE por cliente (consulta agregada COUNT)
        long vigentes = alquilerRepository.contarPorClienteYEstado(cliente.getId(), EstadoAlquiler.VIGENTE);
        if (vigentes >= MAX_ALQUILERES_VIGENTES) {
            throw new ReglaNegocioException("RN-03: el cliente " + cliente.getCodigo() + " ya tiene "
                    + vigentes + " alquileres vigentes (máximo " + MAX_ALQUILERES_VIGENTES + ")");
        }

        Alquiler alquiler = new Alquiler();
        alquiler.setCliente(cliente);
        alquiler.setFechaRegistro(LocalDateTime.now());
        alquiler.setFechaInicio(request.fechaInicio());
        alquiler.setDias(request.dias());
        alquiler.setLugarEvento(request.lugarEvento() == null ? null : request.lugarEvento().trim());
        alquiler.setEstado(EstadoAlquiler.VIGENTE);

        for (DetalleAlquilerRequestDTO d : request.detalles()) {
            // Lectura con bloqueo de fila: evita que otro alquiler concurrente use las mismas unidades
            Equipo equipo = equipoRepository.findByIdParaActualizar(d.equipoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Equipo", d.equipoId()));

            // RN-01: solo equipos activos
            if (!Boolean.TRUE.equals(equipo.getEstado())) {
                throw new ReglaNegocioException("RN-01: el equipo " + equipo.getCodigo() + " está inactivo");
            }
            // RN-02: la cantidad no puede superar las unidades disponibles
            if (d.cantidad() > equipo.getUnidadesDisponibles()) {
                throw new ReglaNegocioException("RN-02: el equipo " + equipo.getCodigo() + " solo tiene "
                        + equipo.getUnidadesDisponibles() + " unidades disponibles y se solicitaron " + d.cantidad());
            }
            // RN-02: descuenta las unidades. No hace falta save(): el equipo es una entidad
            // gestionada y Hibernate detecta el cambio (dirty checking) al hacer commit.
            equipo.setUnidadesDisponibles(equipo.getUnidadesDisponibles() - d.cantidad());

            DetalleAlquiler detalle = new DetalleAlquiler();
            detalle.setEquipo(equipo);
            detalle.setCantidad(d.cantidad());
            detalle.setTarifaDiaria(equipo.getTarifaDiaria());   // copia histórica de la tarifa
            detalle.setImporte(calcularImporte(d.cantidad(), equipo.getTarifaDiaria(), request.dias()));
            alquiler.agregarDetalle(detalle);
        }

        calcularTotales(alquiler);
        alquiler = alquilerRepository.save(alquiler);   // cascade ALL inserta también los detalles

        log.info("Alquiler registrado: id={}, cliente={}, unidades={}, total={}",
                alquiler.getId(), cliente.getCodigo(), alquiler.getTotalUnidades(), alquiler.getMontoTotal());
        return toResponse(alquiler);
    }

    @Override
    @Transactional(readOnly = true)
    public AlquilerResponseDTO buscarPorId(Long id) {
        return toResponse(obtenerConDetalles(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlquilerResponseDTO> listar() {
        return alquilerRepository.listarConDetalles().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlquilerResponseDTO> listarPorCliente(Long clienteId, EstadoAlquiler estado) {
        // SC-A: un cliente inexistente responde 404 (no una lista vacía)
        if (!clienteRepository.existsById(clienteId)) {
            throw new RecursoNoEncontradoException("Cliente", clienteId);
        }
        List<AlquilerResponseDTO> alquileres = alquilerRepository.listarPorCliente(clienteId, estado)
                .stream().map(this::toResponse).toList();
        log.info("Alquileres del cliente: clienteId={}, estado={}, filas={}", clienteId, estado, alquileres.size());
        return alquileres;
    }

    @Override
    @Transactional
    public AlquilerResponseDTO devolver(Long id) {
        Alquiler alquiler = obtenerConDetalles(id);

        // RN-02: la devolución solo procede sobre un alquiler VIGENTE
        if (alquiler.getEstado() != EstadoAlquiler.VIGENTE) {
            throw new ReglaNegocioException("RN-02: el alquiler " + id + " ya fue devuelto el "
                    + alquiler.getFechaDevolucion());
        }
        // RN-02: repone las unidades de cada equipo
        for (DetalleAlquiler detalle : alquiler.getDetalles()) {
            Equipo equipo = detalle.getEquipo();
            equipo.setUnidadesDisponibles(equipo.getUnidadesDisponibles() + detalle.getCantidad());
        }
        alquiler.setEstado(EstadoAlquiler.DEVUELTO);
        alquiler.setFechaDevolucion(LocalDateTime.now());

        log.info("Alquiler devuelto: id={}, unidades repuestas={}", id, alquiler.getTotalUnidades());
        return toResponse(alquiler);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoAlquiladoDTO> reporteEquiposAlquilados(String periodo, Long categoriaId) {
        YearMonth mes;
        try {
            mes = YearMonth.parse(periodo);
        } catch (DateTimeParseException | NullPointerException ex) {
            throw new SolicitudInvalidaException("El periodo debe tener el formato AAAA-MM, por ejemplo 2026-11");
        }
        if (categoriaId != null && !categoriaRepository.existsById(categoriaId)) {
            throw new RecursoNoEncontradoException("Categoría", categoriaId);
        }
        LocalDateTime desde = mes.atDay(1).atStartOfDay();
        LocalDateTime hasta = mes.plusMonths(1).atDay(1).atStartOfDay();
        List<EquipoAlquiladoDTO> filas = alquilerRepository.reporteEquiposAlquilados(desde, hasta, categoriaId);
        log.info("Reporte equipos-alquilados: periodo={}, categoriaId={}, filas={}", periodo, categoriaId, filas.size());
        return filas;
    }

    // ---------------------------------------------------------------- cálculos (RN-04)

    /** importe = cantidad × tarifa diaria × días, con escala 2 y HALF_UP. */
    static BigDecimal calcularImporte(int cantidad, BigDecimal tarifaDiaria, int dias) {
        return tarifaDiaria
                .multiply(BigDecimal.valueOf(cantidad))
                .multiply(BigDecimal.valueOf(dias))
                .setScale(ESCALA, REDONDEO);
    }

    /** subtotal = Σ importes; 15 % de descuento si dias ≥ 7; monto = subtotal − descuento. */
    static void calcularTotales(Alquiler alquiler) {
        BigDecimal subtotal = BigDecimal.ZERO;
        int unidades = 0;
        for (DetalleAlquiler d : alquiler.getDetalles()) {
            subtotal = subtotal.add(d.getImporte());
            unidades += d.getCantidad();
        }
        subtotal = subtotal.setScale(ESCALA, REDONDEO);
        BigDecimal descuento = alquiler.getDias() >= DIAS_PARA_DESCUENTO
                ? subtotal.multiply(TASA_DESCUENTO).setScale(ESCALA, REDONDEO)
                : BigDecimal.ZERO.setScale(ESCALA, REDONDEO);

        alquiler.setTotalUnidades(unidades);
        alquiler.setSubtotal(subtotal);
        alquiler.setDescuento(descuento);
        alquiler.setMontoTotal(subtotal.subtract(descuento).setScale(ESCALA, REDONDEO));
    }

    // ---------------------------------------------------------------- utilidades

    private Alquiler obtenerConDetalles(Long id) {
        return alquilerRepository.buscarConDetalles(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alquiler", id));
    }

    private AlquilerResponseDTO toResponse(Alquiler a) {
        List<DetalleAlquilerResponseDTO> detalles = a.getDetalles().stream()
                .map(d -> new DetalleAlquilerResponseDTO(d.getId(), d.getEquipo().getId(),
                        d.getEquipo().getCodigo(), d.getEquipo().getNombre(),
                        d.getCantidad(), d.getTarifaDiaria(), d.getImporte()))
                .toList();
        Cliente c = a.getCliente();
        return new AlquilerResponseDTO(a.getId(), a.getFechaRegistro(), a.getFechaInicio(), a.getDias(),
                c.getId(), c.getCodigo(), c.getNombre(), a.getLugarEvento(), a.getEstado(),
                a.getFechaDevolucion(), a.getTotalUnidades(), a.getSubtotal(), a.getDescuento(),
                a.getMontoTotal(), detalles);
    }
}
