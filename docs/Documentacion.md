# Manual — Family Expenses V6

**Este documento describe el comportamiento implementado en Family Expenses V6.** Para conocer el funcionamiento del MVP previo consulte [archive/MVP_LEGACY.md](archive/MVP_LEGACY.md). Fuente de verdad: [V6_SPEC.md](V6_SPEC.md).

## Inicio
Al abrir verás las dos cuentas: **Familia** en negro y **Personal** en gris claro. Cada una muestra dinero real disponible, no gasto mensual imputado. Pulsando una cuenta abres su detalle. Hay botones grandes **Nuevo gasto** y **Nuevo ingreso**. Debajo, **Adelantos** indica quién debe a quién o «Sin adelantos pendientes». Siempre visible. Menú inferior Inicio / Movimientos / Análisis. Ajustes arriba.

## Registrar un gasto
1. Pulsa Nuevo gasto, introduce el importe con teclado numérico (coma, C y retroceso).
2. Elige entre las siete categorías favoritas o toca «Más» (permite buscar y crear categorías); se recuerda la última usada.
3. Revisa dos campos independientes: **Pagado desde** (dinero que sale) y **Gasto de** (a quién se imputa); se recuerda la última pareja.
4. Si son distintas, aparecerá aviso de que se crea adelanto automático. No implica dividir el gasto.
5. Por defecto fecha de hoy; si hace falta abre «Añadir detalles» para descripción o nota.
6. Guarda en la misma pantalla; se ofrece Deshacer al volver a Inicio. Con importe 0 no puedes guardar.

## Registrar un ingreso
Selecciona importe, categoría de ingresos (lista ordenada manualmente) y cuenta de destino. Las categorías de ingreso no usan favoritas y recuerdan sus últimas elecciones por separado. Guarda sin crear deuda.

## Ejemplo económico
Si **Personal paga 80 €** de una compra de Familia, los 80 € salen de Personal, Familia conserva su dinero hasta una devolución, el gasto se registra a Familia para análisis y Familia debe 80 € a Personal. Si Familia devuelve 30 €, se transfieren realmente 30 € a Personal y la deuda queda 50 €, sin ingreso ni gasto adicional.

## Consultar movimientos
Listado por día de filas compactas de dos líneas, sin resumen mensual. Filtra por cuenta responsable del gasto (no por la pagadora), busca descripción/categoría y escoge mes, 12 meses, año natural o intervalo personalizado. Pulsa un movimiento para editarlo o eliminarlo con recálculo automático. Las devoluciones y transferencias muestran dirección y se listan sin contabilizarse como gastos/ingresos.

## Análisis
Alterna **Categorías** (anillo monocromático y ranking) y **Evolución** (barras de una única serie). Elige Gastos o Ingresos, cuenta y periodo (dejando periodos futuros vacíos). Pulsar una categoría o una barra abre Movimientos filtrado; al volver se recuperan tus filtros y la vista de análisis.

## Detalle de cuenta, transferencias y adelantos
En cada cuenta puedes Transferir, abrir Movimientos o Adelantos, además de ajustar saldo con motivo o configurar saldo inicial, sin lista de recientes. Para transferir eliges obligatoriamente **Transferencia normal** (solo mueve dinero) o **Devolución de adelanto** (mueve dinero y reduce deuda). La devolución propone dirección e importe pendiente pero permite devolver menos. Adelantos muestra el saldo neto en ambas direcciones y un único historial cronológico con filtros Todos / Adelantos / Devoluciones y acceso directo a la devolución.

## Categorías y ajustes
Desde **Ajustes** accedes a Categorías, Cuentas, Movimientos recurrentes, Datos y seguridad y Acerca de. Las categorías de gasto pueden editarse, archivarse y ordenarse manualmente, con hasta siete favoritas y sustitución guiada al intentar añadir la octava. Las de ingresos se ordenan manualmente, sin favoritas. Las archivadas siguen en histórico y análisis. Los nombres e iconos de Familia y Personal pueden personalizarse; V6 no permite añadir una tercera cuenta.

## Copias y seguridad
V6 guarda los datos localmente y funciona 100 % sin conexión. Desde **Ajustes → Datos y seguridad** puedes exportar movimientos/transferencias/ajustes a CSV validado, crear una copia portable JSON completa y restaurarla tras validación integral y confirmación previa con reemplazo atómico. **Guardar una copia fuera del móvil** es necesario para recuperarte de una pérdida del almacenamiento local. Sin nube, inicio de sesión, sincronización ni PIN propio en V6.

## Recurrencias
Las reglas recurrentes mensuales siguen activas bajo **Ajustes → Movimientos recurrentes** y se ejecutan en segundo plano mediante WorkManager respetando `payingAccountId` y `responsibleAccountId` independientes.

## Estado de verificación
Todos los módulos de V6 están implementados y verificados con 97 pruebas automatizadas (contables, repositorios, casos de uso, ViewModels y backup/CSV) y compilación APK (`assembleDebug`). Queda pendiente únicamente la validación visual interactiva en dispositivo/emulador real (360 y 412 dp) y la medición en uso real del objetivo UX de registro <3 segundos.
