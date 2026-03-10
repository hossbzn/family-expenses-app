# Manual de la aplicacion

## Objetivo
La aplicacion permite registrar ingresos y gastos personales y familiares en local, con un enfoque rapido para el registro diario.

## Estado actual del MVP
- Base Android con Kotlin, Compose, Room, Hilt, Navigation y WorkManager.
- Seed inicial con cuentas `Personal` y `Familiar`.
- Categorias iniciales de ejemplo.
- Dashboard mensual conectado a Room con saldo personal, saldo familiar y pendiente familia a personal.
- Alta manual de gasto con flujo basico para gasto personal y gasto familiar pagado con personal.

## Reglas contables clave
- Todo movimiento pertenece a una cuenta.
- Los importes se guardan en minor units usando `Long`.
- Un gasto familiar pagado con cuenta personal afecta al saldo familiar y al pendiente familia a personal.
- La liquidacion del pendiente sera total en el MVP.

## Uso actual
1. Abrir la app.
2. Esperar a que se ejecute el seed inicial en el primer arranque.
3. Revisar el dashboard mensual.
4. Usar `+ Anadir gasto` para registrar un gasto personal.
5. Usar `+ Gasto familiar pagado con personal` para registrar el caso especial que crea pendiente.

## Pantalla principal
- `Saldo personal`: ingresos menos gastos personales del mes. Si un gasto se marco como pagado con personal pero pertenece a familia, no descuenta aqui.
- `Saldo familiar`: ingresos menos gastos familiares del mes.
- `Pendiente familia a personal`: suma de entradas abiertas en `reimbursement_ledger`.

## Limitaciones actuales
- Los accesos rapidos del dashboard aun no navegan a formularios.
- El alta de movimientos y la liquidacion completa siguen en desarrollo segun `docs/SPRINT.md`.
