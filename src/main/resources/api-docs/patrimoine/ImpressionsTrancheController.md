# ImpressionsTrancheController

Impressions du menu « Imprimer » de la fiche tranche propres à la tranche : la Relance 2 par statut (gabarit OMRAN
`CfgTrancheFicheRelance2`). La « Fiche de réservation » de ce menu n'a pas de route ici : l'écran fait choisir un dossier
de la tranche puis ouvre `GET /api/dossiers/{id}/documents/fiche-reservation`. Lecture seule : aucune écriture.
Réservé à Administrateur et Direction Générale (403 avant tout autre refus) ; clé d'API : 403 `HORS_PORTEE`.

## apercu

Une seule sélection, partagée avec le PDF : dossiers dont l'**article** (sous-article compris) est dans la tranche, au
statut `CONCRETISATION`, `MEILLEURTAUX`, `ENVOI AU NOTAIRE`, `CREDIT EN COURS` ou `CREDIT ACCORDE` (ou au sous-ensemble
`statut`), avec un client principal. Une lettre par acquéreur : le client principal, puis chaque co-acquéreur
(`ComFolderCustomer`) par nom ; tous reçoivent les montants du dossier. Montants = onglet Montants du dossier
(`prixVente`) : `prixRegle + reste = prixVente`. Un dossier au reste ≤ 0 n'a pas de lettre et va dans `ecartes`.
Ordre des `lignes` = ordre des pages : code d'article (tri naturel), dossier, principal avant co-acquéreurs.
`dossiers` et `parStatut[].dossiers` comptent les écartés ; `lettres` non.

## lettres

Une page par ligne de l'aperçu, rendue par le modèle de document actif `RELANCE_TITRE_FONCIER / RELANCE_2` (texte à
rédiger dans les modèles de documents), sinon par le texte livré. Le reste imprimé est `reste.prix` (reste du prix de
vente), jamais `reste.total`. Date de la lettre (`lettre.date`) = `Revival2` du dossier, sinon la date du jour.
Fichier `relance2_<codeTranche>_<aaaammjj>.pdf`.

### reponse 409

Aucun dossier de la tranche n'a un statut à relancer, ou tous les dossiers retenus ont un reste du prix ≤ 0.
