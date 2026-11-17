# PrevisionsLotController

## saisiePrevisions

Fenêtre OMRAN « Prévisions » d'un lot (`ProjectPrevisionMarket`, `FEATURE-lot-previsions.md` §4.2). Ordre des contrôles : lot
invisible **404** (même `detail` que `GET /api/projet/lots/{id}`), puis droit `Yes.Prj.Front.Tdb.ProjectPrevision.toolNew`
absent **403**. `estimationDeBaseModifiable` = droit `Yes.Prj.Front.Market.MarketMonitoring.ManualEstimationAdministrator` en
plus. `fin` = `debut` + `duree` jours, `finBase` = `debutBase` + `dureeBase`. `avancement`, `temps`, `paiement` des points
sont des fractions 0–1 ; `montant` = `paiement` × `prixPrevisions` (points) ou × `estimationBaseTTC` (points de base). Listes
vides si aucun point n'est stocké : la courbe générée par défaut n'est pas renvoyée.

## enregistrerPrevisions

Remplace les prévisions du lot (`FEATURE-lot-previsions.md` §4.3, R1-R8). Contrôles dans l'ordre : **404** lot invisible ;
**403** sans `ProjectPrevision.toolNew`, ou si `debutBase`, `dureeBase`, `estimationBaseHT` ou `pointsBase` diffèrent de l'état
stocké sans `ManualEstimationAdministrator` (valeurs égales acceptées) ; **400** date ou durée absente, durée négative, liste
non vide de moins de 2 points, point hors des bornes de sa liste, avancement hors [0 ; 1] ou décroissant avec la date (message
OMRAN dans `detail`). Écrit `StartDate`, `Delay`, `EndDate`, `StartDateManual`, `DelayManual`, `EndDateManual` et l'estimation
de base (TVA = HT × `DefaultVat`, 0 si absent) ; les points `PrjMarketPrevision` / `PrjMarketUserPrevision` sont supprimés puis
réinsérés ; une liste vide efface les points (retour à la courbe générée). Les prévisions de paiement
(`PrjMarketPaymentPrevision`) du lot, et celles du lot gestionnaire du compte prorata du projet, sont recalculées dans la même
transaction : un échec annule tout (**500**). Une ligne d'historique (`AdmWebHistorics`) par enregistrement. Renvoie le graphe
relu, comme `GET /api/projet/lots/{id}/previsions`.

## ecartsAlignement

Valeurs de la modale OMRAN « Alinger les prévisions au réel » (`FEATURE-lot-previsions-reel.md` §4 A2-A3), calculées sur
l'**état enregistré** du lot : le front fait enregistrer la grille avant d'ouvrir la modale. Ordre : **404** lot invisible,
**403** sans `ProjectPrevision.toolNew`, **409** lot sans date début. `ecartJours` = date de référence − dernier réel, où la date de référence est
interpolée entre les points « Prévisions » qui encadrent l'avancement réel du lot (début du lot sans réel) ; `finSysteme` =
dernier réel (ou `finActuelle`) − `ecartJours`. `ecartAvancement` = avancement réel − avancement prévu à la date du dernier
réel. `ecartMontant` = Σ des pièces à payer « Facture » (hors avances, RG et avoirs) − Σ des nets à payer des décomptes
facturés, ramené à 0 sous 0,1. Couleur à l'écran : vert si ≥ 0, rouge sinon.

## proposerAlignement

Proposition de la grille « Prévisions » alignée sur le réel (OMRAN `Market.cs:AlignPrevisionToReal`, §4 A4-A6). **N'écrit
rien** : le front remplace toute la grille puis enregistre par `PUT /api/projet/lots/{id}/previsions`. Ordre : **404**,
**403** (droit du PUT), **400** `mode` inconnu ou `FIXEE` sans `dateFin`, **409** lot multi-fournisseur, lot sans date
début, ou `FIXEE` avant la date du dernier décompte / facture (message OMRAN dans `detail`). Points = les décomptes réels
(date, avancement réel) puis les points prévus d'avancement supérieur à l'avancement réel, redatés vers la date fin choisie
(`INCHANGEE` : date fin actuelle), le plus avancé de chaque mois. `fin` et `duree` (fin − début) seulement si le mode n'est
pas `INCHANGEE`.

## proposerApplicationModele

Proposition de la grille « Prévisions » tirée d'un modèle de simulation (§4 M5). **N'écrit rien**. Ordre : **404** lot,
**404** modèle inconnu, **403** (droit du PUT), **409** lot sans date début. Un point par ligne du modèle, triée par temps :
date = début + durée × temps (jours tronqués ; durée 0 = tous les points au début), avancement = avancement du modèle. Le
paiement du modèle est ignoré (recalculé à l'enregistrement). Le front demande « remplacer » ou « annuler » si la grille
n'est pas vide.

## modelePropose

Fiche de modèle de simulation pré-remplie pour « Exporter vers modèle de simulation » (§4 M6). **N'écrit rien** :
l'enregistrement passe par `POST /api/projet/modeles-simulation`. Lignes = points **de base** (`PrjMarketUserPrevision`)
triés par date, temps / avancement / paiement recopiés ; liste vide sans point de base. Ordre : **404**, **403** (droit du
PUT).
