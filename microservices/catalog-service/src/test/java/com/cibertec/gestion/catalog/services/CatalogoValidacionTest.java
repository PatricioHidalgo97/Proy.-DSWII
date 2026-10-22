package com.cibertec.gestion.catalog.services;

import com.cibertec.gestion.catalog.dto.BancoRequest;
import com.cibertec.gestion.catalog.dto.GiroRequest;
import com.cibertec.gestion.catalog.dto.PuestoRequest;
import com.cibertec.gestion.catalog.dto.ServicioRequest;
import com.cibertec.gestion.catalog.entities.GiroEntity;
import com.cibertec.gestion.catalog.enums.CargoA;
import com.cibertec.gestion.catalog.enums.Moneda;
import com.cibertec.gestion.catalog.enums.Recurrencia;
import com.cibertec.gestion.catalog.exceptions.BusinessException;
import com.cibertec.gestion.catalog.exceptions.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reglas de duplicidad y validación migradas del monolito (ESP-13 §5.1).
 * Cada prueba levanta un contexto limpio para no depender del orden.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CatalogoValidacionTest {

    @Autowired
    private IGiroService giroService;

    @Autowired
    private IPuestoService puestoService;

    @Autowired
    private IServicioService servicioService;

    @Autowired
    private IBancoService bancoService;

    @Test
    void giroDuplicadoDevuelveConflicto() {
        giroService.create(giro("Giro PM"));

        assertThatThrownBy(() -> giroService.create(giro("giro pm")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ya existe un giro con el nombre indicado");
    }

    @Test
    void puestoDuplicadoDevuelveConflicto() {
        GiroEntity giro = giroService.create(giro("Giro PM"));
        puestoService.create(puesto("P-PM-01", giro.getId(), null));

        assertThatThrownBy(() -> puestoService.create(puesto("P-PM-01", giro.getId(), null)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ya existe un puesto con el número indicado");
    }

    @Test
    void servicioDuplicadoDevuelveConflicto() {
        servicioService.create(servicio("Servicio PM"));

        assertThatThrownBy(() -> servicioService.create(servicio("servicio pm")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ya existe un servicio con el nombre indicado");
    }

    @Test
    void bancoConCciDuplicadoDevuelveConflicto() {
        bancoService.create(banco("0011-0001", "01104550010023456718"));

        assertThatThrownBy(() -> bancoService.create(banco("0011-0002", "01104550010023456718")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ya existe el CCI indicado");
    }

    @Test
    void fechaFinAnteriorAFechaInicioDevuelveErrorDeNegocio() {
        GiroEntity giro = giroService.create(giro("Giro PM"));
        PuestoRequest request = puesto("P-PM-02", giro.getId(), null);
        request.setFechaInicio(LocalDate.of(2026, 5, 10));
        request.setFechaFin(LocalDate.of(2026, 1, 10));

        assertThatThrownBy(() -> puestoService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("La fecha de fin no puede ser anterior a la fecha de inicio");
    }

    @Test
    void puestoGuardadoConservaSocioYGiroAnidados() {
        GiroEntity giro = giroService.create(giro("Giro PM"));
        var creado = puestoService.create(puesto("P-PM-03", giro.getId(), null));

        var recuperado = puestoService.getById(creado.id());

        assertThat(recuperado.giro()).isNotNull();
        assertThat(recuperado.giro().nombre()).isEqualTo("Giro PM");
        assertThat(recuperado.socio()).isNull();
    }

    private GiroRequest giro(String nombre) {
        GiroRequest request = new GiroRequest();
        request.setNombre(nombre);
        request.setDescripcion("Giro de pruebas");
        request.setActivo(true);
        return request;
    }

    private PuestoRequest puesto(String numero, Long giroId, Long socioId) {
        PuestoRequest request = new PuestoRequest();
        request.setNumero(numero);
        request.setNombreInquilino("Inquilino de pruebas");
        request.setGiroId(giroId);
        request.setSocioId(socioId);
        request.setActivo(true);
        return request;
    }

    private ServicioRequest servicio(String nombre) {
        ServicioRequest request = new ServicioRequest();
        request.setNombre(nombre);
        request.setRecurrencia(Recurrencia.MENSUAL);
        request.setCosto(new BigDecimal("50.00"));
        request.setMoneda(Moneda.PEN);
        request.setCargoA(CargoA.PUESTO);
        request.setDependeConsumo(false);
        request.setActivo(true);
        return request;
    }

    private BancoRequest banco(String numeroCuenta, String cci) {
        BancoRequest request = new BancoRequest();
        request.setNombre("Banco PM");
        request.setNumeroCuenta(numeroCuenta);
        request.setCci(cci);
        request.setMoneda(Moneda.PEN);
        request.setActivo(true);
        return request;
    }
}
