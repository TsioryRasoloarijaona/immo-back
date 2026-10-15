# DossierController

Dossiers de vente (ComFolder) — cycle de vie, échéancier, pièces jointes et génération de documents.

Collection soumise au périmètre commercial : sur **toutes** les routes portant un identifiant de dossier — y compris les routes imbriquées (échéancier, règlements, pièces jointes, historique, générateurs de documents) — un dossier hors du périmètre de l'appelant renvoie 404 (jamais 403 — un 403 confirmerait son existence), et un identifiant qui n'est pas un UUID valide renvoie 404 lui aussi, plutôt que le 400 habituel : il est refusé avant d'atteindre le contrôleur. Voir `perimetreRestreint` sur GET /api/mon-compte.

**Entités juridiques en exploitation.** Cette collection ne renvoie que les dossiers rattachés à une entité juridique active (`CfgCompany.IsActive`) — aujourd'hui ADMOD et GHRIZA, comme dans l'application de bureau. Restriction **globale et permanente**, distincte du périmètre commercial : elle s'applique à tout appelant, y compris aux rôles qui en sont exemptés (dont Administrateur). Un dossier masqué renvoie 404 sur toutes les routes portant son identifiant, exactement comme un dossier inexistant.

## findById

Fiche complète du dossier : statut courant et étapes atteignables, client/commercial/notaire,
articles liés, ventilation des montants et prospection d'origine.

**Périmètre commercial** : un dossier hors du périmètre de l'appelant renvoie **404**, et non 403 —
un 403 confirmerait son existence et permettrait d'énumérer les identifiants. La même règle
s'applique à *toutes* les routes imbriquées de ce dossier (échéancier, règlements, pièces jointes,
historique, générateurs de documents).

### reponse 404

Aucun dossier avec cet id, ou dossier hors du périmètre commercial de l'appelant — les deux cas sont
volontairement indiscernables. Également renvoyé (plutôt qu'un 400) lorsque l'identifiant fourni
n'est pas un UUID valide : sur les collections soumises au périmètre, un identifiant non résoluble
est refusé sans être transmis au contrôleur.

## simulerSchemaFinancement

Calcule le "Schéma de financement" (AUTO-FINANCEMENT / AVANCE / FRAIS SYNDIC) que `POST
/api/dossiers` générerait pour ce(s) article(s) et ce prix, **sans rien créer ni persister** — ni
dossier, ni échéance. Étape de simulation dans l'assistant de création : le front appelle cet
endpoint à chaque changement de prix/article pour afficher le récapitulatif avant validation finale
via `POST /api/dossiers`. `articleIds` accepte le même format que la création réelle (le premier
article détermine la tranche — barème AVANCE/SYNDIC — et la vocation — catégorie d'échéance). Les
articles n'ont pas besoin d'être DISPONIBLE ici : rien n'est réservé. Si `remisePercent` est fourni,
AVANCE et AUTO-FINANCEMENT sont calculés sur le prix **après** remise (`prix` étant alors le total
avant remise, annexes comprises — articles suivants de `articleIds`) — mêmes montants que ceux que `POST
/api/dossiers` persistera pour les mêmes paramètres. FRAIS SYNDIC ne dépend pas du prix (barème de
la tranche) et n'est donc jamais remisé.

Les sous-articles libres du lot (`StkItem.ParentId`) absents de `articleIds` sont comptés comme à la
création, qui les rattache d'office : ils sont listés dans `annexesAutomatiques` (prix de référence
compris dans `prixAvantRemise`), pour que l'aperçu égale le dossier créé.

### reponse 200

Schéma calculé. `lignes` peut être vide si aucune catégorie AUTO-FINANCEMENT/AVANCE/FRAIS SYNDIC
n'existe pour la vocation de l'article (cas très inhabituel — même règle qu'à la création).

### reponse 400

`articleIds` manquant/vide, `prix` manquant, `remisePercent` hors de [0 ; 100], un des articles est
introuvable, ou — avec `remisePercent` — `prix` inférieur au prix des annexes.

## create

Voir `POST /api/dossiers/simulation-financement` pour prévisualiser le schéma de financement avant
cet appel final. Si `prix` est fourni ici, le schéma de financement est généré et persisté
automatiquement (mêmes montants que la simulation, à condition que les mêmes articles/prix soient
utilisés), et la ventilation HT/TTC/remise du dossier est calculée comme sur l'onglet "Montants"
d'une Offre (prix commercial de l'article, taux de TVA) — sauf que le total du dossier
(`totalToPay`) inclut en plus les Frais Syndic de la tranche, contrairement au total d'une Offre.

**Périmètre commercial** : créer un dossier sur un article dont le projet n'est pas affecté à
l'appelant renvoie **403** (et non 404 : ici c'est l'appelant qui a nommé l'article, il n'y a rien à
cacher). Le projet est déduit du premier article via sa tranche.

**Offre d'origine** : `offreId` (optionnel) est écrit dans `ComFolder.StlQuoteId`, comme OMRAN
(`GenerateComFolderFromQuote`). Avant toute écriture, l'offre est contrôlée comme dans le logiciel de
bureau : introuvable ou d'une autre prospection, sans article, premier article de `articleIds` autre que
celui de l'offre, article ou sous-article de l'offre déjà
lié à un dossier, vocation de l'article différente de celle de l'offre → **400** ; projet de l'article
de l'offre hors périmètre → **403** ; offre déjà transformée en dossier, ou prospection qui a déjà un
dossier de vente → **409**. Avec une offre, le dossier reprend :
- son **échéancier** : une échéance par ligne du schéma saisi sur l'offre (`StlQuoteDocument` :
  catégorie, montant, date, société, ratio de remise), à la place du schéma calculé ; sans schéma
  saisi, le schéma calculé habituel. La requête ne porte pas de schéma : celui de l'offre s'applique
  tel quel, même si `prix` a changé ;
- le **prix des sous-articles** figurant dans l'offre (`StlQuoteItem` : avant remise HT/TTC, remise,
  après remise, TVA), au lieu du prix catalogue du jour ;
- son **taux de commission** (`CommissionRate`), que la requête ne porte pas ;
- son **commercial** (`CfgCommercialId`), à défaut celui de la prospection.

Le prix, la remise, les notes et le mode de paiement restent ceux de la requête (le formulaire les
préremplit depuis l'offre).

**Remise commerciale** — deux modes, selon `remisePercent` :
- `remisePercent` **fourni** (écran de création où le commercial saisit la remise) : `prix` est le
  total TTC **avant** remise, annexes comprises (articles suivants de `articleIds`, comptés à leur
  prix de référence) ; le principal vaut `prix` − annexes, à la place du prix commercial de
  l'article ; la remise est appliquée sur ce total et se
  répercute sur `prixApresRemise`, `prix`, `totalAPayer` **et** sur le schéma de financement (AVANCE
  / AUTO-FINANCEMENT calculés sur le prix après remise). Les FRAIS SYNDIC viennent du barème de la
  tranche et ne sont jamais remisés.
- `remisePercent` **absent** (comportement historique, inchangé) : `prix` est le prix négocié
  **après** remise, et `remise`/`remiseMontant` sont déduits de l'écart avec le prix de référence
  de l'article.

Dans les deux modes, la base est écrite comme le logiciel de bureau l'écrit : `ComFolder.Discount`
en ratio 0–1 du HT avant remise, `ComFolder.DiscountAmount` en HT. L'API relit `remise` en
pourcentage (0–100) et `remiseMontant` en HT.

**Prix de référence d'un article** : `comSalePrice` s'il est > 0, sinon `comSalePriceFloor` (prix
plancher) s'il est > 0 ; il sert de base pour l'article principal, les annexes, la simulation et
les offres. Sans prix de référence, la base avant remise est le prix négocié (remise nulle). C'est
la valeur exposée par `prixSuggere` sur les articles.

**Sans `prix`** : les montants sont quand même écrits, depuis le prix de référence des articles (0
s'ils n'en ont pas), sans frais de syndic ; aucun schéma de financement n'est généré.

**Écritures compatibles OMRAN** (le dossier apparaît dans les écrans du logiciel de bureau) : statut
initial RESERVATION (obligatoire), type de document `ComFolder`, `DocDate` à minuit, description
« <code> du jj/mm/aaaa », répartition du client principal à 100 %, superficies, prix au m², prix
contractuel, schéma texte, constantes et signatures de création. Les articles suivants de
`articleIds` deviennent les sous-articles du lot principal (ligne `ComFolderItem`, historique
« AFFECTATION »). Voir `FEATURE-interop-omran-ecritures.md` §3.

**Sous-articles du lot** : comme le logiciel de bureau, tout sous-article libre du lot principal
(`StkItem.ParentId` = le lot, sans dossier) est rattaché même s'il n'est pas dans `articleIds` ; son
prix de référence entre dans le prix des sous-articles, donc dans le prix avant remise et la superficie
globale. `prix` doit en tenir compte (avec `remisePercent`, il est le total annexes comprises). Le prix de
référence d'un lot (`comSalePrice`) inclut déjà ses sous-articles chez OMRAN : le prix du lot seul en est
déduit, pour ne pas les compter deux fois — un lot et ses enfants valent donc son `prixSuggere`.

**Type du tiers** : le client (et chaque acquéreur) est client tant qu'il a un dossier actif dont le
statut « transforme le prospect en client », prospect sinon (voir `PATCH /api/dossiers/{id}/statut`).

### reponse 400

Champ requis manquant (`prospectionId`, `articleIds`, `modePaieId`, `cin`), prospection/article/mode
de paiement introuvable, prospection sans prospect lié, article déjà occupé, `remisePercent` hors
de [0 ; 100], ou — avec `remisePercent` — `prix` inférieur au prix des annexes. Avec `offreId`, les
messages du logiciel de bureau : « Vous n'avez pas sélectionné un article ! », « Cet article n'est pas
disponible en stock ! », « Le sous article <code> n'est pas libre ! », « Cet article n'a pas la
vocation correspondante ! ».

### reponse 409

Avec `offreId` seulement. L'offre a déjà donné un dossier de vente : le `detail` nomme son code et
son id, et la propriété `dossierExistant` (`{id, code, location}`) permet de l'ouvrir. Ou la
prospection a déjà un dossier de vente : « Un dossier de vente est déjà généré sur cette prospection ! ».

### reponse 500

Statut RESERVATION introuvable dans la configuration des dossiers de vente : aucun dossier n'est
créé, le `detail` le dit.

## changerStatut

Change le statut du dossier selon le **workflow paramétré d'OMRAN** (CONFIGURATION › VENTE › Statuts /
Transitions), dans cet ordre :

1. **Transition** lue dans `StrWorkFlow` depuis le statut courant (sinon **409**, avec
   `transitionsAutorisees` = les cibles que l'appelant peut exécuter). Base sans paramétrage : table de repli
   des 34 transitions. Un dossier **sans statut** est refusé (**409**, reprise de données nécessaire).
2. **Intervenant** : un des rôles de l'appelant (ou l'appelant lui-même) doit être intervenant de la
   transition (`StrWorkFlowIntervener`), sinon **403**. L'Administrateur passe toujours.
3. `date` et `note` sont posées, puis les **champs obligatoires** de la transition et du statut cible sont
   contrôlés, sur le dossier **et** sur le client et les acquéreurs personnes physiques ; puis les **pièces à
   joindre** (une pièce « juste à cocher » peut n'être que cochée). Manque → **400**, `detail` = texte
   d'OMRAN : « Pour exécuter la transition : … Le champ '…' est obligatoire ! … Le document de type : … est
   obligatoire ! ».
4. Hors annulation / désistement : **seuil de versement** du statut cible (`CfgTrancheStatus`, ligne de la
   tranche sinon globale ; versements = réglé des échéances avance, auto-financement et crédit) — refus
   **409** « Il faut que la sommes des versements soit au moins égal à 12,50 %. Soit un montant de … ».
   `ignorerSeuil=true` le saute (Administrateur seulement, sinon **403**) et laisse une ligne d'historique.
   Puis, si le statut le demande, **schéma de financement** : prix après remise = avance + crédit +
   auto-financement au centime près (sinon **409** « Votre schéma de financement ne correspond pas… »).
5. La `date` va sur la **date associée** au statut cible (`StrStatus.StrFieldDateId` : CONCRETISATION →
   `advanceDate`, MEILLEURTAUX → `loanCompanySendDate`, CREDIT DEBLOQUE → `creditApprovalDate`, FORMALISE →
   `ownershipCertificateDate`…), ignorée s'il n'en a pas. Sans `date`, rien n'est écrit : une date déjà
   renseignée n'est jamais écrasée d'office (un retour à RESERVATION garde `docDate`) ; `dateActuelle` de
   `etapes-suivantes` dit si la colonne est déjà remplie.
   Le seuil de versement n'est contrôlé que pour un utilisateur dont un rôle porte le droit OMRAN
   `toolVerifyThreshold` (tout le monde si ce droit n'est pas paramétré).
6. `StatusValidityDate` = aujourd'hui + durée de vie du statut (vide sans durée) ; si elle change, une action
   système « l'état … du dossier de vente N° … est expiré » est posée avec son alerte à cette date. Les
   relances de la prospection sont désarmées et leurs alertes éteintes (impayés épargnés). Un statut « avec
   affectation de gestionnaire » donne au dossier sans gestionnaire le personnel de l'appelant.
7. L'historique (`StrHistory`) porte la transition exécutée (`StrWorkflowId`).

**Type client / prospect, comme OMRAN** (`Sale.cs:ManageTier`), après chaque changement : le client
principal et chaque acquéreur (`ComFolderCustomer`) qui a au moins un dossier actif (statut ni
annulation ni résiliation) dont le statut porte l'indicateur « Transforme le prospect en client »
(`StrStatus.BoolField011`) passe d'un type prospect (`PRO`, `PRORNT`, `PROPRM`, `PROROM`) au type
`CLI` ; un `CLI` qui n'en a plus aucun redevient `PRO` (`CLIRNT`, `CLIPRM`, `CLIROM` restent). `CfgTierKindId` est
remis à vide dans les deux cas. Une ligne d'historique distincte (type `STATUT`) le dit : « Prospect
transformé en client » ou « Retour prospect » (suivie du nom pour un acquéreur) ; la note du
changement de statut reste intacte.

**Sortie (ANNULATION ou DESISTEMENT) : le lot est libéré**, comme OMRAN, dans la même transaction.
Chaque article lié au dossier (lot principal et annexes TER / PARKING) perd son `ComFolderId` et
redevient `DISPONIBLE` ; un sous-article sans « Toujours affecté » (`AlwaysAffected`) perd aussi son
article parent. `ComFolder.StkItemId` est conservé : le détail d'un dossier sorti montre toujours son
lot principal dans `articles`, même revendu depuis. `note` est **obligatoire** (la cause, écrite dans
`CancelDescription`) ; `date` va dans `CancelDate` (maintenant si absente), pour les deux sorties.
Chaque sous-article libéré reçoit une ligne d'historique « ANNULATION AFFECTATION ». La sortie est
refusée (**409**) si un dossier de syndic en cours est rattaché, ou si une facture de chiffre
d'affaires du dossier n'est pas annulée par un avoir.

**Réactivation ANNULATION → RESERVATION**, comme OMRAN : le lot principal et **tous** les
sous-articles du dossier (`ComFolderItem`) doivent être libres (sinon **409**) ; tous reprennent le
dossier (sous-articles rattachés au lot, ligne « AFFECTATION »). `CancelDate` est vidée et
`CancelDescription` remise à vide.

### reponse 400

`statut` absent ou inconnu, `note` absente ou vide vers ANNULATION / DESISTEMENT (« La cause est
obligatoire pour une annulation ou un désistement »), ou champs obligatoires / pièces à joindre manquants
(texte d'OMRAN dans `detail`) ; rien n'est écrit.

### reponse 403

L'appelant n'est pas intervenant de la transition, `ignorerSeuil` demandé sans être Administrateur, ou un
article à libérer ou à réoccuper est sur un projet hors du périmètre commercial de l'appelant.

### reponse 404

Dossier introuvable.

### reponse 409

Transition non paramétrée depuis le statut courant (le corps liste `transitionsAutorisees`), dossier
sans statut, seuil de versement non atteint, schéma de financement différent du prix, réactivation d'un dossier annulé dont le lot a été revendu entre-temps (« Le lot <code>
a été revendu entre-temps : créer un nouveau dossier ») ou dont un sous-article est pris (« Le sous
article <code> n'est pas libre… »), sortie avec un dossier de syndic en cours ou une facture sans
avoir ; le statut ne change pas.

## getEtapesSuivantes

Transitions que l'utilisateur connecté peut exécuter depuis le statut courant (`StrWorkFlow` filtré par
ses rôles ; toutes pour l'Administrateur), avec pour chacune la **date associée** au statut cible
(`champDate` en camelCase, nul si le statut n'en a pas), le libellé de la transition et le drapeau
« avec proposition du passage automatique ». Liste vide pour un dossier sans statut.

## getTransitionSuggeree

Passage automatique à proposer après un règlement ou un enregistrement (OMRAN le propose à la fermeture
du dossier) : l'**unique** transition « avec proposition du passage automatique » que l'appelant peut
exécuter depuis le statut courant, si le **seuil de versement de son statut cible** est atteint (même
calcul que le refus de `PATCH …/statut`). `date` = date du dernier règlement d'une échéance avance,
auto-financement ou crédit. Aucune proposition (tous les champs nuls) sans transition automatique, avec
deux ou plus, sans seuil paramétré pour la cible, ou seuil non atteint.

## addReglement

Crée un `StlSettlement` + sa `StlSettlementLine` de rattachement à l'échéance `echeanceId`. Le
règlement naît toujours au statut `IN_WORK`. C'est `POST /api/dossiers/{id}/reglements` avec une seule
affectation `{echeanceId, montant}` ; pour répartir un règlement sur plusieurs échéances, utiliser celui-ci.

**Saisie complète obligatoire** (400 sur chaque manquement) : `montant` (strictement positif et au
plus égal au reste à payer de l'échéance), `dateEcheance` et `typeId`. `reference` (n° de chèque...)
est requise dès que le type choisi a `TypeReglementDto.referenceObligatoire=true` (CHÈQUE, EFFET...) ;
`banqueId` est requise dès que `TypeReglementDto.banqueObligatoire=true`.

`typeId` alimente le « Mode de versement » de l'**autorisation d'encaissement**, à ouvrir juste
après création via `GET /api/dossiers/{id}/reglements/{reglementId}/documents/autorisation-versement`
(le `reglementId` est l'`id` renvoyé ici). `payeurId` (« Versement effectué par ») est facultatif :
absent, c'est le client du dossier.

**Écritures compatibles OMRAN** : le tiers du règlement (`TierId`) est toujours le client du dossier,
le payeur saisi va dans `TierPayerId` ; `DocDate` = aujourd'hui à minuit ; société du projet, module
COM, type de document « encaissement », devise par défaut, description « <type> <banque> N° <réf> du
jj/mm/aaaa ». Voir `FEATURE-interop-omran-ecritures.md` §5.

`dateReglement` et `statut=POINTED` sont **refusés** (400) : pointer un règlement exige une preuve
de paiement, impossible à fournir avant que le règlement existe. Pour saisir un règlement déjà
encaissé, enchaîner : ce `POST`, puis `POST .../reglements/{reglementId}/pieces-jointes`, puis
`PATCH .../reglements/{reglementId}/statut`.

### reponse 400

Champ obligatoire absent (`montant`, `dateEcheance`, `typeId` ; `payeurId` si le dossier n'a pas de
client), montant négatif/nul ou
dépassant le reste à payer, `reference`/`banqueId` absente alors que le type la requiert, ou
`dateReglement`/`statut=POINTED` fournis.

## getReglements

Contrepartie en lecture du `POST` ci-dessus — tous les versements déjà enregistrés sur cette
échéance, plus récent d'abord, avec le détail complet de chacun : type de règlement
(`typeId`/`typeCode`/`typeLibelle`, voir `GET /api/referentiels/types-reglement`), banque
(`banqueId`/`banqueNom`, pertinent seulement si le type le requiert), tiers réglant
(`payeurId`/`payeurNom` — peut différer de l'acquéreur principal), date du document (saisie), date
d'échéance, date de règlement effectif (souvent absente), référence (n° de chèque...) et notes. `GET
/api/dossiers/{id}/echeancier` ne renvoie que le total réglé/reste par échéance
(`montantRegle`/`reste`) — cet endpoint donne le détail ligne par ligne derrière ce total.

## montants

Reproduit l'onglet « Montants » du logiciel de bureau — l'onglet ouvert par défaut sur sa fiche
dossier.

**Toujours calculé, jamais stocké** : tout est agrégé à la lecture depuis l'échéancier
(`ComDocument`) et les règlements qui s'y rattachent.

Trois niveaux de lecture :
- `parCategorie` — une ligne Montant / Réglé / Reste par catégorie d'échéancier (`AVANCE`,
  `AUTO-FINANCEMENT`, `FRAIS SYNDIC`…) ;
- `prixVente` et `netAPayer` — les deux lignes de **synthèse**, qui ne sont pas des catégories : le
  prix de vente, puis ce même prix augmenté des frais exclus du prix ;
- `totalGeneral` et `totalSoumisReglePrixTTC` — les totaux bruts.

La distinction « soumis au prix » repose sur `ComDocument.excludeFromPrice`, le booléen prévu pour
cela dans le schéma, et non sur le libellé de la catégorie, qui est du texte libre configurable.

⚠️ **Sur un dossier sorti** (`ANNULATION` / `DESISTEMENT`, signalé par `dossierSorti`), `netAPayer`
se réduit à ce qui a été encaissé et son reste vaut zéro : un dossier annulé ne reste rien devoir.
Règle vérifiée sans exception sur les 30 dossiers sortis réels.

**Signes** : le réglé d'une échéance est la somme de ses lignes de règlement, un décaissement
(`StlSettlement.IsCredit = 0`) comptant en négatif, les règlements annulés exclus ; une échéance de
catégorie au débit (remboursement, `IsCredit = 0`) se **retranche** des montants et totaux.

`tropPercu` / `tropPercuReste` : formule de la vue `ComSaleFolderView` du logiciel de bureau (total
dû − total réglé, quand il est négatif), rendue en positif ; 0 pour un dossier débiteur.

### reponse 200

Synthèse financière. `parCategorie` est vide sur un dossier sans échéancier ; les lignes de synthèse
restent servies, avec un réglé nul.

## getPiecesJointes

Retourne toutes les pièces jointes rattachées au dossier `id` (`AdmAttachedFile.CategoryId =
ComFolder.PKey`, voir la doc de
`AdmAttachedFileSummary`/`AdmAttachedFileSummaryRepository#findByDossierId` pour le rattachement
confirmé), triées par date de dépôt décroissante. Inclut toutes les catégories legacy ("Dossier de
vente...", "Yes.Com.Sales.Sale", etc.) — aucun filtrage sur `Category` n'est appliqué. Chaque pièce
jointe expose son `type` de document (ex. "PIECE D'IDENTITE", "CONTRAT"...), issu du référentiel
`AdmAttachedFileType` — peut être `null` si non renseigné en base. Les lignes "cochées mais jamais
déposées" (checklist sans fichier réel) sont exclues. Liste vide si aucune pièce jointe.

## uploadPieceJointe

Téléverse un fichier (multipart/form-data) et l'associe au dossier `id` (stocké dans
`AdmAttachedFile`, voir la doc de `AdmAttachedFileSummary` pour le rattachement confirmé à
`ComFolder`). `description` est une légende libre (ex. "CIN client", "Contrat signé") ; si absente,
le nom de fichier original est utilisé. `typeId` référence un `AdmAttachedFileType` (voir `GET
/api/referentiels/types-piece-jointe`) ; absent = type non renseigné. `creePar` est déduit du token
de l'utilisateur connecté — jamais demandé au front — et vaut `null` si non authentifiable. Le
fichier réapparaît immédiatement dans `GET /api/dossiers/{id}/pieces-jointes`.

## getFichier

**Renvoie une redirection `302` vers une URL présignée**, pas le contenu binaire : le fichier est
stocké sur S3/MinIO et transite directement entre le client et le stockage, sans passer par le
backend.

L'en-tête `Location` porte une URL signée, valable quelques minutes
(`app.stockage.presign-minutes`), qui vaut autorisation d'accès : elle n'est délivrée qu'après
vérification que la pièce appartient bien à ce dossier, et ne doit pas être journalisée ni partagée.

`Content-Type` et `Content-Disposition` sont portés par la réponse du stockage. **La disposition est
`inline`** : les PDF et les images s'affichent dans un onglet ou une visionneuse sans être
téléchargés ; les types que le navigateur ne sait pas rendre (`.docx`, `.xlsx`) retombent
d'eux-mêmes sur le téléchargement. Le nom proposé reprend la description de la pièce.

## deletePieceJointe

Retire la pièce du dossier. **Suppression logique** : la pièce disparaît immédiatement de `GET
/api/dossiers/{id}/pieces-jointes`, son `.../fichier` renvoie désormais 404, mais le fichier reste
sur le stockage en attendant un balayage hors ligne — une suppression accidentelle est donc
récupérable côté base, jamais via l'API.

L'auteur de la suppression est déduit du token de l'utilisateur connecté et enregistré
(`AppFileObject.DeletedBy`) ; il n'est jamais demandé au front.

**Réponse vide (204)** — ne rien tenter de désérialiser. Rejouer le `DELETE` sur la même pièce
renvoie 404, la pièce n'étant plus visible.

## getPayeurs

Alimente le champ « Réglé par » (`CreerReglementRequest.payeurId`, obligatoire) : l'acquéreur
principal du dossier (`ComFolder.CfgCustomerId`, rôle `ACQUEREUR_PRINCIPAL`) suivi de ses
co-acquéreurs (`ComFolderCustomer`, rôle `CO_ACQUEREUR`, avec leur quote-part). C'est la même liste
que le bloc « Nom et prénom des clients » de l'autorisation d'encaissement.

**Liste indicative, pas fermée** : sur 654 règlements réels portant un payeur, 651 sont l'acquéreur
principal, 1 un co-acquéreur et 2 ni l'un ni l'autre (tiers payeur). La création n'exige donc pas
que `payeurId` appartienne à cette liste, seulement que le tiers existe.

Liste vide si le dossier n'a aucun acquéreur rattaché.

## modifierStatutReglement

Fait évoluer `StlSettlement.Status`. Transitions autorisées : `IN_WORK → POINTED` et `IN_WORK →
CANCELED` — les deux cibles sont terminales, donc toute transition partant de `POINTED` ou
`CANCELED` renvoie 409 avec la liste `transitionsAutorisees`.

**Pointer exige une preuve de paiement** : au moins une pièce jointe doit être présente sur le
règlement (409 sinon). Déposez-la d'abord via `POST
/api/dossiers/{id}/reglements/{reglementId}/pieces-jointes`. Cette règle ne s'applique qu'**au
moment de la transition** : les règlements legacy déjà pointés sans pièce jointe restent lisibles,
et retirer une pièce jointe ne dépointe rien.

`dateReglement` est **requise** pour `POINTED` (400 sinon) et écrit `StlSettlement.SettlementDate` :
statut et date d'encaissement sont deux encodages du même fait et n'ont jamais divergé sur les 3 244
règlements réels. `CANCELED` laisse cette date à `null`.

## getPiecesJointesReglement

Pièces jointes rattachées au règlement (`AdmAttachedFile.CategoryId = StlSettlement.PKey`), plus
récente d'abord. Ce sont les justificatifs d'encaissement — chèques scannés, avis de virement, reçus
: 452 règlements réels en portent déjà. C'est leur présence qui autorise le passage à `POINTED`.
Liste vide si aucune pièce jointe.

## uploadPieceJointeReglement

Téléverse un justificatif (multipart/form-data) et l'associe au règlement. `description` est une
légende libre (ex. "Chèque BMCE n° 123", "Avis de virement") ; si absente, le nom de fichier
original est utilisé. `typeId` est facultatif et vaut `null` par défaut — les preuves de paiement
réelles n'en portent aucune. `creePar` est déduit du token, jamais demandé au front.

Une fois au moins une pièce déposée, le règlement peut passer à `POINTED` via `PATCH
.../reglements/{reglementId}/statut`.

## getFichierReglement

**Renvoie une redirection `302` vers une URL présignée**, pas le contenu binaire : le fichier est
stocké sur S3/MinIO et transite directement entre le client et le stockage, sans passer par le
backend.

L'URL signée est valable quelques minutes et vaut autorisation d'accès ; elle n'est délivrée
qu'après vérification que le règlement appartient à ce dossier et que la pièce appartient à ce
règlement.

**La disposition est `inline`** : le chèque ou l'avis de virement s'affiche dans un onglet ou une
visionneuse sans être téléchargé. Le nom proposé reprend la description de la pièce.

## deletePieceJointeReglement

Retire le justificatif du règlement. **Suppression logique**, mêmes effets que pour une pièce jointe
de dossier : disparition immédiate de la liste, 404 sur `.../fichier`, fichier conservé sur le
stockage en attendant un balayage hors ligne.

**Un règlement `POINTED` ne peut pas perdre sa dernière preuve** (409) : c'est la pièce jointe qui a
autorisé le pointage (voir `PATCH .../reglements/{reglementId}/statut`), et un règlement pointé sans
justificatif serait un trou d'audit. La règle ne vise que ce cas — supprimer une pièce parmi
plusieurs, ou n'importe quelle pièce d'un règlement `IN_WORK` ou `CANCELED`, passe normalement. Pour
remplacer l'unique preuve d'un règlement pointé : déposer la nouvelle d'abord, supprimer l'ancienne
ensuite.

**Réponse vide (204)** — ne rien tenter de désérialiser.

### reponse 404

Dossier ou règlement introuvable, règlement non rattaché à ce dossier, pièce jointe introuvable pour
ce règlement, ou pièce déjà supprimée.

## getFicheBancaire

Modélisé sur le template legacy "FICHE BANCAIRE" — fiche de soutien à une demande de crédit
immobilier : état civil complet de l'acquéreur principal et de chaque co-acquéreur
(`ComFolderCustomer`), désignation complète du bien, et détail prix/frais dossier/frais syndic avec
la part déjà réglée et le reliquat (réutilise le même calcul que `GET /api/dossiers/{id}/montants`,
jamais re-dérivé). PDF uniquement — pas de variante Word pour ce document.

## getFicheVisiteTechnique

Modélisé sur le template legacy "FICHE VISITE TECHNIQUE" — checklist d'inspection avant livraison
("FICHE D'INTERVENTION AVANT LIVRAISON"), référencée par la clause "SERVICE APRES VENTE" de l'Acte
de réservation. Les colonnes Localisation/Date/Observations de la checklist sont volontairement
laissées vides dans le PDF — remplies à la main lors de la visite physique, rien dans le schéma ne
les alimente. Inclut aussi la synthèse des obligations du règlement de copropriété et la clause de
contact SAV (adresse bureau, responsable de tranche/projet selon le code de la tranche, durée de
garantie). Le template legacy est bilingue français/arabe ; seul le contenu français est reproduit
ici. PDF uniquement.

## getFicheDossierVente

Reproduit la mise en page du template legacy Stimulsoft "FICHE COMPLETE" (`ComSaleFiche.yes`) sur 3
pages — récapitulatif interne complet du dossier : page 1 ENTETE/MONTANTS/DATE/ANNULATION, page 2
AUTRES, page 3 SCHEMA DE FINANCEMENT (avec détail des règlements)/SOUS
ARTICLES/ACQUEREURS/HISTORIQUE (réutilise `GET /api/dossiers/{id}/historique`). Le bloc MONTANTS a 9
lignes fixes recalculées depuis l'échéancier via les catégories dédiées
(Frais/Modification/Avance/Crédit/Auto-financement/Pénalité/Résiliation), distinct de `GET
/api/dossiers/{id}/montants` qui groupe par texte libre. Deux champs du template legacy n'ont pas
d'équivalent dans ce modèle de données et restent vides : "Présentation" et "Décision MEILLEURTAUX".
PDF uniquement.

## getFicheTropPercu

Modélisé bande par bande sur le template legacy Stimulsoft `ComSaleFicheTropPercu.yes` — reçu
attestant que les versements du client dépassent le prix du dossier : état civil de l'acquéreur et
de ses éventuels co-acquéreurs, désignation complète du bien (avec sous-articles), prix et total des
frais (réutilise `GET /api/dossiers/{id}/montants` pour le total des frais, jamais re-dérivé), puis
le détail chronologique de chaque versement et le "Reste à payer". Le montant du trop perçu (en
chiffres et en lettres) est le `tropPercu` de `GET .../montants` — formule de la vue `ComSaleFolderView`
du logiciel de bureau, 0 pour un dossier débiteur ; le "Reste à payer" est le reste du net à payer de
ce même calcul (frais compris), jamais négatif. PDF uniquement — pas de variante Word pour ce document.

## getRecuReglement

Modélisé sur la page vente ("Page1") du template legacy Stimulsoft
`instructions/templates/RECEIP/Yes_Stl_Front_Credit_Settlement_*.yes` (la variante locative de ce
même fichier, basée sur RntDocumentView, n'est pas couverte) — une "ATTESTATION DE RÉCEPTION CHEQUE"
pour le règlement `reglementId`, avec le texte inversé (client↔société) quand
`StlSettlement.isCredit` est vrai (remboursement plutôt que versement). Inclut la désignation du
bien, puis la "situation des chèques reçus" (tous les règlements du dossier, pas seulement celui de
ce reçu) et le "Reste à payer" (réutilise `GET /api/dossiers/{id}/montants`, hors FRAIS
DOSSIER/FRAIS SYNDIC, jamais re-dérivé). PDF uniquement.

## getAutorisationVersement

Modélisé sur le template legacy Stimulsoft
`instructions/templates/autorisation_virement/ComSaleDocumentAutorisationVersement.yes` — un
document à remettre à la banque du client, l'autorisant à accepter l'encaissement du règlement
`reglementId` (`StlSettlement.Amount`) pour le compte de la société. **Scopé au règlement
(`StlSettlement`), pas à l'échéance** : bien que le template legacy clé techniquement sur l'échéance
(`ComDocument`), les champs qui donnent sa valeur au document (compte destinataire, mode de
versement, tiers payeur) sont confirmés quasi systématiquement renseignés sur le règlement en
données réelles (`StlAccountId` 76%, `StlSettlementTypeId` 99,9%, `TierId` 99,7%) et quasi jamais
sur l'échéance correspondante (2/958, 1/958) — un document scopé échéance ressortait vide sur la
quasi-totalité des dossiers réels. Inclut la désignation de l'opération/du bien, le prix de vente
TTC du dossier, la liste des clients (acquéreur + co-acquéreurs), le RIB du compte destinataire
(`StlSettlement.StlAccountId`), le délai de retour (`CfgTranche.WithdrawalDelay`) et se termine par
un encadré "Cadre réservé à la banque" à faire remplir et signer. Le "Responsable Commercial"
affiché est `ComFolder.CfgCommercialId` (pas de session utilisateur en jeu ici, contrairement à
l'app desktop legacy). PDF uniquement — pas de variante Word pour ce document.

## getFichesReservationEnLot

Même contenu par dossier que `GET /api/dossiers/{id}/documents/fiche-reservation` (format PDF),
concaténés dans l'ordre de `dossierIds` — un groupe de pages par dossier. PDF uniquement (pas de
variante Word en lot).

## getActesReservationEnLot

Même contenu par dossier que `GET /api/dossiers/{id}/documents/acte-reservation` (format PDF),
concaténés dans l'ordre de `dossierIds`. PDF uniquement (pas de variante Word en lot).

## getFichesVisiteTechniqueEnLot

Même contenu par dossier que `GET /api/dossiers/{id}/documents/fiche-visite-technique`, concaténés
dans l'ordre de `dossierIds`.

## getFichesDossierVenteEnLot

Même contenu par dossier (3 pages chacun, voir `GET
/api/dossiers/{id}/documents/fiche-dossier-vente`) que le endpoint single-dossier, concaténés dans
l'ordre de `dossierIds`.

## getFichesTropPercuEnLot

Même contenu par dossier que `GET /api/dossiers/{id}/documents/fiche-trop-percu`, concaténés dans
l'ordre de `dossierIds` — le scénario d'origine de cette fonctionnalité : sélectionner plusieurs
dossiers en trop-perçu et imprimer toutes leurs fiches en un seul PDF.
