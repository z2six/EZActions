# Accion de tecla

La `Accion de tecla` ejecuta un keybind existente (vanilla o modded) desde el radial.

## Campos

- **Title**
- **Note**
- **Mapping Name**
- **Delivery**: `AUTO`, `INPUT`, `TICK`
- **Toggle**
- **Icon**

## Flujo recomendado

1. Pulsa `Pick from Keybinds`.
2. Selecciona el binding en la lista.
3. Deja `AUTO` como opcion por defecto.
4. Guarda.

## Modos de entrega

| Modo | Que hace | Cuando usar |
|---|---|---|
| `AUTO` | Elige la mejor via automaticamente | Recomendado |
| `INPUT` | Inyecta press/release en input pipeline | Si `AUTO` falla |
| `TICK` | Fuerza estado de tecla por ticks | Modo de respaldo |

## Toggle

- `OFF`: un tap por uso.
- `ON`: alterna estado down/up por uso.

## Problemas comunes

- **No pasa nada:** revisa mapping id o vuelve a elegir en picker.
- **Se activa otra tecla:** evita escribir a mano, usa picker.
- **Funciona en singleplayer y no en server:** depende de reglas/permisos del server.
