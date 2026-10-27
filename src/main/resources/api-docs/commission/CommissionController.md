# CommissionController

Commissions des vendeurs — l'écran « Commissions » d'OMRAN Estate (`F_0249`), par tranche. Une ligne de commission (`ComCommissionLine`) se calcule dossier par dossier depuis les règles de l'onglet « Commissions vendeur » de la tranche (`ComSalesCommission`, jamais modifiées ici). Voir `instructions/FEATURE-vente-commissions.md`.

**Droits** : lus dans `AdmRight` à chaque appel (rôles de `AdmUserRole`, jamais le jeton). Lire exige `Yes.Com.Front.Commission.CommissionList` ; générer, modifier un montant et générer un document à payer exigent en plus `…CommissionList.toolNew` ; supprimer `…toolDelete` ; exporter `…toolPrint`. Un droit absent = 403. **Visibilité** : tranche d'une entité inactive ou hors périmètre commercial, et tout dossier ou ligne de cette tranche → 404.

## tranches

Tranches en vente visibles de l'appelant. `recherche` : chaque mot (espaces ignorés) doit apparaître dans le code ou le libellé. Soldes (`montantGenere`, `montantRegle`, `montantReste`) : mêmes calculs que l'en-tête, les lignes et le rapport n° 34. Ventes (`vendus`, `restants`, `caStock`, `caRealise`, `caReste`) : calcul de valorisation des rapports de vente (articles VENTE, dossiers ni annulés ni désistés), pas la vue OMRAN `StkTrancheValorisation`.

## dossiersDeLaTranche

Tous les dossiers de vente de la tranche, sans statut compris. `type` garde les dossiers qui ont la date de ce type (concrétisation = `AdvanceDate`, finalisation 1 / 2 = colonne date du statut configuré dans la tranche, désistement = `CancelDate`) ; `du` / `au` bornent cette date (jours inclus). Sans `type`, `du` / `au` portent sur la date de concrétisation. `commercialId` : commercial, gestionnaire ou gestionnaire 2.

## lignes

Lignes de la tranche ; `type` filtre le type stocké (variantes « (support) » non filtrables), `du` / `au` la date de la ligne, `commercialId` la personne, `dossier` le N° de dossier. `regle` d'une ligne documentée = réglé du document (règlements annulés exclus) × montant de la ligne / Σ des lignes du document.

## genererLignes

Calcule et écrit une ligne par dossier et par personne. Personne : commercial (concrétisation), gestionnaire (finalisation 1), gestionnaire 2 (finalisation 2). Règle : celle de la tranche, sans employé de support, dont la fenêtre `StartDate` / `EndDate` contient la date du fait (bornes incluses), la plus spécifique (commercial, action commerciale de la prospection, catégorie de l'article) ; à égalité, fenêtre à deux bornes, puis début seul, fin seule, aucune, puis la plus ancienne. Montant : fixe, ou pourcentage (ratio) × prix de vente TTC sans arrondi ; colonnes « Extern » pour un prestataire. Désistement : une reprise négative par ligne d'origine non encore reprise, datée de l'annulation. Un dossier sans personne, sans date, sans règle, à montant nul, déjà généré ou déjà désisté est **ignoré** (listé dans `ignores`), jamais doublé.

### reponse 400

Type inconnu, plus de 500 dossiers, dossier d'une autre tranche, ou finalisation sans statut configuré dans la tranche (ou statut sans date) : rien n'est écrit, message OMRAN.

## modifierMontant

Le montant garde le signe du type : positif, négatif pour un désistement ; zéro refusé (400). Une ligne avec un document à payer est figée (409).

## supprimerLignes

Tout ou rien : une seule ligne avec un document à payer (409) ou hors périmètre (404) et rien n'est supprimé.

## genererDocument

Un document `BuyDocument` (`INVOICE`, `SALE_COMMISSION`, entité `ComInvoiceDebitCommission`, module COM, code de la séquence « Facture fournisseur ») pour une seule personne : une ligne de document par ligne de commission, ventilation par projet de facturation (`CfgTranche.PrjProjectId`, « Projet de facturation des dépenses » de la tranche) au prorata des montants, historique de statut, puis `BuyDocumentId` posé sur chaque ligne. Tout est contrôlé avant la première écriture, dans une seule transaction.

### reponse 400

Aucune ligne, une tranche sans projet de facturation, ou somme nulle.

### reponse 409

Lignes de plusieurs personnes, ou ligne déjà affectée à un document.

## documentDeLigne

### reponse 404

Ligne inconnue ou hors périmètre, ou « Cette ligne n'a pas encore un document à payer! ».
