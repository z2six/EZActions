# Paquetes

Los paquetes son carpetas (categorías) dentro del árbol radial.

Úselos para agrupar acciones por contexto: combate, construcción, utilidad, juego de roles, etc.

## Campos de paquete

- **Título** (también utilizado como identificación interna)
- **Nota** (opcional)
- **Icono**
- **Ocultar del radial principal**
- **Habilitar combinación de teclas**

## Combinar combinaciones de teclas

Si `Enable keybind` está activado, EZ Actions registra una combinación de teclas dedicada para ese paquete.

!!! advertencia "Es necesario reiniciar"
    El registro de combinación de teclas del paquete se aplica en el próximo reinicio del cliente.

EZ Actions muestra un mensaje del cliente cuando es necesario reiniciar (incluidos los paquetes creados por API).

## Ocultar del radial principal

Si está habilitado:

- El paquete está oculto en la página radial raíz.
- El paquete todavía existe en el modelo de menú.
- El paquete aún se puede abrir mediante API o combinación de teclas.

Bueno para "páginas avanzadas" en las que no desea una raíz saturada.

## Paquetes anidados

Los paquetes pueden contener:

- acciones clave
- acciones de comando
- acciones de equipamiento de elementos
- más paquetes

## Mejores prácticas

- Mantenga la raíz pequeña y de alta prioridad.
- Colocar acciones de baja frecuencia en paquetes más profundos.
- Proporciona a los paquetes iconos claros y nombres cortos.

## Paquetes bloqueados

Un paquete se puede marcar `locked` (normalmente mediante API o JSON).

- Las rutas de eliminación en el juego no lo eliminarán.
- Las ediciones manuales de JSON aún pueden eliminarlo.

???+ info "Profundización: identidad y singularidad"
    El título del paquete se utiliza como ID del paquete en los flujos de trabajo del editor.

Los nombres de paquetes duplicados pueden causar ambigüedad, por lo que el editor bloquea los guardados de ID/títulos de paquetes duplicados.
