# Bundles

`Bundle` は Radial ツリー内のフォルダ/カテゴリです。

## Bundle フィールド

- **Title**
- **Note**（任意）
- **Icon**
- **Hide from main radial**
- **Enable keybind**

## Bundle keybind

`Enable keybind` を有効にすると、その Bundle 専用 keybind が登録されます。

!!! warning "再起動が必要"
    Bundle keybind の登録はクライアント再起動後に有効になります。

## Hide from main radial

有効時:

- Root からは非表示
- メニューモデルには残る
- API / Bundle keybind ではアクセス可能

## locked Bundle

`locked` はゲーム内削除を防ぎます。

- GUI から削除不可
- JSON 手動編集なら削除可能
