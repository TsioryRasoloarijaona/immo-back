# CreerReglementRequest

## montant

Montant du règlement. **Requis**, strictement positif, et au plus égal au reste à payer de
l'échéance (400 sinon — le message indique le reste disponible).

## typeId

Type de règlement (`StlSettlementType.PKey`) — voir `GET /api/referentiels/types-reglement`.
**Requis** : alimente le « Mode de versement » de l'autorisation d'encaissement.

## banqueId

Banque (`CfgTier.PKey`, type BAN) — voir `GET /api/referentiels/banques`. **Requise si** le type
choisi a `banqueObligatoire = true` (400 sinon) ; facultative pour les autres types, typiquement
ESPÈCES.

## reference

Référence du règlement (`StlSettlement.Code`) — n° de chèque, de virement... **Requise si** le type
choisi a `referenceObligatoire = true` (400 sinon), c'est-à-dire pour CHÈQUE / CHEQUE NOTAIRE /
SYNDIC / BANQUE / CERTIFIÉ / EFFET ; libre pour ESPÈCES.

## payeurId

Tiers ayant effectivement réglé (`StlSettlement.TierPayerId`) — « réglé par », peut différer de
l'acquéreur principal (co-acquéreur, tiers payeur...). Alimente le bloc « Versement effectué par » de
l'autorisation d'encaissement et le reçu. **Facultatif** : absent, le payeur est le client du
dossier. `StlSettlement.TierId` reçoit toujours le client du dossier, comme le logiciel de bureau ;
400 si `payeurId` est absent et que le dossier n'a pas de client.

## statut

Statut à la création. Seul `IN_WORK` est accepté (ou l'absence de valeur, équivalente). **`POINTED`
est refusé (400)** : pointer un règlement exige une preuve de paiement, or aucune pièce jointe ne
peut exister avant que le règlement existe. Le parcours « saisir directement en pointé » s'obtient
en enchaînant trois appels : créer ici, déposer la preuve sur `POST
.../reglements/{reglementId}/pieces-jointes`, puis `PATCH .../reglements/{reglementId}/statut`.
