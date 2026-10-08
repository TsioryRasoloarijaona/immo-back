# FEATURE — Actions récentes (parcours de vente, partie 3)

Backend (immo-crm) + front (crm-prototype). Branche `feat/actions-recentes` dans les deux repos.
Suite de `crm-prototype/docs/FEATURE-parcours-vente.md` (parties 1 et 2 livrées, #79).

## Pourquoi

Un commercial enchaîne transformations, étapes, notes, archivages, puis doit rechercher la fiche
pour y revenir. Un petit panneau flottant « Actions récentes » liste ce qui vient d'être fait,
avec un lien direct vers la fiche. Deux vues : **Mes actions** et **Équipe** (tout ce qui s'est
passé sur les projets du périmètre de l'utilisateur).

## Données — ce que la base contient (data-explorer, `yesRems`, 2026-10-08)

| Source | Lignes | Auteur | Usage ici |
|---|---|---|---|
| `StrHistory`, `StrConfigEntityId IS NULL`, `ObjectId` → `ComProspection` | 35 (35/35 joignent), 2026-08-26 → 09-25, **toutes écrites par immo-crm** | **0/35** (`AdmUserId` NULL) | cycle de vie prospection |
| `StrHistory`, `StrConfigEntityId = StatutDossier.CONFIG_ENTITY_ID`, `ObjectId` → `ComFolder` | 1 066 (1 066 joignent) | 98,9 % (sur toute la table) | statuts de dossier (création comprise) |
| `ComActionMessage` (interactions / notes) | 48 013 | `AdmUserId` 47 985 (99,94 %), tous joignent `AdmUser` | interactions |
| `AdmWebHistorics` (audit OMRAN) | 561 935 | via session | **non utilisé** : immo-crm n'y écrit pas, table absente de `yesRemsTest`, `Details` en `text` sans index de date |

- OMRAN n'a jamais écrit de `StrHistory` pour une `ComProspection` : la vue prospection se
  remplira avec l'usage du CRM. C'est attendu, pas un défaut.
- Les autres `StrConfigEntityId` (914, 651, 333… lignes : achats, appels d'offres) ne joignent ni
  `ComFolder` ni `ComProspection` : **exclus**.
- `StrHistory` n'a **pas** de colonne type : le type se lit au **début** de `Description`
  (préfixes fixes écrits par `ProspectionService`, voir R3). `Notes` est utilisé par OMRAN sur
  9 lignes : ne pas y stocker de code.
- Jointure auteur : `StrHistory.AdmUserId` / `ComActionMessage.AdmUserId` = `AdmUser.PKey` =
  `admUserId` du JWT (`UtilisateurAffiche.courant()`, `security/UtilisateurAffiche.java:49`).
- Projet (périmètre), comme `FEATURE-perimetre-commercial.md` §4 :
  `ComProspection.StkHierarchyId → CfgTranche.CfgProjectId` ;
  `ComFolder.StkItemId → StkItem.StkHierarchyId → CfgTranche.CfgProjectId` ;
  interactions : `PerimetreCommercialService.filtreInteractions()` (`:213`).

## Partie A — Backend : écrire ce qui manque

Tout dans `services/prospection/ProspectionService.java`.

- **R1 — Auteur.** `enregistrerHistorique` (`:552`) renseigne `AdmUserId`, `AdmUserDescription`
  et `UserDate`, comme `DossierService.enregistrerHistoriqueStatut` (`:583-609`). Si aucun
  utilisateur n'est authentifié (formulaire web public / import par lot, appel de `:816`), auteur
  NULL **sans exception** — vérifier le comportement de `UtilisateurAffiche.courant()` dans ce cas
  et le tester.
- **R2 — Nouveaux événements**, dans la même transaction que le changement :
  | Méthode | Description écrite |
  |---|---|
  | `changerEtape` (`:970`) | `"Étape changée : {ancien code} → {nouveau code}"` (ancien absent → `"— → {nouveau}"`) ; rien si l'étape ne change pas |
  | `archiver` (`:1087`) | `"Archivée : {raison}"` (libellé de la raison, sinon `raisonTexte`) |
  | `desarchiver` (`:1119`) | `"Désarchivée"` |
- **R3 — Type stable.** Une seule table de correspondance préfixe → code (enum `TypeActionRecente`)
  côté backend, utilisée par le flux. Les constantes de préfixe sont **partagées** avec les
  écritures (pas de chaîne recopiée) :
  | Préfixe de `Description` | Code |
  |---|---|
  | `Pré-prospection créée : ` | `PRE_PROSPECTION_CREEE` |
  | `Prospect créé et lié : ` | `PROSPECT_CREE` |
  | `Prospect existant rattaché : ` | `PROSPECT_RATTACHE` |
  | `Transformée en prospection : ` | `TRANSFORMEE_EN_PROSPECTION` |
  | `Prospection créée : ` | `PROSPECTION_CREEE` |
  | `Étape changée : ` | `ETAPE_CHANGEE` |
  | `Archivée : ` | `ARCHIVEE` |
  | `Désarchivée` | `DESARCHIVEE` |
  | ligne dossier (`StrConfigEntityId` dossier) | `DOSSIER_STATUT` |
  | `ComActionMessage` | `INTERACTION` |
  | préfixe inconnu sur une ligne prospection | `AUTRE` (servie, jamais rejetée) |
- Le parcours de la partie 2 (`ParcoursVente.tsx`) lit déjà ces préfixes : ne pas les modifier.

## Partie B — Backend : le flux

### `GET /api/actions-recentes`

| Paramètre | Valeurs | Défaut |
|---|---|---|
| `portee` | `MOI` \| `EQUIPE` | `MOI` |
| `limite` | 1–100 | 20 |
| `avant` | date-heure ISO (exclusive) — pagination « Voir plus » | aucune |

Réponse `200` : `ActionRecenteDto[]`, du plus récent au plus ancien. `400` si `portee` ou `limite`
invalide, `401` sans authentification.

`ActionRecenteDto` (noms du fil exacts) :

| Champ | Type | Contenu |
|---|---|---|
| `id` | uuid | PKey de la ligne source |
| `date` | date-heure | `StrHistory.DocDate` ; interaction : `ComActionMessage.UserCreateDateTime` (repli `DocDate`) |
| `type` | string (code R3) | |
| `libelle` | string | `Description` ; interaction : type d'action + début du message (≤ 120 car.) |
| `auteurId` | uuid \| null | `AdmUserId` |
| `auteurNom` | string \| null | `AdmUserDescription` / nom de l'`AdmUser` |
| `cibleType` | `PRE_PROSPECTION` \| `PROSPECTION` \| `DOSSIER` | prospection : **type actuel** de la fiche (`DiscriminantPreProspection.estPreProspection`, `ProspectionService.java:956`), jamais celui de la ligne d'historique |
| `cibleId` | uuid | PKey de la `ComProspection` / du `ComFolder` |
| `cibleLibelle` | string | prospection : nom du contact/prospect tel que l'affichent les listes ; dossier : code dossier |
| `prospectId` | uuid \| null | `ComProspection.CfgProspectId` (lien secondaire « prospect ») |

Interaction rattachée à un dossier → cible `DOSSIER` ; à une prospection → `PROSPECTION` /
`PRE_PROSPECTION`.

### Règles (visibilité : tests de refus obligatoires)

- **R4 — Sources** : union des trois sources ci-dessus, triée par `date` décroissante, tronquée à
  `limite`. Volumes faibles côté historique (~3 k lignes) ; pour `ComActionMessage` (48 k), lire
  par source les `limite` premières lignes filtrées et triées puis fusionner — pas de chargement
  complet en mémoire.
- **R5 — `MOI`** : `auteurId = admUserId du principal`. Les lignes sans auteur n'apparaissent
  jamais dans `MOI`.
- **R6 — `EQUIPE`** : toutes les lignes dont le projet ∈ `perimetreCourant()`
  (`PerimetreCommercialService.java:85`), avec **exactement** les règles existantes : exempté ou
  sans affectation → aucun filtre (§6.2, §6.3) ; ligne sans projet résoluble → visible (§6.4).
  Réutiliser `filtreProspections()`, `filtreDossiers()`, `filtreInteractions()` — **pas** de
  nouvelle règle de périmètre.
- **R7 — `MOI` reste dans le périmètre** : même filtre que R6 appliqué aussi à `MOI` (un
  commercial dont le périmètre a été réduit ne voit plus ses actions sur un projet retiré).
- **R8** — Une fiche supprimée (ligne d'historique orpheline) est ignorée, pas d'erreur.
- **R9** — `avant` : seules les lignes `date < avant` ; permet « Voir plus » sans doublon.
- springdoc en français, texte long dans `src/main/resources/api-docs/actions-recentes.md`.

## Partie C — Front (crm-prototype)

Après `npm run docs:update` et `git diff docs/api-docsV4.json` : type `ActionRecente` dans
`src/types.ts`, `listActionsRecentes(portee, limite?, avant?)` dans `src/lib/api.ts`.

### Le panneau

```
                ┌ Actions récentes ─────────────── × ┐
                │ [ Mes actions ] [ Équipe ]         │
                │ Aujourd'hui                        │
                │ 10:42  ⇄ Dupont — devenu prospect  │
                │        Ouvrir →   Prospect →       │
                │ 10:30  ↗ P-0123 — étape VISITE     │
                │        (Équipe : par A. Diouri)    │
                │ Hier                               │
                │ 16:05  🗄 Martin — archivée         │
                │            [ Voir plus ]           │
                └────────────────────────────────────┘
                                         ( 🕘 )  ← bouton flottant
```

- **Bouton flottant** en bas à droite, monté **une fois** dans `Layout.tsx` (comme
  `ToastProvider`), visible sur toutes les pages authentifiées ; ne masque pas les toasts (les
  décaler ou décaler le bouton). `aria-label="Actions récentes"`, raccourci facultatif.
- **Panneau** (popover ancré au bouton, pas une modale plein écran) : onglets « Mes actions » /
  « Équipe » (onglet retenu en `localStorage`, lecture/écriture dans `try/catch`), groupes par
  jour (« Aujourd'hui », « Hier », puis date au format de l'app), heure, icône par `type`,
  `libelle`, `cibleLibelle`. Dans « Équipe », afficher l'auteur (« par … », ou « auteur
  inconnu »).
- **Liens** : « Ouvrir → » selon `cibleType` : `/pre-prospections/{id}`, `/prospections/{id}`,
  `/dossiers/{id}` ; si `prospectId` et type `PROSPECT_CREE`/`PROSPECT_RATTACHE`, lien secondaire
  « Prospect → » `/prospects/{prospectId}`. Navigation SPA ; le panneau se ferme au clic.
- **Chargement** : à chaque ouverture et changement d'onglet (pas de polling) ; « Voir plus »
  appelle avec `avant` = date de la dernière ligne, 20 par page.
- **États** : chargement (squelette), vide (« Aucune action récente. » + pour « Mes actions » :
  « Vos transformations, changements d'étape, notes et archivages apparaîtront ici. »), erreur
  (message + « Réessayer »).
- **Accessibilité** : `Échap` ferme et rend le focus au bouton, clic extérieur ferme, liste
  `<ul>`, onglets `role="tablist"`. Responsive : en écran étroit, le panneau prend la largeur
  moins 16 px de marge, pas de débordement horizontal.
- Libellés français, design existant (`crm-prototype/CLAUDE.md` : direction, formats).

## Hors périmètre

- `AdmWebHistorics` (pas d'entité, pas de reprise de l'historique OMRAN).
- Toute modification des préfixes existants, de `ParcoursVente.tsx`, du stepper d'étapes.
- Historique sur `modifierTranchePrincipale`, prospects (`CfgTier`), encaissements.
- Notifications temps réel, polling, compteur de non-lus.
- Index SQL / DDL.

## Vérification

Backend (`yesRemsTest` : `StrHistory` vide, les tests créent leurs données) :
1. R1 : chaque écriture existante (`:662`, `:816`, `:839`, `:869`, `:964`) porte l'auteur ;
   écriture sans utilisateur authentifié → auteur NULL, pas d'exception.
2. R2 : `changerEtape`, `archiver`, `desarchiver` écrivent la ligne attendue ; étape inchangée →
   aucune ligne.
3. R3 : chaque préfixe → son code ; préfixe inconnu → `AUTRE`.
4. R5 : `MOI` ne renvoie que les lignes de l'utilisateur ; ligne sans auteur absente.
5. **R6/R7 refus** : pour chacune des trois sources, une ligne sur un projet hors périmètre est
   absente de `EQUIPE` **et** de `MOI` ; présente pour un exempté ; ligne sans projet visible.
6. R4/R9 : tri décroissant, `limite` respectée (et plafonnée → `400` au-delà de 100), `avant`
   sans doublon ni trou entre deux pages.
7. `cibleType` suit le type **actuel** après transformation pré-prospection → prospection.
8. `400` sur `portee=FOO`, `401` sans cookie.
9. Suite complète `./mvnw test` verte, nombre de tests noté.

Front : `npm run build` vert. Contre le backend local : transformer une fiche, changer une étape,
ajouter une note, archiver puis désarchiver → chaque action apparaît dans « Mes actions » avec un
lien qui ouvre la bonne fiche ; onglet « Équipe » ; « Voir plus » ; états vide et erreur (backend
coupé). Capture optionnelle.

## 10. Révision après revue (une passe de correction)

Les tests `controller/actionsrecentes/ActionsRecentesRevueTest.java` (reviewer) échouent : les
faire passer **sans les affaiblir**.

### 10.1 Visibilité : la cible d'une interaction suit le projet filtré (amende « Interaction rattachée à un dossier → cible DOSSIER »)

`filtreInteractions()` lit le projet de la prospection **d'abord**
(`coalesce(projetProspection, projetDossier)`, `PerimetreCommercialService.java:226`), alors que
`depuisInteraction` (`ActionsRecentesService.java:219-221`) prend le dossier d'abord comme cible :
une interaction liée à une prospection du projet A **et** à un dossier du projet B expose le
dossier B à un commercial de périmètre {A} (3 lignes réelles sur 1 479 doublement rattachées).
Règle : **la cible suit le même ordre que le filtre** — prospection si `ComProspectionId` est
renseigné, sinon dossier. Ne pas modifier `filtreInteractions()`. Le cas miroir (périmètre {B})
reste masqué, c'est cohérent avec le filtre existant.

### 10.2 Pagination sans trou sur dates égales (amende R9)

Curseur composite : ajouter le paramètre `avantId` (uuid, optionnel, utilisé seulement avec
`avant`). Ordre total : `date` décroissante puis `id` décroissant ; page suivante = lignes avec
`date < avant` **ou** (`date = avant` **et** `id < avantId`). Supprimer le recul de 1 ms.
**Piège** : SQL Server ne trie pas un `uniqueidentifier` comme `java.util.UUID.compareTo`. L'ordre
appliqué en SQL (par source) et celui de la fusion en Java doivent être **le même** — choisir une
seule comparaison (par ex. l'id converti en chaîne des deux côtés) et la tester. Front : « Voir
plus » envoie `avant` et `avantId` de la dernière ligne. Mettre à jour la doc springdoc
(`api-docs/.../ActionsRecentesController.md`), `npm run docs:update`, `types.ts`/`api.ts`.

### 10.3 Le bouton masque « Page suivante » (amende Partie C)

Le bouton fixe en bas à droite (`ActionsRecentes.tsx:85`) recouvre la pagination épinglée des
pages `PageShell fullHeight` (Dossiers, Clients, Articles, Encaissements, Suivi des actions,
Offres, Pré-prospections…), sur bureau comme sur mobile. Déplacer le **déclencheur** dans la
barre latérale (zone du bas, avec le profil / `ThemeToggle`, ou l'équivalent présent dans
`Layout.tsx`), le panneau restant un popover flottant ancré à lui ; sur mobile, dans le menu de
navigation. Aucun élément fixe ne doit plus chevaucher le contenu des pages.
