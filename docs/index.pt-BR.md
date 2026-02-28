# Wiki de ações EZ

!!! dica "Idioma / 语言"
    Use o ícone **alternador de idioma** na barra de navegação superior direita para alterar o idioma do wiki.

    使用右上角导航栏中的**语言切换**图标来切换 Wiki语言。

EZ Actions é um mod do Minecraft do lado do cliente que oferece um menu radial rápido para ações que você usa o tempo todo.

Pense nisso como sua “roda rápida de combate + utilidade”: uma tecla pressionada, um toque, pronto.

!!! aviso "Escopo da versão"
    Este wiki foi escrito para **EZ Actions 2.0.0.0**.

    Se você estiver usando uma versão mais recente, alguns recursos e detalhes da interface do usuário podem ter sido alterados.

???+ informações "TLDR"
    - Crie seu próprio menu radial com **Ações-chave**, **Ações de comando**, **Ações de equipar itens** e **Pacotes**.
    - Estilize-o com configurações de cor/design/animação.
    - Menu de importação/exportação JSON para compartilhamento e backups.
    - Os desenvolvedores de mod podem controlar tudo através da API (incluindo radiais de tempo de execução temporários).

## O que as ações EZ podem fazer

- Acione teclas vanilla ou modificadas.
- Execute comandos de uma ou várias linhas.
- Equipe conjuntos de equipamentos gravados usando correspondência exata de itens (NBT incluído).
- Organize ações em pacotes aninhados.
- Oculte pacotes do root, mantendo-os acessíveis por meio do atalho de teclado do pacote.
- Adicione ícones personalizados de `config/ezactions/icons`.
- Crie/edite menus no jogo com arrastar/soltar e atalhos de teclado.
- Deixe outros mods conduzirem EZ Actions por meio da API pública.

## Para quem é este Wiki

- Jogadores que desejam um guia de configuração claro sem ler o código-fonte.
- Usuários avançados que desejam detalhes avançados de comportamento.
- Fabricantes e desenvolvedores de modpack que desejam documentos API completos.

Você verá seções expansíveis de "aprofundamento" na maioria das páginas. Ignore-os se quiser apenas o fluxo prático.

## Início rápido

1. Defina atalhos de teclado para:
   - @@CÓDIGO1@@
   - @@CÓDIGO2@@
2. Abra o editor e adicione sua primeira ação.
3. Segure a tecla radial no jogo e solte uma fatia para executar.
4. Ajuste o visual na tela Config.

## Navegação

Use a navegação esquerda para documentos completos:

- GUI do Editor do Menu Principal
- Ação-chave
- Ação de comando
- Ação de equipar itens
- Pacotes
- Importação Exportação
- Configuração
- API

??? "Nota técnica"
    EZ Actions é totalmente do lado do cliente. Não requer instalação de servidor.

    As ações ainda dependem do que o servidor permite (por exemplo, permissões de comando).
