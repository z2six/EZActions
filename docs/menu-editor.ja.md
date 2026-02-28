# メインメニューエディター GUI

メニューエディターは、Radial を作るための中心画面です。

`Open editor` キーバインドで開きます。

## レイアウト

- **左パネル:** Action / Bundle の追加・編集・削除
- **右パネル:** 現在ページ（Root または現在 Bundle）の一覧
- **左上 Filter:** `Title` / `Note` / Action type で検索
- **左下:** `Import` / `Export` / `Config` / `Close`

## 追加ボタン

- `Add Key Action`
- `Add Command`
- `Add Item Equip`
- `Add Bundle`

## 一覧操作

### マウス

- **LMB:** 選択
- **LMB ドラッグ:** 現在ページ内で並べ替え
- **Bundle 行へドラッグ:** その Bundle に移動
- **Back 行へドラッグ:** Parent / Root へ移動
- **RMB on Bundle:** Bundle を開く

### キーボード

- `Ctrl + F`: Filter にフォーカス
- `Enter`: 選択行を編集
- `Delete` / `Backspace`: 選択行を削除
- `Up Arrow`: 上へ移動
- `Down Arrow`: 下へ移動

!!! tip
    Filter に文字がある間は、矢印での並べ替えは無効です。

## 行タイプ

- 通常行（Action / Bundle）
- パンくず行（`root/.../bundle`）
- `Back to root`
- `Back to <parent>`

## locked エントリ

`locked` の項目はゲーム内削除から保護されます。

- GUI 削除不可
- lock-aware API remove でも削除不可
- `config/ezactions/menu.json` を手動編集すれば削除可能
