# Mejoras potenciales

## Propósito

Este documento recoge observaciones surgidas del uso real de Family Expenses antes de convertirlas en historias comprometidas del backlog.

- Una entrada en este fichero representa una idea pendiente de valorar, no una funcionalidad aprobada.
- Las prioridades son provisionales hasta revisar conjuntamente el impacto, el alcance y las pruebas necesarias.
- Cuando una mejora sea aprobada, se trasladará a `BACKLOG.md` y se dejará aquí la referencia correspondiente.
- Las decisiones técnicas y los hallazgos de revisión continúan registrándose en `AI_REVIEW_LOG.md`.

## Estados

- `IDEA`: observación registrada, todavía sin analizar.
- `EVALUAR`: necesita concretar comportamiento, diseño o impacto técnico.
- `APROBADA`: decisión funcional tomada; preparada para trasladarse al backlog.
- `DESCARTADA`: se decide no implementar, indicando el motivo.
- `MOVIDA_A_BACKLOG`: convertida en una historia formal del backlog.

## Resumen

| ID | Área | Mejora potencial | Prioridad provisional | Estado |
|---|---|---|---|---|
| UX-001 | Accesibilidad visual | Aumentar y unificar el tamaño de la tipografía | Alta | IDEA |
| UX-002 | Sistema visual | Dar más presencia visual a ingresos y gastos | Media | IDEA |
| UX-003 | Acceso rápido | Añadir un widget o acceso de gasto rápido | Media | IDEA |
| UX-004 | Recurrentes | Permitir crear recurrencias desde Nuevo gasto y Nuevo ingreso | Alta | IDEA |
| UX-005 | Recurrentes | Separar la lista de reglas del formulario de creación y edición | Alta | IDEA |
| UX-006 | Recurrentes | Reducir el desplazamiento vertical y simplificar la navegación | Alta | IDEA |

## Observaciones registradas

### UX-001 — Aumentar y unificar el tamaño de la tipografía

- **Fecha:** 2026-11-23
- **Observación:** El texto, especialmente el de los botones, se percibe pequeño frente a otras aplicaciones utilizadas en el mismo dispositivo.
- **Posible dirección:** Definir una escala tipográfica común y aumentar el texto de acciones principales y secundarias sin perder adaptabilidad en pantallas compactas.
- **Validación futura:** Comprobar todas las pantallas en un Galaxy S25 o anchos de 360–412 dp, incluyendo escalas de fuente del sistema del 100 %, 115 % y 130 %.

### UX-002 — Dar más presencia visual a ingresos y gastos

- **Fecha:** 2026-11-23
- **Observación:** Los colores semánticos actuales resultan poco llamativos y cuesta distinguir visualmente ingresos y gastos con rapidez.
- **Posible dirección:** Revisar verde, rojo, iconos y signos `+`/`−`, manteniendo contraste accesible y sin depender únicamente del color.
- **Decisión pendiente:** Elegir tonos definitivos después de probarlos en el dispositivo real y en las pantallas principales.

### UX-003 — Widget o acceso de gasto rápido

- **Fecha:** 2026-11-23
- **Observación:** Registrar un gasto frecuente debería requerir menos pasos y no obligar a navegar desde la pantalla de Inicio.
- **Posible dirección:** Widget compacto o acceso directo que abra una interfaz mínima para introducir importe, categoría y cuentas recordadas.
- **Cautela:** Evitar registrar importes con un único toque sin confirmación, para reducir altas accidentales.

### UX-004 — Crear recurrencias desde Nuevo gasto y Nuevo ingreso

- **Fecha:** 2026-11-23
- **Observación:** Recurrentes está demasiado escondido dentro de Ajustes y no aparece en el momento natural de crear un movimiento.
- **Posible dirección:** Mantener el acceso de gestión general y añadir una acción como «Hacer recurrente» en Nuevo gasto y Nuevo ingreso, reutilizando los datos ya introducidos.
- **Decisión pendiente:** Determinar si la acción será un interruptor, un botón secundario o un paso posterior opcional.

### UX-005 — Separar la lista del formulario de recurrentes

- **Fecha:** 2026-11-23
- **Observación:** Al dar de alta una recurrencia, el formulario aparece arriba y la lista permanece debajo dentro de la misma pantalla, lo que dificulta entender la jerarquía.
- **Posible dirección:** Una pantalla para listar y gestionar reglas, y otra pantalla o diálogo dedicado para crear y editar una recurrencia.
- **Objetivo:** Que al terminar o cancelar la edición se vuelva a la lista en la posición y pestaña anteriores.

### UX-006 — Reducir el desplazamiento vertical en Recurrentes

- **Fecha:** 2026-11-23
- **Observación:** La pantalla actual es demasiado larga y exige mucho desplazamiento vertical.
- **Posible dirección:** Agrupar opciones, mostrar detalles avanzados solo cuando sean necesarios y separar responsabilidades entre lista, formulario, detalle y pendientes.
- **Validación futura:** Probar creación, edición y revisión de pendientes en un dispositivo compacto sin controles ocultos ni desplazamientos innecesarios.

## Plantilla para nuevas observaciones

```markdown
### UX-XXX — Título breve

- **Fecha:** AAAA-MM-DD
- **Área:**
- **Contexto:** Qué se estaba intentando hacer y desde qué pantalla.
- **Observación:** Qué resultó confuso, lento, pequeño o incómodo.
- **Comportamiento deseado:** Cómo debería sentirse o funcionar.
- **Frecuencia:** Ocasional / frecuente / siempre.
- **Estado:** IDEA
```

## Próxima revisión

Revisar conjuntamente las observaciones después de varios días de uso real, agrupar las relacionadas, decidir prioridades y trasladar únicamente las mejoras aprobadas al backlog formal.
