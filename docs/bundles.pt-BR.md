# Pacotes (Bundle)

`Pacote` e uma pasta/categoria dentro da arvore radial.

## Campos do pacote

- **Title**
- **Note** (opcional)
- **Icon**
- **Hide from main radial**
- **Enable keybind**

## Keybind de pacote

Se `Enable keybind` estiver ativo, EZ Actions registra um keybind dedicado para esse pacote.

!!! warning "Requer reinicio"
    O registro do keybind de pacote so entra apos reiniciar o cliente.

## Hide from main radial

Se ativado:

- O pacote nao aparece na root.
- O pacote continua no modelo.
- Ainda pode ser aberto por API ou keybind de pacote.

## Pacotes bloqueados (locked)

`locked` protege o pacote contra remocao no jogo.

- Nao remove pela GUI.
- Ainda pode remover manualmente no JSON.
