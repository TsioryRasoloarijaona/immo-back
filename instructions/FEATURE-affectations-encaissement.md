# FEATURE — Fiche encaissement : les affectations

Enrichit `GET /api/encaissements/{id}` des **échéances que le règlement solde**, et corrige au
passage un 500 introduit par le lot précédent.

Source : la trace d'audit SQL Server d'OMRAN Estate (`C:\backup\*.sqlaudit`), événements du
14/09/2026 entre 11:05 et 11:23, plus des mesures sur `yesRemsProd`.

---

## 1. Ce que le légataire charge à l'ouverture d'une fiche règlement

Séquence relevée, dans l'ordre :

1. `SELECT * FROM StlSettlement WHERE PKey = @P1`
2. `SELECT PKey FROM StlSettlementLine WHERE StlSettlementId = … ORDER BY Amount ASC`, puis chaque
   ligne relue individuellement — **les affectations**
3. `SELECT PKey FROM StlDeduction WHERE StlSettlementId = …`
4. `SELECT * FROM StlSettlementDocumentToAttach WHERE StlSettlementId = …`
5. devise, type, `AdmModule`, `CfgCompany`, `CfgTier` (joint à `ActAccount`)
6. `SELECT * FROM StlSettlementDocumentListView WHERE PKey = @P1`
7. `StlSettlementHistory`, joint au type et à l'utilisateur, `ORDER BY DocDate`
8. **verrou pessimiste** : `SELECT COUNT(*) FROM AdmWebLockEntities … WHERE DataObjectId = <id>
   AND CODE = 'Settlement' AND State = 1`, puis `INSERT AdmWebLockEntities`, puis
   `UPDATE … SET State = 2` à la fermeture

---

## 2. Ce qui est repris, et sur quelle mesure

Population : les 643 règlements du périmètre Vente (`IsCredit = 1` + au moins une ligne
rattachée à une échéance de vente).

| Source légataire | Mesure | Décision |
|---|---|---|
| `StlSettlementLine` | 661 lignes, **max 3** par règlement | **repris** |
| `StlSettlementHistory` | 721 lignes, 643 règlements (100 %) | **écarté** — voir §3 |
| `StlDeduction` | **0 ligne** | écarté |
| `StlSettlementDocumentToAttach` | **0 ligne** | écarté |
| `AdmWebLockEntities` | 13 733 lignes | hors périmètre, reste au backlog |

Les affectations sont l'apport : elles répondent à « ce règlement, il a payé quoi ? », question
que la forme de liste ne sait pas traiter.

---

## 3. L'historique : 100 % de couverture, 10 % d'information

`StlSettlementHistory` couvre la totalité des règlements — le chiffre qui donne envie de conclure
« colonne essentielle ». En comptant les lignes par règlement :

- **65 règlements sur 643 (10,1 %) ont un historique réel**, c'est-à-dire plus d'une ligne ;
- les **578 autres ont une ligne unique qui reproduit l'état courant** — vérifié, pas supposé :
  `Reference = StlSettlement.Code` sur **578/578**.

Quand elle est informative, la table porte la **trame des remplacements de chèque** : un
`PASDEDONEES55` échu au 28/01/2022 devenu deux `CHEQUE NOTAIRE` au 02/03/2022, avec l'utilisateur
à chaque étape.

**Obstacle à traiter en amont si on la reprend un jour** : `StlSettlementHistory` et
`StlDeduction` **n'existent pas dans `yesRemsTest`** (0 colonne), comme `StlSettlementView`. Il
faudra les ajouter au schéma CI *avant* de mapper, sinon la suite tombe en masse sur une dérive de
base.

---

## 4. Contrat

`GET /api/encaissements/{id}` rend désormais un **`EncaissementDetailDto`** : tous les champs de
`EncaissementDto`, plus

```json
"affectations": [
  { "id": "…", "montantAffecte": 1000.00,
    "echeanceId": "…", "echeanceCode": "…", "echeanceDescription": "…",
    "dateEcheance": "2025-06-15T00:00:00", "montantEcheance": 1000.00,
    "categorieId": "…", "categorie": "AVANCE",
    "dossierId": "…", "dossierCode": "…" }
]
```

- **Trié par `montantAffecte` croissant** — l'ordre de l'écran légataire (`ORDER BY Amount ASC`).
- **Jamais `null`** : un tableau vide signifie un règlement encaissé mais pas encore imputé.
- La somme des `montantAffecte` vaut `montantBrut - montantBrutRestant`, et c'est testé.

### Ce que cela corrige

`dossierId`/`dossierCode` de la forme de liste ne portent que le dossier de la **première** ligne.
Mesuré : **8 règlements sur 643 couvrent plusieurs dossiers**, et pour eux la liste en affiche un
et en cache un autre. La fiche les montre tous. Les deux champs restent servis, pour que la fiche
garde la forme de la liste.

### La fiche n'est plus filtrée par `?colonnes=`

`EncaissementDto` porte un `@JsonFilter` : sans `?colonnes=`, seules les colonnes par défaut sont
sérialisées. C'est juste pour une grille, dont l'utilisateur choisit les colonnes ; c'est faux pour
une fiche, qui doit tout dire. `EncaissementDetailDto` n'est donc pas filtré.

**Impact front** : la fiche renvoie *plus* de clés qu'avant — celles hors sélection par défaut
(`description`, `creePar`, `montantBase`…), plus `affectations`. **Aucune clé ne disparaît, aucune
ne change de type.** Un client qui refuse les champs inconnus à la désérialisation doit être
vérifié. Corollaire : `?colonnes=` n'a plus d'effet sur cette route — il n'en avait aucun usage
documenté, mais il était techniquement accepté.

### Duplication gardée par un test

Les composants repris de `EncaissementDto` sont **recopiés** dans `EncaissementDetailDto` : un
`@JsonUnwrapped` aurait réintroduit le filtre de la liste, une imbrication nue aurait changé la
forme de la réponse. Le risque — un champ ajouté à la liste et oublié sur la fiche — est neutralisé
par `ficheEtListe_exposentLesMemesChamps`, qui compare les deux jeux de composants par réflexion.

---

## 5. Un 500 corrigé, introduit par le lot précédent

L'association `StlSettlement.affectation` vers la pré-agrégation a pour clé étrangère le `PKey` du
règlement lui-même, **toujours renseigné** : Hibernate fabrique donc toujours un proxy. Or le
sous-select ne portait que les règlements *ayant* des lignes. Déréférencer ce proxy sur un règlement
sans ligne levait `ObjectNotFoundException`, soit un **500 sur la fiche**.

**Portée réelle : 130 règlements sur 3 243 (4 %)** n'ont aucune ligne — dont les encaissements non
encore imputés, dont la spec précédente documente qu'ils existent (16 pièces, 6 435 400 DH). Le
défaut était donc atteignable en production.

Aucun test ne l'avait vu : la **liste** est restreinte aux règlements affectés, et c'est elle que
les tests couvraient. La **fiche** ne l'est pas — il a fallu écrire le premier test qui charge un
règlement sans ligne pour que le défaut apparaisse.

**Correctif** : le sous-select part désormais de `StlSettlement` en `LEFT JOIN` vers les lignes,
donc porte une ligne par règlement. `SUM` d'un ensemble vide vaut `NULL`, substitué par zéro en
Java — jamais par un littéral en SQL. Coût mesuré : **9 ms** (16 ms à froid), contre 7 ms pour la
version étroite. Non-régression verrouillée par `reglementNonAffecte_rendUnTableauVide`.
