# Forfaits

Les bundles sont des dossiers (catégories) à l’intérieur de l’arborescence radiale.

Utilisez-les pour regrouper les actions par contexte : combat, construction, utilitaire, jeu de rôle, etc.

## Champs de regroupement

- **Titre** (également utilisé comme identifiant interne)
- **Remarque** (facultatif)
- **Icône**
- **Masquer du radial principal**
- **Activer le raccourci clavier**

## Bundle de raccourcis clavier

Si `Enable keybind` est activé, EZ Actions enregistre une combinaison de touches dédiée pour cet ensemble.

!!! avertissement "Redémarrage requis"
    L’enregistrement du raccourci clavier du bundle est appliqué au prochain redémarrage du client.

EZ Actions affiche un message client lorsqu'un redémarrage est nécessaire (y compris les bundles créés par l'API).

## Masquer du radial principal

Si activé :

- Le bundle est masqué sur la page radiale racine.
- Le bundle existe toujours dans le modèle de menu.
- Le bundle peut toujours être ouvert via l'API ou le raccourci clavier du bundle.

Idéal pour les "pages avancées", vous ne voulez pas encombrer la racine.

## Bundles imbriqués

Les lots peuvent contenir :

- les actions clés
- actions de commande
- actions d'équipement d'objets
- plus de forfaits

## meilleures pratiques

- Gardez la racine petite et hautement prioritaire.
- Placez les actions à basse fréquence dans des groupes plus profonds.
- Donnez aux bundles des icônes claires et des noms courts.

## Lots verrouillés

Un bundle peut être marqué `locked` (généralement via API ou JSON).

- Les chemins de suppression dans le jeu ne le supprimeront pas.
- Les modifications manuelles JSON peuvent toujours le supprimer.

???+ info "Plongée profonde : identité et unicité"
    Le titre du bundle est utilisé comme identifiant du bundle dans les flux de travail de l'éditeur.

Les noms de bundle en double peuvent provoquer une ambiguïté, de sorte que l'éditeur bloque les enregistrements de titre/identifiant de bundle en double.
