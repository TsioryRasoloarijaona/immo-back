# FEATURE — Nom des pré-prospections dans « À rappeler » et le calendrier

Branche immo-crm : `fix/nom-pre-prospection-relances`. Backend seulement.

## Problème

Le bloc « À rappeler » du tableau de bord affiche « Lead sans nom » pour les
pré-prospections alors que la fiche montre un nom. Cause : `prospectNom` est
calculé par `TierNomFormatter.nom(prospection.getCfgProspect())`. Une
pré-prospection n'a pas encore de fiche tiers (`CfgProspectId` NULL) : son nom
est saisi dans `ComProspection.TempDescription` (champ `tempDescription` de
l'entité), que l'écran de détail affiche déjà.

Le téléphone a déjà ce repli (`ProspectionService.telephoneProspect(prospect, p)`
→ `TempTel`) ; le nom ne l'a pas.

Chiffres `yesRems` (prod) : 17 703 prospections, 3 394 sans fiche tiers, dont
3 390 avec `TempDescription` renseigné.

## Règles

1. Nouvelle fonction partagée `ProspectionService.nomProspect(CfgTier prospect,
   ComProspection p)`, placée à côté de `telephoneProspect` :
   - fiche tiers présente et `TierNomFormatter.nom(prospect)` non vide → ce nom ;
   - sinon `p.getTempDescription()` nettoyé (trim) s'il a du texte ;
   - sinon `null`.
   `p` peut être `null` (calendrier : action sans prospection) → se comporte
   comme sans `TempDescription`.
2. `RelanceDashboardService.toDto` : `prospectNom` = `nomProspect(prospect, prospection)`.
3. `CalendrierService` (mapping `EvenementCalendrierDto`) : `prospectNom` =
   `nomProspect(prospect, prospection)` (prospection peut être null).
4. Le nom du **commercial** ne change pas.

## Hors périmètre

Pas de changement de DTO, d'endpoint ni de front (le front affiche déjà
`prospectNom || "Lead sans nom"`). Pas de changement aux listes de
pré-prospections (`ColonneProspection.PROSPECT_NOM`).

## Vérification

- Test : une pré-prospection sans fiche tiers avec `TempDescription` et une
  relance à échéance → `GET /api/dashboard/relances…` (endpoint existant du bloc
  « À rappeler ») renvoie `prospectNom` = ce `TempDescription`.
- Test : une prospection avec fiche tiers garde le nom de la fiche même si
  `TempDescription` diffère.
- Test unitaire de `nomProspect` (tiers, repli, tout vide, `p` null).
- `./mvnw compile` vert ; tests ciblés verts. La suite complète est lancée par
  le manager.

---

## §2 — Suivi des actions : colonne « Tiers » (`tiersNom`)

Demande utilisateur (2026-10-07) : dans **Suivi des actions**, une action rattachée
à une pré-prospection n'affiche aucun nom ; une action de prospection, si.

**Cause.** `SuiviActionService.projeter` lit `ti.description` avec
`LEFT JOIN m.cfgTier ti` (le tiers posé **sur l'action**, `ComActionMessage.CfgTierId`).
Les actions de pré-prospection n'ont pas de `CfgTierId`. La colonne catalogue
`ColonneSuiviAction.TIERS_NOM` est `Chemin.groupable("cfgTier.description")`.

**Chiffres `yesRems` (copie locale, peut être en retard sur la prod) :** 48 013
actions ; 1 529 sans `CfgTierId`, **toutes** rattachées à une prospection, et les
1 529 ont un `TempDescription` non blanc. Parmi elles, 1 492 ont une prospection
**avec** fiche prospect (`CfgProspectId`) — elles affichent aussi un nom vide
aujourd'hui — et 37 sont sur des pré-prospections.

### Règles

5. `tiersNom` = premier texte non blanc parmi, dans cet ordre :
   1. `m.cfgTier.description` (inchangé quand présent) ;
   2. `m.comProspection.cfgProspect.description` ;
   3. `m.comProspection.tempDescription` (trim).
   Sinon `null`. Même règle « non blanc » que `StringUtils.hasText`.
6. Tri, filtre et groupement sur `tiersNom` suivent la **même cascade** : passer
   `TIERS_NOM` à `Chemin.repli("cfgTier.description",
   "comProspection.cfgProspect.description", "comProspection.tempDescription")`
   (le mécanisme existe déjà pour `telephone`/`email` de la prospection ; vérifier
   qu'il marche aussi pour le tri/filtre de la liste suivi, pas seulement le
   groupement). Une ligne affichée « X » doit tomber dans le groupe « X ».
7. `tiersId` **ne change pas** (reste l'id du tiers de l'action, `null` ici) ; le
   calcul de rang (actions plus récentes du même tiers) ne change pas.

### Vérification §2

- Test : action sans `CfgTierId` sur une pré-prospection avec `TempDescription`
  → `GET /api/suivi-actions` (endpoint existant de la liste) renvoie ce nom dans
  `tiersNom`.
- Test : action sans `CfgTierId` sur une prospection avec fiche prospect → nom de
  la fiche.
- Test : action avec `CfgTierId` → nom du tiers de l'action, même si la
  prospection a un autre nom.
- Test : groupement `groupBy=tiersNom` met l'action de pré-prospection dans le
  groupe portant son `TempDescription`.
