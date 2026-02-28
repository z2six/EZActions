# Acao de comando

`Acao de comando` envia comandos pelo radial.

## Campos

- **Title**
- **Note**
- **Command** (multilinha)
- **Multi-command delay (ticks)**
- **Cycle commands (one per use)**
- **Icon**

## Regras do campo Command

- Uma linha = um comando.
- Prefixo `/` e opcional.
- Linhas vazias sao ignoradas.

## Delay

`Multi-command delay (ticks)` controla espacamento entre linhas no modo nao ciclico.

- `0`: envia tudo na hora.
- `>0`: envia linha por linha com delay.

## Cycle commands

Se ativado, cada uso envia uma linha e avanca para a proxima.

```text
/time set day
/time set night
```

Uso 1 -> day  
Uso 2 -> night  
Uso 3 -> day

## Notas

- O envio e client-side, mas o server ainda valida permissao.
- Nova sequencia substitui a fila anterior.
