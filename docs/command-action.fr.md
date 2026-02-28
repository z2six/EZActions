# Action de commande

Utilisez une action de commande pour envoyer des commandes depuis le radial.

## Champs

- **Titre**
- **Remarque**
- **Commande** (multiligne)
- **Délai multi-commandes (tics)**
- **Commandes de cycle (une par utilisation)**
- **Icône**

## Règles de la boîte de commande

- Une ligne = une commande.
- Le début de `/` est facultatif (EZ Actions le supprime avant l'envoi).
- Les lignes vides sont ignorées.

## Comportement de retard

`Multi-command delay (ticks)` contrôle l'espacement entre les lignes de commande lorsque vous ne faites pas de cycle.

- `0` : envoie des lignes immédiatement.
- `>0` : file d'attente ligne par ligne avec ce délai.

## Commandes de cycle

Si cette option est activée, chaque utilisation radiale envoie exactement une ligne et passe à la suivante.

Exemple:

```text
/time set day
/time set night
```

Use #1 -> day  
Use #2 -> night  
Use #3 -> day

## Cas d'utilisation pratiques

- Commandes utilitaires rapides (`/home`, `/spawn`, `/warp mine`)
- Bascule le jeu de rôle (`/hat`, `/nick`)
- Flux de travail d'administration répartis sur plusieurs lignes

## Remarques

- Il s'agit d'une répartition côté client : les autorisations du serveur s'appliquent toujours.
- Si une nouvelle séquence de commandes démarre, la séquence précédente en file d'attente est remplacée.

???+ info "Plongée approfondie : modèle de séquençage"
    - Les commandes multilignes sans cycle utilisent un séquenceur de ticks client.
    - Le mode Cyclisme stocke un curseur interne dans l'instance d'action.
    - Les modes Cyclisme et Immédiat annulent les séquences en vol avant l'envoi.
