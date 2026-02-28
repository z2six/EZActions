# Editor do menu principal

O editor de menu e o centro para montar seu radial.

Abra com a tecla `Open editor`.

## Layout

- **Painel esquerdo:** criar, editar e remover acoes e pacotes.
- **Painel direito:** lista da pagina atual (root ou pacote atual).
- **Filter no topo esquerdo:** filtra por `Title`, `Note` e tipo de acao.
- **Parte inferior esquerda:** `Import`, `Export`, `Configuracao`, `Close`.

## Botoes de adicionar

- `Adicionar acao de tecla`
- `Adicionar comando`
- `Adicionar equipar item`
- `Adicionar pacote`

## Interacao com a lista

### Mouse

- **LMB no item:** selecionar.
- **LMB drag:** reordenar na pagina atual.
- **Drag sobre pacote:** mover para dentro do pacote.
- **Drag sobre linhas de retorno:** mover para parent/root.
- **RMB em pacote:** abrir pacote.

### Teclado

- `Ctrl + F`: foco no `Filter`.
- `Enter`: editar selecionado.
- `Delete` ou `Backspace`: remover selecionado.
- `Up Arrow`: mover para cima.
- `Down Arrow`: mover para baixo.

!!! tip
    O movimento com setas e desativado quando o filtro tem texto.

## Tipos de linha

- linhas normais (acao ou pacote)
- breadcrumb (`root/.../bundle`)
- `Back to root`
- `Back to <parent>`

## Entradas bloqueadas (locked)

Entrada `locked` nao pode ser removida no jogo.

- Nao remove via GUI.
- Nao remove via API lock-aware.
- Pode remover manualmente em `config/ezactions/menu.json`.
