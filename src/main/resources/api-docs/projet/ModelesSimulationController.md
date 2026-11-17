# ModelesSimulationController

## listeModelesSimulation

Liste OMRAN « Modèles de simulation lot » (`MarketPrevisionModelList`, `FEATURE-lot-previsions-reel.md` §4 M4) et liste de la
modale « Choix du modèle de suivi du lot » (M5 : le front passe la catégorie de dépense du lot si elle est renseignée).
Lecture ouverte au droit `Yes.Prj.Front.Config.MarketPrevisionModelList` **ou** au droit d'édition des prévisions
`Yes.Prj.Front.Tdb.ProjectPrevision.toolNew`, sinon **403**. Liste vide s'il n'existe aucun modèle.

## creerModeleSimulation

Crée un modèle et ses lignes (§4 M1-M2, M7). Ordre : **403** sans `MarketPrevisionModelList`, **400** code ou description
vide, moins de deux lignes ou temps maximal différent de 1 (« Vous devez saisir au moins deux ligne de prévision avec
obligatoirement un pourcentage d'avancement du temps égale à 0% et 100% »), somme des paiements hors [0,99 ; 1,01] (« le
total des % de paiement doit être égale à 100% »), catégorie inconnue ; **409** code déjà pris (sans tenir compte de la
casse). Une ligne d'historique `AdmWebHistorics` (`Yes.Prj.Config.MarketPrevisionModel`).

## modifierModeleSimulation

Remplace l'en-tête et toutes les lignes du modèle. Mêmes contrôles que la création, précédés du **404** modèle inconnu ; le
modèle peut garder son propre code.

## supprimerModeleSimulation

Supprime le modèle et ses lignes. **409** si une prévision de faisabilité (`FSPrevision`) cite le modèle : la clé étrangère
l'interdit. **404** modèle inconnu, **403** sans `MarketPrevisionModelList`.
