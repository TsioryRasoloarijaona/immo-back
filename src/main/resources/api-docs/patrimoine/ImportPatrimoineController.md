# ImportPatrimoineController

Import Excel du patrimoine (« Importer » d'OMRAN, GEN0018) : articles et sous-articles d'une tranche (P9a). Chaque ligne
passe par le service de l'écran (mêmes règles, même audit) ; Administrateur seul.

## modele

En-têtes OMRAN exacts (`ImportItems` / `ImportUnderItems`), une ligne par article (ou sous-article) existant de la
tranche, puis une feuille d'aide par référentiel (Code, Libellé). Nom : `<code tranche>_STOCK_ARTICLES_<horodatage>.xlsx`
ou `…_STOCK_SOUS_ARTICLES_…`. Réimporter le modèle tel quel donne des lignes INCHANGEE.

## importer

Ligne 1 = en-têtes, puis une ligne de données par objet ; le **pivot** (code dans la tranche) choisit création ou mise à
jour. Colonne absente ou cellule vide = champ intact (l'import ne vide jamais un champ). Nombres en cellule numérique ou
en texte (`1 234,56`, `1234.56`) ; dates en cellule date ou `jj/mm/aaaa` ; cases `Oui/Non`, `Vrai/Faux`, `1/0`, `X`.
TVA supérieure à 1 lue en pourcentage (÷ 100, avertissement). Référentiels : Immeuble, Étage, Catégorie, Catégorie de
charge, Tranche, Nature par code **ou** libellé (casse ignorée) ; Vocation, Type, Type présentation, Orientation, Entité
de gestion du syndic par code seul. En-tête inconnu = avertissement « colonne ignorée ».

Une seule transaction : la moindre ligne en ERREUR, ou `simulation=true`, annule tout (`ecrit=false`, aucune trace
d'audit). Les messages d'erreur métier sont ceux des écrans (`POST/PATCH /api/articles`, `/api/sous-articles`).

Refus du fichier entier (400) : `trancheId` absent (« Vous devez sélectionner une Tranche ! »), format `.xls` (« enregistrez
le fichier au format .xlsx »), contenu non ZIP, archive corrompue ou piégée (bombe, entrée géante, DTD), plus de 1 000
lignes de données, aucune feuille, feuille demandée absente, aucune donnée, en-tête en double.
