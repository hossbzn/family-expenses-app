# AGENTS.md

## Proyecto
App Android en Kotlin para control de gastos personales y familiares.

## Objetivo del producto
La app debe permitir gestionar:
- ingresos y gastos personales
- ingresos y gastos familiares
- gastos familiares pagados con cuenta personal
- deuda acumulada familia → personal
- liquidación total de esa deuda
- movimientos recurrentes automáticos

Objetivo UX principal:
- registrar un gasto en menos de 3 segundos

---

## Fuente de verdad
Antes de hacer cambios, lee siempre estos archivos:
1. `docs/SDD.md`
2. `docs/BACKLOG.md`
3. `docs/SPRINT.md`

Si hay conflicto:
- `docs/SDD.md` define el comportamiento del producto
- `docs/BACKLOG.md` define qué historias existen
- `docs/SPRINT.md` define qué hay que implementar ahora

No inventes requisitos fuera de esos documentos.

---

## Stack técnico obligatorio
- Kotlin
- Jetpack Compose
- Room
- Hilt
- ViewModel
- StateFlow
- Navigation Compose
- WorkManager

No añadas librerías nuevas salvo que sean estrictamente necesarias.

---

## Principios de arquitectura
- Offline-first
- Arquitectura por capas: `data`, `domain`, `ui`
- Room es la source of truth
- Casos de uso pequeños y testeables
- La lógica de negocio debe vivir en `domain`, no en Composables
- Los Composables deben ser simples y centrados en renderizado/acciones UI
- Los ViewModels coordinan estado y casos de uso
- Usa `Flow` en DAO y repositorios donde tenga sentido
- Importes siempre en `minor units` usando `Long`

---

## Reglas de dominio críticas

### Cuentas
- En el MVP existen dos cuentas iniciales: `Personal` y `Familiar`
- El modelo debe soportar múltiples cuentas en el futuro
- Todo movimiento pertenece a una cuenta

### Categorías
- Todo gasto debe tener categoría obligatoria
- La app trae categorías iniciales, pero son editables
- No borrar categorías con movimientos asociados

### Caso especial clave
Existe un tipo de gasto especial:
- gasto familiar pagado con cuenta personal

Si:
- `account = FAMILY`
- `paidFromPersonal = true`

Entonces:
- afecta al saldo mensual familiar
- NO afecta al saldo mensual personal
- genera una entrada en `reimbursement_ledger`

### Pendiente familia → personal
- El pendiente se calcula sumando gastos familiares pagados con cuenta personal no liquidados
- Debe ser trazable por gasto
- Si se edita o borra un gasto afectado, el pendiente se recalcula

### Liquidación
- La liquidación siempre es total
- No hay liquidaciones parciales en MVP
- Liquidar no crea movimientos en cuentas
- Liquidar crea un `Settlement`
- Liquidar marca todos los registros pendientes del `reimbursement_ledger` como `SETTLED`
- Tras liquidar, el pendiente mostrado debe ser 0

### Recurrentes
- Los movimientos recurrentes se generan solo cuando llega su fecha
- Si la app se abre más tarde, se deben generar los recurrentes pendientes
- Los movimientos generados deben marcarse como `AUTO_RECURRENT`
- Editar un movimiento generado no modifica la regla recurrente
- Borrar un movimiento generado no elimina la regla recurrente

---

## Reglas de UX

### Direccion visual
- La app debe seguir un estilo minimalista y black and white
- Priorizar blanco, negro y escala de grises
- Evitar colores decorativos innecesarios
- Si se usan iconos, deben ser simples, monocromos y minimalistas
- Evitar interfaces recargadas o con demasiados botones visibles a la vez

### Dashboard
Debe mostrar:
- saldo personal del mes
- saldo familiar del mes
- pendiente familia → personal

Debe incluir accesos rápidos:
- `+ Añadir gasto`
- `+ Gasto familiar pagado con personal`

### Historial
- Lista única
- Agrupado por día
- Orden descendente
- Filtros:
  - Todo
  - Personal
  - Familiar
  - Recurrentes
  - Pagados con personal

### Añadir gasto
- Flujo importe primero
- Fecha por defecto = hoy
- Recordar última categoría usada
- Recordar última cuenta usada
- Si se activa `pagado con cuenta personal`, la cuenta debe forzarse a `Familiar`

### Quick expense
Debe existir un flujo de gasto ultrarrápido usando valores por defecto:
- importe
- última categoría
- última cuenta
- fecha de hoy

---

## Convenciones de implementación

### Entidades recomendadas
- `UserEntity`
- `AccountEntity`
- `CategoryEntity`
- `TransactionEntity`
- `RecurrenceRuleEntity`
- `ReimbursementLedgerEntity`
- `SettlementEntity`
- `SettlementItemEntity`

### Convenciones de nombres
- Sufijo `Entity` para Room
- Sufijo `Dao` para DAO
- Sufijo `Repository` para repositorios
- Sufijo `UseCase` para casos de uso
- Sufijo `ViewModel` para viewmodels
- Pantallas Compose con sufijo `Screen`

### Base de datos
- Usa índices donde tenga sentido
- Usa claves foráneas si no complican innecesariamente la implementación
- Evita duplicados en datos seed
- El seed inicial debe ejecutarse solo una vez

### Borrado
- Prefiere borrado lógico si simplifica consistencia
- Si eliges borrado físico, asegúrate de no romper relaciones

---

## Forma de trabajar
- Trabaja solo sobre la historia o tareas activas de `docs/SPRINT.md`
- No implementes historias futuras salvo que sea necesario para compilar
- Haz cambios pequeños, claros y revisables
- Mantén siempre el proyecto compilando
- Antes de hacer cambios, explica brevemente qué archivos vas a crear o modificar
- Después de cambios grandes, resume lo implementado
- Si detectas una inconsistencia con el SDD, detente y explícalo en vez de inventar una solución
- Crea fichero en 'docs/Documentacion.md' que contenga un manual de la aplicación

---

## Prioridades
Orden típico de trabajo:
1. Entities
2. DAO
3. Database
4. Repositorios
5. Casos de uso
6. ViewModels
7. Pantallas
8. Workers
9. Tests

---

## Qué no hacer
- No añadir sincronización cloud
- No añadir login real
- No añadir multiusuario real todavía
- No añadir exportaciones
- No añadir estadísticas avanzadas
- No rehacer arquitectura sin necesidad
- No mover archivos masivamente sin explicarlo
- No meter lógica de negocio compleja dentro de Compose
- No cambiar reglas contables del producto

---

## Criterio de calidad
Antes de dar una tarea por terminada, verifica:
- compila
- sigue el SDD
- no rompe historias previas
- nombres consistentes
- sin lógica duplicada obvia
- sin dependencias innecesarias

---

## Modo de respuesta esperado
Cuando trabajes:
1. indica qué vas a tocar
2. implementa solo el alcance pedido
3. menciona riesgos o dudas si las hay
4. deja el proyecto compilando
