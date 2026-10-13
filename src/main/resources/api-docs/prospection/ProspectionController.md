# ProspectionController

Prospections (ComProspection) — pipeline commercial, qualification, interactions, devis.

**Guide d'implémentation front — tranche/article principal vs. "Autres tranches"**
Une prospection porte DEUX notions distinctes de tranche/article, à ne pas confondre côté UI :
1. **Tranche/article principal** (`ProspectionDetailDto.trancheId`/`articleId`, + leurs `...Code`/`...Description` pour l'affichage) — LE couple sur lequel porte la prospection. Modifié exclusivement via `PATCH /api/prospections/{id}/tranche-principale` (jamais via le PATCH générique `PATCH /api/prospections/{id}`, qui ne touche plus à ces deux champs). Cet endpoint garantit que l'article appartient bien à la tranche fournie (400 sinon) — avant ce garde-fou, un mauvais appel avait déjà pu créer une incohérence réelle en base (article d'une tranche assigné alors que la tranche principale en pointait une autre).
2. **Autres tranches** (`GET/POST/DELETE /api/prospections/{id}/autres-tranches`, `ComProspectionProject`) — des intérêts SECONDAIRES et indépendants, sur d'autres tranches/articles que la tranche principale (ex. le prospect est aussi intéressé par un autre programme). N'affecte jamais `trancheId`/`articleId` de la prospection. Chaque entrée porte elle aussi `trancheDescription`/`articleDescription` pour l'affichage.
**Côté front** : le champ "Tranche"/"Article" principal du formulaire de qualification doit appeler l'endpoint (1) ; l'onglet "Autres tranches" doit appeler l'endpoint (2). Ce sont deux actions utilisateur différentes, jamais interchangeables.

Collection soumise au périmètre commercial : sur **toutes** les routes portant un identifiant de prospection — y compris les routes imbriquées (interactions, devis, qualification, documents) — une prospection hors du périmètre de l'appelant renvoie 404 (jamais 403 — un 403 confirmerait son existence), et un identifiant qui n'est pas un UUID valide renvoie 404 lui aussi, plutôt que le 400 habituel : il est refusé avant d'atteindre le contrôleur. Voir `perimetreRestreint` sur GET /api/mon-compte.

**Entités juridiques en exploitation.** Ne sont renvoyées que les prospections rattachées à une entité juridique active (`CfgCompany.IsActive`) — aujourd'hui ADMOD et GHRIZA, comme dans l'application de bureau. Restriction **globale et permanente**, distincte du périmètre commercial : elle s'applique à tout appelant, rôles exemptés compris. Une prospection **sans tranche** n'est rattachée à aucune entité et reste donc visible — le lead n'est pas perdu.

## findAll

Filtre optionnellement par étape/origine/commercial (multi-sélection, une valeur pour un seul choix
se comporte comme avant), tranche(s) visée(s), plage de dates sur la date du document, et recherche
libre. Exclut les pré-prospections (voir GET /api/pre-prospections). Voir
instructions/filtres-avances.md pour le détail des filtres avancés.

**Périmètre commercial** : si l'appelant est restreint (`UtilisateurCourantDto.perimetreRestreint =
true`), cette liste ne contient que les prospections de ses projets affectés — filtrage côté
serveur, en plus des filtres ci-dessus. Le projet d'une prospection est celui de sa tranche visée ;
celles sans tranche restent visibles de tous.

### param etape

Code(s) ComProspectionKind ("Nature"/étape) à filtrer. Répéter le paramètre pour plusieurs étapes.
Absent = toutes les étapes.

### param ouvertes

Filtre d'archivage, **à trois états**. Absent : toutes les prospections. `true` : seulement les
ouvertes (`fermee` à `false` ou `null`). `false` : seulement les archivées (`fermee` à `true`).
Avant le 28/09/2026, `false` était ignoré et renvoyait toute la liste.

### param colonnes

Colonnes à renvoyer. Répétable ou séparé par des virgules. **Absent = comportement historique à
l'identique** : les colonnes du groupe `ACTIVITE` sont omises et la vue légataire n'est pas
interrogée. `id` est toujours renvoyé, demandé ou non. Un nom hors catalogue renvoie 400 — voir `GET
/api/referentiels/colonnes/prospections` pour la liste exacte.

### param groupeColonne

Déplie un groupe : restreint la liste aux prospections d'un groupe renvoyé par `GET
/api/prospections/groupes`. Attend le nom de la colonne qui a servi au groupement.

### param groupeValeur

Valeur du groupe à déplier — **la `cle` renvoyée telle quelle par `/groupes`**. Omise alors que
`groupeColonne` est fourni : le groupe des prospections sans valeur sur cette colonne.

## grouper

Reproduit la bande de groupement du logiciel de bureau : un groupe par valeur distincte de
`groupBy`, portant son effectif et les sommes demandées.

**Les effectifs portent sur tout le périmètre filtré, pas sur une page** — c'est la raison d'être de
cet endpoint : la liste étant paginée, grouper côté front ne grouperait qu'une page et fausserait
tous les totaux.

**Filtres identiques à `GET /api/prospections`**, filtres avancés et périmètre commercial compris.
Pour ouvrir un groupe : `GET /api/prospections?groupeColonne=<groupBy>&groupeValeur=<cle>`.

**Seules les colonnes servies par l'entité sont groupables.** Les colonnes du groupe `ACTIVITE`
(`joursInactivite`, `nbInteractions`…) et de `CONVERSION` viennent de la vue légataire
`ComSaleProspectionView`, chargée par une requête distincte : elles n'existent pas dans le graphe
d'entités et ne peuvent donc pas servir de clé de groupement. `GET
/api/referentiels/colonnes/prospections` donne la liste exacte via le booléen `groupable`.

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

Granularité si `groupBy` est une date. Défaut : MOIS — grouper sur la date exacte rendrait 16 588
groupes pour 17 939 lignes.

### param inclureSansValeur

Inclure le groupe des lignes dont la colonne groupée est vide. **Faux par défaut** : l'écran veut
les valeurs qui existent, et sur une colonne peu renseignée le groupe vide écraserait tous les
autres. Passer `true` pour le récupérer — la somme des effectifs retombe alors sur le
`totalElements` de la liste non groupée.

### param ouvertes

Filtre d'archivage à trois états, **identique à celui de la liste** : absent = tout, `true` =
ouvertes, `false` = archivées. Les effectifs des groupes retombent ainsi sur le `totalElements` de
la liste appelée avec la même valeur.

### reponse 400

`groupBy` absent, inconnu ou non groupable ; ou `agregats` contenant une colonne non agrégeable. Le
corps porte la liste des noms acceptés.

## findById

Sert aussi bien une prospection qualifiée qu'une pré-prospection : `GET /api/pre-prospections`
n'expose pas de route de détail propre, les deux partagent cette fiche.

**Périmètre commercial** : une prospection hors du périmètre de l'appelant renvoie **404**, et non
403 — un 403 confirmerait son existence et permettrait d'énumérer les identifiants. Même règle pour
toutes les routes imbriquées (interactions, devis, qualification, documents).

**Deux origines de données, deux comportements** :
- les champs issus de `ComProspection` et de ses associations (société, type de tiers, coordonnées,
codes de référentiel, audit) sont servis **dans tous les cas**, pré-prospection comprise ;
- le bloc `activite` vient de `ComSaleProspectionView`, qui exclut les pré-prospections : il vaut
alors `null` **en entier**. `null` signifie « non disponible », jamais « zéro ».

Les champs mesurés morts (`prix`, `surface`, avance, financement, `proposeur*`) restent exposés pour
ne pas rompre le contrat existant, mais ne doivent pas être affichés — voir leur `@Schema`.

**Tri manuel et déduplication** : `marqueurTri` (`DOUBLON`, `SPAM`, `TRAITE`) et `doublonDe*`
exposent un mécanisme que l'équipe tenait dans des champs libres. **L'endpoint les expose sans rien
filtrer** — 798 pré-prospections sont marquées doublon ou spam et 795 d'entre elles sont encore à
l'étape `1 A TRAITER` : c'est au client de décider du traitement. Voir
`instructions/FEATURE-marqueurs-tri-pre-prospection.md`.

**Sur une pré-prospection**, `fermee` (0 %), les champs `article*` (0,2 %) et `proposeur*` (0 %)
sont structurellement vides — ne pas les attendre.

### reponse 404

Aucune prospection avec cet id, ou prospection hors du périmètre commercial de l'appelant — les deux
cas sont volontairement indiscernables. Également renvoyé (plutôt qu'un 400) lorsque l'identifiant
fourni n'est pas un UUID valide.

## creerProspection

instructions/from_front.md demande #9 — le point d'entrée du bouton "Nouvelle prospection", pour le
cas où le prospect existe déjà (créé au préalable via `POST /api/prospects`, déjà câblé côté front).

**Champs obligatoires (alignés sur l'écran légataire OMRAN)** : `prospectId`, `notes` (Notes, non
vide), `origineId` (Source de contact), `typeContactId` (Type — attention, c'est
`ComActionMessageType`, à ne pas confondre avec la vocation `vocationId`), `etapeId` (Nature) et
`trancheId` (Tranche). Un de ces champs manquant ⟶ 400 nommant le champ. `commercialId` reste
optionnel (OMRAN ne le bloque pas à la création). Les autres champs sont optionnels et suivent la
même sémantique que `PATCH /api/prospections/{id}` (mise à jour uniquement des champs fournis).
`code` est toujours généré serveur (compteur `AdmSequence` "Prospection", format "AA/NNNNN"), jamais
fourni par le client. `commercialId` peut être fourni directement ici pour obtenir une prospection
immédiatement complète en un seul appel — sinon `POST /api/prospections/{id}/transformer` peut
l'assigner ensuite. `trancheId`/`articleId` suivent la même validation croisée que `PATCH
.../tranche-principale` (l'article doit appartenir à la tranche fournie). Pour un lead brut/non
identifié (walk-in, appel entrant sans prospect connu), voir plutôt le flux pré-prospection (`GET
/api/pre-prospections` + `POST .../creer-prospect` + `POST .../transformer`) — non couvert par cet
endpoint.

### reponse 400

Un champ obligatoire est manquant (`prospectId`, `notes`, `origineId`, `typeContactId`, `etapeId`,
`trancheId`), ou un id fourni (prospect/commercial/tranche/article/origine/type/nature/...) est
introuvable, ou l'article fourni n'appartient pas à la tranche fournie.

### reponse 403

Le projet de la `trancheId` fournie n'est pas dans le périmètre commercial de l'appelant (403 et non
404 : c'est l'appelant qui a nommé la tranche).

### reponse 409

Le prospect a déjà une prospection **ouverte** sur le projet de cette tranche — un prospect n'a
qu'une prospection ouverte par projet, c'est sur elle qu'on ajoute des dossiers. Corps :
`prospectionExistante` (id, code, date, nature, commercial, tranche, projet). Une prospection fermée
ne bloque pas. Voir `FEATURE-doublons-prospect-projet.md` §3.

## historique

Piste d'audit paginée (plus récent en premier) des transitions pré-prospect → prospect → prospection
tracées par cette API : "Prospect créé et lié" (POST /api/pre-prospections/{id}/creer-prospect) et
"Transformée en prospection" (POST /api/prospections/{id}/transformer). Comble une lacune constatée
sur données réelles : Omran ne trace nulle part ces deux transitions
(ComProspection.UserUpdateDateTime reste toujours NULL même sur des lignes déjà transformées, la
même ligne étant mutée en place). La date de création du pré-prospect reste disponible séparément
via ProspectionDetailDto.dateDocument (pas dupliquée ici). Le stade "Dossier de vente" a son propre
historique, voir GET /api/dossiers/{id}/historique.

## modifierProspection

Mise à jour partielle (`null` = inchangé) : origine, vocation, budget, notes... **Ne modifie plus la
tranche/l'article principal** — utiliser `PATCH /api/prospections/{id}/tranche-principale` pour ça
(voir le guide dans la description du tag "Prospections").

## modifierTranchePrincipale

Seul endpoint qui modifie `trancheId`/`articleId` d'une prospection. `trancheId` est obligatoire ;
`articleId` est optionnel mais, s'il est fourni, DOIT appartenir à `trancheId` (l'API renvoie 400
sinon — un article d'une autre tranche ne peut pas devenir article principal). Si `articleId` est
omis et que l'article actuellement assigné n'appartient plus à la nouvelle tranche, il est
automatiquement désassigné (jamais laissé dans un état incohérent). Distinct de `POST
/api/prospections/{id}/autres-tranches` (intérêts secondaires, indépendants) — voir le guide sur le
tag.

**Périmètre commercial** : rattacher la prospection à une tranche dont le projet n'est pas affecté à
l'appelant renvoie **403** — sans quoi le périmètre se contournerait en déplaçant la ressource hors
de son projet.

### reponse 409

La tranche cible est sur un **autre projet** et le prospect lié y a déjà une prospection **ouverte**
(un prospect n'a qu'une prospection ouverte par projet). Changer de tranche au sein du même projet
n'est jamais refusé. Corps : `prospectionExistante`. Voir `FEATURE-doublons-prospect-projet.md` §3.

## transformer

Dernière étape de qualification d'un lead : affecte le commercial et **garantit qu'une tranche
(projet) est rattachée**. Requiert qu'un prospect soit déjà lié via `POST
/api/pre-prospections/{id}/creer-prospect` — 400 sinon.

**La tranche est obligatoire.** Règle métier plus stricte que le légataire (qui n'impose rien au
niveau base — seule `PKey` est NOT NULL sur `ComProspection`), justifiée sur données réelles : 99,83
% des prospections qualifiées (14 700/14 725) portent déjà une tranche. Fournir `trancheId` pour la
poser/remplacer ici (avec périmètre commercial et cohérence `articleId`, comme `PATCH
.../tranche-principale`) ; si `trancheId` est omis, la prospection doit **déjà** en porter une,
faute de quoi la transformation est refusée (400).

Les relances en cours de la ligne sont désarmées (comme à l'archivage) : elles quittent « Ma journée ».

### reponse 400

`commercialId` manquant ; aucun prospect lié ; commercial/tranche/article introuvable ; l'article ne
appartient pas à la tranche ; ou aucune tranche fournie ni déjà présente.

### reponse 409

Le prospect lié a déjà une prospection **ouverte** sur le projet de la tranche effective
(`trancheId` fourni, sinon celle déjà portée) — la transformation en créerait une seconde. Ouvrir
`prospectionExistante` à la place ; une prospection fermée ne bloque pas. Voir
`FEATURE-doublons-prospect-projet.md` §3.

Aussi : la ligne **n'est plus une pré-prospection** (déjà typée prospection de vente ou location) —
la transformer réécrirait son commercial ; pour en changer, `POST /api/equipe/reaffecter`. Une ligne
sans type reste acceptée.

## archiver

Sort la prospection du pipeline : `fermee = true`, `dateFermeture` = maintenant, motif posé depuis
`raisonId` (référentiel `ComProspectionClosingReason`) et/ou `raisonTexte` (libre) — **au moins l'un
des deux est requis** (400 sinon).

Reproduit la séquence du légataire relevée dans sa trace d'audit : les **relances en cours de la
prospection sont désarmées** (`WithoutRevival`, alerte éteinte dans `AdmAlertData`), comme lors
d'une transformation en vente — elles disparaissent du tableau de bord des relances et de
`derniereAlerteDate` de la liste. Les relances rattachées à une échéance de dossier ne sont pas
touchées.

Réversible par `PATCH /api/prospections/{id}/desarchiver`.

## desarchiver

Inverse de `archiver` : `fermee` repasse à `false` et la prospection réapparaît dans la liste
`?ouvertes=true`. Sans corps.

**Fidèle au légataire : seule `fermee` change.** `dateFermeture`, `raisonFermeture*` sont
**conservés** (OMRAN ne les efface pas — 88 prospections vente ouvertes en base réelle portent
encore date et motif de clôture, ce sont des désarchivées), et les relances désarmées à l'archivage
ne sont pas réarmées. **Le badge « Archivée » doit donc se brancher sur `fermee`, jamais sur la
présence de `dateFermeture`.**

## findInteractions

Historique des interactions (appels, messages, notes, visites...) loggées sur cette prospection, les
plus récentes en premier.

**Afficher `note`, pas `description`.** `note` porte le texte saisi par le commercial. `description`
est un libellé généré valant `JJ/MM/AAAA HH:MM <auteur>` sur 100 % des lignes réelles — soit
exactement `date` + `employeNom`, déjà renvoyés séparément. Il n'est conservé que pour ne rien
casser.

## addInteraction

Journalise un appel/message/visite/note sur cette prospection — voir GET
/api/referentiels/types-contact-prospection et GET /api/referentiels/resultats-interaction.

**Champs obligatoires (règle OMRAN GEN0005)** :
- `actionTypeId` ("Action") : toujours obligatoire.
- Action de type **Note** (code `Note`, insensible à la casse) : `description` (le message) est
  obligatoire et non vide, `resultatId` est facultatif.
- Toute autre action : `resultatId` ("Résultat") est obligatoire, `description` est facultative.
- Résultat paramétré « avec alerte » (`alerteRecommandee = true` dans le référentiel) : `dateAlerte`
  est obligatoire.
- Résultat "Rendez vous pris", "RDV reporté" ou "Le prospect veut faire une visite du projet" :
  `dateRdv` est obligatoire et une entrée calendrier est créée, visible ensuite via GET
  /api/calendrier/evenements.

**Catégorie** : celle du résultat s'il y en a un, sinon la catégorie « Prospection » de l'écran
parent, comme OMRAN. Une interaction sans résultat (note) reste donc visible dans « Dernière
action », « Dernier échange » et « Résultat » de la liste des prospections.

**Quand un rendez-vous est créé, une confirmation part automatiquement au prospect, par email ET par WhatsApp** (date, heure,
adresse du projet, conseiller ; les plans seulement par email). Les deux canaux sont indépendants —
57 % des prospects ont une adresse email, 99 % un numéro exploitable, donc l'essentiel est prévenu
par au moins un des deux. L'envoi a lieu après validation de la transaction et hors du temps de
réponse : il ne peut ni retarder ni faire échouer cet appel, et la réponse ne dit pas s'il a abouti.
Pour connaître l'issue, ou pour renvoyer après avoir corrigé une coordonnée, voir POST
/api/prospections/{id}/interactions/{interactionId}/email-visite.

`description` est le champ d'entrée du message : il alimente le champ `note` de la réponse, pas
le libellé. Le libellé `description` de la réponse est **généré par le serveur**
(`JJ/MM/AAAA HH:MM <auteur>`) et ne peut pas être imposé : c'est ce qui rend les interactions créées
ici indiscernables de celles de l'application de bureau.

L'**auteur** est déduit du jeton d'authentification, jamais du corps : le laisser choisir fausserait
les KPI de performance commerciale.

**Relance (`dateAlerte`).** Une interaction datée d'une alerte devient la relance courante de la
prospection, exactement comme dans le logiciel de bureau : les relances précédentes de la
prospection sont désarmées (`WithoutRevival`, alerte éteinte dans `AdmAlertData`) et la nouvelle est
posée là où l'écran du légataire la lit. Elle disparaît donc de `derniereAlerteDate` de la liste et
du tableau de bord des relances, et la nouvelle prend sa place. **Une interaction sans `dateAlerte`
(note comprise) clôt les relances en cours** sans en poser de nouvelle : elles sont désarmées comme à
l'archivage et disparaissent de « Ma journée ». C'est un écart voulu au légataire, qui les laissait
vivantes. Les relances rattachées à une échéance de dossier et les rappels d'impayé ne sont pas touchés.

### reponse 400

`ProblemDetail` dont `detail` nomme le champ : `actionTypeId` manquant ; message manquant pour une
note ; `resultatId` manquant pour une action autre qu'une note ; `dateAlerte` manquante pour un
résultat « avec alerte » ; `dateRdv` manquante alors que le résultat l'exige ; `actionTypeId` ou
`resultatId` inconnu.

## renvoyerEmailVisite

Réexpédie la confirmation déjà envoyée automatiquement à la création de l'interaction. Deux usages :
le prospect n'avait ni adresse email ni numéro exploitable au moment de la prise de rendez-vous et
vient d'en fournir un, ou il déclare n'avoir rien reçu.

**Deux canaux, tentés indépendamment.** La réponse porte un bloc `email` et un bloc `whatsapp`,
chacun avec son propre `envoye`/`motif`/`destinataire` ; le `envoye` de premier niveau vaut vrai dès
qu'**au moins un** a abouti. L'asymétrie est la norme et non l'exception : **99 %** des prospections
avec rendez-vous portent un numéro exploitable, contre **57 %** une adresse email.

Le contenu est **recalculé à chaque appel**, jamais rejoué : une adresse de projet corrigée ou un
plan déposé depuis la prise de rendez-vous seront donc bien dans le message renvoyé.

**Un envoi qui n'a pas lieu n'est pas une erreur.** Un prospect sans destinataire exploitable donne
**200** avec `envoye = false` et un motif explicite : le commercial doit voir pourquoi rien n'est
parti, ce qu'un code 4xx ne dirait pas. `DESTINATAIRE_ABSENT` signifie qu'il faut obtenir
l'information ; `NUMERO_INEXPLOITABLE` qu'un numéro existe mais qu'il est à réparer dans la fiche
(43 cas réels : longueurs anormales et numéros fixes). Seuls l'introuvable (404) et l'interaction
qui ne planifie aucun rendez-vous (409) sont des erreurs.

**Contenu des messages** : date et heure du rendez-vous, projet visité, adresse (celle de la tranche
si elle est renseignée, sinon celle du projet — le bloc est omis plutôt qu'approximatif quand la
donnée n'est pas exploitable), nom et mobile du conseiller. Les **plans ne partent que par email** :
l'envoi d'un document par UltraMsg exige une URL publique, or les URL du stockage expirent en quinze
minutes. Ce sont ceux du lot lorsqu'il est connu — ce qui n'est le cas que dans **3 %** des prises
de rendez-vous — sinon ceux déposés au niveau du projet ; `nombrePiecesJointes = 0` est donc la
valeur normale aujourd'hui.

### param interactionId

Id de l'interaction portant le rendez-vous — celle dont le résultat vaut "Rendez vous pris", "RDV
reporté" ou "Le prospect veut faire une visite du projet".

### reponse 404

Aucune prospection avec cet id, prospection hors du périmètre commercial de l'appelant, ou
interaction inconnue / rattachée à une autre prospection.

## envoyerOffrePrix

Second temps de l'après-visite. Le **remerciement** part automatiquement au passage à l'étape « 4
APRES VISITES » (`PATCH /api/prospections/{id}/etape`) et ne demande rien ; **l'offre de prix**,
elle, s'envoie explicitement par cet endpoint, parce qu'elle suppose un prix que seul un humain peut
arrêter.

**Pourquoi ce n'est pas automatique.** Le prix vient du lot, et le lot n'est renseigné que sur **7
%** des prospections de cette étape (94 sur 1 266). Sans lot il n'y a aucun montant à annoncer —
d'où le **409** ci-dessous, qui est le cas majoritaire attendu et doit être traité comme une étape
de parcours, pas comme une panne.

**Ce que fait l'appel**, dans l'ordre : réutilise l'offre existante si la prospection en porte déjà
une **et** que l'appel n'impose ni remise ni prix (réécrire les montants modifierait un document que
le prospect détient) ; sinon crée une offre (`StlQuote`) numérotée comme le legacy ; rend la Fiche
Offre en PDF ; expédie sur les deux canaux.

**Le WhatsApp porte le montant en clair**, contrairement à la confirmation de visite. À cette étape
le rapport s'inverse : **19,7 %** des prospects ont une adresse email contre **99,7 %** un numéro
exploitable. Le PDF ne part que par email — UltraMsg exige une URL publique et les nôtres expirent
en quinze minutes — et le message WhatsApp **ne mentionne le mail que s'il est réellement parti**.

**Un envoi qui n'a pas lieu n'est pas une erreur** : 200 avec `envoye = false` et un motif par
canal. L'offre est créée avant l'envoi et le reste quoi qu'il arrive — `offreId` et `offreCode` sont
toujours renseignés.

### reponse 409

La prospection n'a pas de lot (rattachez-le via `PATCH /api/prospections/{id}/tranche-principale`
puis relancez), ou elle n'est pas à l'étape « 4 APRES VISITES ».

## getAutresTranches

Indépendant de la tranche/l'article principal (`ProspectionDetailDto.trancheId`/`articleId`) — voir
le guide dans la description du tag "Prospections" pour la distinction.

## ajouterAutreTranche

N'affecte jamais la tranche/l'article principal — utiliser `PATCH
/api/prospections/{id}/tranche-principale` pour ça. `articleId`, s'il est fourni, doit appartenir à
`trancheId` (400 sinon).

## getContacts

Les contacts (conjoint, notaire, apporteur…) du prospect lié à cette prospection — lignes
`CfgTierContact`, jointes au `CfgTier` de chaque personne pour le nom, le mobile, le téléphone et
l'e-mail. Destiné à accompagner l'affichage du détail de la prospection.

Une pré-prospection (sans prospect encore lié) renvoie une **liste vide**, jamais une erreur : le
lead n'est pas perdu.

## ajouterContact

Rattache un contact au prospect lié à la prospection. La personne-contact est soit un `CfgTier`
existant (`contactProspectId`), soit **créée à la volée** (`nouveauContact` : nom, prénom, mobile,
téléphone, e-mail…) puis liée — c'est ce que fait l'écran légataire, qui insère un nouveau `CfgTier`
puis le lien `CfgTierContact`. Fournir l'un **ou** l'autre. `relationTypeId` (type de relation,
p.ex. « Conjoint ») est obligatoire.

Même effet que `POST /api/prospects/{prospectId}/contacts`, mais adressé par la prospection : le
prospect est résolu depuis elle.

### reponse 400

`relationTypeId` manquant ; ni `contactProspectId` ni `nouveauContact` (ou les deux) ; référentiel
introuvable ; ou la prospection n'a pas encore de prospect.

## getDevis

Chaque offre inclut son détail HT/TTC complet (voir `montants`), reproduisant l'onglet "Montants" du
formulaire Offre legacy.

## creerDevis

Si `articleId` est fourni, les montants HT/TTC/TVA/prix au m² sont calculés à partir du prix
commercial de l'article (`ComSalePrice(HT)`, `Vat`) et, s'ils sont fournis, des `sousArticleIds` —
voir `CreerDevisRequest` pour le détail du calcul et des remplacements possibles (`prix`,
`remisePercent`). Sans `articleId`, l'offre est "libre": `prix` devient directement le prix de vente
TTC, sans ventilation HT/m²/sous-articles.

### reponse 400

`trancheId` manquant/introuvable, `prix` manquant alors qu'aucun `articleId` n'est fourni, ou un id
(article/sous-article/prospect/commercial/mode de paiement) est introuvable.

## getDevisDocument

Reproduit exactement le formulaire Offre legacy (entête, onglet "Montants" HT/TTC, tableau des
sous-articles avec leur propre remise) — voir `instructions/templates/priceOffer/OFFRE DE PRIX.pdf`.
Le tableau "SOUS ARTICLES" n'apparaît que si l'offre a été créée avec des `sousArticleIds`.

## getNda

Formulaire "Expression of Interest Form / Non-Disclosure Agreement" (en anglais, reproduisant le
formulaire legacy tel quel) — état civil et coordonnées du prospect lié à la prospection
(`ComProspection.cfgProspect`). L'en-tête/pied de page AYAD Private est fixe (même contenu quel que
soit le projet visé par la prospection — le NDA se signe en amont du choix d'un programme).

## getFiche

Modélisé sur le template legacy "FICHE" (prospection) — récapitulatif imprimable reprenant les mêmes
champs que `GET /api/prospections/{id}` (entête, budget/superficie recherchés, financement, alerte,
notes) plus les 15 dernières actions journalisées (`GET /api/prospections/{id}/interactions`). La
section "PROFIL" (catégorie/type/profil de prospection) du template legacy n'est pas reproduite ici
— hors périmètre de cette itération.
