# DesistementController

Désistements des dossiers de vente — l'écran « Désistements » d'OMRAN Estate (`ComSaleWithdrawalCancellation`). Un désistement est un **brouillon** tant que sa date de validation est vide, **validé** ensuite, **légalisé** quand sa date de légalisation est posée ; il n'a pas de colonne statut. Le lien au dossier est `ComSaleId`. Voir `instructions/FEATURE-desistements.md`.

**Calcul (R3-R5)** : `penaliteApresRemise` = taux × base × (1 − remise), au centime ; taux = `CfgTranche.CancellationPenalityPercent` ; base = prix de vente (`ComFolder.Price`) si la tranche a `CancellationPenalityBasedOnItemPrice`, sinon le total réglé ; remise = `CancellationPenalityPercentDiscount` de la tranche pour un ancien client, sinon 0. Total réglé = réglé signé (décaissements déduits, règlements annulés exclus) des échéances d'avance, de crédit et d'autofinancement. Reste frais à régler = Σ reste dû des échéances dont la catégorie est « toujours payée », au crédit et de frais, pénalité ou modification. À rembourser = max(0, total réglé − pénalité après remise − reste frais à régler).

**Droits** : toutes les écritures sont réservées à Administrateur, Direction Générale et Commercial Chef ; la lecture suit le périmètre commercial de `GET /api/dossiers` (un désistement est visible si son dossier l'est).

## lister

Mêmes règles de visibilité que `GET /api/dossiers` : un désistement n'est listé que si son dossier est visible pour l'appelant (projets affectés, entité juridique active). Montants : ceux enregistrés sur le désistement (recalculés à chaque modification du brouillon, figés à la validation). Tri fixe : date de demande décroissante ; seuls `page` et `size` de la pagination sont lus. `totaux` porte sur tout le filtre.

## detail

Mêmes montants que la liste, plus `resteARembourser` = reste dû de la pièce de remboursement générée à la validation (à rembourser − décaissements enregistrés), 0 avant validation.

## modifier

Brouillon : tous les champs. Saisir `penaliteApresRemise` recalcule la remise (1 − pénalité / (taux × base)) ; saisir `remise` recalcule la pénalité ; les montants du dossier sont relus. Validé non légalisé : seuls observations, motif, dates, type de pièce, référence, banque et contact changent — `remise` ou `penaliteApresRemise` donnent 409, les pièces étant déjà générées. Légalisé : 409.

## valider

Dans une transaction : contrôle des dates (400), recalcul des montants, puis passage du dossier en DESISTEMENT par le même chemin que `PATCH /api/dossiers/{id}/statut` (date d'annulation = jour, cause = « motif : observations », lot libéré, client repassé prospect s'il n'a plus de dossier actif), puis génération de la pièce « Frais de désistement » (catégorie du paramètre `DefaultCancellationDocumentType`) = pénalité si non nulle, et de la pièce « REMBOURSEMENT POUR DÉSISTEMENT » = à rembourser si positif, toutes deux rattachées au désistement. Une date de légalisation fournie rend le désistement légalisé.

### reponse 409

Déjà validé ; statut du dossier sans « Peut effectuer un désistement » ; transition vers DESISTEMENT non paramétrée ; dossier de syndic en cours ou facture sans avoir ; catégorie de frais ou de remboursement introuvable. Rien n'est écrit.

## rembourser

Décaissement (`StlSettlement.IsCredit = 0`, statut `IN_WORK`) écrit par le même chemin qu'un règlement de dossier, affecté en entier à la pièce de remboursement du désistement. Plafond : le reste à rembourser de cette pièce, cumul des décaissements précédents compris.

## creer

Statut du dossier sans « Peut effectuer un désistement » (`StrStatus.BoolField010`) : 409. Un seul désistement non légalisé par dossier : 409. Les montants sont calculés comme la simulation et enregistrés ; le N° de demande vient de la séquence « Désistement ».

## simuler

Montants calculés à l'instant depuis le dossier, avec la remise par défaut (ancien client), sans rien écrire : sert à préremplir la création.
