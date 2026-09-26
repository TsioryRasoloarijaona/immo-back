# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

`immo-crm` (internally "AyadERP") is a Spring Boot 4.1.0 (Java 21) backend for a real-estate CRM — sales folders/dossiers, prospection, prospects/clients, patrimoine (projects/tranches/lots), settlements. It connects to an existing Microsoft SQL Server database; the entity layer mirrors a pre-existing legacy schema rather than one generated from the code.

## Scope: backend only (hard boundary)

**Never touch the frontend.** A session working in this repo is a backend agent. Do not edit
any file in `../crm-prototype/`, do not create a branch there, do not run its build or
`npm run docs:update`, and do not include frontend work in a plan or offer to do it.

This is a boundary, not a preference — the two repos are separate GitHub remotes with
separate PRs, and the frontend is a different lane.

**When a backend change breaks the API contract the frontend depends on** (a field becoming
required, an endpoint being re-scoped, a response shape changing), the job ends at *reporting*
it:

- state it precisely in the `instructions/FEATURE-*.md` spec — endpoint, changed fields, what
  now returns 400 — under an "Impact front" heading;
- make sure the springdoc annotations say the same thing, since `/api-docs` is the frontend's
  only source of truth (see rule 10 below);
- say it plainly in the final report to the user.

Then stop. Do not offer to fix it, do not ask whether you should also do the frontend, do not
cut a branch "in both repos". The spec and the OpenAPI snapshot are the handoff — a report to
the user is not.

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

## Interroger les données réelles (`yesRemsProd`)

**Toute étude de motif, vérification de schéma ou décision de mapping se fait contre la base
locale `yesRemsProd`** — pas contre `yesRemsTest`, qui est une base minimale bâtie à la main, ni
en devinant depuis le code.

```bash
sqlcmd -S localhost,49687 -U tsix -P securePassword123 -d yesRemsProd -C -Q "<requête>"
```

Sous PowerShell, `Invoke-Sqlcmd -ServerInstance "localhost,49687" -Database yesRemsProd
-Username tsix -Password securePassword123 -TrustServerCertificate` évite les problèmes de
chemin et de quoting de `sqlcmd.exe`.

Pourquoi cette base précisément :

- c'est **le schéma complet du légataire** — 462 tables, 216 vues, les triggers et les 1 219
  clés étrangères. `yesRemsTest` n'en contient que 79 tables sans contrainte, donc elle ne dit
  rien de ce que la production accepte ou refuse ;
- elle porte des **données réelles**, ce qui permet de mesurer avant de décider. Plusieurs choix
  de ce dépôt ont été pris ou annulés sur des taux de population, et c'est la règle : une
  colonne « qui a l'air bonne » se vérifie (`INFORMATION_SCHEMA.COLUMNS`, puis un `COUNT`).

Deux réflexes qui ont déjà évité des erreurs ici :

- **compter avant de conclure** — un total qui tombe juste (ex. la somme des statuts = le nombre
  de dossiers) prouve qu'aucune ligne ne se cache ailleurs ; un écart désigne le mécanisme ;
- **ne jamais mapper une colonne de vue** comme si c'était une colonne de table.

C'est une base de **lecture**. Aucune écriture, aucun `UPDATE` de confort : les corrections de
données se livrent en script SQL que l'utilisateur applique lui-même, sur cette base comme sur
`yesRemsDev` (le VPS).

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

## L'aide en ligne d'OMRAN, extraite : `instructions/REFERENCE-aide-omran.md`

Le site d'aide du légataire (https://help.omranestate.com/, 334 pages HelpNDoc) est
**recopié mot pour mot** dans `instructions/REFERENCE-aide-omran.md` : une section par
page, dans l'ordre de la table des matières du site, avec pour chaque écran ses tableaux
`Liste` (colonnes du masque de liste), `Fiche` / `Champs` (champs, **U**nique,
**O**bligatoire, observations), `Actions` (boutons et leurs effets) et ses `Remarque n :`
(règles de gestion). Les règles transverses (`GEN0001`…) sont sur la page « Règles de
gestion générale ». Outils de regénération et mode d'emploi : `instructions/aide-omran/`.

**C'est le premier endroit où chercher quand on reproduit une fonctionnalité d'OMRAN**, avant
la base et avant la trace d'audit :

1. Trouver la page de l'écran — `grep -n "^### <NOM D'ÉCRAN> <a id" instructions/REFERENCE-aide-omran.md`
   (les titres de page sont ceux du site : `PROSPECTIONS`, `DOSSIERS DE VENTE`,
   `TRANSITIONS`…) ou l'ancre `#page-<fichier-html-en-minuscules>`. La table des matières
   en tête du fichier donne le module de chaque page.
2. Lire dans cet ordre : `Fiche`/`Champs` (ce que l'écran saisit et ses valeurs par défaut),
   `Actions` (ce que chaque bouton déclenche), les `Remarque` (les règles), puis les pages
   de configuration liées (les listes déroulantes renvoient vers leur page `CONFIGURATION`).
3. **Citer les phrases du site telles quelles dans la spec `FEATURE-*.md`**, avec le nom de
   la page. Une règle reformulée perd ce qui la rend vérifiable.
4. Puis **confronter au réel** : la base (`yesRemsProd`, source 115) pour les colonnes et
   les taux de population, la trace d'audit (`C:\backup\*.sqlaudit`) pour les ordres SQL que
   l'écran envoie vraiment. Le site décrit le comportement *attendu* ; quand il diverge du
   comportement *observé*, la trace fait foi et l'écart se note dans la spec. Exemple
   vérifié le 26/09/2026 : la règle « Pour désactiver une alerte : on ajoute une nouvelle
   alerte dans le même écran parent » (GEN0005) est exactement ce que la trace montre, et
   la vue `ComSaleProspectionView` code l'affichage avec `<=` là où le site écrit `=`.

Ce que la référence ne contient pas : les captures d'écran (signalées par
`[image : Fichier.png]`, non transcrites) et tout ce qui n'est pas documenté par l'éditeur
(la page « Paramètres système » du site est vide). Ne pas la modifier à la main : la
regénérer avec les outils, **une requête à la fois** — une rafale parallèle a fait tomber le
serveur d'aide pendant plus d'une demi-heure.

## Knowledge graph (graphify)

`graphify-out/` holds a generated knowledge graph of this repo — `graph.json`,
`GRAPH_REPORT.md` (community map, god nodes, audit trail) and an interactive `graph.html`.
It is **gitignored and rebuilt automatically**: a `post-commit` hook re-runs AST extraction
on the files each commit touched, and a `post-checkout` hook rebuilds after a branch switch.

**The hooks only cover code.** After editing `instructions/**` or `instructions/templates/**`
(specs, legacy `.pdf`/`.html` report templates), run `/graphify . --update` by hand — those
go through LLM extraction, which the hook deliberately skips.

Reach for the graph when the question is **structural**:

- **Blast radius before a refactor** — `graphify explain "<Symbol>"` lists everything that
  references a type before you change it. (`CfgTier` carries 215 edges across 39 communities;
  that is worth knowing before touching it.)
- **Questions that span artifact kinds** — code ↔ `instructions/FEATURE-*.md` ↔ the legacy
  `instructions/templates/*.pdf|.html`. Grep cannot span those; the graph can.
- **Orienting in an unfamiliar area** — the community map in `GRAPH_REPORT.md`.
- **Coupling or duplication you have no name for yet** — god nodes and surprising connections.

Do **not** reach for it when:

- You already know the symbol or file — use Grep/Glob. Faster, exact, never stale.
- You are locating a feature by screen or business action — that is `../FEATURES.md`.
- You need the definitive list of files to edit. **Always confirm with Grep before editing.**
  The graph collapses repeated references into one edge and carries dangling edges to
  unresolved third-party symbols, so its counts are a lower bound, not an inventory.

In short: **the graph decides *what* to touch, Grep decides *which lines* to open.**
