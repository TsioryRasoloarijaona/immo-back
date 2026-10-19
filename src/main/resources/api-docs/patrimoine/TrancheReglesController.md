# TrancheReglesController

## enregistrerFinalisations

Liste complète (`id` connu = mise à jour, absent = création, ligne absente = suppression). `entiteDossierId` est
obligatoire et unique dans la tranche ; `statutFinalisation1Id` obligatoire, `statutFinalisation2Id` facultatif, tous
deux statuts du type de dossier choisi (sinon 400 nommant `lignes[i].statutFinalisationNId`). Le CRM paramètre
seulement : il n'applique pas les commissions de finalisation aux dossiers.

## enregistrerCommissions

Liste complète, réconciliée comme les autres onglets. Refus OMRAN : `dateDebut` > `dateFin` → 400 nommant
`lignes[i].dateFin` (« La Date début doit être < à la Date fin! ») ; deux lignes de même clé (personnel, action,
commercial, catégorie ; absent = absent) dont les périodes se chevauchent → 400 « … On ne doit pas avoir un
chevauchement des périodes pour deux lignes de paramétrage identiques ». Le chevauchement suit exactement
`CfgTranche.Verif` d'OMRAN (fin absente = période sans fin). Un bloc absent vaut base sur le montant, 0 %, 0 DH.

## calculerPrevisions

Lit l'en-tête **dans le corps** (rien n'est lu ni écrit en base). Chaque période (concrétisation, finalisation) répartit
`nombreArticles` sur ses mois selon une loi normale normalisée (écart type de la période, absent = 1), arrondi au pair,
écart reporté au mois du milieu ; un mois commun aux deux périodes donne une seule ligne ; les mois à zéro sont retirés.
Refus : une des quatre dates absente → 400 « Toutes les dates doivent avoir une valeur ! ».

## enregistrerPrevisions

Écrit l'en-tête (colonnes `Base*` de la tranche) et remplace toutes les lignes `CfgPrevision`. Refus OMRAN, dans cet
ordre : Σ `quantiteConcretisation` ≠ `nombreArticles`, Σ `quantiteFinalisation` ≠ `nombreArticles`, deux lignes du même
mois → 400. Une ligne dont les deux quantités valent 0 n'est pas enregistrée. Les montants des lignes sont recalculés.

## apercuSchemas

Même sélection que l'exécution : échéances des dossiers **courants** des articles de la tranche
(`StkItem.ComFolderId` ; les dossiers annulés ou désistés en sont donc exclus), dont la catégorie est « basée sur la date
des finalisations » (`IsConfermentBasedDate`), dont la date est antérieure au début des finalisations et dont le reste
dû calculé (montant − règlements non annulés) est positif.

## modifierSchemas

Déplace la date de chaque échéance de l'aperçu au début des finalisations de la tranche, en une seule transaction ;
montants et règlements inchangés, auteur et date de modification tamponnés, une ligne d'audit par échéance. L'aide
OMRAN parle de la date d'octroi des titres fonciers : le code d'OMRAN utilise la date de début des finalisations, que
le CRM reprend.
