# SuiviActionController

L'écran OMRAN « Suivi des actions » : les actions commerciales (appels, visites, messages, notes…) saisies par chaque utilisateur, sur une période. Doc OMRAN : https://help.omranestate.com/SUIVIDESACTIONS.html — détail dans `instructions/FEATURE-suivi-actions.md`.

**Même requête qu'OMRAN**, relevée dans sa trace d'audit : `ComActionMessageView WHERE CfgTrancheId IN (…) AND DocDate >= début AND DocDate < fin [AND UserName = …]`. Mêmes colonnes, même tranche résolue (catégorie Prospection → tranche de la prospection, sinon de l'article), même rang, même état — construits sur la table plutôt que sur la vue, qui coûte 6,6 s la page.

**Compter les appels d'un commercial jour par jour** — le cas d'usage principal (94 % des actions de l'année sont des appels sortants) : `GET /api/suivi-actions/groupes?utilisateur=<nom>&typeId=<Téléphone Sortant>&groupBy=dateAction&granularite=JOUR&dateDebut=…&dateFin=…`. L'effectif de chaque groupe est le nombre d'actions ; la somme des effectifs égale le `totalElements` de la liste aux mêmes filtres.

**Qui voit quoi.** Le suivi **général** est réservé au rôle `Administrateur`. Tout autre utilisateur ne voit que **ses propres actions** — sur la liste comme dans les groupes : celles écrites à son nom (`UserName`), plus celles de son compte hors lignes « Système » (actions générées pendant sa session, qu'OMRAN attribue à « Système »). Pour lui, le filtre `utilisateur` ne peut que restreindre : demander quelqu'un d'autre rend une page vide, jamais un 403 qui confirmerait l'existence de ses actions. Le front masque donc le sélecteur d'utilisateur hors administrateur.

**Périmètre commercial** : appliqué par le service, liste et groupes — une action dont le projet (par la prospection, puis par le dossier) est hors périmètre n'apparaît ni dans la liste ni dans un effectif. Actions d'entités juridiques inactives exclues, comme partout.

## findAll

La grille de l'écran OMRAN. Tous les filtres sont optionnels et se combinent en ET.

**Période fin exclue**, comme OMRAN : `dateDebut <= DocDate < dateFin`. Un jour J = `dateDebut=J
T00:00` et `dateFin=J+1 T00:00`.

**Colonnes** : `?colonnes=` choisit les champs servis (catalogue et liste blanche : `GET
/api/referentiels/colonnes/suivi-actions`). `rang`, `alerteActive`, `etat` et `personnel` sont
calculées pour la page servie, et seulement quand elles sont demandées : ni triables ni groupables.

**Tri** : `sort=docDate,desc` par défaut ; seules les propriétés de l'action (`docDate`, `userName`,
`alertValidityDate`…) sont triables.

Les actions **système** (relances, événements générés) sont listées, comme dans OMRAN —
`systeme=false` pour ne garder que les saisies.

### param trancheId

Tranche(s) résolue(s) à la manière d'OMRAN (catégorie Prospection → tranche de la prospection, sinon
de l'article). Répétable, OU.

### param utilisateur

Utilisateur(s) — `UserName` de l'action, **égalité exacte**, la colonne que filtre OMRAN. Valeurs
présentes : `/api/suivi-actions/groupes?groupBy=utilisateur`. Répétable, OU. **Utile à
l'administrateur seulement** : pour un autre rôle, la liste est déjà réduite à ses propres actions
et ce filtre ne peut que la restreindre davantage.

## grouper

La bande de groupement de la grille OMRAN, calculée côté serveur : **un groupe par valeur** de
`groupBy`, avec son effectif = **le nombre d'actions**. Les effectifs portent sur tout le périmètre
filtré, pas sur une page.

**Mêmes filtres que la liste**, mêmes noms, même périmètre : la somme des effectifs égale le
`totalElements` de `GET /api/suivi-actions` aux mêmes filtres (avec `inclureSansValeur=true`).

Recettes :
- **appels passés par jour** d'un commercial : `utilisateur=<nom>&typeId=<Téléphone
  Sortant>&groupBy=dateAction&granularite=JOUR` ;
- **activité par utilisateur** sur une période : `groupBy=utilisateur` ;
- **par type d'action** : `groupBy=typeAction` ;
- **par tranche** : `groupBy=trancheCode`.

Pour ouvrir un groupe : `GET /api/suivi-actions?groupeColonne=<groupBy>&groupeValeur=<cle>` (+ la
même `granularite`).

**Aucune colonne n'est agrégeable** : la mesure est l'effectif. `agregats` rend donc 400. Colonnes
groupables : le catalogue (`groupable = true`).
