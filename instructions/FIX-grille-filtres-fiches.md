# FIX — filtres de colonne, « Créer prospect », téléphone/mobile (lot 2, front seul)

Origine : campagne de tests navigateur du 08/10/2026. Dépôt : `crm-prototype`, branche
`fix/grille-filtres-fiches` (worktree `.worktrees/fix-grille-filtres-fiches/crm-prototype`).
**Aucun changement backend.**

Règles OMRAN (`REFERENCE-aide-omran.md`, « Les filtres génériques », l.1648) : « Chaque colonne de
la liste et en se basant sur le type de ses données va présenter des critères spécifiques. » Les
opérateurs eux-mêmes ne sont que dans une capture : rien d'autre de documenté. HOT LINE
(l.5844-5845) liste « Mobile » et « Téléphone » comme deux champs distincts.

## 1. Filtre de colonne insensible aux accents

`components/DataGrid.tsx:364` et `:369` : `toLowerCase().includes`. « cheque » ne trouve pas
« CHÉQUE », « qualifie » ne trouve pas « 3 QUALIFIÉ ». Exporter `sansAccents` de `lib/colonnes.ts:41`
(ou le déplacer dans `lib/format.ts`) et l'appliquer à l'aiguille **et** à la valeur comparée.

## 2. Filtre numérique : `=` veut dire égal

`DataGrid.tsx:168-175` `texteComparable` compare `String(v)` en sous-chaîne pour MONTANT/NOMBRE :
« 50000 » ramène 250 000 ; « 50 000,00 » (forme affichée) ne ramène rien. Le glyphe dit « = »,
son `title` dit « Égal à » (`:796`).

Attendu pour les types `NUMERIQUES` : normaliser l'aiguille (retirer espaces, ` `, ` `,
suffixe « DH », virgule → point). Si c'est un nombre fini → **égalité numérique**
`Number(valeur) === n` (tolérance 0,005 pour les montants). Sinon → sous-chaîne sur la **forme
affichée** sans espaces. Si la colonne a un `formatValeur`/`rendu` en % (`remise`,
`probabiliteFinalisation`), comparer sur la forme affichée (« 7,31 » doit trouver 7,31 %).
Texte et date : inchangés (date déjà sur la forme affichée).

## 3. Dire que filtre / Σ / tri ne portent que sur les lignes chargées

Comportement voulu (`DataGrid.tsx:27-31`, `CLAUDE.md`) : on le garde, on l'annonce. La barre
`:605` n'apparaît pas quand seul un filtre est actif. Ajouter à sa condition `filtreActif` (au moins
un filtre non vide) **et** `ordrePersonnalise` (bug latent : « Ordre des colonnes modifié — rétablir »
n'apparaît jamais seul). Quand un filtre est actif, le compteur `:626-632` affiche
« N / M ligne(s) chargée(s) — filtre sur la page affichée seulement ».

## 4. « Créer prospect » reste actif après succès

`pages/prospections/ProspectionDetail.tsx:1526-1538` : `onSubmit={(maj, mode) => { refetch(); … }}`
ignore `maj`. La lecture relue peut être en retard sur l'écriture (base de lecture synchronisée
après coup) : la fiche reste « pré-prospection » et le bouton (`:995-998`, `isPre = !p.prospectId`
`:694`) reste cliquable — risque de double création.

Attendu : `creerProspectFromPreProspection` (`lib/api.ts:767`) renvoie le `ProspectionDetailDto`
à jour ; s'en servir. Mémoriser localement `{ id, prospectId }` de la réponse et calculer `isPre`
sur `p.prospectId ?? prospectId local` ; idem pour le mode `'rattache'` (`lierProspectAPreProspection`).
Le bouton disparaît donc dès la réponse 201, même si le `refetch` revient avec l'ancien état.

## 5. Téléphone affiché sous « Mobile »

`ProspectionDetail.tsx:814` : `mobile = p.mobileProspect ? … : (p.tempTel ?? null)` — le
téléphone de premier contact retombe sous « Mobile ». À la création du prospect, `tempTel` part en
`telephone` (`:690`, `CreateProspectForm.tsx:103,166`). Les listes l'affichent en Téléphone.

Attendu : plus de repli `tempTel` → Mobile. « Téléphone » s'affiche si `isPre` **ou** si le prospect
n'a pas de mobile, avec `p.tempTel` (ou le `telephone` du prospect lié) ; « Mobile » seulement si
`p.mobileProspect` existe.

## 6. « Raison » vide sur une prospection archivée par la création d'un dossier

OMRAN — PROSPECTIONS, Liste : deux colonnes distinctes « Raison d'archivage » et « Note raison
d'archivage » ; Date d'archivage : « Le système archive une prospection et affecte la date après : La
création d'un dossier de vente… ». Le backend écrit `ClosingReasonText = "Création dossier de vente"`
sans motif codé, délibérément (`FEATURE-conformite-legataire.md` §2/§2bis : c'est ce qu'OMRAN écrit
sur 356 / 359 archivées). Le DTO le sert en `raisonFermetureTexte`.

Constat : `ProspectionDetail.tsx:934` n'affiche que `p.raisonFermeture` → « — ».
Attendu : afficher aussi `raisonFermetureTexte` (champ « Note raison d'archivage », ou en repli sous
« Raison » quand le motif codé est vide). Ne pas inventer de motif codé.

## Hors périmètre

Libellés de groupe, glyphes, textes, tri des combos : lot 1 (`FIX-grille-libelles.md`), qui touche
`DataGrid.tsx:830/:854` — ne pas modifier ces lignes ici. Hotline / pièce d'identité : pas un bug
(la fiche trouvée n'en a pas ; OMRAN HOT LINE ne liste pas ce champ).

## Vérification

- `npm run build` vert.
- Navigateur : encaissements, filtre Mode « cheque » → lignes CHÈQUE ; Montant « 50000 » →
  50 000 uniquement, « 50 000,00 » → mêmes lignes ; filtre actif → barre « … filtre sur la page
  affichée seulement » ; prospections, Étape « qualifie » → lignes 3 QUALIFIÉ.
- Pré-prospection → « Créer prospect » → le bouton disparaît dès le toast.
