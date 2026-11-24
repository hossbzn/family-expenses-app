# SDD — Family Expenses V6.0 (Android)

**Estado:** especificación técnica objetivo aprobada; diseño limpio autorizado. **Fuente de verdad:** [V6_SPEC.md](V6_SPEC.md). Fecha: 2026-11-20.

## 1. Objetivo y alcance
Aplicación Android offline-first que sustituye el registro de gastos e ingresos y el seguimiento de adelantos familiares. Dos cuentas iniciales y únicas en V6: **Familia** y **Personal**. Diseño ampliable a más cuentas en el modelo de datos, sin exponer alta de cuentas en la UI. Registro rápido (<3 segundos como objetivo a medir, no garantía).

**Decisión de persistencia:** Al no existir usuarios reales ni datos históricos de producción que preservar (confirmado por el usuario), se adopta un **reinicio limpio del esquema de persistencia (Esquema V6 limpio)**. Esto elimina la necesidad de migraciones complejas desde el MVP, descarta tablas obsoletas (`reimbursement_ledger`, `settlements`, `settlement_items`) y erradica por completo el booleano `paidFromPersonal`.

**Incluido V6:** saldos reales de caja, ingresos, gastos con cuenta pagadora y cuenta responsable independientes, adelantos bidireccionales derivados en tiempo real, devoluciones parciales o totales que mueven caja y reducen deuda, transferencias normales, ajustes de saldo auditables, categorías configurables (7 favoritas manuales en gastos, lista manual en ingresos), Movimientos, Análisis, exportación CSV, copias y restauración local, conservación de recurrencias automáticas V6.1, sistema visual global (fondo blanco roto `#F8F9FA`, azul petróleo `#1A3644`, verde/rojo apagados, rejilla 8 dp, target 48 dp), Inicio con los 3 últimos movimientos y Modo de Privacidad Global (ocultar importes con máscara `"•••• €"`).

**Excluido V6:** sincronización en la nube, multiusuario, login, PIN adicional, OCR, conexión bancaria y creación de cuentas adicionales desde la UI.

## 2. Invariantes de dominio (obligatorios)
1. **Dualidad ortogonal Pagadora vs. Responsable:** Cada gasto tiene `payingAccountId` (de qué caja sale efectivamente el dinero) y `responsibleAccountId` (a qué cuenta se imputa el gasto para presupuestos y análisis).
2. **Impacto en caja estricto:** El gasto **solo descuenta caja de la pagadora**. En la cuenta responsable, su impacto en caja es estrictamente 0 € si no fue pagado por ella.
3. **Saldo real acumulado de caja:**
   $$\text{Saldo Real} = \text{initialBalanceMinor} + \sum \text{Ingresos} - \sum \text{Gastos Pagados} + \sum \text{Transferencias Entrantes} - \sum \text{Transferencias Salientes} + \sum \text{Ajustes}$$
   Nunca deducir saldos reales de la cuenta responsable ni compensar deudas de forma automática.
4. **Adelanto automático bidireccional:** Si `payingAccountId != responsibleAccountId`, nace automáticamente un adelanto de la pagadora a la responsable por el importe del gasto.
5. **Deuda neta firmada:**
   $$\text{Deuda Neta} = \sum \text{Adelantos}_{P \to F} - \sum \text{Adelantos}_{F \to P} - \sum \text{Devoluciones}_{F \to P} + \sum \text{Devoluciones}_{P \to F}$$
   * Signo positivo: Familia debe a Personal.
   * Signo negativo: Personal debe a Familia.
   * Cero: sin deudas pendientes.
6. **Transferencia normal:** Mueve dinero de caja entre cuentas (`sourceAccountId` $\to$ `destinationAccountId`), con impacto neutro en deuda y sin generar ingreso ni gasto estadístico.
7. **Devolución de adelanto:** Transferencia explícitamente clasificada como `REIMBURSEMENT`. Mueve caja de la cuenta deudora a la acreedora, reduce la deuda neta y admite importe parcial o total hasta el tope de la deuda pendiente. No genera ingreso ni gasto estadístico.
8. **Ajuste de saldo auditable:** Operación registrada con motivo explicativo (`reason`) y delta en céntimos; fuera de ingresos y gastos ordinarios.
9. **Recálculo consistente:** Modificar o eliminar gastos o transferencias recalcula automáticamente saldos de caja y deuda neta.
10. **Precisión numérica:** Importes siempre como `Long` en céntimos (EUR). Sin redondeos ni tipos de coma flotante.

**Ejemplo obligatorio:** Personal paga 80 EUR de una compra atribuida a Familia: Personal -80 de caja, Familia 0 de caja, gasto de Familia +80 en análisis, deuda Familia $\to$ Personal +80. Familia devuelve 30: Familia -30 de caja, Personal +30 de caja, deuda 50, sin ingresos ni gastos nuevos.

## 3. Modelo de persistencia Room V6 (Limpio)
El esquema se compone de 6 entidades limpias sin residuos del MVP:

### `AccountEntity` (`accounts`)
* `id: String` (PK, 'account-personal', 'account-family')
* `name: String` ("Personal", "Familiar")
* `type: AccountType` (PERSONAL, FAMILY)
* `currencyCode: String` ("EUR")
* `initialBalanceMinor: Long`
* `isArchived: Boolean`
* `createdAt: Long`

### `CategoryEntity` (`categories`)
* `id: String` (PK)
* `name: String`
* `icon: String` (nombre o identificador del icono MD3)
* `type: CategoryType` (EXPENSE, INCOME)
* `orderIndex: Int` (para ordenación manual)
* `isFavorite: Boolean` (para las 7 favoritas en EXPENSE)
* `favoritePosition: Int?` (1 a 7, nulo si no es favorita)
* `isArchived: Boolean`
* `createdAt: Long`

### `TransactionEntity` (`transactions`)
* `id: String` (PK)
* `type: TransactionType` (EXPENSE, INCOME)
* `amountMinor: Long` (> 0)
* `payingAccountId: String` (FK `accounts.id`) -> cuenta de origen del dinero en gasto o destinataria en ingreso.
* `responsibleAccountId: String` (FK `accounts.id`) -> cuenta imputada en gasto (en ingreso coincide con payingAccountId).
* `categoryId: String` (FK `categories.id`)
* `occurredAt: Long` (timestamp del movimiento)
* `note: String?`
* `source: TransactionSource` (MANUAL, AUTO_RECURRENT)
* `recurrenceRuleId: String?` (FK `recurrence_rules.id`, opcional)
* `createdAt: Long`

### `TransferEntity` (`transfers`)
* `id: String` (PK)
* `type: TransferType` (NORMAL, REIMBURSEMENT)
* `sourceAccountId: String` (FK `accounts.id`)
* `destinationAccountId: String` (FK `accounts.id`)
* `amountMinor: Long` (> 0)
* `occurredAt: Long`
* `note: String?`
* `createdAt: Long`

### `BalanceAdjustmentEntity` (`balance_adjustments`)
* `id: String` (PK)
* `accountId: String` (FK `accounts.id`)
* `amountDeltaMinor: Long` (positivo o negativo)
* `reason: String`
* `occurredAt: Long`
* `createdAt: Long`

### `RecurrenceRuleEntity` (`recurrence_rules`)
* `id: String` (PK)
* `type: TransactionType` (EXPENSE, INCOME)
* `payingAccountId: String` (FK `accounts.id`)
* `responsibleAccountId: String` (FK `accounts.id`)
* `categoryId: String` (FK `categories.id`)
* `amountMinor: Long`
* `note: String?`
* `frequency: RecurrenceFrequency` (MONTHLY)
* `dayOfMonth: Int` (1)
* `startAt: Long`
* `endAt: Long?`
* `nextOccurrenceAt: Long`
* `isActive: Boolean`
* `createdAt: Long`

## 4. Pantallas y navegación
* **Inicio:** Cabecera con saludo/subtítulo ("Buenos días / Resumen familiar"), botón Ojo de Privacidad (`contentDescription`: "Ocultar importes" / "Mostrar importes") y Ajustes (⚙️). Tarjetas de Familia y Personal con el mismo peso y jerarquía visual (azul petróleo y fondo blanco roto neutro `#F8F9FA`) mostrando el saldo del mes en curso (`ingresos − gastos` por cuenta responsable + transferencias normales del mes, sin devoluciones ni ajustes; el saldo disponible real se consulta en Detalle de cuenta). Bloque de Adelantos siempre visible. Sección «Últimos movimientos» mostrando los 3 últimos movimientos (gastos, ingresos, transferencias, devoluciones, ajustes, recurrentes) con concepto, fecha, cuenta e importe (cada fila clicable para abrir `MovementDetailScreen` y editar o eliminar), botón «Ver todos» y estado vacío cuando no hay registros. Sin gráficos acumulados. Barra inferior fija con acciones rápidas **«Ingreso»** (`+`) a la izquierda y **«Gasto»** (`−`) a la derecha (con protagonismo de los signos) justo encima de la navegación Inicio / Movimientos / Análisis.
* **Nuevo gasto:** Teclado numérico propio (coma, 2 decimales, C, retroceso, importe inicial 0). 7 favoritas manuales + Más en 2x4. Dos selectores separados «Pagado desde» y «Gasto de», recordando última combinación y categoría. Detalles plegados. Guardar en un paso con Deshacer.
* **Nuevo ingreso:** Importe, cuenta destinataria, lista manual de categorías de ingreso (sin favoritas), fecha y notas opcionales.
* **Movimientos:** Filas de dos líneas por día descendente, sin resumen mensual. Filtro principal Todas/Familia/Personal por cuenta responsable (en gastos) y destinataria (en ingresos). Periodos: mes, 12 meses móviles, año natural, personalizado. Adelantos mostrados como un único gasto con indicación de pagadora. Ocultación de importes mediante máscara `"•••• €"` si el Modo de Privacidad está activo.
* **Análisis:** Pestañas Categorías y Evolución con selector de serie **Saldo** / **Ingresos** / **Gastos**. Gastos imputados a cuenta responsable. Exclusión de transferencias, devoluciones y ajustes de saldo. Drill-down a Movimientos con retorno de filtros. Ocultación de importes mediante máscara `"•••• €"` si el Modo de Privacidad está activo.
* **Detalle de cuenta y transferencias:** Saldo real, Transferir, Movimientos y Adelantos. Transferencia NORMAL vs. DEVOLUCIÓN con elección explícita.
* **Adelantos:** Saldo neto en ambas direcciones con estado cero visible, historial cronológico único con filtros Todos / Adelantos / Devoluciones y trazabilidad.
* **Ajustes y datos:** Categorías configurables, copia completa portable y exportación CSV.

## 5. Persistencia, recurrencias, privacidad y seguridad
* Kotlin, Jetpack Compose, Room, Hilt, ViewModel/StateFlow, Navigation Compose, WorkManager.
* Arquitectura `data / domain / feature`. Cálculos contables exclusivamente en dominio; UI solo de presentación.
* Transacciones atómicas de Room (`withTransaction`) para cualquier operación que altere saldos o deudas.
* Recurrencias automáticas procesadas por WorkManager aplicando las mismas reglas contables de pagadora/responsable que las transacciones manuales.
* **Modo de Privacidad Global:** Conmutador persistente en SharedPreferences (`UserPreferencesRepository.observePrivacyModeEnabled()`). Reemplaza visualmente valores numéricos por `"•••• €"` sin alterar el motor contable, los flujos de Room, las exportaciones CSV ni los backups JSON. No es un cifrado ni requiere PIN.

## 6. Criterios de aceptación contables
1. Personal paga 80 € de compra familiar: Personal -80 € de caja, Familia 0 € de caja, gasto analítico Familia 80 €, deuda Familia $\to$ Personal +80 €.
2. Devolución parcial de Familia a Personal de 30 €: Familia -30 € de caja, Personal +30 € de caja, deuda restante 50 €, sin nuevo ingreso ni gasto.
3. Transferencia normal entre cuentas: mueve caja, no altera deuda ni estadísticas.
4. Ajuste de saldo auditable: modifica caja con motivo, fuera de estadísticas.
5. Edición y borrado recalculan saldos y deudas consistentemente.
