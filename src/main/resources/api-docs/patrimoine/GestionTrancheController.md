# GestionTrancheController

## supprimer

Réservé au rôle `Administrateur`. Refusé (409, `detail` listant chaque table bloquante avec son nombre de lignes)
tant qu'une donnée référence la tranche par une clé étrangère sans cascade de la production : articles,
prospections, offres, plans, prévisions, devis, tarifs, ventilations, études de faisabilité, réclamations et dépenses
SAV, évènements d'agenda, commissions. Sinon supprime, dans la même transaction, ses terrains, ses étages, son
plancher couvert, ses frais, ses seuils de versement, ses habilitations, ses pièces jointes et notes (`CategoryId`),
puis la tranche ; le journal d'audit OMRAN reçoit une ligne `Deleted`.
