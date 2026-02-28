# Key Action

`Key Action`은 기존 keybind(vanilla/modded)를 Radial에서 실행합니다.

## 필드

- **Title**
- **Note**
- **Mapping Name**
- **Delivery**: `AUTO`, `INPUT`, `TICK`
- **Toggle**
- **Icon**

## 권장 절차

1. `Pick from Keybinds` 클릭
2. 목록에서 대상 keybind 선택
3. 기본값 `AUTO` 유지
4. Save

## Delivery 모드

| Mode | 동작 | 사용 시점 |
|---|---|---|
| `AUTO` | 자동으로 최적 방식 선택 | 기본 권장 |
| `INPUT` | input pipeline에 press/release 주입 | `AUTO` 미동작 시 |
| `TICK` | tick 기반 키 상태 제어 | 폴백 |

## Toggle

- `OFF`: 1회 탭 실행
- `ON`: 사용마다 down/up 토글

## 자주 발생하는 문제

- **반응 없음:** mapping id 확인 또는 picker 재선택
- **다른 키가 실행됨:** 수동 입력 대신 picker 사용
- **싱글은 되는데 서버는 안됨:** 서버 권한/제한 영향
