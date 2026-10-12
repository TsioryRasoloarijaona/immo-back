# ParcoursCommercialController

Parcours du commercial : « Ma journée », Boîte de tri des pré-prospections et avancement d'une prospection
dans le Kanban. « Mes lignes » = les prospections dont `ComProspection.CfgCommercialId` est l'employé du
compte appelant, dans son périmètre projet. Administrateur et Direction Générale voient leurs propres lignes ;
`commercialId` leur permet, ainsi qu'à un chef pour un membre de son équipe, de consulter un autre commercial.

## journee

Quatre blocs, chacun avec son total et ses 20 premières lignes :

- `nouveauxLeads` : pré-prospections du commercial, ouvertes, reçues il y a moins de 6 mois, **sans aucune
  interaction saisie**, les plus récentes d'abord ;
- `relancesDuJour` : relances ouvertes échéant aujourd'hui dont le résultat n'est pas une visite ;
- `visitesDuJour` : relances ouvertes du jour dont le résultat est « Rendez vous pris », « RDV reporté » ou
  « Le prospect veut faire une visite du projet » ;
- `enRetard` : relances ouvertes échues avant aujourd'hui, **même règle que `GET /api/dashboard/relances?echeance=RETARD`**
  (prospections qualifiées, ouvertes, créées dans le mois civil en cours).

Une relance ouverte est la dernière interaction datée d'une prospection ouverte : non désarmée
(`WithoutRevival`), sans échéance de paiement, non impayée. Les relances suivent l'heure de Casablanca.

## boiteDeTri

Pré-prospections du commercial appelant, ouvertes, reçues il y a moins de 6 mois (`toutes=true` lève la
limite), les plus récentes d'abord. Une ligne marquée « Pas joignable » y reste, avec sa `prochaineRelance`.
Chaque ligne porte ses `doublons` : fiches prospect et autres prospections au même numéro (9 derniers
chiffres, sans espaces ni indicatif) ou au même e-mail, limitées au périmètre de l'appelant. Le nombre à
trier du menu est le `totalElements` de cette liste.

Les autres gestes du tri utilisent les routes existantes : qualifier = `POST /api/prospections/{id}/transformer`
(fiche prospect requise, sinon `POST /api/pre-prospections/{id}/creer-prospect`), archiver =
`PATCH /api/prospections/{id}/archiver` (motif « PROSPECTION EN DOUBLE » proposé si un doublon est signalé),
appel effectué = `POST /api/prospections/{id}/interactions`.

## injoignable

Écrit une interaction « Téléphone Sortant », résultat « Prospect injoignable ou Boite vocale », message
« Pas joignable », avec une relance **demain à 10:00 (heure de Casablanca)**. Les relances précédentes de la
ligne sont soldées comme pour toute nouvelle relance. Rien d'automatique ensuite : pas de nouvel essai
programmé, pas d'archivage. La ligne reste dans la Boîte de tri et sort de « nouveaux leads ».

### reponse 409

La ligne n'est plus une pré-prospection (déjà qualifiée), ou elle est archivée. Aussi si le référentiel
d'interactions ne contient pas le type ou le résultat attendu.

## avancer

Geste du Kanban, **en une seule transaction** : change l'étape (mêmes effets que
`PATCH /api/prospections/{id}/etape`, sans toucher aux notes) puis écrit la prochaine action comme
`POST /api/prospections/{id}/interactions` (`typeId` = action, `sousCategorieId` = résultat, `date` = relance,
`message` = message). Les relances précédentes sont soldées. Un résultat de visite (« Rendez vous pris »,
« RDV reporté », « Le prospect veut faire une visite du projet ») crée aussi le rendez-vous à cette date :
il apparaît dans le calendrier et déclenche la confirmation de visite habituelle. Si une des deux écritures
échoue, rien n'est écrit. Le type de la prochaine action n'est pas stocké : l'intention passe par le résultat.

**Messages envoyés au prospect** (après validation de la transaction, comme les routes d'origine) : arriver en
« 4 APRES VISITES » envoie le remerciement de visite (e-mail et WhatsApp) ; un résultat de visite envoie la
confirmation de visite. Le front en avertit l'utilisateur avant d'enregistrer.

### reponse 400

`etapeId`, `prochaineAction`, `prochaineAction.typeId` ou `prochaineAction.date` absent, date dans le passé,
étape inconnue, ou règle d'interaction non respectée (résultat obligatoire hors Note).

### reponse 409

Prospection archivée, ligne encore pré-prospection (elle se trie dans la Boîte de tri), ou étape exigeant un
projet (`IsWithProjectMandatory`) sur une prospection sans tranche.
