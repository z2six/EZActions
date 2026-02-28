# Command Action

`Command Action` は Radial からコマンドを送信します。

## フィールド

- **Title**
- **Note**
- **Command**（複数行）
- **Multi-command delay (ticks)**
- **Cycle commands (one per use)**
- **Icon**

## Command ボックスのルール

- 1 行 = 1 コマンド
- 先頭 `/` は任意
- 空行は無視

## Delay

`Multi-command delay (ticks)` は、非 Cycle 時の行間隔です。

- `0`: 即時送信
- `>0`: 行ごとに delay

## Cycle commands

有効時は、1 回の use で 1 行だけ送信し、次行へ進みます。

```text
/time set day
/time set night
```

Use 1 -> day  
Use 2 -> night  
Use 3 -> day

## 注意

- dispatch は client-side ですが、実行可否はサーバー権限次第
- 新しいシーケンス開始時、既存キューは置き換え
