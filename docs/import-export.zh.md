# 导入导出

EZ Actions 导入/导出通过剪贴板进行。

## ＃＃ 出口

在菜单编辑器中，单击`Export`。

结果：

- 当前完整根树被序列化为 JSON。
- JSON 被复制到剪贴板。

## ＃＃ 进口

在菜单编辑器中，单击`Import`。

结果：

- 剪贴板 JSON 已解析和验证。
- 成功后，将按导入路径添加/替换导入的条目。

## 常见错误消息

- 剪贴板为空
- 剪贴板不是 JSON
- 根 JSON 不是数组
- 条目不是对象/无效

## 实际工作流程

1. 将当前菜单导出到文本文件作为备份。
2. 在 JSON 中测试编辑。
3. 导入。
4. 如果需要，通过导入以前的备份进行回滚。

## JSON 形状

顶级支持菜单项数组（或某些 API 路径中的单个项目）。

每个菜单项必须是：

- 带有`action`对象的**操作**项目
- 带有 `children` 数组的 **bundle** 项目

### 最小操作示例

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
