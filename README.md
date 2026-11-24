<img src="app/src/main/ic_launcher-playstore.png" alt="Icono de Family Expenses" width="80">

# Family Expenses

Family Expenses es una aplicación Android para registrar gastos e ingresos de casa, separar lo personal de lo familiar y saber cuánto queda por devolver cuando una cuenta paga por la otra. Trabaja en euros y guarda los datos en el dispositivo, con uso sin conexión.

## Cómo funciona

Cada gasto distingue **quién pagó** de **a quién corresponde el gasto**. Por ejemplo, pagas 80 € de supermercado desde Personal y asignas la compra a Familia. El saldo de Personal baja 80 €, el gasto aparece en el análisis de Familia y queda un adelanto de 80 €. Si Familia devuelve 30 €, el dinero vuelve a Personal y quedan 50 € por devolver.

## Qué puedes hacer

- **Inicio.** Consultar el saldo disponible acumulado de Familia y Personal, los adelantos pendientes y los tres últimos movimientos. El icono del ojo permite ocultar los importes en las vistas compatibles.
- **Gastos e ingresos.** Registrar movimientos, elegir categorías y configurar favoritas para los gastos.
- **Movimientos y análisis.** Buscar y filtrar el historial, editar operaciones y consultar gastos o ingresos por categoría, cuenta y periodo.
- **Transferencias y devoluciones.** Mover dinero entre las cuentas o liquidar un adelanto, con opción de devolución parcial.
- **Recurrentes.** Programar gastos o ingresos, como el alquiler o una suscripción, y elegir entre registro automático o confirmación manual.
- **Copias locales.** Exportar movimientos a CSV y crear o restaurar una copia JSON desde **Ajustes → Datos y seguridad**.

## Estado del proyecto

La rama `main` reúne las funcionalidades descritas arriba. La estabilización y las mejoras de interfaz avanzan en `fix/estabilizacion-senior-v6`. Entre las próximas tareas están el formulario compacto de gasto e ingreso con sumas y restas en el importe, el resumen mensual en Inicio y la reorganización de Recurrentes. El escaneo de tickets y las copias en Google Drive siguen en evaluación.

[Backlog de desarrollo](https://github.com/hossbzn/family-expenses-app/blob/fix/estabilizacion-senior-v6/docs/BACKLOG.md) · [Ideas y mejoras potenciales](https://github.com/hossbzn/family-expenses-app/blob/fix/estabilizacion-senior-v6/docs/MEJORAS_POTENCIALES.md)

## Probar la app desde el código

1. Clona el repositorio y abre la carpeta en Android Studio.
2. Configura el JDK de Gradle en Java 17 o superior e instala Android SDK 35 desde el SDK Manager.
3. Sincroniza Gradle. Selecciona la configuración `app` y pulsa **Run** con un emulador o un móvil con Android 8.0 (API 26) o posterior.

En Windows puedes ejecutar las pruebas unitarias y generar el APK de depuración desde la raíz del proyecto:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.

## Documentación

La app está escrita en Kotlin con Jetpack Compose y Material 3. Room guarda los datos locales, Hilt proporciona las dependencias y WorkManager ejecuta las tareas de recurrencia.

- [Manual de uso](docs/Documentacion.md)
- [Especificación funcional V6](docs/V6_SPEC.md)
- [Especificación de Recurrentes V6.1](docs/RECURRENTES_V6_1.md)
- [Diseño técnico y modelo de datos](docs/sdd.md)
