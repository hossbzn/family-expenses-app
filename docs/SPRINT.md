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

## Sprint V6.1: Recurrentes V6.1 (ESTADO: DONE)
**Meta:** Implementación completa del motor de recurrentes V6.1, agregando ocurrencias independientes (modos Automático y Pendiente), calendario flexible (Semanal, Mensual, Multi-mes, Anual), ciclo de vida (Pausa, Archivo, Restauración), bandeja de confirmación y respaldo portable JSON.

- [x] **REC-01 — Esquema Room y Ocurrencias:**
  - `RecurrenceRuleEntity` ampliada y creada `RecurrenceOccurrenceEntity` para instantáneas de ejecuciones.
  - Creados `RecurrenceOccurrenceDao` y `TransactionRunner`.
  - Pruebas unitarias de esquema Room (`RecurrenceSchemaTest`).
- [x] **REC-02 / REC-03 — Ciclo de vida y Calculador de Frecuencias:**
  - Implementado `RecurrenceCalculator` soportando las 4 frecuencias y ajuste de fin de mes/bisiestos.
  - Implementado `ManageRecurrenceRulesUseCase`: pausar, reactivar, archivar y restaurar reglas.
  - Pruebas unitarias (`RecurrenceCalculatorTest`, `ManageRecurrenceRulesUseCaseTest`).
- [x] **REC-04 / REC-05 / REC-06 — Motor de Procesamiento y Gestión de Pendientes:**
  - `ProcessRecurringRulesUseCase`: generación atómica e idempotente para modos `AUTOMATIC` y `PENDING`.
  - `ManagePendingOccurrencesUseCase`: confirmación individual/múltiple (con fecha/importe editable), posponer y omitir.
  - Pruebas unitarias (`ProcessRecurringRulesUseCaseTest`, `ManagePendingOccurrencesUseCaseTest`).
- [x] **REC-07 / REC-08 — Inicio, Worker y Copia Portable:**
  - Banner en `HomeScreen` cuando existen pendientes.
  - `GenerateRecurringTransactionsWorker` adaptado al nuevo procesador.
  - Copia portable JSON e importación actualizada con `recurrenceOccurrences`.
  - 111 pruebas unitarias pasando al 100% en verde.

---

## Sprint V6.1-UI: Interfaces Visuales de Recurrentes V6.1 (ESTADO: TODO)
**Meta:** Diseño e implementación de la experiencia de usuario y vistas de Jetpack Compose para configurar y revisar reglas y ocurrencias pendientes.

> [!IMPORTANT]
> **Decisiones visuales pendientes:**
> 1. Ubicación exacta del acceso a la creación de regla: ¿Botón flotante (FAB) o botón de acción integrado en cabecera de "Mis reglas"?
> 2. Formato de la revisión individual de una ocurrencia pendiente: ¿Pantalla dedicada vs Modal Bottom Sheet sobre la bandeja?

### Historias pendientes:
- [ ] **REC-UI-01 — Crear y editar la configuración visual de una recurrencia (TODO, P1):**
  - Cabecera con título dinámico, datos principales (Tipo, Concepto, Importe, Categoría), cuentas dinámicas (Pagadora/Responsable vs Destinataria), programación con 4 frecuencias y previsualización de próxima ejecución con aclaración de días 29/30/31.
  - Modos Automático y Pendiente con explicaciones descriptivas.
  - Aviso en edición de impacto exclusivo en futuras ocurrencias.
  - Validaciones de formulario con errores en línea y adaptabilidad compacta (Galaxy S25).
  - Pruebas UI Compose y preservación de estado.
- [ ] **REC-UI-02 — Configuración visual de la Lista de reglas (TODO, P1):** Filtros por estado, tarjetas resumen y acciones de ciclo de vida.
- [ ] **REC-UI-03 — Configuración visual del Detalle de regla (TODO, P1):** Vista detallada e historial de ocurrencias vinculadas.
- [ ] **REC-UI-04 — Configuración visual de la Bandeja de pendientes (TODO, P1):** Lista de pendientes con tarjetas de instantánea y acciones rápidas.
- [ ] **REC-UI-05 — Configuración visual de Revisión individual (TODO, P1):** Pantalla/diálogo de edición de fecha e importe antes de confirmar.
- [ ] **REC-UI-06 — Configuración visual de Revisión y confirmación múltiple (TODO, P1):** Selección en lote, pantalla de revisión previa con total y confirmación atómica.

---

## Sprint V6-S5: Sistema Visual Global, Rediseño de Inicio y Modo de Privacidad (ESTADO: EN PROGRESO)
**Meta:** Aplicación del nuevo sistema visual MD3 (blanco roto `#F8F9FA`, azul petróleo `#1A3644`, verde/rojo apagados, rejilla 8 dp, target 48 dp) a todas las pantallas de la app, rediseño de Inicio (paridad de cuentas, acciones "Añadir ingreso" / "Añadir gasto", 3 últimos movimientos) y Modo de Privacidad Global (`"•••• €"`).

### Historias completadas:
- [x] **V6-14 — Rediseño visual de Inicio y sección de últimos 3 movimientos (DONE, P1):**
  - Paleta MD3 azul petróleo / blanco roto neutro. Paridad entre Familia y Personal.
  - Botón "Añadir ingreso" (`+`) a la izquierda y "Añadir gasto" (`−`) a la derecha.
  - Sección "Últimos movimientos" con los 3 movimientos más recientes, enlace "Ver todos" y estado vacío ("No hay movimientos registrados.").
  - Layout compacto Galaxy S25 (360-412 dp) con targets táctiles mín. 48 dp.
- [x] **V6-15 — Modo de privacidad global para ocultar importes (DONE, P1):**
  - Botón de ojo (`Visibility` / `VisibilityOff`) en cabecera de Inicio.
  - Persistencia de `privacyModeEnabled` en Preferences DataStore via `UserPreferencesRepository`.
  - Componente reutilizable `MoneyText` que enmascara importes con `"•••• €"` en Inicio, Movimientos, detalles, cuentas, Adelantos, Análisis y Recurrentes.
  - `contentDescription` dinámico ("Ocultar importes" / "Mostrar importes").
  - 112 pruebas unitarias pasando al 100% en verde.

### Historias pendientes:
- [ ] **V6-16 — Aplicación del sistema visual a Movimientos y Detalle de movimiento (TODO, P1):**
  - Paleta azul petróleo, blanco roto y máscara `"•••• €"` en importes con privacidad activa.
- [ ] **V6-17 — Aplicación del sistema visual a Análisis: categorías y evolución (TODO, P1):**
  - Paleta azul petróleo y supresión de valores monetarios en gráfico de barras y anillo con privacidad activa.
- [ ] **V6-18 — Aplicación del sistema visual a Nuevo Gasto y Nuevo Ingreso (TODO, P1):**
  - Paleta azul petróleo en selectores, teclado numérico de 48 dp y visibilidad de Guardar con teclado activo.
- [ ] **V6-19 — Aplicación del sistema visual a Cuentas, Adelantos, Transferencias y Ajustes (TODO, P1):**
  - Paleta azul petróleo y máscara `"•••• €"` en saldos y deudas.
- [ ] **V6-20 — Aplicación del sistema visual a Categorías, Ajustes de Cuentas y Datos (TODO, P1):**
  - Paleta azul petróleo en hub de ajustes, gestión de categorías y copia/restauración JSON.
- [ ] **V6-21 — Aplicación del sistema visual a todas las pantallas de Recurrentes V6.1 (TODO, P1):**
  - Paleta azul petróleo y máscara `"•••• €"` en importes previstos/pendientes de recurrentes.



