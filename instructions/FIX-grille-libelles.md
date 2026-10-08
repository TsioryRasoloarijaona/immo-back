# FIX — grille : libellés de groupe, glyphe « Annulé », textes et tri (lot 1, front seul)

Origine : campagne de tests navigateur du 08/10/2026 (dossier de test ARS 2026/00410).
Dépôt : `crm-prototype`, branche `fix/grille-libelles-accents`. **Aucun changement backend.**

Règles OMRAN : l'aide (`REFERENCE-aide-omran.md`, pages grille l.1635-1664) ne dit rien des
libellés de groupe, du tri des groupes ni de l'ordre des combos articles. Seule référence
voisine, changelog 0001166 (l.839) : « Dans la prospection ordonner le combo Source de contact
par ordre alphabétque. » Le reste suit `crm-prototype/CLAUDE.md`.

## 1. En-têtes de groupe : libellé, pas code brut

Constat : groupé par État (encaissements) → `CANCELED / IN_WORK / POINTED` ; par Statut
(dossiers) → `CONCRETISATION`, `FORMALISE`… Les lignes affichent « Annulé », « Concrétisation ».

- `lib/groupement.ts:93-95` `libelleGroupeAffiche` rend `g.libelle` brut ; utilisé
  `components/DataGrid.tsx:830` (aria-label) et `:854` (texte).
- Les lignes passent par les `VALEURS` des adaptateurs : `EncaissementTable.tsx:30-32`
  (`encaissementEtatLabel`), `DossierTable.tsx:67-69` (`canonicalStatusLabel`).

Attendu : ajouter à `GroupementGrille` (`DataGrid.tsx:87-108`) un `libelleGroupe?: (brut: string) => string`,
l'appliquer aux deux endroits (`g.libelle === null` garde « (non renseigné) »), le fournir depuis
`pages/encaissements/EncaissementsList.tsx` (~:434, colonne `etat` → `encaissementEtatLabel`) et
`pages/dossiers/DossiersList.tsx` (~:584, colonne `statut` → `canonicalStatusLabel`). Toute autre
colonne : inchangée. Le **filtre de groupe envoyé au serveur garde le code brut** — seul l'affichage change.

Même défaut, à corriger avec `canonicalStatusLabel` : statut de dossier brut dans
`components/DossierPreviewDialog.tsx:24`, `components/HotlineResultat.tsx:120`,
`pages/prospects/ProspectDetail.tsx:598`.

## 2. « Alphabétique » sur un groupement par date

`lib/groupement.ts:115-118` (`TRIS`), affiché `components/BandeGroupement.tsx:184` et `:188-190`.
Quand `estDate`, le tri `LIBELLE` s'affiche « Chronologique ». Valeur envoyée inchangée.

## 3. Glyphe sur l'encaissement « Annulé »

`lib/domain.tsx:117` : `CANCELED` → ton `neutral`, donc sans glyphe. `CLAUDE.md` : « the glyph is
what makes a red "Annulé" legible to a colour-blind operator ». Passer `CANCELED` au ton `warning`
(même ton que le dossier « Annulé », `lib/dossierStatus.ts:83`). Vaut aussi pour l'alias
`reglementStatutTone` (`domain.tsx:129`). **Ne pas toucher** « Réservation » (neutral voulu,
`ui.tsx:23-25`).

## 4. Texte « le bouton Enregistrer est sous le formulaire »

`pages/prospections/ProspectionDetail.tsx:1214`. La barre est entre la fiche et les onglets
(`FicheShell.tsx:83-97`). Nouveau texte : « modification non enregistrée — le bouton Enregistrer
est au-dessus des onglets ».

## 5. Combos non triés dans « Nouvelle pré-prospection »

`pages/prospections/NewPreProspection.tsx:108-110` : `projetItems`, `trancheItems`, `articleItems`
dans l'ordre serveur. Trier par libellé, `localeCompare(…, 'fr', { numeric: true })`.

## Hors périmètre

Filtres de colonne (accents, montants, indication « lignes chargées ») : lot 2,
`FIX-grille-filtres-fiches.md`. Ne pas toucher `DataGrid.tsx` hors des lignes du §1.

## Vérification

- `npm run build` vert.
- Navigateur : encaissements groupés par État → « Annulé / En cours / Pointé » et les mêmes
  comptes qu'avant (1 / 329 / 4 sur toutes périodes) ; dossiers groupés par Statut → libellés
  accentués, 143 lignes au total ; un groupe déplié charge toujours ses lignes ; groupement par Date
  → « Chronologique » ; badge Annulé avec glyphe ; combo articles trié.
