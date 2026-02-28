# Importar e exportar

`Import` e `Export` no EZ Actions usam a area de transferencia.

## Exportar

No Menu Editor, clique em `Export`.

Resultado:

- A arvore root completa vira JSON.
- JSON e copiado para o clipboard.

## Importar

No Menu Editor, clique em `Import`.

Resultado:

- JSON do clipboard e parseado e validado.
- Se valido, aplica na rota alvo.

## Erros comuns

- Clipboard vazio
- Clipboard nao e JSON
- Root JSON nao e array
- Entry nao e objeto ou e invalida

## Fluxo recomendado

1. Exporte um backup.
2. Edite o JSON.
3. Importe.
4. Se der ruim, reimporte o backup.

## Shape JSON

Cada item precisa ter:

- `action` (acao), ou
- `children` (pacote)

Nao use ambos no mesmo objeto.
