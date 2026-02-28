# API

Esta página documenta a API pública em `org.z2six.ezactions.api` para EZ Actions **2.0.0.0**.

Público:

- desenvolvedores de mods
- desenvolvedores de modpack
- usuários de automação avançada

## O que a API pode fazer

Resposta curta: tudo o que os usuários podem fazer na GUI, além de controle extra de tempo de execução.

- Leia a árvore do menu
- Adicionar/atualizar/remover/reordenar ações e pacotes
- Marcar entradas como bloqueadas
- Importar/exportar/validar menu JSON
- Abrir telas de editor/config/radial
- Abra radial diretamente em um pacote
- Abra radiais de tempo de execução temporários sem persistir
- Entrada de chave de gatilho e sequenciamento de comando
- Inscreva-se em eventos simples de API

## Acesso

```java
import org.z2six.ezactions.api.EzActions;
import org.z2six.ezactions.api.EzActionsApi;

EzActionsApi api = EzActions.get();
```

## Core Concepts

### MenuPath

`MenuPath` endereça pacotes por **cadeia de título de pacote** da raiz.

```java
MenuPath root = MenuPath.root();
MenuPath p = MenuPath.root().child("Utilities").child("Combat");
```

Notes:

- a correspondência de caminho diferencia maiúsculas de minúsculas e literal de título.
- caminho vazio = lista raiz.

### Entradas bloqueadas

`locked=true` significa protegido contra fluxos de exclusão no jogo e remoções de API com reconhecimento de bloqueio.

As entradas bloqueadas ainda podem ser removidas por edições manuais de JSON.

### Modelo de Persistência

A maioria das chamadas de API mutantes persistem imediatamente.

Geralmente você não precisa de uma chamada extra para salvar.

### Rosqueamento

Os retornos de chamada/eventos de API são projetados para uso de thread do cliente.

## Tipos de ação

### Ação-chave

```java
import org.z2six.ezactions.data.click.ClickActionKey;
import org.z2six.ezactions.helper.InputInjector;

ClickActionKey keyAction = new ClickActionKey(
    "key.inventory",                    // mapping id or label
    false,                              // toggle
    InputInjector.DeliveryMode.AUTO     // AUTO/INPUT/TICK
);
```

### Command Action

```java
import org.z2six.ezactions.data.click.ClickActionCommand;

ClickActionCommand cmd = new ClickActionCommand(
    "/time set day\n/time set night", // multi-line
    10,                                 // delay ticks between lines
    true                                // cycleCommands
);
```

### Item Equip Action

```java
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import org.z2six.ezactions.data.click.ClickActionItemEquip;

HolderLookup.Provider regs = Minecraft.getInstance().player.level().registryAccess();
ItemStack stack = Minecraft.getInstance().player.getMainHandItem();

ClickActionItemEquip.StoredItem stored =
    ClickActionItemEquip.StoredItem.fromStack(stack, regs);

ClickActionItemEquip eq = new ClickActionItemEquip(
    java.util.Map.of(ClickActionItemEquip.TargetSlot.MAINHAND, stored)
);
```

## Icons

```java
import org.z2six.ezactions.data.icon.IconSpec;

IconSpec itemIcon = IconSpec.item("minecraft:ender_pearl");
IconSpec customIcon = IconSpec.custom("custom:my_icon");
```

## Top-Level EzActionsApi

| Método | Finalidade |
|---|---|
| @@CÓDIGO0@@ | Abra a tela do editor do jogo |
| @@CÓDIGO1@@ | Abrir tela de configuração |
| @@CÓDIGO2@@ | Radial de raiz aberta |
| @@CÓDIGO3@@ | Abra radial no ID do pacote |
| @@CÓDIGO4@@ | Abra o radial de tempo de execução único do JSON |
| @@CÓDIGO5@@ | API herdada de ação de adição direta |
| @@CÓDIGO6@@ | API legada de adição direta de pacote |
| @@CÓDIGO7@@ | Remoção herdada por id |
| @@CÓDIGO8@@ | Movimento legado no pai/raiz |
| @@CÓDIGO9@@ | Força persistir |
| @@CÓDIGO10@@ | Importação de área de transferência estilo GUI |
| @@CÓDIGO11@@ | Exportação de área de transferência estilo GUI |
| @@CÓDIGO12@@ | Superfície somente leitura |
| @@CÓDIGO13@@ | Superfície mutante |
| @@CÓDIGO14@@ | Superfície de importação/exportação JSON |
| @@CÓDIGO15@@ | Superfície auxiliar de entrada + comando |
| @@CÓDIGO16@@ | Superfície auxiliar UI/tempo de execução |
| @@CÓDIGO17@@ | Ganchos para eventos |

## MenuLeia

Interface: `MenuRead`

- @@CÓDIGO0@@
- @@CÓDIGO1@@
- @@CÓDIGO2@@
- @@CÓDIGO3@@

### Campos de instantâneo do ApiMenuItem

Noções básicas:

- `id`, `title`, `note`
- `isCategory`, `typeLabel`
- `iconKind`, `iconId`
- `hideFromMainRadial`, `bundleKeybindEnabled`, `locked`

Detalhes da ação:

- @@CÓDIGO0@@
- @@CÓDIGO1@@
- ação principal: `keyMappingName`, `keyToggle`, `keyMode`
- ação de comando: `commandRaw`, `commandDelayTicks`, `commandCycleCommands`
- ação de equipar item: `itemEquipSlotsJson`

##MenuEscrever

Interface: `MenuWrite`

### Criar

- @@CÓDIGO0@@
- @@CÓDIGO1@@
- @@CÓDIGO2@@
- @@CÓDIGO3@@

### Mover

- @@CÓDIGO0@@
- @@CÓDIGO1@@

### Remover

- @@CÓDIGO0@@
- @@CÓDIGO1@@

### Atualizar

- @@CÓDIGO0@@
- @@CÓDIGO1@@
- @@CÓDIGO2@@
- @@CÓDIGO3@@

### Ajudantes Estruturais

- `ensureBundles(path)` cria uma cadeia de pacotes ausentes por título.
- `upsertFromJson(path, jsonObjectOrArray)` adiciona/substitui itens do snippet JSON.

### Exemplo: Crie um pacote de utilitários bloqueado

```java
var write = EzActions.get().menuWrite();

MenuPath root = MenuPath.root();
String bundleId = write.addBundle(
    root,
    "Utilities",
    "Pack-defined utilities",
    IconSpec.item("minecraft:shulker_box"),
    false,   // hideFromMainRadial
    true,    // bundleKeybindEnabled
    true     // locked
).orElseThrow();

write.addAction(
    root.child("Utilities"),
    "Open Inventory",
    "Quick inventory",
    IconSpec.item("minecraft:chest"),
    new ClickActionKey("key.inventory", false, InputInjector.DeliveryMode.AUTO),
    true
);
```

## ImportExport

Interface: `ImportExport`

- @@CÓDIGO0@@
- @@CÓDIGO1@@
- @@CÓDIGO2@@
- @@CÓDIGO3@@
- @@CÓDIGO4@@

### Regras de validação (alto nível)

- root deve ser objeto ou array
- cada item deve conter exatamente um dos seguintes:
  - @@CÓDIGO0@@
  - @@CÓDIGO1@@
- o objeto de ação deve incluir `type` válido
- booleanos opcionais (`hideFromMainRadial`, `bundleKeybindEnabled`, `locked`) devem ser booleanos quando presentes

## Operações de entrada

Interface: `InputOps`

- @@CÓDIGO0@@
- @@CÓDIGO1@@

Exemplo:

```java
var input = EzActions.get().inputOps();

input.deliver("key.inventory", false, InputOps.Mode.AUTO);
input.enqueueCommands(new String[]{"/time set day", "/weather clear"}, 10);
```

## EditorOps

Interface: `EditorOps`

- @@CÓDIGO0@@
- @@CÓDIGO1@@
- @@CÓDIGO2@@
- @@CÓDIGO3@@
- @@CÓDIGO4@@

## Estilo radial temporário dinâmico

Classe: `DynamicRadialStyle`

Todos os campos são substituições anuláveis ​​opcionais.

Cores:

- @@CÓDIGO0@@
- @@CÓDIGO1@@
- @@CÓDIGO2@@
- @@CÓDIGO3@@

Animação:

- @@CÓDIGO0@@
- @@CÓDIGO1@@
- @@CÓDIGO2@@
- @@CÓDIGO3@@
- @@CÓDIGO4@@
- `openStyle` (`WIPE|FADE|NONE`)
- `openDirection` (`CW|CCW`)
- `hoverStyle` (`FILL_SCALE|FILL_ONLY|SCALE_ONLY|NONE`)

Projeto:

- @@CÓDIGO0@@
- @@CÓDIGO1@@
- @@CÓDIGO2@@
- @@CÓDIGO3@@
- @@CÓDIGO4@@
- @@CÓDIGO5@@
- `designStyle` (`SOLID|SEGMENTED|OUTLINE|GLASS`)

### Exemplo: Temporário Radial

```java
String json = """
[
  {
    "id": "tmp_inv",
    "title": "Inventory",
    "icon": "minecraft:chest",
    "action": { "type": "KEY", "name": "key.inventory", "toggle": false, "mode": "AUTO" }
  },
  {
    "id": "tmp_day",
    "title": "Day",
    "icon": "minecraft:sunflower",
    "action": { "type": "COMMAND", "command": "/time set day", "delayTicks": 0, "cycleCommands": false }
  }
]
""";

DynamicRadialStyle style = new DynamicRadialStyle(
    0xAA000000, 0xFFF20044, 0x66FFFFFF, 0xFFFFFFFF,
    true, true, true,
    125, 0.05,
    "WIPE", "CW", "FILL_SCALE",
    18, 72, 28,
    8, 6, 0,
    "SOLID"
);

EzActions.get().editorOps().openTemporaryRadial(json, style);
```

## Events

Classe: `ApiEvents`

- @@CÓDIGO0@@
- @@CÓDIGO1@@

Cargas úteis:

- `MenuChanged.path`, `MenuChanged.reason`
- `ImportEvent.target`, `ImportEvent.json`, `ImportEvent.count`

Exemplo:

```java
var events = EzActions.get().events();

events.onMenuChanged(evt -> {
    System.out.println("Menu changed: " + evt.reason + " at " + evt.path);
});

events.onImported(evt -> {
    System.out.println("Imported " + evt.count + " entries into " + evt.target);
});
```

## JSON Item Schema (API + Import/Export)

```json
{
  "id": "string",
  "title": "string or text component",
  "note": "string or text component",
  "icon": "minecraft:item_id",
  "hideFromMainRadial": false,
  "bundleKeybindEnabled": false,
  "locked": false,
  "action": {
    "type": "KEY | COMMAND | ITEM_EQUIP"
  },
  "children": []
}
```

Rules:

- use `action` para itens de ação
- use `children` para pacotes
- não inclua ambos no mesmo objeto

## Padrão de interoperabilidade estilo KubeJS

A sintaxe exata depende da configuração do KubeJS, mas o fluxo geralmente é:

Primeiro, carregue a classe API Java
2. obtenha singleton via `EzActions.get()`
3. chame os métodos `menuWrite()` / `editorOps()`

Pseudo-fluxo:

```js
// pseudo example, adapt to your exact loader bridge
const EzActions = Java.loadClass('org.z2six.ezactions.api.EzActions')
const api = EzActions.get()

// use api.menuWrite(), api.importExport(), api.editorOps(), etc.
```

## API Notes For Pack Authors

- Se as gravações da sua API permitirem atalhos de teclado de pacote, os usuários ainda precisarão reiniciar para registro de atalhos de teclado.
- EZ Actions agora notifica os usuários no chat quando a reinicialização é necessária para novos atalhos de teclado.
- Mantenha IDs/títulos estáveis ​​se você planeja corrigir menus ao longo do tempo.

???+ aviso "Nota de compatibilidade"
    As assinaturas da API podem mudar em versões futuras. Esta página corresponde ao comportamento 2.0.0.0.