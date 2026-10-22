package com.cibertec.gestion.cash.controllers;

import com.cibertec.gestion.cash.dto.CanjeRequest;
import com.cibertec.gestion.cash.dto.CuentaRequest;
import com.cibertec.gestion.cash.dto.GenerarCuentasRequest;
import com.cibertec.gestion.cash.dto.OperacionLoteRequest;
import com.cibertec.gestion.cash.mappers.CuentaCobrarMapper;
import com.cibertec.gestion.cash.mappers.ReciboMapper;
import com.cibertec.gestion.cash.security.UsuarioPrincipal;
import com.cibertec.gestion.cash.services.ICuentaCobrarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Once operaciones de caja (ESP-04 §4.3, §5.4).
 *
 * <p>La cuenta publica {@code puesto.socio.id} y {@code puesto.giro.id} desde
 * el snapshot: la migración {@code V5__snapshot_ids_puesto.sql} añade
 * {@code puesto_socio_id} y {@code puesto_giro_id} a {@code gestion_cash} y las
 * rellena en los documentos ya emitidos, y el alta las escribe desde
 * {@code PuestoRemoto} (ESP-03 §4.3, §10). Los nombres de campo y los
 * {@code null} explícitos se conservan (ESP-04 §7.5), y el listado no consulta
 * catálogo.</p>
 */
@RestController
@RequestMapping("/api/cuentas")
@RequiredArgsConstructor
@Tag(name = "Cuentas", description = "Cuentas por cobrar, generación masiva, exoneración y pagos (ESP-04 §4.3)")
@SecurityRequirement(name = "bearerAuth")
public class CuentaCobrarController {

    private final ICuentaCobrarService cuentaService;

    @GetMapping
    @Operation(summary = "Lista todas las cuentas por cobrar",
            description = "Construye `puesto`, `socio` y `servicio` desde los snapshots; no llama a catálogo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de cuentas"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido")
    })
    public ResponseEntity<List<CuentaCobrarMapper.CuentaCobrarResponse>> getAll() {
        return ResponseEntity.ok(cuentaService.getAll());
    }

    @GetMapping("/puesto/{puestoId}")
    @Operation(summary = "Cuentas de un puesto",
            description = "Listado histórico por `puestoId`; no requiere que catálogo esté disponible.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuentas del puesto"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido")
    })
    public ResponseEntity<List<CuentaCobrarMapper.CuentaCobrarResponse>> getByPuesto(@PathVariable Long puestoId) {
        return ResponseEntity.ok(cuentaService.getByPuesto(puestoId));
    }

    @GetMapping("/socio/{socioId}")
    @Operation(summary = "Cuentas de un socio",
            description = "Listado histórico por `socioId`; no requiere que catálogo esté disponible.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuentas del socio"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido")
    })
    public ResponseEntity<List<CuentaCobrarMapper.CuentaCobrarResponse>> getBySocio(@PathVariable Long socioId) {
        return ResponseEntity.ok(cuentaService.getBySocio(socioId));
    }

    @PostMapping
    @Operation(summary = "Crea una cuenta individual",
            description = "Exige puesto **o** socio, nunca ambos, y que el servicio tenga el `cargoA` "
                    + "correspondiente. El snapshot se copia de catálogo antes de insertar.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cuenta creada, con `puesto`, `socio` y `servicio` anidados",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "Cuenta por puesto", value = """
                                    {
                                      "id": 10,
                                      "periodo": "2026-10",
                                      "monto": 80.00,
                                      "estado": "PENDIENTE",
                                      "fechaCreacion": "2026-10-05T09:10:00",
                                      "puesto": {
                                        "id": 1,
                                        "numero": "P-101",
                                        "nombreInquilino": "Rosa Quispe Mamani",
                                        "socio": { "id": null, "codigo": "SOC-001", "nombres": "Rosa", "apellidos": "Quispe Mamani" },
                                        "giro": { "id": null, "nombre": "Abarrotes y víveres" }
                                      },
                                      "socio": null,
                                      "servicio": {
                                        "id": 1,
                                        "nombre": "Mantenimiento de puesto",
                                        "moneda": "PEN",
                                        "cargoA": "PUESTO",
                                        "dependeConsumo": false
                                      },
                                      "lecturaInicial": null,
                                      "lecturaFinal": null,
                                      "costoUnitario": null
                                    }
                                    """),
                            @ExampleObject(name = "Cuenta por socio", value = """
                                    {
                                      "id": 11,
                                      "periodo": "2026-10",
                                      "monto": 50.00,
                                      "estado": "PENDIENTE",
                                      "fechaCreacion": "2026-10-05T09:12:00",
                                      "puesto": null,
                                      "socio": { "id": 1, "codigo": "SOC-001", "nombres": "Rosa", "apellidos": "Quispe Mamani" },
                                      "servicio": {
                                        "id": 6,
                                        "nombre": "Cuota ordinaria de socio",
                                        "moneda": "PEN",
                                        "cargoA": "SOCIO",
                                        "dependeConsumo": false
                                      },
                                      "lecturaInicial": null,
                                      "lecturaFinal": null,
                                      "costoUnitario": null
                                    }
                                    """)})),
            @ApiResponse(responseCode = "400", description = "Regla de negocio: destinatario inválido, duplicado de período o servicio inactivo"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "404", description = "Servicio, puesto o socio no encontrado"),
            @ApiResponse(responseCode = "503", description = "Catálogo no disponible; no se insertó ninguna fila")
    })
    public ResponseEntity<CuentaCobrarMapper.CuentaCobrarResponse> create(@Valid @RequestBody CuentaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuentaService.create(request));
    }

    @PostMapping("/generar/puestos")
    @Operation(summary = "Genera cuentas masivas por puestos",
            description = "Sin `puestoIds` usa todos los puestos activos del catálogo. No admite servicios por consumo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cuentas creadas"),
            @ApiResponse(responseCode = "400", description = "Servicio no configurado para puestos, servicio por consumo o período ya generado"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "404", description = "Servicio o puesto no encontrado"),
            @ApiResponse(responseCode = "503", description = "Catálogo no disponible; no se insertó ninguna fila")
    })
    public ResponseEntity<List<CuentaCobrarMapper.CuentaCobrarResponse>> generateForPuestos(
            @Valid @RequestBody GenerarCuentasRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuentaService.generateForPuestos(request));
    }

    @PostMapping("/generar/socios")
    @Operation(summary = "Genera cuentas masivas por socios",
            description = "Sin `socioIds` usa los socios activos del catálogo, con filtro opcional por `etapa` y `sociosUnicos`.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cuentas creadas"),
            @ApiResponse(responseCode = "400", description = "Servicio no configurado para socios, servicio por consumo o período ya generado"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "404", description = "Servicio o socio no encontrado"),
            @ApiResponse(responseCode = "503", description = "Catálogo no disponible; no se insertó ninguna fila")
    })
    public ResponseEntity<List<CuentaCobrarMapper.CuentaCobrarResponse>> generateForSocios(
            @Valid @RequestBody GenerarCuentasRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuentaService.generateForSocios(request));
    }

    @PutMapping("/{id}/exonerar")
    @Operation(summary = "Exonera una cuenta pendiente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuenta exonerada"),
            @ApiResponse(responseCode = "400", description = "La cuenta ya fue pagada o exonerada"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    public ResponseEntity<CuentaCobrarMapper.CuentaCobrarResponse> exonerate(@PathVariable Long id) {
        return ResponseEntity.ok(cuentaService.exonerate(id));
    }

    @PutMapping("/exonerar-lote")
    @Operation(summary = "Exonera varias cuentas pendientes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuentas exoneradas"),
            @ApiResponse(responseCode = "400", description = "Lote vacío o alguna cuenta ya pagada o exonerada"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "404", description = "Alguna cuenta no existe")
    })
    public ResponseEntity<List<CuentaCobrarMapper.CuentaCobrarResponse>> exonerateBatch(
            @Valid @RequestBody OperacionLoteRequest request) {
        return ResponseEntity.ok(cuentaService.exonerateBatch(request.getCuentaIds()));
    }

    @PostMapping("/{id}/pagar")
    @Operation(summary = "Paga una cuenta y emite su recibo",
            description = "El correlativo continúa la secuencia `RECIBO` con bloqueo pesimista. "
                    + "No depende de catálogo. El snapshot del usuario sale del JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Recibo emitido"),
            @ApiResponse(responseCode = "400", description = "La cuenta ya fue pagada o exonerada"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    public ResponseEntity<ReciboMapper.ReciboResponse> pay(@PathVariable Long id,
                                                           @AuthenticationPrincipal UsuarioPrincipal usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuentaService.pay(id, usuario));
    }

    @PostMapping("/pagar-lote")
    @Operation(summary = "Paga varias cuentas y emite un recibo por cada una")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Recibos emitidos"),
            @ApiResponse(responseCode = "400", description = "Lote vacío o alguna cuenta ya pagada o exonerada"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "404", description = "Alguna cuenta no existe")
    })
    public ResponseEntity<List<ReciboMapper.ReciboResponse>> payBatch(
            @Valid @RequestBody OperacionLoteRequest request,
            @AuthenticationPrincipal UsuarioPrincipal usuario) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cuentaService.payBatch(request.getCuentaIds(), usuario));
    }

    @PostMapping("/canjear")
    @Operation(summary = "Registra un canje bancario simulado",
            description = "Solo para cuentas asignadas a socios. Valida el banco contra catálogo y guarda su snapshot.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Recibo bancario emitido, con `banco` y `usuario` anidados"),
            @ApiResponse(responseCode = "400", description = "La cuenta no es de socio, el banco está inactivo o la cuenta ya fue pagada"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "404", description = "Cuenta o banco no encontrado"),
            @ApiResponse(responseCode = "503", description = "Catálogo no disponible; no se insertó ninguna fila")
    })
    public ResponseEntity<ReciboMapper.ReciboResponse> exchange(
            @Valid @RequestBody CanjeRequest request,
            @AuthenticationPrincipal UsuarioPrincipal usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuentaService.exchange(request, usuario));
    }
}
