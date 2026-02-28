# Befehlsaktion

`Befehlsaktion` sendet Befehle aus dem Radialmenu.

## Felder

- **Title**
- **Note**
- **Command** (mehrzeilig)
- **Multi-command delay (ticks)**
- **Cycle commands (one per use)**
- **Icon**

## Regeln fuer das Command-Feld

- Eine Zeile = ein Befehl.
- Fuehrendes `/` ist optional.
- Leere Zeilen werden ignoriert.

## Delay-Verhalten

`Multi-command delay (ticks)` steuert den Abstand zwischen Zeilen im Nicht-Cycle-Modus.

- `0`: sofort senden.
- `>0`: zeilenweise mit Delay.

## Cycle commands

Wenn aktiv, sendet jede Nutzung genau eine Zeile und rotiert zur naechsten.

```text
/time set day
/time set night
```

Use 1 -> day  
Use 2 -> night  
Use 3 -> day

## Hinweise

- Dispatch ist clientseitig, Serverrechte gelten trotzdem.
- Eine neue Sequenz ersetzt eine laufende Sequenz.
