#Configuração

Abra a configuração do Editor de Menu usando o botão `Config`.

A UI de configuração tem três seções:

- Geral
- Animações
- Projeto

## Em geral

| Configuração | Tipo | Faixa/Valores | Padrão |
|---|---|---|---|
| Mover enquanto radialmente aberto | booleano | LIGADO/DESLIGADO | ATIVADO |
| Mostrar rótulo de foco | booleano | LIGADO/DESLIGADO | ATIVADO |
| Linhas Visíveis do Editor de Comandos | interno | 1-20 | 5 |

## Animações

| Configuração | Tipo | Faixa/Valores | Padrão |
|---|---|---|---|
| Animações habilitadas | booleano | LIGADO/DESLIGADO | ATIVADO |
| Animação de abrir/fechar | booleano | LIGADO/DESLIGADO | ATIVADO |
| Animação flutuante | booleano | LIGADO/DESLIGADO | ATIVADO |
| Estilo aberto | enum | `WIPE`, `FADE`, `NONE` | @@CÓDIGO3@@ |
| Direção | enum | @@CÓDIGO4@@, @@CÓDIGO5@@ | @@CÓDIGO6@@ |
| Estilo de foco | enum | `FILL_SCALE`, `FILL_ONLY`, `SCALE_ONLY`, `NONE` | @@CÓDIGO11@@ |
| Porcentagem de crescimento ao passar o mouse | duplo | 0,0-0,5 | 0,05 |
| Duração de abertura/fechamento | interno (ms) | 0-2000 | 125 |

## Projeto

| Configuração | Tipo | Faixa/Valores | Padrão |
|---|---|---|---|
| Zona morta | interno | 0-90 | 18 |
| Raio Externo | interno | 24-512 | 72 |
| Espessura do Anel | interno | 6-256 | 28 |
| Limite inicial de escala | interno | 0-128 | 8 |
| Escala por item | interno | 0-100 | 6 |
| Lacuna da fatia | int (graus) | 0-12 | 0 |
| Estilo de design | enum | `SOLID`, `SEGMENTED`, `OUTLINE`, `GLASS` | @@CÓDIGO4@@ |
| Cor do anel | ARGB interno | Int assinado de 32 bits | @@CÓDIGO5@@ |
| Cor do mouse | ARGB interno | Int assinado de 32 bits | @@CÓDIGO6@@ |
| Cor da borda | ARGB interno | Int assinado de 32 bits | @@CÓDIGO7@@ |
| Cor do texto | ARGB interno | Int assinado de 32 bits | @@CÓDIGO8@@ |

## Visualização

`Preview` abre uma tela de visualização radial e faz um loop de animação com uma pausa de 1s entre os loops.

Use isto para ajustar as configurações visuais antes de confirmar.

## Salvar comportamento

- Salvar grava valores imediatamente em arquivos de configuração.
- Ao salvar, a tela retorna para a tela do editor pai.

## Arquivos de configuração

- @@CÓDIGO0@@
- @@CÓDIGO1@@
- @@CÓDIGO2@@

???+ info "Aprofundamento: formato de cor"
    As cores são armazenadas como entradas ARGB assinadas de 32 bits.

Exemplo:

- @@CÓDIGO0@@
    - alfa `AA`, depois vermelho `RR`, verde `GG`, azul `BB`

Números decimais negativos são normais para valores ARGB opacos.
