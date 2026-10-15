# EncaissementController

Règlements (StlSettlement) rattachés à un Dossier de vente — reproduit l'écran legacy "Encaissements Vente" (module Vente d'OMRAN Estate). Pour l'historique des règlements d'une seule échéance, voir plutôt `GET /api/dossiers/{id}/echeancier/{echeanceId}/reglements`, endpoint plus léger et déjà scopé à un dossier — celui-ci est la vue transverse "tous les encaissements" avec les mêmes filtres que le tableau legacy (entité, type, état, période, compte, dépôt...).

Collection soumise au périmètre commercial : sur **toutes** les routes portant un identifiant, un encaissement hors du périmètre de l'appelant renvoie 404 (jamais 403 — un 403 confirmerait son existence), et un identifiant qui n'est pas un UUID valide renvoie 404 lui aussi, plutôt que le 400 habituel : il est refusé avant d'atteindre le contrôleur. Voir `perimetreRestreint` sur GET /api/mon-compte.

**Entités juridiques en exploitation.** Ne sont renvoyés que les règlements portés par une entité juridique active (`StlSettlement.CfgCompanyId` → `CfgCompany.IsActive`) — aujourd'hui ADMOD et GHRIZA. Restriction **globale et permanente**, distincte du périmètre commercial : elle s'applique à tout appelant, rôles exemptés compris. Un règlement masqué renvoie 404 sur toutes les routes portant son identifiant.

## findAll

Toujours restreint aux règlements liés à au moins une échéance de Dossier de vente (les règlements
d'achat/location, s'il y en a, ne remontent jamais ici), et aux **encaissements** : un décaissement
(`StlSettlement.IsCredit` faux, typiquement un remboursement à montant négatif) ne remonte jamais,
comme sur l'écran legacy. `montantBrutRestant` (calculé : montant brut moins la part déjà affectée à
une échéance) et `montantNet` (montant brut moins retenue source/timbre) sont calculés côté serveur,
jamais stockés. `etat` accepte les 3 valeurs confirmées sur données réelles (IN_WORK, POINTED,
CANCELED) — l'écran legacy propose aussi "Impayé(e)"/"Perdu(e)" comme filtres mais aucune ligne
réelle ne les porte à ce jour ; passés tel quel (comparaison insensible à la casse), donc
utilisables dès qu'un vrai code sera confirmé, sans changement d'API.

**Périmètre commercial** : si l'appelant est restreint (`UtilisateurCourantDto.perimetreRestreint =
true`), cette liste ne contient que les règlements portant au moins une ligne rattachée à un dossier
d'un de ses projets affectés. Les règlements dont aucune ligne ne résout de projet restent visibles
de tous.

**Encaissements non affectés : volontairement absents.** Un règlement encaissé mais pas encore
imputé à une échéance n'a aucune ligne, donc ne remonte pas ici. L'écran legacy les affiche (il les
reconnaît sous `StlSettlementRelationView.Origin = 'StlSettlement-NonAffecte'`) : 16 pièces pour
6 435 400 DH sur les données réelles, toutes à l'état IN_WORK. Les servir suppose de changer le
critère "Vente" — décision produit en attente, voir
`instructions/FEATURE-encaissements-conformite-legataire.md` §4. Ne pas confondre avec un filtre
trop strict : c'est un choix assumé.

### param trancheId

Id(s) de tranche (CfgTranche.id, voir GET /api/patrimoine/tranches). Répétable, OR. Un règlement
remonte dès qu'une de ses échéances porte sur un lot de l'une des tranches citées. Absent = toutes.

### param colonnes

Colonnes à sérialiser, répétable ou séparées par des virgules. Absent : les 27 colonnes historiques.
Catalogue et liste blanche : `GET /api/referentiels/colonnes/encaissements`. Un nom hors catalogue
rend 400 avec la liste des noms valides. `id` est toujours servi.

### param groupeColonne

Déplie un groupe : restreint la liste aux encaissements d'un groupe renvoyé par `GET
/api/encaissements/groupes`. Attend le nom de la colonne qui a servi au groupement.

### param groupeValeur

Valeur du groupe à déplier — **la `cle` renvoyée telle quelle par `/groupes`**, sans transformation.
Omise alors que `groupeColonne` est fourni : le groupe des encaissements sans valeur sur cette
colonne.

### param granularite

Granularité appliquée quand `groupeColonne` est une date. Doit être identique à celle passée à
`/groupes`, sinon la clé ne retombe pas sur le même groupe. Défaut : MOIS.

## grouper

Reproduit la bande de groupement du logiciel de bureau : au lieu des lignes, renvoie **un groupe par
valeur distincte** de la colonne `groupBy`, chacun portant son effectif et les sommes demandées.

**Pourquoi cet endpoint plutôt qu'un groupement côté front** : la liste est paginée. Grouper une
page ne donnerait que des groupes tronqués et des totaux faux. Les effectifs et totaux ci-dessous
portent sur **tout le périmètre filtré**, pas sur une page.

**Les filtres sont exactement ceux de `GET /api/encaissements`** — mêmes noms, même sémantique,
restriction `IsCredit = 1` et périmètre commercial compris. Un même jeu de filtres des deux côtés
garantit que la somme des effectifs des groupes est égale au `totalElements` de la liste.

**Les groupes sont repliés** : pour obtenir les lignes de l'un d'eux, rappeler `GET
/api/encaissements` avec `?groupeColonne=<groupBy>&groupeValeur=<la cle du groupe>`.

Colonnes groupables et agrégeables : `GET /api/referentiels/colonnes/encaissements` (booléens
`groupable` / `agregeable`). **`dossierId` et `dossierCode` n'en sont pas**, et ce n'est pas un
oubli : un règlement n'a pas de dossier, il a des lignes rattachées à des échéances. Mesuré sur les
données réelles — 643 règlements, 654 couples (règlement, dossier), 8 règlements portant sur
plusieurs dossiers — grouper par ce chemin compterait ces 8 plusieurs fois et la somme des effectifs
ne retomberait plus sur le total de la liste. Même chose pour `montantBrutRestant` et `montantNet`,
calculés en Java et absents de la base.

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

Les règlements `CANCELED` restent comptés dans l'effectif de leur groupe et de `totaux`, mais sortent
de toutes les sommes et moyennes de montants (`montantBrut`, `montantBrutRestant`, `montantNet`,
`montantBase`).

### param granularite

Granularité de regroupement d'une colonne date. Ignorée si `groupBy` n'est pas une date. Défaut :
MOIS — grouper sur la date exacte rendrait 390 groupes pour 643 règlements, soit une liste plate.

### param inclureSansValeur

Inclure le groupe des lignes dont la colonne groupée est vide. **Faux par défaut** : l'écran veut
les valeurs qui existent, et sur une colonne peu renseignée — `depotCode` est à 0 sur 643,
`compteCode` à 12 — le groupe vide écraserait tous les autres. Passer `true` pour le récupérer : la
somme des effectifs retombe alors sur le `totalElements` de la liste non groupée.

### reponse 400

`groupBy` absent, inconnu, ou désignant une colonne non groupable ; ou `agregats` contenant une
colonne non agrégeable. Le corps (RFC 7807) porte la liste des noms acceptés.

## exporter

Classeur Excel (`.xlsx`) de la liste courante : mêmes filtres que `GET /api/encaissements`, sans pagination, périmètre commercial appliqué. Une feuille « DONNEES » ; en-têtes = libellés du catalogue de colonnes, dans l'ordre de `colonnes` ; dates, montants, nombres et booléens en cellules typées. Auteur du classeur = utilisateur connecté, titre « Encaissements », fichier `encaissements_<aaaaMMjj_HHmmss>.xlsx`. Plafond : 20 000 lignes. Les filtres de colonne de la grille (appliqués côté navigateur à la page chargée) ne sont pas transmis.

### param format

Seule valeur acceptée : `xlsx`.

### param colonnes

Colonnes exportées, dans l'ordre voulu (celui de la grille). Absent : les colonnes par défaut de la liste.

### reponse 400

`format` autre que `xlsx`, ou colonne inconnue.

## findById

Alimente le panneau détail "Pièce de règlement" côté front (`EncaissementDetail.tsx`).
`montantBrutRestant` et `montantNet` sont recalculés côté serveur, jamais stockés, comme sur la
liste.

**`affectations` : les échéances que ce règlement solde**, montant croissant — l'ordre de l'écran
légataire. C'est la réponse à « ce règlement, il a payé quoi ? », que la liste ne sait pas donner :
elle n'expose qu'un `dossierCode`, celui de la *première* ligne, et masque donc les autres sur les 8
règlements réels qui couvrent plusieurs dossiers. La somme des `montantAffecte` vaut `montantBrut -
montantBrutRestant`, sauf pour un règlement `CANCELED` : ses affectations restent listées (historique)
mais il n'en compte aucune — `montantBrutRestant` = `montantBrut`, comme dans l'échéancier. Jamais
vide : un règlement sans aucune ligne de vente n'est pas servi (404, voir ci-dessous).

**Même filtre que la liste** : un décaissement (`IsCredit` ≠ 1) ou un règlement sans ligne de vente
renvoie **404**, comme un id inconnu.

**Cette réponse n'est pas filtrée par `?colonnes=`** — une fiche dit tout, là où une grille laisse
choisir ses colonnes. Elle porte donc plus de clés que les lignes de la liste : celles hors
sélection par défaut, plus `affectations`. Aucune clé ne disparaît et aucune ne change de type.

**Périmètre commercial** : un encaissement hors du périmètre de l'appelant renvoie **404**, et non
403 — un 403 confirmerait son existence et permettrait d'énumérer les identifiants.

### reponse 404

Aucun StlSettlement pour cet id, décaissement, règlement hors Vente, ou encaissement hors du
périmètre commercial de l'appelant — ces cas sont volontairement indiscernables. Également renvoyé (plutôt qu'un 400) lorsque
l'identifiant fourni n'est pas un UUID valide.
