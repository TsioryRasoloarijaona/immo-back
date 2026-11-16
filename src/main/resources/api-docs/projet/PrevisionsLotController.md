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
