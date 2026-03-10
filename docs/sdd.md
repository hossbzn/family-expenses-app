# SDD - Family Expenses App (Android)

## 1. Objetivo

App Android **offline-first** para gestionar:

- gastos personales
- gastos familiares
- dinero personal adelantado a la familia

Objetivo UX: **registrar un gasto en < 3 segundos**.

---

## 2. Alcance del MVP

Incluido:

- ingresos y gastos personales
- ingresos y gastos familiares
- gasto familiar pagado con cuenta personal
- calculo automatico de **pendiente familia -> personal**
- **liquidacion total** del pendiente
- movimientos recurrentes automaticos
- categorias editables
- dashboard mensual
- historial agrupado por dia
- edicion y borrado de movimientos
- **quick expense**

Excluido (fase futura):

- sincronizacion cloud
- multiusuario real
- exportaciones
- estadisticas avanzadas
- OCR tickets
- integracion bancaria

---

## 3. Modelo conceptual

La app maneja tres balances:

- `saldo_personal_mes`
- `saldo_familiar_mes`
- `pendiente_familia_personal`

Ejemplo:

Gasto supermercado 120 EUR  
pertenece: **Familiar**  
pagado desde: **Personal**

Resultado:

- `saldo_familiar_mes -= 120`
- `saldo_personal_mes` no cambia
- `pendiente_familia_personal += 120`

---

## 4. Entidades principales

### User

- id
- name
- createdAt

### Account

- id
- name
- type (PERSONAL | FAMILY)
- createdByUserId
- createdAt

### Category

- id
- name
- icon
- color
- createdByUserId
- createdAt

### Transaction

- id
- amount_minor (Long)
- type (INCOME | EXPENSE)
- accountId
- categoryId
- description
- transaction_at
- paid_from_personal (boolean)
- source (MANUAL | AUTO_RECURRENT)
- recurrence_rule_id
- createdByUserId
- createdAt
- updatedAt

### RecurrenceRule

- id
- amount_minor
- type
- accountId
- categoryId
- description
- frequency
- interval
- next_run_at
- notifications_enabled
- is_paused
- createdByUserId
- createdAt

### ReimbursementLedger

- id
- transaction_id
- amount_minor
- status (PENDING | SETTLED)
- createdAt

### Settlement

- id
- amount_minor
- settlement_at
- createdAt

### SettlementItem

- id
- settlement_id
- reimbursement_ledger_id
- amount_minor

---

## 5. Reglas de negocio

### 5.1 Todo movimiento pertenece a una cuenta

`transaction.accountId != null`

### 5.2 Gasto familiar pagado con personal

Si:

- `account = FAMILY`
- `paid_from_personal = true`

Entonces:

- `saldo_familiar_mes -= amount`
- `pendiente_familia_personal += amount`

### 5.3 Saldo personal

`saldo_personal_mes = ingresos_personales_mes - gastos_personales_mes`

Los gastos familiares pagados con personal **no afectan al saldo personal**.

### 5.4 Saldo familiar

`saldo_familiar_mes = ingresos_familiares_mes - gastos_familiares_mes`

Incluye gastos pagados con personal.

### 5.5 Pendiente familia -> personal

`pendiente = SUM(gastos familiares pagados con personal no liquidados)`

### 5.6 Liquidacion

- siempre **total**
- nunca parcial
- no genera movimientos en cuentas

Resultado:

- `pendiente -> 0`
- `ledger.status -> SETTLED`

### 5.7 Edicion o borrado

Si un gasto cambia o se elimina:

- recalcular pendiente

### 5.8 Recurrentes

Se generan automaticamente si:

- `next_run_at <= today`

Algoritmo:

- `while next_run_at <= today`
- `createTransaction()`
- `advanceNextRunDate()`

---

## 6. Dashboard

Mostrar:

- saldo personal del mes
- saldo familiar del mes
- pendiente familia -> personal

Acciones:

- acceso rapido `+` y `-` dentro de `Saldo personal`
- acceso rapido `+` y `-` dentro de `Saldo familiar`
- acceso a historial

---

## 7. Historial

- lista unica
- agrupado por dia
- orden descendente

Ejemplo:

- Hoy
- Supermercado -45 EUR Familiar
- Gasolina -60 EUR Personal

- Ayer
- Amazon -30 EUR Familiar - pagado con personal

- 1 marzo
- Netflix -12 EUR Familiar - recurrente

Filtros:

- Todo
- Personal
- Familiar
- Recurrentes
- Pagados con personal

---

## 8. Añadir gasto o ingreso

Campos:

- tipo
- importe
- categoria
- cuenta
- fecha
- nota
- switch pagado con personal

Defaults:

- fecha = hoy
- categoria = ultima usada
- cuenta = ultima usada

Si se activa `pagado con cuenta personal`, la cuenta debe forzarse a `Familiar`.

---

## 9. Quick expense

Pantalla rapida:

- `[ 45,00 ]`
- categoria: ultima usada
- cuenta: ultima usada
- guardar

---

## 10. Direccion visual

La app debe seguir una estetica:

- minimalista
- black and white
- con muy poco ruido visual

Reglas visuales:

- priorizar fondo claro, texto negro y grises neutros
- evitar colores decorativos salvo feedback o estados imprescindibles
- evitar botones recargados o bloques visuales pesados
- usar espaciado amplio y jerarquia tipografica limpia
- si se usan iconos, deben ser simples, monocromos y de estilo minimalista

Objetivo visual:

- que la interfaz se sienta rapida, limpia y sobria

---

## 11. Stack tecnico

- Kotlin
- Jetpack Compose
- Room
- Hilt
- ViewModel + StateFlow
- Navigation Compose
- WorkManager

---

## 12. Arquitectura

Capas:

- data
- domain
- ui

Paquetes:

- data
- database
- dao
- entity
- repository

- domain
- model
- usecase

- ui
- dashboard
- history
- addtransaction
- settlement
- recurrence
- components

---

## 13. Metrica principal

North star metric:

- Tiempo medio de registro de gasto < 3 segundos
