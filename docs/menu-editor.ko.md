# 메인 메뉴 에디터 GUI

메뉴 에디터는 Radial 구성을 위한 핵심 화면입니다.

`Open editor` 키바인드로 엽니다.

## 레이아웃

- **왼쪽 패널:** Action/Bundle 생성, 편집, 삭제
- **오른쪽 패널:** 현재 페이지 목록(Root 또는 현재 Bundle)
- **왼쪽 상단 Filter:** `Title`, `Note`, Action type 기준 필터
- **왼쪽 하단:** `Import`, `Export`, `Config`, `Close`

## 추가 버튼

- `Add Key Action`
- `Add Command`
- `Add Item Equip`
- `Add Bundle`

## 목록 조작

### 마우스

- **LMB:** 선택
- **LMB 드래그:** 현재 페이지 내 재정렬
- **Bundle 행에 드래그:** 해당 Bundle로 이동
- **Back 행에 드래그:** Parent/Root로 이동
- **RMB on Bundle:** Bundle 열기

### 키보드

- `Ctrl + F`: Filter 포커스
- `Enter`: 선택 항목 편집
- `Delete` / `Backspace`: 선택 항목 삭제
- `Up Arrow`: 위로 이동
- `Down Arrow`: 아래로 이동

!!! tip
    Filter에 텍스트가 있을 때는 화살표 재정렬이 비활성화됩니다.

## 행 타입

- 일반 행(Action/Bundle)
- breadcrumb 행(`root/.../bundle`)
- `Back to root`
- `Back to <parent>`

## locked 항목

`locked` 항목은 게임 내 삭제로부터 보호됩니다.

- GUI 삭제 불가
- lock-aware API remove로 삭제 불가
- `config/ezactions/menu.json` 수동 편집으로는 삭제 가능
