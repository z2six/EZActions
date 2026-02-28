# API

Diese Seite dokumentiert die öffentliche API in `org.z2six.ezactions.api` für EZ Actions **2.0.0.0**.

Publikum:

- Mod-Entwickler
- Modpack-Entwickler
- fortgeschrittene Automatisierungsbenutzer

## Was die API kann

Kurze Antwort: Alles, was Benutzer in der GUI tun können, plus zusätzliche Laufzeitkontrolle.

- Menübaum lesen
- Aktionen und Bundles hinzufügen/aktualisieren/entfernen/neu anordnen
- Einträge als gesperrt markieren
- Menü JSON importieren/exportieren/validieren
- Öffnen Sie die Bildschirme „editor/config/radial“.
- Radial direkt am Bündel öffnen
- Öffnen Sie temporäre Laufzeitradiale, ohne sie beizubehalten
- Trigger-Tasteneingabe und Befehlssequenzierung
- Abonnieren Sie einfache API-Ereignisse

## Zugang

```java
import org.z2six.ezactions.api.EzActions;
import org.z2six.ezactions.api.EzActionsApi;

EzActionsApi api = EzActions.get();
```

## Core Concepts

### MenuPath

`MenuPath` adressiert Bundles anhand der **Bundle-Titelkette** vom Stammverzeichnis aus.

```java
MenuPath root = MenuPath.root();
MenuPath p = MenuPath.root().child("Utilities").child("Combat");
```

Notes:

- Beim Pfadabgleich wird die Groß-/Kleinschreibung beachtet und das Titelliteral berücksichtigt.
- leerer Pfad = Stammliste.

### Gesperrte Einträge

`locked=true` bedeutet geschützt vor Löschflüssen im Spiel und sperrenbewussten API-Entfernungen.

Gesperrte Einträge können weiterhin durch manuelle JSON-Bearbeitungen entfernt werden.

### Persistenzmodell

Die meisten mutierenden API-Aufrufe bleiben sofort bestehen.

Sie benötigen im Allgemeinen keinen zusätzlichen Speicheraufruf.

### Einfädeln

API-Rückrufe/Ereignisse sind für die Verwendung durch Client-Threads konzipiert.

## Aktionstypen

### Schlüsselaktion

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

| Methode | Zweck |
|---|---|
| `openEditor(Screen)` | Öffnen Sie den In-Game-Editor-Bildschirm |
| `openConfig(Screen)` | Konfigurationsbildschirm öffnen |
| `openRadial()` | Wurzelradial öffnen |
| `openRadialAtBundle(String)` | Radial bei Bündel-ID öffnen |
| `openTemporaryRadial(String, DynamicRadialStyle)` | Einmaliges Runtime-Radial von JSON öffnen |
| `addAction(...)` | Legacy-API zum direkten Hinzufügen von Aktionen |
| `addBundle(...)` | Legacy-API zum direkten Hinzufügen von Bundles |
| `removeItem(String)` | Legacy nach ID entfernen |
| `moveWithin(String,int,int)` | Legacy-Verschiebung in Parent/Root |
| `persist()` | Fortbestehen erzwingen |
| `importFromClipboard()` | Import der Zwischenablage im GUI-Stil |
| `exportToClipboard()` | Export der Zwischenablage im GUI-Stil |
| `menuRead()` | Schreibgeschützte Oberfläche |
| `menuWrite()` | Mutierende Oberfläche |
| `importExport()` | JSON-Import-/Exportoberfläche |
| `inputOps()` | Eingabe- und Befehlshilfeoberfläche |
| `editorOps()` | UI-/Laufzeit-Hilfsoberfläche |
| `events()` | Ereignis-Hooks |

## MenuRead

Schnittstelle: `MenuRead`

- `list(MenuPath path)`
- `findById(String id)`
- `currentPath()`
- `existsPath(MenuPath path)`

### ApiMenuItem-Snapshot-Felder

Grundlagen:

- `id`, `title`, `note`
- `isCategory`, `typeLabel`
- `iconKind`, `iconId`
- `hideFromMainRadial`, `bundleKeybindEnabled`, `locked`

Aktionsdetails:

- `actionType`
- `actionJson`
- Schlüsselaktion: `keyMappingName`, `keyToggle`, `keyMode`
- Befehlsaktion: `commandRaw`, `commandDelayTicks`, `commandCycleCommands`
- Aktion zum Ausrüsten von Gegenständen: `itemEquipSlotsJson`

## MenuWrite

Schnittstelle: `MenuWrite`

### Erstellen

- `addAction(path, title, note, action, locked)`
- `addAction(path, title, note, icon, action, locked)`
- `addBundle(path, title, note, hideFromMainRadial, bundleKeybindEnabled, locked)`
- `addBundle(path, title, note, icon, hideFromMainRadial, bundleKeybindEnabled, locked)`

### Bewegen

- `moveWithin(path, fromIndex, toIndex)`
- `moveTo(itemId, targetBundle)`

### Entfernen

- `removeFirst(path, predicate)`
- `removeById(id)`

### Aktualisieren

- `updateMeta(id, titleOrNull, noteOrNull, iconOrNull)`
- `replaceAction(id, action)`
- `setBundleFlags(id, hideFromMainRadial, bundleKeybindEnabled)`
- `setLocked(id, locked)`

### Strukturelle Helfer

- `ensureBundles(path)` erstellt fehlende Bundle-Kette nach Titel.
- `upsertFromJson(path, jsonObjectOrArray)` Elemente aus JSON-Snippet hinzufügen/ersetzen.

### Beispiel: Erstellen Sie ein gesperrtes Dienstprogrammpaket

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

Schnittstelle: `ImportExport`

- `exportAllJson()`
- `exportBundleJson(path)`
- `importInto(path, json)`
- `replaceAll(json)`
- `validate(json)`

### Validierungsregeln (hohe Ebene)

- Root muss ein Objekt oder Array sein
- Jedes Element muss genau eines der folgenden Elemente enthalten:
  - `action`
  - `children`
- Aktionsobjekt muss gültigen `type` enthalten
- optionale boolesche Werte (`hideFromMainRadial`, `bundleKeybindEnabled`, `locked`) müssen boolesche Werte sein, wenn vorhanden

## InputOps

Schnittstelle: `InputOps`

- `deliver(mappingNameOrLabel, toggle, mode)`
- `enqueueCommands(commands, perLineDelayTicks)`

Beispiel:

```java
var input = EzActions.get().inputOps();

input.deliver("key.inventory", false, InputOps.Mode.AUTO);
input.enqueueCommands(new String[]{"/time set day", "/weather clear"}, 10);
```

## EditorOps

Schnittstelle: `EditorOps`

- `openEditor()`
- `openConfig()`
- `openRadial()`
- `openRadialAtBundle(bundleId)`
- `openTemporaryRadial(jsonItemOrArray, styleOrNull)`

## Dynamischer temporärer Radialstil

Klasse: `DynamicRadialStyle`

Bei allen Feldern handelt es sich um optionale Nullable-Überschreibungen.

Farben:

- `ringColor`
- `hoverColor`
- `borderColor`
- `textColor`

Animation:

- `animationsEnabled`
- `animOpenClose`
- `animHover`
- `openCloseMs`
- `hoverGrowPct`
- `openStyle` (`WIPE|FADE|NONE`)
- `openDirection` (`CW|CCW`)
- `hoverStyle` (`FILL_SCALE|FILL_ONLY|SCALE_ONLY|NONE`)

Design:

- `deadzone`
- `baseOuterRadius`
- `ringThickness`
- `scaleStartThreshold`
- `scalePerItem`
- `sliceGapDeg`
- `designStyle` (`SOLID|SEGMENTED|OUTLINE|GLASS`)

### Beispiel: Temporäres Laufzeitradial

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

Klasse: `ApiEvents`

- `onMenuChanged(Consumer<MenuChanged>)`
- `onImported(Consumer<ImportEvent>)`

Nutzlasten:

- `MenuChanged.path`, `MenuChanged.reason`
- `ImportEvent.target`, `ImportEvent.json`, `ImportEvent.count`

Beispiel:

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

- Verwenden Sie `action` für Aktionselemente
- Verwenden Sie `children` für Bundles
- Schließen Sie nicht beide in dasselbe Objekt ein

## Interop-Muster im KubeJS-Stil

Die genaue Syntax hängt von Ihrem KubeJS-Setup ab, aber der Ablauf ist normalerweise:

1. Java-API-Klasse laden
2. Singleton über `EzActions.get()` abrufen
3. Rufen Sie die Methoden `menuWrite()` / `editorOps()` auf

Pseudofluss:

```js
// pseudo example, adapt to your exact loader bridge
const EzActions = Java.loadClass('org.z2six.ezactions.api.EzActions')
const api = EzActions.get()

// use api.menuWrite(), api.importExport(), api.editorOps(), etc.
```

## API Notes For Pack Authors

– Wenn Ihre API schreibt, dass Bundle-Tastenkombinationen aktiviert werden sollen, müssen Benutzer für die Tastenkombinationsregistrierung dennoch einen Neustart durchführen.
– EZ Actions benachrichtigt Benutzer jetzt im Chat, wenn ein Neustart für neue Bundle-Tastenkombinationen erforderlich ist.
- Behalten Sie stabile IDs/Titel bei, wenn Sie planen, Menüs im Laufe der Zeit zu patchen.

???+ Warnung „Kompatibilitätshinweis“
    API-Signaturen können sich in zukünftigen Versionen ändern. Diese Seite entspricht dem Verhalten von 2.0.0.0.