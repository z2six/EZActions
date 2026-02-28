# Interface graphique de l'éditeur de menu principal

L'éditeur de menus est votre centre de contrôle pour la création du radial.

Ouvrez-le avec le raccourci clavier `Open editor`.

## Mise en page

- **Panneau de gauche :** créer/modifier/supprimer des actions et des bundles.
- **Panneau de droite :** liste des pages actuelles (racine ou bundle actuel).
- **Filtre en haut à gauche :** filtre par titre, note et texte de type d'action.
- **En bas à gauche :** import/export, config, fermer.

## Ajouter des boutons

- `Add Key Action`
- `Add Command`
- `Add Item Equip`
- `Add Bundle`

## Interaction de liste

### Souris

- **LMB sur l'élément :** sélectionnez.
- **Élément de déplacement LMB :** réorganisation dans la page actuelle.
- **LMB fait glisser l'élément sur la ligne du lot :** déplace l'élément dans ce lot.
- **LMB fait glisser l'élément sur les lignes arrière :** déplace l'élément vers le parent/racine.
- **RMB sur la ligne du bundle :** ouvrez ce bundle.

### Clavier

- `Ctrl + F` focalise la zone de filtre.
- `Enter` modifie la ligne sélectionnée.
- `Delete` ou `Backspace` supprime la ligne sélectionnée.
- `Up Arrow` déplace l'élément sélectionné vers le haut.
- `Down Arrow` déplace l'élément sélectionné vers le bas.

!!! conseil
    Le mouvement du clavier vers le haut/bas est désactivé lorsque le texte filtré est actif, pour éviter les réorganisations ambiguës.

## Types de lignes que vous verrez

- Lignes d'objets normales (actions ou lots)
- Fil d'Ariane (`root/.../bundle`)
- `Back to root`
- `Back to <parent>`

## Entrées verrouillées

Les entrées verrouillées sont protégées des opérations de suppression en jeu.

- Vous ne pouvez pas les supprimer avec la suppression de l'interface graphique.
- Vous ne pouvez pas les supprimer avec les appels de suppression d'API qui honorent le verrouillage.
- Ils peuvent toujours être supprimés en éditant manuellement `config/ezactions/menu.json`.

## Conseils pour une édition rapide

- Utilisez des titres courts pour des étiquettes radiales plus claires.
- Regroupez les actions d'utilitaires partagés (pour moins d'encombrement des racines).
- Utilisez la zone de filtre comme recherche rapide lorsque votre menu devient énorme.

???+ info "Plongée approfondie : comportement du glisser-déposer"
    - La réorganisation utilise une logique d'insertion (pas un simple échange).
    - Le fait de passer dans un bundle s'ajoute à la liste des enfants de ce bundle.
    - Le passage au parent/racine conserve la vue actuelle de votre éditeur au lieu de sauter automatiquement.
    - Toutes les opérations de déplacement/suppression réussies sont conservées immédiatement sur le disque.
