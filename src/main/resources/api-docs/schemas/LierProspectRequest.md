# LierProspectRequest

Rattachement d'un prospect existant à une pré-prospection (réponse « c'est l'une de ces personnes »
à la liste de doublons).

## prospectId

Fiche existante (CfgTier) de type prospect ou client — typiquement un `id` renvoyé par `GET
/api/prospects/doublons` ou par le 409 de `creer-prospect`. Obligatoire ; 400 si inconnue ou d'un
autre type de tiers.
