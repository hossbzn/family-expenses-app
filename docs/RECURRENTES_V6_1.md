# Family Expenses — Recurrentes V6.1
**Estado:** especificación funcional validada, **sin implementar**. Aprobada en conversación de diseño el 21/11/2026.
**Alcance:** evolución del módulo de ingresos/gastos recurrentes, compatible con [V6_SPEC.md](V6_SPEC.md). Las reglas económicas de V6 prevalecen. Ningún punto de este documento implica que el código actual lo cumpla.

## 1. Principios
- La regla recurrente es una *plantilla de programación*; cada ejecución programada es una entidad/ocurrencia independiente.
- Se admiten tanto **gastos como ingresos**.
- Cada regla elige **Automático** o **Pendiente de confirmar**.
- La app es offline-first; no depende de conexión ni notificaciones para el registro. Si no puede ejecutarse en la fecha prevista, reconcilia al reabrirse/procesar trabajo local.
- Una ocurrencia se identifica de forma estable y no puede producir dos movimientos económicos. Registrar, confirmar, omitir y reintentar debe ser idempotente.
- Ningún pendiente altera caja, deudas ni análisis antes de confirmarse.

## 2. Configuración de reglas
Campos:
- Concepto/nota, tipo gasto/ingreso, importe previsto positivo en céntimos, categoría válida.
- Para gasto: **Pagado desde** (cuenta de caja) y **Gasto de** (cuenta responsable), independientes. Para ingreso: cuenta destinataria.
- Modo de ejecución: Automático | Pendiente de confirmar.
- Fecha de inicio obligatoria, fecha de fin opcional (inclusive); validar fin >= inicio.
- Frecuencia:
  - **Semanal:** día de la semana.
  - **Mensual:** día 1–31.
  - **Cada varios meses:** intervalo **2, 3 o 6** meses y día 1–31; usar fecha de inicio como ancla del ciclo.
  - **Anual:** día y mes, estableciendo ancla de año.
- Si el día 29/30/31 no existe en un mes, ejecutar el **último día real** del mes sin cambiar el día nominal configurado. Para el 29 de febrero anual en año no bisiesto, usar el último día de febrero, criterio coherente con la misma regla.
- Evitar generar fechas anteriores al inicio o posteriores al fin. Validar cambios de calendario y zona horaria sin duplicados.

## 3. Estados de regla y acciones
- **Activa:** genera ocurrencias según calendario.
- **Pausada:** no genera nuevas ocurrencias mientras esté pausada. Reactivar desde próxima fecha futura; **no recuperar periodos pausados**.
- **Archivada:** deja de aparecer entre activas y no genera nuevas ocurrencias; conserva configuración, historial y puede restaurarse. Al restaurar, **elegir nueva fecha de inicio** proponiendo por defecto la próxima fecha futura; nunca recuperar automáticamente periodos archivados.
- **Finalizada:** alcanzó fin de vigencia y deja de generar. Para reactivar, revisar nueva vigencia/fecha de inicio.
- **Editar regla:** solo modifica ejecuciones futuras *aún no generadas*. Movimientos ya registrados y pendientes ya generados mantienen su instantánea original de importe, fecha, categoría, pagadora, responsable y modo. Si cambia la frecuencia, recalcular exclusivamente próximas fechas, sin duplicar ocurrencias.
- Archivar/pausar **no elimina pendientes anteriores** ni movimientos ya registrados. Avisar de pendientes existentes al archivar.
- No eliminar permanentemente una regla desde el flujo normal: usar archivar. Conservar vínculo desde Movimientos al origen recurrente.

## 4. Ejecuciones automáticas
- Al vencer, crear el movimiento económico una única vez mediante el mismo motor de registro manual V6.
- Si la app estuvo cerrada o sin ejecución de WorkManager, recuperar fechas vencidas durante periodos **activos** al volver a procesar las reglas. No generar fechas de pausas/archivos.
- Cada ocurrencia conserva fecha económica programada y evidencia de registro. Controlar concurrencia y reintentos con unicidad y transacción.
- No prometer hora exacta de ejecución en segundo plano: Android puede aplazar trabajo; la reconciliación preserva el resultado.
- El modo Automático se recomienda para importes estables, pero se configura individualmente.

## 5. Ejecuciones pendientes de confirmar
- En la fecha prevista crear una ocurrencia PENDIENTE con una **instantánea de los datos de la regla**.
- Mostrar por defecto la **fecha programada** al revisar, aunque se confirme días después. Permitir editar fecha e importe antes del registro (y revisar categoría/cuentas si procede).
- **Confirmar**: registrar exactamente un movimiento, aplicar reglas contables V6 y vincularlo a la ocurrencia. Si falla, no marcar confirmada.
- **Posponer**: modificar la fecha de recordatorio/revisión, **no** la fecha económica programada; la programación futura de la regla no cambia.
- **Omitir**: marcar únicamente esa ocurrencia como OMITIDA, sin movimiento económico y sin alterar la regla. Conservarla en historial.
- Acumulación: guardar una ocurrencia independiente por periodo, sin consolidar de oficio importes o fechas.
- **Confirmación individual y múltiple con revisión previa** de todas las ocurrencias seleccionadas (importe y fecha por fila, total meramente informativo). Al confirmar múltiple, una sola transacción de base de datos: todas se registran o ninguna. No registrar duplicados al repetir una solicitud.
- Una regla editada no cambia pendientes ya generados. Pendientes anteriores siguen revisables incluso con regla pausada, archivada o finalizada.

## 6. Integración con el motor económico V6
- Gastos: la caja se descuenta únicamente de la **pagadora**, el análisis corresponde a la **responsable**, y si difieren se crea adelanto bidireccional vinculado al mismo gasto, sin duplicarlo.
- Ingresos: caja de la cuenta destinataria.
- Pendientes/omitidos no afectan saldos, adelantos, estadísticas ni movimientos.
- Una ejecución confirmada no se vuelve a registrar por cambios posteriores en la regla.
- Ejemplo: gasto familiar de 80 € pagado por Personal pendiente → sin efecto; al confirmar → Personal -80 €, Familia sin movimiento de caja, Familia debe 80 € a Personal. Una devolución de 30 € opera según V6 y deja 50 € pendientes.

## 7. Pantallas y navegación
1. **Recurrentes**, diseño Material 3 monocromático:
   - Acceso a **Pendientes** (con contador) y **Mis reglas**.
   - Filtros de reglas: Activas, Pausadas, Archivadas, Finalizadas; mostrar concepto, importe estimado, frecuencia, próxima fecha y modo.
   - Crear regla con acción destacada.
2. **Crear/editar regla**: concepto, gasto/ingreso, importe, categoría, selectores de cuenta, frecuencia y día, inicio/fin, modo. Validaciones claras.
3. **Detalle de regla**: resumen, próxima ejecución, Editar, Pausar/Reactivar, Archivar/Restaurar, historial de ocurrencias.
4. **Pendientes**: listado agrupable por regla o fecha, vencidos visibles; revisar/confirmar, posponer, omitir.
5. **Revisión múltiple**: seleccionar ocurrencias, revisar cada importe/fecha y confirmar todo atómicamente.
6. **Inicio**: aviso discreto «N recurrentes pendientes» solo si hay pendientes, acceso directo a la bandeja. Sin añadir totales de gasto a Inicio.
7. **Ajustes**: interruptor para notificaciones Android de pendientes, **desactivables**. Aviso dentro de Inicio siempre activo. Respetar permiso Android, agrupar notificaciones y evitar repetición molesta.
- Las cifras/casos incluidos en los prototipos conversacionales eran ejemplos ficticios.

## 8. Datos, compatibilidad y migración
- Antes de implementación: auditar entidades/DAOs/Worker existentes, reglas mensuales del día 1, ejecución AUTO_RECURRENT y esquema Room.
- Preservar **datos reales del dispositivo**: movimientos, reglas actuales, categorías, saldos y referencias. Sin destructive migration, reseed ni borrados.
- Decidir formalmente cómo migrar reglas históricas mensuales al nuevo modelo y cómo versionar ocurrencias sin inventar movimientos antiguos; registrar ambigüedades, no deducir automáticamente cobros omitidos históricos si la evidencia es insuficiente.
- Separar regla, ocurrencia (programada/pendiente/confirmada/omitida) y transacción económica; historiales y vínculos estables.
- Notificaciones Android son auxiliares: WorkManager y reconciliación idempotente deben garantizar persistencia en límites reales de Android.
- Backup/restauración V6 deberá incluir reglas, estados, ocurrencias, vínculos, preferencias y notificaciones cuando aplique.

## 9. Criterios de aceptación mínimos
1. Automático vencido se registra exactamente una vez incluso con reintentos, dos workers o aperturas repetidas.
2. Pendiente creado no modifica caja, análisis ni deuda antes de confirmar.
3. Al confirmar tarde se propone la fecha programada, editable; importe también editable.
4. Confirmación múltiple: revisión previa, sin duplicados, transacción todo-o-nada.
5. Omitir una ocurrencia no afecta el mes siguiente ni crea movimiento.
6. Posponer no cambia fecha económica ni regla.
7. Editar importe, categoría, cuentas o frecuencia no altera registrados ni pendientes previos.
8. Pausar/archivar no recupera periodos suspendidos; activos no procesados se reconcilian al reabrir.
9. Reactivar archivada permite fecha inicial nueva con futura propuesta, sin generar retroactivos silenciosos.
10. Día 31 mensual resuelve febrero y vuelve al 31 en marzo; 29 feb anual definido arriba.
11. Gastos pagados por otra cuenta crean adelanto según V6; pendientes no lo crean.
12. Aviso Inicio siempre y notificación Android solo si opción/permiso permiten.
13. Actualización desde instalación antigua conserva íntegros datos y recurrentes; backup/restauración cubre nuevos registros.
14. Sin red, cambio de fecha y reinicio, ni se pierden ni se duplican ocurrencias.

## 10. No incluido en V6.1
- Sincronización entre móviles, nube, login, OCR/lectura bancaria, importes variables automáticos inferidos, conciliación bancaria o notificaciones insistentes.
- No cambiar la UI ni la contabilidad en esta fase de documentación.

## 11. Dependencias y secuencia recomendada
- Primero **V6-12 (auditoría/migración)** y **V6-01/V6-02 (motor contable)**, porque la generación automática necesita transacciones económicas correctas.
- Después modelo de regla y estados; calendario y reconciliación; ocurrencias pendientes; revisión individual/múltiple; avisos/notificaciones; pruebas end-to-end.
- No modificar código sin autorización explícita. Cambios documentales aprobados no significan autorización de programación.
