# BACKLOG — Family Expenses V6.0

**Estado:** V6 y Recurrentes V6.1 en fase de estabilización senior y verificación de pruebas. Leyenda: TODO | IN_PROGRESS | REVIEW | DONE | BLOCKED. Fuente: [V6_SPEC.md](V6_SPEC.md), [sdd.md](sdd.md) y [RECURRENTES_V6_1.md](RECURRENTES_V6_1.md).

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


## P1 — Interfaces Visuales de Recurrentes V6.1 (DONE)

### REC-UI-01 — Crear y editar la configuración visual de una recurrencia (DONE, P1)
**Como usuario** quiero una interfaz clara e intuitiva para configurar o editar la programación de una recurrencia.
- [x] **Cabecera:** Título dinámico "Nueva recurrencia" o "Editar recurrencia", botón de navegación hacia atrás (Volver) y acción de "Guardar" claramente visible.
- [x] **Datos principales:** Selector segmentado Gasto / Ingreso, campo de concepto/nota, campo de importe numérico y selector de categoría.
- [x] **Cuentas dinámicas:**
  - Para Gasto: Muestra dos selectores independientes "Pagado desde" (pagadora) y "Gasto de" (responsable).
  - Para Ingreso: Muestra un único selector "Ingresar en" (destinataria). Oculta campos no aplicables según el tipo.
- [x] **Programación y calendario:**
  - Selector de frecuencia: Semanal, Mensual, Cada varios meses (2, 3 o 6 meses) y Anual.
  - Controles dinámicos según frecuencia: Día de la semana (semanal), Día del mes 1-31 (mensual / multi-mes), o Día y Mes (anual).
  - Fecha de inicio obligatoria y fecha de fin opcional con calendario.
  - Previsualización dinámica explícita: "Próxima ejecución: X date".
  - Explicación visual aclaratoria para días 29, 30 y 31 ("En meses con menos días se ejecutará el último día real del mes").
- [x] **Modo de ejecución:**
  - Opciones de "Automático" (se registra solo al llegar la fecha) y "Pendiente de confirmar" (crea un recordatorio en la bandeja), cada una acompañada de un texto explicativo breve.
- [x] **Edición:**
  - Aviso explícito de que los cambios solo afectan a futuras ocurrencias no generadas (no altera pendientes anteriores ni movimientos registrados).
  - Actualización automática de la previsualización de fecha si cambia la frecuencia.
- [x] **Validaciones en tiempo real:**
  - Muestra mensajes de error en línea junto al campo correspondiente: fecha fin anterior a inicio, importe nulo/inválido, categoría o cuentas no seleccionadas, frecuencia incompleta.
  - Conserva íntegros los datos introducidos ante un error sin limpiar el formulario.
- [x] **Diseño y adaptabilidad:**
  - Pantalla compacta adaptada a Galaxy S25 / Android (360 dp - 412 dp), objetivos táctiles `>= 48 dp`, botón Guardar siempre accesible.

### REC-UI-02 — Configuración visual de la Lista de reglas (DONE, P1)
- [x] Pestañas/chips de filtro por estado: **Activas**, **Pausadas**, **Archivadas** y **Finalizadas**.
- [x] Tarjetas de regla con concepto, importe, frecuencia, cuentas involucradas, modo y fecha próxima.
- [x] Botones de acción directa: Editar, Pausar/Reactivar, Archivar/Restaurar.

### REC-UI-03 — Configuración visual del Detalle de regla (DONE, P1)
- [x] Diálogo detallado con resumen completo de la configuración y estado.

### REC-UI-04 — Configuración visual de la Bandeja de pendientes (DONE, P1)
- [x] Listado de tarjetas de ejecuciones pendientes con datos de la instantánea y acciones rápidas (Confirmar, Omitir, Revisar).

### REC-UI-05 — Configuración visual de Revisión individual (DONE, P1)
- [x] Diálogo de edición previa con pre-carga de importe y fecha programada original editable antes de confirmar.

### REC-UI-06 — Configuración visual de Revisión y confirmación múltiple (DONE, P1)
- [x] Botón "Confirmar todas" en la bandeja de pendientes con ejecución en lote e idempotente.


## P1 — Sistema Visual Global, Rediseño de Inicio y Modo de Privacidad (TODO)

### 1. Tokens del Sistema Visual
- **Fondo y Superficies:** Background `#F8F9FA` (Blanco roto neutro), Surface `#FFFFFF` (Blanco puro para tarjetas, hojas y campos).
- **Color Principal:** Primary `#1A3644` (Azul petróleo principal) para botones de acción, indicadores de pestañas, bordes de selección y elementos destacados.
- **Colores Semánticos:**
  - `SemanticGreen` (`#2E7D32` - Verde sobrio) para ingresos, saldos positivos y confirmaciones.
  - `SemanticRed` (`#C62828` - Rojo carmesí apagado) para gastos, saldos negativos y acciones destructivas.
  - **Invariante:** Nunca depender únicamente del color; combinar siempre con signos `+` / `−` e iconografía explicativa.
- **Paridad de Cuentas:** Familia y Personal utilizan exactamente el mismo diseño, jerarquía y peso visual.
- **Tipografía y Rejilla:** Escala MD3, rejilla base de `8 dp` (espaciados de 8 dp, 12 dp, 16 dp y 24 dp).
- **Objetivos Táctiles:** Mínimo de `48 dp` $\times$ `48 dp` en todos los componentes interactivos (botones, chips, iconos, filas).
- **Adaptabilidad:** Optimizado para Galaxy S25 base (360-412 dp), con scroll vertical antes que reducción de texto.
- **Estados Explícitos:** Vacío, cargando, error, deshabilitado y confirmación.

### 2. Matriz de Aplicación del Modo de Privacidad Global (`•••• €`)

| Pantalla / Componente | Elementos Enmascarados (`•••• €`) | Elementos Visibles (No Enmascarados) |
| :--- | :--- | :--- |
| **Inicio** | Saldo Familia, Saldo Personal, Deuda en Adelantos, Importes de Últimos 3 Movimientos | Saludo, Nombres de cuenta, "Añadir ingreso", "Añadir gasto", Conceptos, Fechas, "Ver todos" |
| **Movimientos y Filtros** | Importes numéricos de cada movimiento | Concepto, Fecha, Cuentas, Categoría, Iconos |
| **Detalle de Movimiento** | Importe principal del movimiento | Cuentas pagadora/responsable, Categoría, Fecha, Nota |
| **Análisis (Categorías y Evolución)** | Totales por categoría, porcentajes con cifra, etiquetas y valores en gráficos de barras | Nombres de categoría, ranking relativo, fechas/meses en ejes |
| **Nuevo Gasto / Nuevo Ingreso** | Importe introducido en el campo (si se navega con privacidad activa) | Cuentas, Categorías, Teclas del teclado numérico, Fechas |
| **Cuenta y Detalle** | Saldo disponible de la cuenta, Saldo inicial | Nombre de cuenta, Icono, Lista de acciones, Fechas |
| **Adelantos e Historial** | Deuda neta ("Familia te debe •••• €"), Importes de adelantos/devoluciones | Nombres de cuenta, Dirección de deuda, Conceptos, Fechas |
| **Transferencias / Devoluciones** | Saldo pendiente precargado, Importe a transferir | Cuentas origen/destino, Tipo de transferencia, Nota |
| **Ajustes de Saldo** | Delta de importe y saldo resultante | Motivo del ajuste, Cuenta, Fecha |
| **Categorías y Cuentas** | N/A (no maneja importes monetarios directos) | Nombres, Iconos, Pestañas, Favoritas |
| **Datos, CSV y Backup JSON** | **NINGUNO (Cifras reales intactas en archivos y BD)** | Archivos CSV y JSON preservan datos numéricos reales |
| **Módulo Recurrentes (Lista, Detalle, Formulario, Pendientes)** | Importe previsto de reglas, importes en tarjetas de pendientes, importes en revisión | Conceptos, Frecuencias, Próxima fecha, Estados (Activa/Pausada), Modos (Automático/Pendiente) |

### 3. Inventario de Ajustes por Pantalla (`Pantalla actual -> Ajustes necesarios`)

1. **Inicio:**
   - *Actual:* Fondo monocromático blanco/negro/gris, Familia negra, Personal gris claro.
   - *Ajustes:* Fondo blanco roto `#F8F9FA`, azul petróleo `#1A3644`, igual peso visual para Familia y Personal. Acciones rápidas "Añadir ingreso" (`+`) a la izquierda y "Añadir gasto" (`−`) a la derecha. Sección "Últimos movimientos" con 3 items más recientes, enlace "Ver todos" y estado vacío. Botón de ojo de privacidad en la cabecera.
2. **Movimientos y filtros:**
   - *Actual:* Lista monocromática de 2 líneas por día.
   - *Ajustes:* Aplicar fondo blanco roto, azul petróleo en chips seleccionados y máscara `"•••• €"` en importes al activar privacidad.
3. **Detalle y edición de movimiento:**
   - *Actual:* Sheet/pantalla de edición monocromática.
   - *Ajustes:* Azul petróleo en botones y campos, máscara `"•••• €"` en el importe si la privacidad está activa.
4. **Análisis: categorías y evolución:**
   - *Actual:* Gráficos monocromáticos con importes.
   - *Ajustes:* Anillo y barras en azul petróleo. Al activar privacidad, ocultar importes en centro del anillo, ranking y etiquetas/escalas de ejes en el gráfico de barras.
5. **Nuevo gasto:**
   - *Actual:* Formulario con teclado numérico monocromático.
   - *Ajustes:* Paleta azul petróleo, targets de 48 dp en teclado numérico, botón Guardar visible con teclado activo.
6. **Nuevo ingreso:**
   - *Actual:* Formulario de ingreso con lista manual.
   - *Ajustes:* Paleta azul petróleo en selectores de categoría y cuenta.
7. **Cuenta y detalle de cuenta:**
   - *Actual:* Vista de cuenta con saldo real.
   - *Ajustes:* Azul petróleo en tarjeta y botones. Enmascaramiento de saldo disponible e inicial si la privacidad está activa.
8. **Adelantos e historial:**
   - *Actual:* Tarjeta de deuda neta e historial.
   - *Ajustes:* Azul petróleo en cabecera y chips. Enmascaramiento de la deuda neta e importes del historial con `"•••• €"`.
9. **Transferencias y devoluciones:**
   - *Actual:* Formulario de transferencia/devolución.
   - *Ajustes:* Azul petróleo en selector de tipo. Saldo pendiente precargado enmascarado si la privacidad está activa.
10. **Ajustes de saldo:**
    - *Actual:* Formulario con motivo e importe.
    - *Ajustes:* Paleta azul petróleo y campo de importe con soporte de privacidad.
11. **Categorías:**
    - *Actual:* Pestañas Gastos/Ingresos y reordenación.
    - *Ajustes:* Azul petróleo en pestañas, botones de reordenación e icono de estrella de favoritas.
12. **Ajustes de cuentas:**
    - *Actual:* Formulario de edición de nombre e icono.
    - *Ajustes:* Superficies en blanco puro sobre fondo blanco roto y azul petróleo en guardado.
13. **Datos, copia de seguridad y restauración:**
    - *Actual:* Exportación CSV y backup JSON.
    - *Ajustes:* Paleta azul petróleo. Exportaciones e importaciones conservan cifras reales intactas.
14. **Ajustes generales:**
    - *Actual:* Hub de opciones de ajustes.
    - *Ajustes:* Iconos de tarjetas en azul petróleo y fondo blanco roto.
15. **Módulo de Recurrentes (Lista, Formulario, Detalle, Pendientes, Revisión):**
    - *Actual:* Vistas MD3 monocromáticas.
    - *Ajustes:* Aplicar paleta azul petróleo y máscara `"•••• €"` en importes previstos y pendientes cuando la privacidad esté activa.

### 4. Componentes Compartidos a Documentar
- `AccountCard`: Tarjeta de saldo real con paridad Familia/Personal e integración de `MoneyText`.
- `MoneyText`: Componente centralizado para formateo de importes que evalúa el flag global de privacidad (`"•••• €"` o cifra real).
- `MovementRow`: Fila compacta de 2 líneas (concepto + `MoneyText` en L1; categoría + cuentas + fecha en L2).
- `PrimaryActionCard` / `QuickActionButton`: Botón de acción rápida con icono (`+` / `−`), alto mín. 48 dp y azul petróleo.
- `EmptyState`: Vista para estados vacíos con ilustración/icono monocromático y texto explicativo.
- `FilterChip`: Chip de selección con estado activo en azul petróleo (texto blanco) e inactivo en blanco roto con borde suave.
- `MoneyInputField` y `NumericKeypad`: Componentes compartidos por gasto e ingreso, con teclas táctiles de 48 dp, coma, C, retroceso, suma/resta y evaluación decimal exacta en céntimos.
- `CompactTransactionForm`: Estructura compartida de Nuevo gasto y Nuevo ingreso que mantiene importe, selectores esenciales, teclado completo y Guardar visibles en pantallas compactas.
- `TransactionDetailsSheet`: Panel inferior para fecha y datos opcionales, conservando el estado del formulario principal al abrirse o cerrarse.
- `ConfirmationDialog`: Diálogo modal MD3 para confirmaciones críticas.
- `TopAppBar` y `NavigationBar`: Cabecera con título, Ojo de Privacidad (`contentDescription` dinámico) y Ajustes; barra inferior con indicador activo en azul petróleo.
- `RecurrenceStatusBadge`: Chip compacto de estado de regla recurrente (Activa, Pausada, Archivada, Finalizada).

---

### Historias Propuestas (S5)

## P1 — Sistema Visual Global, Rediseño de Inicio y Modo de Privacidad (EN PROGRESO)

### V6-14 — Rediseño visual de Inicio y sección de últimos 3 movimientos (DONE, P1)
**Como usuario** quiero una pantalla de Inicio renovada con la nueva paleta de color y un vistazo rápido a mis últimos 3 movimientos.
- [x] Aplicar la paleta MD3: fondo blanco roto neutro (`#F8F9FA`), azul petróleo (`#1A3644`) y verde/rojo apagados.
- [x] Otorgar exactamente el mismo peso visual a las tarjetas de saldo real de Familia y Personal.
- [x] Botón "Añadir ingreso" (`+`) a la izquierda y "Añadir gasto" (`−`) a la derecha.
- [x] Sección "Últimos movimientos" en Inicio con los 3 movimientos más recientes (gastos, ingresos, transferencias, devoluciones, ajustes, recurrentes) en orden descendente por fecha y creación.
- [x] Cada fila muestra concepto/nota, fecha ("Hoy", "Ayer", "20 nov"), cuenta e importe formatado.
- [x] Botón "Ver todos" que navega a la pantalla completa de Movimientos.
- [x] Estado vacío explícito ("No hay movimientos registrados.") cuando no existan operaciones.
- [x] Optimizado para Galaxy S25 (360-412 dp) con objetivos táctiles mín. 48 dp.

### V6-15 — Modo de privacidad global para ocultar importes (DONE, P1)
**Como usuario** quiero poder ocultar los importes numéricos en toda la app con un toque para consultar la información en público manteniendo la privacidad.
- [x] Botón de ojo con icono conmutables (`Visibility` / `VisibilityOff`) en la cabecera de Inicio junto a Ajustes.
- [x] Preferencia `privacyModeEnabled: Flow<Boolean>` en `UserPreferencesRepository` con persistencia en SharedPreferences.
- [x] Reemplazar visualmente todos los importes por `"•••• €"` en Inicio, Movimientos, detalles, cuentas, Adelantos, Análisis y Recurrentes mediante el componente reutilizable `MoneyText`.
- [x] Ocultar escalas, etiquetas monetarias y valores numéricos en los gráficos de Análisis cuando la privacidad esté activa.
- [x] Mantener visibles conceptos, notas, fechas, nombres de cuenta, categorías y frecuencias.
- [x] `contentDescription` dinámico: "Ocultar importes" cuando están visibles y "Mostrar importes" cuando está la máscara.
- [x] No modificar datos contables de Room, exportaciones CSV ni backups JSON, ni presentarlo como cifrado/seguridad.
- [x] 112 pruebas unitarias pasando al 100% en verde.

### V6-16 — Aplicación del sistema visual a Movimientos y Detalle de movimiento (TODO, P1)
- [ ] Aplicar fondo blanco roto, azul petróleo en chips/cabecera y máscara `"•••• €"` en importes con privacidad activa.
- [ ] **Pruebas necesarias:** Tests de UI Compose para renderizado de lista y detalle con la nueva paleta.

### V6-17 — Aplicación del sistema visual a Análisis (TODO, P1)
- [ ] Aplicar azul petróleo a anillo y gráficos de barras. Ocultar escalas y valores monetarios con privacidad activa.
- [ ] **Pruebas necesarias:** Tests de UI Compose para gráficos con y sin modo de privacidad.

### V6-18 — Rediseño compacto y unificado de Nuevo Gasto y Nuevo Ingreso (TODO, P1)
**Origen:** `UX-010` y `UX-011` de `MEJORAS_POTENCIALES.md`.

**Como usuario** quiero registrar gastos e ingresos desde una pantalla compacta donde el importe, todos los controles del teclado y Guardar estén accesibles sin desplazarme en el uso normal.

- [ ] Crear una estructura visual compartida para Nuevo gasto y Nuevo ingreso, evitando que evolucionen como formularios distintos o dupliquen la lógica del teclado y del importe.
- [ ] Aplicar la paleta azul petróleo, tipografía legible, superficies blanco roto/blanco y objetivos táctiles mínimos de 48 dp.
- [ ] En anchos de 360–412 dp y tamaño de fuente estándar, mantener visibles sin scroll vertical el importe, la selección esencial, el teclado completo —incluidas coma, C y retroceso— y el botón Guardar.
- [ ] Mantener Guardar fijo y accesible; con texto ampliado permitir desplazamiento antes que recortar texto, ocultar controles o reducir objetivos táctiles.
- [ ] Gasto conserva categorías favoritas + Más y los selectores independientes Pagadora/Responsable; Ingreso conserva categoría de ingreso y cuenta destinataria. Ambos comparten jerarquía, espaciado y componentes base.
- [ ] Mover fecha, nota y opciones no esenciales a un `ModalBottomSheet` de Detalles, preservando los valores al abrir, cerrar, rotar o volver al formulario.
- [ ] Añadir suma y resta al teclado propio: mostrar expresión y resultado antes de guardar, aceptar coma decimal y deshabilitar Guardar ante expresiones incompletas o inválidas.
- [ ] Evaluar operaciones con aritmética decimal exacta o céntimos enteros; no usar `Float`/`Double` y persistir únicamente el resultado final como `Long` en céntimos.
- [ ] Rechazar resultados negativos o cero, más de dos decimales y desbordamientos; mantener C y retroceso con comportamiento predecible durante la edición.
- [ ] Respetar el Modo de Privacidad ocultando operandos y resultado monetario sin impedir el uso del teclado.
- [ ] No incluir en esta historia el escaneo de tickets ni la creación de recurrentes: deberán integrarse después mediante sus historias específicas sin volver a alargar el formulario principal.
- [ ] **Pruebas necesarias:** Tests unitarios del evaluador de expresiones; tests de ViewModel para gasto e ingreso; tests UI Compose a 360/412 dp y con texto ampliado; restauración de estado del panel de Detalles; privacidad; y QA funcional en emulador verificando que teclado y Guardar son accesibles.

### V6-19 — Aplicación del sistema visual a Cuentas, Adelantos, Transferencias y Ajustes (TODO, P1)
- [ ] Aplicar paleta azul petróleo y máscara `"•••• €"` en saldos y deudas con privacidad activa.
- [ ] **Pruebas necesarias:** Tests de UI Compose de cuentas y transferencias.

### V6-20 — Aplicación del sistema visual a Categorías, Ajustes de Cuentas y Datos (TODO, P1)
- [ ] Aplicar paleta azul petróleo en gestión de categorías, cuentas y copia/restauración JSON.
- [ ] **Pruebas necesarias:** Tests de UI Compose en hub de ajustes.

### V6-21 — Aplicación del sistema visual a todas las pantallas de Recurrentes V6.1 (TODO, P1)
- [ ] Aplicar paleta azul petróleo y máscara `"•••• €"` en importes previstos/pendientes de recurrentes.
- [ ] **Pruebas necesarias:** Tests de UI Compose en lista, formulario y bandeja de pendientes de recurrentes.

## P1/P2 — Mejoras UX aprobadas tras uso real (TODO)

### V6-22 — Escala tipográfica unificada y presencia semántica de ingresos y gastos (TODO, P1)
**Origen:** `UX-001` y `UX-002` de `MEJORAS_POTENCIALES.md`.

**Como usuario** quiero una tipografía más legible y una diferenciación clara entre ingresos y gastos en toda la aplicación.

- [ ] Definir una escala tipográfica común (`AppTypography`) aumentando legibilidad en botones (`labelLarge` 15–16 sp seminegrita), etiquetas/chips (13–14 sp) y textos de lista (15–16 sp), eliminando tamaños sueltos inconsistentes.
- [ ] Revisar los tonos semánticos de ingreso (verde) y gasto (rojo) y añadir tokens de tinte suave (`IncomeContainer`/`OnIncomeContainer`, `ExpenseContainer`/`OnExpenseContainer`) con contraste WCAG AA, validando los tonos definitivos en dispositivo real.
- [ ] Aplicar el estilo de tinte suave (fondo verde/rojo claro + icono y texto oscuros de alto contraste) en botones de acción de Inicio, filas de movimientos y filtros/análisis, manteniendo siempre signos `+`/`−` e iconografía (nunca depender solo del color).
- [ ] Coordinar su aplicación con `V6-16`..`V6-21` para evitar retrabajo.
- [ ] **Pruebas necesarias:** Tests de UI Compose en 360–412 dp con escalas de fuente del sistema al 100 %, 115 % y 130 % verificando ausencia de recortes y objetivos táctiles `>= 48 dp`, más verificación visual en dispositivo/emulador.

### V6-23 — Resumen del mes actual en las tarjetas de Inicio (TODO, P1)
**Origen:** `UX-009` de `MEJORAS_POTENCIALES.md`.

**Como usuario** quiero ver el resultado del mes actual como dato principal en Inicio para saber cómo va el mes de un vistazo sin perder el saldo disponible.

- [ ] Mostrar en las tarjetas de Familia y Personal el **resultado neto del mes en curso** (`ingresos − gastos`) como cifra principal, indicando explícitamente el nombre del mes (ej. «Resultado de noviembre»), manteniendo la paridad visual entre ambas cuentas.
- [ ] Mostrar la caja acumulada real como dato secundario claramente diferenciado bajo la etiqueta «Saldo disponible» (sin mezclar terminología).
- [ ] Regla contable: imputar gastos a `responsibleAccountId`, ingresos a la cuenta destinataria, y excluir transferencias, devoluciones y ajustes de saldo del cálculo mensual.
- [ ] Mantener el comportamiento actual de navegación al pulsar una tarjeta de Inicio (abrir Detalle de cuenta).
- [ ] Respetar el Modo de Privacidad (`•••• €`) en ambas cifras.
- [ ] Al implementar, actualizar `docs/V6_SPEC.md` y `AGENTS.md` para reflejar el cambio respecto a `V6-04`/`V6-14`.
- [ ] **Pruebas necesarias:** Tests unitarios en `HomeViewModelTest` (cambio de mes, meses sin movimientos, resultados negativos, cuentas con adelantos) y tests UI Compose en 360–412 dp y modo privacidad.

### V6-24 — Reorganización de pantallas y formulario compacto de Recurrentes (TODO, P1)
**Origen:** `UX-005` y `UX-006` de `MEJORAS_POTENCIALES.md` (coordinar con `V6-21`).

**Como usuario** quiero gestionar mis reglas recurrentes desde pantallas separadas y compactas sin scroll excesivo.

- [ ] Separar la Lista de reglas del Formulario de creación y edición en pantallas dedicadas (evitar apilar el formulario encima de la lista).
- [ ] Al guardar o cancelar una creación/edición, volver a la lista conservando la pestaña de estado activa y la posición de scroll.
- [ ] Reducir el desplazamiento vertical agrupando opciones secundarias en secciones plegables o panel inferior y separando claramente Lista, Formulario, Detalle y Bandeja de pendientes.
- [ ] **Pruebas necesarias:** Tests de UI Compose y navegación en 360–412 dp verificando la separación de pantallas, preservación de pestaña/scroll y ausencia de controles ocultos.

### V6-25 — Crear recurrencia desde Nuevo gasto y Nuevo ingreso (TODO, P1)
**Origen:** `UX-004` de `MEJORAS_POTENCIALES.md`. **Dependencias:** `V6-18` y `V6-24`.

**Como usuario** quiero poder convertir un gasto o ingreso en recurrente en el momento de crearlo sin alargar el formulario rápido.

- [ ] Añadir una acción «Hacer recurrente» dentro del panel inferior de Detalles (`ModalBottomSheet`) de Nuevo gasto y Nuevo ingreso.
- [ ] Al pulsarla, abrir la pantalla dedicada de creación de recurrencia (`V6-24`) precargada con los datos ya introducidos (tipo, importe, categoría, cuentas pagadora/responsable o destinataria, concepto y fecha).
- [ ] Mantener intacto el flujo rápido en un paso para movimientos puntuales y las reglas contables V6.
- [ ] **Pruebas necesarias:** Tests unitarios de precarga/mapeo y tests UI Compose verificando el acceso desde Nuevo gasto y Nuevo ingreso sin alterar el diseño compacto.

### V6-26 — Accesos directos (App Shortcuts) para Nuevo gasto y Nuevo ingreso (TODO, P2)
**Origen:** `UX-003` de `MEJORAS_POTENCIALES.md`.

**Como usuario** quiero abrir directamente Nuevo gasto o Nuevo ingreso desde la pantalla de inicio de Android para registrar movimientos en menos pasos.

- [ ] Implementar Android App Shortcuts para «Nuevo gasto» y «Nuevo ingreso» (accesibles al mantener pulsado el icono de la app y fijables en el escritorio).
- [ ] Cada acceso directo abre directamente el formulario compacto correspondiente (`V6-18`) respetando las últimas selecciones recordadas.
- [ ] Exigir siempre confirmación explícita pulsando Guardar (nunca registrar un movimiento con un solo toque).
- [ ] **Pruebas necesarias:** Tests de resolución de intents/rutas de navegación y verificación funcional en emulador/dispositivo Android.

---

### Riesgos y Decisiones Cerradas
1. **Riesgo de Deducción Proporcional en Gráficos:** **Decisión cerrada (2026-11-24):** En modo privacidad es suficiente con suprimir la escala y las etiquetas monetarias en los gráficos de Análisis (no se aplanan las barras a altura uniforme).
2. **Formato de la Revisión Individual de Pendientes (`REC-UI-05`):** **Decisión cerrada (2026-11-24):** Implementar como `ModalBottomSheet` sobre la bandeja de pendientes (mejor encaje con teclado/edición en 360–412 dp y coherente con `V6-18` y `V6-24`).


## Criterio de cierre V6
Todas las funcionalidades V6, Recurrentes V6.1, Sistema Visual Global, Modo de Privacidad y Estabilización Senior se implementan, documentan y verifican con pruebas unitarias/de integración, Android Lint, compilación APK y QA visual/interactiva en emulador Android.
