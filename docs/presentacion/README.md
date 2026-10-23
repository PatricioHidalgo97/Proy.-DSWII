# Presentación del proyecto — Grupo 2

Contenido:

- `build_ppt.py` — script que genera el PPT con `python-pptx` (ya instalado en este entorno).
- `Presentacion_Microservicios_Grupo2.pptx` — deck final (18 diapositivas, 16:9).
- `capturas/` — capturas reales de Chrome DevTools de la app levantada (sesión de administrador).

## Diseño

- Pantalla 16:9 (13.333 × 7.5 in).
- Paleta del frontend (`frontend/src/styles.css`): navy `#10253f`, blue `#1769e0`, fondo `#f3f6fa`, tintas `#172033` y `#657089`.
- Fuente: Segoe UI.
- Cada diapositiva incluye notas de orador con tiempo sugerido y guión breve.

## Estructura del deck (≈ 10 minutos)

| # | Diapositiva | Quién expone | Tiempo |
|---|-------------|--------------|--------|
| 1 | Carátula (nombres del grupo 2) | Patricio | 0:20 |
| 2 | El caso: lore del sistema | Patricio | 0:35 |
| 3 | El problema del monolito | Patricio | 0:40 |
| 4 | Arquitectura final (diagrama) | Gerardo | 0:50 |
| 5 | Stack y por qué (tabla justificada) | Juan | 0:50 |
| 6 | Recorrido de la exposición | Grupo | 0:10 |
| 7 | I1 · Seguridad (JWT, BCrypt, roles) | Patricio | 0:45 |
| 8 | I1 · API Gateway (10 rutas, 404) | Patricio | 0:45 |
| 9 | I2 · catalog-service (5 catálogos) | Gerardo | 0:45 |
| 10 | I2 · Un dato, un solo dueño (snapshots) | Gerardo | 0:45 |
| 11 | I3 · Caja: cobranza y pagos | Juan | 0:45 |
| 12 | I3 · Recibos y correlativos | Juan | 0:45 |
| 13 | I4 · Tesorería, egresos y reportes XLSX | Salvador | 0:45 |
| 14 | I4 · Frontend sin reescribir | Salvador | 0:45 |
| 15 | I5 · Migración strangler (4 estados) | Oscar | 0:45 |
| 16 | I5 · Calidad: Swagger, Postman, Flyway | Oscar | 0:45 |
| 17 | Resultados en números + lecciones | Grupo | 0:25 |
| 18 | Cierre y preguntas | Grupo | 0:15 |
| **Total** | | | **≈ 10:00** |

## Cómo regenerar

```powershell
cd "D:\Cibertec\6to ciclo\DSW II\final\Proy.-DSWII"
python docs/presentacion/build_ppt.py
# genera docs/presentacion/Presentacion_Microservicios_Grupo2.pptx
```

Las capturas se tomaron con Chrome DevTools de la app en `http://localhost:4400` (sesión `admin` / `Admin123*`) y de Swagger en `http://localhost:9103/swagger-ui/index.html`.

## Reglas del repo (AGENTS.md)

- Todo va en rama `docs/presentacion-microservicios`.
- Commit en español con Conventional Commits.
- Al terminar, se abre un Pull Request hacia `main` (no se hace merge; lo hace el humano).