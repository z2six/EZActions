#Configuration

Ouvrez la configuration depuis l'éditeur de menu à l'aide du bouton `Config`.

L'interface utilisateur de configuration comporte trois sections :

- Général
- Animations
- Conception

## Général

| Paramètre | Tapez | Plage / Valeurs | Par défaut |
|---|---|---|---|
| Déplacer pendant l'ouverture radiale | booléen | MARCHE/ARRÊT | SUR |
| Afficher l'étiquette de survol | booléen | MARCHE/ARRÊT | SUR |
| Lignes visibles de l'éditeur de commandes | entier | 1-20 | 5 |

## Animations

| Paramètre | Tapez | Plage / Valeurs | Par défaut |
|---|---|---|---|
| Animations activées | booléen | MARCHE/ARRÊT | SUR |
| Animation Ouvrir/Fermer | booléen | MARCHE/ARRÊT | SUR |
| Animation de survol | booléen | MARCHE/ARRÊT | SUR |
| Style ouvert | énumération | `WIPE`, `FADE`, `NONE` | `WIPE` |
| Itinéraire | énumération | `CW`, `CCW` | `CW` |
| Style de survol | énumération | `FILL_SCALE`, `FILL_ONLY`, `SCALE_ONLY`, `NONE` | `FILL_SCALE` |
| Survolez le pourcentage de croissance | double | 0,0-0,5 | 0,05 |
| Durée d'ouverture/fermeture | entier (ms) | 0-2000 | 125 |

## Conception

| Paramètre | Tapez | Plage / Valeurs | Par défaut |
|---|---|---|---|
| Zone morte | entier | 0-90 | 18 |
| Rayon extérieur | entier | 24-512 | 72 |
| Épaisseur de l'anneau | entier | 6-256 | 28 |
| Seuil de démarrage de l'échelle | entier | 0-128 | 8 |
| Échelle par article | entier | 0-100 | 6 |
| Écart de tranche | int (degrés) | 0-12 | 0 |
| Style de conception | énumération | `SOLID`, `SEGMENTED`, `OUTLINE`, `GLASS` | `SOLID` |
| Couleur de la bague | ARVB int | 32 bits signé en entier | `0xAA000000` |
| Couleur de survol | ARVB int | 32 bits signé en entier | `0xFFF20044` |
| Couleur de la bordure | ARVB int | 32 bits signé en entier | `0x66FFFFFF` |
| Couleur du texte | ARVB int | 32 bits signé en entier | `0xFFFFFFFF` |

## Aperçu

`Preview` ouvre un écran d'aperçu radial et boucle l'animation avec une pause d'une seconde entre les boucles.

Utilisez-le pour régler les paramètres visuels avant de vous engager.

## Enregistrer le comportement

- Enregistrer écrit les valeurs immédiatement dans les fichiers de configuration.
- Lors de l'enregistrement, l'écran revient à l'écran de l'éditeur parent.

## Fichiers de configuration

- `config/ezactions/general-client.toml`
- `config/ezactions/anim-client.toml`
- `config/ezactions/design-client.toml`

???+ info "Plongée approfondie : format couleur"
    Les couleurs sont stockées sous forme d’entiers ARGB 32 bits signés.

Exemple:

- `0xAARRGGBB`
    - alpha `AA`, puis rouge `RR`, vert `GG`, bleu `BB`

Les nombres décimaux négatifs sont normaux pour les valeurs ARGB opaques.
