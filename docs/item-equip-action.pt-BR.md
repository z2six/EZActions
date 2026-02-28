# Ação de equipar item

Equipar itens permite que você salve um instantâneo do equipamento e reequipe os itens correspondentes do seu inventário.

## Slots de destino

Você pode atribuir qualquer um destes:

- Principal
- De improviso
- Capacete
- Peitoral
- Legging
- Botas

Se um slot for deixado vazio no editor, o EZ Actions o ignora durante a execução.

## Como criar um

1. Clique em @@CÓDIGO0@@.
2. Defina título/nota/ícone.
3. Arraste itens da grade de origem para os slots de destino.
4. Salve.

### Grade de origem inclui

- item improvisado atual
- armadura equipada
- inventário principal
- barra quente

## Durante a execução

Quando você aciona a ação radial:

1. EZ Actions verifica cada slot de destino configurado.
2. Se o alvo já corresponder ao item gravado, ele será ignorado.
3. Caso contrário, ele encontra a melhor pilha de origem correspondente.
4. Ele troca itens no slot de destino.

Ele processa slot por slot e permite sucesso parcial.

## Regras de correspondência importantes

- A correspondência é baseada na assinatura de snapshot de pilha completa (NBT e metadados), ignorando a contagem.
- Se existirem várias pilhas correspondentes, ele escolhe aquela com maior contagem.

## Regra principal

`Mainhand` significa o slot da barra de atalho atualmente selecionado no momento do acionamento.

## Comportamento sob pressão

- Se você acionar uma segunda ação de equipar item enquanto uma estiver em execução, a antiga será cancelada e substituída.
- As entradas de movimento e jogabilidade permanecem ativas enquanto a execução é executada em segundo plano.

## Controles rápidos no editor

- Arrastar LMB do slot de origem para o destino: atribua o item.
- RMB no slot alvo: atribuição clara.
- `Refresh Items`: reconstrói a lista de fontes a partir do estado atual do inventário do jogador.

???+ info "Aprofundamento: ordem de execução de slots"
    A ordem de execução atual é:

1. Capacete
    2. Peitoral
    3. Perneiras
    4. Botas
    5. De improviso
    6. Principal

Destinos vazios e correspondências de origem ausentes são ignorados e não tratados como falha grave.
