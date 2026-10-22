package com.cibertec.gestion.cash.services;

import com.cibertec.gestion.cash.clients.CatalogoClient;
import com.cibertec.gestion.cash.clients.dto.PuestoRemoto;
import com.cibertec.gestion.cash.clients.dto.ServicioRemoto;
import com.cibertec.gestion.cash.dto.CuentaRequest;
import com.cibertec.gestion.cash.entities.CorrelativoEntity;
import com.cibertec.gestion.cash.entities.CuentaCobrarEntity;
import com.cibertec.gestion.cash.entities.ReciboEntity;
import com.cibertec.gestion.cash.enums.EstadoCuenta;
import com.cibertec.gestion.cash.enums.TipoRecibo;
import com.cibertec.gestion.cash.exceptions.BusinessException;
import com.cibertec.gestion.cash.exceptions.DependenciaNoDisponibleException;
import com.cibertec.gestion.cash.mappers.CuentaCobrarMapper;
import com.cibertec.gestion.cash.mappers.ReciboMapper;
import com.cibertec.gestion.cash.repositories.CorrelativoRepository;
import com.cibertec.gestion.cash.repositories.CuentaCobrarRepository;
import com.cibertec.gestion.cash.repositories.ReciboRepository;
import com.cibertec.gestion.cash.security.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Reglas de cobranza migradas (ESP-13 §5.1, ESP-12 R-02 y R-05).
 *
 * <p>Cuatro casos portados de {@code backend/.../CuentaCobrarServiceTest.java}
 * más tres casos nuevos: snapshot incompleto, dos pagos concurrentes y catálogo
 * caído. El catálogo se mockea con {@link CatalogoClient}: no hay llamadas de
 * red.</p>
 */
@SpringBootTest
class CuentaCobrarServiceTest {

    private static final Long PUESTO_ID = 1L;
    private static final Long SERVICIO_ID = 1L;
    private static final Long SOCIO_ID = 1L;
    private static final String PERIODO = "2026-10";

    private static final UsuarioPrincipal ADMIN = new UsuarioPrincipal(
            1L, "admin_test", "Administrador", "General", "ADMIN");

    @Autowired
    private ICuentaCobrarService cuentaService;

    @Autowired
    private CuentaCobrarRepository cuentaRepository;

    @Autowired
    private ReciboRepository reciboRepository;

    @MockitoBean
    private CatalogoClient catalogoClient;

    @Autowired
    private CorrelativoRepository correlativoRepository;

    private final AtomicInteger periodoSeq = new AtomicInteger();

    @BeforeEach
    void prepareData() {
        // H2 no aplica Flyway: el correlativo del seed se declara aquí (ESP-06 §5.7).
        reciboRepository.deleteAll();
        cuentaRepository.deleteAll();
        if (correlativoRepository.findById("RECIBO").isEmpty()) {
            correlativoRepository.save(new CorrelativoEntity("RECIBO", 0L));
        }
        periodoSeq.set(0);
    }

    @BeforeEach
    void prepareCatalogo() {
        when(catalogoClient.getServicio(anyLong())).thenReturn(
                new ServicioRemoto(SERVICIO_ID, "Mantenimiento de puesto", "PEN", "PUESTO",
                        false, new BigDecimal("100.00"), true));
        when(catalogoClient.getPuesto(anyLong())).thenReturn(puesto());
        when(catalogoClient.getSocio(anyLong())).thenReturn(
                new com.cibertec.gestion.cash.clients.dto.SocioRemoto(
                        SOCIO_ID, "SOC-001", "Carlos", "Caja", 1, true));
    }

    // --- casos portados del monolito -------------------------------------

    @Test
    void debeGenerarCuentaPendiente() {
        CuentaCobrarMapper.CuentaCobrarResponse cuenta = cuentaService.create(request());

        assertThat(cuenta.id()).isNotNull();
        assertThat(cuenta.estado()).isEqualTo(EstadoCuenta.PENDIENTE.name());
        assertThat(cuenta.puesto()).isNotNull();
        assertThat(cuenta.puesto().id()).isEqualTo(PUESTO_ID);
    }

    @Test
    void debeExonerarCuentaPendiente() {
        CuentaCobrarMapper.CuentaCobrarResponse cuenta = cuentaService.create(request());

        CuentaCobrarMapper.CuentaCobrarResponse exonerada = cuentaService.exonerate(cuenta.id());

        assertThat(exonerada.estado()).isEqualTo(EstadoCuenta.EXONERADA.name());
    }

    @Test
    void debePagarCuentaYCrearRecibo() {
        CuentaCobrarMapper.CuentaCobrarResponse cuenta = cuentaService.create(request());

        ReciboMapper.ReciboResponse recibo = cuentaService.pay(cuenta.id(), ADMIN);

        assertThat(recibo.id()).isNotNull();
        assertThat(recibo.correlativo()).startsWith("REC-");
        assertThat(recibo.cuentaCobrar().estado()).isEqualTo(EstadoCuenta.PAGADA.name());
        assertThat(recibo.usuario().usuario()).isEqualTo("admin_test");
    }

    @Test
    void noDebePagarDosVecesLaMismaCuenta() {
        CuentaCobrarMapper.CuentaCobrarResponse cuenta = cuentaService.create(request());
        cuentaService.pay(cuenta.id(), ADMIN);

        assertThatThrownBy(() -> cuentaService.pay(cuenta.id(), ADMIN))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pagada o exonerada");
    }

    // --- casos nuevos del plan -------------------------------------------

    /**
     * R-02: el snapshot del usuario es obligatorio. Sin los claims del token no
     * se emite el recibo y la cuenta permanece PENDIENTE.
     */
    @Test
    void noDebeEmitirReciboConSnapshotDeUsuarioIncompleto() {
        CuentaCobrarMapper.CuentaCobrarResponse cuenta = cuentaService.create(request());
        UsuarioPrincipal incompleto = new UsuarioPrincipal(2L, "caja", "", null, "OPERADOR");

        assertThatThrownBy(() -> cuentaService.pay(cuenta.id(), incompleto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("datos del usuario");

        CuentaCobrarEntity persistida = cuentaRepository.findById(cuenta.id()).orElseThrow();
        assertThat(persistida.getEstado()).isEqualTo(EstadoCuenta.PENDIENTE);
        assertThat(reciboRepository.count()).isZero();

        // Y la base también rechaza un snapshot nulo: las columnas son NOT NULL.
        assertThatThrownBy(() -> reciboRepository.saveAndFlush(ReciboEntity.builder()
                .correlativo("REC-999999")
                .fecha(LocalDateTime.now())
                .monto(new BigDecimal("10.00"))
                .tipo(TipoRecibo.INGRESO)
                .usuarioId(2L)
                .usuarioUsuario("caja")
                .usuarioNombres(null)
                .usuarioApellidos(null)
                .usuarioRol("OPERADOR")
                .build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * R-05: dos pagos concurrentes deben producir correlativos distintos.
     */
    @Test
    void dosPagosConcurrentesDebenProducirCorrelativosDistintos() throws Exception {
        CuentaCobrarMapper.CuentaCobrarResponse primera = cuentaService.create(request());
        CuentaCobrarMapper.CuentaCobrarResponse segunda = cuentaService.create(request());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch salida = new CountDownLatch(1);

        Callable<String> pago1 = pagoConcurrente(primera.id(), listos, salida);
        Callable<String> pago2 = pagoConcurrente(segunda.id(), listos, salida);

        Future<String> futuro1 = pool.submit(pago1);
        Future<String> futuro2 = pool.submit(pago2);
        try {
            listos.await();
            salida.countDown();
            String correlativo1 = futuro1.get();
            String correlativo2 = futuro2.get();

            assertThat(correlativo1).isNotEqualTo(correlativo2);
            assertThat(correlativo1).startsWith("REC-");
            assertThat(correlativo2).startsWith("REC-");
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * R-03: con catálogo caído el alta responde 503 y no inserta filas.
     */
    @Test
    void catalogoCaidoNoDebeInsertarCuenta() {
        long antes = cuentaRepository.count();
        when(catalogoClient.getServicio(anyLong()))
                .thenThrow(new DependenciaNoDisponibleException(
                        "El catálogo no está disponible; intente más tarde"));

        assertThatThrownBy(() -> cuentaService.create(request()))
                .isInstanceOf(DependenciaNoDisponibleException.class);

        assertThat(cuentaRepository.count()).isEqualTo(antes);
    }

    /**
     * PLAN-09 T-06: el alta persiste los ids del socio y del giro del puesto que
     * llegan en {@link PuestoRemoto} y los devuelve en la respuesta (ESP-04
     * §5.4). Sin ellos, {@code puesto.socio.id} y {@code puesto.giro.id} salían
     * en {@code null} (CA-02 de PLAN-04).
     */
    @Test
    void debePersistirLosIdsAnidadosDelPuesto() {
        CuentaCobrarMapper.CuentaCobrarResponse creada = cuentaService.create(request());

        // Los ids que publica el PuestoRemoto mockeado por el catálogo.
        Long giroEsperado = puesto().giro().id();
        Long socioEsperado = puesto().socio().id();

        CuentaCobrarEntity persistida = cuentaRepository.findById(creada.id()).orElseThrow();
        assertThat(persistida.getPuestoGiroId()).isEqualTo(giroEsperado);
        assertThat(persistida.getPuestoSocioId()).isEqualTo(socioEsperado);

        assertThat(creada.puesto()).isNotNull();
        assertThat(creada.puesto().socio()).isNotNull();
        assertThat(creada.puesto().socio().id()).isNotNull().isEqualTo(socioEsperado);
        assertThat(creada.puesto().giro()).isNotNull();
        assertThat(creada.puesto().giro().id()).isNotNull().isEqualTo(giroEsperado);
    }

    /**
     * PLAN-09 T-06: una cuenta por socio no tiene puesto, así que las dos
     * columnas nuevas quedan {@code null} y {@code puesto()} viaja en
     * {@code null} (ESP-03 §10).
     */
    @Test
    void cuentaPorSocioNoDebeTraerIdsDePuesto() {
        CuentaRequest solicitud = new CuentaRequest();
        solicitud.setPeriodo(PERIODO + "-" + periodoSeq.incrementAndGet());
        solicitud.setMonto(new BigDecimal("50.00"));
        solicitud.setSocioId(SOCIO_ID);
        solicitud.setServicioId(SERVICIO_ID);
        when(catalogoClient.getServicio(anyLong())).thenReturn(
                new ServicioRemoto(SERVICIO_ID, "Cuota ordinaria de socio", "PEN", "SOCIO",
                        false, new BigDecimal("50.00"), true));

        CuentaCobrarMapper.CuentaCobrarResponse creada = cuentaService.create(solicitud);

        assertThat(creada.puesto()).isNull();

        CuentaCobrarEntity persistida = cuentaRepository.findById(creada.id()).orElseThrow();
        assertThat(persistida.getPuestoId()).isNull();
        assertThat(persistida.getPuestoGiroId()).isNull();
        assertThat(persistida.getPuestoSocioId()).isNull();
    }

    // --- utilidades -------------------------------------------------------

    private Callable<String> pagoConcurrente(Long cuentaId, CountDownLatch listos, CountDownLatch salida) {
        return () -> {
            listos.countDown();
            listos.await();
            salida.await();
            return cuentaService.pay(cuentaId, ADMIN).correlativo();
        };
    }

    private CuentaRequest request() {
        CuentaRequest request = new CuentaRequest();
        request.setPeriodo(PERIODO + "-" + periodoSeq.incrementAndGet());
        request.setMonto(new BigDecimal("100.00"));
        request.setPuestoId(PUESTO_ID);
        request.setServicioId(SERVICIO_ID);
        return request;
    }

    private PuestoRemoto puesto() {
        return new PuestoRemoto(PUESTO_ID, "P-101", "Rosa Quispe Mamani",
                new PuestoRemoto.SocioAnidado(SOCIO_ID, "SOC-001", "Rosa", "Quispe Mamani"),
                new PuestoRemoto.GiroAnidado(1L, "Abarrotes y víveres"),
                true);
    }

    @SuppressWarnings("unused")
    private List<ReciboEntity> recibos() {
        return reciboRepository.findAll();
    }
}
