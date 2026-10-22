# PrevisionFaisabiliteController

Onglets « Bilan », « Analyse » et « Données » d'une étude. Le bilan est l'arbre des catégories de trésorerie
(`StlCategory`) que l'utilisateur renseigne (`FSPrevision`) ; ses totaux donnent le coût global, le chiffre d'affaires,
les marges et les retours de l'étude. Lecture et aperçus Administrateur + Direction Générale, écriture Administrateur ;
aucune clé d'API (403). Étude inconnue ou de société inactive → 404.

## lire

`lignes` : l'arbre à plat, parent avant enfants, frères par `Sorting` puis libellé. Montant d'une ligne par unité =
quantité de la ligne d'unité × ratio ; en pourcentage = total HT (enfants compris) de la catégorie de référence × ratio ;
TTC = HT × (1 + TVA), sauf quote-part du terrain (case de l'étude, unité surface à catégorie d'article) : HT + (HT −
coût du terrain / plancher vendable × quantité) × TVA. Total d'un nœud = propre + enfants ; `poids` = total TTC / total
TTC de sa racine. Lignes liées aux relations « prix du terrain », « frais d'enregistrement », « autres frais » et
« intérêts du crédit » (si une catégorie y est configurée) : ajoutées par le serveur, montant imposé, `verrouillee`.
`totaux.terrainNonInclus` vrai quand la relation « prix du terrain » n'a pas de catégorie (cas de la production).

## remplacer

Liste complète des lignes saisies, clé = `categorieId` : absente = supprimée ; le serveur ajoute les ancêtres jusqu'aux
racines et les lignes de relation. Priorité ratio > montantHt > montant, les autres recalculés. 400 : catégorie absente,
inconnue ou « sans prévision » ; unité d'une autre étude ; unité et référence à la fois ; TVA hors [0;1] ; ratio ou
montant négatif ; ligne verrouillée saisie ; « Vous ne pouvez pas selectionnez une catégorie non définie dans le bilan! » ;
« Vous ne pouvez pas selectionnez cette catégorie possédant une relation hiérarchique avec la catégorie courante! » ;
« Vous ne pouvez pas selectionnez cette catégorie car elle crée une boucle infini! » ; écart-type > 1 pour une
répartition sur 3 ou 4 mois (message OMRAN). Tout est contrôlé avant d'écrire. Le recalcul réécrit les lignes par
tranche, les lignes de trésorerie et les colonnes du bilan de l'étude ; réponse = `lire`.

## calculer

Même corps et mêmes contrôles que le PUT, rien n'est écrit : arbre, totaux et bilan de l'aperçu.

## tresorerie

Lignes de trésorerie enregistrées par le dernier recalcul (montants > 0) : concrétisation et finalisation (crédit,
réparties par une loi normale sur leurs fenêtres en nombre d'articles), dépense d'exécution hors indivis et retenue de
garantie (débit), dépenses indivises à leur date, déblocages (crédit) et remboursements (débit). `mois` : chaque mois
du premier au dernier, `solde` = cumul des crédits − débits. Sans financement : déblocages et remboursements exclus.

## calculerTresorerie

Recalcule les lignes avec les écarts-types envoyés (absent = celui de l'étude), sans rien écrire. 400 : écart-type
< 0,5, ou > 1 pour une fenêtre de 3 ou 4 mois.
