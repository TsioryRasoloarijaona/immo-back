# ProspectionDetailDto

## mobileProspect

Numéro de mobile **brut**, sans indicatif — 99,8 %. Le logiciel de bureau affiche `(indicatif)
numéro` ; ce formatage est laissé au front. Même champ que `mobileProspect` de la liste.

## typeProspect

Code du type de tiers — **c'est ce champ qu'il faut tester**, jamais le libellé. 100 %.

⚠️ **Ne pas confondre avec `typeProspectionCode`** : celui-ci qualifie le *tiers* (`PRO`, `PRORNT`),
l'autre qualifie le *document* (`ComProspection`, `ComPreProspection`). Même distinction que sur la
liste.

## prix

Budget déclaré. **Mesuré mort : 4 lignes sur 14 114**, et `0` — jamais `null` — ailleurs. Conservé
pour compatibilité ; ne pas afficher.

## fermee

Prospection archivée (`ComProspection.IsClosed`). **Seul indicateur fiable de l'état archivé** :
après un désarchivage, `dateFermeture` et `raisonFermeture*` restent renseignés (88 prospections
vente ouvertes en base réelle en portent). `null` équivaut à `false` (saisie OMRAN ancienne).
Bascule par `PATCH .../archiver` et `PATCH .../desarchiver`.

## raisonFermetureTexte

Motif de clôture saisi librement (`ClosingReasonText`), complément ou substitut du motif
référentiel. Conservé après désarchivage.

## articleCategorie

Catégorie de l'article principal. Renseignée sur **100 % des prospections qui portent un article** —
soit 5,7 % du total (804 sur 14 118), l'article lui-même étant rare.

## societeId

Identifiant de la société / entité juridique. **99,8 %.** Déjà servi par la liste depuis le lot B ;
la fiche ne le portait pas.

## societeLibelle

Libellé de la société. **Dérivé de la tranche** (`CfgTranche → CfgProject → CfgCompany`), jamais
saisi sur la prospection : une prospection sans tranche n'a pas de société, et ce n'est pas une
anomalie.

## creePar

Auteur de la création. **34,3 % des prospections, 27,1 % des pré-prospections** : les lignes créées
par notre API ne posent pas `UserCreate` (défaut connu). Un front qui affiche l'auteur verra deux
fiches sur trois sans auteur.

## modifiePar

Auteur de la dernière modification. **100 % des prospections, mais 28,1 % des pré-prospections** —
un lead injecté automatiquement et jamais rouvert n'a pas d'auteur de modification.

## noteQualification

Contenu du champ libre de qualification (`TempField1`), servi tel quel — seuls les espaces de
bordure sont retirés, et une valeur vide devient `null`. Ni la casse ni l'orthographe ne sont
corrigées : c'est justement ce qu'on ne sait pas interpréter.

**Son sens dépend de la population.** Sur une pré-prospection c'est un marqueur de tri (« Doublon »,
« spam »). Sur une prospection c'est le plus souvent la typologie recherchée — `salon_+_2_chambres`
(518 lignes), `salon_+_1_chambre` (213) — parfois une ville ou un budget. Servi brut précisément
parce qu'il n'est pas normalisé ; utiliser `marqueurTri` quand on cherche un marqueur.

## marqueurTri

Marqueur de tri **normalisé**, déduit de `noteQualification` par correspondance exacte (casse et
accents ignorés).

`null` dès que le texte n'est pas un marqueur reconnu — **c'est le cas le plus fréquent** (1 239
lignes de texte libre, dont les 1 200 typologies). `null` signifie « pas un marqueur connu », jamais
« pas de note » : regarder `noteQualification`.

Volumes réels : `DOUBLON` 921, `SPAM` 32, `TRAITE` 10.

## doublonDeCode

Code du tiers que cette fiche duplique (`TempField10`). 849 lignes réelles le portent, et **les 849
résolvent vers un `CfgTier` existant**. Toujours renseigné quand le pointeur existe, **même si la
résolution échoue**.

## doublonDeId

Identifiant du tiers dupliqué, une fois `doublonDeCode` résolu.

`null` si le code ne correspond à **aucun** tiers ou à **plusieurs** : `CfgTier.Code` n'est pas
unique (14 516 tiers pour 14 504 codes), et désigner un tiers au hasard serait pire que ne rien
désigner. Dans ce cas `doublonDeCode` reste affichable, sans lien.

## typeProspectionId

Identifiant du type de document légataire (`StrEntity`). `null` sur les 4 prospections dont
`StrEntityId` n'est pas renseigné.

## typeProspectionCode

Code du type de document — **c'est ce champ qu'il faut tester**, jamais le libellé. Valeurs
observées : `ComProspection` (14 114 lignes), `ComPreProspection` (3 817), `RntProspection` (4,
prospection de location).

`null` signifie que la prospection ne porte aucun type : elle est alors **invisible dans le logiciel
de bureau**, dont toutes les vues font un `INNER JOIN` sur `StrEntity`. Ce n'est pas une donnée
manquante mais une anomalie de création — 4 lignes en base réelle.

## typeProspection

Libellé d'affichage du type, traduit — « Prospection vente », « Pré-Prospection ». Ne pas s'en
servir pour tester le type.

## activite

Bloc d'indicateurs d'activité calculés par la base légataire (nombre d'interactions, inactivité,
dernière communication, visites, relance en cours, projet, et le lien vers le dossier issu de la
prospection).

**Vaut `null` en entier sur une pré-prospection** : ces indicateurs viennent de
`ComSaleProspectionView`, qui exclut les pré-prospections. L'absence du bloc signifie « non
disponible », jamais « zéro ». Les autres champs de cette fiche restent, eux, renseignés sur une
pré-prospection.
