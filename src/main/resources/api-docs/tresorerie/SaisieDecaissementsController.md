# SaisieDecaissementsController

## creer

Crée un règlement débit (`IsCredit = 0`) du module PRJ ou BUY et ses affectations aux pièces d'achat
(`StlSettlementLine.BuyDocumentId`). Exige la lecture de l'écran et `toolNew`. Ordre des refus : 404 (société, fournisseur,
compte ou pièce inconnus ou invisibles) → 403 → 400 → 409. Montant brut du règlement (`AmountBase`) > 0 ; retenues à la
source (`retenues`, lignes `StlDeduction`) : `AmountRS` = Σ des lignes, net `Amount = AmountBase − AmountRS`, `RsRatio =
AmountRS / AmountBase` (0 si brut nul) ; pas de timbre. `retenues` absent et `ManageRS = 1` : une ligne automatique du
fournisseur (`CfgTier.RsRatio ≠ 0`) ; `[]` = aucune retenue. `retenues` non vide avec `ManageRS = 0` : 409 « La retenue à
la source n'est pas activée. » ; ligne à montant retenu 0 : 400 « Vous devez corriger le montant ou supprimer la ligne ! ».
Les codes P.P / C.R.A.S du corps sont ignorés (attribués par leurs POST). Une ligne d'historique `StlSettlementHistory`
est écrite. Refus 400, messages OMRAN : aucune ligne
sans `CanSaveWithoutAffectation` (R-a), Σ lignes > montant (R-b), ligne à 0 (R-c), devise de pièce différente (R-d), date
de pointage sans compte (R-e), échéance < date pièce (R-f), pointage sans statut POINTED (R-g), POINTED sans pointage
(R-h), référence / notaire / compte obligatoires selon le type (R-k), statut CANCELED, pièce d'un autre fournisseur, pièce
répétée, compte d'une autre société, ligne > reste à payer de la pièce. 409 : référence déjà utilisée pour un type à
référence unique (même type, sens et compte, R-l) ; pointage à une date ≤ dernier rapprochement du compte (R-i, message
« dernier » corrigé de l'OMRAN « dérnier »).

## modifier

Mêmes règles que la création. Le module est ignoré. Les lignes visibles sont remplacées par `lignes` ; les lignes
masquées (pièce d'un projet ou d'une société non habilités) sont gardées et comptent dans le reste à affecter. Changer
`societeId` exige `CanModifyCompany` (403). `IsAccounted` repasse à 0 si le reste à affecter, la date de pointage, le type
ou le compte changent. Une ligne d'historique s'ajoute si l'échéance, le type ou la référence changent. 409 si le
règlement appartient à un virement interne (`StlInternalTransferId`). Le contrôle R-i ne joue que si la date de pointage
change.

## supprimer

Exige la lecture et `toolDelete`. Supprime le règlement et son historique. 409 : règlement pointé (date de pointage
renseignée, R-m), avec affectations (R-n), référencé par une écriture comptable, un bordereau, une feuille de
transmission, une pièce jointe ou une déduction (R-o), ou jambe d'un virement interne.

## referentiels

Types de règlement (drapeaux de contrôle), sociétés et comptes habilités, devise par défaut, statuts proposés
(`IN_WORK`, `POINTED`, `NOTPAYED`) et droits du compte connecté (`droits`), pour afficher ou masquer Nouveau, Supprimer et
le choix de société.

## piecesAAffecter

Pièces d'achat (`BuyDocument`) du module, du fournisseur et de la société, visibles par le compte connecté, dont le
reste à payer est > 0 et dont le statut est validé pour paiement (`BuyDocumentStatus.IsOkForPayement = 1`, comme le
sélecteur OMRAN). `regle` exclut les règlements annulés et, avec `reglementId`, les lignes de ce règlement (cas de la
modification).

## proposition

« Générer à partir de » d'OMRAN : rend un décaissement **prérempli, non enregistré**, à partir d'une pièce d'achat (facture
ou pièce à payer). Module, société, fournisseur (= payeur) et devise sont ceux de la pièce ; une seule ligne
`{pieceId, montant = reste à payer}` et `montant` = ce reste ; date pièce = date d'échéance = aujourd'hui ; statut `IN_WORK` ;
type, compte, référence et pointage vides. Une pièce soldée n'est pas refusée : la ligne vaut le reste (0 ou négatif) et
l'enregistrement la refusera. L'utilisateur complète la fiche puis l'enregistre par `POST /decaissements`.

## copie

« Copier » d'OMRAN : rend la saisie d'un règlement visible, **non enregistrée**, avec tous ses champs (statut et date de
pointage compris) sauf la référence, le code reçu et les codes P.P / C.R.A.S, vidés, et **sans aucune ligne
d'affectation**. Permise sur un règlement de virement interne. L'utilisateur l'enregistre par `POST /decaissements`.

## codeFeuillePaiement

Bouton « P.P. » d'OMRAN. N'enregistre pas la fiche (le front l'enregistre d'abord). Si `PropositionSheetCode` est vide,
l'attribue depuis la séquence globale de la proposition de paiement (`AdmSequence`, format `aa/00000`, ex. `26/00001`) ;
un code existant n'est jamais réécrit et est rendu tel quel. Exige la lecture de l'écran et `toolNew` ; ordre 404 → 403.
**Écart à OMRAN** : séquence absente → 409 « Aucune séquence de code n'est configurée. » (OMRAN laisse le code vide).

## codeFeuilleRs

Bouton « C.R.A.S. » d'OMRAN. Même fonctionnement que le code P.P, avec la séquence `RSSheetCode` **de la société du
règlement** (format `aa` + 3 chiffres, ex. `26001`). **Écart à OMRAN** : société sans séquence → 409 « Aucune séquence de
code n'est configurée pour cette société. » (OMRAN rend un code NULL, sans erreur).
