# Bundle

Ein `Bundle` ist ein Ordner/Kategorie im Radialbaum.

## Bundle-Felder

- **Title**
- **Note** (optional)
- **Icon**
- **Hide from main radial**
- **Enable keybind**

## Bundle-Keybind

Bei aktivem `Enable keybind` registriert EZ Actions einen eigenen Keybind fuer das Bundle.

!!! warning "Neustart erforderlich"
    Die Registrierung wird erst nach Client-Neustart wirksam.

## Hide from main radial

Wenn aktiv:

- Bundle wird auf der Root-Seite ausgeblendet.
- Bundle bleibt im Menumodell erhalten.
- Bundle ist weiter ueber API oder Bundle-Keybind erreichbar.

## Gesperrte Bundle (locked)

`locked` schuetzt vor Ingame-Loeschung.

- Nicht per GUI loeschbar.
- Manuell per JSON weiterhin entfernbar.
