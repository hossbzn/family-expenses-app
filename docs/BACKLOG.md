\# BACKLOG



Estados: TODO | IN\_PROGRESS | REVIEW | DONE | BLOCKED



\## P0 â€” MVP



\### US-01 Registrar gasto personal

Estado: IN\_PROGRESS  

Criterios:

\- Guardar gasto con categorÃ­a obligatoria

\- Afecta saldo personal mensual

\- Aparece en historial



Tareas:

\- \[ ] DAO insertTransaction

\- \[ ] UseCase AddExpenseUseCase

\- \[ ] ViewModel AddTransactionViewModel

\- \[ ] UI AddTransactionScreen



---



\### US-02 Registrar gasto familiar pagado con personal

Estado: TODO  

Criterios:

\- account=FAMILY y paidFromPersonal=true

\- No afecta saldo personal

\- Afecta saldo familiar

\- Crea entrada en reimbursement\_ledger



Tareas:

\- \[ ] Regla de dominio ValidateFamilyPaidFromPersonalRule

\- \[ ] Crear ledger entry

\- \[ ] Ajustar cÃ¡lculos dashboard



---



\### US-03 Liquidar pendiente familiar â†’ personal

Estado: TODO  

Criterios:

\- LiquidaciÃ³n siempre total

\- Crea Settlement

\- Marca ledger como SETTLED

\- Pendiente pasa a 0



Tareas:

\- \[ ] UseCase CreateFullSettlementUseCase

\- \[ ] PendingSettlementScreen

\- \[ ] DAO updateLedgerStatus



---



\### US-04 Movimientos recurrentes

Estado: IN\_PROGRESS  

Criterios:

\- Regla con next\_run\_at

\- Worker genera movimientos pendientes

\- source = AUTO\_RECURRENT



Tareas:

\- \[x] RecurrenceRule DAO

\- \[ ] Worker GenerateRecurringTransactionsWorker

\- \[ ] UI RecurrenceScreen



---



\### US-05 Dashboard mensual

Estado: TODO  

Criterios:

\- saldo personal mes

\- saldo familiar mes

\- pendiente familia â†’ personal



Tareas:

\- \[ ] ComputeMonthlyBalanceRule

\- \[ ] GetDashboardSummaryUseCase

\- \[ ] DashboardScreen
