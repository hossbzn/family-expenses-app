# SPRINT — Plan de Desarrollo Family Expenses V6

**Estado actual:** **Family Expenses V6 COMPLETADO (Sprints S1–S4 DONE)** — 97 pruebas unitarias en verde, `assembleDebug` correcto y QA visual/interactiva verificada en emulador (`emulator-5554`).
**Fuente de verdad:** [V6_SPEC.md](V6_SPEC.md) → [sdd.md](sdd.md) → [BACKLOG.md](BACKLOG.md).

## Sprint V6-S1: Motor Contable y Persistencia Limpia V6 (ESTADO: DONE)
**Meta:** Establecer el esquema Room V6 limpio, implementar el cálculo reactivo de saldos de caja reales y deuda neta, y blindar el motor con la suite completa de pruebas unitarias contables antes de construir la UI.

### Historias completadas:
- [x] **V6-12 — Esquema Room V6 e inicialización limpia:**
  - Entidades Room V6 creadas: `AccountEntity`, `CategoryEntity` (con 7 favoritas y orden), `TransactionEntity` (`payingAccountId`, `responsibleAccountId`), `TransferEntity` (`NORMAL`, `REIMBURSEMENT`), `BalanceAdjustmentEntity`, `RecurrenceRuleEntity`.
  - DAOs reactivos V6 creados con `Flow`.
  - Tablas obsoletas del MVP (`reimbursement_ledger`, `settlements`, `settlement_items`, `users`) eliminadas de la base de datos.
  - `DatabaseSeeder` V6 con cuentas y categorías iniciales configuradas (7 favoritas manuales de gasto).
  - `AppDatabase` v1 y módulo Hilt configurados limpiamente.
- [x] **V6-01 — Saldos de caja reales y dualidad pagadora/responsable:**
  - Implementado `ComputeAccountBalanceUseCase` (Caja real = inicial + ingresos - gastos pagados + transferencias netas + ajustes).
  - Implementado `AddExpenseUseCase` con dualidad de cuentas ortogonales.
  - Implementado `AddIncomeUseCase` para ingresos a caja destinataria.
  - Implementado `CreateBalanceAdjustmentUseCase` con motivo auditable obligatorio.
  - Verificado caso Personal paga 80 € de Familia: Personal -80 €, Familia 0 € en caja.
- [x] **V6-02 — Adelantos bidireccionales y transferencias/devoluciones:**
  - Implementado `ComputeNetDebtUseCase` (deuda neta en tiempo real con ambos signos y estado cero).
  - Implementado `ExecuteTransferUseCase` separando explícitamente transferencias normales de devoluciones de adelanto.
  - Verificada amortización de deuda en devoluciones parciales y tope por deuda pendiente.
- [x] **Adaptación de Recurrencias al modelo V6:**
  - `GenerateRecurringTransactionsUseCase` y `CreateMonthlyRecurringRuleUseCase` adaptados a `payingAccountId` y `responsibleAccountId`.
- [x] **Suite de pruebas contables:**
  - 30 pruebas unitarias y de integración automatizadas ejecutadas y pasando al 100% en verde.

---

## Sprint V6-S2: Registro Rápido e Inicio V6 (ESTADO: DONE)
**Meta:** Implementar el flujo principal de registro de gastos e ingresos y la pantalla de inicio monocromática MD3.
- [x] **V6-03:** Formulario de Nuevo gasto:
  - Teclado numérico propio (coma, 2 decimales, C, retroceso, importe inicial 0, guardar deshabilitado con 0 €).
  - 7 favoritas manuales + Más en cuadrícula 2x4; recordar última categoría y última pareja de cuentas con `UserPreferencesRepository`.
  - Selectores separados «Pagado desde» y «Gasto de», aviso de adelanto si difieren, fecha hoy por defecto y detalles plegados.
  - Guardado en un paso con opción de Deshacer en el Dashboard de destino y confirmación al descartar cambios con datos introducidos.
  - 14 pruebas unitarias de teclado (`NumericKeypadHandlerTest`) y ViewModel (`AddExpenseViewModelTest`) pasando al 100% (44 pruebas totales en el proyecto).
- [x] **V6-10:** Formulario de Nuevo ingreso:
  - Lista manual de categorías de ingreso (sin favoritas), cuenta destinataria («Ingresar en»), importe con teclado propio numérico, fecha y detalles opcionales plegados.
  - Recordar última cuenta destinataria y categoría de ingreso independientemente del gasto vía `UserPreferencesRepository`.
  - Guardado en un paso directo, opción de Deshacer en Inicio y confirmación al descartar cambios con datos introducidos.
  - 8 pruebas unitarias automatizadas (`AddIncomeViewModelTest`) al 100% en verde.
- [x] **V6-04:** Pantalla de Inicio V6:
  - Tarjetas de saldo de caja real con la misma jerarquía: Familia (negra sólida, texto blanco) y Personal (gris claro, borde y texto oscuro). Sin saldo agregado global.
  - Botones grandes: «Nuevo gasto» (principal negro) y «Nuevo ingreso» (secundario gris/borde).
  - Bloque de Adelantos siempre visible incluso en cero («Familia te debe X», «Personal debe a Familia X» o «Sin adelantos pendientes»).
  - Sin movimientos recientes ni totales del mes; barra inferior con tres destinos V6: Inicio, Movimientos, Análisis; botón de Ajustes en cabecera.
  - Soporte de Deshacer movimiento vía Snackbar reactivo.
  - 3 pruebas unitarias automatizadas (`HomeViewModelTest`) al 100% en verde (55 pruebas totales en el proyecto).
- [x] **V6-07:** Detalle de cuenta y formulario de Transferir (Transferencia normal vs. Devolución de adelanto):
  - Pantalla de detalle de cuenta (`AccountDetailScreen`) mostrando saldo de caja real, accesos directos a Transferir, Movimientos y Adelantos (sin lista de recientes).
  - Gestión de saldo: ajuste de saldo auditable con signo (+/-) y motivo obligatorio (`CreateBalanceAdjustmentUseCase`) y configuración de saldo inicial (`AccountRepository.updateInitialBalance`).
  - Formulario de Transferir (`TransferScreen`) con selección obligatoria y explícita entre «Transferencia normal» y «Devolución de adelanto».
  - En «Devolución de adelanto», precarga automática de dirección según deuda neta y saldo pendiente neto, editable hasta el máximo con validación en tiempo real; soporte para devoluciones parciales y totales.
  - Navegación conectada desde las tarjetas Familia y Personal en Inicio, con soporte de Deshacer transferencia vía Snackbar.
  - 12 nuevas pruebas unitarias (`AccountDetailViewModelTest`, `TransferViewModelTest`, `NumericKeypadHandlerTest`) al 100% en verde (67 pruebas totales en el proyecto).

---

## Sprint V6-S3: Movimientos, Análisis y Adelantos (ESTADO: DONE)
- [x] **V6-05:** Pantalla de Movimientos:
  - Modelo unificado (`HistoryItem`, `MovementKind`) combinando gastos, ingresos, transferencias normales y devoluciones en orden descendente agrupado por día, con filas compactas de dos líneas y sin resumen mensual.
  - Filtro principal Todas / Familia / Personal usando cuenta responsable para gastos, destinataria para ingresos y participación para transferencias/devoluciones (`FilterMovementsUseCase`).
  - Búsqueda de texto, filtros por tipo (Todos, Gastos, Ingresos, Adelantos como atributo sin duplicación, Transferencias, Devoluciones), categoría y periodos (Mes a mes, Últimos 12 meses, Año natural, Personalizado).
  - Detalle de movimiento (`MovementDetailScreen`) para edición y borrado con recálculo reactivo de saldos y deuda (`UpdateTransactionUseCase`, `UpdateTransferUseCase`), preservando filtros y scroll al volver.
  - 9 nuevas pruebas unitarias (`FilterMovementsUseCaseTest`, `HistoryViewModelTest`, `MovementDetailViewModelTest`) al 100% en verde (76 pruebas totales en el proyecto).
- [x] **V6-06:** Pantalla de Análisis:
  - Caso de uso `ComputeAnalysisUseCase` imputando gastos a `responsibleAccountId`, separando estrictamente `EXPENSES` vs. `INCOMES`, excluyendo transferencias/devoluciones/ajustes, preservando categorías archivadas y dejando periodos futuros vacíos (`null`, sin ceros ficticios).
  - Pantalla `AnalysisScreen` y `AnalysisViewModel` con pestañas Categorías (anillo monocromático y ranking por porcentaje/importe) y Evolución (barras diarias/mensuales/personalizadas de una sola serie).
  - Drill-down desde cualquier categoría o barra hacia `HistoryRoute` con cuenta, tipo, categoría y periodo aplicados, manteniendo el estado de `AnalysisViewModel` íntegro al volver.
  - 5 nuevas pruebas unitarias (`ComputeAnalysisUseCaseTest`, `AnalysisViewModelTest`, `HistoryViewModelTest`) al 100% en verde (84 pruebas totales en el proyecto).
- [x] **V6-08:** Pantalla de Adelantos:
  - Pantalla `AdvancesScreen` y `AdvancesViewModel` con tarjeta de saldo neto en ambas direcciones y estado cero visible («Familia debe a Personal», «Personal debe a Familia» o «Sin adelantos pendientes»).
  - Acceso directo a «Registrar devolución» precargando `TransferType.REIMBURSEMENT`, dirección correcta y saldo neto pendiente en `TransferScreen`.
  - Historial cronológico único con filtros Todos / Adelantos / Devoluciones y trazabilidad mediante navegación a `MovementDetailScreen`.
  - 3 nuevas pruebas unitarias (`AdvancesViewModelTest`, `TransferViewModelTest`) al 100% en verde (79 pruebas totales en el proyecto).

---

## Sprint V6-S4: Configuración, Datos y QA Final (ESTADO: DONE)
- [x] **V6-09:** Pantalla de Categorías y Hub de Ajustes:
  - Hub `SettingsScreen` en cabecera de Inicio con acceso a Categorías, Cuentas (edición de nombre e icono de Familia y Personal), Movimientos recurrentes, Datos y seguridad y Acerca de.
  - `ManageCategoriesUseCase`, `CategoriesViewModel` y `CategoriesScreen`: pestañas Gastos e Ingresos, hasta 7 favoritas de gasto con diálogo de sustitución al intentar añadir la 8.ª, reordenación manual, selector de iconos monocromáticos, creación inline en «Más» de `AddExpenseScreen` y archivado/restauración preservando histórico y análisis.
  - 6 nuevas pruebas unitarias (`ManageCategoriesUseCaseTest`, `CategoriesViewModelTest`, `AccountsSettingsViewModelTest`) al 100% en verde (90 pruebas totales).
- [x] **V6-11:** Datos y seguridad:
  - Exportación CSV validada (RFC 4180, precisión entera en céntimos) mediante `ExportCsvUseCase`.
  - Copia de seguridad portable completa en JSON (`PortableJson`, `BackupRestoreUseCase`, `RoomBackupRepository`) con validación integral de esquema/claves y restauración atómica en transacción Room tras diálogo de confirmación explícita.
  - Pantalla `DataSecurityScreen` con Android SAF (`CreateDocument` / `OpenDocument`), resumen de datos locales y aviso explícito de almacenamiento 100 % local offline sin nube ni PIN adicional.
  - 7 nuevas pruebas unitarias (`ExportCsvUseCaseTest`, `BackupRestoreUseCaseTest`, `DataSecurityViewModelTest`) al 100% en verde (97 pruebas totales).
- [x] **V6-13:** Auditoría visual y calidad Android:
  - Habilitado `enableEdgeToEdge()` monocromático en `MainActivity` y `navigationBarsPadding()` + `imePadding()` en las barras inferiores de guardado (`AddExpenseScreen`, `AddIncomeScreen`, `TransferScreen`, `MovementDetailScreen`).
  - Unificada paleta estrictamente monocromática MD3 (eliminado acento verde en `HistoryScreen`) y verificados objetivos táctiles `>= 48dp` en chips y controles interactivos.
  - Actualizado `docs/Documentacion.md` al estado real de implementación V6.
  - Verificación real en emulador (`emulator-5554`) a 412 dp y 360 dp en Inicio, Nuevo gasto, Movimientos, Detalle de movimiento, Análisis, Ajustes, Categorías y Datos y seguridad; validado flujo de gasto rápido en 3 toques (`<3 s`) y recálculo tras borrado.

---

## Línea de trabajo propuesta — Recurrentes V6.1 (sin autorización de programación)
**Fuente:** [RECURRENTES_V6_1.md](RECURRENTES_V6_1.md), historias REC-01 a REC-08 de [BACKLOG.md](BACKLOG.md).

**Puerta previa:** verificar que los componentes V6-01/V6-02 marcados DONE son reutilizables y auditar por separado la migración de los recurrentes existentes. El código V6 actual ya declara completadas las historias V6; eso no valida automáticamente una actualización de instalaciones anteriores ni una migración no destructiva.

- **REC-S0 — Diseño y auditoría:** REC-01. Examinar Room, Worker, compatibilidad de instalaciones existentes y estrategia de migración. Sin código hasta autorización.
- **REC-S1 — Reglas y calendario:** REC-02/REC-03. Edición futura, estados, archivo/restauración, frecuencias y tests de calendario.
- **REC-S2 — Ejecuciones:** REC-04/REC-05. Motor idempotente, modo automático/pendiente, reconciliación y revisión individual.
- **REC-S3 — Experiencia y calidad:** REC-06/REC-07/REC-08. Confirmación múltiple atómica, avisos/permiso, backup, migración y regresión en dispositivo.
- **Criterio de salida:** cero duplicados, saldos/deudas correctos tras confirmar, cero efecto de pendientes, ninguna pérdida de datos reales, pruebas en móvil, sin generar periodos pausados.

**Situación:** diseño funcional aprobado y documentado; **todas las historias REC siguen TODO**. Este plan no autoriza implementar ni modificar el APK instalado.

