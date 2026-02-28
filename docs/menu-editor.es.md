# Editor del menu principal

El editor de menu es el centro para construir tu radial.

Abrelo con la tecla `Open editor`.

## Diseno

- **Panel izquierdo:** crear, editar y eliminar acciones y paquetes.
- **Panel derecho:** lista de la pagina actual (root o paquete actual).
- **Filter arriba a la izquierda:** filtra por `Title`, `Note` y tipo de accion.
- **Parte inferior izquierda:** `Import`, `Export`, `Configuracion`, `Close`.

## Botones de alta

- `Agregar accion de tecla`
- `Agregar comando`
- `Agregar equipar objetos`
- `Agregar paquete`

## Interaccion con la lista

### Mouse

- **LMB en item:** seleccionar.
- **LMB drag:** reordenar dentro de la pagina actual.
- **Drag sobre un paquete:** mover dentro de ese paquete.
- **Drag sobre filas de retorno:** mover a parent/root.
- **RMB sobre paquete:** abrir paquete.

### Teclado

- `Ctrl + F`: foco en `Filter`.
- `Enter`: editar fila seleccionada.
- `Delete` o `Backspace`: eliminar seleccion.
- `Up Arrow`: mover arriba.
- `Down Arrow`: mover abajo.

!!! tip
    El movimiento con flechas se desactiva cuando el filtro tiene texto activo.

## Tipos de filas

- filas normales (accion o paquete)
- fila breadcrumb (`root/.../bundle`)
- `Back to root`
- `Back to <parent>`

## Entradas bloqueadas (locked)

Una entrada `locked` no se puede borrar desde el juego.

- No se borra con delete en GUI.
- No se borra con remove API que respeta lock.
- Si se puede borrar editando `config/ezactions/menu.json` manualmente.
