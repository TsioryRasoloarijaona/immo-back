# FEATURE — Sélection de colonnes sur la liste des prospections

> Lot 2 du refactor « exploiter les vues du légataire ». Le lot 1
> (`FEATURE-vues-legacy-prospection.md`) a enrichi la **fiche** ; celui-ci ouvre la **liste** aux
> mêmes indicateurs, et donne au front de quoi construire un sélecteur de colonnes piloté par le
> serveur.

## 1. Ce qui est livré

- `GET /api/prospections?colonnes=…` — le client choisit les colonnes de la réponse.
- 14 colonnes d'activité disponibles sur la liste, jusqu'ici réservées à la fiche.
- `GET /api/referentiels/colonnes/prospections` — le catalogue qui alimente le sélecteur.
- **400** avec la liste des noms valides quand un nom est hors catalogue.

**Sans le paramètre, la réponse est exactement celle d'avant ce lot** — même clés, même ordre,
et aucune requête supplémentaire. C'est la propriété qui rend le lot additif, et elle est
verrouillée par un test.

## 2. La mesure qui a décidé l'architecture

Le brief initial (et le guide front publié au lot 1) annonçait une **projection SQL dynamique** :
traduire `?colonnes=` en `SELECT` restreint, pour profiter de l'élagage des sous-requêtes
corrélées de `ComSaleProspectionView`. Deux mesures l'ont invalidé.

**D'abord, comment servir la liste.** Deux architectures possibles, connexion chaude :

| | page 1 | `OFFSET 500` | `OFFSET 5000` |
|---|---:|---:|---:|
| **A — servir la liste depuis la vue** (projection + tri + offset) | 32 ms | — | **513 ms** |
| **B — enrichir la page déjà chargée** (`WHERE PKey IN (20 ids)`) | **2–5 ms** | 2–5 ms | **2–5 ms** |

B gagne d'un ordre de grandeur, et pour une raison structurelle : la vue n'est jamais
*parcourue*, seulement interrogée par clé primaire. La pagination profonde reste sur
`ComProspection`, table indexée, avec ses `Specification`, son périmètre commercial et son
exclusion des pré-prospections **inchangés** — donc aucun risque de régression fonctionnelle.

**Ensuite, faut-il projeter en SQL.** Une fois l'option B retenue, non :

| Requête sur la vue, par lot d'identifiants | Temps |
|---|---:|
| 4 colonnes, `IN (20 ids)` | 2 ms |
| 8 colonnes, `IN (20 ids)` | 5 ms |
| **20 colonnes mappées, `IN (20 ids)`** | **7 ms** |
| 20 colonnes mappées, `IN (100 ids)` | 34 ms |

Demander 4 colonnes ou les 20 revient au même. **Assembler du SQL à partir d'une entrée client
pour un gain de 5 ms ne se justifie pas** : la sélection est donc appliquée à la sérialisation,
et la liste blanche sert à valider le contrat d'API, non à sécuriser une concaténation.

Ce que la sélection économise vraiment : la **charge utile**, et — quand aucune colonne
d'activité n'est demandée — la requête de vue, qui n'est alors pas émise du tout.

## 3. Contrat d'API

```
GET /api/prospections?colonnes=code,prospectNom,joursInactivite&page=0&size=25
```

- **Paramètre absent ⇒ comportement historique**, à l'octet près.
- Répétable (`?colonnes=a&colonnes=b`) ou séparé par des virgules (`?colonnes=a,b`) — les deux
  écritures sont équivalentes, un test le tient.
- **`id` est toujours sérialisé**, demandé ou non : sans lui la ligne n'est plus cliquable.
- Nom hors catalogue ⇒ **400**, `ProblemDetail` portant `colonne` et `colonnesValides`. Ignorer
  le nom en silence laisserait le front conclure à une colonne vide plutôt qu'à une faute de
  frappe.
- La forme de `PageDto` ne change pas ; seules les **clés non demandées sont absentes** de chaque
  élément de `content`. Le DTO reste entièrement typé et documenté dans `/api-docs`.

### Les 14 colonnes ajoutées

Groupe `ACTIVITE` : `joursInactivite` (100 % peuplé), `nbInteractions` (97 %),
`derniereActionDate` (97 %), `derniereCommunicationDate` (90 %),
`derniereCommunicationResultat` (90 %), `nbVisites` (20 %), `derniereVisiteDate` (20 %).

Groupe `CONTEXTE` : `projetId` / `projetCode` / `projetLibelle` (99 % — la liste n'exposait que
la tranche). Groupe `IDENTITE` : `cin` (99 %). Groupe `CONVERSION` : `dossierId` / `dossierCode`
/ `statutDossier` (2 %, qui est le taux de conversion réel).

### Le catalogue

`GET /api/referentiels/colonnes/prospections` renvoie, pour chaque colonne : `nom` (à passer tel
quel), `libelle` (français, prêt à afficher), `groupe`, `type` (`TEXTE` | `NOMBRE` | `MONTANT` |
`DATE` | `BOOLEEN`), `parDefaut`.

C'est **la même source que la liste blanche** qui valide `?colonnes=` — l'énumération
`domain/ColonneProspection`. Les deux ne peuvent donc pas diverger, et le front n'a aucune liste
à coder en dur.

## 4. Implémentation, et pourquoi elle est inhabituelle

Le mécanisme usuel — `MappingJacksonValue` + `FilterProvider` par requête — **est inutilisable
ici**. Spring Boot 4.1 sérialise avec **Jackson 3** (`tools.jackson.core:jackson-databind:3.1.4`) ;
Jackson 2 n'est présent que transitivement, via `jjwt`. Or le seul `MappingJacksonValue` que
Spring 7 expose est typé sur le `FilterProvider` de Jackson 2. Les deux univers ne se rencontrent
pas, et la tentative échoue à la compilation.

D'où le montage retenu :

| Classe | Rôle |
|---|---|
| `domain/ColonneProspection` | catalogue + liste blanche + résolution (lève sur nom inconnu) |
| `web/ColonnesSelectionFilter` | filtre servlet : dépose la sélection pour la requête, la retire en `finally` |
| `web/ColonnesSelection` | le porteur (`ThreadLocal`) |
| `web/SelectionColonnesPropertyFilter` | `PropertyFilter` Jackson 3, sans état, lit le porteur |
| `web/ColonnesJacksonConfig` | l'enregistre une fois pour toutes sur le mapper |

Trois points à ne pas défaire :

**Le nettoyage du `ThreadLocal` est en `finally`.** Sur un thread de pool, une sélection oubliée
filtrerait silencieusement les réponses des requêtes suivantes.

**Le filtre est enregistré globalement, pas par endpoint.** `ProspectionListItemDto` est aussi
embarqué par `HotlineFicheDto` (`GET /api/hotline/recherche`) et servi par
`GET /api/pre-prospections`. Jackson **lève une erreur** dès qu'il croise un `@JsonFilter` dont
l'identifiant n'est enregistré nulle part : sans ce bean, ces deux endpoints tomberaient en 500.
Un test le couvre.

**La sélection par défaut est réellement appliquée**, y compris sans paramètre. Ajouter 14
composants au record faisait sinon apparaître 14 clés à `null` dans toutes les réponses — la
promesse « identique à l'existant » aurait été fausse.

**Les pré-prospections n'ont pas de colonnes d'activité**, et `GET /api/pre-prospections` force
donc la sélection par défaut : `ComSaleProspectionView` les exclut par construction, demander la
vue reviendrait à payer une requête pour n'obtenir que des `null`.

## 5. Impact front

Additif. Une intégration qui ignore `colonnes` ne voit aucun changement.

Un seul changement de typage à prévoir dans `types.ts` : **les champs de
`ProspectionListItemDto` deviennent optionnels**, puisqu'une clé non demandée est absente. Le
changement peut être fait dès maintenant — un champ optionnel toujours présent ne gêne pas.

Piège à relayer : sur la liste comme sur la fiche, `dossierId` / `dossierCode` / `statutDossier`
sont absents sur 98 % des lignes, et c'est le taux de conversion réel, pas une donnée manquante.

`GUIDE-FRONT-fiche-prospection.md` est mis à jour par ce lot : sa §5 décrivait le sélecteur comme
« non implémenté » et le justifiait par un gain de pagination profonde que la mesure a invalidé.

## 6. Ce que ce lot ne fait pas

- **Les autres listes.** Dossiers (`ComSaleFolderView`, 217 colonnes), articles
  (`StkItemView`, 186), clients (`ComSaleCustomerView`, 101) gardent leurs colonnes actuelles.
  Le mécanisme est générique dans son principe, mais `ColonneProspection` est spécifique à une
  liste : l'étendre demandera un catalogue par ressource.
- **La mémorisation du choix de colonnes.** Aucune table légataire ne stocke de disposition —
  vérifié, le logiciel de bureau les garde sur le poste client. Le front mémorise en local ; une
  table applicative n'a de sens que si le métier veut retrouver son affichage d'un poste à
  l'autre, et c'est une demande à formuler via `from_front.md`.
- **Le tri sur une colonne d'activité.** `?sort=joursInactivite` renvoie **400** — vérifié par un
  test, et c'est le bon comportement : le tri est appliqué sur `ComProspection`, qui ne porte pas
  ces colonnes. Trier dessus supposerait de servir la liste depuis la vue, soit l'option A
  mesurée à 513 ms. À rouvrir seulement si le métier le demande, en sachant ce que ça coûte.
- **Le découpage pré-prospection / prospection** (les 830 lignes) et le **`StrEntityId` à la
  création** restent ouverts, chacun sur sa décision métier.
