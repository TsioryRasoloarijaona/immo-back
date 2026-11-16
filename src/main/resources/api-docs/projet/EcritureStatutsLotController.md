# EcritureStatutsLotController

Écrans OMRAN « Statuts des lots » et « Transitions des lots » : statuts (`StrStatus`) et transitions (`StrWorkFlow`) des 5 genres de lot (`StrEntity` de code `PrjMarket`, `PrjMarketMultiSupplier`, `PrjMarketAdministrative`, `PrjMarketStudies`, `PrjMarketDealers`). Lectures ouvertes à tout authentifié. Écritures : droit OMRAN `Yes.Prj.Front.Config.MarketStatusList` (statuts) ou `Yes.Prj.Front.Config.MarketWorkFlowList` (transitions), l'Administrateur passe. Ordre des refus : 404 (id inconnu), 403, 400 (saisie), 409. Voir `instructions/FEATURE-restes-projet-achat.md` §6.

## statutsLot

Fiche complète de chaque statut, triée par code de genre puis par ordre. `couleur` est un nom de couleur .NET (`LimeGreen`…), à traduire côté écran ; `cases` liste les numéros cochés, `champsObligatoiresIds` les `StrField` exigés.

## catalogue

`cases` ne rend que les cases dont le genre porte un libellé (`StrEntity.BoolFieldNNNLabel` non vide) : ce sont les seules qu'un statut de ce genre peut cocher. `champs` = les `StrField` du genre.

## creerStatut

Obligatoires : `genreId` (un des 5 genres), `ordre`, `code`, `description`, `typeId`. Le code est unique dans le genre, sans tenir compte de la casse (409). `cases` et `champsObligatoires` doivent figurer au catalogue du genre (400 sinon) ; durée de vie ≥ 0 (0 = infinie) ; couleur = nom .NET connu ou vide.

## modifierStatut

Remplace toute la fiche : un champ facultatif absent est vidé, les cases du catalogue absentes de `cases` sont décochées, les champs obligatoires sont remplacés en bloc. `genreId` ne change pas : une valeur différente du genre du statut répond 400.

## supprimerStatut

Refusée en 409 tant que le statut est cité par un lot (`PrjMarket.StrStatusId`), une transition (départ ou arrivée), une ligne d'historique (`StrHistory`) ou un paramétrage d'intervenants ou de rapports du statut ; le message donne les nombres. Ses champs obligatoires partent avec lui.

## creerTransition

Obligatoires : `genreId`, `description`, `statutDepartId` et `statutArriveeId`, deux statuts différents de ce genre (400 sinon). Une seule transition par couple départ → arrivée dans un genre (409). Chaque rôle de `roleIds` devient un intervenant du genre (`StrIntervener`, réutilisé s'il existe) lié à la transition.

## modifierTransition

Remplace toute la transition, rôles compris (liens recréés). `genreId` ne change pas : une valeur différente répond 400.
