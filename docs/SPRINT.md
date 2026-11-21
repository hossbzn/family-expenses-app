# SPRINT — Preparación de Family Expenses V6

**Estado actual:** documentación V6 alineada; **implementación NO AUTORIZADA / NO INICIADA**.
**Fuente de verdad:** [V6_SPEC.md](V6_SPEC.md) → [sdd.md](sdd.md) → [BACKLOG.md](BACKLOG.md).

## Objetivo de la fase actual (documentación)
- [x] Cerrar decisiones visuales y funcionales V6.
- [x] Definir reglas de caja vs cuenta responsable y deuda neta bidireccional.
- [x] Reescribir SDD y AGENTS con V6 como única norma activa.
- [x] Organizar backlog V6 y separar registro histórico del MVP.
- [x] Documentación base V6 integrada en main mediante PR #1.
- [ ] Autorizar explícitamente desarrollo V6.

## Sprint V6-S1 propuesto — pendiente de autorización
**Meta:** motor contable correcto, migración segura y pruebas antes de rediseñar UI.
- [ ] V6-12: auditar código actual, esquema Room, datos y efectos de settlements anteriores.
- [ ] V6-01: diseñar/implementar cuentas pagadora y responsable y cálculo de caja real.
- [ ] V6-02: deuda neta bidireccional y devolución parcial que mueve caja.
- [ ] Especificar tratamiento de liquidaciones históricas sin movimientos de caja; bloquear conversión ambigua.
- [ ] Tests unitarios, migración y regresión; conservar reglas recurrentes.
**Criterio de salida:** test de Personal paga 80 € de Familia y devolución 30 €, saldos correctos, sin doble gasto, migración sin pérdida.

## Sprint V6-S2 propuesto — pendiente
V6-03 formulario Nuevo gasto, V6-10 Nuevo ingreso, V6-04 Inicio, V6-07 detalle/transferencias. Accesibilidad de pantallas compactas.

## Sprint V6-S3 propuesto — pendiente
V6-05 Movimientos, V6-06 Análisis, V6-08 Adelantos; drill-down y restauración de filtros/estado.

## Sprint V6-S4 propuesto — pendiente
V6-09 Categorías, V6-11 copia/restauración/CSV, V6-13 QA Android, offline y regresión final.

## Línea de trabajo propuesta — Recurrentes V6.1 (sin autorización de programación)
**Fuente:** [RECURRENTES_V6_1.md](RECURRENTES_V6_1.md), historias REC-01 a REC-08 de [BACKLOG.md](BACKLOG.md).

**Puerta previa:** completar V6-12 (auditar/migrar datos) y estabilizar V6-01/V6-02 (motor de caja/adelantos). Evitar implementar recurrentes contra modelo contable heredado.

- **REC-S0 — Diseño y auditoría:** REC-01. Examinar Room, Worker, compatibilidad de instalaciones existentes y estrategia de migración. Sin código hasta autorización.
- **REC-S1 — Reglas y calendario:** REC-02/REC-03. Edición futura, estados, archivo/restauración, frecuencias y tests de calendario.
- **REC-S2 — Ejecuciones:** REC-04/REC-05. Motor idempotente, modo automático/pendiente, reconciliación y revisión individual.
- **REC-S3 — Experiencia y calidad:** REC-06/REC-07/REC-08. Confirmación múltiple atómica, avisos/permiso, backup, migración y regresión en dispositivo.
- **Criterio de salida:** cero duplicados, saldos/deudas correctos tras confirmar, cero efecto de pendientes, ninguna pérdida de datos reales, pruebas en móvil, sin generar periodos pausados.

**Situación:** diseño funcional aprobado y documentado; **todas las historias REC siguen TODO**. Este plan no autoriza implementar ni modificar el APK instalado.

## Reglas de ejecución
No interpretar el antiguo MVP como V6 ya terminada. Actualizar estado de historias solo tras evidencia verificable. Si un cambio afecta saldos o histórico, priorizar transacciones atómicas, migración y pruebas. No alterar el código en esta fase documental.
