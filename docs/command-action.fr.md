# Action de commande

`Action de commande` envoie des commandes depuis le radial.

## Champs

- **Title**
- **Note**
- **Command** (multiligne)
- **Multi-command delay (ticks)**
- **Cycle commands (one per use)**
- **Icon**

## Regles du champ Command

- Une ligne = une commande.
- Le prefixe `/` est optionnel.
- Les lignes vides sont ignorees.

## Delay

`Multi-command delay (ticks)` definit l'intervalle entre lignes en mode non-cyclique.

- `0` : envoi immediat.
- `>0` : envoi ligne par ligne avec delai.

## Cycle commands

Si active, chaque utilisation envoie une seule ligne et passe a la suivante.

```text
/time set day
/time set night
```

Use 1 -> day  
Use 2 -> night  
Use 3 -> day

## Notes

- Dispatch cote client, permissions serveur toujours appliquees.
- Une nouvelle sequence remplace la sequence en cours.
