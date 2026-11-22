
# Registro de revisiones y feedback de agentes

## Propósito

Este documento conserva el feedback técnico entre iteraciones de desarrollo. Todo agente que analice o modifique el proyecto debe leerlo después de `AGENTS.md` y antes de empezar a trabajar.

No sustituye a las especificaciones, al backlog ni al sprint. Sirve como historial verificable de hallazgos, correcciones intentadas y resultados de validación.

## Reglas de mantenimiento

1. No borrar ni reescribir revisiones anteriores. Las correcciones se registran como una nueva iteración.
2. Cada hallazgo mantiene un identificador estable (`REV-xxx`).
3. Estados permitidos: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `ACCEPTED_RISK` y `OBSOLETE`.
4. Un hallazgo solo pasa a `RESOLVED` con evidencia reproducible: commit, prueba y resultado del comando de validación.
5. Compilar no demuestra que una funcionalidad esté terminada. Documentar por separado tests, lint, build y verificación funcional.
6. Si una afirmación de la documentación no coincide con el código o las pruebas, prevalece la evidencia y se registra la contradicción.
7. Al terminar una iteración, añadir una entrada nueva al principio de «Historial de iteraciones» y actualizar la tabla de hallazgos sin eliminar su contexto.

## Estado consolidado de hallazgos

| ID | Severidad | Estado | Área | Hallazgo | Evidencia o criterio de cierre |
|---|---|---|---|---|---|
| REV-001 | Bloqueante | OPEN | Tests | `testDebugUnitTest` no compila porque el falso `RecurrenceOccurrenceDao` de `HomeViewModelTest` no implementa `getAllOccurrences()`. | La suite debe compilar y pasar completa. |
| REV-002 | Bloqueante | OPEN | Room | Las migraciones 1→5, 3→5 y 4→5 no transforman los esquemas históricos al esquema v5. La prueba denominada de migración solo crea una base nueva en memoria. | Pruebas reales desde cada esquema exportado, preservando datos y validando el esquema final con Room. |
| REV-003 | Alta | OPEN | Room | La creación directa del índice único `(ruleId, scheduledAt)` puede fallar si una instalación contiene duplicados. | Definir y probar una estrategia determinista de detección/saneamiento previa al índice. |
| REV-004 | Bloqueante | OPEN | Backup | El repositorio recoge `recurrenceOccurrences` y `privacyModeEnabled`, pero `BackupRestoreUseCase` no los codifica ni decodifica en el JSON. La restauración los pierde silenciosamente. | Prueba round-trip que exporte, importe y compare ocurrencias y privacidad. |
| REV-005 | Alta | OPEN | Notificaciones | `NotificationHelper` no está conectado al Worker ni a otro flujo; tampoco existe solicitud de permiso ni preferencia configurable. | Prueba del flujo completo y verificación funcional con permiso concedido y denegado. |
| REV-006 | Alta | OPEN | Recurrentes | Posponer guarda `reviewAt`, pero las consultas y la interfaz siguen usando `scheduledAt`; la acción no produce el comportamiento esperado. | Consultas, UI y pruebas deben respetar `reviewAt` sin modificar la fecha económica. |
| REV-007 | Alta | OPEN | Documentación | BACKLOG/SPRINT/AGENTS contienen estados y cifras incompatibles: se declara todo completado y tests en verde mientras quedan tareas TODO y la suite no compila. También se documenta DataStore cuando el código usa SharedPreferences. | Alinear documentación exclusivamente con resultados comprobados. |
| REV-008 | Media | OPEN | CI | La CI está bien planteada, pero fallará en `testDebugUnitTest` mientras REV-001 siga abierto. | Ejecución verde en GitHub Actions del mismo commit revisado. |

## Historial de iteraciones

### Iteración 2026-11-22 — Revisión de estabilización senior V6

- Rama revisada: `fix/estabilizacion-senior-v6`
- Commit revisado: `cdae5d862e9776d73a9592b38570b41b1f512a27`
- Base comparada: `main` en el momento de la revisión
- Alcance: arranque, WorkManager, Room, recurrentes, backup, notificaciones, CI y documentación.
- Decisión: **no fusionar todavía**.

Resultados reproducidos:

| Comprobación | Resultado | Observaciones |
|---|---|---|
| `testDebugUnitTest` | FALLA | Error de compilación en `HomeViewModelTest`: falta `getAllOccurrences()`. No se ejecutó la suite. |
| `lintDebug` | PASA | Sin errores; 8 advertencias. |
| `assembleDebug` | PASA | APK debug generado correctamente. |
| Migraciones reales 1/2/3/4→5 | NO VALIDADAS | El test añadido no abre ni migra bases históricas. |
| QA funcional en emulador | NO ACREDITADA | No se aportó evidencia reproducible para este commit. |

Aspectos aprovechables de la iteración:

- Se eliminó `runBlocking` del arranque y se retiró el procesador recurrente antiguo duplicado.
- La inicialización de WorkManager con `HiltWorkerFactory` quedó correctamente encaminada.
- Separar `reviewAt` de `scheduledAt` es una decisión de modelo adecuada.
- El índice único de ocurrencias es el objetivo correcto una vez saneados los datos existentes.
- La incorporación de CI es útil cuando la suite vuelva a estar verde.

## Plantilla para la siguiente iteración

Copiar este bloque al principio de «Historial de iteraciones»:

```markdown
### Iteración AAAA-MM-DD — Título breve

- Rama revisada:
- Commit revisado:
- Objetivo:
- Hallazgos abordados: REV-...
- Decisión: APROBADA / REQUIERE CAMBIOS / NO FUSIONAR

| Comprobación | Resultado | Evidencia |
|---|---|---|
| `testDebugUnitTest` | | |
| `lintDebug` | | |
| `assembleDebug` | | |
| Migraciones | | |
| QA funcional | | |

Cambios realizados:

- ...

Pendientes y riesgos:

- ...
```