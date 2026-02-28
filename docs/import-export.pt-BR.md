# Importar Exportar

A importação/exportação do EZ Actions funciona através da sua área de transferência.

## Exportar

No Editor de Menu, clique em `Export`.

Resultado:

- A árvore raiz completa atual é serializada para JSON.
- JSON é copiado para a área de transferência.

## Importar

No Editor de Menu, clique em `Import`.

Resultado:

- O JSON da área de transferência é analisado e validado.
- Em caso de sucesso, as entradas importadas são adicionadas/substituídas por caminho de importação.

## Mensagens de erro comuns

- A área de transferência está vazia
- A área de transferência não é JSON
- Root JSON não é array
- A entrada não é objeto/inválida

## Fluxo de trabalho prático

1. Exporte o menu atual para um arquivo de texto como backup.
2. Teste as edições em JSON.
3. Importar.
4. Se necessário, reverta importando o backup anterior.

## Forma JSON

O nível superior oferece suporte a uma variedade de itens de menu (ou item único em alguns caminhos de API).

Cada item do menu deve ser:

- um item de **ação** com o objeto `action`
- um item **bundle** com matriz `children`

### Exemplo de ação mínima

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
