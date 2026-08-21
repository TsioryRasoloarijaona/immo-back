# Session Memory
_Last updated: 2026-08-19T16:00:00Z_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\project\immo-crm_

---

## 🎯 Current Objective
Build out the immo-crm REST API (Spring Boot 4.1.0/Java 21) on the legacy `yesRems` SQL Server schema: document generation (PDF/Word), Dossier status workflow + history, reference-data endpoints, hotline/caller-lookup, a visite/rendez-vous calendar, and a full JWT-httpOnly-cookie auth system (reusing legacy `AdmUser`/`AdmRole`) — always deriving business rules from the real DB, never guessing.

---

## ✅ What Worked
- Empirical DB-first research via `sqlcmd -S "localhost\SQLEXPRESS,49687" -d yesRems -U tsix -P securePassword123 -C -Q "..." -W -s"|"` to reverse-engineer undocumented columns/tables before writing code (e.g. confirming "Le prospect veut faire une visite du projet" = 5708 real uses = the visite-triggering résultat).
- Generalizing an existing mechanism instead of building a parallel one: the visite calendar reuses the same `AdmEvent`/`dateRdv` machinery already built for RDV, distinguished only by `resultatCode` — no new field, no API break.
- Reusing legacy `AdmUser`/`AdmRole`/`AdmUserRole` as the auth backbone instead of a parallel user system — one source of truth for accounts/roles, at the cost of a bootstrap step (legacy `Pass` unusable).
- Diagnosing "TransientPropertyValueException" test failures by getting the **full** stack trace (`grep "at com.tsix"`, not just `Caused by:`) to pinpoint the exact originating line — jumping straight to root cause (`save()`-then-reuse-stale-reference pattern) instead of guessing.
- Running `mvn test -Dtest=ClassName` (targeted) to verify a single fix fast, before re-running the full suite.

## ❌ What Failed
- Initial assumption "Résultat" = `ComActionMessageSubCategoryStatusId IS NOT NULL` rows → wrong (unrelated `FICHE INTERVENTION` concept); correct scope is `ComActionMessageCategory.Code = "Prospection"`.
- `spring-security-test` (raw artifact) → `@WithMockUser` silently did nothing, everything 401'd. This project's Boot-4 convention needs **`spring-boot-starter-security-test`** (matches `spring-boot-starter-webmvc-test` already in the pom).
- Injecting `ObjectMapper` in a custom filter → no such autowireable bean under `spring-boot-starter-webmvc` (Boot 4's restructured web module), killed the whole Spring context. Fixed by hand-writing the tiny fixed-shape JSON body instead.
- `Comparator.comparing(x, nullsLast(naturalOrder())).reversed()` → `.reversed()` flips null placement too (nulls end up FIRST, not last). Use `nullsLast(reverseOrder())` directly instead.
- Trusting CLAUDE.md's `ddl-auto=validate/update` claim → actually `none` on both DBs (doc was stale, now fixed). Caused massive latent schema drift on `yesRemsTest` that only surfaced once the full suite finally ran end-to-end.

---

## 🐛 Bugs & Errors Log

| # | Error / Bug | Status | Fix applied |
|---|-------------|--------|-------------|
| 1 | No phone-first caller lookup existed | Fixed | `GET /api/hotline/recherche?telephone=...`, format-agnostic via `PhoneNumberUtils.normalize()` + JPQL `right(replace(...))` |
| 2 | Interaction DTOs had no Action/Résultat/RDV fields | Fixed | Extended `CreerInteractionRequest`/`InteractionDto`; mapped previously-unused `ComActionMessage` FKs |
| 3 | No "Résultat" referentiel endpoint | Fixed | `GET /api/referentiels/resultats-interaction` (23 rows) |
| 4 | Visite planning had no calendar surface | Fixed | `GET /api/calendrier/evenements?debut=&fin=`, reuses `AdmEvent`/`dateRdv` |
| 5 | No auth system for the API at all | Fixed | JWT httpOnly cookie, 3 phases — see Key Points |
| 6 | `OnboardingGateFilter` injected non-existent `ObjectMapper` bean → whole Spring context died | Fixed | Hand-write the JSON body, no Jackson dependency |
| 7 | `@WithMockUser` had no effect (wrong test dependency) | Fixed | Swapped `spring-security-test` → `spring-boot-starter-security-test` |
| 8 | `GenerateBootstrapHashTest` always fails under plain `mvn test` (needs `-Dpassword=`) | Fixed | Renamed to `GenerateBootstrapHashTool` (no "Test" suffix, skipped by Surefire's default pattern) |
| 9 | `yesRemsTest` missing ~13 tables/columns that earlier-session entities expected (`ddl-auto=none`, nothing auto-creates) | Fixed | Hand-generated DDL from `yesRems` real schema, applied via `sqlcmd` (I have DDL rights on `yesRemsTest`, not on `yesRems`) |
| 10 | `DossierFinancingSchemaTest`: `JsonPath.read(...".montant[0]")` → `ClassCastException` (returns `JSONArray` not `Number`) | Fixed | Read as `JSONArray` then `.get(0)`, matching the already-correct `fraisExclu` pattern |
| 11 | `DossierCreateTest`/`DossierHistoriqueTest`: `TransientPropertyValueException` on `CfgTranche`/`StrStatus` | Fixed | Classic Spring Data JPA trap: manually-assigned `@Id` → `save()` calls `merge()` (id already non-null), which returns a **new** managed instance — tests discarded the return value and reused the stale original when setting the association elsewhere. Fix: `x = xRepository.save(x)`, always use the returned reference. |
| 12 | `DossierHistoriqueTest.getHistorique_...`: wrong sort order (null-dated event sorted first) | Fixed | Real app bug in `DossierHistoriqueService` — `nullsLast(naturalOrder()).reversed()` → `nullsLast(reverseOrder())` |
| 13 | `DossierStatutEtapesTest`: 400 instead of 200 on transition to ANNULATION | Fixed | Test fixture only seeded the *starting* `StrStatus`, never the target — added `newDossierStatus("ANNULATION")` |
| 14 | `HotlineControllerTest`: `ComFolder.stkVocation` `@NotNull` violated on persist | Fixed | Fixture predates that constraint (added in an earlier session) — gave the test `ComFolder` a `StkVocation` |

**Current state: 165/165 tests green, `BUILD SUCCESS`.**

---

## 🔑 Key Points to Remember

### Schema / DB process (critical, bit twice this session)
- `spring.jpa.hibernate.ddl-auto=none` on **both** `src/main/resources/application.properties` and `src/test/resources/application.properties` (CLAUDE.md now corrected — it used to wrongly say `validate`/`update`). **Nothing auto-creates on either DB.** Any new table/column (legacy-mirroring or app-only) needs a hand-run SQL script on `yesRems` (real) **and** `yesRemsTest` (test), or tests will fail with "Nom d'objet non valide"/"Nom de colonne non valide" the moment that code path actually runs.
- `tsix` (the DB login used throughout) has `db_datareader`/`db_datawriter` **only** on `yesRems` — no DDL rights there, the user must run schema scripts on `yesRems` themselves (via a higher-privilege login). `tsix` **does** have DDL rights on `yesRemsTest` — I can run `CREATE TABLE`/`ALTER TABLE` there myself via `sqlcmd`.
- **If `yesRemsTest` is ever reset/recreated**, a lot of schema needs re-applying: `AdmUser`/`AdmRole`/`AdmUserRole`/`AppUserCredential` (auth) + `ComActionMessageCategory`/`ComActionMessageSubCategory`/`AdmEvent`/`StkBuilding`/`StkFloor`/`AdmSequence`/`AdmAttachedFile`/`StlQuote`/`StrFolderStatusHistory`/`StrHistory` (earlier sessions) + several ALTER-ed columns on `ComActionMessage`/`ComDocument`/`StkItem`/`ComProspectionSelectedProfile`. No single script file was kept — re-derive per-table from `yesRems` INFORMATION_SCHEMA.COLUMNS the same way (pattern used throughout this transcript) if it recurs.
- **Spring Data JPA + manually-assigned IDs gotcha** (bit 3 different tests this session): when an entity has `@Id` with no `@GeneratedValue` and the ID is set before `save()`, Spring Data sees a non-null ID and calls `entityManager.merge()` instead of `persist()` — `merge()` returns a **new** managed instance, the original object stays a detached/"transient" POJO forever. Always do `x = xRepository.save(x)` and use the **returned** reference for any further association-setting, or a later flush can throw `TransientPropertyValueException`. This pattern is used correctly in most of the codebase already; only a few tests got bitten because of specific flush-ordering (a subsequent query elsewhere happened to "launder" the reference).

### Auth (JWT httpOnly cookie) — 3 phases, all done
- **Design decisions locked in** (see transcript for the full discussion before implementing): reuse legacy `AdmUser`/`AdmRole`/`AdmUserRole` as-is for accounts/roles (`AdmUser.Pass` is NOT reusable — legacy encryption, unknown algorithm/key); new state lives in one app-only table `AppUserCredential` (1-1 with `AdmUser`: bcrypt `PasswordHash`, `MustChangePassword`, `ProfileCompleted`, `TokenVersion`); authorization reuses `AdmRole.Code` directly (`hasRole('Administrateur')`), no simplified role-mapping layer.
- **New required env var**: `JWT_SECRET` (in CLAUDE.md's env var list now). Test properties hardcode a fixed test secret.
- **Bootstrap account** on both DBs: username `tsiory`, password `securePassword123`, `PKey=C3ADB02D-CA0D-4E65-9924-C42FE48721AB` (same both sides), `MustChangePassword=0`/`ProfileCompleted=1` (fully usable immediately, bypasses onboarding).
- **Login** (`POST /api/auth/connexion`): always returns the identical "Identifiants invalides" for unknown username / wrong password / disabled account / no credential row — never reveals which (no user enumeration). JWT claims: `sub`=AdmUserId, `username`, `roles`, `employeeId` (CfgTier id), `mustChangePassword`, `profileCompleted`, `tokenVersion`. Cookie `access_token`, httpOnly, `SameSite=Strict`, `Secure` driven by `app.jwt.cookie-secure` (true prod / false test — Secure cookies don't send over plain HTTP).
- **`JwtAuthenticationFilter`**: reads the cookie, verifies signature/expiry, cross-checks `tokenVersion` claim against `AppUserCredential.TokenVersion` in DB (stale → silently unauthenticated, not an error), populates `SecurityContextHolder` with an `AuthenticatedUser` principal (`@AuthenticationPrincipal`-injectable) + `ROLE_<AdmRole.Code>` authorities.
- **`OnboardingGateFilter`**: while `mustChangePassword` or `!profileCompleted`, blocks everything except `/api/auth/**`/`/api/mon-compte/**` with 403 + `ProblemDetail.code` = `MOT_DE_PASSE_TEMPORAIRE`/`PROFIL_INCOMPLET`. **Only fires for the real `AuthenticatedUser` principal** — `@WithMockUser` tests (different principal type) sail through untouched.
- **CORS preflight** (`OPTIONS`) explicitly `permitAll()` via `CorsUtils::isPreFlightRequest` — real browsers never send cookies on preflight; without this, real cross-origin calls would 401 in prod (caught before shipping, not covered by the existing `CorsConfigTest` since `@WithMockUser` authenticates regardless of HTTP method).
- **`/api/mon-compte`**: `GET` (profile+roles+flags), `POST /changer-mot-de-passe` (bcrypt, ≥8 chars, clears `mustChangePassword`, bumps `TokenVersion`, reissues cookie), `PATCH /profil` (fills `CfgTier.{FirstName,LastName,Mobile,EMail,BirthDate}` + optional `Address` — **adresse deliberately excluded** per user confirmation; 400 if `AdmUser` has no linked `CfgTier`, e.g. `notaire`).
- **Phase 3 — admin reset**: `POST /api/utilisateurs/{admUserId}/reinitialiser-mot-de-passe`, `@PreAuthorize("hasRole('Administrateur')")` (first role-specific check anywhere in the app; `@EnableMethodSecurity` added to `SecurityConfig` for this). Generates a 12-char temp password (unambiguous charset, no 0/O/1/I/l), returns it once in the response body, sets `MustChangePassword=true`, bumps `TokenVersion` (kills every existing session for that account). Creates the `AppUserCredential` row if the legacy account never had one.
- **Logout does NOT bump `TokenVersion`** (deliberate) — only clears the cookie client-side, other devices/sessions stay valid. Only password change/admin reset does a real "invalidate everywhere".
- **Full API lockdown** (`anyRequest().authenticated()`) — confirmed explicitly with the user given the blast radius. Required retrofitting **33 pre-existing test classes** with `@WithMockUser` (scripted sed pass, not manual) — `AuthControllerTest` (tests unauthenticated `/api/auth/**`) and `MonCompteControllerTest`/`UtilisateurAdminControllerTest` (need the real principal or role-based `@WithMockUser(roles=...)`) were handled differently.
- **`GenerateBootstrapHashTool`** (renamed from `...Test`, `security/` package): dev tool, not a real test — `mvnw.cmd test -Dtest=GenerateBootstrapHashTool#printHash -Dpassword=...` to mint a bcrypt hash for manual SQL inserts.

### Visite/RDV calendar
- "Résultat" that triggers a visite: `ComActionMessageSubCategory.Code = "Le prospect veut faire une visite du projet"` (5708 real uses). No separate Visite table — reused the same `AdmEvent`/`dateRdv` mechanism as RDV, distinguished by `resultatCode` on the calendar DTO.
- `GET /api/calendrier/evenements?debut=&fin=` — both required (400 if missing/`fin<debut`), returns `List<EvenementCalendrierDto>`, never paginated. Pulls from `ComActionMessage` (not `AdmEvent` directly) for the prospection/prospect/commercial context for free.
- `ProspectionService`/`ReferentielService`: constant renamed `RESULTATS_AVEC_RENDEZ_VOUS` → `RESULTATS_AVEC_DATE_PLANIFIEE`, now 3 codes.

### Standing rules
- **Never run `mvn test` without being explicitly asked** — user runs the suite normally; `mvn compile`/`test-compile` only otherwise. This session the user explicitly asked me to run it (multiple times) to unblock the auth rollout — that's the exception, not the new normal.
- **pom.xml**: never add a dependency without asking first. This session added (with approval): `spring-boot-starter-security`, `spring-boot-starter-security-test`, `io.jsonwebtoken:jjwt-{api,impl,jackson}:0.12.6`.
- **OpenAPI docs required** on every new/substantially-edited endpoint (CLAUDE.md rule 10): `@Tag`, `@Operation`, `@Parameter`, `@ApiResponses`, `@Schema` — French, matching business language.
- **"Résultat" referentiel**: contains one likely-anomalous row `MEILLEURTAUX` (2 uses) — left in deliberately, not excluded server-side (open question with user, still unconfirmed).
- Local test DB: `DB_HOST=localhost DB_NAME=yesRemsTest DB_USER=tsix DB_PWD=securePassword123`, plus now `JWT_SECRET=<anything>` for tests too (though test properties hardcode their own, so any value works for tests — only matters for running the real app).

---

## 📁 Important Files & Paths

**Auth (all new)**:
- `entities/adm/{AdmUser,AdmRole,AdmUserRole}.java`, `entities/app/AppUserCredential.java` + matching repositories.
- `security/{SecurityConfig,JwtService,JwtAuthenticationFilter,OnboardingGateFilter,AuthenticatedUser}.java`.
- `exceptions/UnauthorizedException.java`.
- `dto/auth/{ConnexionRequest,UtilisateurCourantDto,ChangerMotDePasseRequest,CompleterProfilRequest,ReinitialiserMotDePasseResponse}.java`.
- `services/auth/{AuthService,MonCompteService,UtilisateurAdminService}.java`.
- `controller/auth/{AuthController,MonCompteController,UtilisateurAdminController}.java`.
- Tests: `controller/auth/{AuthControllerTest,MonCompteControllerTest,UtilisateurAdminControllerTest}.java`, `entities/adm/AdmUserAuthEntitiesTest.java`, `security/GenerateBootstrapHashTool.java` (dev tool).
- 33 pre-existing test classes: `@WithMockUser` retrofit (see git diff for full list).

**Calendar (all new)**: `dto/calendrier/EvenementCalendrierDto.java`, `services/calendrier/CalendrierService.java`, `controller/calendrier/CalendrierController.java`, `controller/calendrier/CalendrierControllerTest.java`.

**Hotline (all new)**: `utils/PhoneNumberUtils.java`, `controller/hotline/HotlineController.java`, `services/hotline/HotlineService.java`, `dto/hotline/{HotlineResultDto,HotlineFicheDto}.java`.

**This session's bug fixes** (existing files, not new): `services/dossier/DossierHistoriqueService.java` (comparator fix), `test/.../dossier/{DossierCreateTest,DossierHistoriqueTest,DossierStatutEtapesTest,DossierFinancingSchemaTest}.java`, `test/.../hotline/HotlineControllerTest.java`.

**Modified**: `pom.xml` (security + jjwt deps), `application.properties` (main+test, `app.jwt.*` keys), `CLAUDE.md` (rule 10 OpenAPI standard, `JWT_SECRET` env var, `ddl-auto` doc fix).

**Carried over, unchanged this session**: `domain/{EtapeMilestone,StatutDossier,TransitionsValidees}.java`, `services/dossier/{DossierService,document/*}`, `entities/adm/AdmSequence.java`, `web/GlobalExceptionHandler.java`, `controller/client/ClientController.java`.

---

## 🔧 Environment & Config
- Windows 11, PowerShell primary / Git Bash (`Bash` tool) for `sqlcmd`/`sed`/scripting.
- SQL Server `localhost\SQLEXPRESS,49687` — `yesRems` (real data) vs `yesRemsTest` (test, no DDL rights issue for `tsix`).
- Java 21, Spring Boot 4.1.0, Maven wrapper.
- **`mvn test` run and green: 165/165, `BUILD SUCCESS`** as of end of session.

---

## ⏭️ Next Step
> Auth is feature-complete (Phases 0-3) and the full suite is green. Natural next move: resume the document-template backlog (Fiche bancaire generator), or ask the user what's next — nothing is currently blocking.

### Backlog
- [ ] `ComSaleFicheBancaire.pdf` → Fiche bancaire generator (shortest/fewest fields, natural next doc template)
- [ ] `ComSaleFicheIntervention.pdf` → Fiche intervention generator (note: `ComActionMessageSubCategoryStatus`, discovered but unmapped, likely belongs here)
- [ ] `ComProspectionFiche.pdf` → Fiche prospection generator
- [ ] `NDA AYAD PRIVATE.pdf` → NDA generator
- [ ] Frontend needs a calendar view consuming `GET /api/calendrier/evenements` — no per-commercial filtering yet, confirm if needed before building speculatively
- [ ] Frontend integration note: poll `GET /api/mon-compte` on app load to route to password/onboarding/normal screens; any 403 with `ProblemDetail.code` mid-session means cached state is stale, same redirect logic applies
- [ ] Confirm with user whether `MEILLEURTAUX` should be excluded server-side from `GET /api/referentiels/resultats-interaction`
- [ ] Decide whether to enforce milestone fields as hard preconditions on Dossier status transitions (currently informational only)
- [ ] Consider implementing DESISTEMENT properly via `ComSaleWithdrawalCancellation` (separate feature, out of scope so far)
- [ ] Consider an AYAD logo asset for PDF/Word renderers
- [ ] Open question, still unresolved: `GET /api/commerciaux`'s real data source (`CfgTier`+`CfgTierKind` vs. some `Adm*` table)
