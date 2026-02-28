# Import / Export

Dans EZ Actions, `Import` et `Export` utilisent le presse-papiers.

## Export

Dans Menu Editor, cliquer `Export`.

Resultat :

- L'arbre root complet est serialize en JSON.
- JSON copie dans le presse-papiers.

## Import

Dans Menu Editor, cliquer `Import`.

Resultat :

- JSON parse et valide.
- Si valide, applique sur la cible.

## Erreurs frequentes

- Presse-papiers vide
- Presse-papiers non JSON
- Root JSON non array
- Entry non objet / invalide

## Workflow recommande

1. Exporter un backup.
2. Modifier JSON.
3. Importer.
4. En cas de probleme, reimporter le backup.
