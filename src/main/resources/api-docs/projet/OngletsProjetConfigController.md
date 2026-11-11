# OngletsProjetConfigController

Onglets de la fiche projet de l'écran de configuration (Administrateur seul, tous projets hors périmètre) : statut et transitions, historique, ventilation par tranches, critères d'avancement. Voir `instructions/FEATURE-projets-config-parite.md`.

## executerTransition

Seules les transitions partant du statut courant sont permises (toutes, sans filtre d'intervenant, comme `StrStatus.GetNextWorkFlowList`) ; une autre, inconnue ou déjà faite répond 409 avec `transitionsAutorisees` (codes des statuts d'arrivée permis). Une transition `WithPassword` (second utilisateur) répond 409. Une ligne `StrHistory` est écrite : `« {transition}: {départ}=>{arrivée}({login} & ) »`, notes = commentaire.

## ajouterVentilation

Règle OMRAN (`Project.cs:Verif`) : la somme des taux du projet, arrondie à 2 décimales, ne dépasse pas 1, sinon 400 « La ventilation de votre document est incorrecte ! ». Une somme inférieure à 1 est acceptée. La tranche doit appartenir à la société du projet et ne pas être déjà ventilée sur ce projet.

## modifierVentilation

Mêmes règles que l'ajout, la ligne modifiée étant comptée avec son nouveau taux.

## importerCriteres

Supprime tous les critères du projet puis copie ceux du projet source (O.D.T., description, catégorie). Refusé en 409 si un critère actuel du projet est cité par un PV de réunion (`BulMeetingPrjProjectProgressCriteriaLine`) : rien n'est changé.
