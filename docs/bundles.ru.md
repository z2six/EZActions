# Bundles

`Bundle` — это папка/категория внутри дерева радиала.

Используй Bundle для группировки действий по контексту: combat, utility, building и т.д.

## Поля Bundle

- **Title**
- **Note** (опционально)
- **Icon**
- **Hide from main radial**
- **Enable keybind**

## Bundle keybind

Если включен `Enable keybind`, EZ Actions регистрирует отдельный keybind для этого Bundle.

!!! warning "Нужен рестарт"
    Регистрация Bundle keybind применяется после следующего рестарта клиента.

    EZ Actions показывает клиентское сообщение, когда рестарт обязателен.

## Hide from main radial

Если включено:

- Bundle скрыт на root странице радиала.
- Bundle остается в модели меню.
- Bundle можно открыть через API или bundle keybind.

## Вложенность

Bundle может содержать:

- Key Action
- Command Action
- Item Equip Action
- другие Bundle

## Locked Bundle

Bundle может быть отмечен как `locked`.

- In-game delete не удалит его.
- Ручная правка JSON может удалить.
