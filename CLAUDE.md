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

The app requires a running SQL Server instance and three environment variables (no defaults are set):
```
DB_HOST, DB_NAME, DB_USER, DB_PWD
```
`ImmoCrmApplicationTests` boots the full Spring context (`@SpringBootTest`), so these must be set (or a local SQL Server reachable) even to run tests. Note `spring.jpa.hibernate.ddl-auto` differs between `src/main/resources/application.properties` (`validate` — the schema is owned by the DB) and `src/test/resources/application.properties` (`update`).

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
