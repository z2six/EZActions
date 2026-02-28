#API

Esta página documenta la API pública en `org.z2six.ezactions.api` para EZ Actions **2.0.0.0**.

Audiencia:

- desarrolladores de mods
- desarrolladores de modpacks
- usuarios de automatización avanzada

## Qué puede hacer la API

Respuesta corta: todo lo que los usuarios pueden hacer en la GUI, además de control adicional del tiempo de ejecución.

- Leer árbol de menú
- Agregar/actualizar/eliminar/reordenar acciones y paquetes
- Marcar entradas como bloqueadas
- Importar/exportar/validar menú JSON
- Abrir editor/config/pantallas radiales
- Abrir radial directamente en un paquete
- Abrir radiales de tiempo de ejecución temporales sin persistir
- Entrada de teclas de activación y secuenciación de comandos
- Suscríbete a eventos API simples

## Acceso

```java
import org.z2six.ezactions.api.EzActions;
import org.z2six.ezactions.api.EzActionsApi;

EzActionsApi api = EzActions.get();
```

## Core Concepts

### Ruta del menú

`MenuPath` dirige los paquetes por **cadena de título del paquete** desde la raíz.

```java
MenuPath root = MenuPath.root();
MenuPath p = MenuPath.root().child("Utilities").child("Combat");
```

Notes:

- La coincidencia de rutas distingue entre mayúsculas y minúsculas y es literal al título.
- ruta vacía = lista raíz.

### Entradas bloqueadas

`locked=true` significa protegido contra flujos de eliminación dentro del juego y eliminaciones de API con reconocimiento de bloqueo.

Las entradas bloqueadas aún se pueden eliminar mediante ediciones JSON manuales.

### Modelo de persistencia

La mayoría de las llamadas API mutantes persisten inmediatamente.

Generalmente no necesita una llamada de guardado adicional.

### Enhebrado

Las devoluciones de llamadas/eventos de API están diseñadas para el uso de subprocesos del cliente.

## Tipos de acción

### Acción clave

```java
import org.z2six.ezactions.data.click.ClickActionKey;
import org.z2six.ezactions.helper.InputInjector;

ClickActionKey keyAction = new ClickActionKey(
    "key.inventory",                    // mapping id or label
    false,                              // toggle
    InputInjector.DeliveryMode.AUTO     // AUTO/INPUT/TICK
);
```

### Command Action

```java
import org.z2six.ezactions.data.click.ClickActionCommand;

ClickActionCommand cmd = new ClickActionCommand(
    "/time set day\n/time set night", // multi-line
    10,                                 // delay ticks between lines
    true                                // cycleCommands
);
```

### Item Equip Action

```java
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import org.z2six.ezactions.data.click.ClickActionItemEquip;

HolderLookup.Provider regs = Minecraft.getInstance().player.level().registryAccess();
ItemStack stack = Minecraft.getInstance().player.getMainHandItem();

ClickActionItemEquip.StoredItem stored =
    ClickActionItemEquip.StoredItem.fromStack(stack, regs);

ClickActionItemEquip eq = new ClickActionItemEquip(
    java.util.Map.of(ClickActionItemEquip.TargetSlot.MAINHAND, stored)
);
```

## Icons

```java
import org.z2six.ezactions.data.icon.IconSpec;

IconSpec itemIcon = IconSpec.item("minecraft:ender_pearl");
IconSpec customIcon = IconSpec.custom("custom:my_icon");
```

## Top-Level EzActionsApi

| Método | Propósito |
|---|---|
| @@CÓDIGO0@@ | Abrir la pantalla del editor del juego |
| @@CÓDIGO1@@ | Abrir pantalla de configuración |
| @@CÓDIGO2@@ | Radial de raíz abierta |
| @@CÓDIGO3@@ | Radial abierto en la identificación del paquete |
| @@CÓDIGO4@@ | Abrir radial de tiempo de ejecución único desde JSON |
| @@CÓDIGO5@@ | API heredada de acción de adición directa |
| @@CÓDIGO6@@ | API heredada de paquete de adición directa |
| @@CÓDIGO7@@ | Eliminación heredada por identificación |
| @@CÓDIGO8@@ | Movimiento heredado en padre/raíz |
| @@CÓDIGO9@@ | La fuerza persiste |
| @@CÓDIGO10@@ | Importación de portapapeles estilo GUI |
| @@CÓDIGO11@@ | Exportación de portapapeles estilo GUI |
| @@CÓDIGO12@@ | Superficie de sólo lectura |
| @@CÓDIGO13@@ | Superficie mutante |
| @@CÓDIGO14@@ | Superficie de importación/exportación JSON |
| @@CÓDIGO15@@ | Entrada + superficie auxiliar de comando |
| @@CÓDIGO16@@ | Superficie auxiliar de interfaz de usuario/tiempo de ejecución |
| @@CÓDIGO17@@ | Ganchos para eventos |

## MenúLeer

Interfaz: `MenuRead`

- @@CÓDIGO0@@
- @@CÓDIGO1@@
- @@CÓDIGO2@@
- @@CÓDIGO3@@

### Campos de instantáneas de ApiMenuItem

Lo esencial:

- `id`, `title`, `note`
- `isCategory`, `typeLabel`
- `iconKind`, `iconId`
- `hideFromMainRadial`, `bundleKeybindEnabled`, `locked`

Detalles de la acción:

- @@CÓDIGO0@@
- @@CÓDIGO1@@
- acción clave: `keyMappingName`, `keyToggle`, `keyMode`
- acción de comando: `commandRaw`, `commandDelayTicks`, `commandCycleCommands`
- Acción de equipar el artículo: `itemEquipSlotsJson`

## MenúEscribir

Interfaz: `MenuWrite`

### Crear

- @@CÓDIGO0@@
- @@CÓDIGO1@@
- @@CÓDIGO2@@
- @@CÓDIGO3@@

### Mover

- @@CÓDIGO0@@
- @@CÓDIGO1@@

### Eliminar

- @@CÓDIGO0@@
- @@CÓDIGO1@@

### Actualizar

- @@CÓDIGO0@@
- @@CÓDIGO1@@
- @@CÓDIGO2@@
- @@CÓDIGO3@@

### Ayudantes estructurales

- `ensureBundles(path)` crea una cadena de paquetes faltantes por título.
- `upsertFromJson(path, jsonObjectOrArray)` agrega/reemplaza elementos del fragmento JSON.

### Ejemplo: crear un paquete de utilidades bloqueado

```java
var write = EzActions.get().menuWrite();

MenuPath root = MenuPath.root();
String bundleId = write.addBundle(
    root,
    "Utilities",
    "Pack-defined utilities",
    IconSpec.item("minecraft:shulker_box"),
    false,   // hideFromMainRadial
    true,    // bundleKeybindEnabled
    true     // locked
).orElseThrow();

write.addAction(
    root.child("Utilities"),
    "Open Inventory",
    "Quick inventory",
    IconSpec.item("minecraft:chest"),
    new ClickActionKey("key.inventory", false, InputInjector.DeliveryMode.AUTO),
    true
);
```

## ImportExport

Interfaz: `ImportExport`

- @@CÓDIGO0@@
- @@CÓDIGO1@@
- @@CÓDIGO2@@
- @@CÓDIGO3@@
- @@CÓDIGO4@@

### Reglas de validación (nivel alto)

- la raíz debe ser un objeto o una matriz
- cada artículo debe contener exactamente uno de:
  - @@CÓDIGO0@@
  - @@CÓDIGO1@@
- el objeto de acción debe incluir `type` válido
- valores booleanos opcionales (`hideFromMainRadial`, `bundleKeybindEnabled`, `locked`) deben ser booleanos cuando estén presentes

## Operaciones de entrada

Interfaz: `InputOps`

- @@CÓDIGO0@@
- @@CÓDIGO1@@

Ejemplo:

```java
var input = EzActions.get().inputOps();

input.deliver("key.inventory", false, InputOps.Mode.AUTO);
input.enqueueCommands(new String[]{"/time set day", "/weather clear"}, 10);
```

## EditorOps

Interfaz: `EditorOps`

- @@CÓDIGO0@@
- @@CÓDIGO1@@
- @@CÓDIGO2@@
- @@CÓDIGO3@@
- @@CÓDIGO4@@

## Estilo radial temporal dinámico

Clase: `DynamicRadialStyle`

Todos los campos son anulaciones opcionales que aceptan valores NULL.

Bandera:

- @@CÓDIGO0@@
- @@CÓDIGO1@@
- @@CÓDIGO2@@
- @@CÓDIGO3@@

Animación:

- @@CÓDIGO0@@
- @@CÓDIGO1@@
- @@CÓDIGO2@@
- @@CÓDIGO3@@
- @@CÓDIGO4@@
- `openStyle` (`WIPE|FADE|NONE`)
- `openDirection` (`CW|CCW`)
- `hoverStyle` (`FILL_SCALE|FILL_ONLY|SCALE_ONLY|NONE`)

Diseño:

- @@CÓDIGO0@@
- @@CÓDIGO1@@
- @@CÓDIGO2@@
- @@CÓDIGO3@@
- @@CÓDIGO4@@
- @@CÓDIGO5@@
- `designStyle` (`SOLID|SEGMENTED|OUTLINE|GLASS`)

### Ejemplo: Radial de tiempo de ejecución temporal

```java
String json = """
[
  {
    "id": "tmp_inv",
    "title": "Inventory",
    "icon": "minecraft:chest",
    "action": { "type": "KEY", "name": "key.inventory", "toggle": false, "mode": "AUTO" }
  },
  {
    "id": "tmp_day",
    "title": "Day",
    "icon": "minecraft:sunflower",
    "action": { "type": "COMMAND", "command": "/time set day", "delayTicks": 0, "cycleCommands": false }
  }
]
""";

DynamicRadialStyle style = new DynamicRadialStyle(
    0xAA000000, 0xFFF20044, 0x66FFFFFF, 0xFFFFFFFF,
    true, true, true,
    125, 0.05,
    "WIPE", "CW", "FILL_SCALE",
    18, 72, 28,
    8, 6, 0,
    "SOLID"
);

EzActions.get().editorOps().openTemporaryRadial(json, style);
```

## Events

Clase: `ApiEvents`

- @@CÓDIGO0@@
- @@CÓDIGO1@@

Cargas útiles:

- @@CÓDIGO0@@, @@CÓDIGO1@@
- `ImportEvent.target`, `ImportEvent.json`, `ImportEvent.count`

Ejemplo:

```java
var events = EzActions.get().events();

events.onMenuChanged(evt -> {
    System.out.println("Menu changed: " + evt.reason + " at " + evt.path);
});

events.onImported(evt -> {
    System.out.println("Imported " + evt.count + " entries into " + evt.target);
});
```

## JSON Item Schema (API + Import/Export)

```json
{
  "id": "string",
  "title": "string or text component",
  "note": "string or text component",
  "icon": "minecraft:item_id",
  "hideFromMainRadial": false,
  "bundleKeybindEnabled": false,
  "locked": false,
  "action": {
    "type": "KEY | COMMAND | ITEM_EQUIP"
  },
  "children": []
}
```

Rules:

- use `action` para elementos de acción
- use `children` para paquetes
- no incluir ambos en el mismo objeto

## Patrón de interoperabilidad estilo KubeJS

La sintaxis exacta depende de la configuración de KubeJS, pero el flujo suele ser:

Primero, cargar la clase API de Java
2. obtener singleton a través de `EzActions.get()`
3. llamar a los métodos `menuWrite()` / `editorOps()`

Pseudoflujo:

```js
// pseudo example, adapt to your exact loader bridge
const EzActions = Java.loadClass('org.z2six.ezactions.api.EzActions')
const api = EzActions.get()

// use api.menuWrite(), api.importExport(), api.editorOps(), etc.
```

## API Notes For Pack Authors

- Si las escrituras de su API habilitan combinaciones de teclas en paquete, los usuarios aún deben reiniciar para registrar las combinaciones de teclas.
- EZ Actions ahora notifica a los usuarios en el chat cuando es necesario reiniciar para nuevas combinaciones de teclas del paquete.
- Mantenga identificadores/títulos estables si planea parchear los menús con el tiempo.

???+ advertencia "Nota de compatibilidad"
    Las firmas de API pueden cambiar en versiones futuras. Esta página coincide con el comportamiento 2.0.0.0.