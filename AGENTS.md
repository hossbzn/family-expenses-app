# AGENTS.md — Family Expenses V6

## Misión
Evolucionar la aplicación Android Kotlin/Jetpack Compose hacia **Family Expenses V6**. En la fase presente **SOLO documentación**; no modificar código, esquemas, pruebas ni marcar historias como DONE hasta autorización explícita.

## Fuentes de verdad — rutas y precedencia
1. `docs/V6_SPEC.md`: decisiones funcionales y visuales aprobadas V6.
2. `docs/sdd.md`: especificación técnica y criterios V6 alineados con la anterior.
3. `docs/BACKLOG.md`: historias y estados V6.
4. `docs/SPRINT.md`: plan de trabajo y autorización de implementación.
5. `docs/Documentacion.md`: manual de comportamiento **objetivo**, no evidencia de implementación.
6. `docs/archive/MVP_LEGACY.md`: referencia histórica únicamente. **No aplicar reglas heredadas incompatibles**.

Si documentos V6 divergen, detenerse y proponer corrección antes de cambiar código. No inferir que un DONE del MVP significa DONE en V6.

## Reglas contables INNEGOCIABLES
- Dos cuentas visibles: Familia y Personal; modelo ampliable.
- Gasto: `payingAccountId` (dinero que sale) y `responsibleAccountId` (imputación analítica), **ambos independientes**.
- El gasto descuenta únicamente el saldo real de la pagadora. El gasto cuenta analíticamente para responsable una sola vez.
- Diferentes cuentas generan adelanto de pagadora a responsable, también Familia→Personal.
- Deuda neta firmada y trazable; devoluciones **parciales o completas** mueven dinero de caja de cuenta deudora a acreedora, reducen deuda y NO crean ingreso/gasto estadístico.
- Transferencia normal y devolución son tipos seleccionados explícitamente, NUNCA compensar automáticamente.
- Ajuste de saldo registrado con motivo y fuera de estadísticas de gasto/ingreso.
- Cambiar/eliminar movimientos recalcula saldos y deuda preservando consistencia del historial.
- Importes EUR en céntimos como Long; no usar Float/Double para contabilidad.
- No usar el antiguo booleano `paidFromPersonal` como modelo objetivo; planificar migración.

## UX V6
- Minimalista Material Design 3 blanco/negro/grises. Inicio: Familia negra, Personal gris claro, saldos reales, botones Gasto/Ingreso, adelantos visibles a 0, sin resumen mensual ni recientes.
- Navegación inferior **Inicio / Movimientos / Análisis**; Ajustes en cabecera.
- Nuevo gasto: teclado propio con coma/C/retroceso; 7 favoritas manuales + Más; **dos selectores** pagadora/responsable; última categoría y combinación, fecha hoy, detalles plegados, Guardar sin paso adicional.
- Nuevo ingreso: categorías con orden manual sin favoritas.
- Movimientos: dos líneas por fila, filtro principal por cuenta responsable de gasto, periodos mes/12 meses/año/personalizado, sin resumen mensual.
- Análisis: Categorías y Evolución, una serie Gastos o Ingresos, al entrar en Movimientos conservar filtros al volver.
- Cuenta: saldo real, transferir, movimientos, adelantos; sin recientes. Adelantos: saldo neto e historial único con filtros.
- Datos: CSV, copia completa/restauración; local offline, sin PIN, nube ni login.

## Stack y arquitectura existente
Kotlin, Jetpack Compose, Room, Hilt, ViewModel + StateFlow, Navigation Compose, WorkManager. Capas data/domain/ui, repositorios, DAOs, use cases pequeños; cálculo contable en domain, UI solo presentación. Usar migraciones Room y transacciones atómicas cuando se autorice, nunca destrucción/reseed de datos existentes. Conserva recurrentes y verifica que cumplan nuevas reglas de imputación/caja.

## Proceso obligatorio
1. Antes de cualquier cambio, leer las fuentes V6 y revisar código/estado de la historia activa cuando se autorice.
2. En esta fase solo docs: no implementar funcionalidades ni alterar compilación.
3. Proponer cambios por historia, criterios de aceptación y pruebas antes de programar.
4. Marcar DONE solo al implementar, probar y verificar la historia concreta.
5. Ante incompatibilidad de datos antiguos (p. ej. liquidaciones sin movimiento de caja), documentar caso y pedir decisión; **no inventar dinero ni pagos históricos**.
6. Tras autorización, cambios pequeños y testeables, compilar, pasar tests, mantener docs y manual al día.

## No introducir en V6
Nube, sincronización, multiusuario, login, PIN, OCR bancario ni nuevas cuentas desde UI. Sin dependencias gratuitas o no justificadas, sin borrar recurrentes, sin reglas contables heredadas de saldo mensual por cuenta responsable ni liquidación solo total.
