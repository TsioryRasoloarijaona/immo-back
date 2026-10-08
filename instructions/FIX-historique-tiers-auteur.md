# FIX — auteur de l'historique du tiers (Hotline, fiche prospect)

Origine : re-test navigateur du 09/10/2026 après #115 (dossier ARS 2026/00411, tiers 26/14455).
Branche `fix/historique-tiers-auteur` (immo-crm). Suite de `FIX-tracabilite-historique-hotline.md` §1/§4.

OMRAN — HOT LINE et PROSPECTS, Liste d'historique : « La liste visualise l'historique de dossier de
vente d'un client en affichant : L'utilisateur, La date, L'état, Le N° Dossier, L'article, Les détails ».
GEN0016 : « L'acteur qui a procédé à la création… ».

## Constat

Pour le **même** changement de statut (RESERVATION du dossier ARS 2026/00411) :
- `GET /api/dossiers/{id}/historique` → `utilisateur = "Tsiory Admin"` ;
- `GET /api/hotline/recherche?nom=…` → `historique[0].utilisateur = null` (idem
  `GET /api/prospects/{id}/historique`), affiché « Système ».

`DossierHistoriqueService` résout l'auteur STATUT par `AdmUserId` **puis** se replie sur la ligne
`StrHistory` du même dossier à la même `DocDate` ; `ProspectSubResourcesService.historique` (lot 3 §4)
n'a que la première étape. Nos propres écritures tombent dans le repli quand le compte n'est pas
résolu par `AdmUserId`.

## Attendu

Une **seule** résolution d'auteur pour une ligne `StrFolderStatusHistory`, partagée par les deux
lectures (dossier et tiers) — pas deux règles qui « devraient » concorder : `AdmUser` (Description,
sinon Code) via `AdmUserId`, sinon `StrHistory.AdmUserDescription` du **même dossier** à la même
`DocDate`, sinon `null`. Invariant testable : pour une ligne donnée, `utilisateur` est identique dans
l'historique du dossier et dans celui du tiers.

Rien d'autre ne change (périmètre, tri, pagination, forme du DTO).

## Vérification

- Test : une ligne de statut sans `AdmUserId` résolu mais avec sa ligne `StrHistory` jumelle →
  l'historique du tiers porte le nom ; une jumelle sur **un autre dossier** à la même date ne signe
  pas. Les tests de `TracabiliteHistoriqueHotlineTest` restent verts.
- Ne jamais insérer d'`AdmUserId` inexistant dans un test (FK en CI) — `null` ou un `AdmUser` inséré.
- `gate.mjs back` sur les classes touchées ; CI complète verte.
