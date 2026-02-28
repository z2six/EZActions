#API

Cette page documente l'API publique dans `org.z2six.ezactions.api` pour EZ Actions **2.0.0.0**.

Public:

- les développeurs de modules
- les développeurs de modules
- utilisateurs avancés d'automatisation

## Ce que l'API peut faire

Réponse courte : tout ce que les utilisateurs peuvent faire dans l'interface graphique, plus un contrôle d'exécution supplémentaire.

- Lire l'arborescence du menu
- Ajouter/mettre à jour/supprimer/réorganiser les actions et les bundles
- Marquer les entrées comme verrouillées
- Menu Importer/Exporter/Valider JSON
- Ouvrir les écrans éditeur/config/radial
- Ouvrir le radial directement au niveau d'un paquet
- Ouvrir des radiales d'exécution temporaires sans persister
- Saisie des touches de déclenchement et séquencement des commandes
- Abonnez-vous à des événements API simples

## Accéder

```java
import org.z2six.ezactions.api.EzActions;
import org.z2six.ezactions.api.EzActionsApi;

EzActionsApi api = EzActions.get();
```

## Core Concepts

### Chemin du Menu

`MenuPath` adresse les bundles par **chaîne de titres de bundle** à partir de la racine.

```java
MenuPath root = MenuPath.root();
MenuPath p = MenuPath.root().child("Utilities").child("Combat");
```

Notes:

- La correspondance du chemin est sensible à la casse et au titre littéral.
- chemin vide = liste racine.

### Entrées verrouillées

`locked=true` signifie protégé contre les flux de suppression dans le jeu et les suppressions d'API prenant en compte le verrouillage.

Les entrées verrouillées peuvent toujours être supprimées par des modifications JSON manuelles.

### Modèle de persistance

La plupart des appels d'API en mutation persistent immédiatement.

Vous n’avez généralement pas besoin d’un appel de sauvegarde supplémentaire.

### Fil de discussion

Les rappels/événements d'API sont conçus pour l'utilisation du thread client.

## Types d'actions

### Action clé

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

| Méthode | Objectif |
|---|---|
| `openEditor(Screen)` | Ouvrir l'écran de l'éditeur dans le jeu |
| `openConfig(Screen)` | Ouvrir l'écran de configuration |
| `openRadial()` | Radial à racine ouverte |
| `openRadialAtBundle(String)` | Ouvrir le radial à l'identifiant du bundle |
| `openTemporaryRadial(String, DynamicRadialStyle)` | Ouvrir un runtime radial unique à partir de JSON |
| `addAction(...)` | Ancienne API d'action d'ajout direct |
| `addBundle(...)` | Ancienne API d'ajout direct de bundle |
| `removeItem(String)` | Héritage supprimé par identifiant |
| `moveWithin(String,int,int)` | Déplacement hérité dans parent/racine |
| `persist()` | Forcer persister |
| `importFromClipboard()` | Importation de presse-papiers de style GUI |
| `exportToClipboard()` | Exportation du presse-papiers de style GUI |
| `menuRead()` | Surface en lecture seule |
| `menuWrite()` | Surface en mutation |
| `importExport()` | Surface d'importation/exportation JSON |
| `inputOps()` | Surface d'assistance de saisie + commande |
| `editorOps()` | Surface d'assistance de l'interface utilisateur/d'exécution |
| `events()` | Crochets d'événement |

## MenuLire

Interface : `MenuRead`

- `list(MenuPath path)`
- `findById(String id)`
- `currentPath()`
- `existsPath(MenuPath path)`

### Champs d'instantané ApiMenuItem

Bases :

- `id`, `title`, `note`
- `isCategory`, `typeLabel`
- `iconKind`, `iconId`
- `hideFromMainRadial`, `bundleKeybindEnabled`, `locked`

Détails de l'action :

- `actionType`
- `actionJson`
- action clé : `keyMappingName`, `keyToggle`, `keyMode`
- action de commande : `commandRaw`, `commandDelayTicks`, `commandCycleCommands`
- action d'équipement d'objet : `itemEquipSlotsJson`

## MenuÉcrire

Interface : `MenuWrite`

### Créer

- `addAction(path, title, note, action, locked)`
- `addAction(path, title, note, icon, action, locked)`
- `addBundle(path, title, note, hideFromMainRadial, bundleKeybindEnabled, locked)`
- `addBundle(path, title, note, icon, hideFromMainRadial, bundleKeybindEnabled, locked)`

### Se déplacer

- `moveWithin(path, fromIndex, toIndex)`
- `moveTo(itemId, targetBundle)`

### Retirer

- `removeFirst(path, predicate)`
- `removeById(id)`

### Mise à jour

- `updateMeta(id, titleOrNull, noteOrNull, iconOrNull)`
- `replaceAction(id, action)`
- `setBundleFlags(id, hideFromMainRadial, bundleKeybindEnabled)`
- `setLocked(id, locked)`

### Aides structurelles

- `ensureBundles(path)` crée une chaîne de bundles manquante par titre.
- `upsertFromJson(path, jsonObjectOrArray)` ajouter/remplacer des éléments de l'extrait JSON.

### Exemple : Créer un ensemble d'utilitaires verrouillé

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

Interface : `ImportExport`

- `exportAllJson()`
- `exportBundleJson(path)`
- `importInto(path, json)`
- `replaceAll(json)`
- `validate(json)`

### Règles de validation (haut niveau)

- la racine doit être un objet ou un tableau
- chaque élément doit contenir exactement l'un des éléments suivants :
  - `action`
  - `children`
- l'objet d'action doit inclure un `type` valide
- les booléens facultatifs (`hideFromMainRadial`, `bundleKeybindEnabled`, `locked`) doivent être des booléens lorsqu'ils sont présents

## Opérations d'entrée

Interface : `InputOps`

- `deliver(mappingNameOrLabel, toggle, mode)`
- `enqueueCommands(commands, perLineDelayTicks)`

Exemple:

```java
var input = EzActions.get().inputOps();

input.deliver("key.inventory", false, InputOps.Mode.AUTO);
input.enqueueCommands(new String[]{"/time set day", "/weather clear"}, 10);
```

## EditorOps

Interface : `EditorOps`

- `openEditor()`
- `openConfig()`
- `openRadial()`
- `openRadialAtBundle(bundleId)`
- `openTemporaryRadial(jsonItemOrArray, styleOrNull)`

## Style radial temporaire dynamique

Classe : `DynamicRadialStyle`

Tous les champs sont des remplacements facultatifs nullables.

Couleurs:

- `ringColor`
- `hoverColor`
- `borderColor`
- `textColor`

Animation:

- `animationsEnabled`
- `animOpenClose`
- `animHover`
- `openCloseMs`
- `hoverGrowPct`
- `openStyle` (`WIPE|FADE|NONE`)
- `openDirection` (`CW|CCW`)
- `hoverStyle` (`FILL_SCALE|FILL_ONLY|SCALE_ONLY|NONE`)

Conception:

- `deadzone`
- `baseOuterRadius`
- `ringThickness`
- `scaleStartThreshold`
- `scalePerItem`
- `sliceGapDeg`
- `designStyle` (`SOLID|SEGMENTED|OUTLINE|GLASS`)

### Exemple : Radial d'exécution temporaire

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

Classe : `ApiEvents`

- `onMenuChanged(Consumer<MenuChanged>)`
- `onImported(Consumer<ImportEvent>)`

Charges utiles :

- `MenuChanged.path`, `MenuChanged.reason`
- `ImportEvent.target`, `ImportEvent.json`, `ImportEvent.count`

Exemple:

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

- utilisez `action` pour les éléments d'action
- utilisez `children` pour les bundles
- ne pas inclure les deux dans le même objet

## Modèle d'interopérabilité de style KubeJS

La syntaxe exacte dépend de votre configuration KubeJS, mais le flux est généralement :

1. Charger la classe API Java
2. obtenez un singleton via `EzActions.get()`
3. appeler les méthodes `menuWrite()` / `editorOps()`

Pseudo-flux :

```js
// pseudo example, adapt to your exact loader bridge
const EzActions = Java.loadClass('org.z2six.ezactions.api.EzActions')
const api = EzActions.get()

// use api.menuWrite(), api.importExport(), api.editorOps(), etc.
```

## API Notes For Pack Authors

- Si votre API écrit pour activer les raccourcis clavier, les utilisateurs doivent toujours redémarrer pour l'enregistrement des raccourcis clavier.
- EZ Actions avertit désormais les utilisateurs dans le chat lorsque le redémarrage est requis pour les nouvelles combinaisons de touches du bundle.
- Conservez des identifiants/titres stables si vous prévoyez de corriger les menus au fil du temps.

???+ avertissement "Note de compatibilité"
    Les signatures API peuvent changer dans les versions futures. Cette page correspond au comportement 2.0.0.0.