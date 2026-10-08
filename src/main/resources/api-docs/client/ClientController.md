# ClientController

Tiers ayant acheté — un `CfgTier` référencé par au moins un dossier de vente. Il n'y a pas d'entité Client distincte dans le schéma : la fiche et ses sous-ressources sont celles de /api/prospects.

**Entités juridiques en exploitation.** La qualité de client se juge sur les seuls dossiers rattachés à une entité juridique active (`CfgCompany.IsActive`) — aujourd'hui ADMOD et GHRIZA. Un tier dont tous les dossiers relèvent d'une entité hors exploitation n'est donc plus un client au sens de cet endpoint et disparaît de la liste ; les agrégats `nombreDossiers`, `totalAPayer` et `totalRegle` suivent la même assiette, pour qu'ils concordent avec GET /api/clients/{id}/dossiers.

## findAll

Les tiers portant au moins un dossier de vente sur une entité juridique en exploitation, avec leurs
agrégats calculés côté serveur (nombre de dossiers, total à payer, total réglé). Une page vide
signifie qu'aucun client ne correspond, pas une erreur.

**Filtre par tranche** (`trancheId`, réservé aux administrateurs) : seuls les clients ayant au moins
un dossier dans cette tranche — même règle d'appartenance que `GET /api/dossiers?trancheId=` (un
article lié ou l'article principal du dossier dans la tranche). Les agrégats (`nombreDossiers`,
`totalAPayer`, `totalRegle`) restent calculés sur **tous** les dossiers du client, pas sur ceux de
la tranche seule.

### param trancheId

Tranche (`CfgTranche.id`, une seule valeur) : ne garde que les clients ayant au moins un dossier
dans cette tranche. **Réservé aux administrateurs — 403 sinon.** Absent = tous les clients,
comportement inchangé.

## getDossiers

Les dossiers dont ce tier est l'acquéreur — mêmes `DossierListItemDto` que GET /api/dossiers.

**Périmètre commercial** : si l'appelant est restreint (`UtilisateurCourantDto.perimetreRestreint =
true`), seuls les dossiers de ses projets affectés sont renvoyés. Le **client**, lui, n'est pas
filtré : sa fiche reste accessible, la liste peut simplement revenir vide ou partielle.
`totalElements` reflète le nombre filtré, pas le total réel.
