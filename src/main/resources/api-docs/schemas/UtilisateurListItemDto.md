# UtilisateurListItemDto

Une ligne de GET /api/utilisateurs — compte (AdmUser) avec ses informations, sa fiche employé
(CfgTier) et ses rôles (AdmRole), pour l'écran d'administration des comptes.

## compteWebInitialise

true si des identifiants web (AppUserCredential) existent pour ce compte — false si le compte legacy
ne s'est encore jamais connecté à l'application web et n'a jamais été initialisé par un
administrateur (voir POST /api/utilisateurs/{admUserId}/reinitialiser-mot-de-passe).

## mustChangePassword

Drapeau hérité AppUserCredential.MustChangePassword — ne bloque plus rien (aucun changement imposé à
la connexion). false, y compris si compteWebInitialise est false.

## profileCompleted

Drapeau hérité AppUserCredential.ProfileCompleted — ne bloque plus rien (aucun formulaire de
première connexion). false si compteWebInitialise est false.

## nombreProjets

Nombre de projets affectés à ce compte (périmètre commercial), pour afficher l'état d'affectation
directement dans la liste d'administration sans un appel par ligne. 0 = aucune restriction : ce
compte voit tout (un périmètre vide signifie "pas de restriction", jamais "ne voit rien"). Le détail
des projets s'obtient par GET /api/utilisateurs/{admUserId}/projets. À noter : ce compteur ne tient
pas compte des rôles d'exemption — un Administrateur affecté à 2 projets affiche 2 mais n'est de
fait jamais filtré.
