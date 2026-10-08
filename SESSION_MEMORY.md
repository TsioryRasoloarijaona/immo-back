# Session Memory — immo-crm
_Last updated: 2026-10-08_ · state and decisions only (≤ 3 KB); history = git log + FEATURES.md

## Current state

- `feat/stabilite-connexion-source` (FEATURE-stabilite-connexion-source.md): source pool keeps
  connections alive (keepalive 60 s, maxLifetime 10 min, driver `connectRetryCount`, socketTimeout
  5 min), all via `SOURCE_DB_*` env. Link failures → 503 `SOURCE_INJOIGNABLE` (no statement sent,
  tracked per request by `ConnexionSourceSuivie`) or `LIAISON_COUPEE` (doubt). Two-base mode only;
  single-base keeps the 500. 503 not in OpenAPI (would touch all 185 operations).
- `main` = merge #100 (import sélectif).
- Suite: 1 114 tests, ~10 min, against `yesRemsTest`.

## Open — by priority

1. **Bug 71 — DANGER**: `db/migration/005_CfgCompany_IsActive.sql` must re-enable
   `('ADMOD','GHRIZA','MAYLINE')`. A version listing only MAYLINE would disable APF/ARS.
   Check the file before any use.
2. Bug 70: TRIBECA (société MAYLINE) invisible = `CfgCompany.IsActive = 0` on the VPS copies
   → `UPDATE dbo.CfgCompany SET IsActive = 1 WHERE Code = 'MAYLINE'` on each copy, never on the source.
3. Money defects from the cleanup (annexes of FEATURE-commentaires-et-docs-api.md): receipt
   `IsCredit` wording, two « Net à payer » computations, `COALESCE(SUM,0)` rounding.
4. Bug 68: import accepts the Leads Center export only, not Meta Lead Ads headers.
5. Bug 66: server in UTC, "today" off between 00:00 and 01:00 in Morocco → `Africa/Casablanca`.
6. Bug 59: VPS copies drift from the source (the aperçu reads the copy, the import writes the source).
7. Bug 67: pre-prospection name/phone/email not editable via the API.
8. Bugs 60–62 (audit): commerciaux by `CfgTier.IsVisibleInList`, GEN0005 rules, refuse an alert
   on a closed prospection. Bug 46: duplicate prospection codes (`AdmSequence`).
9. Security: bug 55 (passwords seen in chat → rotation), 50/187 (500 shown as bare 401).

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
- Copy-pool failure is also worded « base OMRAN injoignable » (accepted, sev. 3).
