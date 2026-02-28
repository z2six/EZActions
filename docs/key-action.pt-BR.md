# Ação-chave

Use uma ação-chave para disparar um atalho de teclado existente (vanilla ou modificado) do radial.

## Campos

- **Título**: o que você vê no editor/rótulo radial.
- **Nota**: texto auxiliar opcional.
- **Nome do mapeamento**: ID ou rótulo do mapeamento de chave.
- **Entrega**: `AUTO`, `INPUT` ou `TICK`.
- **Alternar**: alterna o estado da tecla em vez de tocar.
- **Ícone**: clique na caixa do ícone para abrir o Seletor de ícones.

## Configuração recomendada

1. Clique em @@CÓDIGO0@@.
2. Selecione a encadernação no seletor (mais seguro do que digitar à mão).
3. Mantenha a entrega em `AUTO`, a menos que você tenha um motivo específico.
4. Salve.

## Modos de entrega

| Modo | O que faz | Quando usar |
|---|---|---|
| @@CÓDIGO0@@ | Escolhe o melhor caminho automaticamente | Padrão para quase todos |
| @@CÓDIGO1@@ | Injeta pressionamento/liberação de tecla por meio do pipeline de entrada | Se uma ligação não estiver respondendo em `AUTO` |
| @@CÓDIGO3@@ | Define a ativação/desativação da chave por meio de atualizações de estado da chave | Alternativa útil para chaves difíceis de injetar |

## Alternar

- `OFF`: um toque por uso radial.
- `ON`: vira a tecla para baixo/para cima a cada uso.

Útil para ações como alternâncias de estilo sprint/sneak, dependendo de como o atalho de teclado alvo se comporta.

## Problemas comuns

- **Nada acontece:** verifique o ID do mapeamento ou escolha novamente na lista de atalhos de teclado.
- **Gatilhos de atalho de teclado incorretos:** seu nome digitado corresponde a outro rótulo de mapeamento; usar seletor.
- **Funciona no modo singleplayer, mas não no servidor:** a ação alvo pode ser restrita ao servidor.

???+ info "Aprofundamento: como funciona a entrega de chaves"
    Internamente, o EZ Actions resolve nomes de mapeamento:

1. correspondência exata da chave de tradução
    2. correspondência exata de etiqueta localizada
    3. substituto contém correspondência

Para teclas requeridas por modificadores (Ctrl/Shift/Alt), ele usa lógica de injeção extra. `AUTO` pode mudar de estratégia dependendo se uma chave está vinculada, tem um scancode ou requer modificadores.
