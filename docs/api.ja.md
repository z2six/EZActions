# API

このページでは、EZ Actions **2.0.0.0** の `org.z2six.ezactions.api` のパブリック API について説明します。

観客：

- MOD開発者
- modpack開発者
- 高度な自動化ユーザー

## API でできること

短い答え: ユーザーが GUI で実行できるすべての機能に加えて、追加のランタイム制御。

- メニューツリーの読み取り
- アクションとバンドルの追加/更新/削除/並べ替え
- エントリをロック済みとしてマークする
- メニューJSONのインポート/エクスポート/検証
- エディター/構成/ラジアル画面を開く
- バンドルで直接放射状に開きます
- 永続化せずに一時的なランタイムラジアルを開く
- トリガーキー入力とコマンドシーケンス
- 単純な API イベントをサブスクライブする

＃＃ アクセス

```java
import org.z2six.ezactions.api.EzActions;
import org.z2six.ezactions.api.EzActionsApi;

EzActionsApi api = EzActions.get();
```

## Core Concepts

### メニューパス

`MenuPath` は、ルートから **バンドル タイトル チェーン** によってバンドルをアドレス指定します。

```java
MenuPath root = MenuPath.root();
MenuPath p = MenuPath.root().child("Utilities").child("Combat");
```

Notes:

- パスの一致では大文字と小文字が区別され、タイトル リテラルが区別されます。
- 空のパス = ルート リスト。

### ロックされたエントリ

`locked=true` は、ゲーム内の削除フローとロック対応 API の削除から保護されていることを意味します。

ロックされたエントリは、JSON を手動で編集することで削除できます。

### 永続性モデル

ほとんどの変更可能な API 呼び出しはすぐに持続します。

通常、追加の保存呼び出しは必要ありません。

### ねじ切り

API コールバック/イベントは、クライアント スレッドの使用のために設計されています。

## アクションの種類

### キーアクション

```java
import org.z2six.ezactions.data.click.ClickActionKey;
import org.z2six.ezactions.helper.InputInjector;

ClickActionKey keyAction = new ClickActionKey(
    "key.inventory",                    // mapping id or label
    false,                              // toggle
    InputInjector.DeliveryMode.AUTO     // AUTO/INPUT/TICK
);
```

### Command Action

```java
import org.z2six.ezactions.data.click.ClickActionCommand;

ClickActionCommand cmd = new ClickActionCommand(
    "/time set day\n/time set night", // multi-line
    10,                                 // delay ticks between lines
    true                                // cycleCommands
);
```

### Item Equip Action

```java
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import org.z2six.ezactions.data.click.ClickActionItemEquip;

HolderLookup.Provider regs = Minecraft.getInstance().player.level().registryAccess();
ItemStack stack = Minecraft.getInstance().player.getMainHandItem();

ClickActionItemEquip.StoredItem stored =
    ClickActionItemEquip.StoredItem.fromStack(stack, regs);

ClickActionItemEquip eq = new ClickActionItemEquip(
    java.util.Map.of(ClickActionItemEquip.TargetSlot.MAINHAND, stored)
);
```

## Icons

```java
import org.z2six.ezactions.data.icon.IconSpec;

IconSpec itemIcon = IconSpec.item("minecraft:ender_pearl");
IconSpec customIcon = IconSpec.custom("custom:my_icon");
```

## Top-Level EzActionsApi

|方法 |目的 |
|---|---|
| `openEditor(Screen)` |ゲーム内エディタ画面を開く |
| `openConfig(Screen)` |設定画面を開く |
| `openRadial()` |放射状に開いた根元 |
| `openRadialAtBundle(String)` |バンドル ID でラジアルを開く |
| `openTemporaryRadial(String, DynamicRadialStyle)` | JSON から 1 回限りのランタイム ラジアルを開く |
| `addAction(...)` |従来の直接追加アクション API |
| `addBundle(...)` |従来の直接追加バンドル API |
| `removeItem(String)` |従来の ID による削除 |
| `moveWithin(String,int,int)` |親/ルートでのレガシー移動 |
| `persist()` |強制的に永続化する |
| `importFromClipboard()` | GUI スタイルのクリップボードのインポート |
| `exportToClipboard()` | GUI スタイルのクリップボード エクスポート |
| `menuRead()` |読み取り専用サーフェス |
| `menuWrite()` |変化する表面 |
| `importExport()` | JSON インポート/エクスポート サーフェス |
| `inputOps()` |入力 + コマンド ヘルパー サーフェス |
| `editorOps()` | UI/ランタイムヘルパーサーフェス |
| `events()` |イベントフック |

## メニュー読み取り

インターフェース: `MenuRead`

- `list(MenuPath path)`
- `findById(String id)`
- `currentPath()`
- @@コード3@@

### ApiMenuItem スナップショット フィールド

基本:

- `id`、`title`、`note`
- `isCategory`、`typeLabel`
- `iconKind`、`iconId`
- `hideFromMainRadial`、`bundleKeybindEnabled`、`locked`

アクションの詳細:

- `actionType`
- `actionJson`
- キーアクション: `keyMappingName`、`keyToggle`、`keyMode`
- コマンドアクション: `commandRaw`、`commandDelayTicks`、`commandCycleCommands`
- アイテム装備アクション: `itemEquipSlotsJson`

## メニュー書き込み

インターフェース: `MenuWrite`

＃＃＃ 作成する

- `addAction(path, title, note, action, locked)`
- `addAction(path, title, note, icon, action, locked)`
- `addBundle(path, title, note, hideFromMainRadial, bundleKeybindEnabled, locked)`
- @@コード3@@

＃＃＃ 動く

- `moveWithin(path, fromIndex, toIndex)`
- `moveTo(itemId, targetBundle)`

＃＃＃ 取り除く

- `removeFirst(path, predicate)`
- `removeById(id)`

＃＃＃ アップデート

- `updateMeta(id, titleOrNull, noteOrNull, iconOrNull)`
- `replaceAction(id, action)`
- `setBundleFlags(id, hideFromMainRadial, bundleKeybindEnabled)`
- @@コード3@@

### 構造ヘルパー

- `ensureBundles(path)` はタイトルごとに不足しているバンドル チェーンを作成します。
- `upsertFromJson(path, jsonObjectOrArray)` JSON スニペットから項目を追加/置換します。

### 例: ロックされたユーティリティ バンドルの作成

```java
var write = EzActions.get().menuWrite();

MenuPath root = MenuPath.root();
String bundleId = write.addBundle(
    root,
    "Utilities",
    "Pack-defined utilities",
    IconSpec.item("minecraft:shulker_box"),
    false,   // hideFromMainRadial
    true,    // bundleKeybindEnabled
    true     // locked
).orElseThrow();

write.addAction(
    root.child("Utilities"),
    "Open Inventory",
    "Quick inventory",
    IconSpec.item("minecraft:chest"),
    new ClickActionKey("key.inventory", false, InputInjector.DeliveryMode.AUTO),
    true
);
```

## ImportExport

インターフェース: `ImportExport`

- `exportAllJson()`
- `exportBundleJson(path)`
- `importInto(path, json)`
- @@コード3@@
- @@コード4@@

### 検証ルール (高レベル)

- ルートはオブジェクトまたは配列でなければなりません
- 各項目には次のいずれか 1 つが正確に含まれている必要があります。
  - `action`
  - `children`
- アクション オブジェクトには有効な `type` が含まれている必要があります
- オプションのブール値 (`hideFromMainRadial`、`bundleKeybindEnabled`、`locked`) が存在する場合はブール値である必要があります

## 入力オペレーション

インターフェース: `InputOps`

- `deliver(mappingNameOrLabel, toggle, mode)`
- `enqueueCommands(commands, perLineDelayTicks)`

例：

```java
var input = EzActions.get().inputOps();

input.deliver("key.inventory", false, InputOps.Mode.AUTO);
input.enqueueCommands(new String[]{"/time set day", "/weather clear"}, 10);
```

## EditorOps

インターフェース: `EditorOps`

- `openEditor()`
- `openConfig()`
- `openRadial()`
- @@コード3@@
- @@コード4@@

## 動的一時放射状スタイル

クラス: `DynamicRadialStyle`

すべてのフィールドはオプションの null 許容オーバーライドです。

色：

- `ringColor`
- `hoverColor`
- `borderColor`
- @@コード3@@

アニメーション：

- `animationsEnabled`
- `animOpenClose`
- `animHover`
- @@コード3@@
- @@コード4@@
- `openStyle` (`WIPE|FADE|NONE`)
- `openDirection` (`CW|CCW`)
- `hoverStyle` (`FILL_SCALE|FILL_ONLY|SCALE_ONLY|NONE`)

デザイン：

- `deadzone`
- `baseOuterRadius`
- `ringThickness`
- @@コード3@@
- @@コード4@@
- `sliceGapDeg`
- `designStyle` (`SOLID|SEGMENTED|OUTLINE|GLASS`)

### 例: 一時ランタイム ラジアル

```java
String json = """
[
  {
    "id": "tmp_inv",
    "title": "Inventory",
    "icon": "minecraft:chest",
    "action": { "type": "KEY", "name": "key.inventory", "toggle": false, "mode": "AUTO" }
  },
  {
    "id": "tmp_day",
    "title": "Day",
    "icon": "minecraft:sunflower",
    "action": { "type": "COMMAND", "command": "/time set day", "delayTicks": 0, "cycleCommands": false }
  }
]
""";

DynamicRadialStyle style = new DynamicRadialStyle(
    0xAA000000, 0xFFF20044, 0x66FFFFFF, 0xFFFFFFFF,
    true, true, true,
    125, 0.05,
    "WIPE", "CW", "FILL_SCALE",
    18, 72, 28,
    8, 6, 0,
    "SOLID"
);

EzActions.get().editorOps().openTemporaryRadial(json, style);
```

## Events

クラス: `ApiEvents`

- `onMenuChanged(Consumer<MenuChanged>)`
- `onImported(Consumer<ImportEvent>)`

ペイロード:

- `MenuChanged.path`、`MenuChanged.reason`
- `ImportEvent.target`、`ImportEvent.json`、`ImportEvent.count`

例：

```java
var events = EzActions.get().events();

events.onMenuChanged(evt -> {
    System.out.println("Menu changed: " + evt.reason + " at " + evt.path);
});

events.onImported(evt -> {
    System.out.println("Imported " + evt.count + " entries into " + evt.target);
});
```

## JSON Item Schema (API + Import/Export)

```json
{
  "id": "string",
  "title": "string or text component",
  "note": "string or text component",
  "icon": "minecraft:item_id",
  "hideFromMainRadial": false,
  "bundleKeybindEnabled": false,
  "locked": false,
  "action": {
    "type": "KEY | COMMAND | ITEM_EQUIP"
  },
  "children": []
}
```

Rules:

- アクション項目には `action` を使用します
- バンドルには `children` を使用します
- 同じオブジェクトに両方を含めないでください

## KubeJS スタイルの相互運用パターン

正確な構文は KubeJS の設定によって異なりますが、通常、フローは次のようになります。

1. Java APIクラスをロードする
2. `EzActions.get()` 経由でシングルトンを取得します
3. `menuWrite()` / `editorOps()` メソッドを呼び出す

疑似フロー:

```js
// pseudo example, adapt to your exact loader bridge
const EzActions = Java.loadClass('org.z2six.ezactions.api.EzActions')
const api = EzActions.get()

// use api.menuWrite(), api.importExport(), api.editorOps(), etc.
```

## API Notes For Pack Authors

- API がバンドル キーバインドを有効にすると書き込んだ場合でも、ユーザーはキーバインドを登録するために再起動する必要があります。
- 新しいバンドル キーバインドに再起動が必要な場合、EZ アクションはチャットでユーザーに通知するようになりました。
- 時間の経過とともにメニューにパッチを適用する予定がある場合は、安定した ID/タイトルを維持してください。

???+ 警告「互換性に関する注意事項」
    API 署名は将来のバージョンで変更される可能性があります。このページは 2.0.0.0 の動作と一致します。