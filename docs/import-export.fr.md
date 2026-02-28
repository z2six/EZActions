# Importer Exporter

L’importation/exportation EZ Actions fonctionne via votre presse-papiers.

## Exporter

Dans l'éditeur de menu, cliquez sur `Export`.

Résultat:

- L'arborescence racine complète actuelle est sérialisée en JSON.
- JSON est copié dans le presse-papiers.

## Importer

Dans l'éditeur de menu, cliquez sur `Import`.

Résultat:

- Le Presse-papiers JSON est analysé et validé.
- En cas de succès, les entrées importées sont ajoutées/remplacées par chemin d'importation.

## Messages d'erreur courants

- Le presse-papier est vide
- Le Presse-papiers n'est pas JSON
- La racine JSON n'est pas un tableau
- L'entrée n'est pas un objet / invalide

## Flux de travail pratique

1. Exportez le menu actuel vers un fichier texte comme sauvegarde.
2. Testez les modifications dans JSON.
3. Importer.
4. Si nécessaire, effectuez une restauration en important la sauvegarde précédente.

## Forme JSON

Le niveau supérieur prend en charge un tableau d'éléments de menu (ou un élément unique dans certains chemins d'API).

Chaque élément de menu doit être soit :

- un élément **action** avec l'objet `action`
- un élément **bundle** avec le tableau `children`

### Exemple d'action minimale

```json
{
  "id": "act_123",
  "title": "Inventory",
  "icon": "minecraft:chest",
  "action": {
    "type": "KEY",
    "name": "key.inventory",
    "toggle": false,
    "mode": "AUTO"
  }
}
```

### Minimal Bundle Example

```json
{
  "id": "bundle_abc",
  "title": "Utilities",
  "icon": "minecraft:shulker_box",
  "hideFromMainRadial": false,
  "bundleKeybindEnabled": true,
  "locked": false,
  "children": []
}
```

???+ info "Deep dive: schema details"
    - `title` and `note` accept plain string or text component JSON.
    - `locked` is optional; defaults false.
    - `action.type` currently supports `KEY`, `COMMAND`, `ITEM_EQUIP`.
    - `KEY` fields: `name`, `toggle`, `mode`.
    - `COMMAND` fields: `command`, `delayTicks`, `cycleCommands`.
    - `ITEM_EQUIP` fields: `slots` map with stored item snapshots.
