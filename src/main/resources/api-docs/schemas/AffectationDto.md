# AffectationDto

Imputation d'une part d'un règlement sur une échéance précise. Un règlement en porte de une à trois
sur les données réelles.

## montantAffecte

Part du règlement imputée à **cette** échéance.

**Ce n'est pas le montant du règlement**, et c'est la distinction que cette ressource existe pour
rendre visible : un règlement qui couvre trois échéances porte trois lignes dont la somme fait son
montant brut.

## montantEcheance

Montant total de l'échéance, toutes imputations confondues. À comparer à `montantAffecte` pour
savoir si cette échéance est soldée par ce seul règlement.

## dossierId

Dossier de vente portant l'échéance. **Peut différer d'une ligne à l'autre** : un même règlement
peut solder des échéances de plusieurs dossiers.
