-- ============================================================================
-- Vue legataire consommee par le code applicatif, et les deux tables dont elle
-- depend — ECRIT A LA MAIN (comme 040), contrairement aux 010/020/030 generes.
--
-- Pourquoi ce fichier existe
-- --------------------------
-- Jusqu'a la feature « vues legacy — fiche prospection », le README pouvait
-- ecrire que les 216 vues etaient hors perimetre, « aucune requete native ni
-- @Subselect ne les utilise ». Ce n'est plus vrai : ComSaleProspectionView est
-- desormais mappee par une entite JPA @Immutable en lecture seule
-- (instructions/FEATURE-vues-legacy-prospection.md).
--
-- Une seule vue, et c'est deliberé
-- --------------------------------
-- ComActionMessageView a ete evaluee puis ECARTEE : elle n'a aucune selectivite
-- (30 s pour une seule ligne en SELECT *, a cause d'un COUNT correle calculant
-- ActionRank). Tout ce qu'on voulait en tirer — categorie, sous-categorie,
-- projet, tranche — s'obtient par jointures sur les entites existantes, la
-- tranche suivant la regle de la vue elle-meme :
--   categorie = 'Prospection' ? ComProspection.StkHierarchyId : StkItem.StkHierarchyId
-- Ne pas la reintroduire sans remesurer.
--
-- Pourquoi les deux tables ne sont pas dans 010_tables.sql
-- -------------------------------------------------------
-- 010 est genere depuis la fermeture transitive des cles etrangeres des tables
-- mappees par une entite JPA. AdmAlertData et AdmParamsInteger ne sont pointees
-- par aucune d'elles : elles n'entrent dans le perimetre que parce que la vue
-- les lit. Dans un fichier genere, la prochaine regeneration les effacerait.
--
-- Sur les 43 objets dont depend la vue, 41 sont deja dans 010.
--
-- A appliquer APRES 040.
-- ============================================================================
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

-- ---------------------------------------------------------------------------
-- Tables manquantes
-- ---------------------------------------------------------------------------
CREATE TABLE [dbo].[AdmParamsInteger](
	[Code] [nvarchar](255) COLLATE French_CI_AS NOT NULL,
	[CodeValue] [int] NULL,
	[PKey] [uniqueidentifier] NULL,
 CONSTRAINT [AdmParamsInteger_PK] PRIMARY KEY CLUSTERED ([Code] ASC)
)
GO

CREATE TABLE [dbo].[AdmAlertData](
	[Pkey] [uniqueidentifier] NOT NULL,
	[Label] [nvarchar](256) COLLATE French_CI_AS NULL,
	[Code] [nvarchar](256) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](256) COLLATE French_CI_AS NULL,
	[Message] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[Groupping] [nvarchar](256) COLLATE French_CI_AS NULL,
	[AlertDate] [datetime] NULL,
	[RelatedKey] [uniqueidentifier] NULL,
	[RightKey] [nvarchar](256) COLLATE French_CI_AS NULL,
	[Active] [bit] NULL,
 CONSTRAINT [PK_AdmAlertData] PRIMARY KEY CLUSTERED ([Pkey] ASC)
)
GO

-- ---------------------------------------------------------------------------
-- Semis indispensable : la vue calcule
--   DATEADD(day, (SELECT CodeValue FROM AdmParamsInteger WHERE Code='DefaultAlertDelai'), GETDATE())
-- Sans cette ligne la sous-requete rend NULL, DATEADD rend NULL, et
-- LastActiveAlertDate devient silencieusement NULL sur toutes les lignes : un
-- test vert pour une mauvaise raison. La valeur 0 est celle de yesRemsProd.
-- ---------------------------------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM dbo.AdmParamsInteger WHERE Code = 'DefaultAlertDelai')
    INSERT INTO dbo.AdmParamsInteger (Code, CodeValue, PKey)
    VALUES (N'DefaultAlertDelai', 0, '6BB66297-D6AE-4AAE-8164-3117EAB35892');
GO

-- ---------------------------------------------------------------------------
-- StrEntity « ComProspection » : le discriminant du legataire entre prospection
-- de vente et pre-prospection.
--
-- La vue fait un INNER JOIN sur StrEntity : une ComProspection dont StrEntityId
-- est NULL n'apparait dans AUCUNE vue. Sans cette ligne, toute fixture de test
-- serait donc invisible et le bloc « activite » vaudrait null — un test vert
-- pour une mauvaise raison, exactement ce que 040 cherche deja a eviter pour
-- StrStatus. Les PKey sont ceux de yesRemsProd.
-- ---------------------------------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM dbo.StrEntity WHERE PKey = 'E7C73F70-1111-40D9-AA78-A2825C42B601')
    INSERT INTO dbo.StrEntity (PKey, Code, Description)
    VALUES ('E7C73F70-1111-40D9-AA78-A2825C42B601', N'ComProspection', N'Prospection vente');
GO

IF NOT EXISTS (SELECT 1 FROM dbo.StrEntity WHERE PKey = 'B92DF3D8-158C-4E1E-827A-70FEB172A264')
    INSERT INTO dbo.StrEntity (PKey, Code, Description)
    VALUES ('B92DF3D8-158C-4E1E-827A-70FEB172A264', N'ComPreProspection', N'Pré-Prospection');
GO

-- ---------------------------------------------------------------------------
-- ComSaleProspectionView  (copie conforme de yesRemsProd)
-- ---------------------------------------------------------------------------

Create View [dbo].[ComSaleProspectionView] 
As

SELECT 


TT.PKey
, TT.Code
, TT.DocDate
, TT.Area
, TT.AreaTo
, TT.Price
, TT.PriceTo
, TT.AdvanceAmount
, TT.FundingOwnBase
, TT.FundingCredit
, TT.FundingOther
, TT.Notes

, TT.IsClosed
, TT.ClosingDate
, TT.ComProspectionClosingReasonId
, TT.ComProspectionClosingReasonCode
, TT.ComProspectionClosingReasonDescription
, TT.ClosingReasonText

, TT.StkVocationId
, TT.StkVocationCode
, TT.StkVocationDescription

, TT.StrEntityId
, TT.StrEntityCode
, TT.StrEntityDescription

, TT.AdmCountryId
, TT.AdmCountryCode
, TT.AdmCountryDescription

, TT.ComProspectionOriginId
, TT.ComProspectionOriginCode
, TT.ComProspectionOriginDescription

, TT.ComProspectionOriginSupportId
, TT.ComProspectionOriginSupportCode
, TT.ComProspectionOriginSupportDescription

, TT.ComActionMessageTypeId
, TT.ComActionMessageTypeCode
, TT.ComActionMessageTypeDescription

, TT.ComProspectionOriginTypeId
, TT.ComProspectionOriginTypeCode
, TT.ComProspectionOriginTypeDescription

, TT.CfgProspectId
, TT.CfgTierCode
, TT.CfgTierDescription
, TT.CfgTierCity
, TT.CfgTierPhone
, TT.CfgTierMobile
, TT.CfgTierEMail
, TT.CfgTierFirstName
, TT.CfgTierLastName
, TT.CfgTierMobile1
, TT.CfgTierMobile1Indicatif
, TT.CfgTierIDNumber
, TT.CfgTierKindCode
, TT.CfgTierKindDescription
, TT.CfgTierTypeId
, TT.CfgTierTypeCode
, TT.CfgTierTypeDescription

, TT.ComProspectionKindId
, TT.ComProspectionKindCode
, TT.ComProspectionKindDescription
, TT.ComProspectionKindAlertDelay 

, TT.ComActionId
, TT.ComActionCode
, TT.ComActionDescription

, TT.CfgCommercialId
, TT.CfgCommercialCode
, TT.CfgCommercialDescription
, TT.Commercial

, TT.StkHierarchyId
, TT.CfgTrancheCode
, TT.TrancheDescription

, TT.CfgProjectId
, TT.CfgProjectCode
, TT.CfgProjectDescription

, TT.CfgCompanyId
, TT.CfgCompanyCode
, TT.CfgCompanyDescription

, TT.MapLatitude
, TT.MapLongitude

, TT.StkItemId 
, TT.Article
, TT.ArticleDescription

, TT.ItemCategory
, TT.ItemType
, TT.ItemPresentationType

, TT.CfgProposerId
, TT.CfgProposerCode
, TT.CfgProposerDescription

, TT.CfgIntermediateId
, TT.CfgIntermediateCode
, TT.CfgIntermediateDescription
, TT.MapRadius

, TT.ComPaymentMethodId
, TT.ComPaymentMethodCode
, TT.ComPaymentMethodDescription


, TT.ComUtilizationId
, TT.ComUtilizationCode
, TT.ComUtilizationDescription

, TT.ComBudgetId
, TT.ComBudgetCode
, TT.ComBudgetDescription

, TT.ComFolderId
, TT.ComFolderCode
, TT.ComFolderDescription

, TT.AdvanceDate 
, TT.Delai

, TT.CfgCustomerId
, TT.CfgCustomerCode
, TT.CfgCustomerDescription

, TT.IsOldCustomer

, TT.Status
, TT.StatusIsCancellation
, TT.StatusIsClosing
, TT.StatusImg

, TT.Project
, TT.Categories
, TT.Types

, TT.Cities
, TT.FirstVisit
, TT.LastVisitDate
, TT.NbrVisit

, TT.LastCommunicationDate
, TT.LastCommunicationResult
, TT.LastCommunicationMessage
, TT.LastActionDate
, TT.LastActiveAlertDate AS LastActiveAlertOld
, TT.ThereIsInstructionNotRead
, TT.CfgCommercialForInstructionId
, TT.ActionsMessageNumber
, TT.InactivityTimeout

, IIF(TT.ComProspectionKindAlertDelay<> 0 and (TT.InactivityTimeout >= TT.ComProspectionKindAlertDelay),IIF((TT.LastActiveAlertDate > DATEADD(day,TT.ComProspectionKindAlertDelay,TT.LastActionDate)) OR (TT.LastActiveAlertDate is null) ,DATEADD(day,TT.ComProspectionKindAlertDelay,TT.LastActionDate),TT.LastActiveAlertDate),TT.LastActiveAlertDate) AS LastActiveAlertDate
, IIF (TT.ComProspectionKindAlertDelay<> 0 and (TT.InactivityTimeout >= TT.ComProspectionKindAlertDelay)
, IIF((TT.LastActiveAlertDate > DATEADD(day,TT.ComProspectionKindAlertDelay,TT.LastActionDate)) OR (TT.LastActiveAlertDate is null) 
, IIF(DATEADD(day,TT.ComProspectionKindAlertDelay,TT.LastActionDate) is not null,'Délais d"inactivité de '+CONVERT(varchar(255), TT.ComProspectionKindAlertDelay)+' jours' ,'')    
, IIF(TT.LastActiveAlertDate is not null,'Alerte action utilisateur',''))
, IIF(TT.LastActiveAlertDate is not null,'Alerte action utilisateur','')) AS LastActiveAlertReason

-- Données temporaires
, TT.TempDescription
, TT.TempTel
, TT.TempEMail
, TT.TempField1
, TT.TempField2
, TT.TempField3
, TT.TempField4
, TT.TempField5
, TT.TempField6
, TT.TempField7
, TT.TempField8
, TT.TempField9
, TT.TempField10

, TT.UserCreate
, TT.UserCreateDateTime
, TT.UserUpdate
, TT.UserUpdateDateTime

FROM
(
SELECT 
ComProspection.PKey
, ComProspection.Code
, ComProspection.DocDate
, ComProspection.Area
, ComProspection.AreaTo
, ComProspection.Price
, ComProspection.PriceTo
, ComProspection.AdvanceAmount
, ComProspection.FundingOwnBase
, ComProspection.FundingCredit
, ComProspection.FundingOther
, ComProspection.Notes

, ComProspection.TempDescription
, ComProspection.TempTel
, ComProspection.TempEMail
, ComProspection.TempField1
, ComProspection.TempField2
, ComProspection.TempField3
, ComProspection.TempField4
, ComProspection.TempField5
, ComProspection.TempField6
, ComProspection.TempField7
, ComProspection.TempField8
, ComProspection.TempField9
, ComProspection.TempField10

, ComProspection.IsClosed
, ComProspection.ClosingDate
, ComProspection.ComProspectionClosingReasonId
, ComProspectionClosingReason.Code AS ComProspectionClosingReasonCode
, ComProspectionClosingReason.Description AS ComProspectionClosingReasonDescription
, ComProspection.ClosingReasonText

, ComProspection.StkVocationId
, StkVocation.Code AS StkVocationCode
, StkVocation.Description AS StkVocationDescription

, ComProspection.StrEntityId
, StrEntity.Code AS StrEntityCode
, StrEntity.Description AS StrEntityDescription

, ComProspection.AdmCountryId
, AdmCountry.Code AS AdmCountryCode
, AdmCountry.Description AS AdmCountryDescription

, ComProspection.ComProspectionOriginId
, ComProspectionOrigin.Code AS ComProspectionOriginCode
, ComProspectionOrigin.Description AS ComProspectionOriginDescription

, ComProspection.ComProspectionOriginSupportId
, ComProspectionOriginSupport.Code AS ComProspectionOriginSupportCode
, ComProspectionOriginSupport.Description AS ComProspectionOriginSupportDescription

, ComProspection.ComActionMessageTypeId
, ComActionMessageType.Code AS ComActionMessageTypeCode
, ComActionMessageType.Description AS ComActionMessageTypeDescription

, ComActionMessageType.PKey AS ComProspectionOriginTypeId
, ComActionMessageType.Code AS ComProspectionOriginTypeCode
, ComActionMessageType.Description AS ComProspectionOriginTypeDescription

, ComProspection.CfgProspectId
, CfgTier.Code AS CfgTierCode
, CfgTier.Description AS CfgTierDescription
, IIF(CfgTier.AdmCityId IS NULL, CfgTier.City, AdmCity.Description) AS CfgTierCity
, LTRIM(RTRIM(ISNULL(CASE WHEN (CfgTier.Phone='' or CfgTier.Phone=null) THEN '' ELSE (ISNULL(CASE WHEN (CfgTier.PhoneIndicatif='') THEN '' ELSE ('(' + CfgTier.PhoneIndicatif + ')' )End,'') + ' ' + ISNULL(CfgTier.Phone, ''))End,''))) AS CfgTierPhone
, LTRIM(RTRIM(ISNULL(CASE WHEN (CfgTier.Mobile='' or CfgTier.Mobile=null) THEN '' ELSE (ISNULL(CASE WHEN (CfgTier.MobileIndicatif='') THEN '' ELSE ('(' + CfgTier.MobileIndicatif + ')' )End,'') + ' ' + ISNULL(CfgTier.Mobile, ''))End,''))) AS CfgTierMobile
, CfgTier.EMail AS CfgTierEMail
, CfgTier.FirstName AS CfgTierFirstName
, CfgTier.LastName AS CfgTierLastName
, CfgTier.Mobile AS CfgTierMobile1
, CfgTier.MobileIndicatif AS CfgTierMobile1Indicatif
, CfgTier.IDNumber AS CfgTierIDNumber
, CfgTierKind.Code AS CfgTierKindCode
, CfgTierKind.Description AS CfgTierKindDescription
, CfgTierType.PKey AS CfgTierTypeId
, CfgTierType.Code AS CfgTierTypeCode
, CfgTierType.Description AS CfgTierTypeDescription
, ComProspection.ComProspectionKindId
, ComProspectionKind.Code AS ComProspectionKindCode
, ComProspectionKind.Description AS ComProspectionKindDescription
, ISNULL(ComProspectionKind.AlertDelay,0) AS ComProspectionKindAlertDelay

, ComProspection.ComActionId
, ComAction.Code AS ComActionCode
, ComAction.Description AS ComActionDescription

, ComProspection.CfgCommercialId
, CfgCommercial.Code AS CfgCommercialCode
, CfgCommercial.Description AS CfgCommercialDescription
, CfgCommercial.Description AS Commercial

, ComProspection.StkHierarchyId
, CfgTranche.Code As CfgTrancheCode
, CfgTranche.Description As TrancheDescription

, CfgTranche.CfgProjectId
, CfgProject.Code AS CfgProjectCode
, CfgProject.Description AS CfgProjectDescription

, CfgProject.CfgCompanyId
, CfgCompany.Code AS CfgCompanyCode
, CfgCompany.Description AS CfgCompanyDescription

, ComProspection.MapLatitude
, ComProspection.MapLongitude

, ComProspection.StkItemId
, StkItem.Code AS Article
, StkItem.Description AS ArticleDescription

, StkItemCategory.Description AS ItemCategory
, StkItemType.Description AS ItemType
, StkPresentationType.Description AS ItemPresentationType

, ComProspection.CfgProposerId
, CfgProposer.Code AS CfgProposerCode
, CfgProposer.Description AS CfgProposerDescription

, ComProspection.CfgIntermediateId
, CfgIntermediate.Code AS CfgIntermediateCode
, CfgIntermediate.Description AS CfgIntermediateDescription

, ComProspection.MapRadius

, ComProspection.ComPaymentMethodId
, ComPaymentMethod.Code AS ComPaymentMethodCode
, ComPaymentMethod.Description AS ComPaymentMethodDescription

, ComProspection.ComUtilizationId
, ComUtilization.Code AS ComUtilizationCode
, ComUtilization.Description AS ComUtilizationDescription

, ComProspection.ComBudgetId
, ComBudget.Code AS ComBudgetCode
, ComBudget.Description AS ComBudgetDescription


, IIF(ComProspection.StkVocationId in ('7E729AD8-A030-4D20-91CF-9D8E9D89C318', '7897C0C8-E710-4C1D-9EAE-C4EF5294B7F0'), ComFolder.PKey , RntFolder.PKey) AS ComFolderId
, IIF(ComProspection.StkVocationId in ('7E729AD8-A030-4D20-91CF-9D8E9D89C318', '7897C0C8-E710-4C1D-9EAE-C4EF5294B7F0'), ComFolder.Code , RntFolder.Code) AS ComFolderCode
, IIF(ComProspection.StkVocationId in ('7E729AD8-A030-4D20-91CF-9D8E9D89C318', '7897C0C8-E710-4C1D-9EAE-C4EF5294B7F0'), ComFolder.Description , RntFolder.Description) AS ComFolderDescription

, IIF(ComProspection.StkVocationId in ('7E729AD8-A030-4D20-91CF-9D8E9D89C318', '7897C0C8-E710-4C1D-9EAE-C4EF5294B7F0'), ComFolder.AdvanceDate , RntFolder.AgreementDate1) AS AdvanceDate
, IIF(ComProspection.StkVocationId in ('7E729AD8-A030-4D20-91CF-9D8E9D89C318', '7897C0C8-E710-4C1D-9EAE-C4EF5294B7F0'), 
	ISNULL(DATEDIFF(DAY, ComProspection.DocDate, ComFolder.AdvanceDate) ,0), 
	ISNULL(DATEDIFF(DAY, ComProspection.DocDate, RntFolder.AgreementDate1) ,0)
	) AS Delai

, IIF(ComProspection.StkVocationId in ('7E729AD8-A030-4D20-91CF-9D8E9D89C318', '7897C0C8-E710-4C1D-9EAE-C4EF5294B7F0'), ComFolder.CfgCustomerId, RntFolder.CfgCustomerId) AS CfgCustomerId
, IIF(ComProspection.StkVocationId in ('7E729AD8-A030-4D20-91CF-9D8E9D89C318', '7897C0C8-E710-4C1D-9EAE-C4EF5294B7F0'), CfgCustomerSale.Code, CfgCustomerRent.Code) AS CfgCustomerCode
, IIF(ComProspection.StkVocationId in ('7E729AD8-A030-4D20-91CF-9D8E9D89C318', '7897C0C8-E710-4C1D-9EAE-C4EF5294B7F0'), CfgCustomerSale.Description, CfgCustomerRent.Description) AS CfgCustomerDescription

, ComProspection.IsOldCustomer

, IIF(ComProspection.StkVocationId in ('7E729AD8-A030-4D20-91CF-9D8E9D89C318', '7897C0C8-E710-4C1D-9EAE-C4EF5294B7F0'), StrStatus.Description, StrRntStatus.Description) AS Status
, IIF(ComProspection.StkVocationId in ('7E729AD8-A030-4D20-91CF-9D8E9D89C318', '7897C0C8-E710-4C1D-9EAE-C4EF5294B7F0'), StrStatus.BoolField002, StrRntStatus.BoolField002) AS StatusIsCancellation
, IIF(ComProspection.StkVocationId in ('7E729AD8-A030-4D20-91CF-9D8E9D89C318', '7897C0C8-E710-4C1D-9EAE-C4EF5294B7F0'), StrStatus.BoolField003, StrRntStatus.BoolField003) AS StatusIsClosing
, IIF(ComProspection.StkVocationId in ('7E729AD8-A030-4D20-91CF-9D8E9D89C318', '7897C0C8-E710-4C1D-9EAE-C4EF5294B7F0'), CfgImageSale.ImageData, CfgImageRent.ImageData) AS StatusImg

,ISNULL((SELECT STUFF((SELECT DISTINCT ',' + Description FROM (
	SELECT CfgTranche.Description from ComProspectionProject 
	INNER JOIN CfgTranche ON CfgTranche.PKey = ComProspectionProject.StkHierarchyId
	WHERE ComProspectionProject.ComProspectionId=ComProspection.PKey
	) AS Test FOR XML PATH ('')),1,1,'')), '') AS Project

,ISNULL((SELECT STUFF((SELECT DISTINCT ',' + Description FROM (
	SELECT StkItemCategory.Description from ComProspectionCategory 
	INNER JOIN StkItemCategory ON StkItemCategory.PKey = ComProspectionCategory.StkItemCategoryId
	WHERE ComProspectionCategory.ComProspectionId=ComProspection.PKey
	) AS Test FOR XML PATH ('')),1,1,'')), '') AS Categories

,ISNULL((SELECT STUFF((SELECT DISTINCT ',' + Description FROM (
	SELECT StkItemType.Description from ComProspectionType 
	INNER JOIN StkItemType ON StkItemType.PKey = ComProspectionType.StkItemTypeId
	WHERE ComProspectionType.ComProspectionId=ComProspection.PKey
	) AS Test FOR XML PATH ('')),1,1,'')), '') AS Types

,ISNULL((SELECT STUFF((SELECT DISTINCT ',' + Description FROM (
	SELECT AdmCity.Description from ComProspectionCity
	INNER JOIN AdmCity ON AdmCity.PKey = ComProspectionCity.AdmCityId
	WHERE ComProspectionCity.ComProspectionId=ComProspection.PKey
	) AS Test FOR XML PATH ('')),1,1,'')), '') AS Cities

,  (Select Min(ComActionMessage.DocDate) from ComActionMessage 
	where ComActionMessage.ComProspectionId=ComProspection.PKey 
	AND ComActionMessage.ComActionMessageTypeId='fea94cec-b244-4091-9cec-a9b2cffb917c'
	AND ComActionMessage.ComActionMessageCategoryId='69f8e7d6-1111-1111-1111-1aa8b1f02008' 
	) As FirstVisit

,  (Select Max(ComActionMessage.DocDate) from ComActionMessage 
	where ComActionMessage.ComProspectionId=ComProspection.PKey 
	AND ComActionMessage.ComActionMessageTypeId='fea94cec-b244-4091-9cec-a9b2cffb917c'
	AND ComActionMessage.ComActionMessageCategoryId='69f8e7d6-1111-1111-1111-1aa8b1f02008' 
	) As LastVisitDate

, (Select count(*) from ComActionMessage 
	where ComActionMessage.ComProspectionId=ComProspection.PKey 
	AND ComActionMessage.ComActionMessageTypeId='fea94cec-b244-4091-9cec-a9b2cffb917c'
	AND ComActionMessage.ComActionMessageCategoryId='69f8e7d6-1111-1111-1111-1aa8b1f02008' 
	) AS NbrVisit

, (Select Max(ComActionMessage.DocDate)  from ComActionMessage 
	where ComActionMessage.ComActionMessageCategoryId='69f8e7d6-1111-1111-1111-1aa8b1f02008' 
	AND ComActionMessage.DocDate <= GETDATE() AND ComActionMessage.IsSystem =0
	AND ComActionMessage.ComProspectionId = ComProspection.PKey 
	) As LastCommunicationDate

,ISNULL((Select top 1 ComActionMessageSubCategory.Description  from ComActionMessage 
	LEFT OUTER JOIN ComActionMessageSubCategory ON ComActionMessage.ComActionMessageSubCategoryId = ComActionMessageSubCategory.PKey
	where ComActionMessage.ComActionMessageCategoryId='69f8e7d6-1111-1111-1111-1aa8b1f02008' 
	AND ComActionMessage.DocDate <= GETDATE() AND ComActionMessage.IsSystem =0
	AND ComActionMessage.ComProspectionId = ComProspection.PKey 
	ORDER BY ComActionMessage.DocDate desc
	),'') As LastCommunicationResult

,ISNULL((Select top 1 IIF(ISNULL(ComActionMessage.AlertMessage,'') ='',ComActionMessageSubCategory.Description, ISNULL(ComActionMessage.AlertMessage,'')) from ComActionMessage 
	LEFT OUTER JOIN ComActionMessageSubCategory ON ComActionMessage.ComActionMessageSubCategoryId = ComActionMessageSubCategory.PKey
	where ComActionMessage.ComActionMessageCategoryId='69f8e7d6-1111-1111-1111-1aa8b1f02008' 
	AND ComActionMessage.DocDate <= GETDATE() AND ComActionMessage.IsSystem =0
	AND ComActionMessage.ComProspectionId = ComProspection.PKey 
	ORDER BY ComActionMessage.DocDate desc
	),'') As LastCommunicationMessage
	
, (Select Max(ComActionMessage.DocDate)  from ComActionMessage 
	where ComActionMessage.ComActionMessageCategoryId='69f8e7d6-1111-1111-1111-1aa8b1f02008' 
	AND ComActionMessage.DocDate <= GETDATE()
	AND ComActionMessage.ComProspectionId = ComProspection.PKey 
	) As LastActionDate

, (select Max(AdmAlertData.AlertDate) from AdmAlertData 
	INNER JOIN ComActionMessage ON ComActionMessage.PKey = AdmAlertData.RelatedKey
	WHERE AdmAlertData.Active = 1 
	AND AdmAlertData.AlertDate <= DATEADD(day,(Select CodeValue From AdmParamsInteger Where Code ='DefaultAlertDelai'),GETDATE()) 
	AND ComActionMessage.ComProspectionId=ComProspection.PKey
	AND ComActionMessage.ComActionMessageCategoryId='69f8e7d6-1111-1111-1111-1aa8b1f02008' 
	) As LastActiveAlertDate
	
, (IIF((Select Count(ComActionMessage.PKey)  from ComActionMessage where (ComActionMessage.ComActionMessageCategoryId IN('69f8e7d6-1111-1111-1111-1aa8b1f02008'))
	AND ComActionMessage.IsNotRead = 1 AND ComActionMessage.IsSystem =0 AND ComActionMessage.ComProspectionId = ComProspection.PKey 
	) > 0,1,0) ) As ThereIsInstructionNotRead

, (Select top 1 ComActionMessage.CfgEmployeeId  from ComActionMessage where (ComActionMessage.ComActionMessageCategoryId IN('69f8e7d6-1111-1111-1111-1aa8b1f02008'))
	AND ComActionMessage.IsNotRead = 1 AND ComActionMessage.IsSystem =0 AND ComActionMessage.ComProspectionId = ComProspection.PKey 
	ORDER BY ComActionMessage.DocDate Desc
	 ) As CfgCommercialForInstructionId

,(Select COUNT(ComActionMessage.DocDate)  from ComActionMessage 
	where ComActionMessage.ComActionMessageCategoryId='69f8e7d6-1111-1111-1111-1aa8b1f02008' 
	AND ComActionMessage.ComProspectionId = ComProspection.PKey
	)  AS ActionsMessageNumber

, DATEDIFF(DAY,ISNULL((Select top(1) ComActionMessage.DocDate  from ComActionMessage 
	where ComActionMessage.ComActionMessageCategoryId='69f8e7d6-1111-1111-1111-1aa8b1f02008' 
	AND ComActionMessage.DocDate <= GETDATE() 
	AND ComActionMessage.ComProspectionId = ComProspection.PKey 
	Order by ComActionMessage.DocDate DESC) ,ComProspection.DocDate),GETDATE()
	) AS InactivityTimeout


, ComProspection.UserCreate
, ComProspection.UserCreateDateTime
, ComProspection.UserUpdate
, ComProspection.UserUpdateDateTime
FROM            
ComProspection 
INNER JOIN StrEntity ON ComProspection.StrEntityId = StrEntity.PKey
LEFT OUTER JOIN AdmCountry ON ComProspection.AdmCountryId = AdmCountry.PKey
LEFT OUTER JOIN CfgTranche ON ComProspection.StkHierarchyId = CfgTranche.PKey 
LEFT OUTER JOIN StkItem ON ComProspection.StkItemId = StkItem.PKey 
LEFT OUTER JOIN StkItemType ON StkItem.StkItemTypeId = StkItemType.PKey 
LEFT OUTER JOIN StkItemCategory ON StkItem.StkItemCategoryId = StkItemCategory.PKey 
LEFT OUTER JOIN StkPresentationType ON StkItem.StkPresentationTypeId = StkPresentationType.PKey 
LEFT OUTER JOIN CfgTier ON ComProspection.CfgProspectId = CfgTier.PKey 
LEFT OUTER JOIN CfgTierType ON CfgTier.CfgTierTypeId = CfgTierType.PKey 
LEFT OUTER JOIN CfgTierKind ON CfgTier.CfgTierKindId = CfgTierKind.PKey 
LEFT OUTER JOIN AdmCity ON CfgTier.AdmCityId = AdmCity.PKey
LEFT OUTER JOIN StkVocation ON ComProspection.StkVocationId = StkVocation.PKey 
LEFT OUTER JOIN ComProspectionKind ON ComProspection.ComProspectionKindId = ComProspectionKind.PKey 
LEFT OUTER JOIN ComProspectionOrigin ON ComProspection.ComProspectionOriginId = ComProspectionOrigin.PKey
LEFT OUTER JOIN ComProspectionOriginSupport ON ComProspection.ComProspectionOriginSupportId = ComProspectionOriginSupport.PKey
LEFT OUTER JOIN ComAction ON ComProspection.ComActionId = ComAction.PKey
LEFT OUTER JOIN CfgTier AS CfgProposer ON ComProspection.CfgProposerId = CfgProposer.Pkey
LEFT OUTER JOIN CfgTier AS CfgIntermediate ON ComProspection.CfgIntermediateId = CfgIntermediate.Pkey
LEFT OUTER JOIN CfgTier AS CfgCommercial ON ComProspection.CfgCommercialId = CfgCommercial.PKey 
LEFT OUTER JOIN ComProspectionClosingReason ON ComProspection.ComProspectionClosingReasonId = ComProspectionClosingReason.PKey
LEFT OUTER JOIN ComActionMessageType ON ComProspection.ComActionMessageTypeId = ComActionMessageType.PKey
LEFT OUTER JOIN ComPaymentMethod ON ComProspection.ComPaymentMethodId = ComPaymentMethod.PKey

LEFT OUTER JOIN ComFolder ON ComFolder.ComProspectionId = ComProspection.Pkey
LEFT OUTER JOIN CfgTier AS CfgCustomerSale ON ComFolder.CfgCustomerId = CfgCustomerSale.Pkey
LEFT OUTER JOIN StrStatus ON ComFolder.StrStatusId = StrStatus.Pkey
LEFT OUTER JOIN CfgImage AS CfgImageSale ON StrStatus.CfgImageId = CfgImageSale.PKey

LEFT OUTER JOIN RntFolder ON RntFolder.ComProspectionId = ComProspection.Pkey
LEFT OUTER JOIN CfgTier AS CfgCustomerRent ON RntFolder.CfgCustomerId = CfgCustomerRent.Pkey
LEFT OUTER JOIN StrStatus StrRntStatus ON RntFolder.StrStatusId = StrRntStatus.Pkey
LEFT OUTER JOIN CfgImage AS CfgImageRent ON StrRntStatus.CfgImageId = CfgImageRent.PKey

LEFT OUTER JOIN CfgProject on CfgTranche.CfgProjectId = CfgProject.PKey
LEFT OUTER JOIN CfgCompany on CfgProject.CfgCompanyId = CfgCompany.PKey

LEFT OUTER JOIN ComUtilization ON ComProspection.ComUtilizationId = ComUtilization.PKey
LEFT OUTER JOIN ComBudget ON ComProspection.ComBudgetId = ComBudget.PKey

where ComProspection.StrEntityId <> 'B92DF3D8-158C-4E1E-827A-70FEB172A264'
) AS TT
GO
