# Accion de comando

`Accion de comando` envia comandos desde el radial.

## Campos

- **Title**
- **Note**
- **Command** (multilinea)
- **Multi-command delay (ticks)**
- **Cycle commands (one per use)**
- **Icon**

## Reglas del cuadro Command

- Una linea = un comando.
- El prefijo `/` es opcional.
- Lineas vacias se ignoran.

## Delay

`Multi-command delay (ticks)` controla el intervalo entre lineas en modo no-ciclico.

- `0`: envia todo inmediato.
- `>0`: envia linea por linea con delay.

## Cycle commands

Si esta activo, cada uso envia una sola linea y rota a la siguiente.

```text
/time set day
/time set night
```

Uso 1 -> day  
Uso 2 -> night  
Uso 3 -> day

## Notas

- El envio es client-side, pero el server sigue validando permisos.
- Una secuencia nueva reemplaza cualquier cola anterior.
