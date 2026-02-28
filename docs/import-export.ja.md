# インポートエクスポート

EZ アクションのインポート/エクスポートはクリップボードを通じて機能します。

## ＃＃ 輸出

メニュー エディタで、`Export` をクリックします。

結果：

- 現在の完全なルート ツリーは JSON にシリアル化されます。
- JSON がクリップボードにコピーされます。

## ＃＃ 輸入

メニュー エディタで、`Import` をクリックします。

結果：

- クリップボードの JSON が解析され、検証されます。
- 成功すると、インポートされたエントリはインポート パスごとに追加/置換されます。

## 一般的なエラー メッセージ

- クリップボードが空です
- クリップボードは JSON ではありません
- ルート JSON が配列ではありません
- エントリはオブジェクトではありません/無効です

## 実践的なワークフロー

1. 現在のメニューをバックアップとしてテキスト ファイルにエクスポートします。
2. JSON で編集をテストします。
3. インポートします。
4. 必要に応じて、以前のバックアップをインポートしてロールバックします。

## JSON 形状

トップレベルでは、メニュー項目の配列 (または一部の API パスでは単一項目) がサポートされます。

各メニュー項目は次のいずれかである必要があります。

- `action` オブジェクトを含む **アクション** アイテム
- `children` 配列を含む **バンドル** アイテム

### 最小限のアクションの例

```json
{
  "id": "act_123",
  "title": "Inventory",
  "icon": "minecraft:chest",
  "action": {
    "type": "KEY",
    "name": "key.inventory",
    "toggle": false,
    "mode": "AUTO"
  }
}
```

### Minimal Bundle Example

```json
{
  "id": "bundle_abc",
  "title": "Utilities",
  "icon": "minecraft:shulker_box",
  "hideFromMainRadial": false,
  "bundleKeybindEnabled": true,
  "locked": false,
  "children": []
}
```

???+ info "Deep dive: schema details"
    - `title` and `note` accept plain string or text component JSON.
    - `locked` is optional; defaults false.
    - `action.type` currently supports `KEY`, `COMMAND`, `ITEM_EQUIP`.
    - `KEY` fields: `name`, `toggle`, `mode`.
    - `COMMAND` fields: `command`, `delayTicks`, `cycleCommands`.
    - `ITEM_EQUIP` fields: `slots` map with stored item snapshots.
