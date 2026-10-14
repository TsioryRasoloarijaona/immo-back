# ProspectionExportController

Exports Excel des listes de prospections et de pré-prospections. Chaque export relit la liste avec ses propres filtres et le périmètre commercial de l'appelant : il ne rend jamais une ligne que la liste ne montrerait pas.

## exporterProspections

Classeur Excel (`.xlsx`) de la liste courante : mêmes filtres que `GET /api/prospections` (filtres avancés compris), sans pagination, périmètre commercial appliqué. Une feuille « DONNEES » ; en-têtes = libellés du catalogue de colonnes, dans l'ordre de `colonnes` ; dates, montants, nombres et booléens en cellules typées. Auteur du classeur = utilisateur connecté, titre « Prospections », fichier `prospections_<aaaaMMjj_HHmmss>.xlsx`. Plafond : 20 000 lignes. Les filtres de colonne de la grille (appliqués côté navigateur à la page chargée) ne sont pas transmis : seuls les filtres serveur comptent.

### param format

Seule valeur acceptée : `xlsx`.

### param colonnes

Colonnes exportées, dans l'ordre voulu (celui de la grille). Absent : les colonnes par défaut de la liste.

### reponse 400

`format` autre que `xlsx`, ou colonne inconnue.

## exporterPreProspections

Classeur Excel (`.xlsx`) de la liste courante : mêmes filtres que `GET /api/pre-prospections`, sans pagination, périmètre commercial appliqué. Comme la liste, seules les colonnes par défaut portent une valeur (la vue d'activité exclut les pré-prospections) ; une autre colonne demandée sort vide. Titre « Pré-prospections », fichier `pre-prospections_<aaaaMMjj_HHmmss>.xlsx`. Plafond : 20 000 lignes. Les filtres de colonne de la grille ne sont pas transmis.

### param format

Seule valeur acceptée : `xlsx`.

### param colonnes

Colonnes exportées, dans l'ordre voulu (celui de la grille). Absent : les colonnes par défaut de la liste.

### reponse 400

`format` autre que `xlsx`, ou colonne inconnue.
