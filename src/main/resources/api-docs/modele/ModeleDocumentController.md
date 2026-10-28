# ModeleDocumentController

Modèles de documents modifiables par un administrateur (FEATURE-modeles-documents.md). Un modèle appartient à une
**catégorie** (type de fiche source, lot 1 : `DOSSIER_VENTE`) ; son contenu est un **HTML restreint** (liste blanche :
`p`, `h1`–`h3`, `strong`, `em`, `u`, `br`, listes, `hr`, tableaux, images `<img data-image="<id>" alt="…">` **sans
`src`**, l'id venant de `POST /api/modeles-documents/images`) où les valeurs de la fiche s'écrivent
`<span data-champ="cle">libellé</span>`, les lignes répétées `data-repeter="liste"` (sur `tr`, `li`, `p`, `span`), les
blocs conditionnels `data-si="cle"` (+ `data-valeur` pour un booléen), les titres numérotés `<h2 data-numero>` et les
sauts de page `<div data-saut-page></div>`, les pages répétées `<div data-repeter="liste" data-saut-page>` (une page par
élément) et les blocs positionnés `<div data-bloc data-x data-y data-l data-h [data-taille] [data-gras] [data-align]>`
(centimètres décimaux depuis le coin haut gauche de la page A4, x + l ≤ 21 et y + h ≤ 29,7 ; taille 6 à 72 pt, Helvetica,
gras si `data-gras="true"` ; `data-align` left, center ou right ; texte et champs seulement, ni tableau ni image). Tout autre élément, attribut ou style → 400 qui nomme l'élément fautif ;
texte de la version (contenu, en-tête et pied) : 1 Mo au plus. Chaque enregistrement crée une version, jamais
d'écrasement ; pas de suppression : `actif = false`. Concevoir = rôle `Administrateur` ; imprimer = accès à la fiche
(404 hors périmètre).

## modifier

Champs absents = inchangés. `contenu` ou `miseEnPage` présent = nouvelle version (le champ absent des deux est repris
de la version courante). `libelle`, `ordre` et `actif` seuls ne créent pas de version. Une image citée qui n'a pas été
déposée → 400 « Image inconnue ».

## generer

Document du modèle **actif** rempli avec la fiche `ficheId` (pour `DOSSIER_VENTE` : contrôle de périmètre du dossier,
404 hors périmètre). PDF affiché en ligne, DOCX en pièce jointe ; nom de fichier `<code-modele>-<code-fiche>` en
minuscules (`_` → `-`). Le contenu est revalidé avant chaque génération ; chaque image citée est lue dans le stockage
S3 ; une image absente du stockage → 500 « Image <id> introuvable dans le stockage », aucun document partiel.
Réponse `Cache-Control: private, no-store`.

Modèle à droits propres : `FORMULAIRE_BARID` ne s'imprime que pour les profils qui voient le rapport n° 35
(`ComSaleFormulaireBaridMaroc`, rôle libre ou lié dans `AdmReportDataRole`) — 403 sinon, contrôlé avant l'état actif et
avant la fiche — et en PDF seulement (`format=docx` → 400 « Ce formulaire n'existe qu'en PDF. »). Il est absent de la
liste des modèles actifs pour les autres profils.

Pages répétées (`<div data-repeter="liste" data-saut-page>`) : une page par élément ; une liste vide qui laisse le
document sans aucune page → 404 « Aucun destinataire ». Fond de page (`miseEnPage.fondImageId`) : PDF seulement,
ignoré en Word.

## deposerImage

Multipart, champ `fichier`. Le type est reconnu sur les **octets** (signature PNG ou JPEG), jamais sur le nom ni le
type déclaré ; 500 Ko au plus. L'image est écrite dans S3 puis enregistrée ; elle est immuable et jamais supprimée
(une ancienne version du modèle peut la citer). Réponse : `{id, contentType, taille}` ; l'éditeur insère
`<img data-image="id">`.

## lireImage

Accessible à tout utilisateur connecté (l'éditeur l'affiche ; les documents générés lisent les images côté serveur).
Réponse : les octets au `Content-Type` enregistré, `Cache-Control: private` long (une image ne change jamais).
