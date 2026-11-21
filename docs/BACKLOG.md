# BACKLOG — Family Expenses V6.0

**Estado:** diseño aprobado, implementación V6 **NO INICIADA**. Leyenda: TODO | IN_PROGRESS | REVIEW | DONE | BLOCKED. Fuente: [V6_SPEC.md](V6_SPEC.md) y [sdd.md](sdd.md). Los DONE del MVP anterior están archivados en [archive/MVP_LEGACY.md](archive/MVP_LEGACY.md) y no indican cumplimiento de V6.

## P0 — Modelo económico, migración y registro fundamental

### V6-01 — Saldos de caja reales (TODO)
**Como usuario** quiero ver saldo real en Familia/Personal y distinguir quién paga de a quién corresponde cada gasto.
- [ ] Definir contratos de cuenta pagadora/responsable, ingreso y ajuste auditable.
- [ ] Calcular caja por pagadora, imputar análisis a responsable.
- [ ] Probar caso Personal paga 80 € de Familia → Personal -80 €, Familia 0 €, gasto Familia 80 €.
- [ ] Probar saldos iniciales, ingresos, ajustes y ausencia de redondeos.

### V6-02 — Adelantos bidireccionales y devoluciones parciales (TODO)
- [ ] Generar deuda vinculada a gasto sin duplicarlo al cambiar pagadora/responsable.
- [ ] Mostrar saldo neto con ambos signos y estado cero.
- [ ] Permitir devolución parcial/completa que mueve caja en ambas cuentas y reduce deuda.
- [ ] Separar explícitamente devolución de transferencia normal; probar sin alteración de estadísticas.
- [ ] Recalcular al editar/eliminar; tratar operaciones ya compensadas de forma consistente.

### V6-12 — Migración y protección de datos existentes (TODO)
- [ ] Auditar Room, ledger, settlements, saldos actuales y recurrencias.
- [ ] Diseñar migraciones sin borrado de datos ni inventar movimientos históricos.
- [ ] Pruebas de regresión sobre datos históricos y recurrencias AUTO_RECURRENT.
- [ ] Validar correcciones de saldos y conservación de reglas recurrentes.

### V6-03 — Registro rápido de gasto (TODO)
- [ ] Teclado decimal coma/C/retroceso, máximo 2 decimales, Guardar deshabilitado con 0.
- [ ] 7 favoritas manuales + Más, última selección y última pareja de cuentas.
- [ ] Selectores separados Pagado desde / Gasto de, aviso adelanto, fecha hoy, detalles plegados.
- [ ] Guardado en un paso, Deshacer, confirmación al descartar cambios, Android compacto.

### V6-04 — Inicio V6 (TODO)
- [ ] Dos tarjetas igual jerarquía, Familia negra/Personal gris claro, saldo de caja real.
- [ ] Botones Gasto / Ingreso; bloque Adelantos siempre visible incluso en cero.
- [ ] Sin movimientos recientes ni totales del mes; menú inferior solo tres destinos.

## P1 — Navegación, análisis y administración

### V6-05 — Movimientos (TODO)
- [ ] Filas de dos líneas agrupadas por día descendente, sin resumen mensual.
- [ ] Filtro principal responsable/destinataria; transferencias por participación.
- [ ] Búsqueda, categoría, tipo, mes, 12 meses, años y personalizado.
- [ ] Detalle editar/borrar; restaurar filtros al volver; sin duplicar adelantos.

### V6-06 — Análisis (TODO)
- [ ] Categorías: anillo monocromático, ranking e importes/porcentajes.
- [ ] Evolución: barras de un solo tipo (Gastos o Ingresos), por periodo.
- [ ] Periodos y filtros compartidos; drill-down a Movimientos con retorno de estado.
- [ ] Excluir transferencias, devoluciones y ajustes de ingreso/gasto.

### V6-07 — Detalle cuenta y transferencias (TODO)
- [ ] Saldo real; accesos Transferir, Movimientos y Adelantos, sin recientes.
- [ ] Transferencia NORMAL vs DEVOLUCIÓN con elección explícita.
- [ ] Propuesta de dirección/importe en devolución y tope por deuda; ajustes auditables.

### V6-08 — Adelantos, historial (TODO)
- [ ] Deuda neta bidireccional y sin deudas; historial cronológico único.
- [ ] Filtros Todos / Adelantos / Devoluciones; trazabilidad operación original.

### V6-09 — Categorías configurables (TODO)
- [ ] Gasto: siete favoritas ordenadas manualmente, octava con sustitución, Más para restantes.
- [ ] Editar nombre/icono/tipo/orden; archivar sin afectar histórico y análisis.
- [ ] Ingreso: lista ordenable manualmente, sin favoritas.

### V6-10 — Nuevo ingreso (TODO)
- [ ] Categoría de ingresos, cuenta destinataria, importe, fecha y detalles opcionales.
- [ ] Recordar últimas elecciones independientemente de Nuevo gasto.

### V6-11 — Datos y seguridad (TODO)
- [ ] Exportación CSV validada en Excel.
- [ ] Copia portable íntegra y restauración con validación/confirmación.
- [ ] Pruebas de pérdida/recuperación local, sin PIN ni nube.

### V6-13 — Validación visual Android y calidad (TODO)
- [ ] 360 y 412 dp; contraste, accesibilidad, áreas seguras, teclado y botones táctiles.
- [ ] Validar flujos completos y rendimiento de registro, objetivo <3 segundos a medir.
- [ ] Offline y persistencia; navegación y estados al volver.


## P1/P2 — Recurrentes V6.1 (ESPECIFICADO, NO IMPLEMENTADO)
**Fuente:** [RECURRENTES_V6_1.md](RECURRENTES_V6_1.md). Estas historias quedan TODO; dependen de V6-12 y del motor de caja/adelantos V6-01/V6-02. No alterar datos históricos del móvil.

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
No hay historias V6 DONE hasta código, migración y pruebas. No programar sin autorización explícita. El histórico antiguo nunca es fuente de requisitos V6.
