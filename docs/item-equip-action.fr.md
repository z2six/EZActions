# Action d'équipement d'objet

L'équipement d'objet vous permet d'enregistrer un instantané d'équipement et de rééquiper les objets correspondants de votre inventaire.

## Emplacements cibles

Vous pouvez attribuer n'importe lequel de ces éléments :

- Main main
- Désinvolte
- Casque
- Plastron
-Leggings
- Bottes

Si un emplacement reste vide dans l'éditeur, EZ Actions l'ignore lors de l'exécution.

## Comment en créer un

1. Cliquez sur `Add Item Equip`.
2. Définissez le titre/la note/l'icône.
3. Faites glisser les éléments de la grille source vers les emplacements cibles.
4. Enregistrez.

### La grille source comprend

- objet secondaire actuel
- armure équipée
- inventaire principal
- barre de raccourcis

## Pendant l'exécution

Lorsque vous déclenchez l'action depuis radial :

1. EZ Actions vérifie chaque emplacement cible configuré.
2. Si la cible correspond déjà à l'élément enregistré, elle est ignorée.
3. Sinon, il trouve la meilleure pile source correspondante.
4. Il échange les éléments dans l'emplacement cible.

Il traite slot par slot et permet un succès partiel.

## Règles de correspondance importantes

- La correspondance est basée sur la signature d'instantané de pile complète (NBT et métadonnées), ignorant le nombre.
- S'il existe plusieurs piles correspondantes, il sélectionne celle avec le nombre le plus élevé.

## Règle principale

`Mainhand` signifie l'emplacement de votre barre de raccourcis actuellement sélectionné au moment du déclenchement.

## Comportement sous pression

- Si vous déclenchez une deuxième action d'équipement d'objet alors qu'une autre est en cours d'exécution, l'ancienne est annulée et remplacée.
- Les entrées de mouvement et de jeu restent actives pendant que l'exécution s'exécute en arrière-plan.

## Contrôles rapides dans l'éditeur

- Glisser LMB de l'emplacement source vers l'emplacement cible : attribuer un élément.
- RMB sur l'emplacement cible : affectation claire.
- `Refresh Items` : reconstruire la liste des sources à partir de l'état actuel de l'inventaire des joueurs.

???+ info "Plongée approfondie : ordre d'exécution des emplacements"
    L'ordre d'exécution actuel est :

1. Casque
    2. Plastron
    3. Jambières
    4. Bottes
    5. Désinvolte
    6. Main main

Les cibles vides et les correspondances de source manquantes sont ignorées et ne sont pas traitées comme un échec matériel.
