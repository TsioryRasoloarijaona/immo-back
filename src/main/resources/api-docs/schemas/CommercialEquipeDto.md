# CommercialEquipeDto

## portefeuilleOuvert

Prospections (pas les pré-prospections) assignées à ce commercial (`ComProspection.CfgCommercialId` =
son employé), non clôturées, dans le périmètre de projets demandé. État à ce jour : ne dépend pas de
la période.

## sansActionDepuis7Jours

Parmi le portefeuille ouvert, les prospections dont la dernière action (tous auteurs) — à défaut la
date de la prospection — date de plus de 7 jours. À afficher en alerte quand il est non nul.

## relancesEnRetard

Parmi le portefeuille ouvert, les prospections dont la dernière relance non désarmée
(`ComActionMessage.AlertValidityDate`) est échue avant aujourd'hui. À afficher en alerte quand il est
non nul.
