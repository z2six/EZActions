# Importar y exportar

En EZ Actions, `Import` y `Export` usan el portapapeles.

## Exportar

En Menu Editor pulsa `Export`.

Resultado:

- El arbol root completo se serializa a JSON.
- JSON se copia al portapapeles.

## Importar

En Menu Editor pulsa `Import`.

Resultado:

- Se parsea y valida JSON del portapapeles.
- Si es valido, se aplica en la ruta objetivo.

## Errores comunes

- Clipboard vacio
- Clipboard no es JSON
- Root JSON no es array
- Entry no es objeto o es invalido

## Flujo recomendado

1. Exporta backup actual.
2. Edita JSON.
3. Importa.
4. Si algo falla, reimporta el backup.

## Forma JSON

Cada item debe tener:

- `action` (accion), o
- `children` (paquete)

No ambos al mismo tiempo.
