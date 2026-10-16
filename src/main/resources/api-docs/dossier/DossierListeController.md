# DossierListeController

Liste des dossiers de vente à parité avec la grille OMRAN (`SaleMonitoringList`) : colonnes, filtres, lots libres, groupes et totaux, export Excel. La liste, les groupes et l'export partagent exactement les mêmes filtres et le même périmètre commercial (voir FEATURE-vente-liste.md).

## findAll

Filtre optionnellement par statut (codes `StrStatus`, multi-sélection), commercial, projet, tranche(s), période sur une des 47 dates d'OMRAN (`GET /api/dossiers/champs-date`) et recherche libre.

**Dossiers sortis exclus par défaut** : sans filtre `statut`, les dossiers dont le statut est une annulation ou une résiliation (`StrStatus.BoolField002` / `BoolField003`) n'apparaissent pas, comme dans OMRAN. `inclureSortis=true` les réintègre ; un `statut` explicite (par exemple `ANNULATION`) est toujours servi tel quel.

**Lots libres** : `lignes=DOSSIERS_ET_LIBRES` ajoute, après les dossiers, une ligne par article racine sans dossier (`StkItem.ComFolderId` vide, hors annexes) ; `lignes=LIBRES` ne sert qu'eux. Sur ces lignes `libre = true` et `id` est l'id de l'article (à ouvrir sur la fiche article, pas sur la fiche dossier). Un article principal d'un dossier non sorti n'est jamais libre, même sans `StkItem.ComFolderId`. Le filtre commercial les écarte ; la période (`champDate`, `dateDebut`, `dateFin`, `dateVide`) ne les filtre pas, faute de date de dossier. La pagination court sur la concaténation dossiers puis lots libres.

**Périmètre commercial** : si l'appelant est restreint (`UtilisateurCourantDto.perimetreRestreint = true`), seuls les dossiers et lots de ses projets affectés sont servis. Les dossiers dont le projet ne se résout pas restent visibles de tous.

### param statut

Code(s) `StrStatus.Code` à filtrer, tels que renvoyés par `GET /api/dossiers/statuts`. Répéter le paramètre pour plusieurs statuts. Absent = tous les statuts sauf les sortis (voir `inclureSortis`).

### param trancheId

Id(s) de tranche (CfgTranche.id). Répéter le paramètre pour plusieurs tranches. Renvoie les dossiers dont au moins un article lié — ou l'article principal — appartient à une des tranches demandées.

### param champDate

Date sur laquelle appliquer `dateDebut` / `dateFin` ou `dateVide`. Défaut : RESERVATION (date de création du dossier). Les valeurs et leurs libellés : `GET /api/dossiers/champs-date`.

### param dateVide

`true` : seulement les dossiers dont la date `champDate` est vide ; `false` : seulement ceux où elle est renseignée. Prioritaire sur `dateDebut` / `dateFin`. Absent : pas de condition de présence.

### param search

Recherche libre, espaces ignorés des deux côtés, sur les 16 champs d'OMRAN : code du dossier, raison sociale, libellé, prénom, nom, prénom+nom et nom+prénom du client, pièce d'identité (CIN), mobile seul, indicatif+mobile, « + » ou « 00 » devant, code et libellé de l'article, catégorie, type et type de présentation de l'article, et libellé des co-acquéreurs (`ComFolderCustomer`).

### param inclureSortis

Réintègre les dossiers annulés ou résiliés quand aucun `statut` n'est demandé. Faux par défaut.

### param lignes

Population de la liste : `DOSSIERS` (défaut), `DOSSIERS_ET_LIBRES`, `LIBRES`.

### param ordreOmran

Remplace le tri reçu par celui d'OMRAN : code de l'article principal, puis tranche, projet et entité. Faux par défaut : le `sort` reçu s'applique.

### param colonnes

Colonnes à sérialiser, répétable ou séparées par des virgules. Absent : les 13 colonnes historiques. Catalogue et liste blanche : `GET /api/referentiels/colonnes/dossiers`. Un nom hors catalogue rend 400 avec la liste des noms valides. `id` est toujours servi.

Les colonnes renseignées sur moins de 1 % des dossiers réels ont quitté le catalogue (relances, CPF, certificat, actes de précision, minute avocat et client, légalisation, fiches assurance et intervention, remise des clés client, promesses de vente, visite technique, envoi et décision MEILLEURTAUX en libellé, octroi du crédit et ses deux délais, gestionnaire 2, `numeroTitre`…) : les demander rend 400. Leurs dates restent des filtres de période (`champDate`).

Les montants par nature (`avance`, `credit`, `frais`, `netAPayerReste`, `tropPercu`…) sont calculés par page depuis l'échéancier et les règlements : une de ces colonnes demandée coûte deux requêtes par page, aucune si elles sont absentes.

### param groupeColonne

Déplie un groupe : restreint la liste aux dossiers d'un groupe renvoyé par `GET /api/dossiers/groupes`. Attend le nom de la colonne qui a servi au groupement. Un dépliage ne sert jamais de lot libre.

### param groupeValeur

Valeur du groupe à déplier — **la `cle` renvoyée telle quelle par `/groupes`**, sans transformation. Omise alors que `groupeColonne` est fourni : le groupe des dossiers sans valeur sur cette colonne.

### param granularite

Granularité appliquée quand `groupeColonne` est une date. Doit être identique à celle passée à `/groupes`, sinon la clé ne retombe pas sur le même groupe. Défaut : MOIS.

### reponse 400

Colonne inconnue dans `colonnes`, valeur d'énumération invalide (`champDate`, `lignes`, `granularite`) ou `groupeColonne` non groupable.

## grouper

Reproduit la bande de groupement du logiciel de bureau : au lieu des lignes, renvoie **un groupe par valeur distincte** de la colonne `groupBy`, chacun portant son effectif et les sommes demandées.

**Totaux de la liste (Σ en tête)** : le champ `totaux` de la réponse porte les agrégats demandés sur **toutes** les lignes filtrées. Avec `inclureSansValeur=true`, il couvre exactement la population de `GET /api/dossiers` (hors lots libres, qui n'ont pas de prix de vente).

**Les filtres sont exactement ceux de `GET /api/dossiers`** — mêmes noms, même sémantique, sortis exclus par défaut, périmètre commercial compris. Un même jeu de filtres des deux côtés garantit que la somme des effectifs des groupes est égale au `totalElements` de la liste.

**Les groupes sont repliés** : pour obtenir les lignes de l'un d'eux, rappeler `GET /api/dossiers` avec `?groupeColonne=<groupBy>&groupeValeur=<la cle du groupe>`.

Colonnes groupables et agrégeables : `GET /api/referentiels/colonnes/dossiers` (booléens `groupable` / `agregeable`). Une colonne calculée par page (dernière communication, alerte, sous-articles…) n'est ni l'un ni l'autre.

### param agregats

Agrégats à calculer, répétable ou séparés par des virgules.

Deux formes. Un **nom nu** (`prix`) vaut une **somme** et revient sous ce nom nu. Un nom **qualifié** (`prix:AVG`) applique la fonction nommée et revient sous le jeton complet `prix:AVG`. Les deux formes peuvent coexister dans le même appel.

Fonctions : `SUM`, `AVG`, `MIN`, `MAX`, `COUNT` — selon la colonne (`fonctions` du catalogue). Une fonction refusée rend 400 avec les fonctions acceptées par cette colonne-là.

Absent : seul l'effectif de chaque groupe est renvoyé.

### param granularite

Granularité de regroupement d'une colonne date. Ignorée si `groupBy` n'est pas une date. Défaut : MOIS.

### param inclureSansValeur

Inclure le groupe des lignes dont la colonne groupée est vide. **Faux par défaut**. Passer `true` pour que la somme des effectifs et les `totaux` retombent sur le `totalElements` de la liste non groupée.

### reponse 400

`groupBy` absent, inconnu, ou désignant une colonne non groupable ; ou `agregats` contenant une colonne non agrégeable. Le corps (RFC 7807) porte la liste des noms acceptés.

## totaux

Σ en tête des colonnes de montants par nature, sur **toutes** les lignes filtrées (pas seulement la page) : mêmes filtres, mêmes statuts visibles et même périmètre commercial que `GET /api/dossiers`. Chaque montant est celui de la colonne du même nom, dossier par dossier, puis additionné — jamais un second calcul. `sommes` a une clé par colonne additionnable (`prixContractuel`, `prixContractuelRegle`, `prixContractuelReste`, `prixRegle`, `prixReste`, `netAPayerRegle`, `netAPayerReste`, `avance`, `avanceReglee`, `avanceReste`, `autofinancement`, `autofinancementRegle`, `autofinancementReste`, `credit`, `creditRegle`, `creditReste`, `frais`, `fraisRegles`, `fraisReste`, `tropPercu`, `tropPercuRegle`, `tropPercuReste`) ; les taux ne s'additionnent pas et n'y figurent pas. Les lots libres n'ont pas de montant : `lignes=LIBRES` rend des sommes nulles. Les Σ des colonnes de table (`prix`, `totalAPayer`…) restent servies par `GET /api/dossiers/groupes` (`totaux`).

### reponse 400

Valeur d'énumération invalide (`champDate`, `lignes`).

## exporter

Classeur Excel (`.xlsx`) de la liste courante : mêmes filtres que `GET /api/dossiers`, sans pagination, périmètre commercial appliqué. Une feuille « DONNEES » ; en-têtes = libellés du catalogue de colonnes, dans l'ordre de `colonnes` ; dates, montants et nombres en cellules typées. Auteur du classeur = utilisateur connecté, titre « Dossiers de vente », fichier `dossiers_<aaaaMMjj_HHmmss>.xlsx`. Plafond : 20 000 lignes.

### param format

Seule valeur acceptée : `xlsx`.

### param colonnes

Colonnes exportées, dans l'ordre voulu (celui de la grille). Absent : les colonnes par défaut de la liste.

### reponse 400

`format` autre que `xlsx`, ou colonne inconnue.

## statuts

Statuts des dossiers de vente (`StrStatus` du workflow des dossiers) que l'appelant peut voir, triés comme dans OMRAN. `sortie = true` pour les annulations et résiliations, exclues de la liste sans `inclureSortis`. L'option « Libre » du filtre n'est pas un statut : elle se traduit par `lignes`.

## champsDate

Les 46 dates du filtre de période d'OMRAN (`GetDatesFilterWithoutCancelDate`), dans son ordre, plus la date d'annulation. `code` se passe dans `champDate`.
