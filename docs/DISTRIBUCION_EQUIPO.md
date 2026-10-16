# Distribución equilibrada para cuatro integrantes

Cada integrante tiene ocho responsabilidades concretas: cinco técnicas, una prueba, una parte del informe y una parte de la exposición. La carga busca ser equivalente en cantidad y complejidad.

## Integrante 1 — Seguridad, socios y calidad base

1. Explicar `UsuarioEntity`, roles `ADMIN`/`OPERADOR` y usuarios iniciales.
2. Demostrar login correcto/incorrecto, BCrypt y emisión JWT.
3. Explicar filtro JWT, guard, interceptor, CORS y cierre de sesión.
4. Presentar CRUD de socios, validaciones, etapas y socio activo.
5. Explicar correlativos transaccionales y restricciones por rol.
6. Ejecutar y explicar pruebas de autenticación y repositorio de socios.
7. Redactar seguridad, arquitectura por capas y requisitos RF-01 a RF-05.
8. Exponer durante 4–5 minutos y responder preguntas de seguridad.

## Integrante 2 — Catálogos, puestos y servicios

1. Presentar CRUD de giros y sus validaciones de duplicidad.
2. Presentar CRUD de puestos y relación opcional con socio.
3. Explicar relación obligatoria Puesto–Giro y fechas de vigencia.
4. Presentar CRUD de servicios, recurrencia, moneda y destino del cargo.
5. Explicar servicio fijo frente a servicio dependiente de consumo.
6. Probar alta/edición y errores de giros, puestos y servicios en Postman.
7. Redactar modelo de datos y requisitos RF-06 a RF-15 y RF-17.
8. Exponer durante 4–5 minutos con un caso de consumo calculado.

## Integrante 3 — Cuentas, cobranza y recibos

1. Generar cuenta individual por puesto y por socio.
2. Generar cuentas masivas, filtrar por etapa y explicar socios únicos.
3. Consultar cuentas por puesto/socio y mostrar el resumen consolidado.
4. Seleccionar múltiples deudas y explicar el total seleccionado.
5. Demostrar pago/exoneración individual y en lote con transacción.
6. Ejecutar pruebas de cuentas y verificar recibo correlativo/voucher.
7. Redactar requisitos RF-16, RF-18 a RF-23, RF-26 y RF-29.
8. Exponer durante 4–5 minutos el flujo completo de cobranza.

## Integrante 4 — Tesorería, egresos, reportes e integración

1. Presentar CRUD de bancos como catálogo referencial académico.
2. Demostrar canje bancario simulado y explicar que no hay pasarela.
3. Registrar ingresos externos y consultar recibos por tipo/fecha.
4. Registrar, importar CSV, procesar y anular egresos.
5. Descargar y explicar reportes XLSX diarios, mensuales y maestros.
6. Ejecutar compilación Angular, prueba integrada y colección Postman.
7. Redactar RF-24, RF-25, RF-27, RF-28, RF-30 a RF-33 y conclusiones.
8. Exponer durante 4–5 minutos, cerrar la demostración y presentar mejoras.

## Evidencia conjunta

```powershell
cd backend
mvn test

cd ../frontend
npm run build
```

Todos deben conocer el flujo general, pero cada uno demuestra sus ocho puntos. El video Demo Reel se omite por decisión del solicitante.
