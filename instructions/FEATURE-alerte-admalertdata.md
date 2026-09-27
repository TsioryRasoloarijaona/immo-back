# FEATURE — Alerte de relance : le mécanisme du légataire (`AdmAlertData`)

Branche : `feat/alerte-admalertdata` (coupée depuis `main` le 26/09/2026).

## Objectif

Qu'une alerte de relance posée ou éteinte depuis notre API se comporte **exactement** comme
dans OMRAN : visible dans son écran, éteinte par la même action, et réciproquement. Avant ce
lot, une interaction à alerte créée par l'API n'existait pas pour OMRAN et l'alerte
précédente qu'elle aurait dû éteindre restait affichée — dans OMRAN comme dans notre liste.

## Ce que fait OMRAN

### Où vit l'alerte

Deux endroits, et ce n'est pas le premier que l'écran lit :

- `ComActionMessage.AlertValidityDate` / `AlertMessage` — l'interaction porte sa date et son
  texte de relance ;
- **`AdmAlertData`** — une ligne par interaction (`RelatedKey = ComActionMessage.PKey`,
  0 doublon sur 39 599 lignes), avec `Active` (bit). C'est **elle** que lit la vue
  `ComSaleProspectionView` : `LastActiveAlertDate = MAX(AlertDate) WHERE Active = 1 AND
  AlertDate <= GETDATE() + DefaultAlertDelai` (paramètre `AdmParamsInteger`, valeur 0).

Une alerte future est donc cachée jusqu'à son jour ; une alerte échue reste affichée tant
qu'elle est `Active = 1`.

Format constant, mesuré sur la source de vérité (`192.168.11.115`, base `YesRems`, 26/09/2026) :

| Colonne | Valeur | Mesure |
|---|---|---|
| `Description` | `Alerte` | 38 041 / 38 041 (prospection) |
| `Groupping` | `Prospection` (dossiers : `Dossier de vente (Réservation)`, `(Livraison)`) | — |
| `Label` | `NULL` | 39 599 / 39 599 |
| `Code` | `Pkey` en minuscules | 39 599 / 39 599 |
| `AlertDate` | `= AlertValidityDate` | 37 923 / 38 041 |
| `Message` | `= AlertMessage` | 37 926 / 38 041 |
| `RightKey` | `69f8e7d6-1111-1111-1111-1aa8b1f02008` (catégorie « communication ») | 38 041 / 38 041 |

Aucun trigger, aucune procédure ne touche `AdmAlertData` : **c'est le client Windows qui
l'écrit.**

### Les trois ordres (trace d'audit `C:\backup\*.sqlaudit`)

11 occurrences identiques entre le 11/09 et le 26/09/2026, envoyées à l'enregistrement d'une
interaction **qui porte une date d'alerte** :

```sql
-- 1. les relances précédentes de la prospection sont désarmées
UPDATE ComActionMessage SET WithoutRevival=1
WHERE (NOT AlertValidityDate IS NULL) AND (ComDocumentId IS NULL)
  AND (ComProspectionId = '<prospection>') AND (IsUnpaid=0)
  AND (PKey <> '<nouvelle interaction>')

-- 2. leurs alertes sont éteintes
UPDATE AdmAlertData SET Active=0 WHERE RelatedKey IN (
  SELECT TT.PKey FROM (SELECT ComActionMessage.*, ISNULL(StrStatus.Sorting,'-1') AS Sorting
                       FROM ComActionMessage LEFT OUTER JOIN StrStatus ON …) AS TT
  WHERE (TT.Sorting <= -1) AND (TT.ComDocumentId IS NULL) AND (TT.ComProspectionId = '<prospection>'))

-- 3. la nouvelle alerte est posée
INSERT INTO AdmAlertData(code,description,message,groupping,alertDate,relatedKey,rightKey,active,pKey)
```

`ISNULL(Sorting,-1) <= -1` ne retient que les interactions **sans statut de dossier**
(tous les `StrStatus.Sorting` réels valent 1 ou plus) : on l'écrit `StrFolderStatusId IS NULL`.
Une variante `<= 1` a été vue une fois (11/09, écran non identifié) ; elle n'est pas
reproduite.

La transformation en vente et l'archivage envoient les ordres 1 et 2 sans le 3
(`FEATURE-desarchiver-prospection.md`).

### La règle, vérifiée en direct

Rejouée le 26/09/2026 dans l'OMRAN local (prospection `26/05941`) :

| Heure | Saisie | Ordres envoyés | Effet |
|---|---|---|---|
| 14:33 | note, alerte au 26/09 | 1, 2, 3 | alerte A posée (`Active = 1`) |
| 14:35 | note **sans** alerte | `INSERT ComActionMessage` seul | **A reste active** |
| 14:36 | note, alerte au 27/09 | 1, 2, 3 | A éteinte (`WithoutRevival = 1`, `Active = 0`), B posée |

**C'est la prochaine interaction porteuse d'une date d'alerte qui éteint la précédente.**
Ni la date de l'alerte ni une note sans alerte n'y changent rien, et aucun processus n'éteint
une alerte par le temps. Sur la source :

| Situation | `Active = 1` | `Active = 0` |
|---|---:|---:|
| action postérieure **avec** alerte | 8 | 26 263 |
| action postérieure **sans** alerte | 63 | 3 473 |
| aucune action postérieure | 3 729 | 3 796 |

Les deux drapeaux vont ensemble : `Active = 0` et `WithoutRevival = 1` coïncident sur
33 416 des 33 530 alertes inactives. 3 666 alertes échues sans action derrière sont toujours
affichées.

## Ce que fait maintenant le backend

`services/prospection/AlerteRelanceService` :

- `poserAlerte(interaction)` — appelé par `POST /api/prospections/{id}/interactions` **si et
  seulement si** `dateAlerte` est renseignée : ordres 1, 2 puis 3. Sans `dateAlerte`, rien
  n'est touché.
- `desarmerToutes(prospectionId)` — transformation en vente (`DossierService.create`) et
  `PATCH /api/prospections/{id}/archiver` : ordres 1 et 2.

Les alertes et les interactions désarmées sont modifiées **entité par entité**, pas par
`UPDATE` de masse : chaque ligne passe ainsi par le collecteur Hibernate
(`CollecteurLignesEcrites`) et redescend dans la copie de lecture avec la transaction, sans
attendre le cycle de synchronisation. `AdmAlertData` est ajoutée à `ORDRE_CLES_ETRANGERES`
après `ComActionMessage`.

`ComActionMessageRepository.dernieresAlertes` (colonnes `derniereAlerteDate` /
`derniereAlerteMessage` de `GET /api/prospections`) **ignore désormais les relances
désarmées** (`ISNULL(WithoutRevival, 0) = 0`). La liste servie par la vue légataire
(`LastActiveAlertDate`) et le drapeau `alerteActive` du suivi d'actions lisent déjà
`AdmAlertData` : ils deviennent justes par le seul fait d'écrire la table.

Nouveaux fichiers : `entities/adm/AdmAlertData`, `repositories/adm/AdmAlertDataRepository`,
`services/prospection/AlerteRelanceService`, test
`controller/prospection/ProspectionInteractionAlerteTest` (7 cas : format de la ligne,
extinction de la précédente, impayé épargné, note sans alerte inerte, liste, désarmée
ignorée, archivage).

## Impact front

Aucun changement de contrat. `derniereAlerteDate` / `derniereAlerteMessage` deviennent
`null` sur une prospection dont la dernière relance a été désarmée (vendue, archivée, ou
remplacée) — c'était déjà le sens documenté du champ, la valeur était simplement fausse.

## Données : les deux bases, dans cet ordre

`AdmAlertData` **n'est pas sous change tracking** sur la source (`ComActionMessage` l'est).
Conséquence déjà vraie avant ce lot : tout ce qu'OMRAN écrit dans `AdmAlertData` depuis le
montage deux-bases reste sur la source, les copies des VPS sont figées à la copie initiale.

1. **Sur la source 115**, activer le suivi — la table a sa clé primaire (`PK_AdmAlertData`) :
   ```sql
   ALTER TABLE dbo.AdmAlertData ENABLE CHANGE_TRACKING WITH (TRACK_COLUMNS_UPDATED = OFF);
   ```
   La synchronisation lit la liste des tables dans `sys.change_tracking_tables` : aucun
   changement de code, le cycle suivant fait une ligne de base (39 599 lignes, sans
   suppression) puis passe en delta.
2. **Sur la source 115**, appliquer `db/migration/012_AdmAlertData_rattrapage_api.sql` : les
   12 interactions à alerte écrites par l'API (septembre 2026, 9 prospections) reçoivent
   leur ligne `AdmAlertData`, et la règle du légataire est rejouée rétroactivement sur ces
   9 prospections (une seule alerte active, la plus récente, `IsClosed = 0`). Attendu :
   8 `WithoutRevival` posés, 4 alertes éteintes, 12 lignes créées dont 7 actives.
3. **Une synchronisation** suffit ensuite pour les deux copies. Rien à rejouer sur les VPS.

Étapes 1 à 3 **faites le 26/09/2026** (compteurs obtenus : 9 / 8 / 4 / 12, table à 39 611 lignes).

4. **Second rattrapage, `013_AdmAlertData_prospections_fermees.sql`** (27/09) : 583 prospections
   **fermées** portaient encore une alerte active (582 relances armées), un résidu du légataire —
   123 alertes antérieures à la fermeture qui ont échappé au désarmement, 460 posées **après**
   l'archivage par des commerciaux qui ont continué à appeler des prospects archivés
   « INJOIGNABLE » / « NON INTÉRESSÉ » (358 fois plus de 30 jours après). Le script applique le
   prédicat d'archivage d'OMRAN à toute prospection `IsClosed = 1` ; `@garderAlertesFutures`
   permet d'épargner les 9 alertes à date future (toutes « NRP » à décembre 2026-2027). Attendu :
   582 `WithoutRevival`, 583 alertes éteintes, 4 891 → 4 308 actives.

## Hors périmètre

- Les alertes de dossier (`Groupping = 'Dossier de vente (…)'`, `ComDocumentId` non nul) —
  rappels de paiement, écran des règlements.
- La variante `Sorting <= 1`.
- Le doublon de code `26/05941` (bug 46, `AdmSequence`).
