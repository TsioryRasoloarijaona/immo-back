# FEATURE — Conformité au légataire : écritures manquantes sur l'interaction et la vente

Lot issu de la **trace d'audit SQL Server** du logiciel de bureau
(`sys.fn_get_audit_file('C:\backup\*.sqlaudit', …)`), qui a permis de comparer colonne par colonne
ce qu'Omran écrit à ce que nos endpoints écrivent.

**La base auditée est `yesRems`**, pas `yesRemsProd` — deux restaurations différentes du même
schéma (387 dossiers contre 358, 17 678 prospections contre 17 939). Toutes les mesures de ce
document sont prises sur `yesRems`, celle qu'Omran utilise réellement.

---

## 1. Le défaut prouvé : `IsSystem` n'était pas posé

`POST /api/prospections/{id}/interactions` ne posait **aucun** des quatre drapeaux du légataire,
ni `CfgTierId`.

`ComSaleProspectionView` calcule :

```sql
LastCommunicationDate   = MAX(DocDate) WHERE … AND IsSystem = 0
LastCommunicationResult = TOP 1 …      WHERE … AND IsSystem = 0
```

En SQL, `NULL` ne satisfait pas `= 0`. **Nos interactions étaient donc écartées du « dernier
échange »** — dans l'écran d'Omran comme dans notre propre liste, qui lit ces colonnes depuis la vue.

| | `yesRems` |
|---|---:|
| `IsSystem = 0` — interactions humaines | 43 540 |
| `IsSystem = 1` — alertes du légataire | 4 431 |
| **`IsSystem NULL`** — les nôtres | **14**, sur 8 prospections |

Les 14 sont identifiables sans ambiguïté : `UserCreate` nul et note écrite dans `Description` au
lieu d'`AlertMessage` (l'inversion corrigée par `FIX-interaction-note.md`). L'une s'intitule
*« Test avec vrais référentiels backend. »*.

**Posé désormais** : `IsSystem=0`, `IsNotRead=0`, `IsUnpaid=0`, `WithoutRevival=0`, et `CfgTierId`
= le prospect (renseigné sur 96,5 % des interactions humaines du légataire).

**Rattrapage des lignes déjà écrites** : `db/migration/007_ComActionMessage_drapeaux_api.sql`. Il ne
touche ni `Description` ni `AlertMessage` — réinverser la note a posteriori demanderait de
distinguer l'avant et l'après correctif, et une erreur y serait une perte de texte saisi.

---

## 2. La vente ferme la prospection

`POST /api/dossiers` ne fermait pas la prospection transformée. Elle restait **active dans le
pipeline** : visible dans `GET /api/prospections`, comptée dans les tableaux de bord, relances
armées — un commercial relancé sur une affaire déjà signée.

| | |
|---|---:|
| dossiers liés à une prospection | 355 |
| dont prospection **fermée** | **354** (99,7 %) |
| avec `ClosingDate` | 354 / 354 |
| avec un **motif** de clôture | 20 (5,6 %) |

La seule exception ouverte est un dossier de 2022 saisi à la main.

**Aucun motif n'est posé**, et c'est délibéré : l'exiger serait plus strict que le légataire, en
inventer un par défaut fabriquerait une statistique fausse.

**Désarmement des relances** — reprend mot pour mot le prédicat relevé dans la trace :

```sql
UPDATE ComActionMessage SET WithoutRevival = 1
WHERE AlertValidityDate IS NOT NULL AND ComDocumentId IS NULL AND ComProspectionId = …
```

Les deux conditions comptent. `ComDocumentId IS NULL` épargne les alertes rattachées à une
**échéance** : celles du dossier qui vient de naître doivent rester armées, ce sont les rappels de
paiement.

### Le piège rencontré

`@Modifying(clearAutomatically = true)` vide le contexte de persistance et **détache le dossier à
peine créé** ; `create` construit sa réponse juste après en déréférençant ses associations
paresseuses — la sérialisation casse. Trois tests de création sont tombés dessus.
`flushAutomatically` suffit et ne détache rien.

---

## 3. L'historique dit enfin *qui*

Deux tables, deux corrections.

**`StrFolderStatusHistory`** était écrite mais amputée : ni l'auteur ni le client. Le légataire les
renseigne sur **1 282 de ses 1 308 lignes (98 %)**. Un historique qui ne dit pas qui a changé le
statut ne sert qu'à moitié.

**`StrHistory`** — nous la *lisions* déjà pour l'onglet « Historique » sans jamais l'écrire : nos
dossiers n'y figuraient pas du tout. Elle porte 1 065 lignes rattachées à un dossier en base réelle,
`AdmUserId` renseigné à 99,6 %.

`StrWorkflowId` reste nul : nous ne modélisons pas les workflows du légataire, et inventer un
identifiant produirait une ligne que son écran ne saurait pas interpréter. Les 21 % de ses propres
lignes sans workflow montrent que c'est un cas admis.

---

## 4. Ce qui ne sera PAS ajouté, et pourquoi

La comparaison `GET /api/prospections/{id}` contre `ComProspection` donnait 17 colonnes absentes,
dont la géolocalisation annoncée à 79 %. **Mesure faite, il n'y a rien à servir.**

| Colonne | Non nulles | Réellement utiles |
|---|---:|---:|
| `MapLatitude` | 14 033 | **0** (toutes à zéro) |
| `MapLongitude` | 14 033 | **0** |
| `MapRadius` | 14 033 | 2 350 — un rayon sans centre |
| `TempField2` | — | 1 022 (5,8 %) — des villes, que `ComProspectionCity` porte déjà |
| `TempField3` | — | 38 |
| `TempField4` | — | 16 |
| `TempField5/8/9`, `AlertMessage`, `AlertValidityDate`, `CfgIntermediateId`, `ComBudgetId`, `ComPaymentMethodId`, `ComUtilizationId` | — | **0** |

**La leçon, pour la troisième fois dans ce chantier** : `COUNT(colonne)` compte les non-nuls, pas
les valeurs utiles. `PenalityDescription` était non nulle et vide partout ; `MapLatitude` est non
nulle et à zéro partout. **Compter les distinctes, ou les non-zéro, avant de conclure.**

---

## 5. Reste à faire

**Les alertes de jalon du dossier.** Omran crée à la vente 4 `ComActionMessage` + 4 `AdmAlertData`,
identifiés par la trace du 11/09 comme des rappels signés « Système » :

```
« l'état RESERVATION du dossier de vente… »         relance +7 j
« FRAIS SYNDIC (16 800,00 DH) arrivé à échéance »   relance immédiate
```

C'est le plus gros des lots restants : il faut décider quels jalons produisent une alerte, à quelle
échéance, et avec quel libellé — trois questions produit, pas techniques.

**Le diff sémantique de `ComSaleProspectionView`** (148 colonnes contre nos 56). À faire
proprement, en résolvant chaque colonne de la vue vers son origine réelle : le précédent
`H1/H2/H3` sur les dossiers a montré qu'un comptage brut surestime massivement — trois des quatre
colonnes « manquantes » étaient déjà servies sous d'autres noms.

---

## 6. Impact front

**Aucune rupture de contrat.** Les trois corrections sont des écritures serveur ; aucune réponse
d'API ne change de forme.

Un changement de **comportement** en revanche, et il est voulu : après `POST /api/dossiers`, la
prospection transformée **disparaît de `GET /api/prospections`** (elle est fermée). Un écran qui
la cherchait encore après la vente ne la trouvera plus — c'est le comportement du logiciel de
bureau, et l'ancien était le défaut.

## §2bis — Texte de clôture « Création dossier de vente » (08/10/2026)

Mesure `yesRemsProd` : 374 prospections liées à un dossier, 359 archivées ; OMRAN écrit
`ClosingReasonText = "Création dossier de vente"` sur 356/359 (aucun motif codé « vendu » dans
`ComProspectionClosingReason`), immo-crm n'écrivait rien. `cloturerLaProspection` écrit désormais
ce texte (`DossierService.TEXTE_CLOTURE_VENTE`) ; les alertes restent éteintes par
`AlerteRelanceService.desarmerToutes`, comme à l'archivage. Les 14 prospections immo-crm restées
ouvertes avant le 11/09/2026 (commit 044c4c0) ne sont pas rattrapées (décision utilisateur).
