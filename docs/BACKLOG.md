# BACKLOG

Estados: TODO | IN_PROGRESS | REVIEW | DONE | BLOCKED

## P0 - MVP

### US-01 Registrar gasto personal

Estado: DONE

Criterios:

- Guardar gasto con categoria obligatoria
- Afecta saldo personal mensual
- Aparece en historial

Tareas:

- [x] DAO insertTransaction
- [x] UseCase AddExpenseUseCase
- [x] ViewModel AddTransactionViewModel
- [x] UI AddTransactionScreen
- [x] Historial basico de movimientos
- [x] Selector de tipo para ingreso y gasto

---

### US-02 Registrar gasto familiar pagado con personal

Estado: DONE

Criterios:

- account=FAMILY y paidFromPersonal=true
- No afecta saldo personal
- Afecta saldo familiar
- Crea entrada en reimbursement_ledger

Tareas:

- [x] Regla de dominio ValidateFamilyPaidFromPersonalRule
- [x] Crear ledger entry
- [x] Ajustar calculos dashboard

---

### US-03 Liquidar pendiente familiar -> personal

Estado: DONE

Criterios:

- Liquidacion siempre total
- Crea Settlement
- Marca ledger como SETTLED
- Pendiente pasa a 0

Tareas:

- [x] UseCase CreateFullSettlementUseCase
- [x] PendingSettlementScreen
- [x] DAO updateLedgerStatus

---

### US-04 Movimientos recurrentes

Estado: DONE

Criterios:

- Regla con next_run_at
- Worker genera movimientos pendientes
- source = AUTO_RECURRENT
- Permite fecha inicio y fecha fin opcional

Tareas:

- [x] RecurrenceRule DAO
- [x] Worker GenerateRecurringTransactionsWorker
- [x] UI RecurrenceScreen
- [x] Rango de vigencia en reglas mensuales

---

### US-05 Dashboard mensual

Estado: DONE

Criterios:

- saldo personal mes
- saldo familiar mes
- pendiente familia -> personal

Tareas:

- [x] ComputeMonthlyBalanceRule
- [x] GetDashboardSummaryUseCase
- [x] DashboardScreen
- [x] Acciones rapidas + / - en saldo personal y familiar


---

## V6 — Backlog aprobado para planificar (NO IMPLEMENTADO)

Los estados DONE anteriores reflejan el MVP existente y requieren rediseño/migración para cumplir V6. Fuente: docs/V6_SPEC.md. Todas las historias siguientes están **TODO**, sin tareas ejecutadas:

| ID | Prioridad | Historia | Estado |
|---|---|---|---|
| V6-01 | P0 | Contabilidad de caja real: pagar vs responsable, saldos iniciales y ajustes auditables | TODO |
| V6-02 | P0 | Adelantos bidireccionales, deuda neta, devolución parcial/completa con transferencia de caja | TODO |
| V6-03 | P0 | Nuevo gasto rápido: teclado propio, 7 favoritas, dos selectores, último uso, detalles plegados | TODO |
| V6-04 | P0 | Inicio: tarjetas Familia/Personal, acciones rápidas y adelantos siempre visibles | TODO |
| V6-05 | P1 | Movimientos: filas compactas, filtros por responsable, búsqueda, edición y borrado | TODO |
| V6-06 | P1 | Análisis: Categorías y Evolución, ingresos/gastos separados y drill-down | TODO |
| V6-07 | P1 | Cuenta/transferencia: normal vs devolución explícita, saldo y ajustes | TODO |
| V6-08 | P1 | Historial único de adelantos y devoluciones filtrable | TODO |
| V6-09 | P1 | Categorías personalizables, favoritas manuales, archivo sin pérdida histórica | TODO |
| V6-10 | P1 | Nuevo ingreso con categorías ordenadas manualmente | TODO |
| V6-11 | P1 | Datos: exportación CSV, backup completo, restauración validada | TODO |
| V6-12 | P0 | Migración de datos y pruebas de regresión de caja/deuda/recurrentes | TODO |
| V6-13 | P1 | Validación visual Android 360/412 dp, accesibilidad y uso offline | TODO |

Criterio transversal: no marcar DONE hasta implementar, probar y validar comportamiento V6; no destruir datos históricos ni funcionalidad recurrente existente. Sin sincronización cloud, usuarios múltiples ni PIN en V6.
