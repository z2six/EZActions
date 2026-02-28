# Main Menu Editor GUI

Редактор меню — основной центр управления радиалом.

Открывается keybind'ом `Open editor`.

## Layout

- **Левая панель:** создание/редактирование/удаление действий и Bundle.
- **Правая панель:** список текущей страницы (root или текущий Bundle).
- **Filter сверху слева:** фильтрует по Title, Note и типу действия.
- **Низ слева:** Import/Export, Config, Close.

## Кнопки добавления

- `Add Key Action`
- `Add Command`
- `Add Item Equip`
- `Add Bundle`

## Взаимодействие со списком

### Мышь

- **LMB по строке:** выбрать.
- **LMB drag:** reorder в текущей странице.
- **Drag на строку Bundle:** переместить элемент внутрь Bundle.
- **Drag на back-строки:** переместить в parent/root.
- **RMB по Bundle:** открыть Bundle.

### Клавиатура

- `Ctrl + F` — фокус на Filter.
- `Enter` — редактировать выбранное.
- `Delete` / `Backspace` — удалить выбранное.
- `Up Arrow` — сдвинуть выше.
- `Down Arrow` — сдвинуть ниже.

!!! tip
    Перемещение стрелками отключается, когда активен filter text.

## Типы строк

- обычные строки элементов (Action/Bundle)
- breadcrumb строка (`root/.../bundle`)
- `Back to root`
- `Back to <parent>`

## Locked записи

Locked запись защищена от удаления в GUI.

- Нельзя удалить через in-game delete.
- Нельзя удалить lock-aware API удалением.
- Можно удалить вручную через `config/ezactions/menu.json`.

???+ info "Deep dive: drag/drop"
    - Reorder работает по insertion-логике, не swap.
    - Drop в Bundle добавляет в конец children.
    - Drop в parent/root не переключает текущий экран.
    - Успешные move/remove сохраняются сразу на диск.
