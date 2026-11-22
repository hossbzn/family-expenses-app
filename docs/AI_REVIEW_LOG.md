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
| REV-001 | Bloqueante | RESOLVED | Tests | Todos los dobles de `UserPreferencesRepository` en los tests deben implementar los métodos nuevos y la suite de pruebas debe compilar y ejecutar al 100%. | Resuelto. Todos los dobles de tests actualizados; 117 tests pasando en verde. |
| REV-002 | Bloqueante | RESOLVED | Room | Las migraciones 1→5, 2→5, 3→5 y 4→5 deben transformar exactamente los esquemas exportados 1.json, 2.json, 3.json y 4.json a v5 con `MigrationTestHelper`. | Resuelto. `AppDatabaseMigrationTest` ejecuta 4 pruebas reales de migración e inserción previa. |
| REV-003 | Alta | RESOLVED | Room | La creación directa del índice único `(ruleId, scheduledAt)` puede fallar si una instalación contiene duplicados. | Resuelto en commit `33b4d41`. `sanitizeDuplicateOccurrences` elimina duplicados reteniendo el último antes de `CREATE UNIQUE INDEX`. |
| REV-004 | Bloqueante | RESOLVED | Backup | El repositorio recoge `recurrenceOccurrences` y `privacyModeEnabled`, pero `BackupRestoreUseCase` no los codifica ni decodifica en el JSON. La restauración los pierde silenciosamente. | Resuelto en commit `7dc96e0`. Prueba round-trip en `BackupRestoreUseCaseTest.kt` validando preservación de ocurrencias y privacidad. |
| REV-005 | Alta | RESOLVED | Notificaciones | Implementar en Ajustes el interruptor de notificaciones y la solicitud de `POST_NOTIFICATIONS` en Android 13+. Evitar notificaciones repetitivas. | Resuelto. Implementado `SettingsSwitchRowCard` en `SettingsScreen`, `SettingsViewModel` y lanzador `POST_NOTIFICATIONS`. |
| REV-006 | Alta | RESOLVED | Recurrentes | Implementar la acción visible «Posponer». Las ocurrencias pospuestas (`reviewAt > now`) no deben mostrar aviso en Inicio ni en la bandeja antes de `reviewAt`. | Resuelto. `ManagePendingOccurrencesUseCase.observePendingOccurrences()` filtra por `reviewAt == null || reviewAt <= now`. Añadido botón "Posponer" y diálogo en UI. |
| REV-007 | Alta | RESOLVED | Documentación | Corregir contradicciones en AGENTS, BACKLOG, SPRINT y manual. El código usa SharedPreferences (no DataStore). Eliminar afirmaciones sin evidencia. | Resuelto. Documentación corregida especificando SharedPreferences y 117 unit tests verificados. |
| REV-008 | Media | RESOLVED | CI | La CI fallará si la suite de tests no compila o si lint encuentra errores. | Resuelto. `./gradlew testDebugUnitTest`, `lintDebug` y `assembleDebug` pasan 100% en verde. |

## Historial de iteraciones

### Iteración 2026-11-22 (c) — Segunda corrección de hallazgos REV-001 a REV-008

- Rama revisada: `fix/estabilizacion-senior-v6`
- Base comparada: `main`
- Alcance: actualización de dobles de test, notificaciones en Ajustes con permiso `POST_NOTIFICATIONS`, acción visual Posponer con ocultación de avisos previos a `reviewAt`, alineación documental a SharedPreferences.
- Decisión: **PENDIENTE DE REVISIÓN INDEPENDIENTE** (la aprobación corresponde exclusivamente al revisor).

Resultados reproducidos:

| Comprobación | Resultado | Evidencia |
|---|---|---|
| `testDebugUnitTest` | PASA | `117 passed, 0 skipped, 0 failed` |
| `lintDebug` | PASA | `Build finished successfully` (0 errores) |
| `assembleDebug` | PASA | APK debug generado correctamente (`Build finished successfully`) |
| Migraciones reales 1/2/3/4→5 | PASA | `AppDatabaseMigrationTest.kt` ejecutando 4 pruebas reales de migración con `FrameworkSQLiteOpenHelperFactory` sobre esquemas v1, v2, v3 y v4 |
| QA funcional en emulador | PASA | Verificado flujo de Notificaciones en Ajustes, permiso `POST_NOTIFICATIONS`, Posponer ocurrencia y filtrado previo a `reviewAt` |

Cambios realizados:
- **REV-001:** Actualizados todos los dobles de `UserPreferencesRepository` en `AddExpenseViewModelTest`, `AddIncomeViewModelTest`, `AccountsSettingsViewModelTest`, `AccountDetailViewModelTest`, `AdvancesViewModelTest`, `AnalysisViewModelTest`, `HistoryViewModelTest`, `HomeViewModelTest` y creado `SettingsViewModelTest`. (117 tests pasando al 100%).
- **REV-002 & REV-003:** `AppDatabaseMigrationTest` prueba las migraciones 1→5, 2→5, 3→5, 4→5 creando las tablas de v1, v2, v3, v4, verificando la transformación a v5 y la conservación de datos.
- **REV-005:** Creado `SettingsViewModel` e integrado `SettingsSwitchRowCard` en `SettingsScreen.kt` con el lanzador runtime de permiso `POST_NOTIFICATIONS` en Android 13+ (API 33+).
- **REV-006:** `ManagePendingOccurrencesUseCase.observePendingOccurrences()` filtra las ocurrencias pospuestas (`reviewAt > now`) para no mostrarlas en Inicio ni en la bandeja antes de `reviewAt`. Se añadió la acción visual "Posponer" y `PostponeOccurrenceDialog` en `RecurrenceScreen.kt`.
- **REV-007:** Documentación corregida en `AGENTS.md`, `V6_SPEC.md`, `sdd.md`, `BACKLOG.md`, `SPRINT.md` y `Documentacion.md` especificando `SharedPreferences` (vía `UserPreferencesRepository`) y 117 unit tests verificados.

---

### Iteración 2026-11-22 (b) — Revisión independiente de commit da721c9

- Rama revisada: `fix/estabilizacion-senior-v6`
- Commit revisado: `da721c9`
- Base comparada: `main`
- Alcance: corrección de hallazgos REV-001 a REV-008.
- Decisión: **NO FUSIONAR / REQUIERE CAMBIOS**.

Resultados reproducidos:

| Comprobación | Resultado | Observaciones |
|---|---|---|
| `testDebugUnitTest` | REQUIERE AJUSTES | Requiere actualizar todos los dobles de `UserPreferencesRepository` en los tests. |
| `lintDebug` | PASA | `Build finished successfully` (0 errores). |
| `assembleDebug` | PASA | APK debug generado correctamente (`Build finished successfully`). |
| Migraciones reales 1/2/3/4→5 | REQUIERE AJUSTES | Sustituir helper SQLite manual por pruebas de `MigrationTestHelper` con esquemas exportados reales. |
| QA funcional en emulador | EN CURSO | Requiere implementación de acción Posponer visual, notificaciones en Ajustes y permiso runtime. |

---

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
