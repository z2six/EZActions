# Action d'equipement

`Ajouter un equipement` enregistre un snapshot d'equipement et reequipe les items correspondants depuis l'inventaire.

## Slots cibles

- Mainhand
- Offhand
- Helmet
- Chestplate
- Leggings
- Boots

Un slot vide dans l'editeur est ignore a l'execution.

## Creation

1. `Ajouter un equipement`
2. Definir `Title`, `Note`, `Icon`
3. Glisser items de `Source Items` vers `Equip Targets`
4. Sauvegarder

## Source Items inclut

- offhand actuel
- armure equipee
- inventaire principal
- hotbar

## Regles de matching

- Matching via snapshot complet (NBT + metadata), count ignore.
- S'il y a plusieurs matches, prend le stack avec le count le plus eleve.

## Regle Mainhand

`Mainhand` = slot hotbar actuellement selectionne au moment du trigger.

## Execution

- Traitement slot par slot (succes partiel possible).
- Si une autre action d'equipement demarre, l'ancienne est annulee.
- Le mouvement du joueur reste actif pendant l'execution.
