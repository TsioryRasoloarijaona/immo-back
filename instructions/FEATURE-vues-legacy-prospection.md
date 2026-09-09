# FEATURE — Vues legacy : enrichissement de la fiche prospection et des interactions

> Lot 1 du refactor « exploiter les vues du legataire ». Objectif : combler l'écart entre ce que
> nos `GET` renvoient et ce que le logiciel de bureau affiche, en s'appuyant sur les vues que
> `yesRems` calcule déjà, plutôt qu'en réimplémentant ces calculs en Java.

## 1. Pourquoi ce lot précisément

`AdmFormsLog` journalise 311 923 ouvertures d'écran. Sur les 12 derniers mois :

| Écran legacy | Ouvertures | Utilisateurs |
|---|---:|---:|
| `Prospection.Prospection` (fiche) | **7 372** | 16 |
| `Commun.ActionMessage` (interactions) | **6 718** | 16 |
| `Commun.Tier` (fiche tiers) | 5 447 | 17 |
| `Prospection.PreProspection` (fiche) | 5 420 | 17 |
| `Sales.Sale` (fiche dossier) | 1 199 | 14 |
| `Prospection.ProspectionList` (liste) | 270 | 17 |
| `Cfg.TrancheList` / `ProjectList` (listes) | 6 / 6 | 2 |

Deux enseignements dirigent ce lot. **La fiche écrase la liste** — 7 372 contre 270, facteur 27 :
l'effort doit porter sur le détail. Et **les interactions sont le 2ᵉ écran du logiciel**, ce que
ni le backlog ni `from_front.md` ne laissaient deviner.

## 2. Ce que mesure la base, et ce qu'on en retient

Toutes les décisions ci-dessous viennent de `yesRemsProd` : 14 118 lignes dans
`ComSaleProspectionView`, 48 160 dans `ComActionMessageView`.

**Attention aux faux 100 %.** Cinq colonnes de `ComSaleProspectionView` sont non-`NULL` sur
100 % des lignes mais portent des chaînes vides. Hors chaînes vides :
`LastCommunicationResult` 90 % (et non 100), `LastCommunicationMessage` 90 %,
`LastActiveAlertReason` 25 %, `Delai` **0,8 %**. `InactivityTimeout`, lui, est réellement
peuplé à 100 %. Les taux du tableau §3 sont les taux corrigés.

## 3. `GET /api/prospections/{id}` — champs ajoutés à `ProspectionDetailDto`

Ajouts **purement additifs** : aucun champ existant n'est renommé, retypé ni retiré.

| Champ DTO | Colonne vue | Peuplé | Ce que c'est |
|---|---|---:|---|
| `joursInactivite` | `InactivityTimeout` | **100 %** | Jours écoulés depuis la dernière action. Le tri de relance du desktop. |
| `nbInteractions` | `ActionsMessageNumber` | **97 %** | Nombre d'interactions journalisées. |
| `derniereActionDate` | `LastActionDate` | **97 %** | Dernière action, tous types confondus. |
| `projetId` / `projetCode` / `projetLibelle` | `CfgProjectId/Code/Description` | **99 %** | Le projet. Nous n'exposions que la tranche. |
| `cin` | `CfgTierIDNumber` | ~~99 %~~ **2,7 %** | CIN du prospect. **Chiffre corrigé** — voir `INVENTAIRE-colonnes-prospection.md` §4 : la mesure initiale comptait les non-`NULL` d'une colonne remplie de chaînes vides. 387 lignes sur 14 118. |
| `derniereCommunicationDate` | `LastCommunicationDate` | **90 %** | |
| `derniereCommunicationResultat` | `LastCommunicationResult` | **90 %** | Ex. « Prospect n'est plus intéressé ». |
| `derniereCommunicationMessage` | `LastCommunicationMessage` | **90 %** | Le texte saisi. |
| `alerteActiveDate` | `LastActiveAlertDate` | 25 % | Relance en cours. `null` si aucune. |
| `alerteActiveRaison` | `LastActiveAlertReason` | 25 % | |
| `nbVisites` | `NbrVisit` | 20 % | |
| `premiereVisiteDate` | `FirstVisit` | 20 % | |
| `derniereVisiteDate` | `LastVisitDate` | 20 % | |
| `dossierId` / `dossierCode` | `ComFolderId` / `ComFolderCode` | 2 % | **Le lien de conversion prospection → dossier**, aujourd'hui absent. 2 % est le taux réel de conversion (358 dossiers pour 14 118 prospections), pas un défaut de peuplement. |
| `clientId` | `CfgCustomerId` | 2 % | Idem. |
| `statutDossier` | `Status` | 2 % | Statut du dossier issu de la prospection. |

**Écartés parce que mesurés vides** — ne pas les remettre sans nouvelle mesure :
`Categories`, `Types`, `Cities` (**0 %**, malgré leur nom prometteur) ; `ComUtilizationId`,
`ComBudgetId`, `ComPaymentMethodId`, `CfgIntermediateId` (0 %) ; `MapLatitude`/`MapLongitude`
(0 %) ; `ThereIsInstructionNotRead` (0 %) ; `Delai` (0,8 %).

### Règle : la pré-prospection n'est pas dans la vue

`ComSaleProspectionView` filtre `StrEntityId <> 'B92DF3D8-…'` (l'entité `ComPreProspection`) ;
les 3 817 pré-prospections en sont **exclues par construction** et ont leur propre vue,
`ComSalePreProspectionView`. Conséquence assumée pour ce lot : sur une pré-prospection,
`GET /api/prospections/{id}` renvoie les champs existants comme aujourd'hui et **tous les champs
ajoutés ci-dessus à `null`**. Ce n'est pas une erreur et l'endpoint ne doit pas renvoyer 404 pour
autant. `ComSalePreProspectionView` est le lot suivant.

## 4. Interactions — champs ajoutés à `InteractionDto`, **sans passer par la vue**

`InteractionDto` sert `GET /api/prospections/{id}/interactions` et les listes embarquées par
`GET /api/hotline/recherche`. Ajouts additifs :

| Champ DTO | Source | Peuplé | Ce que c'est |
|---|---|---:|---|
| `categorieId` / `categorie` | `ComActionMessage.comActionMessageCategory` | **100 %** | Discriminant : `Prospection` 46 666, `Dossier de vente (Réservation)` 1 325, `(Livraison)` 167, `Réclamation` 2. |
| `projetId` / `projetCode` / `projetLibelle` | tranche → `cfgProject` | **96 %** | |
| `trancheId` / `trancheCode` / `trancheLibelle` | voir la règle ci-dessous | **96 %** | Le contexte projet/tranche, aujourd'hui totalement absent du journal. |
| `sousCategorieId` / `sousCategorie` | `comActionMessageSubCategory` | 92 % | |

### `ComActionMessageView` a été évaluée puis écartée

Elle n'a **aucune sélectivité** : `SELECT *` sur **une seule ligne** par sa clé primaire prend
**30 070 ms**, `TOP 20` en prend 22 997. La cause est `ActionRank`, un `COUNT` corrélé sur les
interactions ultérieures de même type. Restreindre la projection aux 14 colonnes utiles ramène
la ligne unique à 45 ms et le lot de 20 à 337 ms — viable, mais toujours 337 ms pour un contexte
que les entités donnent gratuitement.

Le tout est donc dérivé des entités déjà mappées, en reprenant **la règle de la vue elle-même** :

```
tranche = (catégorie == « Prospection »)  ?  comProspection.stkHierarchy
                                          :  stkItem.stkHierarchy
projet  = tranche.cfgProject
```

Reportés faute de rentabilité : `ActionRank` (le `COUNT` corrélé, valeur marginale) et
`AlertActive` (9 %, exigerait de mapper `AdmAlertData` ; `InteractionDto.dateAlerte` couvre déjà
l'essentiel). Écartés comme trop rares : `StkItemId` (8 %), `ComFolderId` (3 %).

## 5. Comment c'est branché

**Entités JPA `@Immutable`, en lecture seule, une par vue** :
`entities/com/ComSaleProspectionView.java`, `entities/com/ComActionMessageView.java`. Le nom de
classe et le `@Table` reprennent exactement le nom de l'objet SQL, comme toutes les entités du
dépôt. Aucun `@Setter`, aucune écriture : ces objets ne sont jamais persistés.

La règle « ne jamais mapper une colonne de vue comme une colonne de table » reste entière — elle
interdit de greffer une colonne calculée sur `ComProspection` ; elle n'interdit pas de mapper une
vue *en tant que vue*, ce que fait ce lot.

**Le service enrichit, il ne remplace pas.** `ProspectionService.findById` continue de construire
le DTO depuis `ComProspection`, puis complète avec la ligne de vue si elle existe. Une requête de
plus, mesurée à 26 ms.

### Interdit : `SELECT *` sur ces vues en liste

Mesures sur `ComSaleProspectionView`, connexion chaude :

| | toutes colonnes | 8 colonnes |
|---|---:|---:|
| détail, 1 ligne | 26 ms | 3 ms |
| page 1 (20 lignes) | 121 ms | 38 ms |
| `OFFSET 500` | 1 176 ms | 82 ms |
| `OFFSET 5000` | **5 942 ms** | **123 ms** |

SQL Server élague les sous-requêtes corrélées non sélectionnées. Sur du **détail** (une ligne),
charger la vue entière coûte 26 ms et ne pose aucun problème — c'est ce que fait ce lot. Sur une
**liste**, un `findAll()` nu sur l'entité de vue est le chemin à 5 942 ms : les listes restent
donc sur les `Specification` actuelles, et le mécanisme de projection par colonnes fera l'objet
du lot « listes ».

## 6. Schéma de test

`yesRemsCiCheck` contient 165 tables et **0 vue**. Sur les 43 objets dont dépend la vue, 41 y
sont déjà ; il manquait `AdmAlertData` et `AdmParamsInteger`.

Nouveau fichier **`ci/schema/050_views.sql`**, écrit à la main (comme `040`) : les deux tables,
le semis `DefaultAlertDelai`, puis `ComSaleProspectionView` copiée conforme depuis `yesRemsProd`.
Il doit être appliqué après `040`, et `.github/workflows/ci.yml` est étendu en conséquence.

Le semis n'est pas décoratif : la vue calcule
`DATEADD(day, (SELECT CodeValue FROM AdmParamsInteger WHERE Code='DefaultAlertDelai'), GETDATE())`.
Sans la ligne, la sous-requête rend `NULL`, `DATEADD` rend `NULL`, et `LastActiveAlertDate`
devient silencieusement `NULL` partout — un test vert pour une mauvaise raison.

Les deux tables ne vont pas dans `010_tables.sql` : ce fichier est **généré** depuis la fermeture
transitive des clés étrangères des tables mappées, et ces deux-là n'y entrent que parce qu'une
vue les lit. Dans un fichier généré, la prochaine régénération les effacerait.

## 7. Impact front

Tout est **additif** — aucun champ existant n'est renommé, retypé ni retiré, aucun endpoint
re-scopé. Une intégration qui ignore les nouveaux champs continue de fonctionner à l'identique.

Deux points à relayer au dépôt frontend, qui ne sont pas des ruptures mais des pièges d'affichage :

1. **Sur une pré-prospection, tous les champs du §3 valent `null`** (voir la règle §3). Un écran
   qui afficherait « 0 interaction » au lieu de « non disponible » mentirait sur 3 817 fiches.
2. **`dossierId`/`clientId`/`statutDossier` sont `null` sur 98 % des prospections** — c'est le
   taux de conversion réel, pas une donnée manquante.

## 8. Défaut découvert en chemin : `StrEntityId` jamais renseigné

En montant les tests, un blocage a révélé un défaut plus large que ce lot.

`ComSaleProspectionView` fait un **`INNER JOIN StrEntity`**. Or `ComProspection.StrEntityId`
n'était mappé par aucune entité, et `ProspectionService.creerProspection` ne le renseigne donc
pas. Conséquence : **une prospection créée par notre API n'apparaît dans aucune vue du légataire**
— donc dans **aucun écran du logiciel de bureau**, que l'équipe commerciale utilise toujours.

Mesuré sur `yesRemsProd` : 4 lignes sur 17 939 ont `StrEntityId IS NULL`, et **aucune n'est
visible** dans `ComSaleProspectionView` ni dans `ComSalePreProspectionView`. Leur provenance :

| Code | Créée par | Quand |
|---|---|---|
| *(vide)* ×2 | `n8n_api` | 27/07/2026 |
| `26/05949` | *(aucun `UserCreate`)* — signature de notre API | 09/09/2026 |
| `PSN-TEST-TSIORY` | `script-test` | 08/09/2026 |

Le volume est faible **parce que l'API crée peu de prospections aujourd'hui** ; le taux, lui, est
de 100 % de ce qu'elle crée. Ce défaut recoupe le bug #41 déjà consigné (`creerProspection` ne
pose ni `UserCreate` ni `UserCreateDateTime`) : même méthode, mêmes colonnes de rattachement
oubliées.

**Ce lot ne corrige pas le défaut** — il se contente de le rendre visible et testable :

- `ComProspection.strEntityId` est désormais **mappé** (UUID brut, comme
  `StrStatus.StrConfigEntityId` — `StrEntity` n'a pas d'entité JPA dans ce dépôt) ;
- un test, `prospectionSansEntiteStructurelle_nAAucunBlocActivite`, **fige le comportement
  actuel** et devra être inversé le jour où `creerProspection` renseignera le champ.

Corriger revient à poser `strEntityId = 'E7C73F70-1111-40D9-AA78-A2825C42B601'`
(« Prospection vente ») à la création. C'est une ligne, mais **elle change ce que voient les
utilisateurs du logiciel de bureau** : les prospections créées par l'API apparaîtraient dans
leurs écrans. C'est une décision métier, pas technique — d'où une branche séparée.

## 9. Ce que ce lot ne fait pas

- **Les listes** — `GET /api/prospections` n'est pas touché (voir §5).
- **La pré-prospection** — `ComSalePreProspectionView` (70 colonnes) reste à faire.
- **Le mécanisme `?colonnes=`** — reporté au lot « listes », où il est structurant.
- **Le découpage pré-prospection / prospection** : notre critère
  (`CfgProspectId IS NULL AND CfgCommercialId IS NULL`) classe **830 fiches** en prospection que
  le legacy classe en pré-prospection (`StrEntity.Code = 'ComPreProspection'`). L'écart est
  unidirectionnel et mesuré ; il fait l'objet d'une branche séparée, parce qu'il **change le
  contenu de deux listes existantes** là où ce lot-ci n'ajoute que des champs.
