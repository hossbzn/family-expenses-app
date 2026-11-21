# BACKLOG — Family Expenses V6.0

**Estado:** diseño aprobado, Sprint S1 completado con éxito. Leyenda: TODO | IN_PROGRESS | REVIEW | DONE | BLOCKED. Fuente: [V6_SPEC.md](V6_SPEC.md) y [sdd.md](sdd.md). Los DONE del MVP anterior están archivados en [archive/MVP_LEGACY.md](archive/MVP_LEGACY.md) y no indican cumplimiento de V6.

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

## P1/P2 — Recurrentes V6.1 (ESPECIFICADO, NO IMPLEMENTADO)
**Fuente:** [RECURRENTES_V6_1.md](RECURRENTES_V6_1.md). Estas historias quedan TODO; requieren comprobar la compatibilidad con el motor V6-01/V6-02 ya documentado como DONE y auditar la migración de reglas instaladas (REC-01). No alterar datos históricos del móvil.

### REC-01 — Auditoría y migración segura (TODO, P0)
- [ ] Auditar reglas/Worker/Room existentes y reglas mensuales día 1, datos reales instalados.
- [ ] Diseñar esquema con regla, ocurrencia y transacción económica vinculadas; migración no destructiva.
- [ ] Tests de actualización desde base antigua y preservación de movimientos/recurrentes.

### REC-02 — Edición y ciclo de vida (TODO, P1)
- [ ] Editar solo futuras ocurrencias, dejando instantáneas existentes intactas.
- [ ] Pausar/reactivar, archivar/restaurar, finalizar y consultar historial.
- [ ] Reactivar archivada con nueva fecha de inicio (próxima ejecución futura por defecto), sin recuperar periodos suspendidos.

### REC-03 — Frecuencias y calendario (TODO, P1)
- [ ] Semanal, mensual (día 1-31), cada 2/3/6 meses y anual.
- [ ] Último día si fecha nominal no existe; ancla de intervalos; inicio obligatorio y fin opcional.
- [ ] No duplicar ejecuciones por reintentos, cambios de fecha o reconciliación.

### REC-04 — Modos y generación de ocurrencias (TODO, P1)
- [ ] Modo por regla automático o pendiente de confirmar.
- [ ] Automático registra por motor económico V6; pendiente no modifica caja/deuda/análisis.
- [ ] Recuperar fechas activas vencidas al reabrir, omitiendo períodos pausados/archivados.

### REC-05 — Bandeja y gestión individual (TODO, P1)
- [ ] Mostrar pendientes, revisar importe/fecha (programada por defecto), confirmar.
- [ ] Posponer revisión sin alterar fecha económica o futuras fechas; omitir ejecución sin generar gasto.
- [ ] No cambiar pendientes al editar su regla.

### REC-06 — Confirmación múltiple (TODO, P1)
- [ ] Selección y revisión de fechas e importes por ocurrencia.
- [ ] Confirmación atómica todo-o-nada e idempotente; tratar errores sin duplicar.

### REC-07 — Inicio y avisos (TODO, P2)
- [ ] Aviso discreto y acceso a pendientes desde Inicio, siempre que existan.
- [ ] Notificaciones Android opcionales desde Ajustes, agrupadas, respetando permiso y sin repetición molesta.
- [ ] Notificaciones no condicionan generación local.

### REC-08 — QA y respaldo (TODO, P1)
- [ ] Tests de caja/adelantos entre pagadora y responsable; ingresos recurrentes.
- [ ] Integración WorkManager, Room, migración y actualizaciones sin pérdida.
- [ ] Backup/restauración incorpora reglas/ocurrencias y vínculos cuando esté implementado.


## Criterio de cierre V6
Todas las funcionalidades V6 (Sprints S1–S4) están implementadas, documentadas y verificadas con 97 pruebas unitarias/de integración, `assembleDebug` y QA visual/interactiva en emulador Android (412 dp y 360 dp).
