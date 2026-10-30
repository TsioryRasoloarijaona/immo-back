# TachesController

Écran « Tâches » d'OMRAN : la table `AdmTask` porte aussi les rendez-vous, jamais exposés ici (un rendez-vous répond 404). Réservé à l'Administrateur. Voir `instructions/FEATURE-admin-taches.md`.

## changerStatut

Le statut d'arrivée se résout dans `StrWorkFlow` depuis le statut actuel (vide = Non commencée). Même statut ou statut inconnu : 409. Écrit une ligne `StrHistory`, avec le commentaire en notes.
