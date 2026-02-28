# Import / Export

Import und Export in EZ Actions laufen ueber die Zwischenablage.

## Export

Im Menu-Editor `Export` klicken.

Ergebnis:

- Voller Root-Baum wird als JSON serialisiert.
- JSON wird in die Zwischenablage kopiert.

## Import

Im Menu-Editor `Import` klicken.

Ergebnis:

- JSON wird geparst und validiert.
- Bei Erfolg in den Zielpfad importiert.

## Typische Fehler

- Clipboard leer
- Clipboard ist kein JSON
- Root JSON ist kein Array
- Entry ist kein Objekt / ungueltig

## Empfohlener Ablauf

1. Backup exportieren.
2. JSON bearbeiten.
3. Importieren.
4. Bei Problemen Backup reimportieren.
