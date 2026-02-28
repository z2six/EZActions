# Import-Export

Der Import/Export von EZ Actions funktioniert über Ihre Zwischenablage.

## Exportieren

Klicken Sie im Menü-Editor auf `Export`.

Ergebnis:

– Der aktuelle vollständige Stammbaum wird in JSON serialisiert.
- JSON wird in die Zwischenablage kopiert.

## Importieren

Klicken Sie im Menü-Editor auf `Import`.

Ergebnis:

- Zwischenablage-JSON wird analysiert und validiert.
- Bei Erfolg werden importierte Einträge pro Importpfad hinzugefügt/ersetzt.

## Häufige Fehlermeldungen

- Zwischenablage ist leer
- Zwischenablage ist kein JSON
- Root-JSON ist kein Array
- Eintrag ist nicht Gegenstand / ungültig

## Praktischer Arbeitsablauf

1. Aktuelles Menü als Backup in eine Textdatei exportieren.
2. Testen Sie die Änderungen in JSON.
3. Importieren.
4. Führen Sie bei Bedarf ein Rollback durch, indem Sie das vorherige Backup importieren.

## JSON-Form

Die oberste Ebene unterstützt eine Reihe von Menüelementen (oder einzelne Elemente in einigen API-Pfaden).

Jeder Menüpunkt muss entweder:

- ein **Aktionselement** mit dem Objekt `action`
- ein **Bundle**-Element mit `children`-Array

### Beispiel für eine minimale Aktion

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
