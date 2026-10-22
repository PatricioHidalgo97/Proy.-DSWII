# ESP-01 — Arquitectura de microservicios

## 1. Decisión

El sistema se divide en **tres microservicios** más un **API Gateway**:

| ID | Servicio | Responsabilidad | Puerto por defecto |
|---|---|---|---|
| SVC-AUTH | `identity-service` | Login, emisión de JWT, usuarios y roles | 9102 |
| SVC-CATALOG | `catalog-service` | Socios, giros, puestos, servicios y bancos | 9103 |
| SVC-CASH | `cash-service` | Cuentas por cobrar, recibos, ingresos, canjes, egresos y reportes | 9104 |
| SVC-GATEWAY | `api-gateway` | Punto de entrada único y enrutamiento | 9101 |

> Los puertos exactos, sus variables de entorno y la justificación de cada uno
> están en **ESP-02**. Aquí se nombran solo como referencia.

## 2. Diagrama de desplazamiento

```text
                        +---------------------------+
   Navegador  --------->|  api-gateway  :9101        |
   Angular              |  (Spring Cloud Gateway)    |
                        +-------------+-------------+
                                      |
            +-------------------------+-------------------------+
            |                         |                         |
            v                         v                         v
   +------------------+     +------------------+     +------------------+
   | identity-service |     | catalog-service  |     |  cash-service    |
   |      :9102       |     |      :9103       |     |      :9104       |
   +--------+---------+     +--------+---------+     +--------+---------+
            |                        |                        |
            v                        v                        v
     gestion_auth            gestion_catalog            gestion_cash
     (MySQL)                 (MySQL)                    (MySQL)

                                      ^
                                      |
                          cash-service consulta catálogo
                          (solo lecturas, con el JWT del usuario)
```

## 3. Justificación del corte

El criterio no fue “una entidad por servicio”, sino **agrupar casos de uso con
la misma razón para cambiar**.

### 3.1 Por qué identidad es un servicio aparte

- Tiene un ciclo de vida propio: usuarios, credenciales y roles.
- Es el único que emite tokens; los demás solo los validan.
- No comparte tablas con nadie: un usuario no “pertenece” a un socio ni a una
  cuenta.
- Aislarlo evita mezclar seguridad con negocio, que es el error más común al
  partir un monolito.

### 3.2 Por qué catálogo conserva cinco entidades juntas

- `Puesto` depende obligatoriamente de `Giro` y opcionalmente de `Socio`.
  Separarlos obligaría a resolver esa relación por HTTP en cada lectura.
- `Servicio` y `Banco` son catálogos referenciales que solo se consultan y se
  administran; no participan en transacciones.
- Todas comparten el mismo perfil de uso: CRUD de baja frecuencia y muchas
  lecturas.
- Mantener las claves foráneas dentro de una sola base elimina joins
  distribuidos.

### 3.3 Por qué caja agrupa cuentas, recibos, egresos y reportes

- Todo lo que hay en caja **mueve o documenta dinero**.
- Un recibo nace únicamente del pago de una cuenta o de un ingreso externo.
- Los correlativos son un recurso compartido entre recibos y egresos; si se
  separan, hay que sincronizar secuencias entre servicios.
- Los reportes agregan recibos, egresos, socios y bancos; el 90 % de su insumo
  ya está en caja.
- Separar cobranza de tesorería partiría el ciclo de vida de un documento
  financiero y multiplicaría las llamadas remotas.

## 4. Dirección de las dependencias

| Origen | Destino | Tipo | Motivo |
|---|---|---|---|
| Frontend | Gateway | HTTP | Único punto de entrada |
| Gateway | Los tres servicios | HTTP | Enrutamiento por prefijo |
| Caja | Catálogo | HTTP (GET) | Validar y copiar datos de catálogo |
| Caja | Identidad | Ninguna | Los datos del usuario vienen en el JWT |
| Catálogo | Caja | Ninguna | Prohibido: sería dependencia circular |
| Identidad | Cualquiera | Ninguna | No conoce el negocio |

**Regla:** las dependencias apuntan siempre hacia catálogo o identidad, nunca
al revés.

## 5. Qué no se convirtió en servicio

| Candidato | Decisión | Motivo |
|---|---|---|
| Reportes | No | Consume datos de caja y catálogo; separarlo agrega latencia sin beneficio |
| Bancos | No | Es un catálogo referencial; no hay integración bancaria real |
| Correlativos | No | Es una tabla interna de caja; exponerla crearía acoplamiento |
| “Servicio de notificaciones” | No | No existe requisito de notificar |
| “Servicio de auditoría” | No | Fuera del alcance académico actual |

## 6. Características de la arquitectura

| Característica | Estado | Nota |
|---|---|---|
| Servicios independientes | Sí | Cada uno con su propio arranque y su propia base |
| Base de datos por servicio | Sí | Tres esquemas en una misma instancia MySQL |
| Comunicación síncrona | Sí | REST con JWT propagado |
| Descubrimiento de servicios | No | URIs fijas configurables por entorno |
| Mensajería asíncrona | No | No hay eventos de negocio que lo requieran |
| Transacciones distribuidas | No | No existen escrituras en dos bases |
| Tolerancia a fallos de catálogo | Parcial | Caja opera con snapshots; las altas que requieren catálogo fallan con 503 |
| Contrato versionado | Sí | OpenAPI exportado por servicio |

## 7. Consecuencias aceptadas

1. **Caja es el servicio más grande.** Es una consecuencia deliberada de
   mantener la cohesión del dinero. Si en el futuro crece, el punto natural de
   corte es separar tesorería (egresos) de cobranza.
2. **Hay datos duplicados en caja.** Los snapshots son intencionales y
   beneficiosos para documentos históricos.
3. **Un fallo de catálogo degrada parcialmente caja.** Las consultas históricas
   siguen funcionando; las altas que necesitan catálogo no.
4. **No hay escalado independiente real en un entorno local.** La separación es
   lógica y de despliegue, no de infraestructura elástica.
