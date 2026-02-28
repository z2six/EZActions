# Configuration

Открой конфиг через кнопку `Config` в Menu Editor.

Секции:

- General
- Animations
- Design

## General

| Setting | Type | Range / Values | Default |
|---|---|---|---|
| Move While Radial Open | boolean | ON/OFF | ON |
| Show Hover Label | boolean | ON/OFF | ON |
| Command Editor Visible Lines | int | 1-20 | 5 |

## Animations

| Setting | Type | Range / Values | Default |
|---|---|---|---|
| Animations Enabled | boolean | ON/OFF | ON |
| Open/Close Animation | boolean | ON/OFF | ON |
| Hover Animation | boolean | ON/OFF | ON |
| Open Style | enum | `WIPE`, `FADE`, `NONE` | `WIPE` |
| Direction | enum | `CW`, `CCW` | `CW` |
| Hover Style | enum | `FILL_SCALE`, `FILL_ONLY`, `SCALE_ONLY`, `NONE` | `FILL_SCALE` |
| Hover Grow Percent | double | 0.0-0.5 | 0.05 |
| Open/Close Duration | int (ms) | 0-2000 | 125 |

## Design

| Setting | Type | Range / Values | Default |
|---|---|---|---|
| Deadzone | int | 0-90 | 18 |
| Outer Radius | int | 24-512 | 72 |
| Ring Thickness | int | 6-256 | 28 |
| Scale Start Threshold | int | 0-128 | 8 |
| Scale Per Item | int | 0-100 | 6 |
| Slice Gap | int (deg) | 0-12 | 0 |
| Design Style | enum | `SOLID`, `SEGMENTED`, `OUTLINE`, `GLASS` | `SOLID` |
| Ring Color | ARGB int | signed 32-bit | `0xAA000000` |
| Hover Color | ARGB int | signed 32-bit | `0xFFF20044` |
| Border Color | ARGB int | signed 32-bit | `0x66FFFFFF` |
| Text Color | ARGB int | signed 32-bit | `0xFFFFFFFF` |

## Preview

`Preview` открывает экран предпросмотра радиала и циклически показывает анимацию с паузой 1 сек.

## Save

- Save сразу пишет значения в конфиг-файлы.
- После сохранения экран возвращается в родительский editor.

## Файлы

- `config/ezactions/general-client.toml`
- `config/ezactions/anim-client.toml`
- `config/ezactions/design-client.toml`
