# Manual de la aplicacion

## Objetivo
La aplicacion permite registrar ingresos y gastos personales y familiares en local, con un enfoque rapido para el registro diario.

## Estado actual del MVP
- Base Android con Kotlin, Compose, Room, Hilt, Navigation y WorkManager.
- Seed inicial con cuentas `Personal` y `Familiar`.
- Categorias iniciales de ejemplo.
- Dashboard mensual conectado a Room con saldo personal, saldo familiar y pendiente familia a personal.
- Alta manual de ingresos y gastos desde un formulario simplificado con importe destacado y teclado numerico.
- Historial basico de movimientos con lista unica y orden descendente.

## Reglas contables clave
- Todo movimiento pertenece a una cuenta.
- Los importes se guardan en minor units usando `Long`.
- Un gasto familiar pagado con cuenta personal afecta al saldo familiar y al pendiente familia a personal.
- La liquidacion del pendiente sera total en el MVP.

## Uso actual
1. Abrir la app.
2. Esperar a que se ejecute el seed inicial en el primer arranque.
3. Revisar el dashboard mensual.
4. En `Saldo personal`, usar `+` para anadir un ingreso personal o `-` para anadir un gasto personal.
5. En `Saldo familiar`, usar `+` para anadir un ingreso familiar o `-` para anadir un gasto familiar.
6. En el formulario, introducir primero el importe.
7. Elegir categoria y, si aplica, activar `Pagado con personal`.
8. Anadir una nota solo si hace falta.
9. Guardar el movimiento.
10. Usar `Ver historial` para revisar los movimientos guardados.

## Pantalla principal
- `Saldo personal`: ingresos menos gastos personales del mes. Si un gasto se marco como pagado con personal pero pertenece a familia, no descuenta aqui.
- `Saldo familiar`: ingresos menos gastos familiares del mes.
- `Pendiente familia a personal`: suma de entradas abiertas en `reimbursement_ledger`.
- Cada saldo tiene acceso rapido `+ / -` para abrir el formulario ya preconfigurado.

## Alta de movimiento
- El tipo de movimiento llega preconfigurado desde el acceso rapido y no se cambia dentro del formulario.
- El importe aparece en grande al abrir la pantalla y solicita teclado numerico decimal automaticamente.
- Debajo se muestran solo los campos necesarios: categoria, `Pagado con personal`, nota y guardar.
- En gastos familiares pagados con personal, el switch genera pendiente familia a personal y fuerza la logica contable correspondiente.

## Limitaciones actuales
- El historial todavia no tiene filtros, agrupacion por dia ni edicion.
- Los movimientos recurrentes siguen pendientes segun `docs/SPRINT.md`.
