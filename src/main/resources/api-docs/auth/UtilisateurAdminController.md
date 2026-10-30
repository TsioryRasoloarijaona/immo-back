# UtilisateurAdminController

Comptes AdmUser — réservé au rôle Administrateur : liste, fiche, création, désactivation, rôles, comptes de dépôt, habilitations, réinitialisation du mot de passe Arpent et affectation des projets du périmètre commercial. Un compte créé ici n'ouvre pas OMRAN tant qu'un mot de passe n'y est pas défini. Aucune suppression : on désactive.

## findAll

Tous les comptes (AdmUser) avec leurs informations (identifiant, coordonnées, statut
activé/désactivé), la fiche employé (CfgTier) liée et les rôles (AdmRole) assignés — écran
d'administration des comptes. Les filtres se cumulent, et `/export` applique les mêmes. Réservé au
rôle Administrateur.

## reinitialiserMotDePasse

Ce geste est le seul qui ouvre ou rouvre l'accès web d'un compte. Génère **toujours** un nouveau mot de passe Arpent pour le login Omran
(`AdmUser.Code`), le renvoie une seule fois dans la réponse, et rend le compte **utilisable
immédiatement** : aucun changement de mot de passe ni formulaire de profil n'est demandé à la
connexion (mustChangePassword=false, profileCompleted=true). Invalide tous les tokens déjà émis pour
ce compte, sur tous les appareils (TokenVersion incrémenté). Crée l'enregistrement d'identifiants
web si le compte ne s'était jamais connecté.

**Destinataire** : `email` du corps s'il est fourni (pour cet envoi seulement, jamais enregistré),
sinon `AdmUser.Email`, à défaut l'email de la fiche employé (`CfgTier`). **Un compte sans aucune
adresse n'est plus refusé** : la réinitialisation a lieu, aucun email n'est envoyé
(`emailEnvoye=false`, `destinataire=null`) et l'administrateur transmet lui-même les identifiants
affichés. Le corps est facultatif (absent = `{}`).

L'email contient l'identifiant, le mot de passe et le lien vers Arpent (sans identifiant ni mot de
passe dans le lien). L'envoi est **asynchrone** et postérieur au commit : `emailEnvoye=true`
signifie qu'il a été déclenché, pas qu'il a été remis — `motDePasseTemporaire` doit donc toujours
être affiché. Réservé au rôle Administrateur.

### reponse 400

`email` fourni dans le corps mais invalide (pas de forme local@domaine.tld). Rien n'est modifié :
pas de nouveau mot de passe, sessions conservées. Le message serveur peut être affiché tel quel sous
le champ.

## getProjets

Les projets (CfgProject) qui composent le périmètre commercial de ce compte. Un utilisateur
restreint ne voit que les prospections, pré-prospections, dossiers et encaissements rattachés à ces
projets, et le tableau de bord n'agrège que ceux-ci — le filtrage est intégralement côté serveur.
Liste vide = aucune restriction : ce compte voit tout (un périmètre vide ne signifie jamais "ne voit
rien"). La liste des projets sélectionnables s'obtient par GET /api/projets (8 projets en tout, ni
recherche ni pagination nécessaires).

Renvoie les affectations **brutes**, pas le périmètre effectivement appliqué : un compte portant un
rôle d'exemption (Administrateur, Direction Générale) continue de tout voir même si cette liste est
non vide. Une liste non vide ne prouve donc pas qu'un compte est restreint — pour l'état réellement
appliqué, voir `perimetreRestreint` sur GET /api/mon-compte. Réservé au rôle Administrateur.

## remplacerProjets

**Remplacement intégral** du périmètre projet : le corps porte l'état final des cases à cocher, pas
un delta. `projetIds` est obligatoire ; envoyer une liste **vide** retire toute restriction à ce
compte (il voit alors tout) et n'est pas une erreur, alors qu'un corps sans `projetIds` (`{}` ou
`null`) est refusé en 400 — une levée de restriction doit être explicite, jamais la conséquence d'un
corps tronqué. Prise en compte immédiate, sans reconnexion : le périmètre est relu en base à chaque
requête et n'est jamais embarqué dans le jeton. Les identifiants répétés sont dédoublonnés.

Seule la portée **projet** est remplacée. `AdmUserHabilitation` est polymorphe et une même ligne
peut porter plusieurs portées à la fois : une ligne qui ne porte qu'un projet est supprimée, tandis
qu'une ligne mixte (portant aussi une société, une tranche ou un projet-travaux) est **conservée**,
seul son projet étant retiré. Aucune habilitation société / tranche / projet-travaux n'est donc
jamais perdue — elles appartiennent à l'application de bureau légataire, toujours en service.

Deux comportements à connaître côté écran d'affectation : les comptes portant un rôle d'exemption
(Administrateur, Direction Générale) restent non filtrés même après affectation ; et le sélecteur
d'utilisateurs devrait filtrer sur `disabled=false` (la grande majorité des comptes legacy sont
désactivés). Réservé au rôle Administrateur.

### reponse 400

`projetIds` absent ou null (envoyez `[]` pour retirer la restriction), ou au moins un `projetId` ne
correspond à aucun CfgProject.
