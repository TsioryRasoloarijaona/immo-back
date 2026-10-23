# FinancementFaisabiliteController

Onglet « Financement » d'une étude (OMRAN « Déblocage de crédit » `FSFunding`, « Remboursement de crédit »
`FSReimbursement`). Lecture Administrateur + Direction Générale, écriture et génération Administrateur ; aucune clé
d'API (403 `HORS_PORTEE`). Étude inconnue ou de société inactive → 404 ; étude bloquée → 409 sur le PUT.

## lister

Crédits par date de déblocage, remboursements par date. `montantARegler` = Σ à régler des remboursements,
`totalInterets` = Σ intérêts, `ecartBase` = Σ bases − montant débloqué (OMRAN n'impose pas l'égalité : information
à afficher, jamais un refus).

## remplacer

Liste complète (patron des grilles) : `id` connu = mise à jour, sans `id` = création, absent = suppression ; même règle
pour les `remboursements` de chaque crédit. Texte absent → "", nombres absents → 0, `periodiciteMois` < 1 → 1.
Remboursement : `montantARegler` = base + intérêts, intérêts = base × taux ; si le corps porte taux et intérêts, le
taux gagne ; intérêts seuls → taux recalculé (base ≠ 0). 400 (detail = champ) : montant, nombre, taux, base ou intérêts
négatif, date absente. Le bilan (crédit, taux de financement, fonds propres, coût du financement) et les lignes de
trésorerie (`Funding`, `Refunding`) sont recalculés dans la même transaction.

## echeancier

Bouton « Générer » : n'écrit rien. Première échéance = date de déblocage + période de grâce (mois) ; échéance *i* =
première + (i − 1) × périodicité mois, jour borné au dernier jour du mois ; base = montant / nombre ; taux = taux de
charge ; intérêts = base × taux (taux appliqué à chaque échéance, pas annuel). 400 : nombre ≤ 0, montant ou taux négatif,
date absente.
