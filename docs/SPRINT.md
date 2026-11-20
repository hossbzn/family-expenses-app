# SPRINT — Preparación de Family Expenses V6

**Estado actual:** documentación V6 alineada; **implementación NO AUTORIZADA / NO INICIADA**.
**Fuente de verdad:** [V6_SPEC.md](V6_SPEC.md) → [sdd.md](sdd.md) → [BACKLOG.md](BACKLOG.md).

## Objetivo de la fase actual (documentación)
- [x] Cerrar decisiones visuales y funcionales V6.
- [x] Definir reglas de caja vs cuenta responsable y deuda neta bidireccional.
- [x] Reescribir SDD y AGENTS con V6 como única norma activa.
- [x] Organizar backlog V6 y separar registro histórico del MVP.
- [ ] Revisar y aprobar esta propuesta documental en PR.
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

## Reglas de ejecución
No interpretar el antiguo MVP como V6 ya terminada. Actualizar estado de historias solo tras evidencia verificable. Si un cambio afecta saldos o histórico, priorizar transacciones atómicas, migración y pruebas. No alterar el código en esta fase documental.
