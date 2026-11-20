# SDD — Family Expenses V6.0 (Android)

**Estado:** especificación objetivo aprobada; no implementada todavía. **Fuente de verdad:** [V6_SPEC.md](V6_SPEC.md). Fecha: 2026-11-20.

## 1. Objetivo y alcance
Aplicación Android offline-first que sustituye registro de gastos e ingresos y seguimiento de adelantos familiares. Dos cuentas iniciales y únicas en V6: **Familia** y **Personal**. Diseño ampliable a más cuentas, sin exponer alta de cuentas todavía. Registro rápido (<3 segundos como objetivo a medir, no garantía).

**Incluido V6:** saldos reales de caja, ingresos, gastos con cuenta pagadora/responsable separadas, adelantos bidireccionales, devoluciones parciales o totales, transferencias normales, ajustes auditables, categorías configurables, 7 favoritas manuales en gastos, lista manual en ingresos, Movimientos, Análisis, exportación CSV, copias/restauración local, recurrencias existentes conservadas y adaptadas a las nuevas reglas.

**Excluido V6:** cloud sync, multiusuario, login, PIN adicional, OCR, conexión bancaria y creación de cuentas adicionales en UI.

## 2. Invariantes de dominio (obligatorios)
1. Cada gasto tiene `responsibleAccountId` y `payingAccountId`. El gasto pertenece a la responsable para presupuestos/análisis, pero **solo descuenta caja de la pagadora**.
2. Saldo real de cuenta: apertura + ingresos recibidos - gastos efectivamente pagados + transferencias entrantes - salientes + ajustes; NO es un saldo mensual por responsabilidad.
3. Si pagadora ≠ responsable, crear un adelanto vinculado al gasto en dirección pagadora → responsable, con trazabilidad y sin gasto duplicado.
4. Deuda neta firmada: adelantos Personal→Familia - adelantos Familia→Personal - devoluciones Familia→Personal + devoluciones Personal→Familia. Positivo significa Familia debe a Personal. Negativo significa Personal debe a Familia.
5. Transferencia normal mueve caja entre cuentas, sin cambiar deuda ni generar ingreso/gasto estadístico.
6. Devolución es una transferencia explícitamente clasificada como devolución; mueve caja, reduce deuda neta y admite importe parcial. El origen/destino propuesto depende del signo de la deuda y el importe no supera el pendiente neto.
7. Ajustar saldo genera operación auditable con motivo, no ingreso o gasto estadístico.
8. Editar o eliminar gasto, adelanto o devolución requiere recalcular saldos y deuda, con reglas de consistencia si ya hay devoluciones vinculadas.
9. Importes como `Long` en céntimos, EUR, transacciones de base de datos atómicas para efectos contables, identificadores estables y sin redondeos binarios.

**Ejemplo obligatorio:** Personal paga 80 EUR de una compra atribuida a Familia: Personal -80 de caja, Familia 0 de caja, gasto de Familia +80 en análisis, deuda Familia→Personal +80. Familia devuelve 30: Familia -30 de caja, Personal +30 de caja, deuda 50, sin ingresos/gastos nuevos.

## 3. Modelo objetivo orientativo (detallar migración antes de tocar Room)
- `Account`: id estable, tipo FAMILY|PERSONAL, nombre, icono, saldo inicial, activo, timestamps.
- `Category`: id, nombre, icono, tipo EXPENSE|INCOME, orden manual, favorita y posición de favorita cuando proceda, archivada, timestamps.
- `Transaction`: id, tipo EXPENSE|INCOME, importe en céntimos, responsable para gasto, pagadora para gasto, destinataria para ingreso, categoría, fecha, descripción/nota, origen MANUAL|AUTO_RECURRENT, regla recurrente opcional, timestamps.
- `Transfer`: id, origen, destino, importe, tipo NORMAL|REIMBURSEMENT, fecha, motivo, timestamps; no tratar como ingreso/gasto.
- `Advance`: referencia a gasto, pagadora, responsable, importe o derivado verificable del gasto; no duplicar entradas contables.
- `BalanceAdjustment`: id, cuenta, delta, motivo, fecha, timestamps.
- `RecurrenceRule`: conservar funcionalidad mensual y adaptarla a pagadora/responsable o destinataria.
- `Settings`: categorías/última selección, preferencias y versión de esquema.

**Nota:** son entidades conceptuales, no instrucciones para crear tablas directamente. Revisar esquema y migrar registros de `paid_from_personal`, `ReimbursementLedger` y `Settlement` sin duplicar operaciones de caja. Los registros históricos del sistema anterior pueden requerir decisión explícita de conversión porque sus liquidaciones no movían caja. **No inventar transferencias históricas** ni alterar saldos sin revisión.

## 4. Pantallas y navegación
### Inicio
Tarjetas de saldo real Familia (negra) y Personal (gris claro), dos acciones grandes Gasto e Ingreso; adelantos siempre visibles incluso con deuda 0. Sin total agregado, gráficos, movimientos recientes ni balance mensual. Ajustes en cabecera. Barra inferior Inicio / Movimientos / Análisis.

### Nuevo gasto
Importe con teclado numérico propio: coma, máximo 2 decimales, C, retroceso. Cuadrícula 2x4 con 7 favoritas manuales más «Más». **Dos selectores independientes** «Pagado desde» y «Gasto de»; recordar última combinación y categoría. Importe 0 al abrir, hoy por defecto, detalles opcionales plegados. Aviso de adelanto si las cuentas difieren. Guardar en una sola pantalla; deshabilitado si importe 0; confirmación si se sale con cambios. Guardado vuelve a Inicio y ofrece Deshacer.

### Nuevo ingreso
Importe, cuenta destinataria, categoría de ingreso ordenada manualmente (sin favoritas), fecha y detalles opcionales. Recordar últimas elecciones de ingreso independientemente del gasto.

### Movimientos
Filas de **dos líneas** agrupadas por día, orden descendente, sin resumen de periodo. Filtros Todas/Familia/Personal según cuenta responsable del gasto, destinataria del ingreso y participación en transferencias. Búsqueda y filtros por tipo, categoría y periodo. Periodos: mes, últimos 12 meses, año natural, personalizado. Adelantos representados por **un solo gasto** con indicación de pagadora. Abrir detalle para editar/eliminar; preservar filtros y scroll al volver.

### Análisis
Pestañas Categorías (predeterminada, anillo y ranking) y Evolución (una sola serie de barras). Selector Gastos/Ingresos por separado, filtro cuenta y periodo. Gastos imputados a responsable. Excluir transferencias, devoluciones y ajustes. Pulsar categoría/barra abre Movimientos filtrado; volver recupera pestaña, filtros y selección. Meses futuros sin barra, no falsos ceros.

### Detalle de cuenta
Saldo real, Transferir, Movimientos y Adelantos. Sin lista reciente. Transferir exige elección explícita de Normal o Devolución; esta última propone dirección e importe pendiente y admite devolución parcial. Saldo inicial configurado; posteriores correcciones mediante ajustes auditables.

### Adelantos
Saldo neto en ambas direcciones, estado sin deudas visible, historial cronológico único con filtros Todos/Adelantos/Devoluciones, trazabilidad del gasto y devoluciones.

### Ajustes y datos
Categorías de gastos: hasta siete favoritas ordenadas manualmente; octava requiere sustitución, «Más» da acceso al resto y creación. Categorías de ingresos orden manual sin favoritas. Archivo conserva histórico. Dos cuentas renombrables, sin creación de nuevas. Exportación CSV; copia completa portable; restauración validada y con confirmación antes de sustituir. Sin PIN, login ni sincronización.

## 5. Persistencia, recurrencias y protección de datos
Kotlin, Jetpack Compose, Room, Hilt, ViewModel/StateFlow, Navigation Compose, WorkManager. Arquitectura `data/domain/ui`; cálculos contables en dominio y transacciones de Room; UI sin contabilidad embebida. Offline y almacenamiento local en un dispositivo. Copia guardada fuera del móvil necesaria para recuperación fiable. Identificadores globalmente estables para futura sincronización (sin implementarla).

Las recurrencias existentes deben seguir funcionando; al emitir un gasto recurrente se debe aplicar la misma regla pagadora/responsable/adelanto que a uno manual. No eliminar funcionalidad previa.

## 6. Criterios de aceptación y QA
- Casos de caja y deuda en ambas direcciones, devolución parcial/totales y transferencia normal explícita.
- Migración de datos antiguos sin pérdida, sin gastos duplicados y sin reconstruir como pagos reales liquidaciones antiguas sin evidencia.
- Edición/borrado y saldos coherentes con datos históricos y devoluciones existentes.
- Análisis no incluye transferencias, ajustes ni devoluciones en ingresos/gastos.
- Categorías archivadas siguen visibles en histórico.
- Restauración sobre datos existentes requiere comprobación y confirmación; exportación CSV usable.
- Android 360/412 dp, objetivos táctiles en torno a 48 dp, contraste, áreas seguras, teclado, accesibilidad y offline.

## 7. Estado de implementación
Esta SDD especifica el **objetivo V6**, no la versión actualmente compilada. Las historias de [BACKLOG.md](BACKLOG.md) permanecen TODO hasta desarrollo y pruebas; [SPRINT.md](SPRINT.md) gobierna el siguiente trabajo autorizado. Historial MVP conservado en `docs/archive/MVP_LEGACY.md` para referencia, **nunca como requisito vigente**. No programar sin autorización expresa.
