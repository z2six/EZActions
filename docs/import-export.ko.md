# Import / Export

EZ Actions의 Import/Export는 클립보드를 사용합니다.

## Export

Menu Editor에서 `Export` 클릭.

결과:

- Root 전체 트리를 JSON으로 직렬화
- JSON을 클립보드로 복사

## Import

Menu Editor에서 `Import` 클릭.

결과:

- 클립보드 JSON parse/validate
- 성공 시 대상 경로에 적용

## 자주 보는 오류

- Clipboard is empty
- Clipboard is not JSON
- Root JSON is not array
- Entry is not object / invalid

## 권장 워크플로우

1. 먼저 백업 Export
2. JSON 편집
3. Import
4. 문제 시 백업 재Import
