# Configuracion

Abre `Configuracion` desde Menu Editor.

Secciones:

- General
- Animations
- Design

## General

- `Move While Radial Open`
- `Show Hover Label`
- `Command Editor Visible Lines` (1-20)

## Animations

- `Animations Enabled`
- `Open/Close Animation`
- `Hover Animation`
- `Open Style`: `WIPE`, `FADE`, `NONE`
- `Direction`: `CW`, `CCW`
- `Hover Style`: `FILL_SCALE`, `FILL_ONLY`, `SCALE_ONLY`, `NONE`
- `Hover Grow Percent`: 0.0-0.5
- `Open/Close Duration`: 0-2000 ms

## Design

- `Deadzone`
- `Outer Radius`
- `Ring Thickness`
- `Scale Start Threshold`
- `Scale Per Item`
- `Slice Gap`
- `Design Style`: `SOLID`, `SEGMENTED`, `OUTLINE`, `GLASS`
- `Ring Color`, `Hover Color`, `Border Color`, `Text Color` (ARGB int)

## Preview

`Preview` muestra un bucle de animacion del radial con pausa de 1 segundo.

## Guardado

- Save escribe de inmediato en archivos TOML.
- Al guardar, vuelve al editor padre.

Archivos:

- `config/ezactions/general-client.toml`
- `config/ezactions/anim-client.toml`
- `config/ezactions/design-client.toml`
