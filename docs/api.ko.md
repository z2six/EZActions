# API

이 페이지에는 EZ Actions **2.0.0.0**에 대한 `org.z2six.ezactions.api`의 공개 API가 문서화되어 있습니다.

청중:

- 모드 개발자
- 모드팩 개발자
- 고급 자동화 사용자

## API가 할 수 있는 일

짧은 대답: 사용자가 GUI에서 수행할 수 있는 모든 작업과 추가 런타임 제어가 가능합니다.

- 메뉴 트리 읽기
- 작업 및 번들 추가/업데이트/제거/재주문
- 항목을 잠긴 것으로 표시
- JSON 가져오기/내보내기/검증 메뉴 JSON
- 편집기/구성/방사형 화면 열기
- 번들에서 직접 방사형 열기
- 지속되지 않고 임시 런타임 방사형을 엽니다.
- 트리거 키 입력 및 명령 시퀀스
- 간단한 API 이벤트를 구독하세요

## 입장

```java
import org.z2six.ezactions.api.EzActions;
import org.z2six.ezactions.api.EzActionsApi;

EzActionsApi api = EzActions.get();
```

## Core Concepts

### 메뉴경로

`MenuPath`은 루트에서 **번들 제목 체인**으로 번들을 처리합니다.

```java
MenuPath root = MenuPath.root();
MenuPath p = MenuPath.root().child("Utilities").child("Combat");
```

Notes:

- 경로 일치는 대소문자를 구분하며 제목 리터럴입니다.
- 빈 경로 = 루트 목록.

### 잠긴 항목

`locked=true`은 게임 내 삭제 흐름 및 잠금 인식 API 제거로부터 보호된다는 의미입니다.

잠긴 항목은 수동 JSON 편집을 통해 제거할 수 있습니다.

### 지속성 모델

대부분의 돌연변이 API 호출은 즉시 지속됩니다.

일반적으로 추가 저장 호출이 필요하지 않습니다.

### 스레딩

API 콜백/이벤트는 클라이언트 스레드 사용을 위해 설계되었습니다.

## 작업 유형

### 주요 조치

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

| 방법 | 목적 |
|---|---|
| `openEditor(Screen)` | 게임 내 편집기 화면 열기 |
| `openConfig(Screen)` | 구성 화면 열기 |
| `openRadial()` | 오픈 루트 방사형 |
| `openRadialAtBundle(String)` | 번들 ID에서 방사형 열기 |
| `openTemporaryRadial(String, DynamicRadialStyle)` | JSON에서 일회성 런타임 방사형 열기 |
| `addAction(...)` | 레거시 직접 추가 작업 API |
| `addBundle(...)` | 레거시 직접 추가 번들 API |
| `removeItem(String)` | 레거시 ID로 제거 |
| `moveWithin(String,int,int)` | 상위/루트에서 레거시 이동 |
| `persist()` | 강제 지속 |
| `importFromClipboard()` | GUI 스타일 클립보드 가져오기 |
| `exportToClipboard()` | GUI 스타일 클립보드 내보내기 |
| `menuRead()` | 읽기 전용 표면 |
| `menuWrite()` | 표면 돌연변이 |
| `importExport()` | JSON 가져오기/내보내기 표면 |
| `inputOps()` | 입력 + 명령 도우미 표면 |
| `editorOps()` | UI/런타임 도우미 화면 |
| `events()` | 이벤트 후크 |

## 메뉴읽기

인터페이스: `MenuRead`

- `list(MenuPath path)`
- `findById(String id)`
- `currentPath()`
- `existsPath(MenuPath path)`

### ApiMenuItem 스냅샷 필드

기초:

- `id`, `title`, `note`
- `isCategory`, `typeLabel`
- `iconKind`, `iconId`
- `hideFromMainRadial`, `bundleKeybindEnabled`, `locked`

작업 세부정보:

- `actionType`
- `actionJson`
- 주요 동작: `keyMappingName`, `keyToggle`, `keyMode`
- 명령 동작: `commandRaw`, `commandDelayTicks`, `commandCycleCommands`
- 아이템 장착 액션 : `itemEquipSlotsJson`

## 메뉴쓰기

인터페이스: `MenuWrite`

### 만들다

- `addAction(path, title, note, action, locked)`
- `addAction(path, title, note, icon, action, locked)`
- `addBundle(path, title, note, hideFromMainRadial, bundleKeybindEnabled, locked)`
- `addBundle(path, title, note, icon, hideFromMainRadial, bundleKeybindEnabled, locked)`

### 이동하다

- `moveWithin(path, fromIndex, toIndex)`
- `moveTo(itemId, targetBundle)`

### 제거하다

- `removeFirst(path, predicate)`
- `removeById(id)`

### 업데이트

- `updateMeta(id, titleOrNull, noteOrNull, iconOrNull)`
- `replaceAction(id, action)`
- `setBundleFlags(id, hideFromMainRadial, bundleKeybindEnabled)`
- `setLocked(id, locked)`

### 구조적 도우미

- `ensureBundles(path)`은 제목별로 누락된 번들 체인을 생성합니다.
- `upsertFromJson(path, jsonObjectOrArray)` JSON 스니펫의 항목을 추가/교체합니다.

### 예: 잠긴 유틸리티 번들 생성

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

인터페이스: `ImportExport`

- `exportAllJson()`
- `exportBundleJson(path)`
- `importInto(path, json)`
- `replaceAll(json)`
- `validate(json)`

### 유효성 검사 규칙(상위 수준)

- 루트는 객체 또는 배열이어야 합니다.
- 각 항목에는 다음 중 정확히 하나가 포함되어야 합니다.
  - `action`
  - `children`
- 작업 개체에는 유효한 `type`이 포함되어야 합니다.
- 선택적 부울(`hideFromMainRadial`, `bundleKeybindEnabled`, `locked`)이 있는 경우 부울이어야 합니다.

## 입력 작업

인터페이스: `InputOps`

- `deliver(mappingNameOrLabel, toggle, mode)`
- `enqueueCommands(commands, perLineDelayTicks)`

예:

```java
var input = EzActions.get().inputOps();

input.deliver("key.inventory", false, InputOps.Mode.AUTO);
input.enqueueCommands(new String[]{"/time set day", "/weather clear"}, 10);
```

## EditorOps

인터페이스: `EditorOps`

- `openEditor()`
- `openConfig()`
- `openRadial()`
- `openRadialAtBundle(bundleId)`
- `openTemporaryRadial(jsonItemOrArray, styleOrNull)`

## 동적 임시 방사형 스타일

클래스: `DynamicRadialStyle`

모든 필드는 선택적으로 null을 허용하는 재정의입니다.

그림 물감:

- `ringColor`
- `hoverColor`
- `borderColor`
- `textColor`

생기:

- `animationsEnabled`
- `animOpenClose`
- `animHover`
- `openCloseMs`
- `hoverGrowPct`
- `openStyle` (`WIPE|FADE|NONE`)
- `openDirection` (`CW|CCW`)
- `hoverStyle` (`FILL_SCALE|FILL_ONLY|SCALE_ONLY|NONE`)

설계:

- `deadzone`
- `baseOuterRadius`
- `ringThickness`
- `scaleStartThreshold`
- `scalePerItem`
- `sliceGapDeg`
- `designStyle` (`SOLID|SEGMENTED|OUTLINE|GLASS`)

### 예: 임시 런타임 방사형

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

클래스: `ApiEvents`

- `onMenuChanged(Consumer<MenuChanged>)`
- `onImported(Consumer<ImportEvent>)`

페이로드:

- `MenuChanged.path`, `MenuChanged.reason`
- `ImportEvent.target`, `ImportEvent.json`, `ImportEvent.count`

예:

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

- 작업 항목에는 `action`을 사용합니다.
- 번들에는 `children`을 사용하세요.
- 동일한 객체에 두 가지를 모두 포함하지 마십시오.

## KubeJS 스타일 Interop 패턴

정확한 구문은 KubeJS 설정에 따라 다르지만 일반적으로 흐름은 다음과 같습니다.

1. Java API 클래스 로드
2. `EzActions.get()`을 통해 싱글톤을 가져옵니다.
3. `menuWrite()` / `editorOps()` 메소드 호출

의사 흐름:

```js
// pseudo example, adapt to your exact loader bridge
const EzActions = Java.loadClass('org.z2six.ezactions.api.EzActions')
const api = EzActions.get()

// use api.menuWrite(), api.importExport(), api.editorOps(), etc.
```

## API Notes For Pack Authors

- API가 번들 키 바인딩 활성화를 작성하는 경우에도 사용자는 키 바인딩 등록을 위해 다시 시작해야 합니다.
- EZ Actions는 이제 새 번들 키 바인딩에 다시 시작이 필요할 때 채팅 사용자에게 알립니다.
- 시간이 지남에 따라 메뉴를 패치할 계획이라면 안정적인 ID/제목을 유지하세요.

???+ 경고 "호환성 참고"
    API 서명은 향후 버전에서 변경될 수 있습니다. 이 페이지는 2.0.0.0 동작과 일치합니다.