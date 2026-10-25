# SuiviPrevisionsController

Écran « Suivi des prévisions de vente » (Vente › Tableaux de bord, OMRAN F_0250). Ouvert à tout utilisateur connecté, sans
droit par rôle ; les données sont bornées aux tranches visibles de l'appelant (société active, périmètre commercial, tranche
en vente). Aucune clé d'API n'a accès à ces routes (403 `HORS_PORTEE`). Voir `instructions/FEATURE-tdb-vente-previsions.md`.

**Paramètres de requête** (ceux de `/parametres`) : `annee` (défaut : année courante, [1900 ; 2200]), `trancheId` répété
(absent = toutes les tranches visibles ; un `trancheId` hors de ces tranches est ignoré, et si aucun n'est visible le
résultat est à zéro, jamais « toutes les tranches »), `frais` : `SANS` (défaut), `AVEC` ou `SEULS`.

**Prévu** : aucune prévision n'est saisie aujourd'hui ; les colonnes `base`, `ancienPrevu`, `prevu` valent 0 et
l'avertissement « Prévu non saisi… » est renvoyé. Un pourcentage dont le dénominateur vaut 0 est `null` (affiché « — »).

## parametres

`annee` (ANNEE), `trancheId` (TRANCHES, multiple, options = tranches visibles triées par libellé), `frais` (CHOIX : `SANS`
« Sans frais », `AVEC` « Avec frais », `SEULS` « Seulement les frais »). `mois` n'y figure pas : il est propre à `/detail`.

## resultat

Code `SuiviPrevisionsVente`, module `vente`, deux sections de douze lignes (une par mois, `mois` = « Janvier »…) :
`quantites` (concrétisations nettes : dossiers ni annulés ni désistés, au mois de concrétisation) et `montants`
(encaissements selon `frais`, à la date du mouvement ; colonne `schemaFinancement` = échéances dues des dossiers
concrétisés vivants, au mois de l'échéance). Colonnes communes : `base`, `ancienPrevu`, `prevu`, `reel` (totalisées),
`cumulPrevu`, `cumulReel`, `pourcentageRealise` (= reel ÷ prevu × 100), `pourcentageCumule` (= cumulReel ÷ cumulPrevu ×
100), `rentabilite` (= reel ÷ réel de l'année × 100). Cinq graphiques par section, catégorie `mois`.

### reponse 400

Paramètre inconnu, année hors [1900 ; 2200], `frais` hors options, identifiant non UUID, valeur répétée sur un paramètre
non multiple.

## detail

Code `SuiviPrevisionsVenteMois`, mêmes paramètres plus `mois` (1 à 12, obligatoire). Trois sections, triées par date
décroissante, faites des lignes sources exactes des cellules du mois : `dossiers` (projet, tranche, article, dossier,
dateConcretisation, prixVente, regle, client ; leur nombre = `quantites.reel` du mois), `mouvements` (projet, tranche,
article, dateDossier, dateMouvement, categorie, montant, libelle, client ; Σ montant = `montants.reel`), `echeances`
(client, date, categorie, montant, regle, reste ; Σ montant = `schemaFinancement`). `regle` d'une échéance = ses lignes
de règlement comptées en encaissement, toutes dates ; `reste` = montant − regle.

### reponse 400

`mois` absent ou hors [1 ; 12], ou un refus de `/suivi-previsions`.

## exporter

Classeur `SuiviPrevisionsVente_<aaaaMMjj_HHmmss>.xlsx`, une feuille par section de `/suivi-previsions` (nom = titre sans
`[]:*?/\`, 31 caractères au plus), ligne « Total » en gras. Mêmes paramètres que `/suivi-previsions`.

### param format

`xlsx` (défaut), seul format accepté.

### reponse 400

`format` autre que `xlsx`, ou un paramètre refusé comme sur `/suivi-previsions`.
