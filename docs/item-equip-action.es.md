# Acción de equipamiento de artículo

Item Equip te permite guardar una instantánea del equipo y volver a equipar elementos coincidentes de tu inventario.

## Ranuras de destino

Puedes asignar cualquiera de estos:

- Mano principal
- De improviso
- Casco
- Peto
- Polainas
- Botas

Si un espacio se deja vacío en el editor, EZ Actions lo omite durante la ejecución.

## Cómo crear uno

1. Haga clic en `Add Item Equip`.
2. Establezca título/nota/icono.
3. Arrastre elementos desde la cuadrícula de origen a las ranuras de destino.
4. Guardar.

### La cuadrícula de origen incluye

- elemento improvisado actual
- armadura equipada
- inventario principal
- barra de acceso rápido

## Durante la ejecución

Cuando activas la acción desde radial:

1. EZ Actions verifica cada ranura de destino configurada.
2. Si el objetivo ya coincide con el elemento grabado, se omite.
3. Si no, encuentra la pila fuente que mejor coincida.
4. Intercambia elementos en la ranura de destino.

Procesa ranura por ranura y permite un éxito parcial.

## Reglas de coincidencia importantes

- La coincidencia se basa en la firma de instantáneas de pila completa (NBT y metadatos), ignorando el recuento.
- Si existen varias pilas coincidentes, elige la que tiene el mayor número.

## Regla principal

`Mainhand` significa el espacio de la barra de acceso rápido seleccionado actualmente en el momento de activación.

## Comportamiento bajo presión

- Si activas una segunda acción de Equipar artículo mientras una está en ejecución, la anterior se cancela y se reemplaza.
- Las entradas de movimiento y juego permanecen activas mientras la ejecución se ejecuta en segundo plano.

## Controles rápidos en el editor

- Arrastre LMB desde la ranura de origen a la de destino: asignar elemento.
- RMB en la ranura objetivo: asignación clara.
- `Refresh Items`: reconstruir la lista de fuentes a partir del estado actual del inventario del jugador.

???+ info "Análisis profundo: orden de ejecución de las tragamonedas"
    El orden de ejecución actual es:

1. Casco
    2. Pechera
    3. Polainas
    4. Botas
    5. De improviso
    6. mano principal

Los destinos vacíos y las coincidencias de fuentes faltantes se omiten y no se tratan como fallas graves.
