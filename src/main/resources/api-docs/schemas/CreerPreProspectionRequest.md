# CreerPreProspectionRequest

Création d'une pré-prospection (lead brut, non qualifié). Seul `nom` est obligatoire ; le prospect
(`CfgTier`) et le commercial ne se posent pas ici mais par `POST
/api/pre-prospections/{id}/creer-prospect` puis `POST /api/prospections/{id}/transformer`.

## nom

Nom du contact tel que saisi (`TempDescription`, 255 caractères max). **Obligatoire**, non blanc.
Renseigné sur 99,9 % des pré-prospections réelles.

## telephone

Téléphone, stocké tel quel (`TempTel`, 255 max). C'est ce champ que la hotline retrouve par numéro,
quel que soit le format.

## trancheId

Tranche visée (CfgTranche → `StkHierarchyId`). Optionnel ; 400 si inconnue, **403** si son projet
est hors du périmètre commercial de l'appelant.

## vocationId

Vocation (StkVocation). Absente : la vocation de code `VENTE` — celle de 100 % des pré-prospections
réelles qui en portent une.
