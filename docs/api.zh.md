# API

本页记录了 EZ Actions **2.0.0.0** 的 `org.z2six.ezactions.api` 中的公共 API。

观众：

- 模组开发者
- 模组包开发商
- 高级自动化用户

## API 可以做什么

简短的回答：用户可以在 GUI 中执行的所有操作，以及额外的运行时控制。

- 读取菜单树
- 添加/更新/删除/重新排序操作和捆绑包
- 将条目标记为锁定
- 导入/导出/验证菜单 JSON
- 打开编辑器/配置/径向屏幕
- 直接在束处打开径向
- 打开临时运行时径向线而不保留
- 触发按键输入和命令排序
- 订阅简单的API事件

＃＃ 使用权

```java
import org.z2six.ezactions.api.EzActions;
import org.z2six.ezactions.api.EzActionsApi;

EzActionsApi api = EzActions.get();
```

## Core Concepts

### 菜单路径

`MenuPath` 通过从根开始的**捆绑包标题链**来寻址捆绑包。

```java
MenuPath root = MenuPath.root();
MenuPath p = MenuPath.root().child("Utilities").child("Combat");
```

Notes:

- 路径匹配区分大小写和标题文字。
- 空路径=根列表。

### 锁定条目

`locked=true` 表示免受游戏内删除流和锁定感知 API 删除的影响。

锁定的条目仍然可以通过手动 JSON 编辑来删除。

### 持久化模型

大多数变异 API 调用会立即持续。

您通常不需要额外的保存调用。

### 线程

API 回调/事件专为客户端线程使用而设计。

## 动作类型

### 关键行动

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

|方法|目的|
|---|---|
| `openEditor(Screen)` |打开游戏编辑器屏幕 |
| @@代码1@@ |打开配置屏幕 |
| `openRadial()` |开根径向|
| `openRadialAtBundle(String)` |束 id | 处打开径向
| `openTemporaryRadial(String, DynamicRadialStyle)` |从 JSON 打开一次性运行时径向 |
| `addAction(...)` |旧版直接添加操作 API |
| `addBundle(...)` |旧版直接添加捆绑 API |
| `removeItem(String)` |旧版按 ID 删除 |
| `moveWithin(String,int,int)` |父/根中的旧移动 |
| `persist()` |力量坚持 |
| `importFromClipboard()` | GUI 式剪贴板导入 |
| `exportToClipboard()` | GUI 式剪贴板导出 |
| `menuRead()` |只读表面 |
| `menuWrite()` |突变面 |
| `importExport()` | JSON 导入/导出表面 |
| `inputOps()` |输入+命令辅助界面|
| `editorOps()` | UI/运行时辅助界面 |
| `events()` |事件挂钩 |

## 菜单阅读

接口：`MenuRead`

-@@代码0@@
-@@代码1@@
-@@代码2@@
-`existsPath(MenuPath path)`

### ApiMenuItem 快照字段

基础知识：

-`id`、`title`、`note`
-`isCategory`、`typeLabel`
-`iconKind`、`iconId`
-`hideFromMainRadial`、`bundleKeybindEnabled`、`locked`

动作细节：

-@@代码0@@
-@@代码1@@
- 关键操作：`keyMappingName`、`keyToggle`、`keyMode`
- 命令动作：`commandRaw`、`commandDelayTicks`、`commandCycleCommands`
- 物品装备动作：`itemEquipSlotsJson`

## 菜单写入

接口：`MenuWrite`

＃＃＃ 创造

-@@代码0@@
-@@代码1@@
-@@代码2@@
-`addBundle(path, title, note, icon, hideFromMainRadial, bundleKeybindEnabled, locked)`

＃＃＃ 移动

-@@代码0@@
-@@代码1@@

＃＃＃ 消除

-@@代码0@@
-@@代码1@@

＃＃＃ 更新

-@@代码0@@
-@@代码1@@
-@@代码2@@
-`setLocked(id, locked)`

### 结构助手

- `ensureBundles(path)` 按标题创建缺失的捆绑包链。
- `upsertFromJson(path, jsonObjectOrArray)` 添加/替换 JSON 片段中的项目。

### 示例：创建锁定的实用程序包

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

接口：`ImportExport`

-@@代码0@@
-@@代码1@@
-@@代码2@@
-`replaceAll(json)`
-`validate(json)`

### 验证规则（高级）

- 根必须是对象或数组
- 每件物品必须包含以下各项之一：
  -@@代码0@@
  -@@代码1@@
- 操作对象必须包含有效的`type`
- 可选布尔值（`hideFromMainRadial`、`bundleKeybindEnabled`、`locked`）在存在时必须是布尔值

## 输入操作

接口：`InputOps`

-@@代码0@@
-@@代码1@@

例子：

```java
var input = EzActions.get().inputOps();

input.deliver("key.inventory", false, InputOps.Mode.AUTO);
input.enqueueCommands(new String[]{"/time set day", "/weather clear"}, 10);
```

## EditorOps

接口：`EditorOps`

-@@代码0@@
-@@代码1@@
-@@代码2@@
-`openRadialAtBundle(bundleId)`
-`openTemporaryRadial(jsonItemOrArray, styleOrNull)`

## 动态临时径向样式

类：`DynamicRadialStyle`

所有字段都是可选的可为空的覆盖。

颜色：

-@@代码0@@
-@@代码1@@
-@@代码2@@
-`textColor`

动画片：

-@@代码0@@
-@@代码1@@
-@@代码2@@
-`openCloseMs`
-`hoverGrowPct`
- `openStyle` (`WIPE|FADE|NONE`)
- `openDirection` (`CW|CCW`)
- `hoverStyle` (`FILL_SCALE|FILL_ONLY|SCALE_ONLY|NONE`)

设计：

-@@代码0@@
-@@代码1@@
-@@代码2@@
-`scaleStartThreshold`
-`scalePerItem`
-`sliceGapDeg`
- `designStyle` (`SOLID|SEGMENTED|OUTLINE|GLASS`)

### 示例：临时运行时径向

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

类：`ApiEvents`

-@@代码0@@
-@@代码1@@

有效负载：

-`MenuChanged.path`、`MenuChanged.reason`
- `ImportEvent.target`、`ImportEvent.json`、`ImportEvent.count`

例子：

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

- 使用`action`作为操作项
- 对捆绑包使用`children`
- 不要将两者包含在同一个对象中

## KubeJS 风格的互操作模式

确切的语法取决于您的 KubeJS 设置，但流程通常是：

1.加载Java API类
2.通过`EzActions.get()`获取单例
3. 调用`menuWrite()` / `editorOps()`方法

伪流：

```js
// pseudo example, adapt to your exact loader bridge
const EzActions = Java.loadClass('org.z2six.ezactions.api.EzActions')
const api = EzActions.get()

// use api.menuWrite(), api.importExport(), api.editorOps(), etc.
```

## API Notes For Pack Authors

- 如果您的 API 写入启用捆绑键绑定，用户仍然需要重新启动才能注册键绑定。
- 当新的捆绑包密钥绑定需要重新启动时，EZ Actions 现在会在聊天中通知用户。
- 如果您计划随着时间的推移修补菜单，请保持稳定的 ID/标题。

???+警告“兼容性说明”
    API 签名可能会在未来版本中更改。此页面符合 2.0.0.0 行为。