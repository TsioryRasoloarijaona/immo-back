# CreerProspectionRequest

## commercialId

Commercial (CfgTier) affecté. Optionnel : fourni ici pour une prospection immédiatement complète,
sinon posé plus tard par `POST /api/prospections/{id}/transformer`.

## typeContactId

Type (ComActionMessageType → `ComActionMessageTypeId`), champ « Type ». **Obligatoire**. À
distinguer de la vocation (`vocationId`).
