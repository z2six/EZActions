# Aktion zum Ausrüsten von Gegenständen

Mit Item Equip können Sie einen Ausrüstungsschnappschuss speichern und passende Gegenstände aus Ihrem Inventar neu ausrüsten.

## Zielplätze

Sie können Folgendes zuweisen:

- Haupthand
- Nebenbei
- Helm
- Brustpanzer
- Leggings
- Stiefel

Wenn ein Slot im Editor leer bleibt, überspringt EZ Actions ihn während der Ausführung.

## So erstellen Sie eines

1. Klicken Sie auf `Add Item Equip`.
2. Titel/Notiz/Symbol festlegen.
3. Ziehen Sie Elemente aus dem Quellraster in die Zielfelder.
4. Speichern.

### Quellraster enthält

- aktueller Nebenhandgegenstand
- ausgerüstete Rüstung
- Hauptinventar
- Hotbar

## Während der Ausführung

Wenn Sie die Aktion von Radial aus auslösen:

1. EZ Actions überprüft jeden konfigurierten Zielsteckplatz.
2. Wenn das Ziel bereits mit dem aufgezeichneten Element übereinstimmt, wird übersprungen.
3. Wenn nicht, wird der am besten passende Quellstapel gefunden.
4. Es tauscht Gegenstände in den Zielslot aus.

Es verarbeitet Slot für Slot und ermöglicht Teilerfolg.

## Wichtige Matching-Regeln

– Der Abgleich basiert auf der vollständigen Stack-Snapshot-Signatur (NBT und Metadaten), wobei die Anzahl ignoriert wird.
- Wenn mehrere übereinstimmende Stapel vorhanden sind, wird derjenige mit der höchsten Anzahl ausgewählt.

## Mainhand-Regel

`Mainhand` bedeutet Ihren aktuell ausgewählten Hotbar-Slot zum Auslösezeitpunkt.

## Verhalten unter Druck

- Wenn Sie eine zweite Aktion „Gegenstand ausrüsten“ auslösen, während eine Aktion ausgeführt wird, wird die alte Aktion abgebrochen und ersetzt.
- Bewegungs- und Gameplay-Eingaben bleiben aktiv, während die Ausführung im Hintergrund läuft.

## Schnellsteuerung im Editor

- LMB-Ziehen von der Quelle zum Ziel-Slot: Element zuweisen.
- RMB auf Zielslot: eindeutige Zuordnung.
- `Refresh Items`: Quellliste anhand des aktuellen Player-Inventarstatus neu erstellen.

???+ Info „Deep Dive: Slot-Ausführungsreihenfolge“
    Die aktuelle Ausführungsreihenfolge lautet:

1. Helm
    2. Brustpanzer
    3. Leggings
    4. Stiefel
    5. Beiläufig
    6. Haupthand

Leere Ziele und fehlende Quellübereinstimmungen werden übersprungen und nicht als schwerwiegender Fehler behandelt.
