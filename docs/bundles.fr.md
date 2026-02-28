# Groupes (Bundle)

Un `groupe` est un dossier/categorie dans l'arbre radial.

## Champs du groupe

- **Title**
- **Note** (optionnel)
- **Icon**
- **Hide from main radial**
- **Enable keybind**

## Keybind de groupe

Si `Enable keybind` est actif, EZ Actions enregistre un keybind dedie pour ce groupe.

!!! warning "Redemarrage requis"
    L'enregistrement devient effectif au prochain redemarrage client.

## Hide from main radial

Si active :

- Le groupe est cache sur la page root.
- Le groupe reste present dans le modele.
- Le groupe reste accessible via API ou keybind de groupe.

## Groupes verrouilles (locked)

`locked` protege contre suppression en jeu.

- Non supprimable via GUI.
- Suppression manuelle JSON toujours possible.
