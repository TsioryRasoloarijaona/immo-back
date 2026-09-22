# FIX — La sélection d'articles proposait les annexes (parkings racine)

## Symptôme

Dans les sélecteurs d'article (transformation de prospection, devis, fiche prospection),
la liste de FERME BRETONE contenait 10 parkings (`AP-PARKING-xx`) pour 1 seul vrai lot
sélectionnable (`APF-4-00`, COMMERCE). L'utilisateur les perçoit comme des « sous-articles »
à exclure.

## Diagnostic (22/09/2026)

Les vrais sous-articles (`ParentId` renseigné — terrasses d'APF notamment) étaient **déjà
exclus** (`topLevelOnly` sur `GET /api/referentiels/articles`, le seul endpoint que les trois
sélecteurs du front utilisent). Le problème : **241 parkings sont modélisés en articles
RACINE** (sans parent), donc invisibles du filtre de parenté.

**La règle exacte du légataire a été relevée dans sa trace d'audit** (écran Articles rejoué
par l'utilisateur, requête reconstituée depuis les fragments `sequence_number`) :

```sql
WHERE (StkHierarchyId = @P1) AND (StkVocationId = @P2)
  AND (IsUnderItem = 0) AND (ParentId IS NULL)
  AND ((ComFolderId IS NULL) OR (StrComFolderStatus.BoolField009 = 1))
  AND ((RntFolderId IS NULL) OR (StrRntFolderStatus.BoolField004 = 1))
```

Trois enseignements mesurés :

1. **`IsUnderItem` est LE marqueur des annexes**, pas la parenté ni la catégorie : sur les
   1 068 articles réels, PARKING/TERRASSE/JARDIN sont tous à 1 (y compris les 241 parkings
   racine), APPARTEMENT/COMMERCE/LOT DE TERRAIN tous à 0, **zéro NULL**. Cohérent avec les
   dossiers : l'article principal des 358 est toujours APPARTEMENT (353) ou COMMERCE (5).
2. **`BoolField009` (« IsAvailableForProspection » du statut de dossier) vaut 1 sur AUCUN
   `StrStatus`** : le `OR` d'OMRAN ne réactive jamais rien en pratique — sa disponibilité se
   réduit à `ComFolderId IS NULL`, exactement notre `StatutArticle`.
3. OMRAN filtre aussi par **vocation** (`StkVocationId = @P2`, VENTE) — non repris ici,
   noté comme écart connu si un besoin location apparaît.

## Correctif

`StkItemSpecifications.horsAnnexes()` : `IsUnderItem = 0` (un `NULL`, inexistant en réel,
est traité comme « pas une annexe » — ne pas perdre un lot légitime sur une donnée
incomplète). Appliquée par `ReferentielService.articles` **uniquement quand aucune
`categorieId` n'est demandée** — cocher explicitement PARKING dans la cascade
Catégorie + Type rend les parkings, pas une liste vide.

Périmètre volontairement étroit : le listing de tranche (`GET /api/tranches/{id}/articles`),
le catalogue (`GET /api/articles`) et le dropdown des sous-articles
(`GET /api/referentiels/sous-articles`) sont **inchangés** — un parking sous-article reste
choisissable comme annexe d'un lot.

Une première version excluait par **catégories** (`PAR`/`TER`/`TER NC`/`JDN`) ; remplacée
par le drapeau après lecture de la trace : même résultat sur les données du jour, mais la
règle du légataire est robuste aux nouvelles catégories et ne dépend pas des libellés.

## Vérification

- `ReferentielControllerTest.getArticles_ecarteLesAnnexes_saufDemandeExpliciteDeCategorie` :
  parking racine disponible (`IsUnderItem = 1`) absent de la sélection, présent dès que sa
  catégorie est demandée.
- Attendu sur le banc APF après déploiement : la sélection passe de 11 entrées
  (10 parkings + 1 commerce) à 1.

## Impact front

Aucun changement requis : même endpoint, même forme. Les sélecteurs affichent simplement
moins de bruit. Si un écran doit un jour proposer la vente d'un parking seul, passer
`categorieId=<PARKING>` explicitement.
