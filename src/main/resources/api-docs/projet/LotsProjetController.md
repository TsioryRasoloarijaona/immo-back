# LotsProjetController

Module Projet (chantier OMRAN) : lots (`PrjMarket`, « marché » dans la base), bordereau, décomptes (`PrjWorkingOut`) et
leurs factures. Voir `FEATURE-projet-lots-decomptes.md`. Création, suppression et transitions de statut d'un lot, totaux et
export de la liste : `FEATURE-lots-liste-parite.md`.

**Droits OMRAN** (`AdmRight.Code`, lus en base à chaque appel ; l'Administrateur les a tous), préfixe
`Yes.Prj.Front.Market.MarketMonitoringList.` : `toolNew` (créer un lot Entrepreneurs), `toolNewMultiSupplier` (créer un lot
des quatre autres genres), `toolDelete` (supprimer), `toolExport` (exporter). Sans le droit : **403**, contrôlé avant le
corps et avant l'existence du lot.

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
`statutsFacture` sont des référentiels complets. `entites` = entités juridiques (`CfgCompany`) des projets de `projets` ;
chaque projet porte son `entiteId`. `statutsLot` : les statuts des cinq genres, avec leur `couleur` (nom .NET brut de
`StrStatus.ColorName`) et leur `genre`.

## lots

Liste **globale** des lots des projets visibles. `projetIds` est **intersecté** avec le périmètre : un projet demandé hors
périmètre est ignoré sans erreur, et une intersection vide donne une page vide (200). `statutIds` (répétable) : lots à l'un
de ces statuts, aucun = tous ; `entiteId` : lots des projets de cette entité juridique. Tri par défaut : `code` décroissant ;
clés de tri acceptées : `code`, `description`.

Coût prévisionnel (repris de la vue OMRAN `PrjMarketMonitoringView`, lot non terminé) : `coutPrevisionnelHT` / `TTC` =
commandes ou bordereau + dépassements + travaux supplémentaires (+ fluctuations), ou l'estimation si rien n'est offert ;
`coutPrevisionnelNetAPayer` = TTC + régularisations − retenues non remboursables ; `coutPrevisionnelRegle` = Σ des
règlements des pièces d'achat du lot (`BuyDocument.PrjMarketId`), **règlements annulés compris** comme la vue ;
`coutPrevisionnelReste` = net à payer − réglé. `avancement` = fraction (0–1) réalisée du marché HT, comme la vue.

Montants (calculés, jamais stockés) : marché = Σ du bordereau définitif (`Quantity × UnitPriceHT`, TVA par `VatRatio`,
`Quantity × UnitPriceTTC`), ou, si le lot n'a aucune ligne, le montant forfaitaire saisi (`EstimatedPrice*Manual`) ;
`montantsDepuisBordereau` dit quel chemin a servi. Cumuls = sommes des colonnes stockées des décomptes du lot.
`avancementPct` = cumul décomptes TTC / marché TTC × 100 (les travaux supplémentaires n'y entrent pas), `null` si le marché
est nul. Facturé = Σ `BuyDocument.AmountToPay` des factures des décomptes (`PrjWorkingOut.BuyDocumentId`) ; réglé = Σ des
lignes de règlement non annulées de ces factures ; reste = facturé − réglé.

## totaux

Σ des colonnes de coût prévisionnel de **toutes** les lignes que la liste rendrait avec les mêmes filtres (toutes pages), et
`nombreLots` = son `totalElements`. Même calcul que les lignes : la somme des lignes affichées égale ces totaux.

## exporter

Classeur Excel : colonnes de la liste dans le même ordre, toutes les lignes des filtres donnés (sans pagination), triées
comme la liste, et une ligne « Total » en bas égale à `GET /api/projet/lots/totaux`. Droit `toolExport` exigé.

## creer

Crée un lot d'un des cinq `genre` (`ENTREPRENEURS`, `FOURNISSEURS`, `ADMINISTRATION`, `MAITRES_OEUVRE`, `CONCESSIONNAIRES`).
Code = séquence OMRAN « Market » (`yy/nnnnn`), incrémentée jusqu'à un code libre. Statut de départ = statut de type
« Nouveau » du genre de plus petit ordre. Entrepreneurs : non multi-fournisseur ; autres genres : multi-fournisseur et hors
prévisions de paiement. Écrit une ligne d'historique « Création ». Ordre des refus : 403, puis 400, puis 404.
Une `categorieId`, `typeId`, `fournisseurId` ou `deviseId` inconnue répond 400.

## supprimer

Supprime un lot visible qui n'est cité nulle part : son bordereau et ses autres enfants partent avec lui. Un lot cité par un
décompte, une pièce d'achat ou toute autre référence répond **409** et reste en place. Ordre des refus : 403, 404, 409.

## transitions

Transitions OMRAN (`StrWorkFlow`) partant du statut actuel du lot. Si aucun rôle de l'appelant n'est à
`AdmRole.WithSequenceFreeRestriction = 0`, seules celles dont un intervenant (`StrWorkFlowIntervener`) est son rôle ou lui.
Les transitions à second valideur (`WithPassword`) ne sont jamais proposées.
C'est exactement l'ensemble accepté par `POST …/transition`.

## executerTransition

Passe le lot au statut d'arrivée de la transition et écrit l'historique (`StrHistory`, `notes` = commentaire). **409** si la
transition n'est pas dans `GET …/transitions` pour l'appelant ; 400 si elle exige un
commentaire et que `notes` est vide.

## lot

Fiche d'un lot : les champs de la liste, plus le montant forfaitaire saisi, l'estimation (Σ bordereau estimatif, sinon le
forfaitaire), la retenue de garantie et l'avance théoriques (marché TTC × taux), les taux en pourcentage (0–100), les
options et les dates d'appel d'offres et de chantier (`null` si non saisies). Champs des onglets de la fiche OMRAN
(`FEATURE-lot-fiche-parite.md` E1) : devise, catégorie, « Est. de base » (`dateDebutBase`, `delaiBaseJours`,
`dateFinBase`), les 12 dates, les options (ratios en pourcentage 0–100), la résiliation et la description détaillée.
L'avancement décompté de l'en-tête est `avancement` (fraction 0–1) de la ligne de liste.

## modifier

Enregistre la fiche (`FEATURE-lot-fiche-parite.md` E6). Le corps ne contient que les champs à changer : **absent =
inchangé**, `null` explicite = vidé (400 pour `description`, `categorieId`, `typeId`). Taux et ratios en pourcentage 0–100
(10 → 0,1 en base). Tout autre champ (code, genre, statut, projet, multi-fournisseur, dates prévisionnelles, montants
calculés) répond **400** et rien n'est écrit. Ordre des refus : **404** (lot invisible) → **403** (estimation manuelle
changée sans le droit `Yes.Prj.Front.Market.MarketMonitoring.ManualEstimationAdministrator` : `montantForfaitaireHT/TTC`,
`dateDebutBase`, `delaiBaseJours`, `dateFinBase` ; renvoyer la valeur stockée n'est pas un changement) → **409** (statut
du lot à `StrStatus.IsReadOnly = 1`) → **400** (contrôles). Chaîne des 12 dates, dans l'ordre lancement AO, retrait du
dossier, échéance de remise des offres, report AO, ouverture des plis, rapport de dépouillement, adjudication, signature du
marché, ordre de service, lancement des travaux, achèvement des travaux, recollement : chaque date renseignée (après
application du corps) doit être ≥ chaque date renseignée qui la précède, sinon 400 nommant les deux. Estimation
confidentielle comme OMRAN : `montantForfaitaireHT` recalcule TVA et TTC au taux `DefaultVat`, `montantForfaitaireTTC`
recalcule HT et TVA, `tvaDefautPct` changé recalcule TVA et TTC depuis le HT stocké ; une valeur renvoyée identique ne
recalcule rien. Écrit `UserUpdate` / `UserUpdateDateTime` et une ligne d'audit ; renvoie la fiche.

## copier

Copie un lot visible : nouveau code de la séquence « Market » (incrémentée jusqu'à un code libre), description suivie de
« (Copie) », lot ouvert, statut de départ du genre (statut « Nouveau » de plus petit ordre), ligne d'historique
« Création » ; les lignes du bordereau définitif et leurs groupes sont copiés et rattachés à la copie. Prévisions et lignes
de retenue de garantie ne sont pas copiées. Droit `toolNew` (Entrepreneurs) ou `toolNewMultiSupplier` (autres genres) ;
ordre des refus : 403 si l'appelant n'a aucun des deux, 404, puis 403 si le droit du genre du lot manque.

## demandesAchat

Demandes internes d'achat (`BuySelection`) rattachées au lot, en lecture seule, par date décroissante puis code. Onglet
Réclamations : module SAV non porté, aucune route.

## bordereau

Lignes du bordereau **définitif** (`PrjMarketLine` hors stock), tri par ordre, avec leurs totaux calculés. Liste vide : lot
au montant forfaitaire (« Bordereau non saisi »).

## decomptesDuLot

Tous les décomptes du lot (pas de pagination), tri par numéro (`Sorting`) puis date. Mêmes champs que
`GET /api/projet/decomptes`, dont le contrôle d'écart du net à payer.
