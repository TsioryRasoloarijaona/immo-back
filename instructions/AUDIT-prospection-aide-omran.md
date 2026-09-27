# AUDIT — Règles « Prospection » du backend confrontées à l'aide en ligne OMRAN

Date : 27/09/2026. Source : `instructions/REFERENCE-aide-omran.md` (pages `PROSPECTIONS`,
`PROSPECTS`, `PREPROSPECTIONS`, `PROSPECT EXPRESS`, `HOT LINE`, `NATURES PROSPECTIONS`,
`ACTIONS COMMERCIALES`, `SOURCE DE CONTACT`, `SUPPORTS SOURCE DE CONTACT`, `TYPES DES ACTIONS`,
`CATEGORIES` / `SOUS-CATEGORIES DES ACTIONS`, `RAISON D'ARCHIVAGE`, `SUIVI DES ACTIONS`,
`LISTE A FAIRE`, `Règles de gestion générale` GEN0005). Pages non disponibles au moment de
l'audit (serveur d'aide hors service) : `STATUTS DES RELANCES`, `PARAMETRES SYSTEME`,
`PERSONNELS`, `TIERS`, `CONTACTS`.

Côté backend : `ProspectionController` / `ProspectionService`, `ProspectService`,
`CommercialService`, `ReferentielService`, `AlerteRelanceService`, `DossierService`, les specs
`FEATURE-*.md` citées, et deux mesures sur `yesRemsProd`.

Légende : **Conforme** — la règle du site est appliquée ; **Plus strict** — nous refusons ce
que le site accepte, décision assumée et documentée ; **Écart** — comportement différent, à
corriger ou à trancher ; **Non couvert** — la règle n'existe pas chez nous.

---

## 1. Fiche prospection

| # | Règle du site (page) | Notre backend | Verdict |
|---|---|---|---|
| 1.1 | Champs obligatoires de la fiche : **Date, Prospect, Tranche, Source de contact**. Nature, Type, Note, Commercial, Article optionnels (`PROSPECTIONS` › Fiche). | `POST /api/prospections` exige `prospectId`, `notes`, `origineId`, `typeContactId`, `etapeId`, `trancheId` ; `commercialId` optionnel. La doc `@Operation` dit « alignés sur l'écran légataire ». | **Écart de doc** : ce sont les obligatoires de `PROSPECT EXPRESS` (Nature, Type, Notes, Projet obligatoires), pas ceux de la fiche `PROSPECTIONS`. Corriger la description ; le choix de garder Notes/Type/Nature obligatoires est défendable (saisie rapide) mais doit être présenté comme tel. |
| 1.2 | La tranche (projet) n'est obligatoire que si la **nature** est paramétrée « Projet obligatoire » (`NATURES PROSPECTIONS`, champ `IsWithProjectMandatory`). | Tranche toujours obligatoire à la création et à la transformation (`FEATURE-transformation-tranche-obligatoire`, 99,83 % mesuré). Le drapeau est mappé sur `ComProspectionKind`, jamais lu. | **Plus strict**, assumé. À noter dans la spec que le légataire délègue la règle à la nature. |
| 1.3 | Unicité **prospect + tranche + article** : même article ou article vide → erreur bloquante « Il existe déjà un autre enregistrement… » ; article différent → message d'information, l'utilisateur peut continuer (`PROSPECTIONS` › Remarque 2). | Une seule prospection **ouverte** par prospect × **projet** (409 `prospectionExistante`), appliquée à la création, la transformation, le changement de tranche et `lier-prospect` (`FEATURE-doublons-prospect-projet` §3). | **Plus strict**, assumé : le projet englobe la tranche (38 couples réels répétés sur deux tranches d'un même projet) et nous ne proposons pas le « Voulez-vous continuer ? » du cas « article différent ». Une prospection fermée ne bloque pas, comme les 216 re-prospections réelles. |
| 1.4 | Sélecteur d'article : « les articles libres **et** les articles qui ont un dossier de vente dont le statut est paramétré "Peut effectuer une prospection" » (`PROSPECTIONS` › Fiche › Article). | Les listes d'articles filtrent `DISPONIBLE` / `OCCUPE` ; le drapeau de statut « Peut effectuer une prospection » (`StrStatus.BoolFieldNNN`) n'est pas mappé. La création de dossier refuse un article déjà en dossier (400). | **Écart** : un article occupé par un dossier au statut « prospectable » devrait être proposé. Identifier le `BoolField` dans `StrStatus` (`BoolFieldNNNLabel` sur `StrEntity`) avant de le mapper. |
| 1.5 | Commercial : « personnels paramétrés **avec affichage dans la liste des commerciaux** » (`PROSPECTIONS`, `PROSPECTS`, `PROSPECT EXPRESS`). | `GET /api/commerciaux` = `CfgTier` dont le kind ∈ {COM, AGC, ATC, ACC, RC} (`CommercialService.COMMERCIAL_KIND_CODES`). Le drapeau `CfgTier.IsVisibleInList` est mappé mais pas utilisé. | **Écart à corriger.** Mesuré sur `yesRemsProd` : notre critère renvoie 29 tiers dont **21 masqués** dans OMRAN (`IsVisibleInList = 0`), et omet **1 tiers visible** de kind `CA`. La liste OMRAN = `IsVisibleInList = 1` (8 tiers). C'est la réponse à la question ouverte n° 7 de `CLAUDE.md`. |
| 1.6 | « Le champ note prend le type d'action choisi dans la liste déroulante des types » (`PROSPECTIONS` › Fiche › Note). | L'interaction système créée avec la prospection porte `ComActionMessageType` = type de la prospection et `AlertMessage` = notes (`FEATURE-conformite-legataire`). | **Conforme.** |
| 1.7 | Remarque 1 : sans le droit « administrer les prospections », **Action commercial, Type et Note** passent en lecture seule après enregistrement. | `PATCH /api/prospections/{id}` modifie ces champs sans condition de rôle. | **Non couvert.** À rattacher au modèle d'habilitations (`AdmUserHabilitation`) quand il sera exploité côté écriture. |
| 1.8 | Archiver : masque « raison d'archivage + note », pose la date d'archivage, « désactive toutes les relances associées » (`PROSPECTIONS` › Actions). | `PATCH /archiver` : `raisonId` ou `raisonTexte` requis (400 sinon), `ClosingDate`, désarmement `WithoutRevival` + `AdmAlertData.Active = 0` (`AlerteRelanceService.desarmerToutes`). | **Conforme** sur les effets. **Plus strict** sur le motif : le site ne le marque pas obligatoire et 5,6 % des fermetures réelles en portent un (`FEATURE-desarchiver-prospection`). |
| 1.9 | Créer/Ouvrir un dossier : crée le dossier **et archive la prospection** ; si l'article a déjà un dossier, message (`PROSPECTIONS` › Actions). | `DossierService.create` : `IsClosed = 1`, `ClosingDate`, désarmement ; article occupé → 400 « Article déjà occupé ». « Sinon ouvre le dossier correspondant » relève du front. | **Conforme.** |
| 1.10 | Instruction : notifie le commercial par mail, icône dans la liste visible du seul commercial concerné, effacée quand il ouvre l'écran des actions (`IsNotRead`, `CfgCommercialForInstructionId` dans la vue). | Absent ; `IsNotRead` toujours posé à 0. | **Non couvert.** |
| 1.11 | Blocs « Autres tranches », « Profil » (catégories/types d'articles et de sous-articles), « Autres » (superficie de/à, budget de/à, avance, crédit, fonds propres, parrain). | `autres-tranches`, `categories`, `types-bien`, `profils`, `villes` : sous-ressources dédiées ; prix/surface exposés. Parrain (`CfgProposerId`), avance, crédit, fonds propres : non exposés. | **Conforme** sur les blocs vivants. Le bloc financier est mort en base (`INVENTAIRE-colonnes-prospection` §1) et `CfgProposerId` est renseigné sur **1 prospection sur 17 970** : ne pas exposer est justifié. |
| 1.12 | Désarchiver. | `PATCH /desarchiver`, construit sur la trace d'audit. | Le site ne documente pas ce bouton. Rien à confronter. |

## 2. Actions, interactions, alertes (GEN0005 et sous-catégories)

| # | Règle du site | Notre backend | Verdict |
|---|---|---|---|
| 2.1 | « La liste déroulante des résultats est chargée en fonction d'écran parent. » | `GET /api/referentiels/resultats-interaction` filtre les sous-catégories de la catégorie « résultat d'interaction » prospection. | **Conforme** pour l'écran prospection. |
| 2.2 | Type d'action « NOTE » : résultat facultatif, **message obligatoire**. Type ≠ « NOTE » : **résultat obligatoire**, message facultatif. | `POST /interactions` : `description` toujours obligatoire, `resultatId` jamais obligatoire, aucune notion de type « NOTE ». | **Écart** dans les deux sens : nous refusons une visite sans texte que le site accepte, et nous acceptons un appel sans résultat que le site refuse. Règle simple à poser à partir de `ComActionMessageType` (type NOTE = système). |
| 2.3 | Résultat paramétré « **avec alerte** » (`IsRevivalMandatory`) : « Date alerte » et « Message » deviennent obligatoires. | Exposé au front comme `alerteRecommandee`, « jamais imposé côté serveur » (doc de `dateAlerte`). | **Écart assumé** à l'origine, mais le site est explicite : la date est obligatoire. Recommandation : refuser (400) une interaction dont le résultat est « avec alerte » sans `dateAlerte`. |
| 2.4 | Ajouter une date alerte génère une alerte ; 4.1 une nouvelle alerte désactive la précédente ; 4.4 les alertes d'impayés restent actives ; 4.2 le passage en dossier de vente désactive tout. | `AlerteRelanceService` (26/09) : `AdmAlertData` posée, précédentes désarmées sauf `IsUnpaid`, transformation et archivage désarment tout. Vérifié en direct dans OMRAN. | **Conforme.** |
| 2.5 | 4.5 changement de statut du dossier : alertes du statut précédent désactivées sauf impayés ; 4.3 création de dossier : alertes d'impayés générées ; 5.1 transfert / désistement validé : alertes désactivées. | Rien côté `PATCH /dossiers/{id}/statut`, création de dossier, transferts. | **Non couvert** — périmètre dossier, à traiter dans un lot dossier. |
| 2.6 | Résultat « Mettre à jour le schéma de financement sur la base de la date alerte » : décale les échéances antérieures à la date alerte qui ont un reste à payer (`SOUS-CATEGORIES DES ACTIONS`). | `UpdateFinancialSchema` mappé, jamais appliqué. | **Non couvert** — périmètre dossier. |
| 2.7 | Alerte **automatique** par nature : délai d'alerte > 0 → alerte à « date dernière action + délai » ; regénérée si la nature change ; raison affichée « Délais d'inactivité de X jours » (`NATURES PROSPECTIONS`, `PROSPECTIONS` › Raison d'alerte). | Nous ne générons aucune alerte système (le légataire en a écrit 4 431, `IsSystem = 1`). La vue `ComSaleProspectionView` **calcule** cette alerte à la lecture (`LastActiveAlertDate` / `LastActiveAlertReason`) : la liste servie par la vue la montre. | **Partiel** : lecture conforme via la vue, génération absente. Le résultat de la nature « affecté au résultat d'action » n'est pas mappé sur `ComProspectionKind`. |
| 2.8 | Affichage : alerte visible « si date alerte − délai d'alerte = date du jour », et « dernière alerte active » (`PROSPECTIONS` › Liste). | Liste vue : `LastActiveAlertDate` respecte `Active = 1` et `AlertDate <= aujourd'hui + DefaultAlertDelai (0)`. Liste native : `derniereAlerteDate` renvoie la dernière relance **armée**, y compris future. | **Écart d'affichage** sur la liste native : une alerte au 28/09 apparaît dès le 26. Soit filtrer côté serveur (`AlertValidityDate <= aujourd'hui`), soit le documenter pour le front. Le tableau de bord des relances a ses propres échéances explicites, conforme. |
| 2.9 | Masque des actions : liste des notes/actions/alertes, résultat, action ; GEN0005. | `GET /interactions` journal ; `SUIVI DES ACTIONS` reproduit par `FEATURE-suivi-actions` (rang, alerte active, catégorie, depuis `ComActionMessageView`). | **Conforme.** |

## 3. Liste des prospections — colonnes

Conformes (présentes dans `ProspectionListItemDto` ou la liste « vue légataire ») : Action
commerciale, Alerte, Archivée, Code, Commercial, Créé le/par, Modifié le/par, Date
d'archivage, **Date dernière action** et **Date dernière communication** (distinguées comme
le site : toute action vs action utilisateur), Dernier résultat de communication, Délais
d'inactivité, Dossier de vente (statut), E-mail, Mobile, Nombre d'actions, Nombre de visites,
Date dernière visite, Note/raison d'archivage, Prospect, Prospect code, Source de contact,
Support, Tranche, Type de prospection, Type tiers, Société.

Absentes :

| Colonne du site | Où elle est dans la vue | Remarque |
|---|---|---|
| Raison d'alerte (« Alerte action utilisateur » / « Délais d'inactivité de X jours ») | `LastActiveAlertReason` | Exposée par la liste vue, pas par la liste native. |
| Date première visite | `FirstVisitDate` | Non exposée. |
| Date de concrétisation, Délais de concrétisation (= date concrétisation − date prospection) | colonnes de la vue | Non exposées ; le délai se calcule côté serveur. |
| Client (« description du client à partir du dossier de vente », différent du prospect en cas de transfert) | `CfgCustomer*` | Non exposé ; nous n'exposons que `dossierId`/`dossierCode`/`statutDossier`. |
| Dernier message de communication | `LastCommunicationMessage` | Non exposé (nous avons le résultat, pas le message). |
| Ville, Nature prospect, Vocation, Catégorie / Type / Présentation article, Parrain, Budget et Superficie de/à, Avance, Crédit, Fonds propres, Autres tranches | diverses | Non exposées ; le bloc financier et le parrain sont morts en base. |

## 4. Prospect

| # | Règle du site (`PROSPECTS`) | Notre backend | Verdict |
|---|---|---|---|
| 4.1 | Obligatoires personne physique : Forme juridique, Titre, Sexe, Nom, Prénom, **Mobile, E-mail**. Personne morale : Raison sociale, Mobile, Email. | `POST /api/prospects` : « nom ou raisonSociale est requis », le reste optionnel. | **Écart assumé mais non documenté.** La pratique réelle contredit le site : 5 903 fiches sur 14 040 n'ont pas d'email (`FEATURE-doublons-prospect-projet` §1). Documenter le choix dans la spec plutôt que d'aligner. |
| 4.2 | Unicité « Description + mobile » ou « Description + E-mail ». | 409 `doublons` sur nom, téléphone (9 derniers chiffres) ou email, contournable par `ignorerDoublons` ; la trace montre qu'OMRAN vérifie nom + email. | **Conforme dans l'esprit**, plus large (téléphone) et non bloquant. |
| 4.3 | « Est un ancien client », Contacts (type de relation), listes prospections / dossiers / règlements / historique du prospect. | `ancienClient`, `/contacts`, sous-ressources du prospect. | **Conforme.** |

## 5. Pré-prospection et prospect express

| # | Règle du site | Notre backend | Verdict |
|---|---|---|---|
| 5.1 | `PREPROSPECTIONS` : Date obligatoire (défaut date du jour), Description temporaire obligatoire, le reste optionnel. | `POST /api/pre-prospections` : seul `nom` (description temporaire) obligatoire, `DocDate = maintenant`. | **Conforme.** |
| 5.2 | « Créer tiers » : crée un tiers à partir de la fiche. « Valider » : « fait les contrôles nécessaires et crée une prospection ». | `creer-prospect` / `lier-prospect`, puis `transformer` (commercial **et** tranche obligatoires). | **Conforme**, avec un « Valider » **plus strict** (commercial obligatoire alors que la fiche prospection le laisse optionnel) — assumé dans `FEATURE-transformation-tranche-obligatoire`. |
| 5.3 | Données temporaires 1 à 10 (`TempField1..10`). | Non exposées. | **Non couvert**, faible enjeu (9,6 % et 7,6 % peuplés pour les deux premières, `INVENTAIRE` §2). |
| 5.4 | `PROSPECT EXPRESS` : Titre, Nom, Prénom, Pays de résidence, **Mobile unique**, Date, Source, Nature, Type, Projet, E-mail, Notes obligatoires. | C'est le flux que `POST /api/prospections` reproduit (voir 1.1) ; côté prospect, ni titre, ni pays, ni email imposés. | **Écart de doc** (1.1) et **plus souple** sur le prospect (4.1). |

## 6. Configuration prospection

| # | Règle du site | Notre backend | Verdict |
|---|---|---|---|
| 6.1 | `ACTIONS COMMERCIALES` : seules les actions **actives** apparaissent dans la liste déroulante ; une action peut être « sélectionnée par défaut » à la création. | `ReferentielService` renvoie `actionRepository.findAll()` ; `IsActive` mappé, non filtré ; pas de notion de défaut. | **Écart** : filtrer `IsActive = 1`. Défaut : non couvert (front). |
| 6.2 | `SOURCE DE CONTACT` : « Parrain obligatoire » → le champ Parrain devient obligatoire ; « Avec cadeau » → ligne de cadeau à la création du dossier ; source par défaut. | `IsWithProposer`, `IsWithGift`, `IsWithSupport` exposés dans le référentiel ; aucun n'est appliqué ; Parrain non mappé. | **Non couvert**, et défendable : 1 parrain sur 17 970 prospections. Cadeaux : périmètre dossier. |
| 6.3 | `TYPES DES ACTIONS` : type par défaut à la création. | Non couvert. | **Non couvert** (front). |
| 6.4 | `NATURES PROSPECTIONS` : voir 1.2 et 2.7. | | |
| 6.5 | `RAISON D'ARCHIVAGE` : référentiel code + description. | `ComProspectionClosingReason` utilisé par `archiver`. | **Conforme.** |
| 6.6 | `SUPPORTS SOURCE DE CONTACT` : support rattaché à une source. | `origineSupportId` ; le lien support → source (`IsWithSupport`) est exposé. | **Conforme.** |

## 7. Hotline, liste à faire

| # | Règle du site | Notre backend | Verdict |
|---|---|---|---|
| 7.1 | `HOT LINE` : recherche tiers (état, code, affiché comme, mobile, téléphone, nombre de visites, nombre de ventes, notes), disponibilités, listes prospections / dossiers / opérations / règlements / communications, « Ajouter prospection » pour le tiers trouvé. | `GET /api/hotline` : « recherche complète par numéro de téléphone ou par nom » (`FEATURE-hotline-recherche-nom`) ; disponibilités via les articles ; ajout via `POST /api/prospections`. | **Conforme** sur la recherche et l'enchaînement ; le contenu colonne par colonne des cinq listes n'a pas été vérifié ici. |
| 7.2 | `LISTE A FAIRE` / `STATUTS DES RELANCES` : listes de travail sur prospects, prospections, dossiers ; changement de statut de relance ; action groupée. | Absent. | **Non couvert** (page « Statuts des relances » non téléchargée). |

---

## Synthèse

**Conforme** : note → interaction système typée, archivage et ses effets, création de dossier
(archive + refus d'article occupé), tout le cycle des alertes utilisateur (lot du 26/09),
résultats filtrés par écran, colonnes « dernière action » vs « dernière communication », suivi
des actions, pré-prospection, référentiels raison d'archivage et supports.

**Écarts à corriger, par ordre de valeur**

1. **Commerciaux** (1.5) : remplacer le critère par kind par `CfgTier.IsVisibleInList = 1`. Notre liste montre 21 tiers qu'OMRAN cache et en oublie 1.
2. **Règles de saisie d'une interaction** (2.2, 2.3) : résultat obligatoire hors type NOTE, message obligatoire pour une NOTE, `dateAlerte` obligatoire quand le résultat est « avec alerte ».
3. **Actions commerciales actives** (6.1) : filtrer `IsActive`.
4. **Alertes futures dans la liste native** (2.8) : filtrer ou documenter.
5. **Articles « prospectables »** (1.4) : mapper le drapeau de statut « Peut effectuer une prospection » et l'appliquer au sélecteur.
6. **Doc `POST /api/prospections`** (1.1, 5.4) : citer `PROSPECT EXPRESS`, pas la fiche `PROSPECTIONS`.
7. **Colonnes de liste** (§3) : raison d'alerte, première visite, date et délai de concrétisation, client du dossier, dernier message.

**Plus strict, assumé et déjà documenté** : tranche toujours obligatoire (1.2), unicité par
projet (1.3), motif d'archivage requis (1.8), commercial requis à la transformation (5.2).
À documenter : obligatoires du prospect plus souples que le site (4.1), ce que la pratique
réelle justifie.

**Non couvert, hors de ce lot** : droits « administrer les prospections » (1.7), Instruction
(1.10), alertes système par nature (2.7), désactivation d'alertes par statut de dossier /
transfert / désistement et schéma de financement (2.5, 2.6), parrain et cadeaux (6.2),
valeurs par défaut des référentiels (6.1, 6.3), liste à faire (7.2), données temporaires (5.3).
