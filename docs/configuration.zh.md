# 配置

在菜单编辑器点击 `Config` 打开配置界面。

配置界面有三个分区：

- General
- Animations
- Design

## General

| 设置 | 类型 | 范围 / 值 | 默认 |
|---|---|---|---|
| Move While Radial Open | 布尔 | ON/OFF | ON |
| Show Hover Label | 布尔 | ON/OFF | ON |
| Command Editor Visible Lines | 整数 | 1-20 | 5 |

## Animations

| 设置 | 类型 | 范围 / 值 | 默认 |
|---|---|---|---|
| Animations Enabled | 布尔 | ON/OFF | ON |
| Open/Close Animation | 布尔 | ON/OFF | ON |
| Hover Animation | 布尔 | ON/OFF | ON |
| Open Style | 枚举 | `WIPE`, `FADE`, `NONE` | `WIPE` |
| Direction | 枚举 | `CW`, `CCW` | `CW` |
| Hover Style | 枚举 | `FILL_SCALE`, `FILL_ONLY`, `SCALE_ONLY`, `NONE` | `FILL_SCALE` |
| Hover Grow Percent | 小数 | 0.0-0.5 | 0.05 |
| Open/Close Duration | 整数(ms) | 0-2000 | 125 |

## Design

| 设置 | 类型 | 范围 / 值 | 默认 |
|---|---|---|---|
| Deadzone | 整数 | 0-90 | 18 |
| Outer Radius | 整数 | 24-512 | 72 |
| Ring Thickness | 整数 | 6-256 | 28 |
| Scale Start Threshold | 整数 | 0-128 | 8 |
| Scale Per Item | 整数 | 0-100 | 6 |
| Slice Gap | 整数(度) | 0-12 | 0 |
| Design Style | 枚举 | `SOLID`, `SEGMENTED`, `OUTLINE`, `GLASS` | `SOLID` |
| Ring Color | ARGB 整数 | 32 位有符号整数 | `0xAA000000` |
| Hover Color | ARGB 整数 | 32 位有符号整数 | `0xFFF20044` |
| Border Color | ARGB 整数 | 32 位有符号整数 | `0x66FFFFFF` |
| Text Color | ARGB 整数 | 32 位有符号整数 | `0xFFFFFFFF` |

## Preview

`Preview` 会打开径向预览界面，并以 1 秒间隔循环播放开关动画。

可以先调到满意再保存。

## 保存行为

- 点击 Save 会立即写入配置文件。
- 保存后界面会返回父编辑器。

## 配置文件位置

- `config/ezactions/general-client.toml`
- `config/ezactions/anim-client.toml`
- `config/ezactions/design-client.toml`

???+ info "深入说明：颜色格式"
    颜色以 32 位有符号 ARGB 整数存储。

    示例：

    - `0xAARRGGBB`
    - `AA` 是透明度，然后是 `RR`、`GG`、`BB`

    对于不透明颜色，十进制显示为负数是正常现象。
