# Configuración

Abra la configuración desde el Editor de menús usando el botón `Config`.

La interfaz de usuario de configuración tiene tres secciones:

- Generales
- Animaciones
- Diseño

## General

| Configuración | Tipo | Rango / Valores | Predeterminado |
|---|---|---|---|
| Mover mientras está abierto radial | booleano | ENCENDIDO/APAGADO | ENCENDIDO |
| Mostrar etiqueta flotante | booleano | ENCENDIDO/APAGADO | ENCENDIDO |
| Editor de comandos Líneas visibles | entero | 1-20 | 5 |

## Animaciones

| Configuración | Tipo | Rango / Valores | Predeterminado |
|---|---|---|---|
| Animaciones habilitadas | booleano | ENCENDIDO/APAGADO | ENCENDIDO |
| Abrir/cerrar animación | booleano | ENCENDIDO/APAGADO | ENCENDIDO |
| Animación al pasar el mouse | booleano | ENCENDIDO/APAGADO | ENCENDIDO |
| Estilo abierto | enumeración | `WIPE`, `FADE`, `NONE` | @@CÓDIGO3@@ |
| Dirección | enumeración | `CW`, `CCW` | @@CÓDIGO6@@ |
| Estilo de desplazamiento | enumeración | `FILL_SCALE`, `FILL_ONLY`, `SCALE_ONLY`, `NONE` | @@CÓDIGO11@@ |
| Porcentaje de crecimiento al pasar el cursor | doble | 0,0-0,5 | 0,05 |
| Duración de apertura/cierre | entero (ms) | 0-2000 | 125 |

## Diseño

| Configuración | Tipo | Rango / Valores | Predeterminado |
|---|---|---|---|
| Zona muerta | entero | 0-90 | 18 |
| Radio exterior | entero | 24-512 | 72 |
| Grosor del anillo | entero | 6-256 | 28 |
| Umbral de inicio de escala | entero | 0-128 | 8 |
| Escala por artículo | entero | 0-100 | 6 |
| Brecha de corte | int (grados) | 0-12 | 0 |
| Estilo de diseño | enumeración | `SOLID`, `SEGMENTED`, `OUTLINE`, `GLASS` | @@CÓDIGO4@@ |
| Color del anillo | ARGB int | Int con signo de 32 bits | @@CÓDIGO5@@ |
| Color de desplazamiento | ARGB int | Int con signo de 32 bits | @@CÓDIGO6@@ |
| Color del borde | ARGB int | Int con signo de 32 bits | @@CÓDIGO7@@ |
| Color del texto | ARGB int | Int con signo de 32 bits | @@CÓDIGO8@@ |

## Vista previa

`Preview` abre una pantalla de vista previa radial y realiza un bucle de animación con una pausa de 1 segundo entre bucles.

Utilice esto para ajustar la configuración visual antes de comprometerse.

## Guardar comportamiento

- Guardar escribe valores inmediatamente en archivos de configuración.
- Al guardar, la pantalla vuelve a la pantalla del editor principal.

## Archivos de configuración

- @@CÓDIGO0@@
- @@CÓDIGO1@@
- @@CÓDIGO2@@

???+ info "Análisis profundo: formato de color"
    Los colores se almacenan como entradas ARGB de 32 bits con signo.

Ejemplo:

- @@CÓDIGO0@@
    - alfa `AA`, luego rojo `RR`, verde `GG`, azul `BB`

Los números decimales negativos son normales para valores ARGB opacos.
