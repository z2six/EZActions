# Hauptmenu-Editor

Der Menu-Editor ist das Kontrollzentrum fuer dein Radialmenu.

Oeffne ihn mit dem Keybind `Open editor`.

## Aufbau

- **Linkes Panel:** Aktionen und Bundle erstellen, bearbeiten, entfernen.
- **Rechtes Panel:** Liste der aktuellen Seite (Root oder aktuelles Bundle).
- **Filter oben links:** filtert nach `Title`, `Note` und Aktionstyp.
- **Unten links:** `Import`, `Export`, `Konfiguration`, `Close`.

## Hinzufuegen-Buttons

- `Tastenaktion hinzufuegen`
- `Befehl hinzufuegen`
- `Item-Ausruesten hinzufuegen`
- `Bundle hinzufuegen`

## Listen-Interaktion

### Maus

- **LMB auf Eintrag:** auswaehlen.
- **LMB Drag:** innerhalb der aktuellen Seite neu anordnen.
- **Drag auf Bundle-Zeile:** in dieses Bundle verschieben.
- **Drag auf Back-Zeilen:** nach Parent/Root verschieben.
- **RMB auf Bundle:** Bundle oeffnen.

### Tastatur

- `Ctrl + F`: Fokus auf `Filter`.
- `Enter`: ausgewaehlte Zeile bearbeiten.
- `Delete` oder `Backspace`: Auswahl loeschen.
- `Up Arrow`: nach oben.
- `Down Arrow`: nach unten.

!!! tip
    Pfeil-Reorder ist deaktiviert, solange ein Filtertext aktiv ist.

## Zeilentypen

- normale Eintragszeilen (Action oder Bundle)
- Breadcrumb-Zeile (`root/.../bundle`)
- `Back to root`
- `Back to <parent>`

## Gesperrte Eintraege (locked)

`locked`-Eintraege koennen ingame nicht geloescht werden.

- Nicht per GUI delete.
- Nicht per lock-aware API remove.
- Manuell per `config/ezactions/menu.json` weiterhin moeglich.
