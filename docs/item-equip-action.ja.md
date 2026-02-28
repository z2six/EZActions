# Item Equip Action

`Item Equip Action` は装備スナップショットを保存し、一致するアイテムをインベントリから再装備します。

## 対象スロット

- Mainhand
- Offhand
- Helmet
- Chestplate
- Leggings
- Boots

エディターで空のスロットは実行時にスキップされます。

## 作成手順

1. `Add Item Equip`
2. `Title` / `Note` / `Icon` を設定
3. `Source Items` から `Equip Targets` へドラッグ
4. Save

## Source Items に含まれるもの

- 現在 offhand
- 装備中防具
- メインインベントリ
- ホットバー

## マッチング規則

- 完全スナップショット一致（NBT / metadata 含む、count は無視）
- 複数一致時は count 最大のスタックを優先

## Mainhand の意味

`Mainhand` は実行時に選択中のホットバースロットです。

## 実行中の挙動

- スロット単位で処理（部分成功あり）
- 実行中に別 Item Equip を起動すると前の処理をキャンセル
- 移動などの通常操作は継続可能
