# GUI del editor del menú principal

El Editor de menús es su centro de control para construir el radial.

Ábralo con la combinación de teclas `Open editor`.

## Disposición

- **Panel izquierdo:** crear/editar/eliminar acciones y paquetes.
- **Panel derecho:** lista de páginas actuales (raíz o paquete actual).
- **Filtro superior izquierdo:** filtra por título, nota y texto de tipo de acción.
- **Abajo a la izquierda:** importar/exportar, configurar, cerrar.

## Agregar botones

- @@CÓDIGO0@@
- @@CÓDIGO1@@
- @@CÓDIGO2@@
- @@CÓDIGO3@@

## Lista de interacción

### Ratón

- **LMB en el artículo:** seleccionar.
- **LMB arrastrar elemento:** reordenar dentro de la página actual.
- **LMB arrastra el artículo a la fila del paquete:** mueve el artículo a ese paquete.
- **LMB arrastra el elemento a las filas posteriores:** mueve el elemento al nivel principal/raíz.
- **RMB en la fila del paquete:** abre ese paquete.

### Teclado

- `Ctrl + F` enfoca el cuadro de filtro.
- `Enter` edita la fila seleccionada.
- `Delete` o `Backspace` elimina la fila seleccionada.
- `Up Arrow` mueve el elemento seleccionado hacia arriba.
- `Down Arrow` mueve el elemento seleccionado hacia abajo.

!!! propina
    El movimiento del teclado hacia arriba/abajo está deshabilitado mientras el texto del filtro está activo, para evitar reordenamientos ambiguos.

## Tipos de filas que verás

- Filas de artículos normales (acciones o paquetes)
- Fila de ruta de navegación (`root/.../bundle`)
- @@CÓDIGO1@@
- @@CÓDIGO2@@

## Entradas bloqueadas

Las entradas bloqueadas están protegidas de operaciones de eliminación dentro del juego.

- No puedes eliminarlos con la eliminación de GUI.
- No puedes eliminarlos con llamadas de eliminación de API que respetan el bloqueo.
- Aún se pueden eliminar editando manualmente `config/ezactions/menu.json`.

## Consejos para una edición rápida

- Utilice títulos cortos para etiquetas radiales más limpias.
- Coloque las acciones de utilidades compartidas en paquetes (para reducir el desorden de raíces).
- Utilice el cuadro de filtro como búsqueda rápida cuando su menú sea enorme.

???+ info "Inmersión profunda: comportamiento de arrastrar y soltar"
    - El reordenamiento utiliza lógica de inserción (no un simple intercambio).
    - Al ingresar a un paquete, se agrega a la lista secundaria de ese paquete.
    - Al soltar al padre/raíz se mantiene la vista actual del editor en lugar de saltar automáticamente.
    - Todas las operaciones de mover/eliminar exitosas persisten inmediatamente en el disco.
