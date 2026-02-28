# Acción clave

Utilice una acción clave para activar una combinación de teclas existente (vainilla o modificada) desde el radial.

## Campos

- **Título**: lo que ves en el editor/etiqueta radial.
- **Nota**: texto auxiliar opcional.
- **Nombre de asignación**: identificación o etiqueta de asignación de clave.
- **Entrega**: `AUTO`, `INPUT` o `TICK`.
- **Alternar**: alterna el estado de la tecla en lugar de tocarla.
- **Icono**: haga clic en el cuadro del icono para abrir el Selector de iconos.

## Configuración recomendada

1. Haga clic en `Pick from Keybinds`.
2. Seleccione la encuadernación en el selector (más seguro que escribir a mano).
3. Mantenga la entrega el `AUTO` a menos que tenga un motivo específico.
4. Guardar.

## Modos de entrega

| Modo | Qué hace | Cuándo utilizar |
|---|---|---|
| @@CÓDIGO0@@ | Elige el mejor camino automáticamente | Predeterminado para casi todos |
| @@CÓDIGO1@@ | Inyecta pulsación/liberación de teclas a través del canal de entrada | Si un enlace no responde en `AUTO` |
| @@CÓDIGO3@@ | Establece la tecla hacia abajo/arriba a través de actualizaciones de estado clave | Útil respaldo para claves difíciles de inyectar |

## Alternar

- `OFF`: un toque por uso radial.
- `ON`: mueve la tecla hacia abajo/arriba en cada uso.

Útil para acciones como alternar estilos de sprint/sneak, dependiendo de cómo se comporte la combinación de teclas de destino.

## Problemas comunes

- **No pasa nada:** verifique la identificación de la asignación o seleccione nuevamente de la lista de combinaciones de teclas.
- **Activadores de combinación de teclas incorrecta:** el nombre que usted escribió coincidió con otra etiqueta de asignación; utilizar selector.
- **Funciona en un jugador pero no en el servidor:** la acción objetivo puede estar restringida por el servidor.

???+ info "Análisis profundo: cómo funciona la entrega de claves"
    Internamente, EZ Actions resuelve nombres de mapeo mediante:

1. coincidencia de clave de traducción exacta
    2. coincidencia exacta de etiquetas localizadas
    3. el respaldo contiene una coincidencia

Para las teclas que requieren modificadores (Ctrl/Shift/Alt), utiliza una lógica de inyección adicional. `AUTO` puede cambiar de estrategia dependiendo de si una clave está vinculada, tiene un código de escaneo o requiere modificadores.
