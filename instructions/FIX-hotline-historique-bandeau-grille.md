# FIX — historique du tiers, bandeau de création du dossier, combo source, grille (front)

Origine : re-test navigateur du 09/10/2026. Dépôt `crm-prototype`, branche
`fix/hotline-historique-bandeau-grille`. **Aucun changement backend** (l'auteur vide de
l'historique du tiers est traité dans `FIX-historique-tiers-auteur.md`, autre branche).

## 1. Onglet « Historique » du tiers (Hotline et fiche prospect)

OMRAN — HOT LINE et PROSPECTS, Liste d'historique : « …L'utilisateur, La date, L'état, Le N° Dossier,
L'article, Les détails ».

Constat : la Hotline (`components/HotlineResultat.tsx`, onglet Historique) affiche l'état brut
« RESERVATION » et ni n° de dossier ni article ; même onglet sur la fiche prospect
(`pages/prospects/ProspectDetail.tsx` ~:475). Depuis immo-back #115, `HistoriqueDto` porte
`dossierId`, `dossierCode`, `articleCode` (voir `FIX-tracabilite-historique-hotline.md`, « Impact
front ») — vérifier `src/types.ts` et l'ajouter s'il manque (noms exacts du fil).

Attendu, aux deux endroits : état via `canonicalStatusLabel(statutDescription)` ; colonnes/lignes
dans l'ordre OMRAN : Utilisateur (repli « Système » seulement si null), Date, État, N° Dossier (lien
vers `/dossiers/{dossierId}` si présent), Article, Détails (`description`). Valeurs absentes → « — ».

## 2. Bandeau faux après création du dossier

`pages/dossiers/DossierDetail.tsx:1502` : avec `?nouveau=1`, le bandeau dit « Le schéma de
financement est vide — l'API ne permet pas de le pré-remplir à la création ; complétez-le… », alors que
la création génère désormais les échéances (3 lignes sur ARS 2026/00411).

Attendu : ne l'afficher **que si** le schéma de financement du dossier est réellement vide (0 échéance
chargée) ; sinon un bandeau neutre « Dossier créé. » ou rien. Ne pas l'afficher pendant le chargement.

## 3. Combo « Source de contact » trié

OMRAN — changelog 0001166 (`REFERENCE-aide-omran.md` l.839) : « Dans la prospection ordonner le combo
Source de contact par ordre alphabétque. »

Constat : non trié dans « Nouvelle pré-prospection » (`pages/prospections/NewPreProspection.tsx` ~:295 ;
SITE WEB, RECOMMENDATION, PROSPECTION EXTERNE, Bouche à Oreille…). Attendu : tri
`localeCompare(…, 'fr', { numeric: true, sensitivity: 'base' })` partout où ce combo apparaît
(Nouvelle pré-prospection, fiche prospection, nouvelle prospection, import des leads
`ImportPreProspections.tsx:284`, filtres « Origine ») — même ordre partout.

## 4. La grille saute quand on vide un filtre de colonne

`components/DataGrid.tsx` : la barre d'état (filtre actif / tri / Σ / ordre modifié) apparaît et
disparaît selon l'état ; vider un filtre retire la barre, tout remonte de ~20 px et le clic suivant
tombe sur une ligne (ouvre une fiche). Attendu : hauteur de la barre **réservée en permanence** (barre
toujours rendue, contenu vide quand rien n'est actif) — aucun décalage vertical des lignes quand un
filtre, un tri ou un total s'active ou se désactive. Pas d'autre changement de la grille.

## Hors périmètre

Lot 4 (prix plancher, annulés, prospect → client, catégorie d'action). Backend.

## Vérification

- `gate.mjs front` : `BUILD=0`.
- Navigateur (manager) : Hotline « claude parcours2 » → Historique avec Réservation, ARS 2026/00411,
  ARS0801 ; nouveau dossier → bandeau absent ou neutre ; combo source trié ; encaissements : taper puis
  vider un filtre → les lignes ne bougent pas.
