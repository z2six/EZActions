# Pacotes

Pacotes são pastas (categorias) dentro da árvore radial.

Use-os para agrupar ações por contexto: combate, construção, utilidade, roleplay, etc.

## Campos de pacote

- **Título** (também usado como id interno)
- **Nota** (opcional)
- **Ícone**
- **Ocultar do radial principal**
- **Ativar atalho de teclado**

## Pacote de atalhos de teclado

Se `Enable keybind` estiver ativado, EZ Actions registra um atalho de teclado dedicado para esse pacote.

!!! aviso "Reinicialização necessária"
    O registro do atalho de teclado do pacote é aplicado na próxima reinicialização do cliente.

EZ Actions mostra uma mensagem do cliente quando a reinicialização é necessária (incluindo pacotes criados pela API).

## Ocultar do radial principal

Se ativado:

- O pacote está oculto na página radial raiz.
- O pacote ainda existe no modelo de menu.
- O pacote ainda pode ser aberto via API ou atalho de teclado do pacote.

Bom para "páginas avançadas", você não quer bagunçar o root.

## Pacotes aninhados

Os pacotes podem conter:

- ações principais
- ações de comando
- ações de equipar itens
- mais pacotes

## Melhores práticas

- Mantenha a raiz pequena e de alta prioridade.
- Coloque ações de baixa frequência em pacotes mais profundos.
- Dê aos pacotes ícones claros e nomes curtos.

## Pacotes bloqueados

Um pacote pode ser marcado como `locked` (geralmente via API ou JSON).

- Os caminhos de exclusão no jogo não irão removê-lo.
- As edições manuais do JSON ainda podem removê-lo.

???+ info "Aprofundamento: identidade e singularidade"
    O título do pacote é usado como ID do pacote nos fluxos de trabalho do editor.

Nomes de pacotes duplicados podem causar ambiguidade, portanto, o editor bloqueia salvamentos duplicados de título/id do pacote.
