# ModeleDocumentController

Modèles de documents modifiables par un administrateur (FEATURE-modeles-documents.md). Un modèle appartient à une
**catégorie** (type de fiche source, lot 1 : `DOSSIER_VENTE`) ; son contenu est un **HTML restreint** (liste blanche :
`p`, `h1`–`h3`, `strong`, `em`, `u`, `br`, listes, `hr`, tableaux, images `data:image/png|jpeg;base64` de 500 Ko au plus,
2 Mo par version) où les valeurs de la fiche s'écrivent `<span data-champ="cle">libellé</span>`, les lignes répétées
`data-repeter="liste"` (sur `tr`, `li`, `p`, `span`), les blocs conditionnels `data-si="cle"` (+ `data-valeur` pour un
booléen), les titres numérotés `<h2 data-numero>` et les sauts de page `<div data-saut-page></div>`. Tout autre élément,
attribut ou style → 400 qui nomme l'élément fautif. Chaque enregistrement crée une version, jamais d'écrasement ; pas de
suppression : `actif = false`. Concevoir = rôle `Administrateur` ; imprimer = accès à la fiche (404 hors périmètre).

## modifier

Champs absents = inchangés. `contenu` ou `miseEnPage` présent = nouvelle version (le champ absent des deux est repris
de la version courante). `libelle`, `ordre` et `actif` seuls ne créent pas de version.

## generer

Document du modèle **actif** rempli avec la fiche `ficheId` (pour `DOSSIER_VENTE` : contrôle de périmètre du dossier,
404 hors périmètre). PDF affiché en ligne, DOCX en pièce jointe ; nom de fichier `<code-modele>-<code-fiche>` en
minuscules (`_` → `-`). Le contenu est revalidé avant chaque génération.
