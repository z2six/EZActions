# API

Esta pagina documenta la API publica `org.z2six.ezactions.api` para EZ Actions 2.0.0.0.

## Que permite la API

Practicamente todo lo que el usuario hace en GUI, mas control runtime:

- leer arbol de menu
- crear/editar/borrar/reordenar acciones y paquetes
- marcar entradas como `locked`
- importar/exportar/validar JSON
- abrir editor/config/radial
- abrir radial temporal en runtime sin persistir

## Acceso

```java
import org.z2six.ezactions.api.EzActions;
import org.z2six.ezactions.api.EzActionsApi;

EzActionsApi api = EzActions.get();
```

## Tipos de accion

- `KEY`
- `COMMAND`
- `ITEM_EQUIP`

## Nota para modpacks

Si API crea paquetes con keybind habilitado, el usuario necesita reinicio para registrar ese keybind.

## Referencia completa

Para firmas completas y ejemplos avanzados, usa la pagina API en ingles (es la mas detallada y sincronizada con el codigo).
