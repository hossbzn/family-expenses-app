# Bugs y QA de Batman

Registro de hallazgos reproducibles y comprobaciones pendientes. Para cerrar un hallazgo, anotar el commit de la corrección y el resultado de su prueba.

## Revisión del 24 de noviembre de 2026

- **Rama:** `fix/estabilizacion-senior-v6`.
- **Commit revisado:** `d95255e` (resumen mensual en Inicio y serie «Saldo» en Análisis).
- **Comprobación posterior:** los archivos de los tres hallazgos siguen iguales en `719738d`. Ese commit añade la apertura de movimientos recientes desde Inicio (V6-27) y queda fuera de esta validación.

| ID | Prioridad | Estado | Hallazgo |
|---|---|---|---|
| BATMAN-001 | Alta | Abierto | El script de Gradle falla en Linux y bloquea la CI. |
| BATMAN-002 | Alta | Abierto | El detalle de la gráfica «Saldo» puede mostrar operaciones ajenas al cálculo. |
| BATMAN-003 | Media | Abierto | Inicio puede quedarse en el mes anterior mientras está abierto. |

### BATMAN-001 · Recuperar la CI de Android

El script falla en la línea 82 por un `fi` inesperado. El flujo de GitHub Actions en Ubuntu ejecuta ese script para lanzar las pruebas, lint y la compilación. Referencias en [gradlew](../gradlew) y [ci.yml](../.github/workflows/ci.yml).

- **Reproducción:** ejecutar `bash -n gradlew` desde la raíz del repositorio.
- **Criterio de cierre:** el script pasa la comprobación de sintaxis y las tareas `testDebugUnitTest`, `lintDebug` y `assembleDebug` terminan en GitHub Actions para el commit corregido.

### BATMAN-002 · Hacer que el detalle de «Saldo» cuadre con su gráfica

Análisis calcula «Saldo» como ingresos menos gastos. Al tocar una barra, el detalle se abre con el filtro «Todos», que permite mostrar transferencias, devoluciones y ajustes. Inicio incluye el efecto de las transferencias normales en su cifra mensual, según la especificación. Las dos métricas necesitan etiquetas claras. Referencias en [AnalysisViewModel.kt](../app/src/main/java/com/familyexpenses/app/feature/analysis/AnalysisViewModel.kt) y [V6_SPEC.md](V6_SPEC.md).

- **Reproducción:** registrar un ingreso, un gasto, una transferencia normal y un ajuste en el mismo periodo. Abrir **Análisis → Evolución → Saldo**, pulsar una barra y comparar sus movimientos con el importe mostrado.
- **Criterio de cierre:** el detalle conserva la cuenta y las fechas de la barra y muestra solo los ingresos y gastos que explican su importe. Añadir una prueba de regresión para el filtro y verificar que las etiquetas de Inicio y Análisis identifican la métrica que muestran.

### BATMAN-003 · Actualizar Inicio al cambiar el mes

El mes se calcula al recibir datos de Room. Si la app permanece en Inicio al pasar del último día del mes al primero y los datos no cambian, la tarjeta puede conservar la etiqueta y el importe anteriores. Referencia en [DashboardRepository.kt](../app/src/main/java/com/familyexpenses/app/data/repository/DashboardRepository.kt).

- **Reproducción:** con Inicio visible y datos del mes anterior, simular el cambio de mes sin registrar movimientos.
- **Criterio de cierre:** la etiqueta y la cifra se actualizan juntas para Familia y Personal. Cubrir con un reloj controlable el paso de mes mientras la pantalla está abierta y la vuelta desde segundo plano.

### Verificación y QA visual

- **En `d95255e`:** ejecutando Gradle 9.9.1 directamente, pasaron 127 pruebas unitarias, lint terminó con 0 errores y 4 advertencias y se generó el APK de depuración.
- **Pendiente en Galaxy S25:** comprobar Ingreso y Gasto con tamaño de fuente estándar y ampliado. Verificar que los textos caben, que las acciones fijas se alcanzan cómodamente y que se puede llegar a los tres últimos movimientos con la barra inferior visible.
