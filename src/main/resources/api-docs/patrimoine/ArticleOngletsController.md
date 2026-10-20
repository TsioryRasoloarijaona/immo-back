# ArticleOngletsController

## prospections

Équivalent de `ComSaleProspectionView WHERE StkItemId = id` d'OMRAN, recalculé sur `ComProspection` : prospections
dont `StkItemId` est l'article, pré-prospections exclues, prospections sans tranche incluses. Périmètre commercial de
`GET /api/prospections` (par la tranche de la prospection). Mêmes lignes que la liste des prospections, sans pagination.

## dossiers

Union d'OMRAN (`CfgTierSale.GetFoldersForStkItem`) : dossiers de **vente** dont l'article est l'article principal
(`ComFolder.StkItemId`) ou une ligne (`ComFolderItem.StkItemId`, cas d'un sous-article), **tous statuts** (annulés et
résiliés compris), sous le périmètre commercial et les statuts visibles de la liste des dossiers ; puis dossiers de
**location** (`RntFolder.StkItemId`) et de **syndic** (`SynFolder.StkItemId`), visibles si l'article est dans le
périmètre. Les lignes de location `RntFolderItem` ne sont pas reprises. Montants : voir `ArticleDossierDto`.

## historique

Lignes `StrFolderStatusHistory` dont `StkItemId` est l'article, plus celles d'un **transfert produit légalisé**
(`ComSaleWithdrawalProduct.IsLegalized = 1`) dont l'article est l'ancien ou le nouveau. Tri par date décroissante (OMRAN
trie par date croissante ; le CRM garde l'ordre de l'historique du tiers). Une ligne d'un dossier de vente hors
périmètre commercial est masquée ; une ligne sans dossier de vente (location, syndic) l'est si l'article est hors
périmètre, comme dans l'onglet Dossiers. Origine : `ComFolderId` → VENTE, sinon `RntFolderId` → LOCATION, sinon `SynFolderId` →
SYNDIC. Auteur : le compte de la ligne, sinon la ligne `StrHistory` jumelle du dossier de vente, sinon « Système ».
