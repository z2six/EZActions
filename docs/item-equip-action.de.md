# Item-Ausruesten-Aktion

`Item-Ausruesten` speichert einen Gear-Snapshot und ruestet passende Items aus dem Inventar erneut aus.

## Ziel-Slots

- Mainhand
- Offhand
- Helmet
- Chestplate
- Leggings
- Boots

Leere Slots im Editor werden bei der Ausfuehrung uebersprungen.

## Erstellung

1. `Item-Ausruesten hinzufuegen`
2. `Title`, `Note`, `Icon` setzen
3. Items aus `Source Items` in `Equip Targets` ziehen
4. Speichern

## Source Items enthalten

- aktuelle Offhand
- getragene Ruestung
- Hauptinventar
- Hotbar

## Matching-Regeln

- Matching ueber kompletten Snapshot (inkl. NBT/Metadata), Count wird ignoriert.
- Bei mehreren Treffern wird der Stack mit der hoechsten Anzahl genommen.

## Mainhand-Regel

`Mainhand` ist der aktuell ausgewaehlte Hotbar-Slot zum Triggerzeitpunkt.

## Laufzeitverhalten

- Verarbeitung Slot fuer Slot (Teilerfolg erlaubt).
- Wird waehrenddessen eine neue Item-Ausruesten-Aktion gestartet, wird die alte abgebrochen.
- Spielerbewegung bleibt waehrenddessen aktiv.
