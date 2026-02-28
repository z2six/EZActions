# Befehlsaktion

Verwenden Sie eine Befehlsaktion, um Befehle vom Radial zu senden.

## Felder

- **Titel**
- **Hinweis**
- **Befehl** (mehrzeilig)
- **Verzögerung mehrerer Befehle (Ticks)**
- **Zyklusbefehle (einer pro Verwendung)**
- **Symbol**

## Befehlsfeldregeln

- Eine Zeile = ein Befehl.
- Das führende `/` ist optional (EZ Actions entfernt es vor dem Senden).
- Leerzeilen werden ignoriert.

## Verzögerungsverhalten

`Multi-command delay (ticks)` steuert den Abstand zwischen Befehlszeilen, wenn nicht gewechselt wird.

- `0`: Zeilen sofort senden.
- `>0`: Warteschlange Zeile für Zeile mit dieser Verzögerung.

## Zyklusbefehle

Wenn aktiviert, sendet jede radiale Verwendung genau eine Zeile und rotiert zur nächsten.

Beispiel:

```text
/time set day
/time set night
```

Use #1 -> day  
Use #2 -> night  
Use #3 -> day

## Praktische Anwendungsfälle

- Schnelle Dienstprogrammbefehle (`/home`, `/spawn`, `/warp mine`)
- Rollenspiel schaltet um (`/hat`, `/nick`)
- Admin-Workflows sind zeilenübergreifend aufgeteilt

## Notizen

– Dies ist ein clientseitiger Versand: Serverberechtigungen gelten weiterhin.
– Wenn eine neue Befehlssequenz beginnt, wird die vorherige in der Warteschlange befindliche Sequenz ersetzt.

???+ Info „Deep Dive: Sequenzierungsmodell“
    – Nicht-zyklische mehrzeilige Befehle verwenden einen Client-Tick-Sequenzer.
    - Der Wechselmodus speichert einen internen Cursor in der Aktionsinstanz.
    - Die Modi „Cycling“ und „Immediate“ brechen Flugsequenzen vor dem Versand ab.
