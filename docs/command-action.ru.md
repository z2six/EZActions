# Command Action

`Command Action` отправляет команды из радиального меню.

## Поля

- **Title**
- **Note**
- **Command** (multi-line)
- **Multi-command delay (ticks)**
- **Cycle commands (one per use)**
- **Icon**

## Правила Command box

- Одна строка = одна команда.
- Начальный `/` опционален.
- Пустые строки игнорируются.

## Delay

`Multi-command delay (ticks)` задает интервал между строками в non-cycle режиме.

- `0`: отправить сразу.
- `>0`: отправлять построчно с задержкой.

## Cycle commands

Если включено, каждое использование отправляет только одну строку и переключается на следующую.

Пример:

```text
/time set day
/time set night
```

Use #1 -> day  
Use #2 -> night  
Use #3 -> day

## Примечания

- Dispatch клиентский, но серверные права все равно применяются.
- Новый запуск sequence заменяет предыдущую очередь.
