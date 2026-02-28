# Import Export

Import/Export в EZ Actions работает через clipboard.

## Export

В Menu Editor нажми `Export`.

Результат:

- Полное root-дерево сериализуется в JSON.
- JSON копируется в clipboard.

## Import

В Menu Editor нажми `Import`.

Результат:

- JSON из clipboard парсится и валидируется.
- При успехе элементы добавляются/заменяются по пути импорта.

## Частые ошибки

- Clipboard is empty
- Clipboard is not JSON
- Root JSON is not array
- Entry is not object / invalid

## Практичный workflow

1. Экспортируй текущее меню в файл (бэкап).
2. Проверь/измени JSON.
3. Импортируй.
4. При необходимости откатись предыдущим экспортом.

## JSON shape

Верхний уровень: массив элементов меню (в некоторых API-путях допускается single item).

Элемент должен быть либо:

- action item с `action`
- bundle item с `children`
