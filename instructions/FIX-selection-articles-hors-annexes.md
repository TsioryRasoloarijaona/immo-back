# FIX — La sélection d'articles proposait les annexes (parkings racine)

## Symptôme

Dans les sélecteurs d'article (transformation de prospection, devis, fiche prospection),
la liste de FERME BRETONE contenait 10 parkings (`AP-PARKING-xx`) pour 1 seul vrai lot
sélectionnable (`APF-4-00`, COMMERCE). L'utilisateur les perçoit comme des « sous-articles »
à exclure.

## Diagnostic (22/09/2026, mesuré sur le 115)

Les vrais sous-articles (`ParentId` renseigné — terrasses d'APF notamment) étaient **déjà
exclus** (`topLevelOnly` sur `GET /api/referentiels/articles`, le seul endpoint que les trois
sélecteurs du front utilisent). Le problème : **241 parkings sont modélisés en articles
RACINE** (sans parent), donc invisibles du filtre.

La mesure tranche ce qu'est un « lot principal » :

- article principal des **358 dossiers réels** : APPARTEMENT (353) ou COMMERCE (5) —
  **jamais** parking/terrasse/jardin ;
- les annexes liées à un dossier sont **toujours des sous-articles** (PARKING 19,
  TERRASSE 49, JARDIN 7 — zéro racine) ;
- les 241 parkings racine n'ont jamais été vendus, ni seuls ni en annexe.

## Correctif

`StkItemSpecifications.CATEGORIES_ANNEXES = {PAR, TER, TER NC, JDN}` + spécification
composable `horsCategoriesAnnexes()` (LEFT JOIN, un article **sans** catégorie reste
sélectionnable), appliquée par `ReferentielService.articles` **uniquement quand aucune
`categorieId` n'est demandée** — cocher explicitement PARKING dans la cascade
Catégorie + Type rend les parkings, pas une liste vide.

Périmètre volontairement étroit : le listing de tranche (`GET /api/tranches/{id}/articles`),
le catalogue (`GET /api/articles`) et le dropdown des sous-articles
(`GET /api/referentiels/sous-articles`) sont **inchangés** — un parking sous-article reste
choisissable comme annexe d'un lot.

`TER NC` (terrasse non couverte) : 0 article aujourd'hui, exclue par cohérence de famille.

## Vérification

- `ReferentielControllerTest.getArticles_ecarteLesCategoriesAnnexes_saufDemandeExplicite`
  (23/23 verts) : parking racine disponible absent de la sélection, présent dès que sa
  catégorie est demandée.
- Attendu sur le banc APF après déploiement : la sélection passe de 11 entrées
  (10 parkings + 1 commerce) à 1.

## Impact front

Aucun changement requis : même endpoint, même forme. Les sélecteurs affichent simplement
moins de bruit. Si un écran doit un jour proposer la vente d'un parking seul, passer
`categorieId=<PARKING>` explicitement.
