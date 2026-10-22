package com.cibertec.gestion.cash.services;

import com.cibertec.gestion.cash.dto.EgresoRequest;
import com.cibertec.gestion.cash.entities.CorrelativoEntity;
import com.cibertec.gestion.cash.entities.EgresoEntity;
import com.cibertec.gestion.cash.enums.EstadoEgreso;
import com.cibertec.gestion.cash.exceptions.BusinessException;
import com.cibertec.gestion.cash.repositories.CorrelativoRepository;
import com.cibertec.gestion.cash.repositories.EgresoRepository;
import com.cibertec.gestion.cash.security.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tesorería migrada (ESP-13 §5.1, ESP-04 §4.5, §5.6).
 *
 * <p>Siete casos: alta en {@code REGISTRADO}, procesamiento con fecha,
 * anulado que no se procesa, doble anulación, importación de la plantilla,
 * CSV con siete columnas y snapshot de usuario. El correlativo {@code EGRESO}
 * se declara aquí porque H2 no aplica Flyway.</p>
 */
@SpringBootTest
class EgresoServiceTest {

    private static final UsuarioPrincipal ADMIN = new UsuarioPrincipal(
            1L, "admin_test", "Administrador", "General", "ADMIN");

    @Autowired
    private IEgresoService egresoService;

    @Autowired
    private EgresoRepository egresoRepository;

    @Autowired
    private CorrelativoRepository correlativoRepository;

    @BeforeEach
    void prepareData() {
        egresoRepository.deleteAll();
        if (correlativoRepository.findById("EGRESO").isEmpty()) {
            correlativoRepository.save(new CorrelativoEntity("EGRESO", 0L));
        }
    }

    @Test
    void debeCrearEgresoEnEstadoRegistrado() {
        IEgresoService.EgresoResponse creado = egresoService.create(request("F001-100"), ADMIN);

        assertThat(creado.id()).isNotNull();
        assertThat(creado.estado()).isEqualTo(EstadoEgreso.REGISTRADO.name());
        assertThat(creado.comprobante()).startsWith("EGR-");
        assertThat(creado.fechaActualizacion()).isNull();
        assertThat(creado.usuario().usuario()).isEqualTo("admin_test");
        assertThat(creado.usuario().nombres()).isEqualTo("Administrador");
        assertThat(creado.usuario().apellidos()).isEqualTo("General");
        assertThat(creado.usuario().rol()).isEqualTo("ADMIN");
    }

    @Test
    void procesarDebeCambiarAProcesadoYActualizarFecha() {
        IEgresoService.EgresoResponse creado = egresoService.create(request("F001-101"), ADMIN);

        IEgresoService.EgresoResponse procesado = egresoService.process(creado.id());

        assertThat(procesado.estado()).isEqualTo(EstadoEgreso.PROCESADO.name());
        assertThat(procesado.fechaActualizacion()).isNotNull();
        assertThat(procesado.fechaRegistro()).isNotNull();
    }

    @Test
    void unEgresoAnuladoNoSeProcesa() {
        IEgresoService.EgresoResponse creado = egresoService.create(request("F001-102"), ADMIN);
        egresoService.annul(creado.id());

        assertThatThrownBy(() -> egresoService.process(creado.id()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("anulado");
    }

    @Test
    void anularDosVecesDevuelveError() {
        IEgresoService.EgresoResponse creado = egresoService.create(request("F001-103"), ADMIN);
        IEgresoService.EgresoResponse anulado = egresoService.annul(creado.id());

        assertThat(anulado.estado()).isEqualTo(EstadoEgreso.ANULADO.name());
        assertThat(anulado.fechaActualizacion()).isNotNull();

        assertThatThrownBy(() -> egresoService.annul(creado.id()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya está anulado");
    }

    @Test
    void laPlantillaDebeCrearDosEgresos() {
        String plantilla = """
                tipoDocumento;numeroDocumento;proveedor;fecha;subtotal;impuesto;total;motivo;documentoAsociado
                FACTURA;F001-100;Proveedor Demo;2026-08-19;100.00;18.00;118.00;Útiles de oficina;OC-001
                BOLETA;B001-205;Servicios Generales;2026-08-19;50.00;0.00;50.00;Mantenimiento menor;
                """;

        List<IEgresoService.EgresoResponse> importados = egresoService.importCsv(csv(plantilla), ADMIN);

        assertThat(importados).hasSize(2);
        assertThat(importados.get(0).comprobante()).startsWith("EGR-");
        assertThat(importados.get(1).comprobante()).isNotEqualTo(importados.get(0).comprobante());
        assertThat(importados.get(1).documentoAsociado()).isNull();
        assertThat(egresoRepository.count()).isEqualTo(2);
    }

    @Test
    void unCsvDeSieteColumnasDevuelveErrorConNumeroDeFila() {
        String invalido = """
                tipoDocumento;numeroDocumento;proveedor;fecha;subtotal;impuesto;total;motivo
                FACTURA;F001-100;Proveedor Demo;2026-08-19;100.00;18.00;118.00
                """;

        assertThatThrownBy(() -> egresoService.importCsv(csv(invalido), ADMIN))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Fila 2")
                .hasMessageContaining("8 columnas");
        assertThat(egresoRepository.count()).isZero();
    }

    @Test
    void noDebeCrearEgresoSinSnapshotDeUsuario() {
        UsuarioPrincipal incompleto = new UsuarioPrincipal(2L, "caja", "Operador", null, "OPERADOR");

        assertThatThrownBy(() -> egresoService.create(request("F001-104"), incompleto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("datos del usuario");
        assertThat(egresoRepository.count()).isZero();
    }

    // --- utilidades -------------------------------------------------------

    private MockMultipartFile csv(String contenido) {
        return new MockMultipartFile("archivo", "egresos.csv", "text/csv",
                contenido.getBytes(StandardCharsets.UTF_8));
    }

    private EgresoRequest request(String numeroDocumento) {
        EgresoRequest request = new EgresoRequest();
        request.setTipoDocumento("FACTURA");
        request.setNumeroDocumento(numeroDocumento);
        request.setProveedor("Proveedor Demo");
        request.setFecha(LocalDate.of(2026, 10, 21));
        request.setSubtotal(new BigDecimal("100.00"));
        request.setImpuesto(new BigDecimal("18.00"));
        request.setTotal(new BigDecimal("118.00"));
        request.setMotivo("Prueba");
        request.setDocumentoAsociado("OC-900");
        return request;
    }

    @SuppressWarnings("unused")
    private List<EgresoEntity> egresos() {
        return egresoRepository.findAll();
    }
}
