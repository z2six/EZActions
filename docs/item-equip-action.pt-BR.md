# Acao de equipar item

`Acao de equipar item` salva um snapshot de equipamento e reequipa itens que batem com esse snapshot.

## Slots alvo

- Mainhand
- Offhand
- Helmet
- Chestplate
- Leggings
- Boots

Se um slot ficar vazio no editor, ele e ignorado na execucao.

## Como criar

1. `Adicionar equipar item`
2. Defina `Title`, `Note`, `Icon`
3. Arraste itens de `Source Items` para `Equip Targets`
4. Salve

## Source Items inclui

- offhand atual
- armadura equipada
- inventario principal
- hotbar

## Regras de matching

- Matching por snapshot completo (NBT e metadata), ignorando quantidade.
- Se houver varios matches, usa a pilha com maior count.

## Regra Mainhand

`Mainhand` e o slot de hotbar selecionado no momento da execucao.

## Comportamento

- Processa slot por slot (sucesso parcial e permitido).
- Se outra acao de equipar iniciar, a anterior e cancelada.
- Movimento do jogador continua ativo durante a execucao.
