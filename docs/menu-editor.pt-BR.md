# GUI do editor do menu principal

O Editor de Menu é o seu centro de controle para construir o radial.

Abra-o com o atalho de teclado `Open editor`.

##Layout

- **Painel esquerdo:** criar/editar/remover ações e pacotes.
- **Painel direito:** lista de páginas atuais (raiz ou pacote atual).
- **Filtro superior esquerdo:** filtra por título, nota e texto do tipo de ação.
- **Inferior esquerdo:** importar/exportar, configurar, fechar.

## Adicionar botões

- @@CÓDIGO1@@
- @@CÓDIGO2@@
- @@CÓDIGO3@@
- @@CÓDIGO4@@

## Interação da lista

### Rato

- **LMB no item:** selecione.
- **Item de arrastar LMB:** reordenar dentro da página atual.
- **LMB arrastar item para a linha do pacote:** move o item para esse pacote.
- **LMB arrastar item para as últimas linhas:** mover item para pai/raiz.
- **RMB na linha do pacote:** abra esse pacote.

### Teclado

- `Ctrl + F` foca a caixa de filtro.
- `Enter` edita a linha selecionada.
- `Delete` ou `Backspace` remove a linha selecionada.
- `Up Arrow` move o item selecionado para cima.
- `Down Arrow` move o item selecionado para baixo.

!!! dica
    O movimento para cima/para baixo do teclado está desativado enquanto o texto do filtro está ativo, para evitar reordenações ambíguas.

## Tipos de linha que você verá

- Linhas normais de itens (ações ou pacotes)
- Linha de localização atual (`root/.../bundle`)
- @@CÓDIGO12@@
- @@CÓDIGO13@@

## Entradas bloqueadas

As entradas bloqueadas são protegidas contra operações de exclusão no jogo.

- Você não pode removê-los com a exclusão da GUI.
- Você não pode removê-los com chamadas de remoção de API que respeitam o bloqueio.
- Eles ainda podem ser removidos editando manualmente `config/ezactions/menu.json`.

## Dicas para edição rápida

- Use títulos curtos para etiquetas radiais mais limpas.
- Coloque ações de utilidade compartilhada em pacotes (para menos confusão de root).
- Use a caixa de filtro como uma pesquisa rápida quando seu menu ficar enorme.

???+ info "Aprofundamento: comportamento de arrastar e soltar"
    - Reordenar usa lógica de inserção (não troca simples).
    - Cair em um pacote é anexado à lista filho desse pacote.
    - Cair para pai/raiz mantém a visualização atual do editor em vez de saltar automaticamente.
    - Todas as operações de movimentação/remoção bem-sucedidas persistem imediatamente no disco.
