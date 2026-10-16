# DossierEditionController

Fiche du dossier de vente modifiable, onglet par onglet, comme l'écran OMRAN (`Yes.Com.Front.Sales/Sale.cs:FilterToolsByRights`). Chaque champ appartient à un groupe de droits (voir `GET /api/dossiers/{id}/edition`). Voir FEATURE-vente-edition.md.

## modifier

Corps partiel : seuls les champs présents et non nuls changent ; pour remettre un champ à vide, mettre son nom dans `champsAVider` (le client ne peut pas être vidé). Un texte vide vaut absent.

Groupes et rôles : DATES et ANNULATION (Commercial Chef, Direction Générale, Administrateur) ; CLIENT, COMMERCIAL, PENALITES et PRIX — remise, TVA, « modifications intégrées au prix » (Administrateur) ; GESTIONNAIRES, NOTAIRE, CREDIT et OPTIONS (Commercial Chef, Administrateur) ; NOTE (tout utilisateur qui voit le dossier). Un seul champ d'un groupe interdit suffit pour un 403, rien n'est écrit.

Champs verrouillés (403, rien n'est écrit ; la liste de l'appelant est `champsVerrouilles` de `GET /{id}/edition`) :
- `concretisation` déjà renseignée : la modifier ou la vider est réservé au groupe PRIX (Administrateur) ; le premier renseignement reste ouvert au groupe DATES.
- `impressionContrat` : groupe PRIX seulement.
- `remise` : article sans premier plafond (`StkItem.DiscountMax` nul ou 0) et appelant sous le deuxième niveau de remise : toute nouvelle remise non nulle est refusée, avec le message d'OMRAN « Vous ne pouvez pas appliquer une remise pour cet article (x %) ! ».

Règles d'OMRAN appliquées :
- **Date du dossier** (`dateDossier`, groupe DATES) : ne se vide pas ; entre dans l'ordre des dates ci-dessous.
- **Prix de vente** (`prixVenteHT` / `prixVenteTTC`) : autre saisie de la remise, exclusive des trois autres. HT : montant de remise = total HT avant remise − saisie ; TTC : taux = 1 − saisie / total TTC avant remise. Saisie au-dessus du prix avant remise ⇒ 400.
- **Commercial** : `commercialId` doit être un tiers de `GET /api/commerciaux` (genre COM, AGC, ATC, ACC ou RC), sinon 400.
- **Dossier sorti** : vider `annulationDate` ou `annulationCauses` ⇒ 400 avec le message d'OMRAN (`Sale.cs:BeforeUpdate`).
- **Remise** (`Sale.cs:RecalcDiscount`) : montant HT ou pourcentage du total HT ; le montant est conservé et le taux recalculé, les prix de l'article et des sous-articles suivent en partant du prix article HT stocké (pas de superficie × prix du m²), comme pour un changement de TVA ou de « modifications intégrées au prix ». Plafonds de l'article (`VerifDiscount`) : au-delà de `StkItem.DiscountMax2` (deuxième niveau), 400 avec le message d'OMRAN, sauf `forcerRemise = true`. `DiscountChangeDate` = maintenant à chaque changement de montant. Le schéma de financement n'est pas rééquilibré : `ecartSchema` donne l'écart à corriger dans l'échéancier.
- **Ordre des dates** (`VerifDates`) : prospection ≤ dossier ≤ concrétisation ≤ envoi notaire prévisionnel ≤ envoi notaire ≤ minute ≤ remise des clés effective ; contrôlé quand l'un de ces jalons change, 400 avec le message d'OMRAN.
- **Client** : le nouveau tiers devient l'acquéreur principal (refusé s'il est déjà co-acquéreur) ; ligne d'historique de statut, puis bascule client / prospect du nouveau et de l'ancien client.
- **Note** (`MigrateNotes`) : une note non vide et changée est aussi enregistrée en action système « Note ».
- **Délai et crédit maximaux** (`Sale.cs:VerifThreshold`, à chaque enregistrement d'un dossier non sorti, et à la création) : option « délai max d'échéance » de la tranche — dernière échéance après la remise des clés prévue (article, sinon début de finalisation de la tranche ; à défaut la remise effective) + `MaxEcheanceDelai` jours ⇒ 409 « Vous avez dépassé le délai max de paiement ! » ; option « vérification du crédit » — crédit du schéma au-dessus de prix × pourcentage ⇒ 409 « Vous avez dépassez le montant max du crédit ! ». Pas de contrôle pour un utilisateur sans le droit OMRAN `toolVerifyThreshold` quand ce droit est paramétré.
- Dossier signé (`UserUpdate*`) et ligne d'historique « Modification : <champs> » dès qu'un champ change.

### reponse 400

Corps vide, champ inconnu dans `champsAVider`, plusieurs saisies de remise ensemble, valeurs hors bornes (remise ou prix de vente négatifs, prix de vente au-dessus du prix avant remise, pourcentage hors 0–100, TVA hors 0–1, texte trop long), tiers ou décision introuvable, commercial qui n'en est pas un, annulation vidée sur un dossier sorti, plafond de remise dépassé, dates dans le désordre.
