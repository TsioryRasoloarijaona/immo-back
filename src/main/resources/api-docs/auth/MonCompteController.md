# MonCompteController

Profil de l'utilisateur authentifié et changement volontaire de son mot de passe. Aucun portail de première connexion : mustChangePassword/profileCompleted ne bloquent plus aucune route.

## moi

Le compte authentifié : identité, rôles, et l'état mustChangePassword/profileCompleted. Ces deux
drapeaux restent renvoyés pour le contrat mais ne bloquent plus rien : il n'y a plus de portail de
première connexion (les comptes viennent d'Omran, déjà renseignés), le front les ignore.

## changerMotDePasse

Vérifie ancienMotDePasse puis stocke nouveauMotDePasse (bcrypt, min. 8 caractères). Efface
mustChangePassword et réémet un cookie access_token à jour — invalide tous les tokens émis avant cet
appel (TokenVersion incrémenté), y compris sur d'autres appareils.

## completerProfil

Renseigne prenom/nom/telephone/email/dateNaissance (adresse optionnelle) sur la fiche employé
(CfgTier) liée au compte, et marque profileCompleted=true. Réémet un cookie access_token à jour.
