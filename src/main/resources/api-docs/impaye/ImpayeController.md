# ImpayeController

Liste des échéances impayées des dossiers de vente — l'écran « Impayés » du module Vente d'OMRAN Estate (« liste des documents impayés des dossiers de vente »). Une ligne est une échéance (`ComDocument`) : catégorie d'encaissement (`IsCredit = 1`), dossier à un statut de vente effective (`StrStatus.BoolField001 = 1` : la réservation, l'annulation et le désistement en sont exclus), échue, et dont le reste à payer est strictement positif. Voir `instructions/FEATURE-impayes.md`.

## findAll

**Reste à payer : le même calcul que l'échéancier de la fiche dossier.** `montantRegle` est la somme des lignes de règlement affectées à l'échéance, règlements annulés (`CANCELED`) exclus et décaissements (`StlSettlement.IsCredit = 0`) soustraits — exactement le `montantRegle` de `GET /api/dossiers/{id}/echeancier`. `montantReste` = `montantAPayer − montantRegle`, arrondi au centime. Une ligne de cette liste est donc une échéance au statut `EN_RETARD` sur la fiche.

**Échue** (`echu`, défaut `ECHEANCE`) : date d'échéance au plus tard aujourd'hui — l'échéance du jour est impayée. `RECOUVREMENT` : date d'échéance + `RecoveryAge` jours (paramètre OMRAN, 90 par défaut) au plus tard aujourd'hui. `TOUS` : aucune condition de date.

**Société, projet, tranche** : par l'article principal du dossier (article → tranche → projet → société), jamais par `ComDocument.CfgCompanyId`. Une échéance dont la chaîne est incomplète reste listée, avec ces libellés vides.

**Totaux** : `totaux` porte sur toutes les lignes du filtre, pas sur la page ; `total` est le nombre de lignes du filtre.

**Tri** : `sort=<colonne>,asc|desc`, répétable, sur les noms du catalogue `GET /api/referentiels/colonnes/impayes`. Défaut : `dateEcheance,desc`. Les vides finissent en bas.

**Périmètre commercial** : exactement celui de `GET /api/dossiers` — une échéance n'est listée que si son dossier y est visible pour l'appelant (projets affectés, entité juridique active).

**Pas de groupement serveur** : le reste se calcule hors SQL, aucune colonne n'est groupable.

### param champDate

Date sur laquelle portent `du` et `au`. `DOCUMENT` et `ECHEANCE` désignent la même colonne pour une échéance de vente (`ComDocument.DocDate`, comme la vue OMRAN) ; `RECOUVREMENT` = date d'échéance + `RecoveryAge` jours. Ignoré sans `du` ni `au`.

### reponse 400

Paramètre invalide : valeur hors liste pour `echu` ou `champDate`, date mal formée, identifiant non UUID, ou colonne de tri inconnue (le corps RFC 7807 liste les colonnes acceptées).

## exporter

Classeur Excel (`.xlsx`) de la liste courante : mêmes filtres et même tri que `GET /api/impayes`, sans pagination, périmètre commercial appliqué. Une feuille « DONNEES » ; en-têtes = libellés du catalogue de colonnes, dans l'ordre de `colonnes` ; dates, montants et nombres en cellules typées. Fichier `impayes_<aaaaMMjj_HHmmss>.xlsx`. Plafond : 20 000 lignes.

### param format

Seule valeur acceptée : `xlsx`.

### param colonnes

Colonnes exportées, dans l'ordre voulu (celui de la grille). Absent : les colonnes par défaut de la liste. Catalogue : `GET /api/referentiels/colonnes/impayes`.

### reponse 400

`format` autre que `xlsx`, colonne inconnue, ou un filtre invalide comme sur la liste.
