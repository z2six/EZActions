# 导入导出

EZ Actions 的导入 / 导出通过剪贴板完成。

## 导出

在菜单编辑器点击 `Export`。

结果：

- 当前完整根树会序列化为 JSON。
- JSON 复制到剪贴板。

## 导入

在菜单编辑器点击 `Import`。

结果：

- 解析并校验剪贴板 JSON。
- 成功后按导入路径添加 / 替换条目。

## 常见错误信息

- 剪贴板为空
- 剪贴板不是 JSON
- 根 JSON 不是数组
- 条目不是对象 / 条目无效

## 实用工作流

1. 先导出当前菜单到文本文件做备份。
2. 在 JSON 里测试修改。
3. 导入。
4. 需要回滚时导入旧备份。

## JSON 结构

顶层支持菜单条目数组（某些 API 路径也支持单对象）。

每个菜单条目必须是以下之一：

- 含 `action` 对象的**动作条目**
- 含 `children` 数组的**组合条目**

### 最小动作示例

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

### 最小组合示例

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

???+ info "深入说明：结构细节"
    - `title` 和 `note` 可用纯字符串或文本组件 JSON。
    - `locked` 可选，默认 `false`。
    - `action.type` 当前支持 `KEY`、`COMMAND`、`ITEM_EQUIP`。
    - `KEY` 字段：`name`、`toggle`、`mode`。
    - `COMMAND` 字段：`command`、`delayTicks`、`cycleCommands`。
    - `ITEM_EQUIP` 字段：`slots` 映射，值为存储物品快照。
