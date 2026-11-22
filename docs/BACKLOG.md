# BACKLOG — Family Expenses V6.0

**Estado:** diseño aprobado, Sprint S1 completado con éxito. Leyenda: TODO | IN_PROGRESS | REVIEW | DONE | BLOCKED. Fuente: [V6_SPEC.md](V6_SPEC.md) y [sdd.md](sdd.md). Los DONE del MVP anterior están archivados en [archive/MVP_LEGACY.md](archive/MVP_LEGACY.md) y no indican cumplimiento de V6.

## P0 — Modelo económico, persistencia limpia y registro fundamental

### V6-12 — Esquema Room V6 e inicialización limpia (DONE)
**Como desarrollador** quiero un esquema Room limpio y ortogonal que soporte las invariantes V6 sin deuda técnica del MVP.
- [x] Definir entidades limpias: `AccountEntity`, `CategoryEntity` (con favoritas y orden), `TransactionEntity` (con `payingAccountId` y `responsibleAccountId`), `TransferEntity`, `BalanceAdjustmentEntity` y `RecurrenceRuleEntity`.
- [x] Eliminar tablas obsoletas del MVP (`reimbursement_ledger`, `settlements`, `settlement_items`, `users`) y descartar el campo `paidFromPersonal`.
- [x] Implementar DAOs V6 con flujos reactivos (`Flow`) y operaciones atómicas de base de datos.
- [x] Configurar `DatabaseSeeder` V6 con cuentas 'account-personal', 'account-family' y categorías iniciales con 7 favoritas configuradas.
- [x] Configurar base de datos limpia para desarrollo con `AppDatabase` v1 y fallback destructivo para entorno de pruebas.
- [x] Pruebas unitarias de integridad de DAOs e inserciones.

### V6-01 — Saldos de caja reales (DONE)
**Como usuario** quiero ver el saldo real en Familia y Personal y distinguir quién paga de a quién corresponde cada gasto.
- [x] Implementar `ComputeAccountBalanceUseCase` calculando el saldo acumulado real:
  $$\text{Saldo Real} = \text{initialBalanceMinor} + \sum \text{Ingresos} - \sum \text{Gastos Pagados} + \sum \text{Transferencias Entrantes} - \sum \text{Transferencias Salientes} + \sum \text{Ajustes}$$
- [x] Descontar el gasto exclusivamente de la caja de `payingAccountId`; en `responsibleAccountId` su impacto en caja es 0 €.
- [x] Implementar `AddExpenseUseCase` con validación de cuentas pagadora y responsable independientes.
- [x] Implementar caso de uso `CreateBalanceAdjustmentUseCase` para registro de ajuste de saldo auditable con motivo (`reason`).
- [x] Probar caso Personal paga 80 € de Familia → Personal -80 €, Familia 0 €, gasto Familia 80 €.
- [x] Probar saldos iniciales, ingresos, ajustes y ausencia de redondeos (Long en céntimos).

### V6-02 — Adelantos bidireccionales y devoluciones parciales (DONE)
**Como usuario** quiero que los adelantos entre cuentas se calculen automáticamente y poder hacer devoluciones parciales o totales que muevan caja real.
- [x] Calcular deuda neta derivada en tiempo real:
  $$\text{Deuda Neta} = \sum \text{Adelantos}_{P \to F} - \sum \text{Adelantos}_{F \to P} - \sum \text{Devoluciones}_{F \to P} + \sum \text{Devoluciones}_{P \to F}$$
- [x] Mostrar saldo neto con ambos signos (+ Familia debe, - Personal debe) y estado cero visible.
- [x] Implementar `ExecuteTransferUseCase` separando explícitamente `TransferType.NORMAL` vs. `TransferType.REIMBURSEMENT`.
- [x] Validar que la devolución mueva caja deudora a acreedora, reduzca la deuda y admita importes parciales hasta el tope de la deuda neta pendiente.
- [x] Probar que las devoluciones y transferencias no alteren las estadísticas de gasto e ingreso.
- [x] Recalcular saldos y deuda automáticamente al editar o eliminar transacciones o transferencias.

### V6-03 — Registro rápido de gasto (DONE)
- [x] Teclado decimal numérico propio: coma, C, retroceso, máximo 2 decimales, Guardar deshabilitado con 0 €.
- [x] Cuadrícula 2x4 con 7 favoritas manuales + Más; recordar última categoría y última pareja de cuentas.
- [x] Selectores separados «Pagado desde» y «Gasto de», aviso de adelanto si difieren, fecha hoy por defecto y detalles plegados.
- [x] Guardado en un paso directo, opción de Deshacer y confirmación al descartar cambios con datos introducidos.

### V6-04 — Inicio V6 (DONE)
- [x] Dos tarjetas con la misma jerarquía: Familia (negra) y Personal (gris claro), mostrando saldo de caja real individual.
- [x] Botones grandes «Nuevo gasto» (principal negro) y «Nuevo ingreso» (secundario).
- [x] Bloque de Adelantos siempre visible incluso en cero («Familia te debe X», «Personal debe a Familia X» o «Sin adelantos pendientes»).
- [x] Sin movimientos recientes ni totales del mes; barra inferior con tres destinos: Inicio, Movimientos, Análisis.

## P1 — Navegación, análisis y administración

### V6-05 — Movimientos (DONE)
- [x] Filas compactas de dos líneas agrupadas por día descendente, sin resumen mensual.
- [x] Filtro principal Todas/Familia/Personal según cuenta responsable del gasto y destinataria del ingreso.
- [x] Búsqueda y filtros por tipo, categoría y periodos (mes, últimos 12 meses, año natural, personalizado).
- [x] Detalle para editar y borrar con recálculo atómico; preservar filtros y scroll al volver.

### V6-06 — Análisis (DONE)
- [x] Pestañas Categorías (predeterminada, anillo monocromático y ranking con importes/porcentajes) y Evolución (barras de un solo tipo: Gastos o Ingresos).
- [x] Periodos y filtros compartidos; drill-down a Movimientos con retorno íntegro de filtros y estado.
- [x] Excluir transferencias, devoluciones y ajustes de saldo de los gráficos.

### V6-07 — Detalle cuenta y transferencias (DONE)
- [x] Saldo real; accesos Transferir, Movimientos y Adelantos, sin lista de recientes.
- [x] Formulario de Transferir con selección obligatoria y explícita: «Transferencia normal» vs. «Devolución de adelanto».
- [x] Devolución precarga dirección y saldo pendiente neto, editable hasta el máximo; ajustes de saldo auditables.

### V6-08 — Adelantos, historial (DONE)
- [x] Saldo de deuda neto en ambas direcciones y estado cero visible.
- [x] Historial cronológico único con filtros Todos / Adelantos / Devoluciones y trazabilidad a la operación de origen.

### V6-09 — Categorías configurables (DONE)
- [x] Gastos: exactamente hasta siete favoritas ordenadas manualmente con estrella y controles de orden; sustitución explícita si se añade octava; «Más» para restantes, búsqueda y creación.
- [x] Ingresos: lista ordenada manualmente, sin favoritas.
- [x] Editar nombre, icono, tipo y orden; archivar/restaurar categorías sin afectar al histórico ni al análisis.

### V6-10 — Nuevo ingreso (DONE)
- [x] Categoría de ingresos en lista manual (sin favoritas), cuenta destinataria, importe, fecha y detalles opcionales.
- [x] Recordar últimas elecciones de ingreso de forma independiente al formulario de gasto.

### V6-11 — Datos y seguridad (DONE)
- [x] Exportación de movimientos, transferencias y ajustes a CSV validado (RFC 4180).
- [x] Copia de seguridad portable completa JSON (cuentas, categorías, transacciones, transferencias, ajustes, reglas, preferencias) y restauración validada con confirmación previa y reemplazo atómico.
- [x] Pruebas de persistencia y funcionamiento 100 % local y offline, sin PIN ni nube, con aviso visible de copia externa.

### V6-13 — Validación visual Android y calidad (DONE)
- [x] Auditoría de código para contraste monocromático MD3, áreas seguras (`enableEdgeToEdge`, `navigationBarsPadding`, `imePadding`), objetivos táctiles `>=48 dp` y navegación hacia atrás/restauración de estado en todas las pantallas.
- [x] Verificación visual en dispositivo/emulador real (`emulator-5554` a 412 dp y 360 dp) y validación del flujo rápido de gasto en 3 toques (<3 s: importe + Guardar con últimas elecciones recordadas).

## P1/P2 — Recurrentes V6.1 (ESPECIFICADO, EN DESARROLLO)
**Fuente:** [RECURRENTES_V6_1.md](RECURRENTES_V6_1.md). No alterar datos históricos del móvil.

### REC-01 — Auditoría y esquema Room de ocurrencias (DONE, P0)
- [x] Auditar reglas/Worker/Room existentes y definir enums para frecuencia (Semanal, Mensual, Multi-mes, Anual), modos (Automático/Pendiente) y estados de regla (Activa, Pausada, Archivada, Finalizada).
- [x] Diseñar e implementar las entidades Room `RecurrenceRuleEntity` y `RecurrenceOccurrenceEntity` con soporte de instantáneas de importes y vinculación con `TransactionEntity`.
- [x] Implementar `RecurrenceOccurrenceDao` y actualizar `RecurrenceRuleDao` y `AppDatabase` v1/v2.
- [x] Pruebas unitarias de esquema y modelos (`RecurrenceSchemaTest`).

### REC-02 — Edición y ciclo de vida (DONE, P1)
- [x] Implementado `ManageRecurrenceRulesUseCase`: pausar/reactivar, archivar/restaurar con nueva fecha inicial por defecto y edición de reglas.
- [x] Garantizado que la edición/pausa no altere instantáneas de ocurrencias o transacciones ya creadas.
- [x] Pruebas unitarias de ciclo de vida (`ManageRecurrenceRulesUseCaseTest`).

### REC-03 — Frecuencias y calendario (DONE, P1)
- [x] Implementado `RecurrenceCalculator` soportando las 4 frecuencias: Semanal (día de semana), Mensual (día 1-31), Multi-mes (2, 3 o 6 meses) y Anual (mes y día).
- [x] Ajuste automático al último día real del mes para días 29/30/31 y 29 de febrero en años no bisiestos.
- [x] Pruebas unitarias de cálculo de fechas y ajuste de fin de mes (`RecurrenceCalculatorTest`).

### REC-04 — Modos y generación de ocurrencias (DONE, P1)
- [x] Implementado `ProcessRecurringRulesUseCase` distinguiendo modo `AUTOMATIC` (registra directo con motor V6) y modo `PENDING` (genera ocurrencia sin alterar caja/deuda).
- [x] Reconciliación idempotente de fechas vencidas sin duplicados en transacciones Room atómicas (`TransactionRunner`).
- [x] Pruebas unitarias de generación automática y pendiente (`ProcessRecurringRulesUseCaseTest`).

### REC-05 — Bandeja y gestión individual (DONE, P1)
- [x] Implementado `ManagePendingOccurrencesUseCase`: confirmación individual con fecha/importe editable y aplicación estricta de contabilidad V6.
- [x] Acciones de omitir (`SKIPPED`) y posponer sin alterar fecha económica ni reglas futuras.
- [x] Pruebas unitarias de gestión individual (`ManagePendingOccurrencesUseCaseTest`).

### REC-06 — Confirmación múltiple (DONE, P1)
- [x] Confirmación múltiple en `ManagePendingOccurrencesUseCase` en transacción todo-o-nada e idempotente.
- [x] Pruebas unitarias de confirmación múltiple (`ManagePendingOccurrencesUseCaseTest`).

### REC-07 — Inicio y avisos (DONE, P2)
- [x] Banner discreto en pantalla de Inicio (`PendingRecurrencesBanner`) avisando del número de recurrentes pendientes.
- [x] Integración de `GenerateRecurringTransactionsWorker` con `ProcessRecurringRulesUseCase` para reconciliación automática de fondo.

### REC-08 — QA y respaldo (DONE, P1)
- [x] `PortableBackupSnapshot` y `RoomBackupRepository` actualizados para incluir `recurrenceOccurrences` en copias de seguridad JSON y restauración atómica.
- [x] 111 pruebas unitarias e integración en verde (`:app:testDebugUnitTest`).


## Criterio de cierre V6
Todas las funcionalidades V6 (Sprints S1–S4) están implementadas, documentadas y verificadas con 97 pruebas unitarias/de integración, `assembleDebug` y QA visual/interactiva en emulador Android (412 dp y 360 dp).
