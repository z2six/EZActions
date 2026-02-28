# Acao de tecla

`Acao de tecla` dispara um keybind existente (vanilla ou modded) pelo radial.

## Campos

- **Title**
- **Note**
- **Mapping Name**
- **Delivery**: `AUTO`, `INPUT`, `TICK`
- **Toggle**
- **Icon**

## Fluxo recomendado

1. Clique em `Pick from Keybinds`.
2. Selecione o binding na lista.
3. Use `AUTO` como padrao.
4. Salve.

## Modos de entrega

| Modo | O que faz | Quando usar |
|---|---|---|
| `AUTO` | Escolhe o melhor metodo automaticamente | Padrao |
| `INPUT` | Injeta press/release no pipeline de input | Se `AUTO` falhar |
| `TICK` | Controla estado da tecla por ticks | Fallback |

## Toggle

- `OFF`: um tap por uso.
- `ON`: alterna down/up a cada uso.

## Problemas comuns

- **Nada acontece:** revise mapping id ou escolha novamente no picker.
- **Aciona tecla errada:** evite digitar manualmente, use picker.
- **Funciona no singleplayer e nao no server:** depende das regras/permissoes do server.
