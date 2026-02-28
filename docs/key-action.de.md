# Schlüsselaktion

Verwenden Sie eine Tastenaktion, um eine vorhandene Tastenkombination (Vanilla oder Modded) vom Radial aus auszulösen.

## Felder

- **Titel**: was Sie im Editor/Radial-Label sehen.
- **Hinweis**: optionaler Hilfstext.
- **Zuordnungsname**: Schlüsselzuordnungs-ID oder -Bezeichnung.
- **Lieferung**: `AUTO`, `INPUT` oder `TICK`.
- **Umschalten**: Tastenstatus umschalten statt antippen.
- **Symbol**: Klicken Sie auf das Symbolfeld, um die Symbolauswahl zu öffnen.

## Empfohlenes Setup

1. Klicken Sie auf `Pick from Keybinds`.
2. Wählen Sie die Bindung im Picker aus (sicherer als von Hand zu tippen).
3. Behalten Sie die Lieferung an `AUTO` bei, es sei denn, Sie haben einen bestimmten Grund.
4. Speichern.

## Liefermodi

| Modus | Was es tut | Wann sollte | verwendet werden?
|---|---|---|
| `AUTO` | Wählt automatisch den besten Pfad | Standard für fast alle |
| `INPUT` | Fügt Tastendruck/-freigabe über die Eingabepipeline ein | Wenn eine Bindung in `AUTO` | nicht antwortet
| `TICK` | Setzt die Taste nach unten/oben über Schlüsselstatusaktualisierungen | Nützlicher Fallback für schwer einzuschleusende Schlüssel |

## Umschalten

- `OFF`: ein Tipp pro radialer Verwendung.
- `ON`: Klappt den Schlüssel bei jeder Verwendung nach unten/oben.

Nützlich für Aktionen wie das Umschalten zwischen Sprint- und Sneak-Stil, je nachdem, wie sich die Zieltastenkombination verhält.

## Häufige Probleme

- **Es passiert nichts:** Überprüfen Sie die Zuordnungs-ID oder wählen Sie erneut aus der Tastenbelegungsliste aus.
- **Falsche Tastenkombination löst aus:** Ihr eingegebener Name stimmte mit einer anderen Zuordnungsbezeichnung überein; Picker verwenden.
- **Funktioniert im Einzelspielermodus, aber nicht auf dem Server:** Die Zielaktion ist möglicherweise serverbeschränkt.

???+ Info „Deep Dive: So funktioniert die Schlüsselübergabe“
    Intern löst EZ Actions Zuordnungsnamen auf wie folgt:

1. Genaue Übereinstimmung des Übersetzungsschlüssels
    2. Genaue lokalisierte Etikettenübereinstimmung
    3. Fallback enthält Übereinstimmung

Für Modifizierer-erforderliche Tasten (Strg/Umschalt/Alt) wird eine zusätzliche Injektionslogik verwendet. `AUTO` kann die Strategie ändern, je nachdem, ob ein Schlüssel gebunden ist, einen Scancode hat oder Modifikatoren erfordert.
