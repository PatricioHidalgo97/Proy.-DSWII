# AGENTS.md — Reglas de trabajo para agentes de IA

Este repositorio usa un flujo basado en ramas y Pull Requests. **Lee estas reglas antes de tocar cualquier archivo.**

## Regla de oro: `main` es intocable

- **Nunca** hagas `commit`, `push` ni `merge` directamente sobre `main`.
- **Nunca** hagas `git push origin main`.
- **Nunca** hagas force-push sobre `main` ni sobre ramas ajenas.
- Todo cambio, por pequeño que sea (código, docs, configuración, refactor, fix de typo), va en su propia rama.

## Flujo obligatorio para un agente normal

1. **Sincroniza** desde `main` y crea una rama nueva con nombre descriptivo:
   - `feature/<tema-corto>` para funcionalidad nueva.
   - `fix/<tema-corto>` para correcciones.
   - `docs/<tema-corto>` para documentación.
   - `refactor/<tema-corto>`, `chore/<tema-corto>`, `test/<tema-corto>` según corresponda.
2. **Una tarea = una rama.** No mezcles cambios de tareas distintas en la misma rama.
3. **Commits** pequeños, en español, con Conventional Commits (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`).
4. Al terminar, **haz push de la rama** y **abre un Pull Request** hacia `main`:
   - Describe qué se cambió y por qué.
   - Indica cómo probarlo (comandos, endpoints, pasos manuales).
   - Menciona riesgos, migraciones, variables de entorno o cambios de contrato de API.
5. **No hagas merge.** El PR queda abierto esperando revisión humana.

## Ciclo de revisión (humano)

- El dueño del repositorio revisa el PR.
- Si pide cambios, **se siguen agregando commits a la misma rama** (sin abrir una rama nueva ni reescribir historia ya publicada).
- El **merge a `main` lo autoriza y ejecuta únicamente el humano**.

## Excepción: agente orquestador

Solo un agente que haya sido **explícitamente designado como orquestador** por el humano puede saltarse el paso 5. En ese caso:

- Crea la rama de trabajo y el PR igual que cualquier otro agente.
- Puede delegar el trabajo en otros agentes y **resolver los conflictos** con ellos.
- **Evalúa** el resultado (build, tests, criterios de aceptación) antes de integrar.
- Genera el PR y, si la evaluación es correcta, **hace el merge a `main`**.
- Luego continúa con el siguiente agente y la siguiente tarea.

Si no se te indicó explícitamente que eres el orquestador, **no eres el orquestador**: no hagas merge.

## Prohibiciones adicionales

- No reescribas historia (`rebase`, `amend`, `force-push`) en ramas ya publicadas o en PRs abiertos.
- No borres ramas ni PRs ajenos.
- No toques archivos no relacionados con la tarea asignada.
- No hagas commit de secretos, credenciales, `.env` reales ni artefactos de build.
- No modifiques la configuración de CI/CD, permisos o protección de ramas sin autorización explícita.

## Resumen en una línea

> Toda modificación nace en una rama, termina en un Pull Request, y solo el humano (o el orquestador designado) integra a `main`.
