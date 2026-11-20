# Manual objetivo — Family Expenses V6

**Este documento describe el comportamiento aprobado para V6, todavía NO implementado.** Para conocer el funcionamiento previo consulte [archive/MVP_LEGACY.md](archive/MVP_LEGACY.md). Fuente de verdad: [V6_SPEC.md](V6_SPEC.md).

## Inicio
Al abrir verás las dos cuentas: **Familia** en negro y **Personal** en gris claro. Cada una muestra dinero real disponible, no gasto mensual imputado. Pulsando una cuenta abres su detalle. Hay botones grandes **Nuevo gasto** y **Nuevo ingreso**. Debajo, **Adelantos** indica quién debe a quién o «Sin adelantos pendientes». Siempre visible. Menú inferior Inicio / Movimientos / Análisis. Ajustes arriba.

## Registrar un gasto
1. Pulsa Nuevo gasto, introduce el importe con teclado numérico (coma, C y retroceso).
2. Elige entre las siete categorías favoritas o toca «Más»; se recuerda la última usada.
3. Revisa dos campos independientes: **Pagado desde** (dinero que sale) y **Gasto de** (a quién se imputa); se recuerda la última pareja.
4. Si son distintas, aparecerá aviso de que se crea adelanto automático. No implica dividir el gasto.
5. Por defecto fecha de hoy; si hace falta abre «Añadir detalles» para descripción o nota.
6. Guarda en la misma pantalla; se ofrece Deshacer. Con importe 0 no puedes guardar.

## Registrar un ingreso
Selecciona importe, categoría de ingresos (lista ordenada manualmente) y cuenta de destino. Las categorías de ingreso no usan favoritas. Guarda sin crear deuda.

## Ejemplo económico
Si **Personal paga 80 €** de una compra de Familia, los 80 € salen de Personal, Familia conserva su dinero hasta una devolución, el gasto se registra a Familia para análisis y Familia debe 80 € a Personal. Si Familia devuelve 30 €, se transfieren realmente 30 € a Personal y la deuda queda 50 €, sin ingreso ni gasto adicional.

## Consultar movimientos
Listado por día de filas compactas de dos líneas. Filtra por cuenta responsable del gasto (no por la pagadora), busca descripción/categoría y escoge mes, 12 meses, año natural o intervalo. Pulsa un movimiento para editarlo o eliminarlo. Las devoluciones y transferencias muestran dirección y se listan sin contabilizarse como gastos/ingresos.

## Análisis
Alterna **Categorías** (anillo y ranking) y **Evolución** (barras). Elige Gastos o Ingresos, cuenta y periodo. Pulsar una categoría o una barra abre Movimientos filtrado; al volver se recuperan tus filtros y la vista de análisis.

## Detalle de cuenta, transferencias y adelantos
En cada cuenta puedes Transferir, abrir Movimientos o Adelantos, sin lista de recientes. Para transferir eliges **Normal** (solo mueve dinero) o **Devolución** (mueve dinero y reduce deuda). La devolución propone dirección e importe pendiente pero permite devolver menos. Adelantos muestra el saldo neto y un único historial cronológico con filtros.

## Categorías y ajustes
Las categorías de gasto pueden editarse, archivarse y ordenarse; hasta siete favoritas manuales en Nuevo gasto. Las de ingresos se ordenan manualmente, sin favoritas. Las archivadas siguen en histórico. Los nombres/iconos de Familia y Personal pueden cambiarse; V6 no permite añadir una tercera cuenta.

## Copias y seguridad
V6 guarda los datos localmente y funciona sin conexión. Desde Ajustes podrás exportar CSV, crear copia portable y restaurarla tras validación y confirmación. **Guardar una copia fuera del móvil** es necesario para recuperarte de una pérdida del almacenamiento local. Sin nube, inicio de sesión, sincronización ni PIN propio en V6.

## Recurrencias
Las reglas recurrentes del producto previo deben conservarse y adaptarse al modelo de caja y responsabilidad; su integración visual final se validará en desarrollo sin eliminar la funcionalidad.

## Estado y pruebas pendientes
Todo lo anterior es una especificación, no un manual de funciones ya disponibles en el código. Faltan implementación, migración segura, pruebas contables y validación visual Android 360/412 dp.
