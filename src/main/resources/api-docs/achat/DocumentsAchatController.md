# DocumentsAchatController

Pièces d'achat (`BuyDocument`) en **lecture seule** : factures, bons de commande, devis, documents à régler, bons de livraison, avoirs. Voir `instructions/FEATURE-achat-lecture-factures.md`.

**Visibilité, comme les listes OMRAN** : une pièce est visible si et seulement si sa société (`CfgCompanyId`) figure dans les habilitations société de l'appelant (`AdmUserHabilitation.CfgCompanyId`) **et** son projet (`PrjProjectId`) dans ses habilitations projet (`AdmUserHabilitation.PrjProjectId`) **ou** la pièce n'a pas de projet ; **pour tous les rôles** (aucune exemption Administrateur / Direction générale). Sans habilitation société : rien. Un filtre `projetIds` exclut les pièces sans projet. Une pièce hors périmètre répond 404, comme un id inconnu. Voir `instructions/FEATURE-visibilite-omran.md` R3.

**Solde** : `montantRegle` = somme des lignes de règlement (`StlSettlementLine.Amount`) de la pièce dont le règlement n'est pas annulé (`CANCELED`) ; `resteARegler` = `montantAPayer` (`AmountToPay`) − réglé, à 2 décimales ; `solde` = |reste| ≤ 0,05 ; `aDeduire` = à payer < −0,05 (récupération d'avance sur décompte, jamais « trop réglée ») ; `tropRegle` = reste < −0,05 sur une pièce qui n'est pas à déduire. « Payé » ne se lit jamais dans le statut. Calcul unique : `SoldeDocumentAchat` (FEATURE-achat-impayes.md §3.1).

## referentiels

`societes` = sociétés habilitées de l'appelant seulement (vide sans habilitation société) ; `modules` = BUY (Achat), PRJ (Projet), STL (Trésorerie) ; `typesLot` = tous les types de lot (`PrjMarketType`).

## liste

Pièces visibles, filtrées. `projetIds` est restreint au périmètre (un projet hors périmètre est ignoré, jamais d'erreur ; s'il ne reste rien, la page est vide). `societeIds` est de même croisé avec les sociétés habilitées : une société non habilitée est ignorée, et si aucune ne reste la page est vide (jamais « aucun filtre »). Une liste vide ou un paramètre vide (`societeIds=`) = pas de filtre. `nonSoldes=true` garde les pièces dont le reste dépasse 0,05 ; une pièce trop réglée ou à déduire n'y figure pas. `okPourPaiement=true` garde les pièces dont le statut est « OK pour paiement » ; avec `natures=INVOICE&natures=DOCUMENT_TO_PAY&nonSoldes=true`, c'est la liste « Validées, non réglées ». `modules` se combine par ET avec `moduleProjet`. `montantDe` / `montantA` sont inclus et portent sur `AmountToPay` (`montantDe > montantA` = page vide). `du` / `au` (inclus) portent sur la colonne de `champDate` (`DATE_PIECE` = `DocDate`, `ECHEANCE` = `MaturityDate`) ; `dateRenseignee=true` garde les pièces dont cette colonne est renseignée et ignore `du` / `au`. `search` cherche, sans tenir compte de la casse, dans le code, la référence, la référence bureau d'ordre, la description, les notes, le fournisseur, le projet (code, description), le lot (code, description) et le dossier de groupement.

**Bons de commande** (quelles que soient les natures demandées, Documents Achats compris) : comme OMRAN (`OrderView.cs:803`), seuls les BC dont le déclencheur (`CfgTierTriggerId`) est le personnel du compte (`AdmUser.CfgEmployeeId`) sont rendus, sauf droit OMRAN `Yes.Buy.Front.Order.OrderList.WithNoRestriction` (l'Administrateur l'a) ; un compte sans personnel n'est pas restreint. Les autres natures ne sont jamais restreintes. Même règle pour les totaux, l'export et la fiche.

**À clôturer** (`aCloturer`) : bon de commande dont le HT est inférieur ou égal à la Σ des HT de ses factures (`BuyOrderId`), et dont le statut n'est ni « Terminé avec succès » ni « Terminé avec échec » (une annulation alerte) ; faux sans statut et pour toute autre nature.

Tri par défaut : `dateDocument` décroissante puis `code` décroissant. Propriétés de tri acceptées : `code`, `nature`, `dateDocument`, `dateEcheance`, `reference`, `fournisseurNom`, `projetCode`, `lotCode`, `statutCode`, `montantTTC`, `montantAPayer`, `montantRegle`, `resteARegler`, `decompteCode`, `typeLotLibelle`, `moduleLibelle`, `societeNom`, `archive`, `comptabilise`, `referenceBureauOrdre`, `personneAContacter`, `dossierGroupement`, `aCloturer`, `deviseCode`, `categories`, `dateAlerte` ; une autre propriété répond 400.

**Agrégats des bons de commande** (`agregats`, objet `AgregatsBonCommandeDto`, `null` hors bon de commande) : calculés sur les pièces liées par `BuyOrderId` (voir le schéma). Clés de tri en plus, lues dans `agregats` : `factureHT`, `factureTTC`, `factureNetAPayer`, `livreHT`, `livreTTC`, `livreNetAPayer`, `resteALivrerHT`, `resteALivrerTTC`, `resteALivrerNetAPayer`, `avance`, `avanceTauxPrevu`, `avanceTauxEffectif`, `avanceAmortie`, `avanceRegle`, `avanceReste`, `avanceResteAAmortir`, `garantieRetenue`, `garantieRestituee`, `garantieReste`, `regle`, `resteAPayerSurFacture`, `resteAPayerTotal`, `bonsLivraisonCodes`, `facturesCodes`, `devisCode` ; les pièces qui ne sont pas des bons de commande restent en fin de liste dans les deux sens.

**Décompte lié** (`decompteId`, `decompteCode`, `decompteNumero`) : le décompte de chantier dont `PrjWorkingOut.BuyDocumentId` pointe la pièce (le premier par date puis code si plusieurs), `null` sinon. `BuyDocument.PrjWorkingOutId` (sens des avances) n'est jamais lu.

## totaux

Totaux de la liste pour les mêmes filtres (même requête, toutes pages ; `sort`, `page` et `size` ignorés) : `nombre` = `totalElements` de la liste, `resteARegler` = Σ à payer − Σ réglé (signé), `resteDu` = Σ des seuls restes supérieurs à 0,05 et `nombreARegler` leur nombre, `montantHT` / `montantTVA` = Σ HT / TVA des lignes. `agregats` = Σ champ à champ des agrégats des bons de commande du même jeu (ratios et codes `null`), `null` si le jeu ne contient aucun bon de commande. Aucune pièce visible : tout à zéro.

## soldes

Solde de chaque fournisseur, une ligne par (fournisseur, société), sur les factures et documents à régler (avoirs exclus) **visibles de l'appelant** (même règle que la liste : société habilitée et projet habilité ou sans projet, aucune exemption de rôle) : deux utilisateurs aux projets différents voient deux soldes différents pour le même fournisseur. Les règlements non affectés à une pièce ne sont pas comptés. À payer = `AmountToPay`, réglé = lignes de règlement non annulées, reste = différence (calcul `SoldeDocumentAchat`). `projetIds` hors périmètre est ignoré ; `search` compare le nom du fournisseur sans espaces ni casse ; `nonSoldes=true` masque les lignes soldées (|reste| ≤ 0,05). Non paginé, trié par nom de fournisseur puis code société ; `sort` est ignoré. `total` somme les lignes rendues.

## exporterSoldes

Mêmes filtres et mêmes lignes que `GET /api/achat/fournisseurs/soldes`, sans la ligne de total.

## fiche

Fiche d'une pièce visible : les champs de la ligne de liste (mêmes calculs, `agregats` compris pour un bon de commande), l'en-tête (notes, régularisation, timbre, entité, audit, déclencheur, date de livraison souhaitée, note 2, devis sélectionné, retenues et avances lues telles quelles), les lignes (`Sorting`) avec leurs colonnes calculées et le récapitulatif par taux de TVA (`recapTva`), la ventilation par catégorie et par projet (`ventilationProjets`, tous les projets, même non habilités), l'historique des statuts (date et heure croissantes, transition empruntée) et les règlements, annulés compris avec `annule = true` (exclus du réglé).

La fiche répond 200 exactement quand la pièce figure dans la liste du compte : un bon de commande d'un autre déclencheur (voir la liste) répond 404 « Pièce introuvable », comme une pièce hors périmètre.

**Pièces liées** (`bonsLivraison`, `factures` pour un bon de commande ; `avoirs` pour une facture ; listes vides pour les autres natures) : seules les pièces visibles du compte (même règle que la liste), par date puis code. Factures d'un bon de commande = `BuyOrderId` ∪ table `BuyOrderInvoice`, sans doublon. `devisOrigine*`, `bonCommande*`, `factureSource*` : `null` si la pièce désignée n'est pas visible. `avoirsMontant` = Σ TTC des avoirs visibles dont `InvoiceSourceId` est la pièce (0 sans avoir) : affiché, jamais soustrait (`montantAPayer` est déjà net des avoirs).
