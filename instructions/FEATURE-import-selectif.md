# FEATURE — Import CSV des leads : choisir les statuts à importer

Demande utilisateur du 08/10/2026. Prolonge `FEATURE-creation-pre-prospection.md` §5 (à lire
d'abord : parcours aperçu → import, statuts `NOUVELLE` / `DOUBLON` / `DEJA_IMPORTEE` / `ERREUR`).

**Besoin** : après l'aperçu, pouvoir n'importer que les **nouvelles**, que les **doublons**, ou
les deux. Aujourd'hui `POST /import` crée toujours toutes les `NOUVELLE` **et** toutes les
`DOUBLON`, quel que soit le filtre affiché à l'écran (le filtre n'est qu'un affichage).

Choix retenus avec l'utilisateur : sélection **par statut** (pas ligne par ligne), et filtrage
**côté back** (le front ne réécrit pas le CSV : encodage, guillemets et numéros de ligne
resteraient fragiles).

## 1. Backend — `POST /api/pre-prospections/import`

Nouveau paramètre de requête, optionnel :

| Param | Type | Valeurs admises | Absent |
|---|---|---|---|
| `statuts` | liste de `StatutLigneImport` (répété `statuts=NOUVELLE&statuts=DOUBLON` **et** séparé par virgules `statuts=NOUVELLE,DOUBLON` acceptés) | `NOUVELLE`, `DOUBLON` | `NOUVELLE` + `DOUBLON` = comportement actuel, inchangé |

Règles (chacune testée) :

- **R1** — Seules les lignes dont le statut est dans `statuts` sont créées. Une ligne `NOUVELLE`
  ou `DOUBLON` hors sélection n'est **pas** écrite : `preProspectionId` et `code` null.
- **R2** — **La classification ne dépend pas de la sélection** : pour un même fichier et un même
  état de base, chaque ligne a, dans la réponse de `/import`, le **même `statut`** que dans
  `/import/apercu`, quelle que soit la valeur de `statuts`. (Les lignes non sélectionnées restent
  prises en compte pour la détection des doublons internes au fichier : une `DOUBLON` « même
  contact que la ligne 3 du fichier » reste `DOUBLON` même si la ligne 3, `NOUVELLE`, n'est pas
  importée.)
- **R3** — Message d'une ligne `NOUVELLE`/`DOUBLON` non sélectionnée : `"Non importée (statut non
  sélectionné)"` pour une `NOUVELLE` (dont le message est null aujourd'hui) ; pour une `DOUBLON`,
  le message existant suivi de `" — non importée (statut non sélectionné)"`.
- **R4** — `statuts` contenant `DEJA_IMPORTEE`, `ERREUR`, une valeur inconnue, ou vide
  (`statuts=`) → **400**, rien n'est écrit. Attention : aucun handler n'existe aujourd'hui pour
  `MethodArgumentTypeMismatchException` — une valeur d'enum inconnue ne doit **pas** finir en 500.
- **R5** — Les règles existantes ne bougent pas : `trancheId` obligatoire, 403 hors périmètre
  sans aucune écriture, réimport idempotent (`DEJA_IMPORTEE`), avertissements.
- **R6** — `/import/apercu` ne change pas de contrat (il n'accepte pas `statuts`).

Conséquence attendue, à documenter (pas un défaut) : importer d'abord les `DOUBLON` seuls, puis
réimporter le fichier avec `NOUVELLE` — la ligne 3 de l'exemple ci-dessus devient alors
`DOUBLON` (son contact existe désormais en base), et est donc filtrée si l'on ne choisit que
`NOUVELLE`. La classification est toujours relative à l'état de la base ; l'aperçu le montre.

### DTO — `RapportImportPreProspectionsDto`

Nouveau champ, à la fin :

| Champ | Type | Valeur |
|---|---|---|
| `statutsImportes` | `List<StatutLigneImport>` | Après import : les statuts effectivement retenus (défaut `[NOUVELLE, DOUBLON]`), dans cet ordre. **`null` dans l'aperçu.** |

Ajouter aussi `creees` (`int`) : nombre de lignes réellement écrites (= lignes avec
`preProspectionId` non null) ; `0` dans l'aperçu. Les compteurs `nouvelles` / `doublons` restent
des compteurs de **classification** (inchangés).

Mettre à jour les `@Schema` concernés : `doublons` (« créées elles aussi » → « créées si
sélectionnées »), `preProspectionId` de `LigneImportDto` (null aussi pour une ligne non
sélectionnée), et l'`@Operation` de `/import` (summary + description du paramètre `statuts`).
Texte long → `src/main/resources/api-docs/*.md`. Mettre à jour la Javadoc de `importer` et
celle de `StatutLigneImport` (« créée » → « créée si sélectionnée »).

## 2. Front — `crm-prototype`

Après le backend : `npm run docs:update`, **`git diff docs/api-docsV4.json`**, puis
`src/types.ts` (`statutsImportes`, `creees`), `ImportPreProspectionsParams.statuts`, et
`importerPreProspections` dans `src/lib/api.ts` (vérifier que `postMultipart` sérialise une
liste — sinon l'envoyer jointe par virgules).

Écran `/pre-prospections/import` (`src/pages/prospections/ImportPreProspections.tsx`), carte
« 2. Tranche de rattachement » :

- Ajouter un groupe **« Lignes à importer »** : deux cases à cocher, **« Nouvelles (n) »** et
  **« Doublons (n) »** (n = `rapport.nouvelles` / `rapport.doublons`), **cochées toutes les deux
  par défaut** (= comportement actuel). Une case dont n = 0 est désactivée. Phrase d'aide
  courte : les déjà importées et les erreurs ne sont jamais importées.
- Le bouton devient « Importer N ligne(s) » avec N = somme des cases cochées ; désactivé si N = 0
  ou pas de tranche. Envoyer `statuts` = les statuts cochés.
- Remise à zéro (nouveau fichier / « Importer un autre fichier ») : les deux cases recochées.
- Résultat : le résumé affiche `creees` (« N pré-prospections créées »). Dans le tableau, une
  ligne non importée garde son badge de statut ; la colonne Code affiche « Non importée » en
  texte atténué au lieu d'être vide. Les filtres d'affichage existants ne changent pas.

Libellés en français, pas de nouveau composant si un existant convient (case à cocher déjà
utilisée ailleurs dans `src/components` ?).

## Hors périmètre

- Sélection ligne par ligne.
- Tout changement de la classification, de l'aperçu, des défauts de référentiel, du lecteur CSV.
- generateur-virement.

## Vérification

Backend, dans `PreProspectionImportTest` (ou un test voisin), sur un fichier contenant au moins
une `NOUVELLE`, une `DOUBLON` interne au fichier (même contact qu'une `NOUVELLE` plus haut,
autre minute), une `DEJA_IMPORTEE` et une `ERREUR` :

1. `statuts=NOUVELLE` → seules les `NOUVELLE` ont un `preProspectionId` ; nombre de lignes
   `ComProspection` créées en base = `rapport.nouvelles` = `creees` ; `statutsImportes = [NOUVELLE]`.
2. `statuts=DOUBLON` → seules les `DOUBLON` sont créées ; la `DOUBLON` interne reste `DOUBLON`
   bien que sa `NOUVELLE` de référence ne soit pas importée (R2) ; message R3.
3. Sans `statuts` → mêmes créations qu'avant (les tests existants restent verts).
4. R2 : les statuts ligne à ligne de `/import?statuts=NOUVELLE` égalent ceux de `/apercu` sur le
   même fichier.
5. `statuts=ERREUR`, `statuts=DEJA_IMPORTEE`, `statuts=FOO`, `statuts=` → 400 chacun, **zéro**
   ligne écrite.
6. Aperçu : `statutsImportes` null, `creees` 0.

Full `./mvnw test` vert ; `npm run build` exit 0. Navigateur : aperçu d'un fichier, décocher
« Doublons », le bouton annonce le nombre de nouvelles, import, résultat « Non importée » sur
les doublons.
