# EtudeFaisabiliteController

Écran OMRAN « ETUDES DE FAISABILITE » (`StkFeasibilityStudy`), lot P8a : fiche, bloc A (surfaces, coûts du terrain et de
construction), phases, unités de base, tranches, Bloquer / Débloquer. **Lecture réservée à Administrateur et Direction
Générale** (coûts, marges, prix d'achat), écriture à l'Administrateur ; aucune clé d'API n'y a accès (403 `HORS_PORTEE`).
Une étude d'une société inactive est introuvable (404). Les valeurs calculées viennent toutes du serveur (`POST /calcul`
pour l'aperçu) : le front ne réimplémente aucune formule. Le bilan (chiffre d'affaires, coûts globaux, marges) est en
lecture seule, tel que stocké par OMRAN.

## creer

`code` (unique → 409), `description`, `dateDocument`, `societeId` (société active, sinon 400) obligatoires ;
`terrainId` : article de nature TER (sinon 400). Statut initial « En étude », une ligne d'historique au nom de
l'appelant ; `tauxTvaDefaut` 0,20 et `tauxTvaInterets` 0,10 si absents. Bloc A et phases calculés et stockés ;
`unites` / `tranches` du corps sont ignorés (grilles par les PUT).

## modifier

Absent ou null = inchangé ; `champsAVider` vide `terrainId`, `debutAchatTerrain`, un nombre (→ 0) ou un texte (→ "").
Pour chaque paire coefficient ↔ montant, le montant envoyé seul recalcule le coefficient ; sans l'un ni l'autre, le
coefficient stocké est repris (changer la superficie recalcule donc les montants). Verif s'exécute sur les grilles
enregistrées (400 avec le message OMRAN, rien n'est écrit). `previsionsARecalculer` est vrai si l'étude a des prévisions.

## calculer

Même corps que la création, sans contrôle de champ obligatoire et sans écriture. `unites` et `tranches` facultatifs : Verif
ne contrôle que les grilles reçues ; une ventilation désigne sa ligne d'unité par `uniteEtudeId` = `id` de la ligne (PKey
ou identifiant provisoire choisi par le front). Les messages Verif sont renvoyés dans `erreurs`, dans l'ordre OMRAN.
`CountryFormule` (`AdmParamsString`, `MA` par défaut) choisit les formules marocaines de l'emprise et du plancher couvert.

## remplacerUnites

Liste complète : `id` connu = mise à jour, sans `id` = création, absente = suppression (avec ses ventilations) ; `id`
d'une autre étude ou `uniteBaseId` inconnu → 400. Verif ensuite : sommes des surfaces par fonction spécifique, puis, si
l'étude a des tranches, ventilation de chaque unité (une unité ajoutée non ventilée est refusée).

## remplacerTranches

Liste complète comme les unités. `code` et `sorting` obligatoires, `code` unique dans la liste (409) ; `trancheId` :
tranche visible de la société de l'étude (400). `unites` de chaque tranche : ventilations recréées, `uniteEtudeId` = ligne
d'unité de cette étude (400). Verif : somme des poids exactement 1, puis quantité ventilée de chaque unité (arrondi à 2
décimales).

## supprimer

409 si l'étude est bloquée, ou porte des prévisions (`FSPrevision`) ou des financements (`FSFunding`). Sinon ventilations,
tranches, unités et historique sont supprimés avec elle, dans la transaction.
