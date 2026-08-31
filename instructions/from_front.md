# Demandes d'endpoint — Front → Backend

> **Règle** : ce fichier est le canal officiel par lequel le front
> (`crm-prototype`) fait remonter ses besoins d'endpoint au backend
> (`immo-crm`). Toute nouvelle demande s'ajoute en tête de la section
> "Demandes en cours" ci-dessous, dans le même format que celles déjà
> présentes (Contexte / Endpoint souhaité / Pourquoi / Contournement actuel
> côté front). Une fois un endpoint livré et vérifié côté front (voir
> `docs/api-docsV4.json` du repo front), déplacez son entrée dans "Traitées"
> avec la date, au lieu de la supprimer — ça garde un historique de ce qui a
> été demandé et pourquoi.

---

## Demandes en cours

### 10. `GET /api/dossiers?statut=…` renvoie 0 résultat pour la plupart des statuts filtrés

**Contexte** : remonté par le métier — le filtre "Statut" de `/dossiers`
(checklist multi-sélection) renvoie souvent une liste vide alors que des
dossiers existent bien dans ce statut (visible en parcourant la liste sans
filtre).

**Diagnostic côté front** : le filtre envoie la valeur affichée dans la
checklist — ex. `?statut=Concrétisation` (Title Case, accentué), tirée de
`DOSSIER_STATUS_PIPELINE` (`lib/dossierStatus.ts`), qui est aussi la forme
canonique utilisée pour l'affichage (badges/stepper). L'exemple donné par
`GET /api/dossiers`'s doc OpenAPI pour ce paramètre est d'ailleurs
`"Réservation"`, la même forme.

Mais la demande #5 (déjà livrée) a confirmé sur données réelles que
`DossierDetailDto.statut`/`ArticleListItemDto.dossierStatut` portent la
valeur brute de `StrStatus.Code`, dont un exemple réel cité est
`CONCRETISATION` — majuscules, sans accent. `lib/dossierStatus.ts` a même
un commentaire de longue date documentant cet écart ("seen in the wild:
'CONCRETISATION' vs 'Concrétisation'") pour la normalisation d'affichage,
mais rien n'existait côté requête sortante avant ce correctif.

Si `?statut=` fait une comparaison exacte (sensible à la casse/aux
accents) contre `StrStatus.Code`, envoyer la forme accentuée manque tout
statut dont le code réel diffère — ce qui correspondrait exactement au
symptôme "beaucoup de résultats vides".

**Deux corrections possibles côté backend** (l'une ou l'autre suffit) :

1. **Normaliser la comparaison** — accent/casse-insensible entre la valeur
   de `statut` reçue et `StrStatus.Code` (ex. même normalisation que
   `String.toUpperCase()` + suppression des accents des deux côtés). Le
   front pourrait alors continuer à envoyer la forme affichée sans
   changement.
2. **Documenter/exposer la liste exacte des `StrStatus.Code` réels** — un
   petit référentiel (`GET /api/referentiels/statuts-dossier` ou
   équivalent, même forme que `types-reglement`/`natures`) donnant
   code/libellé pour chaque statut réellement présent en base, pour que le
   front envoie le code exact plutôt qu'une forme dérivée du libellé
   d'affichage.

**Contournement actuel côté front** (`DossiersList.tsx`,
`fetchDossiersWithStatusWorkaround`) : dès qu'au moins un statut est coché,
le front récupère un lot de 500 dossiers (les autres filtres — recherche,
tranche, commercial, période — restent appliqués côté serveur) puis filtre
et pagine **côté client** via `normalizeStatus` (comparaison
accent/casse-insensible, déjà utilisée pour l'affichage). Ça corrige le
symptôme immédiatement mais ne scale pas si le portefeuille dépasse ~500
dossiers, et fait un aller-retour de plus que nécessaire même dans le cas
courant. À retirer dès qu'une des deux corrections ci-dessus est livrée.

---

## Traitées

### 4. Écart colonnes/filtres — écran legacy "Articles" (Patrimoine) vs `ArticleListItemDto`/`GET /api/articles` (livré le 28/08/2026)

**Contexte** : comparaison demandée par le métier entre la grille "Articles
(220)" de l'ancien système (capture jointe, module Patrimoine) et ce que
`ArticleListItemDto`/`GET /api/articles`/`GET /api/tranches/{id}/articles`
exposaient côté front (`ArticlesList.tsx`).

**Colonnes de la grille legacy, toutes désormais couvertes** :

- ~~**N° Dossier** / **Client**~~ et ~~**État plus riche que
  DISPONIBLE/OCCUPE**~~ — livrés via la demande #5.
- **Type Présentation** — **résolu** : colonne à code court (`F`, `C`,
  `D`, `H`...) confirmée être un vrai référentiel distinct
  (`StkItem.StkPresentationTypeId`, table `StkPresentationType`, non
  mappée jusqu'ici), scopé par tranche (`StkPresentationType.StkHierarchyId`)
  — pas une transformation de Catégorie/Type. Nouvelle entité + repository,
  ajouté sous `presentationTypeId`/`presentationTypeCode`/
  `presentationTypeLibelle` sur `ArticleListItemDto` **et**
  `ArticleDetailDto`.
- **Superficie Sous Articles (m²)** — ajouté : `superficieSousArticles`
  sur les deux DTO, somme de `StkItem.Area` des sous-articles rattachés
  (`ParentId`), `0` si aucun. Batch-agrégée en un aller-retour DB par page
  de résultats (même précédent que le batch-loading des Dossiers liés,
  demande #5) — pas de requête N+1.
- **TVA (%)** — ajouté : `tauxTVA` (`StkItem.Vat`, ratio brut) sur les
  deux DTO.
- **Prix plancher en repli sur "Prix de vente"** — `salePriceFloor`/
  `comSalePriceFloor` (déjà sur `ArticleDetailDto`, voir le fix ARS0804)
  ajoutés aussi sur `ArticleListItemDto`, comme demandé — la colonne
  "Prix de vente" de la grille `/articles` peut désormais utiliser le
  même repli que la fiche détail.
- **Nature/Étage/Immeuble sur la liste** — `natureId`/`natureCode`/
  `natureLibelle` (`StkKind`), `etageId`/`etageCode`/`etageLibelle`
  (`StkFloor`) et `immeubleId`/`immeubleCode`/`immeubleLibelle`
  (`StkBuilding`) — déjà sur `ArticleDetailDto`, désormais aussi sur
  `ArticleListItemDto` (fermait le point "reste ouvert pour la partie
  colonne liste").

**Filtres du panneau legacy, tous ajoutés sur `GET /api/articles` ET `GET
/api/tranches/{id}/articles`** (les deux endpoints, comme demandé) :

- **`entiteId`** (`CfgCompany.PKey`, via `StkItem.stkHierarchy.cfgProject.cfgCompany`)
  — combinable avec la restriction ADMOD/GHRIZA déjà appliquée à `GET
  /api/articles`.
- **`natureId`** (`StkKind.PKey`) — nouveau référentiel `GET
  /api/referentiels/natures` ajouté pour peupler ce filtre (n'existait
  pas du tout auparavant, contrairement à Vocation/Entité).
- **`vocationId`** (`StkVocation.PKey`) — câblé sur `GET
  /api/referentiels/vocations`, déjà existant.
- **`etageId`** (`StkFloor.PKey`) — nouveau référentiel `GET
  /api/referentiels/etages` ajouté, même raison que Nature ci-dessus.
- **`prixDe`/`prixA`** (bornes inclusives sur `StkItem.ComSalePrice`) et
  **`surfaceDe`/`surfaceA`** (sur `StkItem.Area`) — même convention que
  `GET /api/encaissements`'s `montantDe`/`montantA`, comme suggéré.

**Contournement front à retirer** : `ArticlesList.tsx` peut maintenant
câbler les filtres Entité/Nature/Vocation/Étage/Prix/Surface et les
colonnes Type Présentation/Superficie Sous Articles/TVA/Nature/Étage/
Immeuble — plus besoin de rester sur le sous-ensemble recherche/tranche/
statut/catégorie/type.

### 9. `POST /api/prospections` — aucun endpoint de création n'existait, **bloquant** (livré le 28/08/2026)

**Contexte** : besoin métier confirmé sur captures des écrans legacy Omran
"Prospection :" et "Prospect :" jointes — l'objectif est un bouton "Nouvelle
prospection" côté front qui permette de créer le prospect ET la
prospection dans le même formulaire (avec la contrainte confirmée qu'une
prospection nécessite un prospect existant au préalable). La moitié
"Prospect" existe déjà et est câblée (`POST /api/prospects`, voir
`NewProspect.tsx`) ; la moitié "Prospection" était bloquante.

**Vérifié exhaustivement dans `docs/api-docsV4.json`** : aucun verbe `POST`
de création n'existait ni sur `/api/prospections` ni sur
`/api/pre-prospections` (ce dernier est `GET`-only). Tous les verbes
d'écriture confirmés nécessitaient un `{id}` de prospection déjà existant
en base (`PATCH /api/prospections/{id}`, `.../tranche-principale`,
`.../etape`, `.../archiver`, `POST .../transformer`).

**Mapping champs legacy → DTO fourni par le front** (capture "Prospection :") :
Code → `code` (généré serveur) · Date → `dateDocument` · Prospect (FK,
obligatoire) → `prospectId` · Tranche/Article → `trancheId`/`articleId` ·
Commercial → `commercialId` · Action commerciale → `actionCommercialeId` ·
Nature → `etapeId` · Source de contact → `origineId` · Support →
`origineSupportId` · Type (ex. "Visite") → `typeContactId` · Notes →
`notes` · "C'est un ancien client" → `ancienClient` · Date
d'archivage/Archivée/Raison → `fermee`/`dateFermeture`/`raisonFermetureId`/
`raisonFermetureTexte`.

**Décisions prises côté implémentation** (les deux points laissés ouverts
dans la demande) :
- **Point d'entrée : `POST /api/prospections`, pas `/api/pre-prospections`.**
  Le mapping champs fourni par le front listait déjà `prospectId` comme
  **obligatoire** ("Prospect (FK, obligatoire selon le métier)") — ce qui ne
  correspond pas à une pré-prospection (`CfgProspectId IS NULL` par
  définition, voir `PreProspectionController`). Le flux réel est : le front
  crée déjà le prospect via `POST /api/prospects` (existant, câblé), puis
  appelle ce nouvel endpoint avec son id pour créer directement une
  prospection qualifiée — sans passer par la moitié pré-prospection
  (`creer-prospect`/`transformer`), qui reste inchangée pour son propre cas
  d'usage (lead brut/non identifié).
- **`tempDescription`/`tempTel`/`tempEmail` : pas repris par ce nouvel
  endpoint.** Confirmé que c'est un flux distinct (walk-in/appel entrant
  sans prospect connu) — hors périmètre de cette demande, qui porte sur le
  cas où le prospect existe déjà. Pas de changement sur ces 3 champs, qui
  restent des colonnes legacy inertes côté API.
- **`code` généré serveur**, confirmé et implémenté : même mécanisme
  `AdmSequence` que `ComFolder`, ligne `Code = "Prospection"` trouvée en
  base réelle (`SeqValue` à 5932 au moment de l'implémentation, format
  confirmé sur des codes réels "26/05932" → 2 chiffres d'année / 5 chiffres
  de numéro, contrairement au Dossier qui a 4 chiffres d'année + préfixe
  projet).
- **Nature → `etapeId`** : confirmé un simple renommage (même
  `ComProspectionKind`, mêmes id que `PATCH .../etape`), repris tel quel.
- **Date d'archivage/Archivée/Raison** : **pas repris** à la création (
  `fermee`/`dateFermeture`/`raisonFermetureId`/`raisonFermetureTexte`) —
  créer une prospection déjà fermée n'a pas de sens pour ce bouton
  "Nouvelle prospection" ; `PATCH .../archiver` reste le seul chemin, y
  compris juste après création si un jour un besoin réel apparaît.

**Livré** : `POST /api/prospections` (`CreerProspectionRequest` →
`ProspectionService.creerProspection`), 201 + `ProspectionDetailDto`.
`prospectId` obligatoire (400 si absent ou introuvable) ; tous les autres
champs du mapping fourni sont repris en optionnel, avec la même validation
d'existence à la demande que `PATCH /api/prospections/{id}` :
`commercialId`, `trancheId`/`articleId` (même garde-fou croisé que
`PATCH .../tranche-principale` — 400 si l'article n'appartient pas à la
tranche), `origineId`, `origineSupportId`, `vocationId`, `typeContactId`,
`actionCommercialeId`, `etapeId`, `surface`/`surfaceJusqua`,
`prix`/`prixJusqua`, `avance`, `financementFondsPropres`/`financementCredit`/
`financementAutre`, `ancienClient`, `notes`. Fournir `commercialId` dès la
création rend la prospection immédiatement visible sur `GET
/api/prospections` (qui exclut les pré-prospections) sans second appel à
`POST .../transformer`. Une ligne d'historique ("Prospection créée : "…)
est journalisée, visible via `GET /api/prospections/{id}/historique`
au même titre que les transitions pré-prospect → prospect → prospection.

### 8. `GET /api/dossiers/{id}/reglements/{reglementId}/documents/recu` renvoie 404 "Règlement introuvable" pour un `id` pris dans `ReglementDto` (livré le 28/08/2026)

**Contexte** : nouveau bouton "Voir/Télécharger le reçu" ajouté sur chaque
ligne de "Détail des règlements" (`DossierDetail.tsx`, onglet Financier,
`EcheanceReglementsHistory`). Repro live, pas un id de test périmé :
dossier `597c522c-0c70-441a-a0a1-826b19a96b72`, échéance FRAIS SYNDIC
(Réglé 8 400,00 DH), règlement VIREMENT BMCE BANK Réf. 145783320 affiché
avec `montant = 16 800,00 DH` (le double du total réglé de l'échéance
affiché juste au-dessus). Le `r.id` de cette ligne (tel que renvoyé par
`GET /api/dossiers/{id}/echeancier/{echeanceId}/reglements`) envoyé tel
quel comme `reglementId` à l'endpoint reçu → `404 {"detail":"Règlement
introuvable: <id>"}`.

**Diagnostic** : deux bugs distincts dans `EcheancierService.toReglementDto`,
tous deux dans le sens front→back (`ReglementDto` mal peuplé, pas
l'endpoint reçu lui-même) :

- **La 404** : `ReglementDto.id` était peuplé avec `StlSettlementLine.PKey`
  (l'id de la ligne d'échéancier) au lieu de `StlSettlement.PKey` (l'id du
  règlement) — deux tables différentes, même nom de champ. L'endpoint reçu,
  lui, cherchait bien un `StlSettlement.PKey` (`RecuReglementService.collect`
  → `settlementRepository.findById(reglementId)`), d'où le 404 sur un id
  pourtant copié tel quel depuis la réponse de l'API.
- **Le montant doublé (repéré par le front)** : `ReglementDto.montant`
  utilisait `StlSettlement.Amount` (le montant **total** du règlement) au
  lieu de `StlSettlementLine.Amount` (la part affectée à **cette**
  échéance). Un règlement réparti sur plusieurs échéances (ex. FRAIS
  DOSSIER + FRAIS SYNDIC, exactement le cas remonté) affichait donc le
  total sur chaque ligne au lieu de la part réelle — d'où 16 800 affiché
  pour une échéance réglée à 8 400.

**Livré** : `toReglementDto` prend maintenant la `StlSettlementLine`
elle-même (plutôt que des ids éclatés) et en tire `id = ligne.settlement.id`
(le vrai `StlSettlement.PKey`, envoyable tel quel à l'endpoint reçu) et
`montant = ligne.amount` (la part de cette échéance, pas le total). Doc
Javadoc du DTO mise à jour pour préciser explicitement les deux champs
(demandé dans le rapport : *"soit confirmer que `ReglementDto.id` est bien
le `StlSettlement.PKey`"*) — c'est confirmé, et maintenant vrai en pratique.
Test de non-régression ajouté (`ReglementTest.getReglements_settlementSplitAcrossEcheances_idIsSettlementPKeyAndMontantIsLineShare`)
avec un règlement scindé sur deux échéances, vérifiant montant = part de
ligne et que l'`id` renvoyé résout bien contre l'endpoint reçu.

**Contournement front à retirer** : aucun changement requis côté front —
le bouton reçu envoyait déjà `r.id` correctement, c'est la donnée serveur
qui était fausse.

### 7. CORS — accès LAN, cookie non injecté hors localhost (livré le 27/08/2026)

**Contexte** : suite du point 2 de la demande #7 initiale (config CORS à
étendre aux IP LAN). Après la correction front (`API_BASE_URL` dérivé de
`window.location.hostname`), les appels partaient bien vers la bonne IP
LAN mais la connexion "réussissait" sans jamais persister — confirmé en
session : *"on the remote host only login work, then the cookies are not
injected"*.

**Diagnostic** :
- **CORS lui-même n'était pas le problème** — `CorsConfig.java`
  (`allowedOriginPatterns("*")` + `allowCredentials(true)`) reflète déjà
  correctement n'importe quelle origine LAN. Vérifié en rejouant un
  preflight `OPTIONS` et un vrai `POST /api/auth/connexion` avec
  `Origin: http://192.168.11.114:5173` directement contre le backend
  tournant : réponses 200/401 toutes deux avec
  `Access-Control-Allow-Origin`/`-Credentials` corrects.
- **Le vrai coupable : le cookie `access_token`**. `AuthController`/
  `MonCompteController` posent le cookie avec `.secure(cookieSecure)`,
  et `app.jwt.cookie-secure=true` en dur dans `application.properties`
  (pas de profil dev). Un cookie `Secure` est **silencieusement jeté par
  le navigateur** sur une connexion HTTP non chiffrée — indolore sur
  `localhost` (traité comme "contexte sécurisé" même en HTTP par les
  navigateurs), mais fatal dès qu'on accède au backend via une IP LAN en
  HTTP simple : le login répond 200 + `Set-Cookie`, mais rien n'est
  stocké côté client, donc toute requête authentifiée suivante repart
  sans cookie.

**Livré** : nouveau profil Spring `application-dev.properties`
(`app.jwt.cookie-secure=false`) — à activer via
`SPRING_PROFILES_ACTIVE=dev` (ou `-Dspring.profiles.active=dev`) pour un
run de démo/test LAN en HTTP. Comportement par défaut (`cookie-secure=true`)
inchangé pour tout run sans ce profil. `SameSite=Strict` n'était pas en
cause (même IP LAN, ports différents = même "site" pour les navigateurs).

### 6. `search` sur `GET /api/dossiers` étendu au nom du client (livré le 27/08/2026)

**Contexte** : remontée métier — "le searchbar sur Dossiers de vente ne
marche pas". `search` fonctionnait bien mais ne matchait que `code`
(confirmé par la description live d'alors : *"Recherche libre sur le code
du dossier"*), alors que le placeholder du champ (`DossiersList.tsx`)
promettait "client, code de dossier…" — un utilisateur qui tapait un nom
de client obtenait zéro résultat et pensait la recherche cassée.

**Livré** : `search` matche maintenant `code` **OU** nom/prénom/raison
sociale du client lié (sous-chaîne, insensible à la casse) — confirmé
dans la description live du paramètre. Placeholder restauré côté front
("Recherche multi-critère — client, code de dossier…", `DossiersList.tsx`),
aucun autre changement nécessaire (le composant envoyait déjà `search`
tel quel).

---

### 5. Lien inverse Article → Dossier (livré le 27/08/2026)

**Contexte** : confirmation métier sur capture jointe (grille "Articles"
legacy, colonnes État/N° Dossier/Client) — quand un article est
réservé/vendu, l'écran doit montrer son statut réel (`CONCRETISATION`,
`RESERVATION`…) et pas seulement `DISPONIBLE`/`OCCUPE`, plus le dossier et
le client concernés directement sur la ligne. Le détail d'un article doit
en plus afficher les infos du "Dossier de vente" lié. Précisait et
prenait le pas sur le point "État plus riche" de la demande #4 — une
seule livraison couvre les deux.

**Livré** : `dossierId`/`dossierCode`/`dossierStatut`/`clientId`/`clientNom`
ajoutés sur `ArticleListItemDto` **et** `ArticleDetailDto`, mêmes
noms/types que `DossierListItemDto`/`DossierDetailDto` comme demandé —
`dossierStatut` porte la valeur brute de `StrStatus.Code` (même format
que `DossierDetailDto.statut`, aucune nouvelle logique de mapping côté
front). `null` sur les 5 champs quand l'article n'est pas réservé/vendu
(`StkItem.ComFolderId` absent).

Implémentation : `ArticleService` batch-charge les `ComFolder` (+
`strStatus`/`cfgCustomer`) référencés par une page/liste d'articles en
une seule requête (`ComFolderRepository.findWithStatusAndCustomerByIdIn`)
plutôt qu'un aller-retour DB par article — vaut pour les 4 endpoints de
listing/détail (`GET /api/articles`, `GET /api/tranches/{id}/articles`,
`GET /api/articles/{id}/sous-articles`, `GET /api/articles/{id}`).

---

### 3. `GET /api/encaissements/{id}` — détail d'un règlement (livré le 27/08/2026)

**Contexte** : `/encaissements` navigue vers une vraie page
`/encaissements/:id` au clic sur une ligne — comme `/dossiers/:id`,
`/prospections/:id`, `/articles/:id`. Sans `GET /api/encaissements/{id}`
cette page ne pouvait qu'afficher une 404 (**bloquant**, pas de
contournement acceptable — voir historique complet ci-dessous).

**Livré** : `GET /api/encaissements/{id}` → `EncaissementDto`, 404 si
l'id n'existe pas — signature strictement identique à `getDossier`/
`getProspection`/`getArticle`, déjà câblée côté front (`getEncaissement`
dans `api.ts`, `EncaissementDetail.tsx`) sans rien à changer. Même forme
que les lignes de `GET /api/encaissements` (`montantBrutRestant`/
`montantNet` recalculés côté serveur, jamais stockés, comme sur la liste).

Les champs legacy plus riches que `EncaissementDto` ne sont **pas**
inclus dans cette livraison (pas bloquants, demandés séparément si
besoin un jour) : retenue (%)/montant timbre, tier payeur/tier
autrui/notaire, dossier de groupe, remise au siège, bordereau (n°/date),
date de pointage + C.R.A.S., devise, détail d'affectation par échéance
(un règlement → ses échéances), onglets Documents joints/Historique.
`EncaissementDetail.tsx` continue de n'afficher que ce que le DTO expose
aujourd'hui.

### 2. `GET /api/referentiels/types-reglement` (+ `/banques`) — picklists pour "Ajouter un règlement" (livré le 27/08/2026)

**Contexte** : sur la fiche Dossier (`/dossiers/:id`, onglet Financier), le
formulaire "+ Règlement" (`AddReglementRow` dans `DossierDetail.tsx`) laisse
saisir `typeId` (`CreerReglementRequest.typeId`) en texte libre — l'UUID du
type de règlement doit être connu et collé à la main, ce qui n'est
utilisable par personne en pratique (voir capture jointe).

Un référentiel `GET /api/referentiels/modes-paiement` existe déjà mais sert
un champ différent : `CreerDossierRequest.modePaieId`, utilisé une seule
fois à la création du dossier (`NewDossier.tsx`). Rien ne confirme que
`modes-paiement` et le `typeId` d'un règlement pointent vers la même table
— merci de préciser si c'est le cas (auquel cas le front peut réutiliser
`getModesPaiement()` directement, aucun nouvel endpoint requis) ou si
`StlSettlementLine.typeId`/`CreerReglementRequest.typeId` référence une
table distincte (auquel cas un nouveau référentiel est nécessaire).

**Endpoint souhaité** (si distinct de `modes-paiement`) :

```
GET /api/referentiels/types-reglement
```

Forme `ReferentielItemDto[]` (id/code/description), comme les autres
référentiels statiques.

**Bonus lié** : `CreerReglementRequest.banqueId` existe aussi dans le DTO
mais n'a aucun référentiel ni champ dans le formulaire actuel — le front ne
l'envoie jamais aujourd'hui. Si un règlement par chèque/virement nécessite
réellement une banque, un `GET /api/referentiels/banques` serait utile
aussi ; sinon dites-le et on laisse `banqueId` de côté.

**Contournement actuel côté front** : `Input` texte brut "UUID type de
règlement", pas de valeur par défaut — l'utilisateur doit connaître l'UUID
exact. Dès qu'un référentiel est confirmé/livré, ce champ devient un
`Select`/`SearchableSelect` comme les autres formulaires de l'app.

**Livré** :
- **Confirmé (via le code) : `typeId` ≠ `modes-paiement`.**
  `EcheancierService.ajouterReglement` résout
  `CreerReglementRequest.typeId` contre `StlSettlementType`, jamais
  `ComPaymentMethod` — ce sont bien deux tables distinctes. Nouvel endpoint
  `GET /api/referentiels/types-reglement` ajouté (`ReferentielService.typesReglement`),
  14 types réels confirmés en base (CHEQUE, VIREMENT, ESPÈCES, CMI...).
- **Confirmé : oui, la banque est réellement utile pour certains types.**
  `StlSettlementType.IsBankMandatory` existe déjà en base et varie par
  type (vrai pour CHEQUE/VIREMENT/EFFET/VERSEMENT, faux pour
  ESPÈCES/CMI/REGUL...) — exposé sous `TypeReglementDto.banqueObligatoire`
  pour piloter l'affichage conditionnel du champ Banque côté front.
  `GET /api/referentiels/banques` ajouté (`ReferentielService.banques`,
  `CfgTier` filtré sur `CfgTierType.Code = "BAN"`, 21 tiers réels confirmés
  en base), forme `ReferentielItemDto[]` standard.
- Réponse de `types-reglement` : `TypeReglementDto[]` (id/code/description/`banqueObligatoire`),
  **pas** `ReferentielItemDto[]` comme demandé initialement — le flag
  supplémentaire nécessitait un DTO dédié.

---

### 1. `GET /api/articles` — liste globale paginée/filtrable des articles (livré le 27/08/2026)

**Contexte** : nouvelle section "Articles" dans le module Patrimoine
(`/articles`, accessible aussi aux comptes Commercial), pensée pour
lister/parcourir tous les lots comme les autres écrans de liste
(`/prospects`, `/dossiers`…) — recherche libre + filtres + pagination
serveur, sans devoir d'abord choisir une Tranche.

Aujourd'hui, l'unique endpoint de listing paginé est
`GET /api/tranches/{id}/articles` (`trancheId` obligatoire dans le path).
Il n'existe aucun moyen de parcourir le catalogue d'articles à plat, tous
tranches confondues, avec une recherche texte.

**Endpoint souhaité** :

```
GET /api/articles?search=...&trancheId=...&statut=...&categorieId=...&typeId=...&page=...&size=...
```

- `search` — sous-chaîne sur `code`/`description` (même convention que
  `GET /api/prospects`/`GET /api/dossiers`).
- `trancheId` — optionnel, répétable (OR), pour filtrer sur une ou
  plusieurs tranches sans changer d'écran.
- `statut` — `DISPONIBLE`/`OCCUPE`, même enum que l'existant. Idéalement,
  **absent = tous les statuts** (pas seulement DISPONIBLE par défaut — la
  doc actuelle de `GET /api/tranches/{id}/articles` dit "Absent =
  DISPONIBLE", mais le front s'appuie déjà sur le fait qu'omettre le
  paramètre renvoie tout, cf. `TrancheDetail.tsx`. Merci de confirmer le
  comportement réel et de le documenter clairement pour ce nouvel endpoint).
- `categorieId`/`typeId` — répétables (OR), même sémantique que sur
  `GET /api/tranches/{id}/articles`.
- Réponse : `PageDtoArticleListItemDto` (même DTO que l'existant).

**Contournement actuel côté front** : `ArticlesList.tsx` impose de choisir
une Tranche au préalable (via `GET /api/referentiels/tranches`) puis
appelle `GET /api/tranches/{id}/articles` — ergonomique pour parcourir une
tranche à la fois (ça correspond au cascade métier Tranche → Catégorie →
Type → Article de l'ancien système), mais pas pour une recherche
transverse ("où est le lot A-204 ?") ni pour une vue "tout le patrimoine"
comme un vrai catalogue. Dès que l'endpoint existe, le sélecteur de
Tranche deviendra un filtre optionnel au lieu d'une étape obligatoire.

**Livré** : `GET /api/articles` (contrôleur `ArticleController`, service
`ArticleService.findAll`), documenté OpenAPI. Points confirmés/décidés côté
implémentation :
- `statut` absent → **tous les statuts** (DISPONIBLE + OCCUPE), comme
  demandé — comportement volontairement différent de
  `GET /api/tranches/{id}/articles` (qui garde son défaut DISPONIBLE), et
  documenté comme tel dans le `@Operation`.
- `search` filtre en sous-chaîne insensible à la casse sur `code` OU
  `description`.
- `trancheId` répétable (OR), même sémantique que `categorieId`/`typeId`.
- Réponse : `PageDto<ArticleListItemDto>`, DTO inchangé.
- Décision supplémentaire (non demandée explicitement mais cohérente avec
  la restriction déjà appliquée aux autres endpoints de listing
  Projet/Tranche, voir `domain/EntitesJuridiquesActives`) : ce catalogue
  transverse est lui aussi restreint aux entités juridiques actives
  (ADMOD/GHRIZA) — sinon la vue "tout le patrimoine" mélangerait des lots
  d'entités de test (BENACEUR, MAYLINE, YESTEST...). À signaler si le
  front veut un comportement différent.
