# HistoriqueDto

Une ligne de l'onglet « Historique » d'une fiche, la plus récente en premier. Deux sources selon
l'endpoint :

- **tiers** (`GET /api/prospects/{id}/historique`, bloc `historique` de la Hotline) : les changements
  de statut des dossiers de vente dont le tiers est le client (`StrFolderStatusHistory` filtré par
  `CfgTierId`), soit les colonnes OMRAN « Utilisateur, Date, État, N° Dossier, Article, Détails ».
  `dossierId`, `dossierCode` et `articleCode` y sont renseignés ;
- **prospection** (`GET /api/prospections/{id}/historique`) : le journal `StrHistory` de la
  prospection. `dossierId`, `dossierCode` et `articleCode` y sont toujours null.

## id

Identifiant de la ligne d'historique : `StrFolderStatusHistory.PKey` pour un tiers, `StrHistory.PKey`
pour une prospection.

## description

Détails saisis avec l'événement : la note du changement de statut pour un tiers (souvent null),
le texte de l'action pour une prospection (ex. « Étape changée : 2 RDV → 4 APRES VISITES »).

## statutDescription

État atteint : libellé du statut de dossier (ex. « RESERVATION ») pour un tiers ; statut éventuel
de la ligne `StrHistory` pour une prospection.
