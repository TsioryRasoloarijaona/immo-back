# LotsProjetController

Module Projet (chantier OMRAN), en **lecture seule** : lots (`PrjMarket`, « marché » dans la base), bordereau, décomptes
(`PrjWorkingOut`) et leurs factures. Voir `FEATURE-projet-lots-decomptes.md`.

**Visibilité, comme OMRAN** : l'appelant voit les projets de ses lignes `AdmUserHabilitation.PrjProjectId`, **quel que soit
son rôle** (aucune exemption Administrateur ni Direction Générale). Sans ligne d'habilitation, ou appelant non identifiable :
listes vides. **Lots** : il faut en plus, comme OMRAN (`StrStatus.GetStatusFilterQueryClauseForListing`), que le statut
du lot soit lié (`StrStatusIntervener`) à un intervenant dont le rôle est un rôle de l'appelant ou qui est l'appelant ; seule
exemption : un rôle de l'appelant à `AdmRole.WithSequenceFreeRestriction = 0` (aucune exemption par le code de rôle). Lot
sans statut ou statut sans intervenant : invisible. Un lot sans projet, hors périmètre ou au statut invisible répond **404**
comme un identifiant inconnu (jamais 403), fiche et sous-routes. Les décomptes (`/api/projet/decomptes`) ne suivent que
le projet. Voir `FEATURE-visibilite-omran.md` R1, R2, R7.

## referentiels

Listes de choix des écrans Lots et Décomptes. `projets` (projets habilités dont le statut est visible, même clause que les
lots), `nbLots`, `categories` et `fournisseurs` ne contiennent que ce que l'appelant voit ; `typesLot`, `statutsLot` et
`statutsFacture` sont des référentiels complets.

## lots

Liste **globale** des lots des projets visibles. `projetIds` est **intersecté** avec le périmètre : un projet demandé hors
périmètre est ignoré sans erreur, et une intersection vide donne une page vide (200). Tri par défaut : `code` décroissant ;
clés de tri acceptées : `code`, `description`.

Montants (calculés, jamais stockés) : marché = Σ du bordereau définitif (`Quantity × UnitPriceHT`, TVA par `VatRatio`,
`Quantity × UnitPriceTTC`), ou, si le lot n'a aucune ligne, le montant forfaitaire saisi (`EstimatedPrice*Manual`) ;
`montantsDepuisBordereau` dit quel chemin a servi. Cumuls = sommes des colonnes stockées des décomptes du lot.
`avancementPct` = cumul décomptes TTC / marché TTC × 100 (les travaux supplémentaires n'y entrent pas), `null` si le marché
est nul. Facturé = Σ `BuyDocument.AmountToPay` des factures des décomptes (`PrjWorkingOut.BuyDocumentId`) ; réglé = Σ des
lignes de règlement non annulées de ces factures ; reste = facturé − réglé.

## lot

Fiche d'un lot : les champs de la liste, plus le montant forfaitaire saisi, l'estimation (Σ bordereau estimatif, sinon le
forfaitaire), la retenue de garantie et l'avance théoriques (marché TTC × taux), les taux en pourcentage (0–100), les
options et les dates d'appel d'offres et de chantier (`null` si non saisies).

## bordereau

Lignes du bordereau **définitif** (`PrjMarketLine` hors stock), tri par ordre, avec leurs totaux calculés. Liste vide : lot
au montant forfaitaire (« Bordereau non saisi »).

## decomptesDuLot

Tous les décomptes du lot (pas de pagination), tri par numéro (`Sorting`) puis date. Mêmes champs que
`GET /api/projet/decomptes`, dont le contrôle d'écart du net à payer.
