# PiecesJointesPatrimoineController

## lister

Pièces jointes rattachées au porteur (`AdmAttachedFile.CategoryId` = PKey de l'article, de la tranche ou du programme),
sans filtre sur le type ni sur `Category`, la plus récente d'abord. Les lignes « cochées mais jamais déposées » et les
pièces supprimées logiquement sont exclues. Porteur d'une entité juridique inactive → 404, comme sa fiche.

## fichier

**Renvoie une redirection `302` vers une URL présignée**, pas le contenu : le fichier transite directement entre le
client et le stockage S3. L'URL vaut autorisation quelques minutes ; elle n'est délivrée qu'après vérification que la
pièce appartient au porteur. Disposition `inline` (affichage d'un plan sans téléchargement), nom = légende de la pièce.

## ajouter

Multipart `fichier` ; `description`, `typeId` et `groupe` facultatifs. Extensions admises (pour **tous** les dépôts du
CRM, dossiers et règlements compris) : pdf, jpg, jpeg, jfif, png, gif, webp, heic, doc, docx, xls, xlsx, txt — l'extension
seule décide, le type MIME déclaré n'est pas lu ; fichier sans extension ou d'une autre extension (svg, dwg…) → 400
nommant la liste. Plus de 50 Mo → 413, rien n'est écrit. La légende (`description`, sinon le nom d'origine) est
assainie : séparateurs de chemin et caractères de contrôle retirés. Écrit `Category` (`Yes.Stk.Building.Item`,
`Yes.Cfg.CfgTranche` ou `Yes.Cfg.CfgProject`), `CategoryDescription` (code de l'article, description de la tranche,
code du programme) et une ligne d'audit `Added`.

## supprimer

Suppression **logique** (`AppFileObject.DeletedAt`), sans restauration ; la ligne `AdmAttachedFile` reste (date et
auteur de modification posés) et une ligne d'audit `Deleted` est écrite. Pièce d'un autre porteur → 404.

## attacherEnMasse

Écran OMRAN « Attachement de fichiers » (`ItemList.AttachFile`). Pour chaque fichier, le nom sans extension est comparé
au code des articles **de nature Bâtiment** de la tranche (sous-articles et terrains exclus), en égalité **stricte,
sensible à la casse**. Article trouvé : s'il porte déjà une pièce du même `typeId`, la plus récente est **remplacée**
(même PKey, contenu, extension, groupe, légende et type réécrits, nouvel objet de stockage) → `REMPLACE` ; sinon une
pièce `Yes.Stk.Building.Item` est créée → `ATTACHE`. Aucun article → `CODE_INTROUVABLE` (« Code article
introuvable »). Fichier refusé (vide, extension, taille) → `REFUSE` avec le motif. **Une transaction par fichier** : un
refus n'annule pas les autres, et un échec en cours de lot laisse écrits les fichiers déjà traités. Le front découpe
les envois en lots de moins de 50 Mo et de 100 fichiers au plus (limite du nombre de parties multipart).

## groupes

Valeurs distinctes non vides de `AdmAttachedFile.FoderGroup` (toutes pièces confondues), triées, pour proposer une
saisie libre avec suggestions comme OMRAN.
