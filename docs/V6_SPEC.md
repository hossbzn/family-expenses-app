# Family Expenses — Especificación V6.0
Fecha: 2026-11-20
Estado: diseño funcional y visual aprobado provisionalmente; pendiente validación en Android.
Alcance de este documento: requisitos para la próxima implementación, NO funcionalidades ya entregadas. No modificar código durante esta fase.

## 1. Producto y principios
App Android offline-first de gastos familiares, inicialmente de un solo dispositivo. Dos cuentas activas (Familia y Personal); modelo ampliable en el futuro. Registro rápido de gastos, contabilidad de caja real, gastos atribuidos a cuenta responsable y adelantos bidireccionales. Moneda EUR e importes en céntimos (enteros).

### Sistema visual global
- **Fondo y superficies:** Fondo neutro blanco roto (`#F8F9FA`) y superficies en blanco puro (`#FFFFFF`).
- **Color principal:** Azul petróleo (`#1A3644`) para botones principales, indicadores de pestaña, selecciones y elementos destacados.
- **Colores semánticos:** Verde apagado (`#2E7D32`) para ingresos/positivos y rojo apagado (`#C62828`) para gastos/negativos, utilizados únicamente cuando aportan significado. No depender exclusivamente del color para transmitir información (combinar con signos `+` / `−` e iconos).
- **Paridad de cuentas:** Familia y Personal se representan con la misma jerarquía y peso visual.
- **Diseño e iconografía:** Iconografía y tipografía MD3 consistente, cuadrícula base de 8 dp, objetivos táctiles mínimos de 48 dp $\times$ 48 dp en todos los componentes interactivos.
- **Adaptabilidad:** Diseño compacto optimizado para Galaxy S25 base (360 dp - 412 dp de ancho), permitiendo scroll vertical antes que reducir excesivamente el tamaño de fuente. Compatible con tamaño de fuente aumentado y contraste accesible.
- **Estados explícitos:** Definición de estados vacío, cargando, error, deshabilitado y confirmación en todos los componentes.
- Navegación inferior: Inicio / Movimientos / Análisis. Ajustes y Modo de Privacidad en la cabecera de Inicio. Sin PIN adicional.

## 2. Modelo económico — INVARIANTES
- Cada gasto tiene *cuenta responsable* y *cuenta pagadora* por separado. El gasto se contabiliza UNA vez para análisis en la cuenta responsable; el dinero sale ÚNICAMENTE de la pagadora.
- Ingresos incrementan el saldo de caja de la cuenta destinataria.
- Saldo de caja = saldo inicial + ingresos de caja - gastos pagados + transferencias entrantes - salientes + ajustes de saldo. Nunca deducir saldos reales de la cuenta responsable ni compensarlos implícitamente con deudas.
- Si pagadora != responsable, nace adelanto de pagadora a responsable por el importe. Es bidireccional (Familia ↔ Personal), no se reparte al 50 %.
- Deuda neta: adelantos Personal→Familia menos adelantos Familia→Personal menos devoluciones Familia→Personal más devoluciones Personal→Familia. Signo positivo: Familia debe a Personal; negativo: Personal debe a Familia; cero: sin deudas.
- Transferencia normal: movimientos de caja iguales y opuestos, SIN cambiar deuda, ingresos ni gastos estadísticos.
- Devolución de adelanto: transferencia real de caja entre cuentas, además reduce deuda; puede ser parcial o completa, hasta el pendiente neto y en la dirección correcta. Nunca crea nuevo ingreso ni gasto.
- Ajuste de saldo: operación auditable con motivo, fuera de ingresos/gastos ordinarios.
- Edición/eliminación debe recalcular saldos y deuda, evitando historiales inconsistentes y doble cómputo.
Ejemplo: Personal paga 80 € de Familia: saldo Personal -80 €, saldo Familia sin cambios, gasto Familia +80 € para análisis, Familia debe 80 €. Si Familia devuelve 30 €, caja Familia -30 €, caja Personal +30 €, deuda pendiente 50 €.
No interpretar transferencias como devolución automáticamente.

## 3. Inicio
- Cabecera con saludo/subtítulo ("Buenos días / Resumen familiar"), botón de ojo para el Modo de Privacidad y botón de Ajustes (⚙️).
- Tarjetas de Familia y Personal con el mismo peso y jerarquía visual: muestran únicamente el **saldo del mes en curso** (`ingresos − gastos` imputados a cuenta responsable + transferencias normales entrantes − transferencias normales salientes del mes; excluyendo devoluciones de adelanto, ajustes de saldo y meses anteriores), sin saldo agregado global. El saldo disponible real acumulado se consulta al pulsar cada tarjeta (Detalle de cuenta); en Análisis las transferencias normales siguen excluidas.
- Botones grandes de acciones rápidas fijados en la parte inferior sobre la barra de navegación (para uso cómodo con una sola mano): **«Ingreso»** a la izquierda con icono `+` protagonista, y **«Gasto»** a la derecha con icono `−` protagonista (Gasto colocado a la derecha por ser la acción más frecuente).
- Adelantos siempre visibles: «Familia te debe X», «Personal debe a Familia X» o «Sin adelantos pendientes», clic para detalle.
- Sección «Últimos movimientos»:
  - Muestra exactamente los **tres últimos movimientos** registrados en orden descendente por fecha (`occurredAt`) y orden de creación (`createdAt`).
  - Cada fila muestra concepto/nota, fecha (ej. "Hoy", "Ayer", "20 nov"), cuenta (ej. "Familia", "Personal") e importe formatado.
  - Incluye gastos, ingresos, transferencias, devoluciones, ajustes de saldo y ejecuciones de recurrentes.
  - Botón «Ver todos» a la derecha del título que navega a la pantalla de Movimientos.
  - Muestra un estado vacío claro ("No hay movimientos registrados") cuando no existan operaciones.
  - Sin gráficos acumulados en Inicio.
- Diseño compacto y desplazable optimizado para un dispositivo base Galaxy S25 (360-412 dp).
- Cabecera con Ajustes y Modo de Privacidad; barra inferior con Inicio, Movimientos y Análisis.

## 4. Nuevo gasto / ingreso
- Importe destacado; teclado numérico propio con coma decimal, 2 decimales, C y retroceso; importe inicia en 0.
- Categorías gasto: 7 favoritas de orden manual + «Más», en 2×4; recordar última categoría. Editables, con iconos sin colores.
- Dos selectores SEPARADOS: «Pagado desde» y «Gasto de»; recordar última combinación. Diferencia entre cuentas genera adelanto automáticamente y aviso breve de deuda. Nunca forzar cuenta responsable.
- Fecha actual; descripción y nota opcionales bajo «Añadir detalles» plegado. Todos los campos imprescindibles, teclado y Guardar visibles sin paso intermedio; guardar deshabilitado con 0 €.
- Nuevo ingreso: cuenta destinataria y categoría de ingresos en lista manual, sin favoritas; recordar últimas elecciones.
- Guardar vuelve a Inicio con opción de deshacer; salir con cambios sin guardar requiere confirmación.

## 5. Movimientos
- Lista compacta de DOS líneas, por día y descendente, sin resumen mensual.
- Filtro principal Todas/Familia/Personal: para gastos usa cuenta RESPONSABLE; para ingresos la destinataria; transferencias/devoluciones visibles cuando participa la cuenta.
- Buscar, filtrar categoría/tipo/periodo; tipos gastos, ingresos, adelantos (como atributo del gasto, NO gasto duplicado), transferencias y devoluciones.
- Periodos: mes a mes, últimos 12 meses móviles, años naturales, personalizado.
- Pulsar fila abre detalle para editar/eliminar; conservar estado de filtros al volver. Mostrar origen/destino en transferencias y adelantos.

## 6. Análisis
- Dos pestañas Categorías y Evolución; inicial Categorías.
- Filtros compartidos de cuenta, periodo y serie (**Saldo**, **Ingresos** o **Gastos**).
- Categorías: anillo monocromático y ranking con importes/porcentajes.
- Evolución: una serie de barras (**Saldo**, **Ingresos** o **Gastos**); mensual para años/12 meses, diaria para mes; granularidad adecuada para rango personalizado. Meses futuros vacíos, no ceros ficticios.
- Al pulsar categoría o barra abrir Movimientos con filtros aplicados; al volver recuperar íntegro el estado de Análisis.
- Atribuir gastos a responsable y excluir transferencias, devoluciones y ajustes de ingresos/gastos.

## 7. Cuenta y transferencias
- Solo Familia y Personal configurables (nombre/icono). No añadir cuentas desde V6, pero diseñar modelo extensible.
- Detalle: saldo disponible, «Transferir», «Movimientos», acceso a adelantos. SIN últimos movimientos.
- «Transferir» obliga a escoger explícitamente «Transferencia normal» o «Devolución de adelanto».
- Devolución precarga dirección e importe neto pendiente, importe editable hasta el máximo; actualiza ambas cajas y deuda.
- Saldo inicial configurable; correcciones por ajustes auditables, no edición directa de saldo.
- No borrar cuenta con historial.

## 8. Adelantos
- Saldo de deuda NETO en ambas direcciones, estado cero visible.
- Historial cronológico ÚNICO con filtros Todos / Adelantos / Devoluciones.
- Mostrar trazabilidad del gasto original y transferencias de devolución. Devoluciones parciales o completas.

## 9. Categorías y ajustes
- Gastos: exactamente hasta siete favoritas ordenadas manualmente, estrella y arrastrar. Intentar octava pide sustitución; «Más» muestra resto y permite buscar/crear.
- Ingresos: lista ordenada manualmente, sin favoritas.
- Nombre, icono, tipo, orden, archivar; archivos preservan historial y análisis; no se ofrecen en nuevas operaciones.
- Ajustes: Categorías, Cuentas, Datos y seguridad, Acerca de.
- Datos: exportar CSV; backup portable completo con cuentas, categorías, operaciones, preferencias, adelantos y devoluciones; validar antes de restaurar, pedir confirmación y reemplazar de forma completa.
- Datos locales, offline, sin login, sin nube, sin sincronización ni PIN en V6. Advertir de riesgo al perder almacenamiento local, recomendar copia externa. Identificadores estables y diseño preparado para sincronización futura.

## 10. Modo de privacidad global (Ocultar importes)
- **Acceso:** Botón de ojo (`Visibility` / `VisibilityOff`) en la cabecera de Inicio junto al icono de Ajustes (⚙️) que controla una preferencia global persistente.
- **Alcance global:** Permite mostrar u ocultar de forma global todos los importes numéricos visibles en todas las pantallas de la aplicación: Inicio, Movimientos, detalles, cuentas, Adelantos, Análisis, Recurrentes y cualquier diálogo con cantidades.
- **Máscara uniforme:** Al activar la privacidad, aplica una máscara uniforme de longitud fija como `"•••• €"`.
- **En Análisis y Gráficos:** Los gráficos de evolución y anillos de categorías deben ocultar valores, escalas numéricas y etiquetas monetarias en ejes cuando el modo esté activo, evitando que permitan deducir cantidades.
- **Exclusiones de la máscara:** NO oculta conceptos, notas, fechas, nombres de cuenta, categorías ni frecuencias de ejecución.
- **Persistencia local:** La preferencia se guarda localmente en `UserPreferencesRepository` (SharedPreferences) y se mantiene tras cerrar y reabrir la app.
- **Transparencia y límites:** NO modifica los datos contables reales, el formato de exportación CSV ni los backups JSON. Tampoco se promociona ni presenta como una función de seguridad, cifrado o protección con PIN.

## 11. Accesibilidad y validación
- Priorizar 360 y 412 dp de ancho (Android, optimizado para Galaxy S25 base), objetivos táctiles mínimos de 48 dp $\times$ 48 dp, contraste suficiente en azul petróleo y fondo blanco roto, texto ampliado, áreas seguras y sin scroll horizontal.
- Accesibilidad en Modo de Privacidad: `contentDescription` dinámico ("Ocultar importes" cuando los valores están visibles; "Mostrar importes" cuando la máscara está activa). No depender exclusivamente del color para transmitir información semántica (combinar color con signos `+` / `−` e iconos).
- Verificar teclado, barras de navegación, pantallas compactas, navegación hacia atrás y persistencia offline.
- Tests de caja vs responsabilidad, adelantos bidireccionales, devoluciones parciales, modificaciones/borrados, respaldos, modo de privacidad global y restauración.
- La métrica aspiracional «registro de gasto <3 segundos» es objetivo UX, a medir en prueba real, no una garantía.

## 11. Alcance existente y migración
Código existente: Kotlin, Jetpack Compose, Room, Hilt, ViewModel/StateFlow, Navigation Compose y WorkManager. Funciones previas marcadas DONE en backlog son estado histórico de implementación, NO evidencia de conformidad V6.
La versión previa calcula «saldos mensuales» por responsabilidad, asume adelantos solo Personal→Familia y liquidación total sin movimientos de caja. Esto contradice V6. Antes de implementar: diseñar migración de entidades/ledger, reglas de saldo y pruebas de regresión. Preservar datos existentes.
Movimientos recurrentes son funcionalidad heredada: conservarla, pero su ubicación visual y adaptación a los nuevos selectores quedan pendientes; no borrarla silenciosamente.
Los documentos operativos docs/sdd.md y AGENTS.md están alineados con V6; las reglas incompatibles del MVP anterior se conservan únicamente como referencia en docs/archive/MVP_LEGACY.md. No declarar V6 implementada hasta completar migración, desarrollo y pruebas.
