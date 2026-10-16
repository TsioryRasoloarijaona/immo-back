# ModifierEcheanceRequest

Mise à jour partielle d'une échéance : seuls les champs non nuls sont appliqués. Le client n'envoie que les champs que l'utilisateur a changés.

## dateEcheance

Date d'exigibilité, ramenée à minuit. Absente : la date de la ligne reste, même en changeant de catégorie.

## montant

Montant TTC dû. Peut être inférieur au déjà réglé (le reste devient négatif). Absent : le montant reste, même en changeant de catégorie.

## description

Libellé de la ligne.

## categorieId

Catégorie d'échéancier (`ComSettlementCategory`). Changée : TVA, « Exclure du prix » et remise ancien client sont repris de la nouvelle catégorie.

## exclureDuPrix

« Exclure du prix ». Envoyé avec un changement de catégorie, prime sur la valeur de la catégorie.

## notes

Notes libres de la ligne.
