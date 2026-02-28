# Editeur du menu principal

L'editeur de menu est le centre de controle de ton radial.

Ouvre-le avec la touche `Open editor`.

## Structure

- **Panneau gauche :** creer, modifier, supprimer actions et groupes.
- **Panneau droit :** liste de la page actuelle (root ou groupe courant).
- **Filter en haut a gauche :** filtre par `Title`, `Note` et type d'action.
- **Bas gauche :** `Import`, `Export`, `Configuration`, `Close`.

## Boutons d'ajout

- `Ajouter une action de touche`
- `Ajouter une commande`
- `Ajouter un equipement`
- `Ajouter un groupe`

## Interaction liste

### Souris

- **LMB sur item :** selection.
- **LMB drag :** reordonner dans la page courante.
- **Drag sur un groupe :** deplacer dans ce groupe.
- **Drag sur lignes retour :** deplacer vers parent/root.
- **RMB sur groupe :** ouvrir le groupe.

### Clavier

- `Ctrl + F` : focus sur `Filter`.
- `Enter` : modifier la ligne selectionnee.
- `Delete` ou `Backspace` : supprimer la selection.
- `Up Arrow` : monter.
- `Down Arrow` : descendre.

!!! tip
    Le deplacement au clavier est desactive quand le filtre contient du texte.

## Types de lignes

- lignes normales (action ou groupe)
- ligne breadcrumb (`root/.../bundle`)
- `Back to root`
- `Back to <parent>`

## Entrees verrouillees (locked)

Une entree `locked` ne peut pas etre supprimee en jeu.

- Pas de suppression via GUI.
- Pas de suppression via API lock-aware.
- Suppression manuelle possible via `config/ezactions/menu.json`.
