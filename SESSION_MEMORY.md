# Session Memory — immo-crm
_Last updated: 2026-10-08 (end of day)_ · state and decisions only (≤ 3 KB); history = git log + FEATURES.md

## Current state

- `main` = 7de6a0a, Quality Gate SonarCloud **green** (306 → ~27 open, none security/reliability).
  API token in gitignored `.env.sonar` (raw or `SONAR_TOKEN=`) — **regenerate it** (leaked 08/10).
- One business clock: `Clock` bean `HorlogeConfig` = `FuseauMetier.ZONE` (Africa/Casablanca),
  injected everywhere; source SQL Server measured at +01:00 Casablanca (same zone as `GETDATE()`).
  CI runs with `TZ: Africa/Casablanca` because tests read the system clock.
- Dossier creation closes the prospection with OMRAN's text « Création dossier de vente » and switches
  off all its alerts (FEATURE-conformite-legataire.md §2bis). 14 immo-crm prospections from before
  11/09 stay open (user's decision).
- Sonar « accepted » items carry their justification in SonarCloud (CSRF, SQL, 50 MB, S107…).
- Tests: full suite only in CI (every branch push, ~3.5 min); locally targeted `-Dtest=` only.

## Open — by priority

1. **Bug 71 — DANGER**: `db/migration/005_CfgCompany_IsActive.sql` must re-enable
   `('ADMOD','GHRIZA','MAYLINE')`. A version listing only MAYLINE would disable APF/ARS.
   Check the file before any use.
2. Bug 70: TRIBECA (société MAYLINE) invisible = `CfgCompany.IsActive = 0` on the VPS copies
   → `UPDATE dbo.CfgCompany SET IsActive = 1 WHERE Code = 'MAYLINE'` on each copy, never on the source.
3. Money defects from the cleanup (annexes of FEATURE-commentaires-et-docs-api.md): receipt
   `IsCredit` wording, two « Net à payer » computations, `COALESCE(SUM,0)` rounding.
4. Bug 68: import accepts the Leads Center export only, not Meta Lead Ads headers.
5. Bug 59: VPS copies drift from the source (the aperçu reads the copy, the import writes the source).
6. Bug 67: pre-prospection name/phone/email not editable via the API.
7. Bugs 60–62 (audit): commerciaux by `CfgTier.IsVisibleInList`, GEN0005 rules, refuse an alert
   on a closed prospection. Bug 46: duplicate prospection codes (`AdmSequence`).
8. Security: bug 55 (passwords seen in chat → rotation), 50/187 (500 shown as bare 401).

## Rules worth remembering

- Alerts: the next interaction **carrying an alert date** clears the previous one; archiving and
  conversion to dossier clear all.
- Dashboard and list count the same population (qualified, open, `DocDate` in the calendar month).
- A lead = contact + submission minute; NOUVELLE / DOUBLON created, DEJA_IMPORTEE / ERREUR skipped.
- Production writes only on explicit request; credentials never written down.
- First place to look for an OMRAN screen rule: `instructions/REFERENCE-aide-omran.md`.
- Source 115 has change tracking on 28 tables; `CfgCompany` is not tracked (each copy keeps its own).
- Prod reaches the source over internet (`197.230.172.136:1433` port-forward). `LazyConnectionDataSourceProxy`
  must keep its declared defaults, or every read contacts the source again.
