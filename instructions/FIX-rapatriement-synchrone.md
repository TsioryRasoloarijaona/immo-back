# FIX — rapatriement synchrone des écritures (lecture après écriture)

Origine : tests navigateur du 08/10/2026. Branche `fix/rapatriement-synchrone` (immo-crm).
Complète `FEATURE-source-de-verite-115.md`.

## Constat

Le backend écrit dans la source de vérité et lit dans la copie (`DataSourceConfig.java:47-58`).
La recopie de ses propres écritures (`RemonteeLectureListener` → `RemonteeLecture.remonter`) est
annotée `@Async("remonteeExecutor")` (`RemonteeLectureListener.java:27`) : la réponse 201 part
**avant** que la ligne soit dans la copie. Le front relit aussitôt et lit l'ancien état :

- action ajoutée → toast « Action enregistrée », liste inchangée ;
- « Créer prospect » → la fiche reste « pré-prospection » ;
- `POST /api/dossiers` 201 → `GET /api/dossiers/{id}` 404 « Dossier introuvable » ~10 s.

La spec d'origine promettait l'inverse : « la ligne est dans la copie sans attendre de
synchronisation » (§ l.87) et « Impact front : Aucun » (l.346-350). Vaut aussi en production
(lecture `yesRemsDev`, écriture 115 par Internet), où l'écart est plus long.

## Attendu

1. **Recopie dans le fil de la requête.** Retirer `@Async` de `RemonteeLectureListener` : l'événement
   `EcrituresValidees` est publié en `afterCommit` (`CollecteurLignesEcrites.java:123-134`), donc avant
   que le contrôleur écrive la réponse. Invariant : **quand un POST/PUT/PATCH/DELETE répond 2xx, chaque
   ligne qu'il a écrite est lisible dans la copie** (sauf échec de recopie, voir 3).
2. **Pas d'interblocage de pool.** Pendant `afterCommit`, la requête peut encore tenir sa connexion
   source ; la recopie en emprunte une seconde au même pool (5 par défaut). Cinq écritures
   simultanées s'attendraient mutuellement jusqu'au timeout. Donner à `RemonteeLecture` **son propre
   petit pool Hikari sur la source** (taille `SOURCE_DB_POOL_REMONTEE`, défaut 2, nom
   `hikari-remontee`) — ou démontrer par un test que la connexion de la requête est libérée avant
   `afterCommit` et l'écrire dans la spec.
3. **Échec de recopie = pas d'échec de la requête.** L'écriture source est validée ; on garde le
   `try/catch` + `log.error` actuel, la synchronisation périodique rattrape (`RemonteeLecture.java:221`).
4. **Ordre parent → enfant.** `CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES` (`:35-40`) n'a pas
   `AdmEvent` : il passe après `ComActionMessage`, qui le référence (RDV d'une action « Visite »,
   `ProspectionService.java:1288-1294`). Ajouter `AdmEvent` **avant** `ComActionMessage`.
5. Mettre à jour `FEATURE-source-de-verite-115.md` (section rapatriement + « Impact front ») : la
   recopie est synchrone, et pourquoi.

`remonteeExecutor` (`DataSourceConfig.java:145-154`) : le supprimer s'il n'a plus d'usage.

## Hors périmètre

Synchronisation périodique et bouton « Synchroniser » : inchangés. Routage lecture/écriture :
inchangé. « Dernière action / Dernière comm. » vides (vue `ComSaleProspectionView`, horloge de la
copie et catégorie NULL) : traité ailleurs. Aucun changement front ; `/api-docs` inchangé.

## Vérification

- Test d'intégration : après `POST /api/prospections/{id}/interactions` (avec résultat « visite » et
  `dateRdv`, donc un `AdmEvent`), un `GET …/interactions` **immédiat** contient la nouvelle action ;
  après `POST /api/dossiers`, `GET /api/dossiers/{id}` immédiat → 200. Si l'environnement de test n'a
  pas de source distincte (`SOURCE_DB_HOST` vide → routage inactif, `DataSourceConfig.java:35`), tester
  au niveau `RemonteeLectureListener`/`RemonteeLecture` : l'écouteur s'exécute dans le fil appelant, et
  l'ordre trie `AdmEvent` avant `ComActionMessage`.
- Tests ciblés `-Dtest=…` verts localement ; CI complète verte.
- Navigateur (manager) : ajout d'action → visible sans recharger ; création de dossier → fiche
  affichée directement.

## Réalisé

- §2 : **pool dédié `hikari-remontee`** (`SOURCE_DB_POOL_REMONTEE`, défaut 2), pas la
  démonstration. Avec `JpaTransactionManager` + Hibernate, la connexion de la requête n'est rendue
  qu'au nettoyage de fin de transaction, après les `afterCommit` : l'option « démontrer qu'elle
  est libérée » aurait démontré l'inverse. Le pool sert aussi la relecture source de
  `SynchronisationDescendante`, qui appelle le même `RemonteeLecture`.
- Le test d'intégration HTTP n'est pas faisable ici : la suite n'a pas de source distincte
  (routage inactif). Test de repli `config/RemonteeSynchroneTest` : écouteur exécuté dans le fil
  appelant malgré `@EnableAsync`, échec de recopie non propagé, chaîne écouteur Hibernate →
  `afterCommit` → recopie avec `AdmEvent` avant `ComActionMessage`, réglages du pool.
