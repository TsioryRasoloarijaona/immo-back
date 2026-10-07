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
