# PiecesLotController

## modele

Pré-remplissage de la fiche d'une pièce à payer du lot (FEATURE-lot-pieces-a-payer.md R1-R8). Mêmes contrôles que la création,
dans le même ordre : **404** lot invisible, **403** sans `Yes.Prj.Front.Market.MarketMonitoringList.toolAdvance` (sauf
`MARKET_PROPORTION`), **409** lot mono-fournisseur sans fournisseur (« Veuillez d'abord selectionnez un fournisseur »), puis statut
du lot sans le drapeau de la nature (`BoolField010` pour `MARKET_ADVANCE` et `MARKET_WARRANTY`, `BoolField008` pour
`WORKINGOUT_ADVANCE`) ou restitution de RG bloquée. `reference` = AVA / W.AVA / RRG + (nombre de pièces de cette nature du lot,
tout statut, + 1), RCP sans numéro. `montantTTC` = reste calculé, plancher 0 : avance sur marché = min(avance prévue − réglée,
avance prévue − validée) ; restitution de RG = RG des factures et avenants facturés − RG déjà validée (0 si bloquée) ; prorata =
prorata facturé du projet − déjà remboursé ; avance sur décompte = 0. `montantMaximum` = ce reste, null pour W.AVA et RCP.

## creer

Crée une pièce `DOCUMENT_TO_PAY` (`ReferenceType` = `nature`) par le chemin commun d'écriture des pièces d'achat : gestion OMRAN
de la nature, module PRJ, société du projet, lot, fournisseur du lot, devise du lot pour les avances (devise par défaut sinon),
statut « Nouveau » du workflow et une ligne d'historique, code par la séquence « Document à régler entrant ». Ordre des refus :
**404**, **403**, **400** (`date` absente, `montantTTC` ou `penaliteRg` négatif, `echeance` avant `date`), **409** (comme le
modèle). `montantTTC` absent = montant par défaut du modèle. Une ligne par catégorie de
ventilation du lot (TTC = montant × taux, TVA du lot, unité U, quantité 1), une seule ligne sans catégorie si le lot n'a pas de
ventilation. Restitution de RG : `AmountRegul` = − `penaliteRg`. Avance sur décompte : rattachée au dernier décompte du lot s'il
n'est ni final ni facturé, dont le reste d'avance sur décompte est recalculé. Les prévisions de paiement du lot sont recalculées.

## lire

Fiche d'une pièce à payer du lot ; **404** si la pièce n'est pas une des quatre natures de ce lot, payable, d'une société
habilitée. `modifiable` et `supprimable` = mêmes règles que le PUT et le DELETE.

## modifier

Modifie date, échéance, montant TTC (lignes recalculées comme à la création), notes et pénalité de RG ; `nature` ignorée.
**409** « Vous ne pouvez pas modifier cette pièce dans l'état actuel du lot ! » si le statut du lot n'a plus le drapeau de la
nature ou si le statut de la pièce est en lecture seule. Pas de droit propre. Reste d'avance du décompte rattaché et prévisions
recalculés.

## supprimer

Supprime la pièce, ses lignes, ventilations et historique de statut. **403** sans `toolAdvance` (sauf prorata) ; **409** statut
en lecture seule (« Vous n'avez pas le droit de supprimer un Documents à régler avec un statut <statut> ») ou règlements liés.
Reste d'avance du décompte rattaché et prévisions recalculés.

## supprimerAvoir

Supprime un avoir (`REFUND`) du lot. **404** si ce n'est pas un avoir payable de ce lot ; **403** sans
`Yes.Prj.Front.Market.RefundList.toolDelete` ; **409** statut en lecture seule (« Vous n'avez pas le droit de supprimer un avoir
avec un statut <statut> ») ou règlements liés. Prévisions de paiement du lot recalculées.
