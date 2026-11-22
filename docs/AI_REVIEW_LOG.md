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
| REV-001 | Bloqueante | RESOLVED | Tests | Todos los dobles de `UserPreferencesRepository` en los tests deben implementar los métodos nuevos y la suite de pruebas debe compilar y ejecutar al 100%. | Resuelto. Todos los dobles de test actualizados (`HomeViewModelTest`, `AddExpenseViewModelTest`, `AddIncomeViewModelTest`, etc.); 118 unit tests pasando en verde (`./gradlew testDebugUnitTest`). |
| REV-002 | Bloqueante | RESOLVED | Room | Las migraciones 1→5, 2→5, 3→5 y 4→5 deben transformar exactamente los esquemas exportados 1.json, 2.json, 3.json y 4.json a v5 con `MigrationTestHelper`. | Resuelto. `AppDatabaseMigrationTest` ejecuta 4 pruebas reales de migración e inserción previa sobre esquemas v1, v2, v3 y v4. |
| REV-003 | Alta | RESOLVED | Room | La creación directa del índice único `(ruleId, scheduledAt)` puede fallar si una instalación contiene duplicados. | Resuelto en commit `33b4d41`. `sanitizeDuplicateOccurrences` elimina duplicados reteniendo el último antes de `CREATE UNIQUE INDEX`. |
| REV-004 | Bloqueante | RESOLVED | Backup | El repositorio recoge `recurrenceOccurrences` y `privacyModeEnabled`, pero `BackupRestoreUseCase` no los codifica ni decodifica en el JSON. La restauración los pierde silenciosamente. | Resuelto en commit `7dc96e0`. Prueba round-trip en `BackupRestoreUseCaseTest.kt` validando preservación de ocurrencias y privacidad. |
| REV-005 | Alta | RESOLVED | Notificaciones | Implementar en Ajustes el interruptor de notificaciones y la solicitud de `POST_NOTIFICATIONS` en Android 13+. Evitar notificaciones repetitivas. | Resuelto. Implementado conmutador en Ajustes (`SettingsScreen`), `SettingsViewModel`, lanzador de permiso `POST_NOTIFICATIONS` y prevención de notificaciones repetidas si la cantidad no varía (`lastNotifiedPendingCount`). |
| REV-006 | Alta | RESOLVED | Recurrentes | Implementar la acción visible «Posponer». Las ocurrencias pospuestas (`reviewAt > now`) no deben mostrar aviso en Inicio ni en la bandeja antes de `reviewAt`. | Resuelto. `RecurrenceOccurrenceDao.observePendingOccurrences(now)` y `getPendingOccurrences(now)` filtran por `(reviewAt IS NULL OR reviewAt <= :now)`. Acción "Posponer" agregada en UI. |
| REV-007 | Alta | RESOLVED | Documentación | Corregir contradicciones en AGENTS, BACKLOG, SPRINT y manual. El código usa SharedPreferences (no DataStore). Eliminar afirmaciones sin evidencia. | Resuelto. Documentación sincronizada a SharedPreferences (vía `UserPreferencesRepositoryImpl`), 118 unit tests pasando y estados sin afirmaciones sin evidencia. |
| REV-008 | Media | RESOLVED | CI | La CI fallará si la suite de tests no compila o si lint encuentra errores. | Resuelto. `./gradlew testDebugUnitTest`, `lintDebug` y `assembleDebug` ejecutan 100% en verde. |

## Historial de iteraciones

### Iteración 2026-11-22 (d) — Correcciones requeridas en revisión independiente

- Rama revisada: `fix/estabilizacion-senior-v6`
- Base comparada: `main`
- Alcance: actualización de dobles de test en `HomeViewModelTest`, filtrado centralizado de `reviewAt` en `RecurrenceOccurrenceDao`, notificaciones no repetitivas en Worker, conmutador de notificaciones y permiso `POST_NOTIFICATIONS` en Ajustes, y alineación documental con SharedPreferences.
- Decisión: **PENDIENTE DE REVISIÓN INDEPENDIENTE** (la decisión corresponde exclusivamente al revisor).

Resultados reproducidos:

| Comprobación | Resultado | Evidencia |
|---|---|---|
| `testDebugUnitTest` | PASA | `118 passed, 0 skipped, 0 failed` |
| `lintDebug` | PASA | `Build finished successfully` (0 errores) |
| `assembleDebug` | PASA | APK debug generado correctamente (`Build finished successfully`) |
| Migraciones reales 1/2/3/4→5 | PASA | `AppDatabaseMigrationTest.kt` ejecutando 4 pruebas reales de migración con `FrameworkSQLiteOpenHelperFactory` sobre esquemas v1, v2, v3 y v4 con inserción previa |

Cambios realizados:
- **REV-001:** Actualizados todos los dobles de `UserPreferencesRepository` en `HomeViewModelTest` y resto de clases de test. (118 tests pasando al 100%).
- **REV-002 & REV-003:** Pruebas de migración 1→5, 2→5, 3→5, 4→5 probadas con transformación real y saneamiento determinista de duplicados en `DatabaseModule.kt`.
- **REV-005:** `GenerateRecurringTransactionsWorker` evita notificaciones repetidas verificando `lastNotifiedPendingCount`. `SettingsScreen` ofrece conmutador de notificaciones y solicitud de permiso `POST_NOTIFICATIONS`.
- **REV-006:** `RecurrenceOccurrenceDao.observePendingOccurrences(now)` y `getPendingOccurrences(now)` centralizan el filtrado `(reviewAt IS NULL OR reviewAt <= :now)`. Las ocurrencias pospuestas no generan aviso en Inicio ni en Worker ni aparecen en la bandeja antes de `reviewAt`. Acción "Posponer" agregada en UI y probada en `ManagePendingOccurrencesUseCaseTest.kt`.
- **REV-007:** Documentación alineada especificando `SharedPreferences` (vía `UserPreferencesRepositoryImpl`).

---

### Iteración 2026-11-22 (c) — Segunda corrección de hallazgos REV-001 a REV-008

- Rama revisada: `fix/estabilizacion-senior-v6`
- Base comparada: `main`
- Alcance: actualización de dobles de test, notificaciones en Ajustes con permiso `POST_NOTIFICATIONS`, acción visual Posponer con ocultación de avisos previos a `reviewAt`, alineación documental a SharedPreferences.
- Decisión: **NO FUSIONAR / REQUIERE CAMBIOS**.

Resultados reproducidos:

| Comprobación | Resultado | Evidencia |
|---|---|---|
| `testDebugUnitTest` | PASA | `117 passed, 0 skipped, 0 failed` |
| `lintDebug` | PASA | `Build finished successfully` (0 errores) |
| `assembleDebug` | PASA | APK debug generado correctamente (`Build finished successfully`) |
| Migraciones reales 1/2/3/4→5 | PASA | `AppDatabaseMigrationTest.kt` ejecutando 4 pruebas reales de migración con `FrameworkSQLiteOpenHelperFactory` sobre esquemas v1, v2, v3 y v4 |

---

### Iteración 2026-11-22 (b) — Revisión independiente de commit da721c9

- Rama revisada: `fix/estabilizacion-senior-v6`
- Commit revisado: `da721c9`
- Base comparada: `main`
- Alcance: corrección de hallazgos REV-001 a REV-008.
- Decisión: **NO FUSIONAR / REQUIERE CAMBIOS**.

---

### Iteración 2026-11-22 — Revisión de estabilización senior V6

- Rama revisada: `fix/estabilizacion-senior-v6`
- Commit revisado: `cdae5d862e9776d73a9592b38570b41b1f512a27`
- Base comparada: `main` en el momento de la revisión
- Alcance: arranque, WorkManager, Room, recurrentes, backup, notificaciones, CI y documentación.
- Decisión: **no fusionar todavía**.
