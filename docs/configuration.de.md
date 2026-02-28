# Konfiguration

Öffnen Sie die Konfiguration im Menü-Editor mit der Schaltfläche `Config`.

Die Konfigurationsoberfläche besteht aus drei Abschnitten:

- Allgemein
- Animationen
- Design

## Allgemein

| Einstellung | Geben Sie | ein Bereich/Werte | Standard |
|---|---|---|---|
| Bewegen bei radialer Öffnung | boolescher Wert | EIN/AUS | EIN |
| Hover-Label anzeigen | boolescher Wert | EIN/AUS | EIN |
| Sichtbare Linien des Befehlseditors | int | 1-20 | 5 |

## Animationen

| Einstellung | Geben Sie | ein Bereich/Werte | Standard |
|---|---|---|---|
| Animationen aktiviert | boolescher Wert | EIN/AUS | EIN |
| Animation öffnen/schließen | boolescher Wert | EIN/AUS | EIN |
| Hover-Animation | boolescher Wert | EIN/AUS | EIN |
| Offener Stil | Aufzählung | `WIPE`, `FADE`, `NONE` | `WIPE` |
| Richtung | Aufzählung | `CW`, `CCW` | `CW` |
| Hover-Stil | Aufzählung | `FILL_SCALE`, `FILL_ONLY`, `SCALE_ONLY`, `NONE` | `FILL_SCALE` |
| Hover-Wachstumsprozentsatz | doppelt | 0,0-0,5 | 0,05 |
| Öffnungs-/Schließdauer | int (ms) | 0-2000 | 125 |

## Design

| Einstellung | Geben Sie | ein Bereich/Werte | Standard |
|---|---|---|---|
| Deadzone | int | 0-90 | 18 |
| Außenradius | int | 24-512 | 72 |
| Ringstärke | int | 6-256 | 28 |
| Skalierungsstartschwelle | int | 0-128 | 8 |
| Skalierung pro Artikel | int | 0-100 | 6 |
| Slice-Lücke | int (Grad) | 0-12 | 0 |
| Designstil | Aufzählung | `SOLID`, `SEGMENTED`, `OUTLINE`, `GLASS` | `SOLID` |
| Ringfarbe | ARGB int | 32-Bit-Int mit Vorzeichen | `0xAA000000` |
| Hover-Farbe | ARGB int | 32-Bit-Int mit Vorzeichen | `0xFFF20044` |
| Randfarbe | ARGB int | 32-Bit-Int mit Vorzeichen | `0x66FFFFFF` |
| Textfarbe | ARGB int | 32-Bit-Int mit Vorzeichen | `0xFFFFFFFF` |

## Vorschau

`Preview` öffnet einen radialen Vorschaubildschirm und wiederholt die Animation mit einer Pause von 1 Sekunde zwischen den Schleifen.

Verwenden Sie dies, um die visuellen Einstellungen vor dem Commit zu optimieren.

## Verhalten speichern

- Speichern schreibt Werte sofort in Konfigurationsdateien.
- Beim Speichern kehrt der Bildschirm zum übergeordneten Editor-Bildschirm zurück.

## Konfigurationsdateien

- `config/ezactions/general-client.toml`
- `config/ezactions/anim-client.toml`
- `config/ezactions/design-client.toml`

???+ Info „Deep Dive: Farbformat“
    Farben werden als signierte 32-Bit-ARGB-Ints gespeichert.

Beispiel:

- `0xAARRGGBB`
    - Alpha `AA`, dann Rot `RR`, Grün `GG`, Blau `BB`

Negative Dezimalzahlen sind für undurchsichtige ARGB-Werte normal.
