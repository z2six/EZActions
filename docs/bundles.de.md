# Bündel

Bundles sind Ordner (Kategorien) innerhalb des radialen Baums.

Verwenden Sie sie, um Aktionen nach Kontext zu gruppieren: Kampf, Bauen, Nutzen, Rollenspiel usw.

## Bündelfelder

- **Titel** (wird auch als interne ID verwendet)
- **Hinweis** (optional)
- **Symbol**
- **Vom Hauptradial ausblenden**
- **Tastenkombination aktivieren**

## Tastenkombinationen bündeln

Wenn `Enable keybind` aktiviert ist, registriert EZ Actions eine dedizierte Tastenkombination für dieses Bundle.

!!! Warnung „Neustart erforderlich“
    Die Bundle-Tastenkombinationsregistrierung wird beim nächsten Client-Neustart angewendet.

EZ Actions zeigt eine Client-Nachricht an, wenn ein Neustart erforderlich ist (einschließlich API-erstellter Bundles).

## Vor Hauptradial verstecken

Wenn aktiviert:

– Das Bundle ist auf der Root-Radialseite ausgeblendet.
- Bundle existiert noch im Menümodell.
– Bundle kann weiterhin über API oder Bundle-Tastenkombination geöffnet werden.

Gut für „erweiterte Seiten“, bei denen das Stammverzeichnis nicht überladen werden soll.

## Verschachtelte Bundles

Bundles können enthalten:

- Schlüsselaktionen
- Befehlsaktionen
- Aktionen zum Ausrüsten von Gegenständen
- weitere Pakete

## Best Practices

- Halten Sie Root klein und mit hoher Priorität.
- Niederfrequente Aktionen in tiefere Bündel bündeln.
- Geben Sie den Bundles klare Symbole und Kurznamen.

## Gesperrte Pakete

Ein Bundle kann mit `locked` markiert werden (normalerweise über API oder JSON).

- Löschpfade im Spiel entfernen es nicht.
– Durch manuelle JSON-Änderungen kann es immer noch entfernt werden.

???+ Info „Deep Dive: Identität und Einzigartigkeit“
    Der Bundle-Titel wird in Editor-Workflows als Bundle-ID verwendet.

Doppelte Bundle-Namen können zu Mehrdeutigkeiten führen, daher blockiert der Editor das Speichern doppelter Bundle-Titel/-IDs.
