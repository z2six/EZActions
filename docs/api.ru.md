# API

На этой странице описан общедоступный API в `org.z2six.ezactions.api` для EZ Actions **2.0.0.0**.

Аудитория:

- разработчики модов
- разработчики модпаков
- продвинутые пользователи автоматизации

## Что может API

Короткий ответ: все, что пользователи могут делать в графическом интерфейсе, плюс дополнительный контроль во время выполнения.

- Читать дерево меню
- Добавить/обновить/удалить/изменить порядок действий и пакетов.
- Отметить записи как заблокированные
- Импорт/экспорт/проверка меню JSON
- Открыть редактор/конфигурацию/радиальные экраны
- Открыть радиально непосредственно на связке
- Открытие временных радиалов времени выполнения без сохранения
- Ввод клавиш триггера и последовательность команд
- Подпишитесь на простые события API

## Доступ

```java
import org.z2six.ezactions.api.EzActions;
import org.z2six.ezactions.api.EzActionsApi;

EzActionsApi api = EzActions.get();
```

## Core Concepts

###МенюПуть

`MenuPath` адресует пакеты по **цепочке заголовков** от корня.

```java
MenuPath root = MenuPath.root();
MenuPath p = MenuPath.root().child("Utilities").child("Combat");
```

Notes:

- Сопоставление пути учитывает регистр и является литеральным по заголовку.
- пустой путь = корневой список.

### Заблокированные записи

`locked=true` означает защиту от внутриигровых потоков удаления и удаления API с поддержкой блокировки.

Заблокированные записи по-прежнему можно удалить путем редактирования JSON вручную.

### Модель персистентности

Большинство изменяющихся вызовов API сохраняются немедленно.

Обычно вам не нужен дополнительный вызов сохранения.

### Потоки

Обратные вызовы/события API предназначены для использования клиентских потоков.

## Типы действий

### Ключевое действие

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

| Метод | Цель |
|---|---|
| @@КОД0@@ | Открыть экран внутриигрового редактора |
| `openConfig(Screen)` | Открыть экран конфигурации |
| `openRadial()` | Открытый корень радиальный |
| `openRadialAtBundle(String)` | Открыть радиальную по идентификатору пакета |
| `openTemporaryRadial(String, DynamicRadialStyle)` | Открыть одноразовую радиальную среду выполнения из JSON |
| `addAction(...)` | Устаревший API действия прямого добавления |
| `addBundle(...)` | Устаревший API прямого добавления пакета |
| `removeItem(String)` | Legacy удалить по id |
| `moveWithin(String,int,int)` | Унаследованный перенос в родительский/корневой каталог |
| `persist()` | Сила сохраняется |
| `importFromClipboard()` | Импорт буфера обмена в стиле графического интерфейса |
| `exportToClipboard()` | Экспорт из буфера обмена в стиле графического интерфейса |
| `menuRead()` | Поверхность только для чтения |
| `menuWrite()` | Мутирующая поверхность |
| `importExport()` | поверхность импорта/экспорта JSON |
| `inputOps()` | Вспомогательная поверхность ввода + команды |
| `editorOps()` | Вспомогательная поверхность пользовательского интерфейса/среды выполнения |
| `events()` | Крючки событий |

## МенюЧтение

Интерфейс: `MenuRead`

- `list(MenuPath path)`
- `findById(String id)`
- `currentPath()`
- `existsPath(MenuPath path)`

### Поля снимка ApiMenuItem

Основы:

- `id`, `title`, `note`
- `isCategory`, `typeLabel`
- `iconKind`, `iconId`
- `hideFromMainRadial`, `bundleKeybindEnabled`, `locked`

Детали действия:

- `actionType`
- `actionJson`
- ключевое действие: `keyMappingName`, `keyToggle`, `keyMode`
- действие команды: `commandRaw`, `commandDelayTicks`, `commandCycleCommands`
- действие экипировки предмета: `itemEquipSlotsJson`

## МенюЗапись

Интерфейс: `MenuWrite`

### Создавать

- `addAction(path, title, note, action, locked)`
- `addAction(path, title, note, icon, action, locked)`
- `addBundle(path, title, note, hideFromMainRadial, bundleKeybindEnabled, locked)`
- `addBundle(path, title, note, icon, hideFromMainRadial, bundleKeybindEnabled, locked)`

### Двигаться

- `moveWithin(path, fromIndex, toIndex)`
- `moveTo(itemId, targetBundle)`

### Удалять

- `removeFirst(path, predicate)`
- `removeById(id)`

### Обновлять

- `updateMeta(id, titleOrNull, noteOrNull, iconOrNull)`
- `replaceAction(id, action)`
- `setBundleFlags(id, hideFromMainRadial, bundleKeybindEnabled)`
- `setLocked(id, locked)`

### Структурные помощники

- `ensureBundles(path)` создает недостающую цепочку пакетов по заголовку.
- `upsertFromJson(path, jsonObjectOrArray)` добавить/заменить элементы из фрагмента JSON.

### Пример: создание заблокированного пакета утилит

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

Интерфейс: `ImportExport`

- `exportAllJson()`
- `exportBundleJson(path)`
- `importInto(path, json)`
- `replaceAll(json)`
- `validate(json)`

### Правила проверки (высокий уровень)

- корень должен быть объектом или массивом
- каждый элемент должен содержать ровно одно из:
  - `action`
  - `children`
- объект действия должен содержать действительный `type`
- необязательные логические значения (`hideFromMainRadial`, `bundleKeybindEnabled`, `locked`) должны быть логическими значениями, если они присутствуют.

## Операции ввода

Интерфейс: `InputOps`

- `deliver(mappingNameOrLabel, toggle, mode)`
- `enqueueCommands(commands, perLineDelayTicks)`

Пример:

```java
var input = EzActions.get().inputOps();

input.deliver("key.inventory", false, InputOps.Mode.AUTO);
input.enqueueCommands(new String[]{"/time set day", "/weather clear"}, 10);
```

## EditorOps

Интерфейс: `EditorOps`

- `openEditor()`
- `openConfig()`
- `openRadial()`
- `openRadialAtBundle(bundleId)`
- `openTemporaryRadial(jsonItemOrArray, styleOrNull)`

## Динамический временный радиальный стиль

Класс: `DynamicRadialStyle`

Все поля являются необязательными переопределениями, допускающими значение NULL.

Цвета:

- `ringColor`
- `hoverColor`
- `borderColor`
- `textColor`

Анимация:

- `animationsEnabled`
- `animOpenClose`
- `animHover`
- `openCloseMs`
- `hoverGrowPct`
- `openStyle` (`WIPE|FADE|NONE`)
- `openDirection` (`CW|CCW`)
- `hoverStyle` (`FILL_SCALE|FILL_ONLY|SCALE_ONLY|NONE`)

Дизайн:

- `deadzone`
- `baseOuterRadius`
- `ringThickness`
- `scaleStartThreshold`
- `scalePerItem`
- `sliceGapDeg`
- `designStyle` (`SOLID|SEGMENTED|OUTLINE|GLASS`)

### Пример: временный радиус времени выполнения

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

Класс: `ApiEvents`

- `onMenuChanged(Consumer<MenuChanged>)`
- `onImported(Consumer<ImportEvent>)`

Полезная нагрузка:

- `MenuChanged.path`, `MenuChanged.reason`
- `ImportEvent.target`, `ImportEvent.json`, `ImportEvent.count`

Пример:

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

- используйте `action` для действий
- используйте `children` для пакетов
- не включайте оба в один и тот же объект

## Шаблон взаимодействия в стиле KubeJS

Точный синтаксис зависит от вашей настройки KubeJS, но обычно порядок действий следующий:

1. загрузить класс Java API
2. получить синглтон через `EzActions.get()`
3. вызвать методы `menuWrite()` / `editorOps()`

Псевдопоток:

```js
// pseudo example, adapt to your exact loader bridge
const EzActions = Java.loadClass('org.z2six.ezactions.api.EzActions')
const api = EzActions.get()

// use api.menuWrite(), api.importExport(), api.editorOps(), etc.
```

## API Notes For Pack Authors

- Если ваш API пишет, что необходимо включить привязки клавиш пакета, пользователям все равно потребуется перезагрузка для регистрации привязки клавиш.
- EZ Actions теперь уведомляет пользователей в чате, когда для новых сочетаний клавиш пакета требуется перезагрузка.
- Сохраняйте стабильные идентификаторы/заголовки, если вы планируете со временем обновлять меню.

???+ предупреждение «Примечание о совместимости»
    Подписи API могут измениться в будущих версиях. Эта страница соответствует поведению версии 2.0.0.0.