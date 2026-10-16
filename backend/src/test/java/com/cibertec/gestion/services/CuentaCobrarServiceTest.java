package com.cibertec.gestion.services;

import com.cibertec.gestion.entities.*;
import com.cibertec.gestion.enums.*;
import com.cibertec.gestion.exceptions.BusinessException;
import com.cibertec.gestion.models.CuentaRequest;
import com.cibertec.gestion.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CuentaCobrarServiceTest {

    @Autowired private ICuentaCobrarService cuentaService;
    @Autowired private SocioRepository socioRepository;
    @Autowired private GiroRepository giroRepository;
    @Autowired private PuestoRepository puestoRepository;
    @Autowired private ServicioRepository servicioRepository;

    private PuestoEntity puesto;
    private ServicioEntity servicio;

    @BeforeEach
    void prepareData() {
        SocioEntity socio = socioRepository.save(SocioEntity.builder()
                .codigo("SOC-CAJA")
                .nombres("Carlos")
                .apellidos("Caja")
                .etapa(1)
                .fechaNacimiento(LocalDate.of(1985, 1, 1))
                .activo(true)
                .build());
        GiroEntity giro = giroRepository.save(GiroEntity.builder()
                .nombre("Abarrotes")
                .descripcion("Venta de alimentos")
                .activo(true)
                .build());
        puesto = puestoRepository.save(PuestoEntity.builder()
                .numero("P-101")
                .socio(socio)
                .giro(giro)
                .activo(true)
                .build());
        servicio = servicioRepository.save(ServicioEntity.builder()
                .nombre("Mantenimiento")
                .recurrencia(Recurrencia.MENSUAL)
                .costo(new BigDecimal("100.00"))
                .moneda(Moneda.PEN)
                .cargoA(CargoA.PUESTO)
                .dependeConsumo(false)
                .activo(true)
                .build());
    }

    @Test
    void debeGenerarCuentaPendiente() {
        CuentaCobrarEntity cuenta = cuentaService.create(request());

        assertThat(cuenta.getId()).isNotNull();
        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.PENDIENTE);
        assertThat(cuenta.getPuesto().getId()).isEqualTo(puesto.getId());
    }

    @Test
    void debeExonerarCuentaPendiente() {
        CuentaCobrarEntity cuenta = cuentaService.create(request());

        CuentaCobrarEntity exonerada = cuentaService.exonerate(cuenta.getId());

        assertThat(exonerada.getEstado()).isEqualTo(EstadoCuenta.EXONERADA);
    }

    @Test
    void debePagarCuentaYCrearRecibo() {
        CuentaCobrarEntity cuenta = cuentaService.create(request());

        ReciboEntity recibo = cuentaService.pay(cuenta.getId(), "admin_test");

        assertThat(recibo.getId()).isNotNull();
        assertThat(recibo.getCorrelativo()).startsWith("REC-");
        assertThat(recibo.getCuentaCobrar().getEstado()).isEqualTo(EstadoCuenta.PAGADA);
    }

    @Test
    void noDebePagarDosVecesLaMismaCuenta() {
        CuentaCobrarEntity cuenta = cuentaService.create(request());
        cuentaService.pay(cuenta.getId(), "admin_test");

        assertThatThrownBy(() -> cuentaService.pay(cuenta.getId(), "admin_test"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pagada o exonerada");
    }

    private CuentaRequest request() {
        CuentaRequest request = new CuentaRequest();
        request.setPeriodo("2026-08");
        request.setMonto(new BigDecimal("100.00"));
        request.setPuestoId(puesto.getId());
        request.setServicioId(servicio.getId());
        return request;
    }
}
