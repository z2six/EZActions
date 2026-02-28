# Paquetes (Bundle)

Un `paquete` es una carpeta/categoria dentro del arbol radial.

## Campos de paquete

- **Title**
- **Note** (opcional)
- **Icon**
- **Hide from main radial**
- **Enable keybind**

## Keybind de paquete

Si activas `Enable keybind`, EZ Actions registra un keybind dedicado para ese paquete.

!!! warning "Requiere reinicio"
    El registro del keybind de paquete se aplica al reiniciar el cliente.

## Hide from main radial

Si esta activo:

- El paquete no aparece en root.
- El paquete sigue existiendo en el modelo.
- Sigue accesible por API o por keybind de paquete.

## Paquetes bloqueados (locked)

`locked` protege el paquete contra borrado en juego.

- No se borra desde GUI.
- Si puede borrarse editando JSON manualmente.
