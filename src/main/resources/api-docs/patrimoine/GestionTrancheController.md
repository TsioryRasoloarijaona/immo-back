# GestionTrancheController

## chargerDepuis

Réservé au rôle `Administrateur`. Reprend « Charger à partir d'une autre tranche » d'OMRAN, en une transaction.
Recopie de la source : standing, nombre d'étages, responsable SAV, coordonnées (adresses, ville, code postal, pays,
téléphones, email, site), adresse SAV, description détaillée, montant de pré-réservation, avance (mode, taux,
montant), frais de dossier, pénalités d'annulation et de transfert, syndic, autres frais, délais et pénalités des
engagements, délai de solde, garanties, tribunal, code du droit de rétractation, cadeaux, toutes les remises, délai
d'intervention, visites max, délais notaire. Les listes Étages, Plancher couvert et Frais sont fusionnées par clé
(ligne de la source absente de la cible → ajoutée ; présente → valeurs de la source ; les autres lignes de la cible
restent) et le plancher couvert est recalculé. Jamais copiés : code, libellé, programme, dates, visibilités, délai de
rétractation, délai max d'échéance et sa vérification, crédit, écarts-types, tiers autrui, projet de facturation, plan
comptable, rubrique, terrains, commissions, seuils de versement.

## supprimer

Réservé au rôle `Administrateur`. Refusé (409, `detail` listant chaque table bloquante avec son nombre de lignes)
tant qu'une donnée référence la tranche par une clé étrangère sans cascade de la production : articles,
prospections, offres, plans, prévisions, devis, tarifs, ventilations, études de faisabilité, réclamations et dépenses
SAV, évènements d'agenda, commissions. Sinon supprime, dans la même transaction, ses terrains, ses étages, son
plancher couvert, ses frais, ses seuils de versement, ses habilitations, ses pièces jointes et notes (`CategoryId`),
puis la tranche ; le journal d'audit OMRAN reçoit une ligne `Deleted`.
