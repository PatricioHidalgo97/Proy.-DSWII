package com.cibertec.gestion.cash.services;

import com.cibertec.gestion.cash.clients.CatalogoClient;
import com.cibertec.gestion.cash.clients.dto.BancoRemoto;
import com.cibertec.gestion.cash.clients.dto.PuestoRemoto;
import com.cibertec.gestion.cash.clients.dto.ServicioRemoto;
import com.cibertec.gestion.cash.clients.dto.SocioRemoto;
import com.cibertec.gestion.cash.dto.CanjeRequest;
import com.cibertec.gestion.cash.dto.CuentaRequest;
import com.cibertec.gestion.cash.dto.GenerarCuentasRequest;
import com.cibertec.gestion.cash.entities.CuentaCobrarEntity;
import com.cibertec.gestion.cash.entities.ReciboEntity;
import com.cibertec.gestion.cash.enums.EstadoCuenta;
import com.cibertec.gestion.cash.enums.TipoRecibo;
import com.cibertec.gestion.cash.exceptions.BusinessException;
import com.cibertec.gestion.cash.exceptions.NotFoundException;
import com.cibertec.gestion.cash.mappers.CuentaCobrarMapper;
import com.cibertec.gestion.cash.mappers.ReciboMapper;
import com.cibertec.gestion.cash.repositories.CuentaCobrarRepository;
import com.cibertec.gestion.cash.repositories.ReciboRepository;
import com.cibertec.gestion.cash.security.UsuarioPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Reglas de cobranza portadas del monolito sin cambios (ESP-11 §5.1). Lo único
 * que cambia es la fuente de los datos ajenos: {@link CatalogoClient} en lugar
 * de repositorios de catálogo, y {@link UsuarioPrincipal} en lugar de la tabla
 * de usuarios (ESP-03 §6, §7).
 */
@Service
@RequiredArgsConstructor
public class CuentaCobrarService implements ICuentaCobrarService {

    private static final String CARGO_PUESTO = "PUESTO";
    private static final String CARGO_SOCIO = "SOCIO";
    private static final String TIPO_CORRELATIVO = "RECIBO";
    private static final String PREFIJO_CORRELATIVO = "REC";

    private final CuentaCobrarRepository cuentaRepository;
    private final ReciboRepository reciboRepository;
    private final CatalogoClient catalogoClient;
    private final CorrelativoService correlativoService;

    @Override
    public List<CuentaCobrarMapper.CuentaCobrarResponse> getAll() {
        return CuentaCobrarMapper.toResponse(cuentaRepository.findAll());
    }

    /**
     * Listado por puesto. No cruza la frontera con catálogo (ESP-03 §6, §8):
     * los listados históricos deben seguir funcionando con el catálogo caído.
     * Si el puesto no existe, la lista es vacía.
     */
    @Override
    public List<CuentaCobrarMapper.CuentaCobrarResponse> getByPuesto(Long puestoId) {
        return CuentaCobrarMapper.toResponse(
                cuentaRepository.findByPuestoIdOrderByFechaCreacionDesc(puestoId));
    }

    @Override
    public List<CuentaCobrarMapper.CuentaCobrarResponse> getBySocio(Long socioId) {
        return CuentaCobrarMapper.toResponse(
                cuentaRepository.findBySocioIdOrderByFechaCreacionDesc(socioId));
    }

    @Override
    @Transactional
    public CuentaCobrarMapper.CuentaCobrarResponse create(CuentaRequest request) {
        ServicioRemoto servicio = getServicio(request.getServicioId());
        boolean tienePuesto = request.getPuestoId() != null;
        boolean tieneSocio = request.getSocioId() != null;
        if (tienePuesto == tieneSocio)
            throw new BusinessException("Seleccione un puesto o un socio, pero no ambos");

        PuestoRemoto puesto = null;
        SocioRemoto socio = null;
        if (tienePuesto) {
            if (!CARGO_PUESTO.equals(servicio.cargoA()))
                throw new BusinessException("El servicio seleccionado no está configurado para puestos");
            puesto = catalogoClient.getPuesto(request.getPuestoId());
            validarDuplicado(request.getPeriodo(), servicio.id(), puesto.id(), null);
        } else {
            if (!CARGO_SOCIO.equals(servicio.cargoA()))
                throw new BusinessException("El servicio seleccionado no está configurado para socios");
            socio = catalogoClient.getSocio(request.getSocioId());
            validarDuplicado(request.getPeriodo(), servicio.id(), null, socio.id());
        }

        BigDecimal monto = calcularMonto(servicio, request.getMonto(), request.getLecturaInicial(),
                request.getLecturaFinal(), request.getCostoUnitario());
        return CuentaCobrarMapper.toResponse(cuentaRepository.save(
                nuevaCuenta(request.getPeriodo(), monto, puesto, socio, servicio,
                        request.getLecturaInicial(), request.getLecturaFinal(), request.getCostoUnitario())));
    }

    @Override
    @Transactional
    public List<CuentaCobrarMapper.CuentaCobrarResponse> generateForPuestos(GenerarCuentasRequest request) {
        ServicioRemoto servicio = getServicio(request.getServicioId());
        if (!CARGO_PUESTO.equals(servicio.cargoA()))
            throw new BusinessException("El servicio debe estar configurado para puestos");
        validarServicioMasivo(servicio);

        List<Long> ids = request.getPuestoIds();
        if (ids == null || ids.isEmpty())
            ids = catalogoClient.listPuestos().stream()
                    .filter(PuestoRemoto::activo)
                    .map(PuestoRemoto::id)
                    .toList();
        if (ids.isEmpty())
            throw new BusinessException("No hay puestos seleccionados para generar cuentas");

        BigDecimal monto = calcularMonto(servicio, request.getMonto(), null, null, null);
        List<CuentaCobrarEntity> creadas = new ArrayList<>();
        for (Long id : new LinkedHashSet<>(ids)) {
            PuestoRemoto puesto = catalogoClient.getPuesto(id);
            if (!cuentaRepository.existsByPeriodoAndServicioIdAndPuestoId(
                    request.getPeriodo(), servicio.id(), id)) {
                creadas.add(cuentaRepository.save(nuevaCuenta(request.getPeriodo(), monto, puesto, null,
                        servicio, null, null, null)));
            }
        }
        validarCreadas(creadas);
        return CuentaCobrarMapper.toResponse(creadas);
    }

    @Override
    @Transactional
    public List<CuentaCobrarMapper.CuentaCobrarResponse> generateForSocios(GenerarCuentasRequest request) {
        ServicioRemoto servicio = getServicio(request.getServicioId());
        if (!CARGO_SOCIO.equals(servicio.cargoA()))
            throw new BusinessException("El servicio debe estar configurado para socios");
        validarServicioMasivo(servicio);

        List<SocioRemoto> socios = new ArrayList<>();
        if (request.getSocioIds() != null && !request.getSocioIds().isEmpty()) {
            for (Long id : new LinkedHashSet<>(request.getSocioIds()))
                socios.add(catalogoClient.getSocio(id));
        } else {
            socios.addAll(catalogoClient.listSocios().stream()
                    .filter(SocioRemoto::activo)
                    .filter(s -> request.getEtapa() == null || request.getEtapa().equals(s.etapa()))
                    .toList());
        }
        if (socios.isEmpty())
            throw new BusinessException("No hay socios seleccionados para generar cuentas");
        if (request.isSociosUnicos()) {
            Set<String> nombres = new HashSet<>();
            socios = socios.stream().filter(s -> nombres.add(
                    (s.nombres() + " " + s.apellidos()).trim().toLowerCase(Locale.ROOT))).toList();
        }

        BigDecimal monto = calcularMonto(servicio, request.getMonto(), null, null, null);
        List<CuentaCobrarEntity> creadas = new ArrayList<>();
        for (SocioRemoto socio : socios) {
            if (!cuentaRepository.existsByPeriodoAndServicioIdAndSocioId(
                    request.getPeriodo(), servicio.id(), socio.id())) {
                creadas.add(cuentaRepository.save(nuevaCuenta(request.getPeriodo(), monto, null, socio,
                        servicio, null, null, null)));
            }
        }
        validarCreadas(creadas);
        return CuentaCobrarMapper.toResponse(creadas);
    }

    @Override
    @Transactional
    public CuentaCobrarMapper.CuentaCobrarResponse exonerate(Long id) {
        CuentaCobrarEntity cuenta = getPendingAccount(id);
        cuenta.setEstado(EstadoCuenta.EXONERADA);
        return CuentaCobrarMapper.toResponse(cuentaRepository.save(cuenta));
    }

    @Override
    @Transactional
    public List<CuentaCobrarMapper.CuentaCobrarResponse> exonerateBatch(List<Long> ids) {
        validarLote(ids);
        List<CuentaCobrarEntity> cuentas = ids.stream().distinct().map(this::getPendingAccount).toList();
        cuentas.forEach(c -> c.setEstado(EstadoCuenta.EXONERADA));
        return CuentaCobrarMapper.toResponse(cuentaRepository.saveAll(cuentas));
    }

    @Override
    @Transactional
    public ReciboMapper.ReciboResponse pay(Long id, UsuarioPrincipal usuario) {
        return ReciboMapper.toResponse(
                registrarPago(getPendingAccount(id), usuario, TipoRecibo.INGRESO, null, null));
    }

    @Override
    @Transactional
    public List<ReciboMapper.ReciboResponse> payBatch(List<Long> ids, UsuarioPrincipal usuario) {
        validarLote(ids);
        validarUsuario(usuario);
        List<CuentaCobrarEntity> cuentas = ids.stream().distinct().map(this::getPendingAccount).toList();
        List<ReciboEntity> recibos = new ArrayList<>();
        for (CuentaCobrarEntity cuenta : cuentas)
            recibos.add(registrarPago(cuenta, usuario, TipoRecibo.INGRESO, null, null));
        return recibos.stream().map(ReciboMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public ReciboMapper.ReciboResponse exchange(CanjeRequest request, UsuarioPrincipal usuario) {
        CuentaCobrarEntity cuenta = getPendingAccount(request.getCuentaId());
        if (cuenta.getSocioId() == null)
            throw new BusinessException("El canje bancario corresponde a cuentas asignadas a socios");
        BancoRemoto banco = catalogoClient.getBanco(request.getBancoId());
        if (!banco.activo()) throw new BusinessException("El banco seleccionado está inactivo");
        return ReciboMapper.toResponse(
                registrarPago(cuenta, usuario, TipoRecibo.BANCARIO, banco, request.getFechaDeposito()));
    }

    private ReciboEntity registrarPago(CuentaCobrarEntity cuenta, UsuarioPrincipal usuario,
                                       TipoRecibo tipo, BancoRemoto banco, LocalDate fechaDeposito) {
        validarUsuario(usuario);
        cuenta.setEstado(EstadoCuenta.PAGADA);
        cuentaRepository.save(cuenta);
        return reciboRepository.save(ReciboEntity.builder()
                .correlativo(correlativoService.next(TIPO_CORRELATIVO, PREFIJO_CORRELATIVO))
                .fecha(LocalDateTime.now())
                .monto(cuenta.getMonto())
                .tipo(tipo)
                .cuentaCobrar(cuenta)
                .bancoId(banco == null ? null : banco.id())
                .bancoNombre(banco == null ? null : banco.nombre())
                .bancoNumeroCuenta(banco == null ? null : banco.numeroCuenta())
                .bancoCci(banco == null ? null : banco.cci())
                .bancoMoneda(banco == null ? null : banco.moneda())
                .fechaDeposito(fechaDeposito)
                .concepto(tipo == TipoRecibo.BANCARIO ? "Canje bancario académico" : "Pago de cuenta")
                .usuarioId(usuario.id())
                .usuarioUsuario(usuario.usuario())
                .usuarioNombres(usuario.nombres())
                .usuarioApellidos(usuario.apellidos())
                .usuarioRol(usuario.rol())
                .build());
    }

    private CuentaCobrarEntity getPendingAccount(Long id) {
        CuentaCobrarEntity cuenta = cuentaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Cuenta por cobrar no encontrada"));
        if (cuenta.getEstado() != EstadoCuenta.PENDIENTE)
            throw new BusinessException("La cuenta ya fue pagada o exonerada");
        return cuenta;
    }

    private ServicioRemoto getServicio(Long id) {
        ServicioRemoto servicio = catalogoClient.getServicio(id);
        if (!servicio.activo()) throw new BusinessException("El servicio seleccionado está inactivo");
        return servicio;
    }

    /**
     * El snapshot del usuario es obligatorio en la base ({@code NOT NULL}) y
     * CA-04 exige que nunca quede vacío (ESP-03 §4.1).
     */
    private void validarUsuario(UsuarioPrincipal usuario) {
        if (usuario == null || usuario.id() == null
                || isBlank(usuario.usuario()) || isBlank(usuario.nombres())
                || isBlank(usuario.apellidos()) || isBlank(usuario.rol())) {
            throw new BusinessException(
                    "El token no contiene los datos del usuario necesarios para emitir el recibo");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private BigDecimal calcularMonto(ServicioRemoto servicio, BigDecimal montoSolicitado,
                                     BigDecimal lecturaInicial, BigDecimal lecturaFinal,
                                     BigDecimal costoUnitario) {
        if (!servicio.dependeConsumo()) {
            BigDecimal referencia = montoSolicitado != null ? montoSolicitado : servicio.costo();
            return referencia.setScale(2, RoundingMode.HALF_UP);
        }
        if (lecturaInicial == null || lecturaFinal == null || costoUnitario == null)
            throw new BusinessException("Ingrese lectura inicial, lectura final y costo unitario");
        if (lecturaFinal.compareTo(lecturaInicial) < 0)
            throw new BusinessException("La lectura final no puede ser menor que la lectura inicial");
        BigDecimal calculado = lecturaFinal.subtract(lecturaInicial).multiply(costoUnitario)
                .setScale(2, RoundingMode.HALF_UP);
        if (calculado.compareTo(BigDecimal.ZERO) <= 0)
            throw new BusinessException("El consumo calculado debe generar un monto mayor a cero");
        return calculado;
    }

    private CuentaCobrarEntity nuevaCuenta(String periodo, BigDecimal monto, PuestoRemoto puesto,
                                           SocioRemoto socio, ServicioRemoto servicio,
                                           BigDecimal inicial, BigDecimal fin, BigDecimal costo) {
        CuentaCobrarEntity.CuentaCobrarEntityBuilder builder = CuentaCobrarEntity.builder()
                .periodo(periodo)
                .monto(monto)
                .estado(EstadoCuenta.PENDIENTE)
                .fechaCreacion(LocalDateTime.now())
                .puestoId(puesto == null ? null : puesto.id())
                .socioId(socio == null ? null : socio.id())
                .servicioId(servicio.id())
                .lecturaInicial(inicial)
                .lecturaFinal(fin)
                .costoUnitario(costo)
                // snapshot del servicio (ESP-03 §4.4)
                .servicioNombre(servicio.nombre())
                .servicioMoneda(servicio.moneda())
                .servicioCargoA(servicio.cargoA())
                .servicioDependeConsumo(servicio.dependeConsumo())
                // snapshot del socio destinatario (ESP-03 §4.2)
                .socioCodigo(socio == null ? null : socio.codigo())
                .socioNombres(socio == null ? null : socio.nombres())
                .socioApellidos(socio == null ? null : socio.apellidos());
        if (puesto != null) {
            PuestoRemoto.SocioAnidado socioPuesto = puesto.socio();
            builder.puestoNumero(puesto.numero())
                    .puestoNombreInquilino(puesto.nombreInquilino())
                    .puestoGiroNombre(puesto.giro() == null ? null : puesto.giro().nombre())
                    .puestoSocioCodigo(socioPuesto == null ? null : socioPuesto.codigo())
                    .puestoSocioNombres(socioPuesto == null ? null : socioPuesto.nombres())
                    .puestoSocioApellidos(socioPuesto == null ? null : socioPuesto.apellidos());
        }
        return builder.build();
    }

    private void validarDuplicado(String periodo, Long servicioId, Long puestoId, Long socioId) {
        boolean existe = puestoId != null
                ? cuentaRepository.existsByPeriodoAndServicioIdAndPuestoId(periodo, servicioId, puestoId)
                : cuentaRepository.existsByPeriodoAndServicioIdAndSocioId(periodo, servicioId, socioId);
        if (existe)
            throw new BusinessException("Ya existe una cuenta para ese período, servicio y destinatario");
    }

    private void validarServicioMasivo(ServicioRemoto servicio) {
        if (servicio.dependeConsumo())
            throw new BusinessException("Los servicios por consumo se registran individualmente con sus lecturas");
    }

    private void validarCreadas(List<CuentaCobrarEntity> creadas) {
        if (creadas.isEmpty())
            throw new BusinessException("Las cuentas seleccionadas ya fueron generadas para ese período y servicio");
    }

    private void validarLote(List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw new BusinessException("Seleccione al menos una cuenta");
    }
}
