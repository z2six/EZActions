# Importar Exportar

La importación/exportación de EZ Actions funciona a través de su portapapeles.

## Exportar

En el Editor de menús, haga clic en `Export`.

Resultado:

- El árbol raíz completo actual está serializado en JSON.
- JSON se copia al portapapeles.

## Importar

En el Editor de menús, haga clic en `Import`.

Resultado:

- El portapapeles JSON se analiza y valida.
- En caso de éxito, las entradas importadas se agregan/reemplazan por ruta de importación.

## Mensajes de error comunes

- El portapapeles está vacío.
- El portapapeles no es JSON
- JSON raíz no es una matriz
- La entrada no es objeto / no es válida

## Flujo de trabajo práctico

1. Exporte el menú actual a un archivo de texto como copia de seguridad.
2. Pruebe las ediciones en JSON.
3. Importar.
4. Si es necesario, retroceda importando la copia de seguridad anterior.

## Forma JSON

El nivel superior admite una variedad de elementos de menú (o un solo elemento en algunas rutas API).

Cada elemento del menú debe ser:

- un elemento de **acción** con objeto `action`
- un elemento **paquete** con matriz `children`

### Ejemplo de acción mínima

```json
{
  "id": "act_123",
  "title": "Inventory",
  "icon": "minecraft:chest",
  "action": {
    "type": "KEY",
    "name": "key.inventory",
    "toggle": false,
    "mode": "AUTO"
  }
}
```

### Minimal Bundle Example

```json
{
  "id": "bundle_abc",
  "title": "Utilities",
  "icon": "minecraft:shulker_box",
  "hideFromMainRadial": false,
  "bundleKeybindEnabled": true,
  "locked": false,
  "children": []
}
```

???+ info "Deep dive: schema details"
    - `title` and `note` accept plain string or text component JSON.
    - `locked` is optional; defaults false.
    - `action.type` currently supports `KEY`, `COMMAND`, `ITEM_EQUIP`.
    - `KEY` fields: `name`, `toggle`, `mode`.
    - `COMMAND` fields: `command`, `delayTicks`, `cycleCommands`.
    - `ITEM_EQUIP` fields: `slots` map with stored item snapshots.
