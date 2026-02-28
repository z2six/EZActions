# API

本页记录 EZ Actions **2.0.0.0** 的公开 API：`org.z2six.ezactions.api`。

面向：

- 模组开发者
- 整合包作者
- 高阶自动化用户

## API 能做什么

简短回答：玩家在 GUI 里能做的几乎都能做，另外还有运行时扩展能力。

- 读取菜单树
- 添加 / 更新 / 删除 / 重排动作与组合
- 标记条目为 locked
- 导入 / 导出 / 校验菜单 JSON
- 打开编辑器 / 配置 / 径向菜单界面
- 直接打开某个组合
- 打开不落盘的临时运行时径向菜单
- 触发按键输入与命令时序
- 订阅 API 事件

## 获取入口

```java
import org.z2six.ezactions.api.EzActions;
import org.z2six.ezactions.api.EzActionsApi;

EzActionsApi api = EzActions.get();
```

## 核心概念

### MenuPath

`MenuPath` 通过**组合标题链**从根路径定位组合。

```java
MenuPath root = MenuPath.root();
MenuPath p = MenuPath.root().child("Utilities").child("Combat");
```

说明：

- 路径匹配区分大小写，按标题字面量匹配。
- 空路径表示根列表。

### Locked 条目

`locked=true` 表示游戏内删除流程和遵守锁定规则的 API 删除流程不会移除该条目。

但仍可通过手动编辑 JSON 删除。

### 持久化模型

大多数修改型 API 调用会立即持久化。

通常不需要额外 save。

### 线程

API 回调 / 事件按客户端线程使用模型设计。

## 动作类型

### Key Action

```java
import org.z2six.ezactions.data.click.ClickActionKey;
import org.z2six.ezactions.helper.InputInjector;

ClickActionKey keyAction = new ClickActionKey(
    "key.inventory",
    false,
    InputInjector.DeliveryMode.AUTO
);
```

### Command Action

```java
import org.z2six.ezactions.data.click.ClickActionCommand;

ClickActionCommand cmd = new ClickActionCommand(
    "/time set day\n/time set night",
    10,
    true
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

## 图标

```java
import org.z2six.ezactions.data.icon.IconSpec;

IconSpec itemIcon = IconSpec.item("minecraft:ender_pearl");
IconSpec customIcon = IconSpec.custom("custom:my_icon");
```

## API 说明建议

- 如果你的 API 写入开启了组合按键，用户仍需重启客户端才能注册该按键。
- EZ Actions 已在需要重启时给出客户端提示。
- 如果你会持续更新菜单，建议保持稳定 ID / 标题以便增量补丁。

???+ warning "兼容性提示"
    API 签名未来可能变化。本页以 2.0.0.0 行为为准。

## 详细接口

完整方法、字段与示例请继续参考英文 API 页（与源码同步最快）：

- `menuRead()` / `menuWrite()`
- `importExport()`
- `inputOps()`
- `editorOps()`
- `events()`

> 为什么中文页保留精简版？
>
> API 在迭代时变化较快。中文页优先提供稳定心智模型，英文页提供完整签名级细节。
