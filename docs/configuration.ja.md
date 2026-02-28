# Configuration

Menu Editor から `Config` を開きます。

セクション:

- General
- Animations
- Design

## General

- `Move While Radial Open`
- `Show Hover Label`
- `Command Editor Visible Lines`（1-20）

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
- 色: `Ring/Hover/Border/Text`（ARGB int）

## Preview

`Preview` は 1 秒間隔で開閉アニメーションをループ表示します。

## Save

- Save で即時に TOML へ書き込み
- 保存後は親エディターへ戻る
