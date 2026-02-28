# Wiki EZ Actions

!!! Astuce "Langue / 语言"
    Utilisez l'icône **changeur de langue** dans la barre de navigation en haut à droite pour changer la langue du wiki.

    使用右上角导航栏中的**语言切换**图标来切换 Wiki 语言。

EZ Actions est un mod Minecraft côté client qui vous offre un menu radial rapide pour les actions que vous utilisez tout le temps.

Considérez-le comme votre « roue rapide de combat + utilitaire » : une touche enfoncée, un coup, c'est fait.

!!! avertissement "Portée de la version"
    Ce wiki est écrit pour **EZ Actions 2.0.0.0**.

    Si vous utilisez une version plus récente, certaines fonctionnalités et détails de l'interface utilisateur peuvent avoir changé.

???+ infos "TLDR"
    - Créez votre propre menu radial avec **Actions clés**, **Actions de commande**, **Actions d'équipement d'objet** et **Bundles**.
    - Stylisez-le avec des configurations couleur/conception/animation.
    - Menu import/export JSON pour partage et sauvegardes.
    - Les développeurs de modules peuvent tout contrôler via l'API (y compris les radiales d'exécution temporaires).

## Ce que les actions EZ peuvent faire

- Déclenchez des raccourcis clavier vanille ou modifiés.
- Exécutez des commandes sur une ou plusieurs lignes.
- Équipez les ensembles d'équipements enregistrés en utilisant la correspondance exacte des éléments (NBT inclus).
- Organisez les actions en groupes imbriqués.
- Masquez les bundles à la racine tout en les gardant accessibles via le raccourci clavier du bundle.
- Ajoutez des icônes personnalisées de `config/ezactions/icons`.
- Créez/modifiez des menus dans le jeu avec glisser/déposer et raccourcis clavier.
- Laissez les autres mods piloter EZ Actions via l'API publique.

## À qui s'adresse ce wiki

- Les joueurs qui souhaitent un guide de configuration clair sans lire le code source.
- Utilisateurs expérimentés qui souhaitent des détails avancés sur le comportement.
- Créateurs de modpacks et développeurs de mods qui souhaitent une documentation API complète.

Vous verrez des sections « approfondies » extensibles dans la plupart des pages. Ignorez-les si vous voulez juste le déroulement pratique.

## Démarrage rapide

1. Définissez les raccourcis clavier pour :
   - `Open radial menu`
   - `Open editor`
2. Ouvrez l'éditeur et ajoutez votre première action.
3. Maintenez votre clé radiale dans le jeu et relâchez-la sur une tranche pour exécuter.
4. Ajustez les visuels dans l'écran de configuration.

## Navigation

Utilisez la navigation de gauche pour les documents complets :

- Interface graphique de l'éditeur de menu principal
-Action clé
- Action de commande
- Action d'équipement d'objet
- Forfaits
- Importer Exporter
-Configuration
-API

??? "Note technique"
    EZ Actions est entièrement côté client. Il ne nécessite pas d'installation de serveur.

    Les actions dépendent toujours de ce que le serveur autorise (par exemple les autorisations de commande).
