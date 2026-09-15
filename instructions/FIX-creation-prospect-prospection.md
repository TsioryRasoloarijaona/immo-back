# FIX — Créations de prospect et de prospection : l'écart avec le légataire

L'utilisateur a rejoué les deux écrans de création dans OMRAN pendant la session, ce qui a produit
une trace fraîche : `INSERT INTO CfgTier` à 15:34:52 et `INSERT INTO ComProspection` à 15:36:46, le
15/09/2026, session 68. C'est de là que vient tout ce document.

> **Lire la trace sans se tromper de client.** Le compte SQL est `tsix` pour tout le monde, nos
> propres `sqlcmd` compris. Le discriminant est `application_name` (`OMRAN estate`), et `event_time`
> est en UTC — heure locale = UTC + 1, vérifié par une requête-sonde horodatée.

---

## 1. Les deux séquences du légataire

**Prospect**

```
1. SELECT … SOUNDEX(FirstName) / SOUNDEX(LastName)   détection de doublon phonétique
2. SELECT Count(*) … (nom+prénom) ET Mobile          unicité
3. SELECT Count(*) … (nom+prénom) ET EMail           unicité
4. UPDATE AdmSequence  PKey=50c4f768…  (Code = 'Tier')
5. INSERT INTO CfgTier(… 84 colonnes …)
```

**Prospection**

```
1. SELECT … WHERE CfgProspectId=@P1 AND StkHierarchyId=@P2 AND IsClosed=0
2. UPDATE AdmSequence  PKey=51c4f768…  (Code = 'Prospection')
3. SELECT StkVocation …
4. INSERT INTO ComProspection(… 54 colonnes …)
5. INSERT INTO ComActionMessage(… 34 colonnes …)
```

Les sous-collections (`ComProspectionCity`, `…Category`, `…Type`, `…Project`,
`…SelectedProfile`) n'ont reçu **aucun `INSERT`** : elles sont optionnelles et se remplissent
après, ce qui valide notre découpage « création puis sous-ressources ».

---

## 2. Pourquoi la liste des colonnes ne répond pas à « quelles données sont obligatoires »

L'ORM d'OMRAN écrit **toutes** les colonnes de l'entité, renseignées ou non, et la trace masque les
valeurs (`@P1…@Pn`). 84 colonnes sur `CfgTier` ne veut donc pas dire 84 données attendues.

L'obligation a été cherchée là où elle est réellement — et le résultat surprend :

| Table | Colonnes `NOT NULL` |
|---|---|
| `CfgTier` | **`PKey` seule** |
| `ComProspection` | **`PKey` seule** |
| `ComActionMessage` | **`PKey` seule** |

**Le schéma légataire n'impose rien.** Toute obligation est applicative, et ce sont exactement les
requêtes qui précèdent l'`INSERT` : nom + prénom pour un prospect (les trois contrôles reposent
dessus), plus mobile **ou** e-mail sur lesquels porte l'unicité ; prospect + tranche pour une
prospection.

---

## 3. Corrigé — le prospect reçoit son code

`ProspectService.createEntity` ne posait jamais `CfgTier.Code`. Mesure : sur **13 913** prospects et
clients réels, **12 seulement** n'ont pas de code. Chaque prospect créé par l'API rejoignait donc
une anomalie à 0,09 %.

Le compteur est `AdmSequence.Code = 'Tier'`, format `YY/NNNNN` — `26/14431` en base réelle.
C'est **la même ligne** que celle qu'OMRAN incrémente : une numérotation parallèle produirait des
codes en collision, les deux applications écrivant dans `yesRems`.

Le correctif porte sur `createEntity`, donc il couvre **les deux chemins de création** :
`POST /api/prospects` et `POST /api/pre-prospections/{id}/creer-prospect`.

### Un générateur au lieu d'une troisième copie

`DossierService` et `ProspectionService` portaient déjà chacun leur générateur, au même mécanisme.
Le prospect en réclamait un troisième. Extrait en `services/sequence/GenerateurCodeSequence`, que
les trois appellent désormais ; le préfixe projet du dossier (`APF 2026/00380`) reste à sa charge,
il dépend de l'article et non du compteur.

Au passage, `AdmSequenceRepository` disparaît de `DossierService` et `ProspectionService` : la
dépendance n'y servait plus qu'à être injectée.

---

## 4. Corrigé — la prospection écrit son interaction

Le légataire insère un `ComActionMessage` dans la seconde qui suit son `ComProspection`. Nous ne
l'écrivions pas : `creer()` posait `setComActionMessageType(...)`, une simple référence au type,
puis sauvegardait. **Une prospection naissait sans aucun échange**, ce qui fausse dès la création
tout ce qui se fonde sur les interactions — dernier échange, délai de prise en charge, compteurs
d'activité de `/api/kpi/commerciaux`.

### Les drapeaux, mesurés sur les 2 634 interactions de création réelles

| Champ | Valeur | Pourquoi |
|---|---|---|
| `IsSystem` | **1** | exclut la ligne du « dernier échange » — `ComSaleProspectionView` le calcule avec `AND IsSystem = 0` |
| `WithoutRevival` | **1** | ce n'est pas une relance |
| `AlertValidityDate` | `null` | idem |
| `AlertMessage` | les notes de la prospection | identique au caractère près sur 1 507 des 2 634, les écarts n'étant que des espaces |
| `IsNotRead` | 0 | |
| `CfgTier` | le prospect | comme sur les interactions manuelles |
| type | celui de la prospection | 2 198 des 2 634 (83 %) |
| catégorie | `Prospection` | 2 250 des 2 634 ; les 384 autres sont « Dossier de vente (Réservation) » et relèvent de la vente |

**`IsSystem = 1` est l'inverse de ce que pose `creerInteraction`, et c'est voulu.** Les deux
conventions cohabitent : une saisie humaine est un échange, la création d'une prospection n'en est
pas un. C'est le choix du légataire, et il préserve la sémantique de notre vue.

Une catégorie introuvable ne fait pas échouer la création — la ligne part sans. Refuser une
prospection pour un référentiel incomplet serait disproportionné.

---

## 5. **Non** corrigé, et pourquoi : trois contrôles qui casseraient le contrat

Le légataire exécute trois vérifications que nous n'avons pas :

1. **doublon phonétique** — `SOUNDEX(FirstName)` + `SOUNDEX(LastName)`, dans les deux ordres ;
2. **unicité** (nom+prénom+mobile) et (nom+prénom+e-mail) ;
3. **une seule prospection ouverte** par couple (prospect, tranche).

Elles ne sont **pas** implémentées ici, pour une raison précise : la trace montre les `SELECT`,
jamais ce qu'OMRAN en fait. Le premier et le troisième sont des `SELECT *` — donc vraisemblablement
une liste montrée à l'utilisateur —, le deuxième un `Count(*)` — donc vraisemblablement un blocage.
Vraisemblablement n'est pas mesuré.

Surtout, les transformer en refus ajouterait des **409 là où l'API en renvoyait aucun**. C'est une
rupture du contrat servi par `/api-docs`, et une décision produit : bloquant ou consultatif ?
Elle appartient à l'utilisateur, pas à ce lot.

**Ce qu'il faudrait trancher** : si consultatif, un champ dans la réponse de création ou un endpoint
`GET /api/prospects/doublons-potentiels` ; si bloquant, un 409 documenté et le front à prévenir.

---

## 6. Impact front

**Aucune rupture de contrat.** Les deux correctifs ajoutent des écritures manquantes, ils ne
changent ni les chemins, ni les champs, ni les codes de statut.

Deux changements de comportement observables, tous deux dans le sens de la conformité :

> **`POST /api/prospects` renvoie désormais un `code` renseigné** (`26/14431`) là où il était nul.
> Un front qui affichait un tiret à la place peut maintenant afficher la valeur.

> **`POST /api/prospections` crée une interaction.** La fiche d'une prospection neuve montre donc
> une ligne d'historique là où elle en montrait zéro. Cette ligne porte `IsSystem = 1` : elle ne
> doit **pas** être présentée comme un échange avec le client, et elle n'entre pas dans le « dernier
> échange ».
