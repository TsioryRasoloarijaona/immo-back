# FEATURE — Encaissements : conformité au filtre du légataire

**Source** : trace d'audit SQL Server du 11/09/2026, écran `Yes.Com.Front.Credit.SettlementList`
d'OMRAN Estate (module Vente → « Encaissements Vente »), observé à 17:20:29 heure locale.
**Base de mesure** : `yesRemsProd` sauf mention contraire. **Base tracée** : `yesRems`.

Ce document fait suite à `FEATURE-conformite-legataire.md` (lot J) et applique la même méthode :
partir de ce que le logiciel de bureau exécute réellement, pas d'une capture d'écran.

---

## 1. Ce que la trace contient, et ce qu'elle ne contient pas

La session tracée a ouvert trois écrans :

| Heure locale | `AdmFormsLog.NameSpace` |
|---|---|
| 17:19:58 | `Yes.Com.Front.Commission.CommissionList` |
| 17:20:16 | `Yes.Com.Front.ToDoList.ToDoList` |
| 17:20:29 | `Yes.Com.Front.Credit.SettlementList` |

**Aucune écriture métier** dans ce tronçon — uniquement `AdmWebHistorics` et `AdmFormsLog`.
Il n'y a donc **pas** de séquence « création d'encaissement » à reproduire ici, contrairement au
lot J qui exploitait la transformation prospection → vente. Ce que la trace apporte, c'est le
**filtre exact de la liste**.

Les deux autres écrans, pour mémoire :

- `CommissionList` n'a lancé que son sélecteur de tranches (`ComCommissionTrancheView`, filtré par
  `AdmUserHabilitation`) — aucune requête de liste : l'écran attend une sélection avant d'afficher
  quoi que ce soit. Nous n'avons pas d'équivalent.
- `ToDoList` lit `AdmToDoList` + `AdmToDoListLineState` filtrés sur l'utilisateur. Pas d'équivalent
  non plus. Hors périmètre de ce lot.

---

## 2. Le filtre réel de l'écran Encaissements

Requête relevée (une passe à 17:20:39 avec période, une seconde à 17:20:51 sans) :

```sql
SELECT StlSettlementView.*
FROM StlSettlementView
WHERE (1 = 1)
  AND CfgCompanyId IN (SELECT CfgCompanyId FROM AdmUserHabilitation
                       WHERE AdmUserId = '…')                          -- (a) société
  AND PKey IN (SELECT StlSettlementId FROM StlSettlementRelationView
               WHERE CfgTrancheId IN ('…ARS…', '…APF…'))               -- (b) tranche
  AND ( AdmModuleId = @P1                                              -- (c) module Vente
        OR PKey IN (SELECT StlSettlementId FROM StlSettlementRelationView
                    WHERE AdmModuleId = @P2) )
  AND IsCredit = 1                                                     -- (d) encaissement
  AND DocDate >= @P3 AND DocDate < @P4                                 -- (e) période
```

`@P1 = @P2 = 00002C2D-BDFA-4541-94F7-C708A0E00001` (`AdmModule.Code = 'COM'`, « Vente »).

La seconde passe est identique moins la clause (e) : l'utilisateur a vidé la période sans toucher
aux tranches.

---

## 3. Écarts avec `GET /api/encaissements`, mesurés

Population de référence, sur `yesRemsProd` :

| Critère | Règlements |
|---|---:|
| Le nôtre — `EXISTS(StlSettlementLine WHERE ComDocumentId IS NOT NULL)` | 644 |
| Celui d'Omran — (c) ET (d) | 659 |
| Chez Omran, pas chez nous | **16** |
| Chez nous, pas chez Omran | **1** |

### 3.1 `IsCredit = 1` — clause (d) — **corrigé dans ce lot**

Notre `findAll` ne filtrait pas dessus. Une seule ligne réelle en profitait : un règlement de
**−470 000 DH**, `IsCredit = 0`, rattaché à COM, c'est-à-dire un **décaissement** affiché dans une
liste d'encaissements. Un seul enregistrement, mais à montant négatif : il fausse tout total.

Sur l'ensemble de `StlSettlement` : 663 lignes à `IsCredit = 1`, 2 580 à `0`, **0 NULL**. La
colonne est toujours renseignée, et `EcheancierService` pose `Boolean.TRUE` à la création — le
test strict `cb.isTrue(...)` n'escamote donc rien. Ce point mérite d'être écrit parce que c'est le
scénario exact du bug #8 du lot J (`IsSystem` non posé, vue filtrant `= 0`, `NULL` ne satisfaisant
pas l'égalité) : il a été vérifié des deux côtés avant d'ajouter le prédicat.

### 3.2 Filtre par tranche — clause (b) — **ajouté dans ce lot**

L'écran offre un sélecteur de tranches (celles de `AdmUserHabilitation` avec `IsForSale = 1` :
5 proposées à l'utilisateur tracé, **2 sélectionnées**, donc un choix délibéré et non une
présélection de tout). Notre API n'avait aucun paramètre équivalent.

Omran passe par `StlSettlementRelationView.CfgTrancheId`. Plutôt que de mapper cette vue en
`@Subselect`, le paramètre est implémenté par le chemin JPA déjà disponible :
`StlSettlementLine.comDocument.comFolder.stkItem.stkHierarchy` (`StkItem.StkHierarchyId` est une
`CfgTranche`). **Les deux chemins ont été comparés avant de trancher** :

| Couples (règlement, tranche) | Nombre |
|---|---:|
| Par le chemin des lignes | 644 |
| Par `StlSettlementRelationView` | 649 |
| Présents par ligne, absents de la vue | **0** |
| Présents dans la vue, absents par ligne | 5 |

Les 5 divergences portent `Origin = 'BuyDocument-ActExpenseView'` et `'RntDocument-RntFolder'`
(achat et location, tranche `YES`) — hors du périmètre Vente de cet endpoint. **Côté Vente les
deux chemins coïncident exactement**, d'où la décision de ne pas mapper la vue.

### 3.3 Rattachement au module — clause (c) — **non repris, décision produit** (voir §4)

### 3.4 Borne haute de période — clause (e) — **aucun changement, et c'est délibéré**

Omran écrit `DocDate < @P4`, nous écrivons `<= dateFin`. Ce n'est **pas** un écart de résultat mais
de convention : notre `dateFin` est documenté « borne supérieure **incluse** » avec l'exemple
`2026-08-31T23:59:59`, Omran passe le lendemain à minuit. Les deux expriment la même intention.

Basculer en borne exclusive romprait le contrat publié et ferait disparaître les règlements
estampillés exactement à la seconde de fin. **Ne pas « corriger » ce point** — il a déjà été
signalé une fois comme divergence avant vérification.

---

## 4. Les encaissements non affectés — décision produit ouverte

C'est le point structurant, et il n'est **pas** tranché par ce lot.

Nous scopons « Vente » par les lignes (`EXISTS` d'une ligne rattachée à un `ComDocument`) ; Omran
scope par le module (`AdmModuleId = COM`, directement ou via la vue de relations). L'écart n'est
pas cosmétique :

**16 règlements, 6 435 400 DH**, visibles chez Omran, invisibles chez nous. Tous ont **zéro ligne**,
tous sont à l'état `IN_WORK`, tous rattachés directement à COM. Ce sont les **encaissements non
affectés** : de l'argent reçu, enregistré, mais pas encore imputé à une échéance. La vue légataire
les reconnaît explicitement sous `Origin = 'StlSettlement-NonAffecte'`. Le plus récent date du
**28/08/2026** — ce n'est pas de l'historique mort, le flux est vivant.

Montants unitaires observés : 750 000, 1 238 000, 1 020 000, 756 000, 658 000 DH…

**Une nuance qui borne la portée de l'écart, vérifiée et non supposée** : ces 16 portent
`CfgTrancheId = NULL` dans la vue de relations. Dans le run tracé — deux tranches sélectionnées —
**Omran ne les affichait donc pas non plus**. Ils n'apparaissent que lorsque aucune tranche n'est
sélectionnée. L'écart reste réel, puisque nous les masquons *inconditionnellement*, mais il se joue
dans l'état « sans filtre de tranche » et non partout. La trace ne permet pas de dire si l'écran
s'ouvre tranches présélectionnées : sa première requête de liste en portait déjà deux, sur cinq
habilitées.

### Ce qu'il faudrait décider

1. **Sert-on les encaissements non affectés dans `GET /api/encaissements` ?** Si oui, le critère
   « Vente » passe du test sur les lignes à `AdmModuleId = COM` (ou la vue de relations), et le
   DTO doit accepter `dossierId`/`dossierCode` nuls — ce qu'il fait déjà, `toDto` les laissant à
   `null` quand aucune ligne ne résout de dossier.
2. **Ou bien en fait-on un écran séparé** (« à affecter »), ce que leur volume et leur nature de
   file d'attente suggèrent ?

Point favorable dans les deux cas, vérifié : ces 16 n'ayant aucune ligne,
`PerimetreCommercialService.filtreEncaissements()` les laisse passer par sa branche
`cb.not(cb.exists(avecProjet))`. Relâcher le critère « Vente » ne serait donc pas annulé par le
périmètre commercial.

### Accessoirement : le `OR` de la clause (c)

Sur nos 644 règlements, 641 sont `AdmModuleId = COM` en direct et **3** sont rattachés au module
`STL` (Trésorerie) tout en portant des lignes de vente. La branche `OR … relation view` d'Omran les
rattrape, et notre critère par les lignes aussi — les deux approches s'accordent sur ces 3.

---

## 5. Impact front

Aucune rupture de contrat : `trancheId` est un paramètre **optionnel** et son absence laisse le
comportement inchangé.

Un **changement de comportement** en revanche, voulu : un règlement à `IsCredit` faux ne remonte
plus. Concrètement, une seule pièce sur les données réelles — celle à −470 000 DH. Un écran qui
totalisait la liste verra son total augmenter de 470 000 DH, ce qui est la correction et non une
régression.

Le nouveau paramètre permet au front de reproduire le sélecteur de tranches du légataire :
`GET /api/encaissements?trancheId=<uuid>&trancheId=<uuid>`.

---

## 6. Reste à faire

- [ ] Trancher le §4 (encaissements non affectés) — **décision produit**.
- [ ] `CommissionList` : écran sans équivalent chez nous, alimenté par `ComCommissionTrancheView`.
- [ ] `ToDoList` : `AdmToDoList` + `AdmToDoListLineState`, sans équivalent.
- [ ] Le sélecteur de tranches légataire filtre sur `CfgTranche.IsForSale = 1` en plus de
      l'habilitation — à vérifier sur `GET /api/patrimoine/tranches`, hors périmètre de ce lot.
