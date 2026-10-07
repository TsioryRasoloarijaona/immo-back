# FEATURE — Nettoyage des commentaires et textes OpenAPI hors du Java

Branche immo-crm : `refactor/commentaires-et-docs-api`. **Aucun changement de
comportement.** Backend seulement ; le front n'est pas touché.

## Pourquoi (décision utilisateur, 2026-10-07)

Mesure sur `src/main/java` (lignes non vides) : 44 841 au total, dont
**29 596 de code**, **~10 000 de commentaires** (Javadoc + `//`) et **5 253 de
textes OpenAPI** (`@Operation`, `@Schema`, `@Parameter`, `@ApiResponse`, `@Tag`).
Dans `controller/`, les textes OpenAPI dépassent le code (3 002 contre 2 311) ;
dans `domain/`, les commentaires égalent presque le code (1 217 contre 1 418).
Le code est pénible à lire. Le but : du Java qui ne contient que du code et des
commentaires courts sur les pièges, le raisonnement long vivant dans les specs
et les textes du contrat dans des fichiers Markdown.

## Invariants (valables à chaque étape)

- **I1 — Contrat identique.** Le JSON servi par `/api-docs` est **égal** à la
  référence `crm-prototype/docs/api-docsV4.json` (capturée sur `main` le
  2026-10-07, vérifiée égale à un `/api-docs` en direct). Comparaison par
  `immo-crm/scripts/compare-api-docs.mjs <reference> <url-ou-fichier>` (à écrire,
  étape 1) : égalité JSON stricte, **sauf** dans les valeurs `description` et
  `summary` où toute suite d'espaces blancs (espaces, `\n`) est ramenée à un
  espace avant comparaison. Sortie : liste des chemins JSON qui diffèrent ;
  code de sortie ≠ 0 s'il y en a. Aucune autre tolérance.
- **I2 — Code identique.** Hors textes des annotations OpenAPI, aucun jeton de
  code ne change. Vérifié par `immo-crm/scripts/compare-code.mjs <ref-git>`
  (à écrire, étape 1) : pour chaque `.java` modifié sous `src/main`, retire les
  commentaires (`//`, `/* */`, en respectant les littéraux chaîne et les text
  blocks), retire les arguments `description`/`summary` des annotations OpenAPI
  listées plus haut, normalise les blancs, et compare avec la version de
  `<ref-git>` traitée pareil. Code de sortie ≠ 0 et liste des fichiers sinon.
  Les fichiers ajoutés par l'étape 1 (mécanisme) sont exclus de la comparaison.
  **Seule exception admise** (décision manager, lot B) : supprimer une constante
  `private static final String` qui ne servait **qu'à** composer des textes
  OpenAPI déplacés en Markdown. `compare-code` la signale ; le rapport doit la
  nommer, et le manager vérifie que c'est le seul écart.
- **I3 — Tests verts.** `./mvnw test` complet vert à la fin de chaque étape
  (env : `DB_HOST=localhost DB_NAME=yesRemsTest DB_USER=tsix
  DB_PWD=securePassword123 JWT_SECRET=test-secret-key-not-for-production-use-0123456789abcdef`).
  Les tests (`src/test`) ne sont pas nettoyés dans cette feature.
- **I4 — Aucun piège perdu.** Un commentaire qui porte un fait non évident et
  vrai (comportement du légataire, contrainte SQL Server/Hibernate, raison d'un
  choix contre-intuitif) **reste**, raccourci. En cas de doute, garder court
  plutôt que supprimer.

Pour démarrer l'application (I1) : `DB_HOST=localhost DB_PORT=49687
DB_NAME=yesRemsTest DB_USER=tsix DB_PWD=securePassword123 JWT_SECRET=…
S3_BUCKET=local S3_ENDPOINT=http://localhost:9000 S3_ACCESS_KEY=x
S3_SECRET_KEY=x ./mvnw -q spring-boot:run`, attendre `/api-docs` en 200, puis
arrêter le processus qui écoute sur 8080. Ne pas toucher à
`crm-prototype/docs/*` (la référence ne bouge pas).

## Règle des commentaires (la nouvelle norme)

**Garder** (≤ 3 lignes, au plus près de la ligne concernée) :
- un piège : « `lower()` refuse les CLOB », « `NULLIF` sinon un champ vide forme
  son propre groupe », « la vue légataire code `<=` là où l'aide écrit `=` » ;
- le *pourquoi* d'un choix qui a l'air faux sans lui ;
- un pointeur : `// voir FEATURE-xxx.md §n` quand le raisonnement complet est
  dans une spec existante.

**Supprimer** :
- les mesures et dates (« 1 507 / 48 213 », « au 08/09/2026, 5 des 15… ») —
  elles vieillissent ; si le chiffre justifie un choix, il est dans la spec
  (vérifier, et sinon ne garder que la conclusion sans le chiffre) ;
- l'historique (« jusqu'ici », « autrefois », « l'ancienne version ») — c'est
  le travail de git ;
- la paraphrase du code et les Javadoc qui répètent le nom de la méthode ;
- les Javadoc de classe de plus de 3 lignes : réduire à la phrase qui dit à quoi
  sert la classe, si elle n'est pas évidente.

Ne pas créer de nouveaux fichiers de documentation pour y déplacer des
commentaires : soit le fait est un piège (reste court dans le code), soit il est
déjà dans une spec (pointeur), soit il disparaît.

## Mécanisme Markdown pour les textes OpenAPI (étape 1)

### Emplacement et format

`src/main/resources/api-docs/<domaine>/<NomDuController>.md` pour les
opérations, `src/main/resources/api-docs/schemas/<NomDuRecord>.md` pour les DTO.
`<domaine>` = le sous-package du contrôleur (`auth`, `dossier`, …).

Contrôleur :

```markdown
# UtilisateurAdminController

Texte long du @Tag (facultatif).

## reinitialiserMotDePasse

Description longue de l'opération (Markdown, paragraphes, listes…).

### param admUserId

Description longue du paramètre.

### reponse 400

Description longue de la réponse 400.
```

- `##` = nom **Java** de la méthode du handler (pas l'operationId).
- `### param <nom>` = paramètre de requête/chemin, par son nom OpenAPI.
- `### reponse <code>` = description d'une réponse.
- Le texte sous `#` (avant le premier `##`) = description du `@Tag`.

Schéma :

```markdown
# ReinitialiserMotDePasseResponse

Description du record.

## motDePasseTemporaire

Description du champ.
```

### Règles du mécanisme

1. Un `OperationCustomizer` (handler connu → classe + méthode) et un
   `OpenApiCustomizer` (tags, schémas) lisent ces fichiers au démarrage et
   **remplacent** la description correspondante. Le `summary`, les
   `example`, `required`, `nullable` etc. restent dans les annotations.
2. Le texte injecté est le Markdown de la section, sans ligne vide de tête ni
   de queue. Choisir la mise en forme du fichier pour que I1 passe (les retours
   à la ligne de l'annotation d'origine sont libres grâce à la normalisation
   des blancs, mais pas les mots, la ponctuation, ni le Markdown).
3. **Échec au démarrage** si une section désigne une méthode, un paramètre, une
   réponse, un record ou un champ qui n'existe pas (doc périmée détectée), ou si
   un même élément a **à la fois** un texte dans l'annotation **et** dans le
   Markdown (une seule source). Message d'erreur : fichier + section.
4. **Seuil** : un texte reste dans l'annotation s'il tient sur **une ligne de
   ≤ 120 caractères** ; au-delà, il va dans le Markdown et l'argument
   `description` est retiré de l'annotation (garder l'annotation si elle porte
   encore autre chose, sinon la retirer seulement si le JSON reste égal — I1).
5. Tests JUnit du mécanisme : injection d'opération, de paramètre, de réponse,
   de tag, de schéma et de champ ; échec sur section orpheline ; échec sur
   double source. (Ce sont des tests du mécanisme ; I3 couvre le reste.)

### Pièges trouvés à l'étape 1 (à respecter aux étapes 2–3)

- **Texte du `@Tag` (sous `#`) : chaque paragraphe sur une seule ligne,
  octet pour octet comme l'ancienne chaîne.** springdoc construit `$.tags` via un
  `HashSet` dont le hash inclut la description ; un retour à la ligne ajouté
  change l'ordre de `$.tags` et casse I1, même si le texte est « égal aux blancs
  près ». (`DocsApiMarkdown.ordonnerTags`.)
- Le seuil de 120 caractères se mesure sur la chaîne (échappements résolus), pas
  sur la ligne source.
- `### reponse <code>` ne vise que les réponses déclarées par `@ApiResponse`.
- Un `##` sur une méthode surchargée est refusé ; le dossier `<domaine>` doit
  être le dernier segment du package du contrôleur.
- Les descriptions de corps de requête
  (`@io.swagger.v3.oas.annotations.parameters.RequestBody(description=…)`)
  restent dans l'annotation (pas d'emplacement Markdown).
- Un **enum** avec un long `@Schema(description)` le garde dans l'annotation :
  springdoc l'écrit en ligne (pas sous `components/schemas`), il n'a pas
  d'emplacement Markdown. Un commentaire d'une ligne dit pourquoi.
- Un **record absent de `components/schemas`** (ex. porté seulement par un
  `ProblemDetail` 409, ou données de PDF) n'a pas d'emplacement Markdown : son
  texte long reste dans l'annotation, avec un commentaire d'une ligne.
- Après suppression d'un `.md` source, supprimer aussi sa copie dans
  `target/classes/api-docs/…` : `spring-boot:run` ne la retire pas et le
  démarrage échoue en « double source ».
- Un texte d'API qui contient un chiffre ou un mot « historique » se déplace tel
  quel : I1 fige le contrat, la règle 11 ne s'applique qu'aux commentaires.

### Pilote

`controller/auth/UtilisateurAdminController` et ses DTO
(`ReinitialiserMotDePasseRequest`, `ReinitialiserMotDePasseResponse`,
`UtilisateurListItemDto`, et ceux de ses routes `/projets`), textes OpenAPI
**et** commentaires de ces fichiers. I1, I2, I3 verts.

## Étapes

Le manager commite après chaque étape verte ; une session par étape (ou par
groupe d'étapes si le contexte le permet). Ordre :

1. **Mécanisme + scripts + pilote** (ci-dessus).
2. **Textes OpenAPI**, par domaine : contrôleurs de `controller/<d>` + DTO de
   `dto/<d>` (+ `dto/common`, `dto/perimetre` avec le premier domaine qui les
   utilise). Commentaires de ces mêmes fichiers nettoyés au passage.
   Domaines : auth (reste), referentiel, patrimoine, prospect, prospection,
   dossier, encaissement, dashboard, calendrier, client, hotline, kpi, suivi,
   synchronisation, passerelle.
3. **Commentaires du reste**, par paquet : `services/<d>` (même ordre de
   domaines, plus groupement, perimetre, piecejointe, sequence, tranche),
   puis `domain`, `entities`, `repositories`, `config`, `security`, `web`,
   `mail`, `whatsapp`, `storage`, `utils`, `exceptions`.

À la fin de chaque étape, la session rapporte : fichiers touchés, lignes de
commentaires avant/après (même méthode de comptage que plus haut), sortie de
`compare-api-docs.mjs`, sortie de `compare-code.mjs main`, nombre de tests de
la suite complète.

## Hors périmètre

`src/test/**` (sauf tests du mécanisme), `crm-prototype`, la référence
`api-docsV4.json`, `pom.xml` (si le mécanisme semble demander une dépendance :
s'arrêter et le dire), toute correction de bug rencontrée en route (la noter
dans le rapport, ne pas la faire).

## Annexe — Mesures retirées du code et gardées ici

Faits chiffrés qui justifiaient un choix, retirés des commentaires par le
nettoyage et absents de toute autre spec. La conclusion reste dans le code.

- **`AutorisationVersementData` — rattachée au règlement, pas à l'échéance.**
  `StlSettlement.StlAccountId` renseigné à 76 %, `StlSettlementTypeId` 99,9 %,
  `TierId` 99,7 %, contre 2/958 et 1/958 pour les équivalents de `ComDocument` :
  un document rattaché à l'échéance serait vide sur presque toutes les lignes.
- **`FicheDossierVenteData.MontantsFiche` — Avance / Crédit / Auto-financement
  ne s'additionnent pas.** Vérifié sur l'exemple réel du template : 900 000 de
  Prix de vente = 900 000 d'Auto-financement, pas 1 800 000.
- **`ArticleSummaryDto` — un dossier peut lier plusieurs articles.** Exemple
  réel : le dossier « MBR 2022/00205 » lie « MB 18 1 04 » et « MB 18 1 04 TER ».
- **`CreerReglementRequest` — `EcheanceDate`** est renseignée sur plus de 99 %
  des règlements réels (les chiffres 3 244 / 14 / 3 218 sont dans
  `FEATURE-reglement-statut-preuve.md` §2).
- **`FiltresAvancesProspection`** : temps mesurés déplacés dans
  `INVENTAIRE-colonnes-prospection.md` §3.
- **`ProspectService.CODES_TYPE_PROSPECT` — prospect = type, pas `IsProspect`.**
  `CfgTierType.Code = "PRO"` porte 13 567 tiers contre 13 avec
  `CfgTier.IsProspect = true` (flag app-only) ; `PRORNT` en ajoute 4 ; ~13 730 tiers
  distincts liés à une `ComProspection`.
- **`ProspectionService.modifierTranchePrincipale`** : 100 % des prospections avec
  article principal ont la tranche de l'article (0 exception sur 807).
- **`NumeroTelephoneSupport.indicatifsConnus` — pas de cache** : table de 47 lignes,
  moins chère à relire qu'un cache à invalider si un pays est ajouté à chaud.
- **`IsNotRead` (`addInteraction`)** : 43 493 lignes sur 43 540 à 0 ; le 1 signale
  une note à lire, pas une interaction.
- **`DevisService.genererCodeOffre`** : `AdmSequence` ligne `Offre`, `SeqValue` = 313
  = plus grand `StlQuote.Code` (« 26/00313 ») ; compteur global jamais remis à 0
  par année (« 22/00001 » → « 26/00313 »).
- **`DevisService.calculerMontants`** : vérifié sur données réelles
  `ComSaleUnitCostHT × Area = ComSalePriceHT` et
  `ComSalePriceHT × (1 + Vat) = ComSalePrice`.
- **`telephoneProspect`** : sur 8 613 prospections ouvertes, 5 034 renseignées par
  la fiche, 7 596 par le champ temporaire, 8 591 par l'une ou l'autre.
- **`nomProspect` / `emailProspect`** : 3 394 prospections sur 17 703 sans fiche,
  dont 3 390 avec `TempDescription` ; email : 4 752 des 8 613 ouvertes (55 %).
- **`toActivite`** : colonnes non-`NULL` à 100 % mais non vides à 90 % (résultat,
  message) et 25 % (`LastActiveAlertReason`).
- **Téléphones** : 43 fiches portent un numéro que rien ne peut envoyer ;
  `000000000` et `111111111` portent 17 fiches chacun (doublons).
- **Contradiction entre specs** : `CfgEmployeeId` vide « 4 fois sur 5 »
  (`FIX-interaction-note.md` §2) contre renseigné à 77 % (37 286 / 48 278,
  `FIX-auteur-interactions.md`). À trancher.
- **`DossierService.genererCodeDossier`** : une seule séquence globale pour tous les
  préfixes (codes 379→378→377 sur APF/ART/ARS, sans trou ni remise à zéro).
- **Statut initial** : 100 % des dossiers réels avec historique (359/359)
  commencent à RESERVATION.
- **Montant d'avance de la fiche** = `AgreementAmount` (99,7 % des 358 dossiers),
  pas une colonne `AdvanceAmount`.
- **`estSorti`** ≡ `IsCancellation` de la vue légataire, vérifié 30/30
  (22 ANNULATION + 8 DESISTEMENT).
- **`idsDossiersDeTranche`** : volume borné (387 dossiers au total en base réelle).
- **Chaîne article → tranche → projet → société** vérifiée 358/358 ;
  `StkItem.StkHierarchyId` renseigné sur les 358 articles de dossier ;
  `StkItem.CfgProjectId` null sur les 358.
- **`EcheancierService`** : `IsReferenceMandatory` vrai pour CHÈQUE / CHEQUE
  NOTAIRE / SYNDIC / BANQUE / CERTIFIÉ / EFFET, référence renseignée sur > 99 %
  de ces lignes ; `TierId` renseigné sur 3 232 / 3 240 (99,75 %).
- **`FicheReservationPdfRenderer`** : les marques MBR et ART représentent
  221 dossiers réels.
- **`GenerateurCodeSequence`** : la ligne réelle porte encore `SeqYear` 2021 alors
  que les codes 2026 commencent par « 26 » (d'où `BasedOnSystemYear = 1`).
- **`AppartenanceTranche.dossier` — deux branches nécessaires** : le lien inverse ne
  couvre que 328 des 358 dossiers réels ; l'article principal (`ComFolder.StkItemId`,
  358/358) couvre les 30 autres.
- **`PerformanceCommercialService`** : auteur de l'activité et commercial affecté
  coïncident dans 86 % des cas réels.
- **`HotlineService.FICHES_MAX_PAR_NOM`** : « MOHAMED » remonte 555 lignes, soit
  ~2 775 requêtes pour une frappe ; la borne à 100 limite le pire cas.
- **`ReferentielService`** : 21 tiers banques ; 3 dépôts seulement
  (FOURNISSEUR / CLIENT / BANQUE).
- **`VisibiliteEntites.actifOuNonResolu`** : `cb.isTrue(…)` au lieu de
  `cb.equal(…, TRUE)` avait cassé neuf tests sous SQL Server.

## Annexe — Défauts possibles relevés pendant le nettoyage (non corrigés)

1. ~~**Visibilité** : `GET /api/encaissements/{id}` sans périmètre~~ — **faux
   positif** : couvert par `PerimetreInterceptor` (`verifierAccesEncaissement`),
   vérifié par le manager.
2. **Argent** : le reçu (`RecuReglementService`) imprime « remboursement » quand
   `IsCredit = true`, alors que `EcheancierService.addReglement` écrit
   `IsCredit = true` pour un encaissement client.
3. **Argent** : deux calculs du « Net à payer » — `FicheDossierVenteService.montants`
   (PriceAfterDiscount + frais + modification + pénalité + résiliation) et
   `DossierService.netAPayer` (Price + frais exclus du prix).
4. **Statuts** : un dossier dont `StrStatus.Code` est inconnu de `StatutDossier`
   échappe à `TransitionsValidees`.
5. **Specs** : `FEATURE-reglement-statut-preuve.md` §5.5 (« aucun blocage sur la
   suppression d'une PJ ») contredit `FEATURE-suppression-pieces-jointes.md` §6 ;
   `FEATURE-conformite-legataire.md` §3 dit à tort que l'onglet Historique des
   dossiers lit `StrHistory`.
6. **Visibilité (hotline)** : les requêtes pré-prospection derrière `appelsSansFiche`
   (`findByNormalizedTempTel[DansPerimetre]`, `rechercherPreProspectionsParNom…`)
   n'appliquent pas le filtre entité ; `subResourcesService.historique(tierId)` sans
   périmètre.
7. **Visibilité (patrimoine par id)** : `ProjetService.findById/findTranches`,
   `TrancheService.findById`, `ArticleService.findById/findByTranche/findSousArticles`,
   `ArticlePieceJointeService` sans filtre entité ; la réponse article contient le nom
   du client du dossier lié (R4 dit que l'accès par id doit être filtré aussi).
8. **Autorisation (suivi)** : « mes actions » = `UserName` = nom affiché ; deux
   comptes de même nom affiché se voient mutuellement (« admin » sur deux comptes).
9. **Argent (clients)** : `COALESCE(SUM(...), 0)` dans
   `ComDocumentRepository/StlSettlementLineRepository.sumAmountGroupedByClientId`
   peut arrondir à l'entier (190,75 → 191) : `totalAPayer`, `totalRegle`, `solde` de
   `GET /api/clients`.
10. **Clés API** : `CleApiAdminService.creer(compteCode)` peut lier une clé à un compte
    Administrateur, qui hérite alors de l'exemption de périmètre.
11. **Spec périmée** : `FEATURE-kpi-performance-commerciaux.md` §3 (interactions sans
    employé exclues) contredit le code et `FEATURE-suivi-actions.md` §7.

## Annexe — Incohérences du contrat relevées (non corrigées : I1 fige le texte)

À traiter dans une feature dédiée (changement de contrat → `docs:update` + front) :

1. Constante supprimée au lot D (exception I2) : `DashboardController.TRANCHE_ID`.
2. `RelanceProspectionDto.joursDeRetard` dit « de 1 à 30 pour `echeance=RETARD` »,
   l'opération `relances` dit qu'il peut dépasser 30.
3. Tag Clients et `findAll` nomment l'agrégat `nombreDossiers` ; le champ de
   `ClientListItemDto` est `nbDossiers`.
4. Textes de `/api/dashboard/annuel` sans accents (« Synthese calculee »…).
5. `HotlineFicheDto`, `HotlineResultDto`, `connu` ne décrivent que la recherche par
   téléphone ; la recherche par nom existe aussi.
6. `ActionParTypeDto.type` : « (9 sur 12 mois en base réelle) » mal placé.
7. `PATCH /api/prospections/{id}/etape` (`changerEtape`) n'a pas de `@Operation`.
8. Textes d'API contenant des mesures datées (« 643 règlements réels »,
   « 48 213 actions »…) : à reformuler sans chiffres.
