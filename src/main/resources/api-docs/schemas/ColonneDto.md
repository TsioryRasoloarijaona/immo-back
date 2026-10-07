# ColonneDto

Description d'une colonne sélectionnable. Le front construit son sélecteur à partir de cette liste
plutôt que d'en coder une en dur — quand une colonne est ajoutée ou renommée côté serveur, le
sélecteur suit sans redéploiement.

## parDefaut

Vraie pour les colonnes renvoyées quand `?colonnes=` est absent. L'ensemble des colonnes `parDefaut`
reproduit exactement la réponse historique de la liste.

## groupable

Vraie si la colonne peut servir de clé à `?groupBy=` sur l'endpoint `/groupes` de la liste. Le front
pilote sa bande de groupement là-dessus, sans liste en dur.

**Toutes les colonnes ne le sont pas, et c'est délibéré** : un montant porte presque autant de
valeurs distinctes que de lignes (190 prix pour 358 dossiers), et les colonnes calculées par la vue
légataire n'existent pas dans le graphe d'entités. Voir `instructions/FEATURE-groupement-listes.md`
§2.

## agregeable

Vraie si la colonne peut être sommée via `?agregats=` — les bandes « Total : » sous un en-tête de
groupe. Concerne les colonnes MONTANT et NOMBRE.

## fonctions

Fonctions d'agrégat que **cette colonne** accepte dans `?agregats=`, dans l'ordre où le menu Σ doit
les proposer. C'est la liste à piloter pour construire ce menu — aucune ne doit être codée en dur
côté front.

Elle n'est pas la même pour toutes : un montant accepte les cinq, une **date** n'accepte que
`MIN`/`MAX`/`COUNT` (sommer une date n'a pas de sens), une colonne calculée par différence n'accepte
ni `MIN` ni `MAX`, et une colonne sans chemin rend une liste **vide** — elle n'existe pas en base,
il n'y a rien à agréger.

Rappel : un nom nu dans `?agregats=` vaut toujours `SUM`. `agregeable` ci-dessus reste donc vrai
exactement quand `SUM` figure ici.
