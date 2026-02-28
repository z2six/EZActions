# Configuration

Menu Editor에서 `Config`를 엽니다.

섹션:

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
- 색상(`Ring/Hover/Border/Text`)은 ARGB int

## Preview

`Preview`는 1초 간격으로 Radial 애니메이션 루프를 보여줍니다.

## Save

- Save 즉시 TOML 파일에 반영
- 저장 후 부모 에디터로 복귀
