\# SDD — Family Expenses App (Android)



\## 1. Objetivo

App Android \*\*offline-first\*\* para gestionar:

\- gastos personales

\- gastos familiares

\- dinero personal adelantado a la familia



Objetivo UX: \*\*registrar un gasto en < 3 segundos\*\*.



---



\## 2. Alcance del MVP

Incluido:

\- ingresos y gastos personales

\- ingresos y gastos familiares

\- gasto familiar pagado con cuenta personal

\- cálculo automático de \*\*pendiente familia → personal\*\*

\- \*\*liquidación total\*\* del pendiente

\- movimientos recurrentes automáticos

\- categorías editables

\- dashboard mensual

\- historial agrupado por día

\- edición y borrado de movimientos

\- \*\*quick expense\*\*



Excluido (fase futura):

\- sincronización cloud

\- multiusuario real

\- exportaciones

\- estadísticas avanzadas

\- OCR tickets

\- integración bancaria



---



\## 3. Modelo conceptual



La app maneja tres balances:



\- `saldo\_personal\_mes`

\- `saldo\_familiar\_mes`

\- `pendiente\_familia\_personal`



Ejemplo:



Gasto supermercado 120€  

pertenece: \*\*Familiar\*\*  

pagado desde: \*\*Personal\*\*



Resultado:





saldo\_familiar\_mes -= 120

saldo\_personal\_mes no cambia

pendiente\_familia\_personal += 120





---



\## 4. Entidades principales



\### User



id

name

createdAt





\### Account



id

name

type (PERSONAL | FAMILY)

createdByUserId

createdAt





\### Category



id

name

icon

color

createdByUserId

createdAt





\### Transaction



id

amount\_minor (Long)

type (INCOME | EXPENSE)

accountId

categoryId

description

transaction\_at

paid\_from\_personal (boolean)

source (MANUAL | AUTO\_RECURRENT)

recurrence\_rule\_id

createdByUserId

createdAt

updatedAt





\### RecurrenceRule



id

amount\_minor

type

accountId

categoryId

description

frequency

interval

next\_run\_at

notifications\_enabled

is\_paused

createdByUserId

createdAt





\### ReimbursementLedger



id

transaction\_id

amount\_minor

status (PENDING | SETTLED)

createdAt





\### Settlement



id

amount\_minor

settlement\_at

createdAt





\### SettlementItem



id

settlement\_id

reimbursement\_ledger\_id

amount\_minor





---



\## 5. Reglas de negocio



\### 5.1 Todo movimiento pertenece a una cuenta



transaction.accountId ≠ null





\### 5.2 Gasto familiar pagado con personal



Si:





account = FAMILY

paid\_from\_personal = true





Entonces:





saldo\_familiar\_mes -= amount

pendiente\_familia\_personal += amount





---



\### 5.3 Saldo personal



saldo\_personal\_mes =

ingresos\_personales\_mes



gastos\_personales\_mes





Los gastos familiares pagados con personal \*\*no afectan al saldo personal\*\*.



---



\### 5.4 Saldo familiar



saldo\_familiar\_mes =

ingresos\_familiares\_mes



gastos\_familiares\_mes





Incluye gastos pagados con personal.



---



\### 5.5 Pendiente familia → personal





pendiente =

SUM(gastos familiares pagados con personal

no liquidados)





---



\### 5.6 Liquidación



\- siempre \*\*total\*\*

\- nunca parcial

\- no genera movimientos en cuentas



Resultado:





pendiente → 0

ledger.status → SETTLED





---



\### 5.7 Edición o borrado



Si un gasto cambia o se elimina:





recalcular pendiente





---



\### 5.8 Recurrentes



Se generan automáticamente si:





next\_run\_at <= today





Algoritmo:





while next\_run\_at <= today

createTransaction()

advanceNextRunDate()





---



\## 6. Dashboard



Mostrar:



\- saldo personal del mes

\- saldo familiar del mes

\- pendiente familia → personal



Acciones:





Añadir gasto



Gasto familiar pagado con personal





---



\## 7. Historial



\- lista única

\- agrupado por día

\- orden descendente



Ejemplo:





Hoy

Supermercado -45€ Familiar

Gasolina -60€ Personal



Ayer

Amazon -30€ Familiar • pagado con personal



1 marzo

Netflix -12€ Familiar • recurrente





Filtros:





Todo

Personal

Familiar

Recurrentes

Pagados con personal





---



\## 8. Añadir gasto



Campos:





importe

categoría

cuenta

fecha

nota

switch pagado con personal





Defaults:





fecha = hoy

categoría = última usada

cuenta = última usada





---



\## 9. Quick expense



Pantalla rápida:





\[ 45,00 ]

categoría: última usada

cuenta: última usada



✔ guardar





---



\## 10. Stack técnico





Kotlin

Jetpack Compose

Room

Hilt

ViewModel + StateFlow

Navigation Compose

WorkManager





---



\## 11. Arquitectura



Capas:





data

domain

ui





Paquetes:





data

database

dao

entity

repository



domain

model

usecase



ui

dashboard

history

addtransaction

settlement

recurrence

components





---



\## 12. Métrica principal



North star metric:





Tiempo medio de registro de gasto < 3 segundos

