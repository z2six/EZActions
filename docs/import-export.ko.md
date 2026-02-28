# 수입 수출

EZ Actions 가져오기/내보내기는 클립보드를 통해 작동합니다.

## 내보내다

메뉴 편집기에서 `Export`을 클릭합니다.

결과:

- 현재 전체 루트 트리는 JSON으로 직렬화됩니다.
- JSON이 클립보드에 복사됩니다.

## 가져오기

메뉴 편집기에서 `Import`을 클릭합니다.

결과:

- 클립보드 JSON이 구문 분석되고 검증됩니다.
- 성공하면 가져온 항목이 가져오기 경로별로 추가/교체됩니다.

## 일반적인 오류 메시지

- 클립보드가 비어 있습니다.
- 클립보드가 JSON이 아닙니다.
- 루트 JSON은 배열이 아닙니다.
- 항목이 개체가 아니거나 유효하지 않습니다.

## 실용적인 작업 흐름

1. 현재 메뉴를 텍스트 파일로 백업으로 내보냅니다.
2. JSON에서 편집 내용을 테스트합니다.
3. 수입.
4. 필요한 경우 이전 백업을 가져와서 롤백합니다.

## JSON 형태

최상위 수준은 메뉴 항목 배열(또는 일부 API 경로의 단일 항목)을 지원합니다.

각 메뉴 항목은 다음 중 하나여야 합니다.

- `action` 개체가 포함된 **작업** 항목
- `children` 배열이 포함된 **번들** 항목

### 최소한의 조치 예

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
