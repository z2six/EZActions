# Comando Acción

Utilice una acción de comando para enviar comandos desde el radial.

## Campos

- **Título**
- **Nota**
- **Comando** (multilínea)
- **Retraso de múltiples comandos (tics)**
- **Ciclar comandos (uno por uso)**
- **Icono**

## Reglas del cuadro de comandos

- Una línea = un comando.
- El `/` inicial es opcional (EZ Actions lo elimina antes de enviarlo).
- Se ignoran las líneas vacías.

## Comportamiento de retraso

`Multi-command delay (ticks)` controla el espacio entre las líneas de comando cuando no se realiza el ciclo.

- `0`: envía líneas inmediatamente.
- `>0`: cola línea por línea con ese retraso.

## Comandos de ciclo

Si está habilitado, cada uso radial envía exactamente una línea y rota a la siguiente.

Ejemplo:

```text
/time set day
/time set night
```

Use #1 -> day  
Use #2 -> night  
Use #3 -> day

## Casos de uso prácticos

- Comandos de utilidad rápida (`/home`, `/spawn`, `/warp mine`)
- Alternancias de juego de roles (`/hat`, `/nick`)
- Flujos de trabajo de administración divididos en líneas

## Notas

- Este es el envío del lado del cliente: los permisos del servidor aún se aplican.
- Si comienza una nueva secuencia de comando, se reemplaza la secuencia en cola anterior.

???+ info "Análisis profundo: modelo de secuenciación"
    - Los comandos multilínea sin ciclos utilizan un secuenciador de ticks del cliente.
    - El modo de ciclismo almacena un cursor interno en la instancia de acción.
    - Los modos cíclico e inmediato cancelan las secuencias en vuelo antes del envío.
