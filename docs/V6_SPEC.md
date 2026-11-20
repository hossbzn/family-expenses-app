# Family Expenses — Especificación V6.0
Fecha: 2026-11-20
Estado: diseño funcional y visual aprobado provisionalmente; pendiente validación en Android.
Alcance de este documento: requisitos para la próxima implementación, NO funcionalidades ya entregadas. No modificar código durante esta fase.

## 1. Producto y principios
App Android offline-first de gastos familiares, inicialmente de un solo dispositivo. Dos cuentas activas (Familia y Personal); modelo ampliable en el futuro. Registro rápido de gastos, contabilidad de caja real, gastos atribuidos a cuenta responsable y adelantos bidireccionales. Moneda EUR e importes en céntimos (enteros).
Diseño Material Design 3 monocromático (blanco, negro, grises), controles accesibles y navegación inferior Inicio / Movimientos / Análisis. Ajustes desde Inicio. Sin PIN adicional.

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
- Tarjeta Familia negra y Personal gris claro; ambas muestran saldos de caja reales individualmente, sin saldo global.
- Botones grandes «Nuevo gasto» (principal negro) y «Nuevo ingreso» (secundario).
- Adelantos siempre visibles: «Familia te debe X», «Personal debe a Familia X» o «Sin adelantos pendientes», clic para detalle.
- Sin listado de recientes, gráficos ni totales del mes.
- Ajustes en cabecera; barra inferior con solo Inicio, Movimientos, Análisis.

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
- Filtros compartidos de cuenta, periodo y Gastos/Ingresos (se muestran por separado).
- Categorías: anillo monocromático y ranking con importes/porcentajes.
- Evolución: una serie de barras (gastos O ingresos); mensual para años/12 meses, diaria para mes; granularidad adecuada para rango personalizado. Meses futuros vacíos, no ceros ficticios.
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

## 10. Accesibilidad y validación
- Priorizar 360 y 412 dp de ancho (Android), objetivos táctiles aproximados >=48 dp, contraste suficiente, texto ampliado, áreas seguras, botones Guardar accesibles, sin scroll horizontal.
- Verificar teclado, barras de navegación, pantallas compactas, navegación hacia atrás, persistencia offline.
- Tests de caja vs responsabilidad, adelantos bidireccionales, devoluciones parciales, modificaciones/borrados, respaldos y restauración.
- La métrica aspiracional «registro de gasto <3 segundos» es objetivo UX, a medir en prueba real, no una garantía.

## 11. Alcance existente y migración
Código existente: Kotlin, Jetpack Compose, Room, Hilt, ViewModel/StateFlow, Navigation Compose y WorkManager. Funciones previas marcadas DONE en backlog son estado histórico de implementación, NO evidencia de conformidad V6.
La versión previa calcula «saldos mensuales» por responsabilidad, asume adelantos solo Personal→Familia y liquidación total sin movimientos de caja. Esto contradice V6. Antes de implementar: diseñar migración de entidades/ledger, reglas de saldo y pruebas de regresión. Preservar datos existentes.
Movimientos recurrentes son funcionalidad heredada: conservarla, pero su ubicación visual y adaptación a los nuevos selectores quedan pendientes; no borrarla silenciosamente.
Los documentos operativos docs/sdd.md y AGENTS.md están alineados con V6; las reglas incompatibles del MVP anterior se conservan únicamente como referencia en docs/archive/MVP_LEGACY.md. No declarar V6 implementada hasta completar migración, desarrollo y pruebas.
