# Ação de comando

Use uma ação de comando para enviar comandos do radial.

## Campos

- **Título**
- **Nota**
- **Comando** (multilinha)
- **Atraso multi-comando (tiques)**
- **Comandos de ciclo (um por uso)**
- **Ícone**

## Regras da caixa de comando

- Uma linha = um comando.
- Inicializar `/` é opcional (EZ Actions remove-o antes de enviar).
- Linhas vazias são ignoradas.

## Comportamento de atraso

`Multi-command delay (ticks)` controla o espaçamento entre as linhas de comando quando não está em ciclo.

- `0`: envia linhas imediatamente.
- `>0`: fila linha por linha com esse atraso.

## Comandos de Ciclo

Se ativado, cada uso radial envia exatamente uma linha e gira para a próxima.

Exemplo:

```text
/time set day
/time set night
```

Use #1 -> day  
Use #2 -> night  
Use #3 -> day

## Casos de uso prático

- Comandos utilitários rápidos (`/home`, `/spawn`, `/warp mine`)
- Alternadores de roleplay (`/hat`, `/nick`)
- Fluxos de trabalho administrativos divididos entre linhas

## Notas

- Este é o envio do lado do cliente: as permissões do servidor ainda se aplicam.
- Se uma nova sequência de comandos for iniciada, a sequência anterior na fila será substituída.

???+ info "Aprofundamento: modelo de sequenciamento"
    - Comandos multilinhas sem ciclo usam um sequenciador de ticks do cliente.
    - O modo de ciclismo armazena um cursor interno na instância da ação.
    - Os modos cíclico e imediato cancelam as sequências em voo antes do despacho.
