# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

`immo-crm` (internally "AyadERP") is a Spring Boot 4.1.0 (Java 21) backend for a real-estate CRM — sales folders/dossiers, prospection, prospects/clients, patrimoine (projects/tranches/lots), settlements. It connects to an existing Microsoft SQL Server database; the entity layer mirrors a pre-existing legacy schema rather than one generated from the code.

## Build & run


```bash
./mvnw compile              # compile
./mvnw test                 # run all tests
./mvnw test -Dtest=ImmoCrmApplicationTests#contextLoads   # run a single test
./mvnw spring-boot:run       # run the app
./mvnw clean package         # build jar
```

On Windows use `mvnw.cmd` instead of `./mvnw`.

The app requires a running SQL Server instance and these environment variables (no defaults are set):
```
DB_HOST, DB_NAME, DB_USER, DB_PWD, JWT_SECRET
```
`ImmoCrmApplicationTests` boots the full Spring context (`@SpringBootTest`), so these must be set (or a local SQL Server reachable) even to run tests. `spring.jpa.hibernate.ddl-auto=none` in both `src/main/resources/application.properties` and `src/test/resources/application.properties` — the schema is owned by the DB on both sides; new app-only tables (e.g. `AppUserCredential`) are created by hand-written SQL scripts, not by Hibernate.

## Architecture

- **Entities mirror an existing DB schema.** Every entity's `@Table`/`@Column` names are PascalCase matching the SQL Server table/column names exactly (e.g. `CfgTranche`, `PKey`, `StkVocationId`). Primary keys are `UUID` mapped from a `PKey` column. Do not rename columns/tables when adding fields — check the actual DB schema (see `.idea/dataSources.xml` for the configured local `yesRems` DB connection) rather than guessing.
- **Package layout**, under `com.tsix.immocrm`:
  - `entities/{cfg,com,stk,stl}` and `repositories/{cfg,com,stk,stl}` — mirror the legacy DB module prefixes, one repository per entity.
    - `cfg` — configuration/reference data: `CfgProject`, `CfgTranche` (a phase/lot within a project), `CfgTier` (a third party — customer/prospect, supplier, lawyer, bank, etc.) and its `CfgTierKind`/`CfgTierType`, `CfgTierContact`.
    - `com` — commercial process: `ComFolder` ("Dossier" — a sales file, the central, widest entity, with dozens of date/amount milestone columns tracking a sale's lifecycle), `ComProspection`, `ComAction`/`ComActionMessage`, `ComDocument` (échéancier lines).
    - `stk` — stock/inventory: `StkItem` ("Article"/lot — can self-reference via `parent` for sub-lots), `StkItemType`, `StkItemCategory`, `StkVocation`.
    - `stl` — settlements/payments: `StlSettlement`, `StlSettlementLine`, `StlSettlementRelation`.
  - `services/`, `controller/`, `dto/` — organized by business domain (Prospection, Dossier, Prospect, Patrimoine), **never** by the `cfg`/`com`/`stk`/`stl` legacy prefix.
  - `domain/` — pure business logic (enums, transition tables), no JPA annotations here. Central pieces: `StatutDossier` (13-value dossier status enum) and `TransitionsValidees` (the allowed-transitions table enforced on `PATCH .../statut`).
- **Entity conventions** (follow exactly when adding/editing entities):
  - Lombok `@Getter @Setter` on the class — no `@Data`, no explicit constructors.
  - `@ManyToOne(fetch = FetchType.LAZY)` for all associations; add `@NotNull` + `optional = false` only where the DB column is non-nullable.
  - String columns use `@Nationalized` (maps to `NVARCHAR`) plus `@Size(max = ...)` matching the DB column length.
  - Monetary/ratio columns are `BigDecimal` with `precision = 26, scale = 6` (except lat/long and some rate columns using `precision = 20, scale = 16`).
  - Timestamps are `LocalDateTime`; boolean flags are `Boolean` (nullable wrapper, not primitive).
  - Reserved-word columns are quoted in the `name` attribute (e.g. `@Column(name = "\"Usage\"")`).
- **Naming mismatch to keep in mind**: the DTO/API layer uses French business names (`Dossier`, `Prospect`, `Article`, `Contact`) that map onto differently-named JPA entities (`ComFolder`, `CfgTier`, `StkItem`, `CfgTierContact` respectively) — don't rename the entities to match, translate at the DTO/mapping boundary.

## Git workflow (mandatory)

1. **Always branch from `main`.** Before starting a new feature, `git checkout main && git pull`, then create the branch from that up-to-date `main`. Never branch from another feature branch, and never start work directly on `main`.
2. **Ask before creating a branch.** Propose the branch name and wait for the user's go-ahead — don't create it unilaterally.
3. **Commit and push after every validated feature.** "Validated" = the feature is implemented *and* its tests run green (`./mvnw.cmd test -Dtest=...`, then the full suite when relevant). One coherent feature = one commit; push it immediately, don't let work pile up uncommitted across sessions.
4. **The user merges on GitHub.** After the push, stop — the user opens/merges the PR into `main` themselves. Don't merge, don't push to `main` directly.
5. **After the merge, pull `main` again** before starting the next feature, so the next branch is cut from the merged state.

## Endpoint implementation workflow (`instructions/CLAUDE-CODE-endpoints-implementation.md`)

This repo is mid-way through building out a REST API (~27 endpoints across Prospection, Prospect, Dossier, Patrimoine, Dashboard) on top of the entity layer. Rules that apply for the whole duration of that effort:

1. **Never modify `pom.xml` without asking first** — including for seemingly-obvious deps like `springdoc-openapi` or a mapping library. Stop and ask; never assume a dependency is "probably already there."
2. **One endpoint (or small coherent group) at a time**: implement (DTO + repository if needed + service + controller) → write the test → run it (`./mvnw test -Dtest=...`) → only move on once green. Don't chain multiple endpoint implementations before running the previous test.
3. **If a test fails for a non-obvious reason, don't loop on fixes** — report the blocker with the full error instead of pushing on with a broken base.
4. **`TransitionsValidees` (Dossier status) must be enforced in the service layer**, not just documented. Any transition outside the table returns `409 Conflict` via `ProblemDetail` (RFC 7807), body includes the allowed transitions.
5. **Minimum test coverage per endpoint**: happy path, a 404 on an unknown id, and — specifically for `PATCH .../statut` — an explicit 409 test for an invalid transition. Exhaustive edge-case coverage is not expected at this stage.
6. Recommended implementation order is dependency-based, not alphabetical: Phase 0 foundations (`domain/StatutDossier`, `domain/TransitionsValidees`, `domain/EtapeProspection`, missing repositories) → Phase 1 Patrimoine (simplest, no transition rules) → Phase 2 Prospect → Phase 3 Prospection → Phase 4 Dossier (densest, depends on everything before it) → Phase 5 Dashboard.
7. `GET /api/commerciaux`'s data source (`CfgTier` filtered by `CfgTierKind`, vs. some `Adm*` table) is an **open question** — stop and confirm before implementing it.
8. `GET /api/dossiers/{id}/montants` is always computed server-side, never stored; when implementing it, group by category and exclude `FRAIS` from the amount subject to the TTC price rule.
9. DTOs are `record`s; field names should follow `API-endpoints-vente-prospection-patrimoine.md` (not present in the repo yet — if it's still missing when you need it, ask rather than inventing field names) rather than being reinvented ad hoc.
10. **Every endpoint must be documented for OpenAPI/Swagger (`springdoc-openapi`, already on the classpath), not just implemented.** The goal is that a frontend dev can understand and consume an endpoint from `/swagger-ui.html` alone, without reading the Java. Concretely, for every new (or edited) controller/DTO:
    - `@Tag(name = ..., description = ...)` on the controller class, describing the resource/domain it covers.
    - `@Operation(summary = ..., description = ...)` on every handler method — `summary` is a short label, `description` explains what it does, any non-obvious business rules it enforces (e.g. transition validation, computed fields, exclusions), and what an empty/absent result means.
    - `@Parameter(description = ..., example = ...)` on every `@RequestParam`/`@PathVariable` that isn't self-evident from its name alone.
    - `@ApiResponses`/`@ApiResponse` documenting at least the non-200 cases (404, 400, 409...) with what triggers them.
    - `@Schema(description = ...)` on every DTO `record` and on its individual components — explain what the field represents and where it comes from (e.g. "computed server-side", "null if not yet reached"), not just repeat the field name.
    - Write descriptions in French, matching the DTO/API layer's existing business language (see the "Naming mismatch" note above).
    This applies going forward to all endpoints, not only new ones — retrofit an existing endpoint's docs if you're touching it substantially anyway, but don't go out of scope to backfill unrelated ones.
