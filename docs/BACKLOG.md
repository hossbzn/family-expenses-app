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

Estado: TODO

Criterios:

- Liquidacion siempre total
- Crea Settlement
- Marca ledger como SETTLED
- Pendiente pasa a 0

Tareas:

- [ ] UseCase CreateFullSettlementUseCase
- [ ] PendingSettlementScreen
- [ ] DAO updateLedgerStatus

---

### US-04 Movimientos recurrentes

Estado: IN_PROGRESS

Criterios:

- Regla con next_run_at
- Worker genera movimientos pendientes
- source = AUTO_RECURRENT

Tareas:

- [x] RecurrenceRule DAO
- [ ] Worker GenerateRecurringTransactionsWorker
- [ ] UI RecurrenceScreen

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
