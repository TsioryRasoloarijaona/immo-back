# FEATURE — KPI de performance des commerciaux

> Spécifié le 08/09/2026. Porte sur `immo-crm` uniquement. Toutes les mesures citées viennent de
> `yesRemsProd`, restreintes aux entités juridiques en exploitation sauf mention contraire.

## 1. Objectif

Deux endpoints : un commercial voit ses propres chiffres, un administrateur voit ceux de tous.
Au-delà des quatre compteurs demandés (appels, prospections, visites, dossiers), la spec ajoute la
**réactivité**, la **transformation** et la **valeur** — sans elles le classement récompense le
volume d'appels et non la vente : sur 2025-2026, un commercial fait 1 460 prospections pour
7 dossiers quand un autre en fait 246 pour 12.

## 2. Endpoints

```
GET /api/kpi/commerciaux/moi?du=2026-08-09&au=2026-09-08   → un objet
GET /api/kpi/commerciaux?du=…&au=…                          → une liste (Administrateur)
```

`du` / `au` sont des dates (`yyyy-MM-dd`) optionnelles ; défaut = **30 derniers jours**. `du` est
inclusive à 00:00, `au` inclusive jusqu'à 23:59:59.999 — sans quoi un `au` valant aujourd'hui
exclurait toute la journée en cours. `du` postérieure à `au` ⇒ 400.

| Code | Quand |
|---|---|
| 200 | Résultat, éventuellement à zéro (voir §6). |
| 400 | `du` > `au`, ou date non parsable. |
| 403 | `GET /api/kpi/commerciaux` sans le rôle `Administrateur`. |
| 404 | `/moi` : le compte connecté n'est rattaché à aucun employé (`AdmUser.CfgEmployeeId` nul — 16 comptes sur 75). |

## 3. L'attribution — la décision structurante

Deux clés d'attribution coexistent et **ne désignent pas la même chose**.

**Les interactions s'attribuent à leur auteur : `ComActionMessage.AdmUserId`, puis
`AdmUser.CfgEmployeeId` pour remonter au commercial.** Ce n'est pas un choix de confort, c'est le
seul chemin correct, et il a été vérifié :

- `AdmUserId` est renseigné sur **48 118/48 118** interactions ; `ComActionMessage.CfgEmployeeId`
  seulement sur 37 186 (77 %). Sur les interactions récentes des entités actives, l'auteur se
  résout **12 799 fois sur 12 859 (99,5 %)** par le compte, contre 9 404 (73 %) par la colonne
  employé.
- Surtout, **les deux divergent** : là où les deux sont renseignés, elles désignent une personne
  différente **844 fois**. Le départage vient de `UserCreate`, la trace textuelle laissée par
  l'application Windows : elle vaut le compte **37 186/37 186 (100 %)** et l'employé seulement
  30 894/37 186 (83 %). `ComActionMessage.CfgEmployeeId` n'est donc **pas** l'auteur — c'est
  l'employé *concerné*. L'utiliser attribuerait à des commerciaux des appels qu'ils n'ont pas
  passés.

**Les prospections et dossiers s'attribuent au commercial assigné** (`CfgCommercialId`), qui est
la notion de portefeuille et n'a pas d'équivalent côté compte.

**La clé de regroupement du DTO est donc le commercial (`CfgTier`), jamais le compte.** Un même
employé peut porter **deux comptes** (mesuré : 1 cas) ; regrouper par compte scinderait ses
chiffres en deux lignes. Le sens de résolution compte-→employé est toujours N→1, donc sûr.

**Angle mort assumé** : 80 interactions sur 48 118 (0,17 %) ont un auteur sans employé rattaché.
Elles ne sont attribuables à aucun commercial et sont exclues de tous les compteurs.

## 4. Les indicateurs

`ComActionMessageType.Code` et `ComActionMessageSubCategory.Code` sont les discriminants. Valeurs
exactes vérifiées en base (encodage compris), volumes toutes entités confondues :

| Champ DTO | Source | Volume base |
|---|---|---:|
| `appelsPasses` | type `Téléphone Sortant` | 40 325 |
| `appelsRecus` | type `Téléphone entrant` | 867 |
| `visitesRealisees` | type `Visite` | 4 311 |
| `visitesDecrochees` | résultat `Le prospect veut faire une visite du projet` | 5 715 |
| `rdvObtenus` | résultat `Rendez vous pris` | 2 444 |
| `interactionsTotal` | toutes interactions de la période | — |
| `prospectionsTraitees` | prospections **distinctes** sur lesquelles il a saisi ≥ 1 interaction | — |

« Visite décrochée » et « visite réalisée » sont deux choses distinctes et les deux sont exposées :
la première est le résultat d'un appel (l'engagement obtenu), la seconde une visite effectivement
consignée. Les confondre ferait passer un commercial qui décroche beaucoup mais ne concrétise pas
pour un excellent élément.

Portefeuille (attribution `CfgCommercialId`) :

| Champ DTO | Source |
|---|---|
| `prospectionsAssignees` | `ComProspection.CfgCommercialId`, `DocDate` dans la période |
| `prospectionsCloturees` | idem, `IsClosed = 1` |
| `prospectionsPrisesEnCharge` | assignées ayant reçu ≥ 1 interaction, quel qu'en soit l'auteur |
| `tauxPriseEnCharge` | prises en charge / assignées, `null` si aucune assignée |
| `delaiMedianPremiereReponseHeures` | médiane de `MIN(interaction.DocDate) − prospection.DocDate` |
| `dossiersCrees` | `ComFolder.CfgCommercialId`, `DocDate` dans la période |
| `montantTotal`, `panierMoyen` | `ComFolder.TotalToPay` |
| `tauxAppelVersRdv` | `rdvObtenus / appelsPasses` |
| `tauxProspectionVersDossier` | `dossiersCrees / prospectionsAssignees` |

**La réactivité est l'indicateur qui sépare le plus.** Sur les 6 533 prospections reçues depuis
2025 sur les entités actives : **2 117 n'ont jamais été contactées (32 %)**, 2 815 l'ont été sous
24 h, délai moyen 109 h.

**Chaque taux est accompagné de son effectif brut**, et vaut `null` — jamais `0` — quand le
dénominateur est nul. Avec 36 dossiers créés depuis 2025 toutes personnes confondues, un
pourcentage sans effectif serait une invitation à conclure du bruit.

**Écarté : `ComFolder.ReservationDate`**, renseignée sur **2 dossiers sur 358**. Le seul jalon de
signature réellement peuplé est `AgreementDate` (73/358) ; il n'est pas exposé ici, faute d'usage
demandé, mais c'est lui qu'il faudra prendre si un KPI « vente signée » est ajouté — surtout pas
`ReservationDate`.

## 5. Visibilité — le point de fuite

**`/api/kpi/**` n'est pas une collection scopée** : `PerimetreInterceptor` ne la couvre pas (il ne
connaît que `dossiers`, `prospections`, `pre-prospections`, `encaissements`). Le périmètre
commercial **et** la visibilité par entité juridique doivent donc être appliqués **explicitement à
chaque agrégat**, exactement comme `DashboardService`, dont le javadoc appelle déjà cela « le point
de fuite le plus probable de la feature ».

- Filtre d'entité : `(soc.isActive = true OR soc.id IS NULL)` sur chaque requête, R2 comprise
  (ligne sans société résoluble ⇒ comptée).
- Périmètre : variante `…DansPerimetre` par agrégat, selon la convention déjà en place.
- Pour une interaction, la société se résout **prospection d'abord, dossier ensuite**
  (`COALESCE`), même ordre que `VisibiliteEntites.depuisDeuxProjets` — sinon l'agenda, la hotline
  et les KPI désigneraient trois sociétés différentes pour la même ligne.

`GET /api/kpi/commerciaux/moi` reste soumis au filtre : un commercial restreint ne voit que ses
propres chiffres **dans son périmètre**, ce qui peut être inférieur à son activité réelle. C'est
volontaire et cohérent avec le reste de l'application.

## 6. Comptes et lignes vides

- La liste admin ne renvoie **que les commerciaux ayant au moins une ligne** dans la période :
  interaction, prospection assignée ou dossier. Sur 2025-2026 cela fait 23 personnes, à comparer
  aux 75 comptes existants dont **60 sont désactivés**.
- `/moi` renvoie **200 avec tous les compteurs à zéro** quand le commercial n'a rien fait sur la
  période — jamais 404. Un écran vide est une information ; une erreur n'en est pas une.

## 7. Surface à modifier

| Fichier | Nature |
|---|---|
| `entities/com/ComActionMessage.java` | **mapper `AdmUserId`** (`@ManyToOne` vers `AdmUser`) — colonne existante, jusqu'ici non mappée |
| `domain/InteractionKpi.java` | les codes de type/résultat, avec leurs volumes mesurés |
| `dto/kpi/PerformanceCommercialDto.java` | le DTO |
| `repositories/com/ComActionMessageRepository.java` | agrégat interactions ± périmètre |
| `repositories/com/ComProspectionRepository.java` | projection prospections ± périmètre |
| `repositories/com/ComFolderRepository.java` | projection dossiers ± périmètre |
| `services/kpi/PerformanceCommercialService.java` | assemblage, médiane, taux |
| `controller/kpi/PerformanceCommercialController.java` | 2 routes + springdoc |

## 8. Tests attendus

- Les 4 compteurs demandés, sur un jeu d'interactions typées.
- **Un appel saisi par un chef sur la prospection d'un autre est compté chez le chef**, pas chez
  le commercial assigné — c'est le cœur du §3.
- Un commercial portant **deux comptes** voit ses interactions additionnées sur une seule ligne.
- Les bornes de période : une interaction du jour de `au` est incluse.
- Taux `null` quand le dénominateur est nul, jamais `0`.
- **Visibilité** : une interaction rattachée à une entité inactive n'est comptée nulle part ; un
  compte restreint par un périmètre ne voit pas les chiffres hors périmètre.
- `GET /api/kpi/commerciaux` sans le rôle `Administrateur` ⇒ 403.

## 9. Impact front

**Purement additif** — deux routes nouvelles, aucune réponse existante modifiée.

- Les taux sont des **`BigDecimal` entre 0 et 1, ou `null`** — jamais des pourcentages entiers. Le
  front formate ; `null` doit s'afficher « — », pas « 0 % ».
- `delaiMedianPremiereReponseHeures` est un entier d'heures, `null` si aucune prospection prise en
  charge.
- La liste admin est triée par `dossiersCrees` décroissant puis `appelsPasses` décroissant, et
  **omet les commerciaux sans activité** : le front ne doit pas supposer qu'elle couvre tous les
  commerciaux de `GET /api/commerciaux`.
