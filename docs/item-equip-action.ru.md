# Item Equip Action

`Item Equip Action` сохраняет слепок экипировки и переэкипирует совпадающие предметы из инвентаря.

## Target slots

Можно назначить:

- Mainhand
- Offhand
- Helmet
- Chestplate
- Leggings
- Boots

Если слот пустой в редакторе, он пропускается при выполнении.

## Как создать

1. Нажми `Add Item Equip`.
2. Заполни Title/Note/Icon.
3. Перетащи предметы из source grid в target slots.
4. Сохрани.

### Source grid включает

- текущий offhand
- надетую броню
- основной инвентарь
- hotbar

## При выполнении

1. Для каждого настроенного target slot идет проверка.
2. Если target уже совпадает — skip.
3. Иначе ищется лучший matching stack.
4. Выполняется swap в target slot.

Обработка идет по слотам и допускает частичный успех.

## Правила matching

- Сравнение по полному snapshot (NBT + metadata), count игнорируется.
- При нескольких совпадениях берется stack с максимальным count.

## Правило Mainhand

`Mainhand` — это текущий выбранный hotbar слот на момент триггера.

## Поведение под нагрузкой

- Если запустить второй `Item Equip Action`, первый отменяется.
- Движение и основной gameplay остаются активными, выполнение идет в background ticks.
