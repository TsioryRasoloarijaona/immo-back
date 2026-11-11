# DepensesProjetController

Dépenses des projets de chantier en **lecture seule** : factures fournisseurs (`BuyDocument.DocType = 'INVOICE'`) et
règlements qui leur sont affectés. Voir `FEATURE-achat-depenses-projet.md`.

**Visibilité, comme OMRAN** : projets des lignes `AdmUserHabilitation.PrjProjectId` de l'appelant, **quel que soit son
rôle** (aucune exemption Administrateur). Un filtre hors périmètre (projet, lot, facture) donne une réponse vide (200),
jamais 403 ni 404. Documents à régler, avoirs, commandes, devis et bons de livraison ne sont ni comptés ni sommés.

**Solde** : celui de `GET /api/achat/documents` (même calcul) : réglé = Σ des lignes de règlement non annulées
(`CANCELED` exclu), reste = à payer − réglé ; un total de projet ou de lot = Σ à payer − Σ réglé, arrondi une fois.

## depenses

Un élément par projet visible (restreint à `projetIds` s'il est fourni) **ayant au moins une facture**, tri par code de
projet. `lots[]` : une ligne par lot ayant des factures, tri par code, puis la ligne « Hors lot » (`lotId` null).
Non soldée = reste > 0,05 ; trop réglée = reste < −0,05 sur une pièce à payer positive, comptée à part, jamais parmi
les non soldées. « À payer » d'un lot = toutes ses factures : il diffère du « Facturé » de la fiche lot (factures des
décomptes seulement) quand une facture du lot n'a pas de décompte.

## reglements

Une ligne par affectation `StlSettlementLine` sur une facture d'un projet visible. Un règlement réparti sur plusieurs
projets ne montre que ses lignes visibles, chacune avec son montant. Les lignes des règlements annulés sont listées avec
`annule = true` (exclues avec `inclureAnnules=false`). `du` / `au` portent sur la date du règlement (jour, bornes
incluses). Tri fixe : date du règlement décroissante, puis n° de règlement décroissant ; `sort` est ignoré.
