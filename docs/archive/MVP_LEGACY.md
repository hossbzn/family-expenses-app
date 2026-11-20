# Archivo histórico — MVP anterior a V6

**Solo referencia histórica. NO fuente de requisitos vigentes ni instrucciones de programación.** Conserva la trazabilidad de lo que la documentación anterior describía o marcaba como entregado. Los términos «saldo mensual», «liquidación total sin caja» y «pagado con personal» son **obsoletos para V6**.

## Antiguas historias y estado histórico

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


## Antiguo sprint registrado

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


## Documentación funcional del MVP anterior

# Manual de la aplicacion

## Objetivo
La aplicacion permite registrar ingresos y gastos personales y familiares en local, con un enfoque rapido para el registro diario.

## Estado actual del MVP
- Base Android con Kotlin, Compose, Room, Hilt, Navigation y WorkManager.
- Seed inicial con cuentas `Personal` y `Familiar`.
- Categorias iniciales de ejemplo.
- Dashboard mensual conectado a Room con saldo personal, saldo familiar y pendiente familia a personal.
- Alta manual de ingresos y gastos desde un formulario simplificado con importe destacado y teclado numerico.
- Historial basico de movimientos con lista unica y orden descendente.

## Reglas contables clave
- Todo movimiento pertenece a una cuenta.
- Los importes se guardan en minor units usando `Long`.
- Un gasto familiar pagado con cuenta personal afecta al saldo familiar y al pendiente familia a personal.
- La liquidacion del pendiente sera total en el MVP.

## Uso actual
1. Abrir la app.
2. Esperar a que se ejecute el seed inicial en el primer arranque.
3. Revisar el dashboard mensual.
4. En `Saldo personal`, usar `+` para anadir un ingreso personal o `-` para anadir un gasto personal.
5. En `Saldo familiar`, usar `+` para anadir un ingreso familiar o `-` para anadir un gasto familiar.
6. En el formulario, introducir primero el importe.
7. Elegir categoria y, si aplica, activar `Pagado con personal`.
8. Anadir una nota solo si hace falta.
9. Guardar el movimiento.
10. Usar `Ver historial` para revisar los movimientos guardados.

## Pantalla principal
- `Personal`: ingresos menos gastos personales del mes. Si un gasto se marco como pagado con personal pero pertenece a familia, no descuenta aqui.
- `Familiar`: ingresos menos gastos familiares del mes.
- `Pendiente familia a personal`: suma de entradas abiertas en `reimbursement_ledger`.
- Cada saldo tiene acceso rapido `+ / -` para abrir el formulario ya preconfigurado.
- El dashboard usa un layout adaptable con scroll de seguridad para que no se pierdan acciones en pantallas mas bajas.

## Alta de movimiento
- El tipo de movimiento llega preconfigurado desde el acceso rapido y no se cambia dentro del formulario.
- El importe aparece en grande al abrir la pantalla y solicita teclado numerico decimal automaticamente.
- Debajo se muestran solo los campos necesarios: categoria, `Pagado con personal`, nota y guardar.
- En gastos familiares pagados con personal, el switch genera pendiente familia a personal y fuerza la logica contable correspondiente.

## Recurrentes
- Existe una pantalla de reglas recurrentes accesible desde el dashboard.
- En el MVP actual, todas las reglas recurrentes se generan el dia 1 de cada mes.
- Cada regla permite indicar `fecha inicio` obligatoria y `fecha fin` opcional.
- La primera generacion se normaliza siempre al siguiente dia 1 valido dentro del rango indicado.
- Inicio y fin se configuran desde campos de fecha preparados para abrir un selector de calendario.
- Si la app se abre tarde, los movimientos pendientes se generan automaticamente al arrancar.
- Cuando una regla supera su `fecha fin`, deja de generar y pasa a estado finalizado.
- Los movimientos creados por este flujo se guardan como `AUTO_RECURRENT` y quedan vinculados a su regla.

## Limitaciones actuales
- El historial todavia no tiene filtros, agrupacion por dia ni edicion.
- La edicion de reglas recurrentes aun no esta disponible en el MVP actual.


## Nota de migración
El comportamiento histórico no debe reconstruirse a ciegas bajo V6: las liquidaciones anteriores podrían no representar movimientos de caja. Revisar registros y pruebas sin crear cobros/pagos artificiales.
