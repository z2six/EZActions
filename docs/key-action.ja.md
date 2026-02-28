# Key Action

`Key Action` は、既存の keybind（vanilla / mod）を Radial から実行します。

## フィールド

- **Title**
- **Note**
- **Mapping Name**
- **Delivery**: `AUTO`, `INPUT`, `TICK`
- **Toggle**
- **Icon**

## 推奨フロー

1. `Pick from Keybinds` を押す
2. 一覧から対象 keybind を選ぶ
3. 基本は `AUTO` を使う
4. Save

## Delivery モード

| Mode | 内容 | 使いどころ |
|---|---|---|
| `AUTO` | 自動で最適な方式を選択 | 通常はこれ |
| `INPUT` | input pipeline に press/release を注入 | `AUTO` が効かない時 |
| `TICK` | tick ベースでキー状態を制御 | フォールバック |

## Toggle

- `OFF`: 1 回押下相当
- `ON`: use ごとに down/up を切り替え

## よくある問題

- **反応しない:** mapping id を確認、または picker で再選択
- **別キーが動く:** 手入力より picker 推奨
- **SP では動くが鯖では動かない:** サーバー権限/制限の影響
