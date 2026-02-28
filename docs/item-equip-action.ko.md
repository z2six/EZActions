# Item Equip Action

`Item Equip Action`은 장비 스냅샷을 저장하고 인벤토리에서 일치 아이템을 다시 장착합니다.

## 대상 슬롯

- Mainhand
- Offhand
- Helmet
- Chestplate
- Leggings
- Boots

에디터에서 비워둔 슬롯은 실행 시 건너뜁니다.

## 생성 방법

1. `Add Item Equip`
2. `Title`/`Note`/`Icon` 설정
3. `Source Items`에서 `Equip Targets`로 드래그
4. Save

## Source Items 포함 항목

- 현재 offhand
- 착용 방어구
- 메인 인벤토리
- 핫바

## 매칭 규칙

- 완전 스냅샷 매칭(NBT/metadata 포함, count는 무시)
- 다중 매칭 시 count가 가장 큰 스택 우선

## Mainhand 규칙

`Mainhand`는 실행 순간 선택된 핫바 슬롯입니다.

## 실행 중 동작

- 슬롯별 처리(부분 성공 허용)
- 실행 중 다른 Item Equip 실행 시 기존 작업 취소
- 실행 중에도 이동 등 일반 조작 가능
