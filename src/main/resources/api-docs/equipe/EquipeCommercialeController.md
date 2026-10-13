# EquipeCommercialeController

Écran « Mon équipe » du chef commercial. Réservé aux rôles `Commercial Chef`, `Administrateur` et `Direction Générale`.

L'équipe d'un chef = les comptes actifs « Commercial » ou « Commercial Aide » affectés à au moins un de ses projets (`AdmUserHabilitation`). Administrateur et Direction Générale voient tous les commerciaux actifs et tous les projets. Toutes les routes passent par ce même calcul ; un compte désactivé n'est jamais membre.

Les écritures (distribuer, réaffecter, affectations) vont directement dans les tables d'OMRAN, sans validation : un lot est contrôlé en entier avant la première écriture et un lot partiellement invalide n'écrit rien.

## entonnoir

Entonnoir **de cohorte** : les leads créés sur la période, et l'étape la plus avancée atteinte par
chacun à ce jour (voir `EntonnoirDto`). Les étapes ne sont pas datées en base : on ne peut pas compter
« les visites de la période » des leads plus anciens.

## commerciaux

Une ligne par commercial de l'équipe (avec `projet` : ceux qui peuvent recevoir une ligne de ce
projet). Le portefeuille et ses alertes sont l'état d'aujourd'hui ; `performance` porte les
indicateurs de la période, calculés exactement comme `GET /api/kpi/commerciaux`, sur les projets du
périmètre. Pour lister les prospections d'un commercial : `GET /api/prospections` filtré sur lui.

## aDistribuer

Pré-prospections ouvertes, sans commercial, dont la tranche est sur un projet de l'appelant (toutes
pour Administrateur / Direction Générale sans `projet`, lignes sans tranche comprises), **reçues dans la
période** (`ComProspection.DocDate` entre `du` et `au`, 30 derniers jours par défaut, même règle que
`entonnoir` et `commerciaux`) et, par défaut, **jamais traitées** : aucune action (`ComActionMessage`)
autre que système (`IsSystem`), même règle que la Boîte de tri. `toutes=true` lève cette dernière
condition. La plus ancienne est celle de la période. La liste elle-même :
`GET /api/equipe/a-distribuer/lignes` avec les mêmes paramètres — son `totalElements` est toujours ce
`nombre`.

## lignesADistribuer

Les lignes que compte `GET /api/equipe/a-distribuer` pour les mêmes paramètres (même règle, même
périmètre, même 403 pour un projet hors périmètre), paginées et triées par `DocDate` croissante.
Avec `toutes=true`, `dejaTraitee` signale celles qui ont au moins une action non système.

## distribuer

Pose le commercial choisi (`ComProspection.CfgCommercialId`) sur chaque pré-prospection, **comme le
champ « Commercial » de la fiche pré-prospection d'OMRAN** : la ligne reste une pré-prospection, aucun
prospect ni tranche n'est exigé. La transformer reste le geste du commercial
(`POST /api/prospections/{id}/transformer`). Une ligne qui avait déjà un commercial est redistribuée.
Historique : « Commercial assigné : B », ou « Commercial réaffecté : A → B ». Tout le lot ou rien.

### reponse 409

Une ligne n'est plus une pré-prospection ou est clôturée, ou le commercial n'est pas de l'équipe ou
n'est pas affecté au projet de la ligne : rien n'est écrit.

## reaffecter

Change le commercial (`ComProspection.CfgCommercialId`) de prospections ouvertes et trace « Commercial
réaffecté : A → B » dans leur historique. Le commercial référent de la fiche prospect n'est pas
modifié. Tout le lot ou rien.

### reponse 409

Une ligne est une pré-prospection (elle se distribue) ou une prospection clôturée, ou le commercial
n'est pas de l'équipe ou pas affecté au projet de la ligne : rien n'est écrit.

## repartir

Répartition automatique : pour chaque pré-prospection demandée, le commercial est choisi par la même
fonction que la création et l'import. Projet = projet de la tranche ; éligibles = comptes actifs
« Commercial » / « Commercial Aide » avec un employé et une **affectation explicite** à ce projet (un
commercial sans restriction n'est pas éligible). Lead déjà connu (même téléphone, 9 derniers chiffres,
ou même e-mail qu'une autre prospection ouverte ou qu'un prospect suivis par un éligible) → ce
commercial (`CONNU`) ; sinon le moins de leads à trier (`MOINS_CHARGE`, même définition que la Boîte de
tri), puis la dernière pré-prospection reçue la plus ancienne, puis le nom ; la charge est mise à jour
au fil du lot. Une ligne déjà attribuée n'est jamais reprise (`DEJA_ATTRIBUEE`). Avec `simulation`
(défaut `true`) rien n'est écrit ; sinon historique « Commercial assigné : B — répartition
automatique » et notification à chaque commercial. 403 si une ligne est hors des projets d'un chef.

Le lot est soit `preProspectionIds` (les lignes cochées), soit `filtre` : alors **toutes** les lignes que
`GET /api/equipe/a-distribuer/lignes` renverrait pour ce filtre, toutes pages confondues — c'est-à-dire
le `nombre` du compteur. Exactement un des deux, sinon 400 ; au-delà de 5 000 lignes au filtre, 400
(découper la période).

## modifierAffectations

Ajoute ou retire des projets à un commercial, **jamais un remplacement total**. Un chef ne touche que
ses propres projets, et seulement des commerciaux : un autre chef, un administrateur, la direction
générale ou un commercial sans aucune restriction de projet lui sont refusés (403). Un projet déjà
affecté (ajout) ou non affecté (retrait) est ignoré.
