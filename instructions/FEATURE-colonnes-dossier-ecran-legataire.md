# FEATURE — Les colonnes du dossier que l'écran légataire affiche et que nous ne servions pas

Lot issu d'une **trace d'audit SQL Server du logiciel de bureau** — première fois qu'on part de ce
qu'Omran exécute réellement plutôt que d'une capture d'écran ou d'une lecture de vue.

```sql
SELECT event_time, action_id, statement
FROM sys.fn_get_audit_file('C:\backup\*.sqlaudit', DEFAULT, DEFAULT)
```

739 événements sur 9 minutes, 68 requêtes distinctes — dont 8 sont l'introspection de l'IDE et non
Omran. Séquence tracée : authentification, liste des dossiers, ouverture d'un dossier.

---

## 1. Ce que la trace a corrigé dans notre compréhension

**La hiérarchie `H1/H2/H3` n'était pas un manque.** Le tri par défaut de la liste légataire est
`ORDER BY ArticleCode, H1Code, H2Code, H3Code`, et une première lecture laissait croire à trois
niveaux d'article absents de chez nous. La définition de la vue dit autre chose :

| Colonne de la vue | Origine réelle | Chez nous |
|---|---|---|
| `H1Code` / `H1Description` | `CfgTranche.Code` / `.Description` | `trancheCode` / `trancheLibelle` ✅ |
| `H2Code` / `H2Description` | `CfgProject` | `projetCode` / `projetLibelle` ✅ |
| `H3Code` / `H3Description` | `CfgCompany` | `societeCode` / `societeLibelle` ✅ |
| `ArticleCode` | `StkItem.Code` | **absent** ❌ |

Sur les quatre clés de tri, trois étaient déjà servies. Seul l'article manquait. **Vérifier la
définition avant de chiffrer un écart** — c'est ce qui a évité de spécifier neuf colonnes pour en
livrer une.

---

## 2. Les colonnes ajoutées

Toutes `parDefaut = false` : une intégration qui ignore `?colonnes=` ne voit aucun changement.

**Article principal** (groupe `ARTICLE`) — `articleId`, `articleCode`, `articleLibelle`,
`articleTitreFoncier`, `bloc`, `etage`, `categorieArticle`, `typeArticle`, `typePresentation`.

`ARTICLE` est un groupe distinct de `PATRIMOINE`, et ce n'est pas cosmétique :
`exigeLePatrimoine` déclenche la résolution de la chaîne tranche → projet → société, dont ces
colonnes n'ont pas besoin. Les confondre ferait payer trois jointures à qui demande un code
d'article. D'où `exigeLArticle`, sur le même modèle.

**Responsable de vente** (groupe `ACTEURS`) — `responsableVenteId`, `responsableVenteNom`.
Acteur **distinct du commercial**, renseigné sur **221 des 358 dossiers (61 %)**, exposé nulle part
jusqu'ici. `CfgSaleManager2Id` n'est **pas** repris : mesuré à **0 ligne sur 358**.

**Jalons et délais** (groupe `DELAIS`) — `dateCompromis` (73/358), `dateLivraisonReelle` (63/358),
`delaiConcretisation`, `delaiNotaire`, `delaiLivraison`.

Servis aussi par la fiche (`DossierDetailDto`), qui ne les avait pas non plus.

---

## 3. Les délais sont des `@Formula`, et c'est un choix

Recopiés de `ComFolderView`, qui les calcule en `DATEDIFF` :

```sql
DelayConcretisation = DATEDIFF(d, IIF(ComProspectionId IS NULL, ComFolder.DocDate,
                                      ComProspection.DocDate), AdvanceDate)
DelayLawyer         = DATEDIFF(d, AdvanceDate, AgreementDate)
DelayDelivery       = DATEDIFF(d, AgreementDate, DeliveryDateReal)
```

**Pourquoi `@Formula` plutôt qu'un calcul en Java** : ce sont alors des propriétés ordinaires, donc
utilisables comme clé de `?groupBy=` et comme colonne de `?colonnes=` sans que le moteur de
groupement ait à connaître la notion de « champ calculé ». Un calcul côté Java aurait produit une
colonne affichable mais **non groupable** — une incohérence de plus à expliquer dans le catalogue.

**Coût mesuré** (`yesRemsProd`) : page de 20 dossiers, **1 ms** avec les trois formules contre
0–1 ms sans ; **6 ms** sur les 358 dossiers. La sous-requête corrélée ne change pas l'ordre de
grandeur.

**`DelayCredit` n'est pas repris** : il dépend de `CreditPossessionDate`, mesurée à **0 ligne sur
358**. Il vaudrait `null` partout.

### La limite à connaître : `delaiConcretisation` n'est pas groupable

Sa formule contient une sous-requête corrélée, et SQL Server refuse « un agrégat ou une sous-requête
dans une expression utilisée dans la liste GROUP BY ». Déclaré `Chemin.agregeable` et non
`groupableEtAgregeable` : **sommable, sélectionnable, pas clé de groupe**. Le référentiel l'annonce
(`groupable: false`) et `?groupBy=delaiConcretisation` rend un **400**, pas une
`SQLGrammarException` en 500 — ce qui arrivait avant la déclaration.

Les deux autres délais n'arithmétisent que des colonnes de `ComFolder` : groupables normalement.

---

## 4. Impact sur les deux features voisines

C'est le point à ne pas rater : **`ColonneDossier` pilote à la fois le sélecteur de colonnes et le
groupement**. Toute entrée ajoutée au catalogue doit avoir son champ dans `DossierListItemDto`,
sinon le sélecteur annonce une colonne absente de la réponse.

- **Sélecteur** — `GET /api/referentiels/colonnes/dossiers` gagne 16 entrées et deux groupes
  (`ARTICLE`, `DELAIS`). Additif : aucun champ existant ne change.
- **Groupement** — 15 des 16 sont groupables immédiatement, sans une ligne de code dans le moteur :
  elles ont un `Chemin`, ça suffit. La 16ᵉ est `delaiConcretisation` (voir §3).
- **Défaut inchangé** — vérifié par `sansSelection_aucuneNouvelleCleNApparait`.

### Impact front

Purement additif. Le sélecteur de colonnes du front se peuple depuis le référentiel, donc les
nouvelles colonnes y apparaîtront **sans redéploiement**, rangées sous deux sections nouvelles.
La bande de groupement les acceptera de même.

---

## 5. Le piège de test à retenir

**Une `@Formula` n'est peuplée qu'au chargement depuis la base.** Un test qui sauve une entité puis
interroge l'API dans la même transaction reçoit l'instance encore en session — celle construite en
Java, dont la formule n'a jamais été lue — et voit `null` en croyant tester le calcul. La première
version de ces tests est tombée exactement là-dessus.

D'où `relireDepuisLaBase()` (`flush` + `clear`) avant chaque appel HTTP touchant un délai.

---

## 6. Reste hors périmètre

La trace montre 123 colonnes de `ComFolderView` que nous ne servons pas et 84 de `ComFolder`, mais
l'essentiel est un backlog déjà écrit et mesuré — `FEATURE-fiche-dossier-alignement-ecran.md` §6
écarte explicitement les onglets « Dates » (32 dates, 12 à 0 %), « Autres montants »,
« Annulation / Résiliation » et les onglets du bas.

Deux faux manques identifiés au passage, et écartés sur mesure plutôt que sur impression :

- **`ComFolder.Description`** — 358 valeurs distinctes, l'air d'un vrai manque. C'est en fait
  `"ARS 2026/00379 du 31/08/2026"` : le code et la date concaténés, deux champs déjà servis.
- **`ComDocumentDetail`** — le schéma de financement **rendu en texte**. Nous le servons structuré
  par `GET /api/dossiers/{id}/echeancier`.

Et cinq colonnes qui paraissent peuplées à 100 % ne le sont pas : `PenalityDescription`,
`CreditAgency` et `FinalTitleNumber` sont non-nulles mais **vides partout** ; `DeliveryRatio` et
`DeliveryMaxNumber` portent une valeur unique. Compter les non-nuls ne suffit pas — il faut compter
les distinctes.

---

## 7. Ce que la trace a révélé et qui dépasse ce lot

À spécifier séparément, par ordre d'importance :

1. **Une seconde couche de permissions.** En plus du périmètre par société
   (`AdmUserHabilitation`, que nous avons), Omran filtre les dossiers **par statut visible** via
   `StrStatusIntervener` × `StrIntervener`, par rôle *ou* par utilisateur nommé. Notre
   `PerimetreCommercialService` ne connaît que le projet.
2. **Des verrous pessimistes** — `AdmWebLockEntities` adossés à `AdmWebSessions` : deux
   utilisateurs ne peuvent pas ouvrir le même dossier en écriture.
3. **Une journalisation d'usage** — `AdmWebHistorics` et `AdmFormsLog`, 12 insertions chacun pour
   une session de 9 minutes.
