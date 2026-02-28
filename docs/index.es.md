# Wiki de acciones EZ

!!! consejo "Idioma / 语言"
    Utilice el ícono **cambiador de idioma** en la barra de navegación superior derecha para cambiar el idioma de la wiki.

    语言切换**图标来切换 Wiki 语言。

EZ Actions es un mod de Minecraft del lado del cliente que te brinda un menú radial rápido para las acciones que usas todo el tiempo.

Piensa en ello como tu "rueda rápida de combate + utilidades": una tecla presionada, un movimiento rápido y listo.

!!! advertencia "Alcance de la versión"
    Este wiki está escrito para **EZ Actions 2.0.0.0**.

    Si está utilizando una versión más reciente, es posible que algunas funciones y detalles de la interfaz de usuario hayan cambiado.

???+ información "TLDR"
    - Cree su propio menú radial con **Acciones clave**, **Acciones de comando**, **Acciones de equipamiento de elementos** y **Paquetes**.
    - Dale estilo con configuraciones de color/diseño/animación.
    - Menú de importación/exportación JSON para compartir y realizar copias de seguridad.
    - Los desarrolladores de mods pueden controlar todo a través de la API (incluidos los radiales de tiempo de ejecución temporales).

## Qué pueden hacer las acciones EZ

- Activar combinaciones de teclas vainilla o modificadas.
- Ejecutar comandos de una o varias líneas.
- Equipa conjuntos de equipo grabados usando una coincidencia exacta de elementos (NBT incluido).
- Organizar acciones en paquetes anidados.
- Ocultar paquetes desde la raíz y mantenerlos accesibles mediante la combinación de teclas del paquete.
- Agregue íconos personalizados de `config/ezactions/icons`.
- Crea/edita menús en el juego con arrastrar/soltar y atajos de teclado.
- Permita que otros mods impulsen EZ Actions a través de la API pública.

## ¿Para quién es este wiki?

- Jugadores que quieran una guía de configuración clara sin leer el código fuente.
- Usuarios avanzados que desean detalles de comportamiento avanzados.
- Creadores de modpacks y desarrolladores de mods que desean documentos API completos.

Verá secciones ampliables de "inmersión profunda" en la mayoría de las páginas. Omítelos si solo quieres el flujo práctico.

## Inicio rápido

1. Configure combinaciones de teclas para:
   - @@CÓDIGO1@@
   - @@CÓDIGO2@@
2. Abra el editor y agregue su primera acción.
3. Mantén presionada la tecla radial en el juego y suéltala sobre un corte para ejecutar.
4. Ajuste las imágenes en la pantalla de configuración.

## Navegación

Utilice el navegador izquierdo para ver documentos completos:

- GUI del editor de menú principal
- Acción clave
- Acción de comando
- Acción de equipar el artículo
- Paquetes
- Importación Exportación
- Configuración
-API

??? "Nota técnica"
    EZ Actions está completamente del lado del cliente. No requiere instalación de servidor.

    Las acciones aún dependen de lo que permite el servidor (por ejemplo, permisos de comando).
