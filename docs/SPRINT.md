# SPRINT ACTUAL

Objetivo: persistencia basica + primeras pantallas funcionales del MVP

## Completadas

### US-01 Registrar gasto personal

Estado: DONE

Tareas:

- [x] Entities Room
- [x] DAO transactions
- [x] AppDatabase
- [x] Seed inicial
- [x] AddExpenseUseCase
- [x] AddTransactionViewModel
- [x] AddTransactionScreen
- [x] Historial basico visible
- [x] Selector de tipo para ingreso y gasto

---

### US-05 Dashboard mensual

Estado: DONE

Tareas:

- [x] ComputeMonthlyBalanceRule
- [x] GetDashboardSummaryUseCase
- [x] DashboardScreen
- [x] Acciones rapidas + / - por saldo
- [x] Ajuste responsive con scroll de seguridad

---

### US-02 Gasto familiar pagado con personal

Estado: DONE

Tareas:

- [x] Regla de validacion para forzar cuenta familiar
- [x] Creacion de `reimbursement_ledger`
- [x] Pendiente reflejado en dashboard
- [x] Acceso rapido desde dashboard al flujo preconfigurado

---

### US-03 Liquidar pendiente familiar -> personal

Estado: DONE

Tareas:

- [x] CreateFullSettlementUseCase
- [x] PendingSettlementScreen
- [x] Liquidacion total de ledger abierto
- [x] Pendiente reseteado a 0 tras liquidar

---

### US-04 Movimientos recurrentes

Estado: DONE

Tareas:

- [x] Regla mensual a dia 1
- [x] GenerateRecurringTransactionsUseCase
- [x] Worker de generacion
- [x] Generacion pendiente al abrir la app
- [x] RecurrenceScreen y alta de regla
- [x] Fecha inicio y fecha fin opcional

---

## Proximo

- Revisar UX y siguientes mejoras del MVP

---

## Hecho

- [x] Crear proyecto Android base
- [x] Definir entidades Room
- [x] Dashboard inicial conectado a Room
- [x] Formulario basico de alta de gasto
- [x] Historial basico con navegacion
- [x] Alta simplificada desde saldo personal y familiar
