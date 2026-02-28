# Accion de equipar objetos

`Accion de equipar objetos` guarda un snapshot de equipo y vuelve a equipar items coincidentes desde inventario.

## Slots objetivo

- Mainhand
- Offhand
- Helmet
- Chestplate
- Leggings
- Boots

Si un slot queda vacio en el editor, se omite al ejecutar.

## Como crear

1. `Agregar equipar objetos`
2. Define `Title`, `Note`, `Icon`
3. Arrastra items desde `Source Items` a `Equip Targets`
4. Guarda

## Source Items incluye

- offhand actual
- armadura equipada
- inventario principal
- hotbar

## Reglas de matching

- Coincidencia por snapshot completo (NBT y metadata), ignorando count.
- Si hay multiples coincidencias, usa la pila con mayor cantidad.

## Regla Mainhand

`Mainhand` significa el slot de hotbar seleccionado al momento de ejecutar.

## Comportamiento en ejecucion

- Se procesa slot por slot (permite exito parcial).
- Si lanzas otra accion de equipar mientras una sigue activa, la anterior se cancela.
- El movimiento del jugador sigue activo durante la ejecucion.
