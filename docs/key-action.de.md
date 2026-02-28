# Tastenaktion

Eine `Tastenaktion` loest einen vorhandenen Keybind (vanilla oder modded) ueber das Radialmenu aus.

## Felder

- **Title**
- **Note**
- **Mapping Name**
- **Delivery**: `AUTO`, `INPUT`, `TICK`
- **Toggle**
- **Icon**

## Empfohlener Ablauf

1. `Pick from Keybinds` klicken.
2. Binding in der Liste auswaehlen.
3. Standardmaessig `AUTO` verwenden.
4. Speichern.

## Delivery-Modi

| Modus | Funktion | Verwendung |
|---|---|---|
| `AUTO` | Waehlt automatisch den besten Weg | Standard |
| `INPUT` | Injektion von Press/Release in Input-Pipeline | Wenn `AUTO` nicht reagiert |
| `TICK` | Steuert Tastenstatus pro Tick | Fallback |

## Toggle

- `OFF`: ein Tap pro Nutzung.
- `ON`: wechselt zwischen down/up pro Nutzung.

## Haeufige Probleme

- **Keine Reaktion:** Mapping-ID pruefen oder erneut per Picker waehlen.
- **Falscher Keybind:** nicht per Hand raten, Picker nutzen.
- **Singleplayer geht, Server nicht:** serverseitige Regeln/Berechtigungen.
