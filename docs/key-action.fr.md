# Action clé

Utilisez une action clé pour déclencher un raccourci clavier existant (vanille ou moddé) à partir du radial.

## Champs

- **Titre** : ce que vous voyez dans l'éditeur/étiquette radiale.
- **Remarque** : texte d'aide facultatif.
- **Nom du mappage** : identifiant ou étiquette du mappage de clé.
- **Livraison** : `AUTO`, `INPUT` ou `TICK`.
- **Toggle** : basculez l'état de la touche au lieu d'appuyer.
- **Icône** : cliquez sur la zone d'icône pour ouvrir le sélecteur d'icônes.

## Configuration recommandée

1. Cliquez sur `Pick from Keybinds`.
2. Sélectionnez la reliure dans le sélecteur (plus sûr que de taper à la main).
3. Continuez la livraison sur `AUTO` sauf si vous avez une raison spécifique.
4. Enregistrez.

## Modes de livraison

| Mode | Ce qu'il fait | Quand utiliser |
|---|---|---|
| `AUTO` | Sélectionne automatiquement le meilleur chemin | Par défaut pour presque tout le monde |
| `INPUT` | Injecte la pression/relâchement de la touche via le pipeline d'entrée | Si une liaison ne répond pas dans `AUTO` |
| `TICK` | Définit la touche vers le bas/vers le haut via les mises à jour de l'état des clés | Solution de secours utile pour les clés difficiles à injecter |

## Basculer

- `OFF` : un robinet par utilisation radiale.
- `ON` : retourne la clé vers le bas/vers le haut à chaque utilisation.

Utile pour des actions telles que les bascules de style sprint/sneak, en fonction du comportement de la combinaison de touches cible.

## Problèmes courants

- **Rien ne se passe :** vérifiez l'identifiant de mappage ou choisissez à nouveau dans la liste des raccourcis clavier.
- **Déclencheurs de raccourcis clavier incorrects :** votre nom saisi correspond à une autre étiquette de mappage ; utiliser le sélecteur.
- **Fonctionne en mode solo mais pas sur serveur :** l'action cible peut être restreinte au serveur.

???+ info "Plongée approfondie : comment fonctionne la livraison des clés"
    En interne, EZ Actions résout les noms de mappage en :

1. correspondance exacte des clés de traduction
    2. correspondance exacte des étiquettes localisées
    3. La solution de secours contient une correspondance

Pour les touches requises par le modificateur (Ctrl/Shift/Alt), il utilise une logique d'injection supplémentaire. `AUTO` peut changer de stratégie selon qu'une touche est liée, possède un scancode ou nécessite des modificateurs.
