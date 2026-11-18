# PrevisionsProjetController

## projetsPrevisions

Liste « Projet » de l'écran OMRAN « Prévisions des lôts » (`FEATURE-n4-previsions-chantier.md` R1) : projets de
l'habilitation du compte (`AdmUserHabilitation.PrjProjectId`), triés par description. **403** sans le droit d'écran.

## previsionsProjet

Écran OMRAN **Projet › Prévisions** (`FEATURE-n4-previsions-chantier.md` R1-R10). Lots retenus : lots du projet avec
`IncludedInPaymentPrevision = 1` (NULL exclu) **et** visibles du compte (statut visible d'un compte restreint) ; un lot
invisible n'apparaît ni dans les lignes ni dans les montants. Séries par lot identiques à
`GET /api/projet/lots/{id}/previsions` : estimation de base simulée (Prévision), prévisions de paiement datées
`PrjMarketPaymentPrevision` (P), pièces payables hors avoirs par échéance (R).

- Prévision : `total` = Σ estimation, `due` = Σ des lignes datées avant `aujourdhui`, `reste` = `total` − `due`.
- Prévision ajustée : `total` = ΣP + ΣR, `due` = ΣP échues + ΣR, `reste` = ΣP non échues.
- Réel : `total` null, `due` = ΣR, `reste` = ΣP (comme OMRAN, `ProjectPrevision.cs` l.220).
- Avancements : lots non multi-fournisseurs, poids = HT prévisionnel du lot / Σ ; mois sans point = dernière valeur
  connue. Total = valeur du mois du jour, 0 avant le début du projet, 1 après sa fin (1ᵉʳ du mois suivant la dernière fin
  de lot). Les listes `avancements.*` ne gardent que les mois où la valeur change.

Aucun arrondi côté serveur. Projet sans lot retenu : **200**, zéros et listes vides. **404** projet inconnu ou non habilité
(même message), **403** sans droit `Yes.Prj.Front.Tdb.ProjectPrevision`, **400** `projetId` absent ou mal formé.

## exporterPrevisionsProjet

Classeur des mêmes chiffres que `GET /api/projet/previsions` (R11) : feuilles « GENERAL » (bloc Net à payer), « Lots »,
puis Décaissement et Avancement de Prévision, Prévision ajustée et Réel (noms coupés à 31 caractères). Pas d'image du
graphe. Mêmes codes d'erreur que la lecture.
