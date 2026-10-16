# Sistema de Gestión Administrativa y de Caja

Proyecto académico de Desarrollo de Aplicaciones Web I desarrollado con Angular, Spring Boot, JPA, JWT/BCrypt y MySQL. Implementa el flujo administrativo solicitado sin pasarela de pagos ni conexión con bancos reales.

## Funcionalidades implementadas

- Login, cierre de sesión, JWT, BCrypt, rutas protegidas y roles `ADMIN`/`OPERADOR`.
- Identidad y rol del usuario visibles en la cabecera.
- CRUD de socios, giros, puestos, servicios y bancos.
- Puestos con socio opcional y giro obligatorio.
- Servicios destinados a puesto o socio, con recurrencia, moneda y dependencia de consumo.
- Cuentas individuales y masivas por puestos o socios; filtros por etapa y socios únicos.
- Lectura inicial/final, costo unitario y cálculo automático del consumo.
- Consulta de cuentas por socio o puesto, estados y resumen independiente.
- Selección de varias cuentas, total seleccionado, pago o exoneración en lote.
- Recibos correlativos transaccionales y voucher detallado.
- Canje bancario simulado e ingresos externos. No mueve dinero ni se conecta a terceros.
- Egresos individuales, importación CSV, procesamiento, anulación y comprobante correlativo.
- Reportes XLSX diarios, mensuales, de socios, egresos y bancos.
- Colección Postman, pruebas automáticas y perfil H2 para demostración.

## Estructura

```text
sistema-gestion-4-personas/
|- backend/       API REST, seguridad, reglas de negocio y pruebas
|- frontend/      aplicación Angular y pantallas
|- database/      creación de la base MySQL
|- docs/          informe, Postman, plantilla CSV y reparto del equipo
`- README.md
```

## Requisitos

- Java 21 y Maven 3.9+
- Node.js 22.12+ y npm 10+
- MySQL 8, salvo que se use el perfil demostrativo H2

## Ejecución rápida sin MySQL

Terminal 1:

```powershell
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Terminal 2:

```powershell
cd frontend
npm install
npm start
```

Abrir `http://localhost:4200`. El backend usa `http://localhost:9090`.

## Ejecución con MySQL

1. Ejecutar `database/crear_base.sql`.
2. Iniciar el backend con las credenciales:

```powershell
$env:DB_USERNAME='gestion_user'
$env:DB_PASSWORD='Gestion2026*'
cd backend
mvn spring-boot:run
```

## Usuarios iniciales

| Rol | Usuario | Contraseña | Uso |
|---|---|---|---|
| Administrador | `admin` | `Admin123*` | Catálogos, anulaciones y operaciones |
| Operador | `caja` | `Caja123*` | Consultas, caja, ingresos, egresos y reportes |

Ambas contraseñas se guardan con BCrypt. En un entorno distinto deben definirse mediante `ADMIN_*` y `OPERATOR_*`.

## Importación de egresos

Usar un CSV UTF-8 separado por punto y coma. Se incluye `docs/plantilla_egresos.csv`.

```text
tipoDocumento;numeroDocumento;proveedor;fecha;subtotal;impuesto;total;motivo;documentoAsociado
FACTURA;F001-100;Proveedor Demo;2026-08-19;100.00;18.00;118.00;Útiles de oficina;OC-001
```

## Pruebas y compilación

```powershell
cd backend
mvn test

cd ../frontend
npm run build
```

## Secuencia sugerida de demostración

1. Iniciar sesión; la aplicación abre Recibos.
2. Registrar socio, giro y puesto; mostrar que el socio del puesto es opcional.
3. Registrar un servicio fijo y otro por consumo.
4. Generar cargos individuales y masivos para puestos y socios.
5. Seleccionar varias cuentas y mostrar el total; pagar o exonerar el lote.
6. Abrir el voucher emitido y consultar el resumen por destinatario.
7. Registrar un banco referencial, un canje simulado y un ingreso externo.
8. Registrar e importar egresos, procesar/anular y descargar XLSX.

## Alcance académico y seguridad

El módulo “bancario” solo registra datos digitados por el usuario. No existe pasarela, webhooks, lectura de movimientos reales ni almacenamiento de tarjetas. Los recibos bancarios son constancias internas de la simulación.

Hibernate actualiza el esquema durante el desarrollo (`ddl-auto=update`). Para producción se recomendarían migraciones versionadas, secretos externos, auditoría más detallada, copias de respaldo y pruebas de carga; esos puntos no son necesarios para esta entrega de instituto.

## Entregables

- `docs/Informe_Final_Proyecto_Sistema_Gestion.docx`
- `docs/Sistema_Gestion.postman_collection.json`
- `docs/DISTRIBUCION_EQUIPO.md`
- `docs/plantilla_egresos.csv`
- `docs/evidencias/`

El video Demo Reel fue omitido por indicación expresa del solicitante.
