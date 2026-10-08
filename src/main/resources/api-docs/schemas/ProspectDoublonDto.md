# ProspectDoublonDto

Fiche prospect ou client existante (CfgTier de type PRO/PRORNT/CLI/CLIRNT) correspondant à
l'identité saisie sur au moins un critère. Sert à demander à l'utilisateur « est-ce l'une de ces
personnes ? » avant de créer une fiche.

## id

Identifiant (CfgTier.PKey) — ouvrable par `GET /api/prospects/{id}`, rattachable par `POST
/api/pre-prospections/{id}/lier-prospect`.

## criteres

Critères sur lesquels la fiche correspond — jamais vide. Une fiche qui correspond sur plusieurs
critères est listée avant les autres.
