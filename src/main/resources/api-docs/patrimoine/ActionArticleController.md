# ActionArticleController

Panneau « Actions » d'une fiche article (règle OMRAN GEN0005) : actions, notes et alertes enregistrées dans
`ComActionMessage` avec `StkItemId` = l'article. Lecture : tout utilisateur authentifié ; écriture : rôle
`Administrateur`. **Programme et tranche n'ont pas de panneau Actions** : `ComActionMessage` n'a aucune colonne
programme ni tranche.

## lister

Toutes les actions rattachées à l'article (`StkItemId`), y compris celles saisies depuis une prospection ou un dossier
qui citent l'article, avec la visibilité par entité du suivi des actions mais sans sa portée « mes actions ». Lignes
au format de `GET /api/suivi-actions` (colonnes par défaut). Tri par défaut : `docDate` décroissante.

## creer

Corps identique à une interaction de prospection. Règles GEN0005 : `actionTypeId` obligatoire ; action « Note » ⇒
`description` obligatoire ; autre action ⇒ `resultatId` obligatoire ; résultat « avec alerte » ⇒ `dateAlerte`
obligatoire ; résultat de rendez-vous ⇒ `dateRdv` obligatoire (un `AdmEvent` est créé). Catégorie : celle du résultat,
sinon « Prospection ». Auteur, date, `UserName` et libellé « jj/MM/aaaa HH:mm Nom » posés par le serveur. Une
`dateAlerte` pose une alerte (`AdmAlertData`) et **désactive** les alertes des autres actions propres de l'article
(sans prospection ni dossier) : c'est ainsi qu'OMRAN désactive une alerte.

### reponse 400

Type ou résultat introuvable ; champ obligatoire manquant selon les règles ci-dessus.
