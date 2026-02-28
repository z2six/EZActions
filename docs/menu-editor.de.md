# Hauptmenü-Editor-GUI

Der Menü-Editor ist Ihr Kontrollzentrum für die Erstellung des Radials.

Öffnen Sie es mit der Tastenkombination `Open editor`.

## Layout

- **Linker Bereich:** Aktionen und Bundles erstellen/bearbeiten/entfernen.
- **Rechter Bereich:** aktuelle Seitenliste (Root oder aktuelles Bundle).
- **Filter oben links:** filtert nach Titel, Notiz und Aktionstyptext.
- **Unten links:** Import/Export, Konfiguration, Schließen.

## Schaltflächen hinzufügen

- `Add Key Action`
- `Add Command`
- `Add Item Equip`
- `Add Bundle`

## Interaktion auflisten

### Maus

- **LMB auf Element:** auswählen.
- **LMB-Ziehelement:** innerhalb der aktuellen Seite neu anordnen.
- **LMB-Element in die Bundle-Zeile ziehen:** Element in dieses Bundle verschieben.
- **LMB-Element in die hinteren Zeilen ziehen:** Element zum übergeordneten/Stammverzeichnis verschieben.
- **RMB in der Bundle-Zeile:** Öffnen Sie das Bundle.

### Tastatur

- `Ctrl + F` fokussiert die Filterbox.
- `Enter` bearbeitet die ausgewählte Zeile.
- `Delete` oder `Backspace` entfernt die ausgewählte Zeile.
- `Up Arrow` verschiebt das ausgewählte Element nach oben.
- `Down Arrow` verschiebt das ausgewählte Element nach unten.

!!! Tipp
    Während der Filtertext aktiv ist, ist die Bewegung nach oben/unten per Tastatur deaktiviert, um mehrdeutige Neuordnungen zu vermeiden.

## Zeilentypen, die Sie sehen werden

- Normale Artikelzeilen (Aktionen oder Bundles)
- Breadcrumb-Zeile (`root/.../bundle`)
- `Back to root`
- `Back to <parent>`

## Gesperrte Einträge

Gesperrte Einträge sind vor Löschvorgängen im Spiel geschützt.

- Sie können sie nicht mit GUI-Löschen entfernen.
– Sie können sie nicht mit API-Remove-Aufrufen entfernen, die die Sperre berücksichtigen.
– Sie können weiterhin durch manuelles Bearbeiten von `config/ezactions/menu.json` entfernt werden.

## Tipps für eine schnelle Bearbeitung

- Verwenden Sie kurze Titel für klarere radiale Beschriftungen.
- Fassen Sie gemeinsam genutzte Dienstprogrammaktionen in Bündeln zusammen (für weniger Unordnung).
- Verwenden Sie das Filterfeld als schnelle Typsuche, wenn Ihr Menü umfangreich wird.

???+ Info „Deep Dive: Drag-and-Drop-Verhalten“
    - Neuordnung verwendet Einfügungslogik (kein einfacher Austausch).
    - Durch das Ablegen in ein Bundle wird die untergeordnete Liste dieses Bundles angehängt.
    - Wenn Sie zum übergeordneten Element/Stammverzeichnis wechseln, bleibt Ihre aktuelle Editoransicht erhalten, anstatt automatisch zu springen.
    – Alle erfolgreichen Verschiebungs-/Entfernungsvorgänge werden sofort auf der Festplatte gespeichert.
