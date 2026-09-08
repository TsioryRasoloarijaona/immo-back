# FEATURE — Suppression d'une pièce jointe

> Statut : spécifié le 08/09/2026. Porte sur `immo-crm` uniquement.

## 1. Pourquoi

`AppFileObject.DeletedAt` existe depuis `002_AppFileObject.sql` et **toute la chaîne de
lecture le respecte déjà** :

- `AdmAttachedFileSummary.hasFile` est une `@Formula` qui teste
  `EXISTS (… AND afo.DeletedAt IS NULL)` — une pièce marquée disparaît donc de toutes les
  listes ;
- `AppFileObjectRepository.findByAdmAttachedFileIdAndDeletedAtIsNull` fait qu'elle n'est
  plus téléchargeable (404) ;
- `FEATURE-s3-storage.md` §3.4 fixe la règle : « Suppressions : marquer `DeletedAt`, jamais
  supprimer l'objet dans la transaction de la requête ».

**Rien n'écrit jamais cette colonne.** Mesuré sur `yesRemsProd` : 3 138 lignes
`AppFileObject`, **0 avec `DeletedAt` renseigné**. Un fichier déposé par erreur — mauvais
dossier, mauvais client, scan illisible — ne peut aujourd'hui être retiré que par
l'application Windows. Il ne manque que le contrôleur et le service.

## 2. Volumétrie (`yesRemsProd`, 08/09/2026)

`AdmAttachedFile` compte 3 410 lignes, réparties par porteur :

| Porteur | Pièces | Endpoint de dépôt existant |
|---|---:|---|
| Dossier (`ComFolder`) | 745 | oui |
| Règlement (`StlSettlement`) | 461 | oui |
| Article (`StkItem`) | 476 | **non** (lecture seule) |
| Autres porteurs (tiers, prospects…) | 1 728 | hors API |

272 lignes n'ont aucun `AppFileObject` : ce sont les cases de checklist
`IsCheckedButNotAttached` déjà exclues des listes.

## 3. Portée

**Dossier et règlement uniquement** — les deux porteurs qui ont un `POST`. L'Article reste
en lecture seule, comme aujourd'hui : ses 476 pièces sont des plans legacy
(`PLAN`/`PLAN_ARCHITECTE`/`PLAN_ETAGE`) que cette application ne produit pas, et
`/api/articles/**` n'est **pas** couvert par `PerimetreInterceptor` — y ouvrir une écriture
demanderait d'abord d'écrire le contrôle de visibilité à la main.

## 4. Endpoints

```
DELETE /api/dossiers/{id}/pieces-jointes/{pieceId}                        → 204
DELETE /api/dossiers/{id}/reglements/{reglementId}/pieces-jointes/{pieceId} → 204
```

Pas de corps de requête, pas de corps de réponse.

| Code | Quand |
|---|---|
| 204 | Pièce marquée supprimée. |
| 404 | Dossier inconnu · règlement inconnu ou non rattaché à ce dossier · pièce inconnue ou appartenant à un autre porteur · pièce **déjà** supprimée. |
| 409 | Uniquement sur règlement : dernière preuve d'un règlement `POINTED` (voir §6). |

Aucune restriction de rôle : même exigence que le `POST` correspondant (authentifié).
La visibilité par périmètre et par entité juridique est appliquée par
`PerimetreInterceptor`, qui couvre `/api/dossiers/{id}/**` quelle que soit la méthode HTTP —
rien à ajouter.

## 5. Règles

**R1 — Suppression logique, jamais physique.** On écrit `AppFileObject.DeletedAt` et
`DeletedBy`. La ligne `AdmAttachedFile` **reste** : elle porte les métadonnées et l'auteur du
dépôt, et sa disparition rendrait la suppression intraçable. Elle devient simplement
`hasFile = false`, donc indiscernable d'une case de checklist — c'est-à-dire invisible.

**R2 — L'objet S3 n'est pas touché.** Ni dans la transaction, ni après. Un balayage hors
ligne s'en charge (`FEATURE-s3-storage.md` §3.4). Symétrie avec le dépôt, qui écrit S3 avant
la base : la seule fuite possible reste un objet orphelin dans le bucket, jamais une ligne
pointant vers le vide.

**R3 — Le porteur borne la pièce.** `pieceId` est cherché par
`findByIdAndCategoryId(pieceId, porteurId)` : une pièce d'un autre dossier donne 404, jamais
le fichier d'autrui. Même garde qu'au téléchargement.

**R4 — Supprimer deux fois donne 404.** La seconde requête ne trouve plus d'`AppFileObject`
non supprimé. On ne renvoie pas 204 par confort d'idempotence : le 404 dit à l'appelant que
la pièce n'est plus là, ce qui est l'information utile.

**R5 — Traçabilité.** `DeletedBy` porte le nom affiché de l'utilisateur connecté
(`UtilisateurAffiche.nom`), symétrique de `UploadedBy`. Nouvelle colonne, migration `006`.
On ne réutilise **pas** `AdmAttachedFile.UserUpdate` : peuplée à 3 410/3 410 par
l'application Windows avec la sémantique « dernier modificateur », l'écraser détruirait une
donnée legacy et rendrait la trace ambiguë.

## 6. Le cas du règlement `POINTED`

`ReglementStatutService` exige au moins une pièce jointe pour passer un règlement à
`POINTED` : la preuve de paiement est ce qui autorise le pointage. Supprimer la dernière
preuve d'un règlement déjà pointé laisserait donc un règlement pointé sans justificatif —
un trou d'audit que rien ne permettrait de détecter après coup.

**Décision : 409.** Elle ne mord que sur la *dernière* pièce d'un règlement `POINTED`.
Supprimer une pièce parmi plusieurs, ou n'importe quelle pièce d'un règlement `IN_WORK` ou
`CANCELED`, reste permis.

> **Cette décision revient sur une consigne écrite.** Le javadoc de `ReglementStatutService`
> dit : « Ne pas "renforcer" cette règle en la déplaçant vers la lecture ou vers la
> suppression d'une pièce jointe : ce serait casser des dossiers réels au nom d'un invariant
> que la base n'a jamais respecté. » L'argument qui la fondait était le risque rétroactif —
> et il ne s'applique pas ici. Mesuré sur `yesRemsProd` : **17 règlements `POINTED`, dont 3
> portent exactement une pièce jointe**. Ce sont les 3 seules lignes de toute la base sur
> lesquelles le 409 pourrait se déclencher, et uniquement si quelqu'un tente d'en supprimer
> la preuve. Aucune lecture ne change, rien n'est vérifié rétroactivement, aucune ligne
> existante ne devient illisible. Le javadoc est amendé pour enregistrer cette exception
> plutôt que de la laisser contredire le code.

## 7. Surface à modifier

| Fichier | Nature |
|---|---|
| `src/main/resources/db/migration/006_AppFileObject_DeletedBy.sql` | nouveau — `ALTER TABLE … ADD DeletedBy nvarchar(255) NULL`, idempotent |
| `ci/schema/010_tables.sql` | colonne ajoutée à `AppFileObject` pour que la suite tourne |
| `entities/app/AppFileObject.java` | champ `deletedBy` |
| `services/piecejointe/PieceJointeSupport.java` | `exigerFichier`, `marquerSupprime`, `supprimer`, `nombreDeFichiers` |
| `services/dossier/PieceJointeService.java` | `supprimer(dossierId, pieceId, principal)` |
| `services/dossier/ReglementPieceJointeService.java` | `supprimer(...)` + garde 409 |
| `services/dossier/ReglementStatutService.java` | javadoc amendé (§6) |
| `controller/dossier/DossierController.java` | 2 routes `DELETE` + springdoc |

## 8. Tests attendus

- Dossier : suppression → 204, puis absence de la liste, puis 404 au téléchargement.
- Dossier : pièce d'un autre dossier → 404 ; seconde suppression → 404.
- Dossier : `DeletedBy` porte le nom de l'utilisateur réellement connecté (login réel, pas
  `@WithMockUser` — le principal de `@WithMockUser` n'est pas un `AuthenticatedUser`).
- Règlement : suppression d'une preuve parmi deux sur un règlement `POINTED` → 204.
- Règlement : suppression de la dernière preuve d'un `POINTED` → 409, et la pièce est
  toujours là après coup.
- Règlement : la même suppression sur un `IN_WORK` → 204.
- **Non-régression** : l'objet reste présent dans le stockage après suppression (R2).

## 9. Impact front

**Additif — aucune rupture de contrat.** Deux routes nouvelles, aucune réponse existante
modifiée, aucun champ devenu obligatoire.

À noter côté `crm-prototype` quand la fonctionnalité y sera branchée :

- la réponse est **204 sans corps** — ne pas tenter de parser du JSON ;
- le **409** est propre au règlement et porte un `ProblemDetail` RFC 7807 dont le `detail`
  explique qu'un règlement pointé ne peut pas rester sans preuve. Il doit être affiché tel
  quel, pas transformé en erreur générique ;
- une pièce supprimée disparaît de `GET …/pieces-jointes` immédiatement, et son
  `…/fichier` renvoie 404 : invalider la liste après le `DELETE` suffit.
