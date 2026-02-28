# API

이 페이지는 EZ Actions 2.0.0.0의 공개 API `org.z2six.ezactions.api`를 설명합니다.

## API로 가능한 것

GUI에서 가능한 대부분의 작업 + 런타임 제어:

- 메뉴 트리 조회
- Action/Bundle 추가, 수정, 삭제, 재정렬
- `locked` 설정
- JSON import/export/validate
- editor/config/radial 열기
- 비영속 임시 runtime radial 열기

## Access

```java
import org.z2six.ezactions.api.EzActions;
import org.z2six.ezactions.api.EzActionsApi;

EzActionsApi api = EzActions.get();
```

## Action type

- `KEY`
- `COMMAND`
- `ITEM_EQUIP`

## modpack 참고

API로 Bundle keybind를 켜면 사용자는 재시작이 필요합니다.

## 전체 레퍼런스

완전한 시그니처와 고급 예시는 영어 API 페이지를 참고하세요.
