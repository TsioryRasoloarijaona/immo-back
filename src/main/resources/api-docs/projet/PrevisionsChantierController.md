# PrevisionsChantierController

Bouton « Prévisions chantier » de la fiche projet de configuration : prévisions de main-d'œuvre du projet par type de travaux (`PrjProjectBuildingPrevision` × `GrhWorkedDayType`). Administrateur seul (règle d'URL de la configuration projet) **et** droit OMRAN `Yes.Prj.Front.Definition.Project.ProjectList.BuildingPrevision`. Ordre des refus : 404 projet inconnu, puis 403. Voir `instructions/FEATURE-n4-previsions-chantier.md` (N4b).

## lire

`actif` = le projet a un nivellement (`StkLevelingId`) ; sans lui la grille se lit mais ne s'enregistre pas (OMRAN grise le bouton). `totalHT` = PU HT × quantité, `totalTTC` = `totalHT` × (1 + TVA), calculés à la lecture (valeur absente comptée 0). TVA en pourcentage 0–100 sur le fil, en ratio en base.

## modifier

Remplace la grille entière : une ligne existante du même type de travaux est mise à jour en place (même `id`), un type nouveau crée une ligne, une ligne dont le type n'est plus envoyé est supprimée. Tout est validé avant la première écriture : 409 sans nivellement ; 400 si `lignes` est absente, si un type est absent, inconnu ou présent deux fois, si quantité, PU HT ou TVA est négatif ou dépasse `decimal(26,6)`, si les notes dépassent 1000 caractères. Trace `UserCreate*` / `UserUpdate*` remplie.
