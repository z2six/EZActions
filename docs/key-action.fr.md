# Action de touche

`Action de touche` declenche un keybind existant (vanilla ou modde) depuis le radial.

## Champs

- **Title**
- **Note**
- **Mapping Name**
- **Delivery** : `AUTO`, `INPUT`, `TICK`
- **Toggle**
- **Icon**

## Procedure recommandee

1. Cliquer `Pick from Keybinds`.
2. Choisir le binding dans la liste.
3. Garder `AUTO` par defaut.
4. Sauvegarder.

## Modes Delivery

| Mode | Fonction | Usage |
|---|---|---|
| `AUTO` | Choisit automatiquement la meilleure methode | Defaut |
| `INPUT` | Injection press/release via input pipeline | Si `AUTO` ne repond pas |
| `TICK` | Controle etat de touche par tick | Fallback |

## Toggle

- `OFF` : un tap par utilisation.
- `ON` : alterne down/up a chaque utilisation.

## Problemes frequents

- **Rien ne se passe :** verifier mapping id, rechoisir via picker.
- **Mauvaise touche :** eviter saisie manuelle, utiliser picker.
- **SP ok / serveur non :** restrictions ou permissions serveur.
