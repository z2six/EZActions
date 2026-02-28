# API

Diese Seite beschreibt die oeffentliche API `org.z2six.ezactions.api` fuer EZ Actions 2.0.0.0.

## Was die API kann

Alles Wesentliche aus der GUI plus Runtime-Steuerung:

- Menubaum lesen
- Actions/Bundle erstellen, aendern, loeschen, verschieben
- Eintraege als `locked` markieren
- JSON importieren/exportieren/validieren
- Editor/Config/Radial oeffnen
- temporaeres Runtime-Radial ohne Persistenz oeffnen

## Zugriff

```java
import org.z2six.ezactions.api.EzActions;
import org.z2six.ezactions.api.EzActionsApi;

EzActionsApi api = EzActions.get();
```

## Aktionstypen

- `KEY`
- `COMMAND`
- `ITEM_EQUIP`

## Hinweis fuer Modpacks

Wenn API Bundle-Keybinds aktiviert, ist fuer Nutzer ein Neustart noetig.

## Vollstaendige Referenz

Fuer komplette Signaturen und erweiterte Beispiele die englische API-Seite verwenden.
