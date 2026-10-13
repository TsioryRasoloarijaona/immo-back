# DossierController

Dossiers de vente (ComFolder) — cycle de vie, échéancier, pièces jointes et génération de documents.

Collection soumise au périmètre commercial : sur **toutes** les routes portant un identifiant de dossier — y compris les routes imbriquées (échéancier, règlements, pièces jointes, historique, générateurs de documents) — un dossier hors du périmètre de l'appelant renvoie 404 (jamais 403 — un 403 confirmerait son existence), et un identifiant qui n'est pas un UUID valide renvoie 404 lui aussi, plutôt que le 400 habituel : il est refusé avant d'atteindre le contrôleur. Voir `perimetreRestreint` sur GET /api/mon-compte.

**Entités juridiques en exploitation.** Cette collection ne renvoie que les dossiers rattachés à une entité juridique active (`CfgCompany.IsActive`) — aujourd'hui ADMOD et GHRIZA, comme dans l'application de bureau. Restriction **globale et permanente**, distincte du périmètre commercial : elle s'applique à tout appelant, y compris aux rôles qui en sont exemptés (dont Administrateur). Un dossier masqué renvoie 404 sur toutes les routes portant son identifiant, exactement comme un dossier inexistant.

## findAll

Filtre optionnellement par statut (multi-sélection, une valeur pour un seul statut se comporte comme
avant), commercial, entité (projet), tranche(s), plage de dates sur un jalon au choix, et recherche
libre sur le code du dossier OU le nom/prénom/raison sociale du client lié. Voir
instructions/filtres-avances.md pour le détail des filtres avancés.

**Périmètre commercial** : si l'appelant est restreint (`UtilisateurCourantDto.perimetreRestreint =
true`), cette liste ne contient que les dossiers de ses projets affectés — filtrage côté serveur,
appliqué en plus des filtres ci-dessus. Le front ne doit pas refiltrer par projet lui-même. Les
dossiers dont le projet ne se résout pas restent visibles de tous.

### param statut

Code(s) StrStatus à filtrer. Répéter le paramètre pour sélectionner plusieurs statuts
(?statut=Réservation&statut=Concrétisation). Absent = tous les statuts.

### param trancheId

Id(s) de tranche (CfgTranche.id, StkItem.stkHierarchy). Répéter le paramètre pour plusieurs
tranches. Renvoie les dossiers dont au moins un article lié — ou l'article principal — appartient à
une des tranches demandées.

### param champDate

Jalon sur lequel appliquer dateDebut/dateFin. Ignoré si aucun des deux n'est fourni. Défaut :
RESERVATION (= date de création du dossier).

### param search

Recherche libre sur le code du dossier OU le nom/prénom/raison sociale du client lié (sous-chaîne,
insensible à la casse).

### param colonnes

Colonnes a serialiser, repetable ou separees par des virgules. Absent : les 13 colonnes historiques.
Catalogue et liste blanche : `GET /api/referentiels/colonnes/dossiers`. Un nom hors catalogue rend
400 avec la liste des noms valides. `id` est toujours servi.

### param groupeColonne

Déplie un groupe : restreint la liste aux dossiers d'un groupe renvoyé par `GET
/api/dossiers/groupes`. Attend le nom de la colonne qui a servi au groupement.

### param groupeValeur

Valeur du groupe à déplier — **la `cle` renvoyée telle quelle par `/groupes`**, sans transformation.
Omise alors que `groupeColonne` est fourni : le groupe des dossiers sans valeur sur cette colonne.

### param granularite

Granularité appliquée quand `groupeColonne` est une date. Doit être identique à celle passée à
`/groupes`, sinon la clé ne retombe pas sur le même groupe. Défaut : MOIS.

## grouper

Reproduit la bande de groupement du logiciel de bureau : au lieu des lignes, renvoie **un groupe par
valeur distincte** de la colonne `groupBy`, chacun portant son effectif et les sommes demandées.

**Pourquoi cet endpoint plutôt qu'un groupement côté front** : la liste est paginée. Grouper une
page ne donnerait que des groupes tronqués et des totaux faux. Les effectifs et totaux ci-dessous
portent sur **tout le périmètre filtré**, pas sur une page.

**Les filtres sont exactement ceux de `GET /api/dossiers`** — mêmes noms, même sémantique, périmètre
commercial compris. Un même jeu de filtres des deux côtés garantit que la somme des effectifs des
groupes est égale au `totalElements` de la liste.

**Les groupes sont repliés** : pour obtenir les lignes de l'un d'eux, rappeler `GET /api/dossiers`
avec `?groupeColonne=<groupBy>&groupeValeur=<la cle du groupe>`.

Colonnes groupables et agrégeables : `GET /api/referentiels/colonnes/dossiers` (booléens `groupable`
/ `agregeable`). Toutes les colonnes ne le sont pas, et c'est mesuré : un montant porte presque
autant de valeurs distinctes que de lignes, une date exacte aussi — d'où `granularite` pour les
dates et le refus de grouper un montant.

### param agregats

Agrégats à calculer, répétable ou séparés par des virgules.

Deux formes. Un **nom nu** (`prix`) vaut une **somme** et revient sous ce nom nu — c'est le
comportement historique, inchangé. Un nom **qualifié** (`prix:AVG`) applique la fonction nommée et
revient sous le jeton complet `prix:AVG`. Les deux formes peuvent coexister dans le même appel.

Fonctions : `SUM`, `AVG`, `MIN`, `MAX`, `COUNT` — mais **pas sur toutes les colonnes**. Sommer une
date n'a pas de sens, et une colonne calculée par différence n'a pas d'extremum exprimable. La liste
autorisée par colonne est publiée par le catalogue (`fonctions`) : le menu Σ doit s'y piloter, pas
sur une liste en dur. Une fonction refusée rend 400 avec les fonctions acceptées par cette
colonne-là.

`AVG` est calculée à partir de la somme et de l'effectif renseigné, en décimal exact et arrondie au
centime — jamais en virgule flottante.

Absent : seul l'effectif de chaque groupe est renvoyé.

### param granularite

Granularité de regroupement d'une colonne date. Ignorée si `groupBy` n'est pas une date. Défaut :
MOIS — grouper sur la date exacte rendrait 214 groupes pour 358 dossiers, soit une liste plate.

### param inclureSansValeur

Inclure le groupe des lignes dont la colonne groupée est vide. **Faux par défaut** : l'écran veut
les valeurs qui existent, et sur une colonne peu renseignée le groupe vide écraserait tous les
autres. Passer `true` pour le récupérer — la somme des effectifs retombe alors sur le
`totalElements` de la liste non groupée.

### reponse 400

`groupBy` absent, inconnu, ou désignant une colonne non groupable ; ou `agregats` contenant une
colonne non agrégeable. Le corps (RFC 7807) porte la liste des noms acceptés.

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

**Prospect → client** : si le statut initial porte l'indicateur OMRAN « Transforme le prospect en
client », le tiers passe au type client (voir `PATCH /api/dossiers/{id}/statut`).

### reponse 400

Champ requis manquant (`prospectionId`, `articleIds`, `modePaieId`, `cin`), prospection/article/mode
de paiement introuvable, prospection sans prospect lié, article déjà occupé, `remisePercent` hors
de [0 ; 100], ou — avec `remisePercent` — `prix` inférieur au prix des annexes.

## changerStatut

Change le statut du dossier. La transition doit figurer dans la table des transitions validées
(sinon **409**, avec `transitionsAutorisees`) ; un dossier sans statut courant accepte tout statut.

**Prospect → client, comme OMRAN** : si le statut cible porte l'indicateur « Transforme le prospect
en client » (`StrStatus.BoolField011` : CONCRETISATION, COMPROMIS, FORMALISE…), et que le client du
dossier est de type prospect (`PRO`, `PRORNT`, `PROPRM`, `PROROM`), son type devient le type client
correspondant (`CLI`, `CLIRNT`, `CLIPRM`, `CLIROM`), dans la même transaction. Une ligne
d'historique distincte « Prospect transformé en client » (type `STATUT`) s'ajoute alors à celle du
changement de statut, dont la note reste intacte.
Déjà client, type absent ou inconnu : rien ne change. Jamais de retour client → prospect.

**Sortie (ANNULATION ou DESISTEMENT) : le lot est libéré**, comme OMRAN, dans la même transaction.
Chaque article lié au dossier (lot principal et annexes TER / PARKING) perd son `ComFolderId` et
redevient `DISPONIBLE` ; un sous-article sans « Toujours affecté » (`AlwaysAffected`) perd aussi son
article parent. `ComFolder.StkItemId` est conservé : le détail d'un dossier sorti montre toujours son
lot principal dans `articles`, même revendu depuis. `note` est **obligatoire** (la cause, écrite dans
`CancelDescription`) ; `date` va dans `CancelDate` (maintenant si absente), pour les deux sorties.

**Réactivation ANNULATION → RESERVATION** : le lot principal doit être encore libre (sinon **409**) ;
il reprend le dossier, avec ses sous-articles « Toujours affecté » encore libres. Les annexes libérées
sans cet indicateur ne reviennent pas. `CancelDate` et `CancelDescription` restent en historique.

### reponse 400

`statut` absent ou inconnu, ou `note` absente ou vide vers ANNULATION / DESISTEMENT (« La cause est
obligatoire pour une annulation ou un désistement ») ; rien n'est écrit.

### reponse 403

Un article à libérer ou à réoccuper est sur un projet hors du périmètre commercial de l'appelant.

### reponse 404

Dossier introuvable.

### reponse 409

Transition non autorisée depuis le statut courant (le corps liste `transitionsAutorisees`), ou
réactivation d'un dossier annulé dont le lot a été revendu entre-temps (« Le lot <code> a été revendu
entre-temps : créer un nouveau dossier ») ; le statut ne change pas.

## addReglement

Crée un `StlSettlement` + sa `StlSettlementLine` de rattachement à l'échéance `echeanceId`. Le
règlement naît toujours au statut `IN_WORK`.

**Saisie complète obligatoire** (400 sur chaque manquement) : `montant` (strictement positif et au
plus égal au reste à payer de l'échéance), `dateEcheance`, `typeId` et `payeurId`. `reference` (n°
de chèque...) est requise dès que le type choisi a `TypeReglementDto.referenceObligatoire=true`
(CHÈQUE, EFFET...) ; `banqueId` est requise dès que `TypeReglementDto.banqueObligatoire=true` — ce
flag était annoncé mais pas appliqué jusqu'ici.

`typeId` et `payeurId` sont exigés parce qu'ils alimentent le « Mode de versement » et le «
Versement effectué par » de l'**autorisation d'encaissement**, à ouvrir juste après création via
`GET /api/dossiers/{id}/reglements/{reglementId}/documents/autorisation-versement` (le `reglementId`
est l'`id` renvoyé ici).

`dateReglement` et `statut=POINTED` sont **refusés** (400) : pointer un règlement exige une preuve
de paiement, impossible à fournir avant que le règlement existe. Pour saisir un règlement déjà
encaissé, enchaîner : ce `POST`, puis `POST .../reglements/{reglementId}/pieces-jointes`, puis
`PATCH .../reglements/{reglementId}/statut`.

### reponse 400

Champ obligatoire absent (`montant`, `dateEcheance`, `typeId`, `payeurId`), montant négatif/nul ou
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
le détail chronologique de chaque versement et le "Reste à payer" (négatif quand il y a
effectivement un trop perçu — c'est cette valeur, en valeur absolue, qui est reprise dans le montant
du trop perçu, y compris en toutes lettres). PDF uniquement — pas de variante Word pour ce document.

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
