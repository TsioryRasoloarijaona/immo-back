# TransformerProspectionRequest

## trancheId

Tranche (projet) à rattacher, `ComProspection.StkHierarchyId`. Fourni ici pour poser ou remplacer la
tranche au moment de la transformation. S'il est omis, la prospection doit **déjà** porter une
tranche — sinon 400, la tranche étant obligatoire pour transformer. Contrôle de périmètre commercial
: rattacher à un projet non affecté renvoie 403.

## articleId

Article/lot principal (StkItem) optionnel. S'il est fourni, il DOIT appartenir à la tranche
effective (celle de `trancheId`, ou celle déjà posée si `trancheId` est omis) — 400 sinon. Même
validation croisée que `PATCH /api/prospections/{id}/tranche-principale`.
