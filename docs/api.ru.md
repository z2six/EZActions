# API

Страница описывает публичный API `org.z2six.ezactions.api` для EZ Actions **2.0.0.0**.

Для:

- разработчиков модов
- авторов модпаков
- продвинутой автоматизации

## Что может API

Коротко: почти всё, что делает пользователь в GUI, плюс runtime-контроль.

- читать дерево меню
- добавлять/обновлять/удалять/переставлять Action и Bundle
- ставить флаг locked
- импорт/экспорт/валидация JSON
- открывать editor/config/radial
- открывать radial сразу в нужном Bundle
- открывать временный runtime radial без сохранения
- вызывать key input и command sequencing
- подписываться на API events

## Access

```java
import org.z2six.ezactions.api.EzActions;
import org.z2six.ezactions.api.EzActionsApi;

EzActionsApi api = EzActions.get();
```

## Core concepts

### MenuPath

`MenuPath` адресует Bundle по цепочке названий от root.

```java
MenuPath root = MenuPath.root();
MenuPath p = MenuPath.root().child("Utilities").child("Combat");
```

### Locked

`locked=true` защищает от in-game delete и lock-aware API remove.

### Persistence

Большинство mutating API вызовов сохраняют изменения сразу.

## Action types

- `Key Action`
- `Command Action`
- `Item Equip Action`

Иконки:

```java
IconSpec itemIcon = IconSpec.item("minecraft:ender_pearl");
IconSpec customIcon = IconSpec.custom("custom:my_icon");
```

## Практические заметки для pack authors

- Если API включает bundle keybind, пользователю нужен рестарт клиента.
- EZ Actions показывает сообщение в чат при необходимости рестарта.
- Для долгой поддержки меню держи стабильные id/title.

???+ warning "Compatibility"
    API может меняться в следующих версиях. Эта страница соответствует 2.0.0.0.

## Полный reference

Полный список методов и расширенные примеры смотри в английской API-странице (она обновляется первой):

- `menuRead()` / `menuWrite()`
- `importExport()`
- `inputOps()`
- `editorOps()`
- `events()`
