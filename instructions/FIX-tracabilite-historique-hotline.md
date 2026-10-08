# FIX — auteur et traçabilité des dossiers, historique et interactions de la Hotline (lot 3)

Origine : tests navigateur du 08/10/2026 (dossier ARS 2026/00410, prospect 26/14454).
Branche `fix/tracabilite-historique-hotline` (immo-crm). Front : uniquement le repli « Système »
(§1) s'il faut le retoucher — sinon aucun.

## 1. Historique du dossier signé « Système »

OMRAN — GEN0016 (Règles de gestion générale) : « La date de création, La date de modification,
L'acteur qui a procédé à la création, L'acteur qui a procédé à la modification ». DOSSIERS DE VENTE,
Liste (Historique) : « La liste visualise l'historique du dossier de vente en indiquant :
L'utilisateur, La date, L'état… ».

Constat : `DossierHistoriqueService.java:87` passe `null` comme utilisateur des événements STATUT,
alors que `DossierService.java:641` écrit `StrFolderStatusHistory.AdmUserId` et `:651-652`
`StrHistory.AdmUserDescription`. Les événements ECHEANCE lisent `ComDocument.UserCreate` (`:98`), que
`DossierService.genererSchemaFinancement` (`:518-526`) et `EcheancierService.java:232-234` ne posent pas.
Le front affiche alors `h.utilisateur || 'Système'` (`DossierDetail.tsx:1409`).

Décision antérieure levée : `FEATURE-fiche-dossier-alignement-ecran.md` §3 laissait `UserCreate` non
posé « à traiter ensemble le jour où l'auteur sera exposé ». Il l'est.

Attendu : STATUT → nom de l'auteur (description de l'`AdmUser`, ou `StrHistory.AdmUserDescription`
correspondant). Lignes d'échéancier créées → `UserCreate = utilisateurAffiche.courant().nom()` et
`UserCreateDateTime = now(clock)`. Les lignes legacy sans auteur restent `null` (affichage inchangé).

## 2. Traçabilité du dossier vide

Même règle GEN0016 ; la liste DOSSIERS DE VENTE a « Crée le / Crée par / Modifier le / Modifier par ».

Constat : `DossierService.create` (`:335-347`) ne pose ni `UserCreate` ni `UserCreateDateTime` (vérifié
NULL en base sur 2026/00410) ; `changerStatut` (`:578-611`) ne pose pas `UserUpdate*`. Le DTO
(`:880-883`) et le front (`DossierDetail.tsx:1653-1656`) sont déjà prêts.

Attendu : `create` pose `UserCreate`/`UserCreateDateTime` ; `changerStatut` et toute modification du
dossier posent `UserUpdate`/`UserUpdateDateTime`. Modèle : `ProspectionService.java:838-839`
(`marquerModification`). Même trou dans `ProspectService.creerEntite` (`:178-209`) : le combler aussi.

## 3. Commercial référent du tiers vide (Hotline, fiche prospect)

OMRAN — PROSPECTS, Champs : « Personnel | Liste déroulante des personnels qui sont paramétrés avec
affichage dans la liste des commerciaux ». Aucune règle de remplissage automatique documentée.

Constat : la Hotline lit `CfgTier.CfgCommercialId` (`ProspectService.java:578-579`). Il n'est posé que si
`CreerProspectRequest.commercialId` est fourni (`:390-391`). `ProspectionService.transformer`
(`:985-993`) n'affecte le commercial qu'à la prospection, `DossierService.create:344` qu'au dossier.

Comportement observé du légataire (yesRemsProd) : 13 725 / 14 038 tiers PRO/CLI ont un commercial,
égal à celui de leur première prospection dans 79 % des cas.

Attendu : dans `transformer`, si le prospect n'a pas de commercial, lui copier celui de la prospection.
**Ne jamais écraser** une valeur existante.

## 4. Onglet « Historique » du tiers toujours vide (Hotline et fiche prospect)

OMRAN — HOT LINE et PROSPECTS, Liste d'historique : « La liste visualise l'historique de dossier de
vente d'un client en affichant : L'utilisateur, La date, L'état, Le N° Dossier, L'article, Les détails ».

Constat : `ProspectSubResourcesService.historique` (`:132-136`) lit `StrHistory WHERE ObjectId = tierId`.
Mesuré : 0 des 3 175 lignes `StrHistory` ne pointe un tiers (1 069 → dossier, 42 → prospection).
L'onglet est donc vide pour **tous** les tiers (Hotline `HotlineService.java:158`, fiche prospect
`ProspectDetail.tsx:475`).

Attendu : lire `StrFolderStatusHistory WHERE CfgTierId = :tier` (rempli sur 1 286 / 1 313 lignes
legacy ; nous l'écrivons, `DossierService.java:642`), joint à son `ComFolder` (n° dossier, article), au
statut, à l'`AdmUser` et à la description — les colonnes OMRAN une pour une. Tri date décroissante,
paginé comme aujourd'hui. Si le DTO change de forme : mettre à jour les annotations springdoc et
signaler l'« Impact front » ici (le front suivra dans un lot séparé).

## 5. Interactions saisies au stade pré-prospection absentes de la Hotline

OMRAN — PREPROSPECTIONS, Actions : « Créer tiers | …le système crée un tiers en utilisant les données
renseignées dans la fiche ». Rien sur le rattachement des actions existantes ; le légataire ne
rattrape pas non plus (1 499 interactions humaines à `CfgTierId` NULL sur des prospections qui ont un
prospect).

Constat : `addInteraction` pose `CfgTier` = prospect courant à l'écriture (`ProspectionService.java:1270`),
donc NULL avant « Créer prospect ». La Hotline filtre `cfgTier.id = tierId` (`HotlineService.java:147-149`).

Précédent : `FEATURE-nom-pre-prospection-relances.md` §2 a choisi une cascade **en lecture**, sans
écriture. Attendu : même choix — la Hotline lit les interactions où
`cfgTier.id = :tier OR comProspection.cfgProspect.id = :tier`, sans doublon, en gardant
`perimetreService.filtreInteractions()`. **Aucune réécriture** de `CfgTierId`.

## Impact front

- `GET /api/prospects/{id}/historique` et `HotlineFicheDto.historique` (`GET /api/hotline/recherche`) :
  **même DTO `HistoriqueDto`, trois champs ajoutés** (`dossierId`, `dossierCode`, `articleCode`), rien
  de retiré ni renommé. Le **contenu** change : une ligne = un changement de statut d'un dossier du
  tiers. `utilisateur`, `date`, `statutDescription` (état), `description` (détails) sont renseignés ;
  `workflowDescription` vaut toujours `null` pour un tiers. Le tri client est ignoré (date décroissante
  imposée). Les dossiers hors périmètre de l'appelant n'y figurent pas (comme le bloc `dossiers`).
- `GET /api/prospections/{id}/historique` : même DTO, les trois nouveaux champs y valent `null`.
- `HistoriqueEvenementDto.utilisateur` (`GET /api/dossiers/{id}/historique`) : désormais renseigné sur
  les événements STATUT et sur les ECHEANCE créées par l'API ; reste `null` sur les lignes legacy sans
  auteur, donc le repli « Système » de `DossierDetail.tsx` reste utile pour elles.
- `HotlineFicheDto.interactions` : inclut les interactions des prospections du tiers (saisies avant
  « Créer prospect »), sans doublon. Forme inchangée.
- `DossierDetailDto.creePar/creeLe/modifiePar/modifieLe` : remplis pour les dossiers créés / changés
  de statut via l'API. Forme inchangée.

À prévoir côté front (lot séparé) : colonnes « N° Dossier » et « Article » de l'onglet Historique
(fiche prospect, Hotline), lien `dossierId` vers la fiche dossier.

## Notes d'implémentation

- §1 : auteur STATUT = `AdmUser.Description` (sinon `Code`) via `AdmUserId` ; à défaut, le nom de la
  ligne `StrHistory` du même dossier à la même `DocDate` (cas « compte absent de la base d'écriture »).
  Mesuré sur `yesRemsProd` : 1 284 / 1 313 lignes ont un `AdmUserId` résolu ; le repli `StrHistory`
  n'en rattrape qu'1 des 29 autres.
- §2 : « toute modification du dossier » = les deux écritures de `ComFolder` existantes (`create`,
  `changerStatut`). L'ajout/modification d'une échéance n'écrit pas `ComFolder` et ne pose donc pas
  `ComFolder.UserUpdate`.
- §4 : 29 lignes `StrFolderStatusHistory` legacy n'ont pas de `ComFolder` existant ; elles restent
  visibles (n° dossier/article null). 36 lignes ont un `CfgTierId` nul (27) ou différent du client
  du dossier (9) : la spec dit `CfgTierId`, c'est ce qui est lu.

## Hors périmètre

Prix de l'article / Total HT-TTC / remise négative, encaissements annulés dans les totaux, passage
prospect → client, catégorie d'interaction : lot 4 (décisions en attente). Recopie synchrone : lot 5
(`FIX-rapatriement-synchrone.md`, autre branche) — ne pas toucher `config/`.

## Vérification

Tests ciblés (`-Dtest=…`), au minimum :
- création de dossier → `UserCreate`/`UserCreateDateTime` posés ; changement de statut → `UserUpdate*`
  posés ; historique : l'événement STATUT et les ECHEANCE créées portent le nom de l'auteur ;
- `transformer` : prospect sans commercial → reçoit celui de la prospection ; prospect avec commercial
  → inchangé ;
- historique du tiers : un tiers ayant un dossier avec historique de statut → lignes non vides ; tiers
  sans dossier → vide ; tiers inconnu → 404 comme aujourd'hui ;
- Hotline : une interaction créée avant « Créer prospect » apparaît pour le tiers ; une interaction
  hors périmètre de l'utilisateur reste invisible.
CI complète verte sur le dernier commit poussé.
