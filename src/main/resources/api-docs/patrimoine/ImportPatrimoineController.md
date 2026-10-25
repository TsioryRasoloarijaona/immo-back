# ImportPatrimoineController

Import Excel du patrimoine (« Importer » d'OMRAN, GEN0018) : articles et sous-articles d'une tranche (P9a), types de
présentation, terrains et prévisions de base de la tranche (P9b). Chaque ligne passe par le service de l'écran (mêmes
règles, même audit) ; Administrateur seul.

## modele

En-têtes OMRAN exacts, une ligne par objet existant, puis une feuille d'aide par référentiel (Code, Libellé).
`articles` / `sous-articles` : lignes de la tranche, nom `<code tranche>_STOCK_ARTICLES_<horodatage>.xlsx` ou
`…_STOCK_SOUS_ARTICLES_…`. `presentations` : feuille `TYPES_PRESENTATION` (Tranche, Code, Description, une colonne par
contenance) des tranches visibles, feuille `PROJETS` ; sans `trancheId`. `terrains` : feuille `STOCK_TERRAINS`
pré-remplie avec les terrains visibles (au-delà de 1 000, en-têtes seuls), nom `STOCK_TERRAINS_<horodatage>.xlsx` ; sans
`trancheId`. `previsions` : feuille `PREVISIONS` pré-remplie avec les lignes de la tranche (`trancheId` obligatoire).
Réimporter le modèle tel quel donne des lignes INCHANGEE (prévisions : MISE_A_JOUR, les lignes étant toutes remplacées).

## importer

Ligne 1 = en-têtes, puis une ligne de données par objet ; le **pivot** choisit création ou mise à jour (articles : code
dans la tranche ; terrains : code du terrain dans l'entité de la ligne ; présentations : tranche + code). Colonne
absente ou cellule vide = champ intact (l'import ne vide jamais un champ, sauf une contenance de présentation à 0 ou
vide, retirée). Nombres en cellule numérique ou en texte (`1 234,56`, `1234.56`) ; dates en cellule date ou
`jj/mm/aaaa` ; cases `Oui/Non`, `Vrai/Faux`, `1/0`, `X`. TVA ou % de concrétisation supérieur à 1 lu en pourcentage
(÷ 100, avertissement). Référentiels par code **ou** libellé (casse ignorée) sauf Orientation (code seul) et, pour les
articles, Vocation, Type, Type présentation, Entité de gestion du syndic (code seul). Entité inactive = introuvable.
En-tête inconnu = avertissement « colonne ignorée ».

`previsions` remplace toutes les lignes de la tranche en un seul appel (règles de `PUT /api/tranches/{id}/previsions-base`) :
une ligne de bilan globale (pivot = code de la tranche) plus les erreurs de lecture par ligne ; l'en-tête existant est
gardé, les colonnes facultatives de la première ligne le surchargent ; `Nombre d'articles` absent = valeur de la
tranche si > 0, sinon stock de la tranche (avertissement).

Une seule transaction : la moindre ligne en ERREUR, ou `simulation=true`, annule tout (`ecrit=false`, aucune trace
d'audit). Les messages d'erreur métier sont ceux des écrans.

Refus du fichier entier (400) : `trancheId` absent pour articles, sous-articles ou prévisions (« Vous devez sélectionner
une Tranche ! »), format `.xls` (« enregistrez le fichier au format .xlsx »), contenu non ZIP, archive corrompue ou
piégée (bombe, entrée géante, DTD), plus de 1 000 lignes de données, aucune feuille, feuille demandée absente, aucune
donnée, en-tête en double.
