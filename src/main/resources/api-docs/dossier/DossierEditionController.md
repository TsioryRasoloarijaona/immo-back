# DossierEditionController

Fiche du dossier de vente modifiable, onglet par onglet, comme l'écran OMRAN (`Yes.Com.Front.Sales/Sale.cs:FilterToolsByRights`). Chaque champ appartient à un groupe de droits (voir `GET /api/dossiers/{id}/edition`). Voir FEATURE-vente-edition.md.

## modifier

Corps partiel : seuls les champs présents et non nuls changent ; pour remettre un champ à vide, mettre son nom dans `champsAVider` (le client ne peut pas être vidé). Un texte vide vaut absent.

Groupes et rôles : DATES et ANNULATION (Commercial Chef, Direction Générale, Administrateur) ; CLIENT, COMMERCIAL, PENALITES et PRIX — remise, TVA, « modifications intégrées au prix » (Administrateur) ; GESTIONNAIRES, NOTAIRE, CREDIT et OPTIONS (Commercial Chef, Administrateur) ; NOTE (tout utilisateur qui voit le dossier). Un seul champ d'un groupe interdit suffit pour un 403, rien n'est écrit.

Règles d'OMRAN appliquées :
- **Remise** (`Sale.cs:RecalcDiscount`) : montant HT ou pourcentage du total HT ; le montant est conservé et le taux recalculé, les prix de l'article et des sous-articles suivent en partant du prix article HT stocké (pas de superficie × prix du m²), comme pour un changement de TVA ou de « modifications intégrées au prix ». Plafonds de l'article (`VerifDiscount`) : au-delà de `StkItem.DiscountMax2` (deuxième niveau), 400 avec le message d'OMRAN, sauf `forcerRemise = true`. `DiscountChangeDate` = maintenant à chaque changement de montant. Le schéma de financement n'est pas rééquilibré : `ecartSchema` donne l'écart à corriger dans l'échéancier.
- **Ordre des dates** (`VerifDates`) : prospection ≤ dossier ≤ concrétisation ≤ envoi notaire prévisionnel ≤ envoi notaire ≤ minute ≤ remise des clés effective ; contrôlé quand l'un de ces jalons change, 400 avec le message d'OMRAN.
- **Client** : le nouveau tiers devient l'acquéreur principal (refusé s'il est déjà co-acquéreur) ; ligne d'historique de statut, puis bascule client / prospect du nouveau et de l'ancien client.
- **Note** (`MigrateNotes`) : une note non vide et changée est aussi enregistrée en action système « Note ».
- Dossier signé (`UserUpdate*`) et ligne d'historique « Modification : <champs> » dès qu'un champ change.

### reponse 400

Corps vide, champ inconnu dans `champsAVider`, valeurs hors bornes (remise négative, pourcentage hors 0–100, TVA hors 0–1, texte trop long), tiers ou décision introuvable, plafond de remise dépassé, dates dans le désordre.
