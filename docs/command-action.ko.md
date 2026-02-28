# Command Action

`Command Action`은 Radial에서 명령을 전송합니다.

## 필드

- **Title**
- **Note**
- **Command**(멀티라인)
- **Multi-command delay (ticks)**
- **Cycle commands (one per use)**
- **Icon**

## Command 박스 규칙

- 한 줄 = 한 명령
- `/` 접두사는 선택 사항
- 빈 줄은 무시

## Delay

`Multi-command delay (ticks)`는 비순환 모드의 줄 간격입니다.

- `0`: 즉시 전송
- `>0`: 줄 단위 지연 전송

## Cycle commands

활성화 시 1회 사용마다 한 줄만 전송하고 다음 줄로 순환합니다.

```text
/time set day
/time set night
```

Use 1 -> day  
Use 2 -> night  
Use 3 -> day

## 참고

- dispatch는 client-side이지만 서버 권한은 그대로 적용
- 새 시퀀스 시작 시 기존 큐는 대체
