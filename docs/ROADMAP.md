# ROADMAP — Family Expenses V6 / Recurrentes V6.1

**Importante:** la app MVP se está utilizando en un móvil, pero el código y las funcionalidades actuales **no equivalen** a V6. El antiguo roadmap describía avances del MVP (incluidas reglas mensuales) y puede consultarse como historial al final. Ninguna funcionalidad V6/V6.1 debe considerarse entregada solo por figurar como completada en el MVP.

## Etapa 0 — Especificaciones y preparación
- [x] Especificación funcional y visual de V6: [V6_SPEC.md](V6_SPEC.md).
- [x] Especificación funcional de Recurrentes V6.1: [RECURRENTES_V6_1.md](RECURRENTES_V6_1.md).
- [x] Historias de implementación documentadas en [BACKLOG.md](BACKLOG.md).
- [ ] Auditoría de compatibilidad del código y de la base local instalada.
- [ ] Aprobación explícita para programar.

## Etapa 1 — Fundamentos contables V6 (marcados DONE en backlog, verificar en dispositivo)
- [x] V6-12: persistencia V6 documentada como completada; **no asumir migración segura desde una instalación MVP antigua**.
- [x] V6-01: caja real y distinción cuenta pagadora/responsable (marcado DONE).
- [x] V6-02: deuda bidireccional y devoluciones parciales que mueven caja (marcado DONE).
- [ ] Comprobar explícitamente migración no destructiva y pruebas con datos de instalaciones antiguas antes de actualizar el móvil.

## Etapa 2 — Experiencia principal V6 (marcada DONE en backlog, verificar en dispositivo)
- [x] Registro rápido de gastos e ingresos y categoría favorita (marcado DONE).
- [x] Inicio con saldos reales, adelantos y navegación definitiva (marcado DONE).
- [x] Movimientos, cuentas, transferencias, análisis y categorías (marcado DONE).
- [x] Copia/restauración y QA Android (marcado DONE).

## Etapa 3 — Recurrentes V6.1 (aprobado para desarrollo)
**Dependencia:** El motor de operaciones V6 (S1-S4) está 100% completado, validado y funciona offline. La persistencia actual se inició limpia en v1 (sin datos de MVP), por lo que se ajustará la estrategia para no necesitar migración compleja.
- [ ] REC-01: Ajustar esquema Room (ampliar `RecurrenceRuleEntity` y añadir `RecurrenceOccurrenceEntity`) para soporte de ocurrencias independientes, sin requerir migración destructiva (al estar recién inicializado V6).
- [ ] REC-02/03: edición/archivo/pausa/reactivación y cuatro frecuencias (Semanal, Mensual, Multi-mes, Anual).
- [ ] REC-04: modos automático/pendiente y motor idempotente (WorkManager + Room transaccional).
- [ ] REC-05/06: bandeja de pendientes, revisión individual y múltiple atómica.
- [ ] REC-07: aviso discreto en Inicio (UI) y notificaciones Android opcionales.
- [ ] REC-08: pruebas end-to-end (QA en dispositivo), asegurar inclusión en backup CSV/JSON y protección frente a reinicios.

## Regla de entrega
Todo el desarrollo V6.1 debe escribirse mediante commits atómicos y PR, cubriendo DAO, WorkManager, ViewModels y UI, con pruebas unitarias para cada caso. No alterar el comportamiento contable (invariantes de V6).

---

## Histórico del MVP anterior (no representa V6 implementada)

## Estado actual

Fase 1 completada.  
Fase 2 completada.  
Fase 3 completada en su parte basica.  
Fase 4 completada en su primer corte funcional.  
Fase 5 completada en su primer corte funcional.  
Fase 6 completada en su primer corte.  
Fase 7 completada en su version MVP con rango de vigencia.

---

## Fase 1 - Base tecnica

Objetivo: proyecto Android compilando.

- [x] Crear proyecto Android
- [x] Configurar Compose
- [x] Configurar Room
- [x] Configurar Hilt
- [x] Crear entidades
- [x] Crear AppDatabase

---

## Fase 2 - Persistencia basica

- [x] DAO
- [x] Repositorios basicos
- [x] Seed inicial
- [x] Pantalla base para verificar datos

---

## Fase 3 - Registro de movimientos

- [x] AddTransactionScreen
- [x] Guardar ingresos y gastos
- [x] Historial basico
- [ ] Edicion y borrado

---

## Fase 4 - Caso especial familiar

- [x] Gasto familiar pagado con personal
- [x] reimbursement_ledger
- [x] Calculo pendiente

---

## Fase 5 - Liquidacion

- [x] Pantalla PendingSettlement
- [x] Boton liquidar
- [x] Creacion Settlement
- [x] Reset del pendiente

---

## Fase 6 - Dashboard

- [x] Saldo personal mensual
- [x] Saldo familiar mensual
- [x] Pendiente familia -> personal
- [x] Acciones rapidas por saldo (+ / -)
- [x] Layout adaptable con scroll de seguridad
- [ ] Ultimos movimientos

---

## Fase 7 - Recurrentes

- [x] Reglas recurrentes
- [x] Worker de generacion
- [x] Rango con fecha inicio y fecha fin opcional
- [ ] Edicion de reglas

---

## Fase 8 - UX rapida

- [ ] Quick expense
- [ ] Recordar ultima categoria
- [ ] Recordar ultima cuenta

---

## Fase 9 - Mejoras

- [ ] Filtros de historial
- [ ] Notificaciones recurrentes
- [ ] Mejoras de UI
