# API

Esta pagina cobre a API publica `org.z2six.ezactions.api` para EZ Actions 2.0.0.0.

## O que a API faz

Praticamente tudo que o usuario faz na GUI, mais controle runtime:

- ler arvore de menu
- criar/editar/remover/reordenar acoes e pacotes
- marcar entrada como `locked`
- importar/exportar/validar JSON
- abrir editor/config/radial
- abrir radial temporario sem persistir

## Access

```java
import org.z2six.ezactions.api.EzActions;
import org.z2six.ezactions.api.EzActionsApi;

EzActionsApi api = EzActions.get();
```

## Tipos de acao

- `KEY`
- `COMMAND`
- `ITEM_EQUIP`

## Nota para modpacks

Se API criar pacote com keybind habilitado, o usuario precisa reiniciar para registrar o keybind.

## Referencia completa

Para assinaturas completas e exemplos avancados, consulte a pagina API em ingles (mais completa e mais atualizada).
