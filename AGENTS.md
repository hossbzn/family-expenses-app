# AGENTS.md — Family Expenses V6

## Misión
Evolucionar y estabilizar la aplicación Android Kotlin/Jetpack Compose hacia **Family Expenses V6**. La fase presente es de **estabilización senior, migraciones Room explícitas, QA automatizada y alineación visual**.

## Fuentes de verdad — rutas y precedencia
1. `docs/V6_SPEC.md`: decisiones funcionales y visuales aprobadas V6.
2. `docs/sdd.md`: especificación técnica y criterios V6 alineados con la anterior.
3. `docs/BACKLOG.md`: historias y estados V6.
4. `docs/SPRINT.md`: plan de trabajo y autorización de implementación.
5. `docs/RECURRENTES_V6_1.md`: especificación funcional y técnica del motor de recurrentes V6.1.
6. `docs/Documentacion.md`: manual de comportamiento **objetivo** de la aplicación.
7. `docs/archive/MVP_LEGACY.md`: referencia histórica únicamente. **No aplicar reglas heredadas incompatibles**.

Si documentos V6 divergen, detenerse y proponer corrección. No declarar una historia terminada solo porque compile. Marcar DONE únicamente tras cumplir la implementación, pruebas automatizadas, `testDebugUnitTest`, `lintDebug`, `assembleDebug` y verificación funcional.
## Registro obligatorio de feedback

Después de leer este archivo, leer siempre `docs/AI_REVIEW_LOG.md`. Ese documento conserva los hallazgos y resultados de validación entre iteraciones.

- No borrar ni reescribir entradas históricas.
- Antes de trabajar, identificar los hallazgos `OPEN` relacionados con la tarea.
- Mantener sus identificadores `REV-xxx` en commits, informes o pull requests cuando corresponda.
- No marcar un hallazgo `RESOLVED` sin commit y evidencia reproducible de pruebas.
- Al terminar una iteración de corrección o revisión, añadir su resultado al historial aunque la solución haya fallado.
## Reglas contables INNEGOCIABLES
- Dos cuentas visibles: Familia y Personal; modelo ampliable.
- Gasto: `payingAccountId` (dinero que sale) y `responsibleAccountId` (imputación analítica), **ambos independientes**.
- El gasto descuenta únicamente el saldo real de la pagadora. El gasto cuenta analíticamente para responsable una sola vez.
- Diferentes cuentas generan adelanto de pagadora a responsable, también Familia→Personal.
- Deuda neta firmada y trazable; devoluciones **parciales o completas** mueven dinero de caja de cuenta deudora a acreedora, reducen deuda y NO crean ingreso/gasto estadístico.
- Transferencia normal y devolución son tipos seleccionados explícitamente, NUNCA compensar automáticamente.
- Ajuste de saldo registrado con motivo y fuera de estadísticas de gasto/ingreso.
- Cambiar/eliminar movimientos recalcula saldos y deuda preservando consistencia del historial.
- Importes EUR en céntimos como Long; no usar Float/Double para contabilidad.

## UX y Sistema Visual V6
- Sistema visual Material Design 3 con fondo neutro blanco roto (`#F8F9FA`), superficies en blanco puro (`#FFFFFF`) y **Azul Petróleo** (`#1A3644`) como color primario. Verde sobrio (`#2E7D32`) y rojo carmesí apagado (`#C62828`) como colores semánticos.
- Inicio: Mismo peso visual para tarjetas Familia y Personal, botones "Añadir ingreso" (`+`) a la izquierda y "Añadir gasto" (`−`) a la derecha, bloque de Adelantos visible, sección "Últimos movimientos" (3 ítems más recientes) con enlace "Ver todos", y botón de Modo de Privacidad en cabecera junto a Ajustes.
- Modo de Privacidad Global: Botón de ojo en cabecera con conmutador persistente en SharedPreferences (`UserPreferencesRepository`). Aplica la máscara `"•••• €"` a saldos, deudas, importes de movimientos y gráficos de Análisis sin alterar los datos contables de Room, CSV o copias JSON.
- Navegación inferior **Inicio / Movimientos / Análisis**; Ajustes en cabecera.
- Nuevo gasto: teclado propio con coma/C/retroceso; 7 favoritas manuales + Más; **dos selectores** pagadora/responsable; última categoría y combinación, fecha hoy, detalles plegados, Guardar en un paso.
- Nuevo ingreso: categorías en lista manual sin favoritas.
- Movimientos: dos líneas por fila, filtro principal por cuenta responsable de gasto, periodos mes/12 meses/año/personalizado.
- Análisis: Categorías y Evolución, una serie Gastos o Ingresos, al entrar en Movimientos conservar filtros al volver.
- Cuenta: saldo real, transferir, movimientos, adelantos. Adelantos: saldo neto e historial único con filtros.
- Datos: CSV, copia completa/restauración portable JSON; local offline, sin PIN, nube ni login.

## Stack y reglas de arquitectura
- Kotlin, Jetpack Compose, Room (v5), Hilt, ViewModel + StateFlow, Navigation Compose, WorkManager.
- **ACCEPTED_RISK:** Dado que es una instalación desde cero (Esquema V6 limpio, sin datos históricos productivos en el MVP), se autoriza el uso de `fallbackToDestructiveMigration()` para descartar versiones de desarrollo (1-4).
- Prohibición de `runBlocking` en `Application.onCreate`. Toda inicialización o reconciliación inicial debe ser asíncrona e idempotente.
- Notificaciones de pendientes configurables en `UserPreferencesRepository` con permisos `POST_NOTIFICATIONS` y canal Android.

## Proceso obligatorio antes de DONE
1. Implementación funcional completa.
2. Pruebas unitarias/instrumentadas ejecutadas y en verde (`./gradlew testDebugUnitTest`).
3. Verificación de Android Lint sin errores (`./gradlew lintDebug`).
4. Compilación correcta del APK (`./gradlew assembleDebug`).
5. Verificación funcional interactiva en emulador.
6. Actualización coherente de la documentación sin contradicciones.
