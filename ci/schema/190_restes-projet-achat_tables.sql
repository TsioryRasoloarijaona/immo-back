-- FEATURE-restes-projet-achat.md §5 : documents à joindre d'une transition d'achat, colonnes de yesRemsProd.
-- 165_projet-config.sql n'avait créé que PKey et PrjMarketTypeId ; idempotent, applicable à yesRemsTest.
IF COL_LENGTH('dbo.BuyDocumentWorkFlowDocumentToAttach', 'BuyDocumentWorkFlowId') IS NULL
ALTER TABLE dbo.BuyDocumentWorkFlowDocumentToAttach ADD BuyDocumentWorkFlowId uniqueidentifier NULL,
    IsMandatory bit NULL, WithTitle bit NULL, UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL, AdmAttachedFileTypeId uniqueidentifier NULL,
    ForFirstWorkingOut bit NULL, ForIntermediateWorkingOut bit NULL, ForFinalWorkingOut bit NULL;
GO
