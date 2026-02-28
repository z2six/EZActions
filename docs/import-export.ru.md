# Импорт Экспорт

Импорт/экспорт EZ Actions работает через буфер обмена.

## Экспорт

В Редакторе меню нажмите `Export`.

Результат:

— Текущее полное корневое дерево сериализуется в JSON.
- JSON копируется в буфер обмена.

## Импорт

В Редакторе меню нажмите `Import`.

Результат:

- JSON буфера обмена анализируется и проверяется.
- В случае успеха импортированные записи добавляются/заменяются для каждого пути импорта.

## Распространенные сообщения об ошибках

- Буфер обмена пуст
- Буфер обмена не JSON
- Корневой JSON не является массивом.
- Запись не является объектом/недействительна

## Практический рабочий процесс

1. Экспортируйте текущее меню в текстовый файл в качестве резервной копии.
2. Тестовые правки в JSON.
3. Импорт.
4. При необходимости выполните откат, импортировав предыдущую резервную копию.

## Форма JSON

Верхний уровень поддерживает массив пунктов меню (или один элемент в некоторых путях API).

Каждый пункт меню должен быть:

- элемент **action** с объектом `action`
- элемент **bundle** с массивом `children`

### Пример минимального действия

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
