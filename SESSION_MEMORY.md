# Session Memory
_Last updated: 2026-08-19T03:36:50Z_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\project\immo-crm_

---

## 🎯 Current Objective
Build out the immo-crm REST API (Spring Boot 4.1.0/Java 21) on top of a legacy SQL Server schema: document generation (PDF/Word), Dossier status workflow + traceable history, and reference-data endpoints — always deriving business rules from the real `yesRems` database, never guessing.

---

## ✅ What Worked
- Empirical DB-first research via `sqlcmd -S "localhost\SQLEXPRESS,49687" -d yesRems -U tsix -P securePassword123 -C -Q "..."` — cross-referencing `StrFolderStatusHistory`, `ComFolder` date columns, and the legacy printed report to confirm field meanings before coding.
- OpenPDF (`org.openpdf.text` package — NOT `com.lowagie.text`, that's the old v2 package) + Apache POI (XWPF) for dual PDF/Word document generation, no external process (no LibreOffice) needed.
- `AdmSequence` table (`Code = "Dossier de vente"`, field `SeqValue`) is the real legacy document-numbering counter — confirmed by matching it to the highest real `ComFolder.Code` sequence number.
- Backfilling bad production rows directly via `sqlcmd -i`/piped SQL (piping via `cat file | sqlcmd ...` — the `-i` flag fails with "Accès refusé" on these Git-Bash-translated Windows paths).

## ❌ What Failed
- `PdfWriter`/`Document`/etc. under `com.lowagie.text` → compile error "package does not exist". OpenPDF 3.0.5 renamed the package to `org.openpdf.text`.
- `sqlcmd -i <path>` with the Git-Bash-mangled `/c/Users/...` scratchpad path → "erreur : une erreur s'est produite lors de l'ouverture ou de l'utilisation du fichier C: (Accès refusé)". Fix: pipe the file into stdin instead (`cat file.sql | sqlcmd ...`).
- Assuming `StkItem.CfgProjectId` holds the project link → always NULL in real data (confirmed `COUNT(*) WHERE CfgProjectId IS NOT NULL` = 0). Real path is `StkItem.StkHierarchyId → CfgTranche.CfgProjectId`.

---

## 🐛 Bugs & Errors Log

| # | Error / Bug | Status | Fix applied |
|---|-------------|--------|-------------|
| 1 | `PropertyReferenceException: No property 'dateDocument' found for type 'ComFolder'` on paginated GETs when client sorts by the JSON/DTO field name instead of the JPA entity property | Fixed | `GlobalExceptionHandler` now catches `org.springframework.data.core.PropertyReferenceException` (note: not `.mapping` — this Spring Data version moved the class) → clean 400 instead of 500 |
| 2 | `DossierService.create()` never set `ComFolder.Code` or `ComFolder.DocDate` | Fixed | Generates `Code` via the real `AdmSequence` counter (`"{ProjectCode} {année}/{5-digit num}"`), sets `DocDate = now()`; self-heals (creates the AdmSequence row) if it's ever missing rather than blocking creation |
| 3 | `DossierService.create()` never set `ComFolder.StkItemId` (main-article back-link) — only the reverse `StkItem.ComFolderId` was set, so `folder.getStkItem()` was always null for API-created dossiers, silently breaking `FicheReservationService`/`ActeReservationService` | Fixed | `create()` now also does `folder.setStkItem(articles.get(0))` |
| 4 | `FicheReservationService` / `ActeReservationService` looked up the projet via `StkItem.getCfgProject()` (always null) instead of `StkItem.getStkHierarchy().getCfgProject()` | Fixed | Both switched to the tranche path |
| 5 | 13 real `ComFolder` rows in `yesRems` had NULL `Code`/`DocDate`/`StkItemId` (created via API testing before fix #2/#3), including the user-reported "Karim Mazzi" dossier | Fixed | One-off backfill SQL script run directly against `yesRems`: assigned sequential real `Code`s (consumed `AdmSequence.SeqValue` 380→392), `DocDate = now()`, and `StkItemId` (lexicographically-smallest linked article per dossier). Karim Mazzi's dossier is now `APF 2026/00390`. |
| 6 | `PATCH /api/dossiers/{id}/statut` updated `ComFolder.StrStatusId` but never inserted a row into `StrFolderStatusHistory` — status changes made via the API were invisible to the audit trail (unlike legacy desktop-app changes) | Fixed | `changerStatut()` now inserts a `StrFolderStatusHistory` row (`Description` = the request's `note`) on every transition |
| 7 | `DossierService.create()` never set an initial `StrStatusId` — new dossiers had `statut = null` until a separate `PATCH .../statut` call | Fixed | Confirmed 100% of real dossiers (359/359 with history) start at RESERVATION — `create()` now sets it automatically + records the matching `StrFolderStatusHistory` row; self-heals (skips, doesn't block) if the StrStatus row is missing |

---

## 🔑 Key Points to Remember
- **Workflow rule (explicit, standing)**: write/edit code + tests, run `mvn compile` / `mvn test-compile` only — **never** `mvn test`. The user runs the actual test suite themselves.
- **pom.xml**: never add a dependency without asking first. `poi-ooxml` 5.5.1 and `openpdf` 3.0.5 are already added (user did it after I asked).
- **Dossier status milestone fields** (`domain/EtapeMilestone.java`) — confirmed via the legacy printed report + `StrFolderStatusHistory` correlation:
  - CONCRETISATION → `AdvanceDate`, MEILLEURTAUX → `LoanCompanyDecisionDate`, COMPROMIS → `CreditApprovalDate`, ENVOI AU NOTAIRE → `AgreementDate`, MINUTE → `AgreementSavingDate`, REMISE DES CLEFS → `DeliveryDateReal`, FORMALISE → `AgreementRegistryDate`, ANNULATION → `CancelDate`+`CancelDescription` (all confirmed).
  - CREDIT DEBLOQUE → `CreditPossessionDate` (plausible only — often left NULL even once reached).
  - RESERVATION, CREDIT EN COURS: no dedicated field (transient states). LIVRAISON PROVISOIRE: **0 occurrences ever** in `StrFolderStatusHistory` — unconfirmed/unused. DESISTEMENT: needs the separate `ComSaleWithdrawalCancellation` entity (reimbursement/reason/legalization) — explicitly out of scope so far.
- **Dossier numbering**: single global sequence (`AdmSequence`, `Code = "Dossier de vente"`), shared across ALL projects/prefixes, **never resets per year** (confirmed: codes count 360→361 straight across the 2025→2026 boundary). Format: `"{CfgProject.Code} {année}/{SeqValue, 5-digit zero-padded}"`.
- **Pièces jointes**: `AdmAttachedFile` has no declared FK to `ComFolder` — linkage is `CategoryId = ComFolder.PKey` + `Category LIKE 'Dossier de vente%'` (confirmed empirically, no FK exists in schema). `IsCheckedButNotAttached=1` rows are checklist placeholders with no real file — excluded from the list via a `hasFile` `@Formula`.
- **Échéancier "Catégorie"**: the real FK is `ComDocument.ComSettlementCategoryId`, NOT `StlSettlementTypeId` (always NULL on real rows) — this was a pre-existing bug fixed earlier in the project.
- **SQL Server JDBC type gotchas** (recurring bug class, hit twice already): `text` columns need `@JdbcTypeCode(SqlTypes.LONGVARCHAR)` not `LONGNVARCHAR`; `varbinary(max)` needs `@JdbcTypeCode(SqlTypes.VARBINARY)` not `@Lob`.
- Local test DB: `DB_HOST=localhost DB_NAME=yesRemsTest DB_USER=tsix DB_PWD=securePassword123`. Real-data queries: `sqlcmd -S "localhost\SQLEXPRESS,49687" -d yesRems -U tsix -P securePassword123 -C -Q "..."` (real DB is `yesRems`, not `yesRemsTest`).
- Document generation lives under `services/dossier/document/` (renderers) + `dto/dossier/*Data.java` (data records) + `services/dossier/*ReservationService.java` (data-gathering). Two documents done so far: **Fiche de réservation**, **Acte de réservation**. Remaining legacy templates not yet implemented: `ComProspectionFiche.pdf`, `ComSaleFicheBancaire.pdf`, `ComSaleFicheIntervention.pdf`, `NDA AYAD PRIVATE.pdf` (in `instructions/templates/`).
- No AYAD logo asset exists in the repo yet — Fiche/Acte de réservation PDFs render without it (colors/table/text only). Drop a PNG into `src/main/resources/` when available.
- **New dossier default status**: `RESERVATION`, set automatically by `create()` now (see bug #7).
- **`/api/clients` vs `/api/prospects`**: same underlying `CfgTier` table, no separate Client entity/table in the schema. "Client" = any `CfgTier` referenced by ≥1 `ComFolder.CfgCustomerId` (no dedicated flag, unlike `CfgTier.IsProspect` for prospects). `/api/clients/{id}` and `/api/clients/{id}/dossiers` deliberately delegate to `ProspectService`/`ProspectSubResourcesService` internally rather than duplicating logic — only the list endpoint has client-specific shape (`ClientListItemDto` with server-aggregated `nbDossiers`/`totalAPayer`/`totalRegle`/`totalReste`, computed via 3 grouped queries, never one call per row).

---

## 📁 Important Files & Paths
- `src/main/java/com/tsix/immocrm/domain/EtapeMilestone.java` — status→milestone-field mapping + confidence level.
- `src/main/java/com/tsix/immocrm/services/dossier/DossierService.java` — `create()` (Code/DocDate/StkItem generation), `changerStatut()` (milestone field + history write), `etapesSuivantes()`.
- `src/main/java/com/tsix/immocrm/services/dossier/DossierHistoriqueService.java` — unified traceable history (`GET /api/dossiers/{id}/historique`): merges STATUT / ECHEANCE / REGLEMENT / PIECE_JOINTE events in memory, paginated.
- `src/main/java/com/tsix/immocrm/services/dossier/document/` — `FicheReservationPdfRenderer`, `FicheReservationDocxRenderer`, `ActeReservationPdfRenderer`, `ActeReservationDocxRenderer`, `MontantFormatter`.
- `src/main/java/com/tsix/immocrm/entities/str/StrFolderStatusHistory.java`, `entities/adm/AdmSequence.java`, `entities/stk/StkBuilding.java`, `entities/stk/StkFloor.java` — newly mapped entities for existing (not created) DB tables.
- `src/main/java/com/tsix/immocrm/web/GlobalExceptionHandler.java` — now also handles `PropertyReferenceException` (400).
- Test files mirroring each feature: `DossierStatutEtapesTest`, `DossierHistoriqueTest`, `FicheReservationTest`, `ActeReservationTest`, `DossierCreateTest` (extended), `GlobalExceptionHandlerSortTest`, `ClientControllerTest`.
- `instructions/templates/*.pdf` — legacy report templates being ported one at a time.
- `src/main/java/com/tsix/immocrm/controller/client/ClientController.java`, `services/client/ClientService.java`, `dto/client/ClientListItemDto.java` — new `/api/clients[/...]` endpoints (list is the only new logic; detail/dossiers delegate to the Prospect services).

---

## 🔧 Environment & Config
- Windows 11, PowerShell primary / Git Bash (`Bash` tool) also used, esp. for `sqlcmd` and `sed`.
- SQL Server: `localhost\SQLEXPRESS,49687`, DB `yesRems` (real data, read carefully) vs `yesRemsTest` (used for `mvn test`).
- `sqlcmd` output flags used throughout: `-C -Q "..." -W -s"|"`.
- Java 21, Spring Boot 4.1.0, Maven wrapper (`./mvnw` / `mvnw.cmd`).

---

## ⏭️ Next Step
> Continue porting the remaining legacy document templates one at a time (same pattern as Fiche/Acte de réservation: PDF via OpenPDF + Word via POI). Suggested next: **FICHE BANCAIRE** (`ComSaleFicheBancaire.pdf`) — shortest, fewest variable fields.

### Backlog (optional)
- [ ] `ComSaleFicheBancaire.pdf` → Fiche bancaire generator
- [ ] `ComSaleFicheIntervention.pdf` → Fiche intervention generator
- [ ] `ComProspectionFiche.pdf` → Fiche prospection generator
- [ ] `NDA AYAD PRIVATE.pdf` → NDA generator
- [ ] Decide whether to enforce any milestone fields as hard preconditions on status transitions (currently informational only — nothing blocks a transition for a missing field)
- [ ] Consider implementing DESISTEMENT properly via `ComSaleWithdrawalCancellation` (separate feature, currently out of scope)
- [ ] Consider adding an AYAD logo asset for the PDF/Word renderers
- [ ] Open question from earlier session, still unresolved: `GET /api/commerciaux`'s real data source (`CfgTier` filtered by `CfgTierKind` vs. some `Adm*` table)
