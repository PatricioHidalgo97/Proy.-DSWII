package com.cibertec.gestion.services.implementations;

import com.cibertec.gestion.entities.*;
import com.cibertec.gestion.enums.CargoA;
import com.cibertec.gestion.enums.EstadoCuenta;
import com.cibertec.gestion.enums.TipoRecibo;
import com.cibertec.gestion.exceptions.BusinessException;
import com.cibertec.gestion.exceptions.NotFoundException;
import com.cibertec.gestion.models.CanjeRequest;
import com.cibertec.gestion.models.CuentaRequest;
import com.cibertec.gestion.models.GenerarCuentasRequest;
import com.cibertec.gestion.repositories.*;
import com.cibertec.gestion.services.ICuentaCobrarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CuentaCobrarService implements ICuentaCobrarService {
    private final CuentaCobrarRepository cuentaRepository;
    private final PuestoRepository puestoRepository;
    private final SocioRepository socioRepository;
    private final ServicioRepository servicioRepository;
    private final ReciboRepository reciboRepository;
    private final UsuarioRepository usuarioRepository;
    private final BancoRepository bancoRepository;
    private final CorrelativoService correlativoService;

    @Override public List<CuentaCobrarEntity> getAll() { return cuentaRepository.findAll(); }

    @Override public List<CuentaCobrarEntity> getByPuesto(Long puestoId) {
        if (!puestoRepository.existsById(puestoId)) throw new NotFoundException("Puesto no encontrado");
        return cuentaRepository.findByPuestoIdOrderByFechaCreacionDesc(puestoId);
    }

    @Override public List<CuentaCobrarEntity> getBySocio(Long socioId) {
        if (!socioRepository.existsById(socioId)) throw new NotFoundException("Socio no encontrado");
        return cuentaRepository.findBySocioIdOrderByFechaCreacionDesc(socioId);
    }

    @Override @Transactional
    public CuentaCobrarEntity create(CuentaRequest request) {
        ServicioEntity servicio = getServicio(request.getServicioId());
        boolean tienePuesto = request.getPuestoId() != null;
        boolean tieneSocio = request.getSocioId() != null;
        if (tienePuesto == tieneSocio) throw new BusinessException("Seleccione un puesto o un socio, pero no ambos");
        PuestoEntity puesto = null;
        SocioEntity socio = null;
        if (tienePuesto) {
            if (servicio.getCargoA() != CargoA.PUESTO)
                throw new BusinessException("El servicio seleccionado no está configurado para puestos");
            puesto = puestoRepository.findById(request.getPuestoId())
                    .orElseThrow(() -> new NotFoundException("Puesto no encontrado"));
            validarDuplicado(request.getPeriodo(), servicio.getId(), puesto.getId(), null);
        } else {
            if (servicio.getCargoA() != CargoA.SOCIO)
                throw new BusinessException("El servicio seleccionado no está configurado para socios");
            socio = socioRepository.findById(request.getSocioId())
                    .orElseThrow(() -> new NotFoundException("Socio no encontrado"));
            validarDuplicado(request.getPeriodo(), servicio.getId(), null, socio.getId());
        }
        BigDecimal monto = calcularMonto(servicio, request.getMonto(), request.getLecturaInicial(),
                request.getLecturaFinal(), request.getCostoUnitario());
        return cuentaRepository.save(nuevaCuenta(request.getPeriodo(), monto, puesto, socio, servicio,
                request.getLecturaInicial(), request.getLecturaFinal(), request.getCostoUnitario()));
    }

    @Override @Transactional
    public List<CuentaCobrarEntity> generateForPuestos(GenerarCuentasRequest request) {
        ServicioEntity servicio = getServicio(request.getServicioId());
        if (servicio.getCargoA() != CargoA.PUESTO)
            throw new BusinessException("El servicio debe estar configurado para puestos");
        validarServicioMasivo(servicio);
        List<Long> ids = request.getPuestoIds();
        if (ids == null || ids.isEmpty())
            ids = puestoRepository.findAll().stream().filter(PuestoEntity::isActivo).map(PuestoEntity::getId).toList();
        if (ids.isEmpty()) throw new BusinessException("No hay puestos seleccionados para generar cuentas");
        BigDecimal monto = calcularMonto(servicio, request.getMonto(), null, null, null);
        List<CuentaCobrarEntity> creadas = new ArrayList<>();
        for (Long id : new LinkedHashSet<>(ids)) {
            PuestoEntity puesto = puestoRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Puesto no encontrado: " + id));
            if (!cuentaRepository.existsByPeriodoAndServicioIdAndPuestoId(request.getPeriodo(), servicio.getId(), id))
                creadas.add(cuentaRepository.save(nuevaCuenta(request.getPeriodo(), monto, puesto, null, servicio,
                        null, null, null)));
        }
        validarCreadas(creadas);
        return creadas;
    }

    @Override @Transactional
    public List<CuentaCobrarEntity> generateForSocios(GenerarCuentasRequest request) {
        ServicioEntity servicio = getServicio(request.getServicioId());
        if (servicio.getCargoA() != CargoA.SOCIO)
            throw new BusinessException("El servicio debe estar configurado para socios");
        validarServicioMasivo(servicio);
        List<SocioEntity> socios = new ArrayList<>();
        if (request.getSocioIds() != null && !request.getSocioIds().isEmpty()) {
            for (Long id : new LinkedHashSet<>(request.getSocioIds()))
                socios.add(socioRepository.findById(id)
                        .orElseThrow(() -> new NotFoundException("Socio no encontrado: " + id)));
        } else {
            socios.addAll(socioRepository.findAll().stream().filter(SocioEntity::isActivo)
                    .filter(s -> request.getEtapa() == null || request.getEtapa().equals(s.getEtapa())).toList());
        }
        if (socios.isEmpty()) throw new BusinessException("No hay socios seleccionados para generar cuentas");
        if (request.isSociosUnicos()) {
            Set<String> nombres = new HashSet<>();
            socios = socios.stream().filter(s -> nombres.add(
                    (s.getNombres() + " " + s.getApellidos()).trim().toLowerCase(Locale.ROOT))).toList();
        }
        BigDecimal monto = calcularMonto(servicio, request.getMonto(), null, null, null);
        List<CuentaCobrarEntity> creadas = new ArrayList<>();
        for (SocioEntity socio : socios)
            if (!cuentaRepository.existsByPeriodoAndServicioIdAndSocioId(request.getPeriodo(), servicio.getId(), socio.getId()))
                creadas.add(cuentaRepository.save(nuevaCuenta(request.getPeriodo(), monto, null, socio, servicio,
                        null, null, null)));
        validarCreadas(creadas);
        return creadas;
    }

    @Override @Transactional
    public CuentaCobrarEntity exonerate(Long id) {
        CuentaCobrarEntity cuenta = getPendingAccount(id);
        cuenta.setEstado(EstadoCuenta.EXONERADA);
        return cuentaRepository.save(cuenta);
    }

    @Override @Transactional
    public List<CuentaCobrarEntity> exonerateBatch(List<Long> ids) {
        validarLote(ids);
        List<CuentaCobrarEntity> cuentas = ids.stream().distinct().map(this::getPendingAccount).toList();
        cuentas.forEach(c -> c.setEstado(EstadoCuenta.EXONERADA));
        return cuentaRepository.saveAll(cuentas);
    }

    @Override @Transactional
    public ReciboEntity pay(Long id, String username) {
        return registrarPago(getPendingAccount(id), getUsuario(username), TipoRecibo.INGRESO, null, null);
    }

    @Override @Transactional
    public List<ReciboEntity> payBatch(List<Long> ids, String username) {
        validarLote(ids);
        UsuarioEntity usuario = getUsuario(username);
        List<CuentaCobrarEntity> cuentas = ids.stream().distinct().map(this::getPendingAccount).toList();
        List<ReciboEntity> recibos = new ArrayList<>();
        for (CuentaCobrarEntity cuenta : cuentas)
            recibos.add(registrarPago(cuenta, usuario, TipoRecibo.INGRESO, null, null));
        return recibos;
    }

    @Override @Transactional
    public ReciboEntity exchange(CanjeRequest request, String username) {
        CuentaCobrarEntity cuenta = getPendingAccount(request.getCuentaId());
        if (cuenta.getSocio() == null)
            throw new BusinessException("El canje bancario corresponde a cuentas asignadas a socios");
        BancoEntity banco = bancoRepository.findById(request.getBancoId())
                .orElseThrow(() -> new NotFoundException("Banco no encontrado"));
        if (!banco.isActivo()) throw new BusinessException("El banco seleccionado está inactivo");
        return registrarPago(cuenta, getUsuario(username), TipoRecibo.BANCARIO, banco, request.getFechaDeposito());
    }

    private ReciboEntity registrarPago(CuentaCobrarEntity cuenta, UsuarioEntity usuario, TipoRecibo tipo,
                                       BancoEntity banco, java.time.LocalDate fechaDeposito) {
        cuenta.setEstado(EstadoCuenta.PAGADA);
        cuentaRepository.save(cuenta);
        return reciboRepository.save(ReciboEntity.builder()
                .correlativo(correlativoService.next("RECIBO", "REC"))
                .fecha(LocalDateTime.now()).monto(cuenta.getMonto()).tipo(tipo).cuentaCobrar(cuenta)
                .banco(banco).fechaDeposito(fechaDeposito)
                .concepto(tipo == TipoRecibo.BANCARIO ? "Canje bancario académico" : "Pago de cuenta")
                .usuario(usuario).build());
    }

    private CuentaCobrarEntity getPendingAccount(Long id) {
        CuentaCobrarEntity cuenta = cuentaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Cuenta por cobrar no encontrada"));
        if (cuenta.getEstado() != EstadoCuenta.PENDIENTE)
            throw new BusinessException("La cuenta ya fue pagada o exonerada");
        return cuenta;
    }

    private ServicioEntity getServicio(Long id) {
        ServicioEntity servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Servicio no encontrado"));
        if (!servicio.isActivo()) throw new BusinessException("El servicio seleccionado está inactivo");
        return servicio;
    }

    private UsuarioEntity getUsuario(String username) {
        return usuarioRepository.findByUsuario(username)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private BigDecimal calcularMonto(ServicioEntity servicio, BigDecimal montoSolicitado,
                                     BigDecimal lecturaInicial, BigDecimal lecturaFinal, BigDecimal costoUnitario) {
        if (!servicio.isDependeConsumo())
            return (montoSolicitado != null ? montoSolicitado : servicio.getCosto()).setScale(2, RoundingMode.HALF_UP);
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

    private CuentaCobrarEntity nuevaCuenta(String periodo, BigDecimal monto, PuestoEntity puesto, SocioEntity socio,
                                            ServicioEntity servicio, BigDecimal inicial, BigDecimal fin, BigDecimal costo) {
        return CuentaCobrarEntity.builder().periodo(periodo).monto(monto).estado(EstadoCuenta.PENDIENTE)
                .fechaCreacion(LocalDateTime.now()).puesto(puesto).socio(socio).servicio(servicio)
                .lecturaInicial(inicial).lecturaFinal(fin).costoUnitario(costo).build();
    }

    private void validarDuplicado(String periodo, Long servicioId, Long puestoId, Long socioId) {
        boolean existe = puestoId != null
                ? cuentaRepository.existsByPeriodoAndServicioIdAndPuestoId(periodo, servicioId, puestoId)
                : cuentaRepository.existsByPeriodoAndServicioIdAndSocioId(periodo, servicioId, socioId);
        if (existe) throw new BusinessException("Ya existe una cuenta para ese período, servicio y destinatario");
    }

    private void validarServicioMasivo(ServicioEntity servicio) {
        if (servicio.isDependeConsumo())
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
