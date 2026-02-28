# API

Cette page couvre l'API publique `org.z2six.ezactions.api` pour EZ Actions 2.0.0.0.

## Ce que l'API permet

Presque tout ce que fait la GUI, plus le controle runtime :

- lire l'arbre menu
- creer/modifier/supprimer/reordonner actions et groupes
- marquer des entrees `locked`
- importer/exporter/valider JSON
- ouvrir editor/config/radial
- ouvrir un radial temporaire runtime sans persistance

## Access

```java
import org.z2six.ezactions.api.EzActions;
import org.z2six.ezactions.api.EzActionsApi;

EzActionsApi api = EzActions.get();
```

## Types d'action

- `KEY`
- `COMMAND`
- `ITEM_EQUIP`

## Note pour modpacks

Si l'API active des keybinds de groupe, un redemarrage client est requis.

## Reference complete

Pour les signatures completes et exemples avances, consulter la page API anglaise.
