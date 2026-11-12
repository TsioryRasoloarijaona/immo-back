-- Clés de yesRemsProd : CASCADE vers le statut, NO ACTION vers le type de document.
IF OBJECT_ID('dbo.BuyDocumentStatusDocumentToAttach__BuyDocumentStatus_FK1', 'F') IS NULL
ALTER TABLE dbo.BuyDocumentStatusDocumentToAttach ADD CONSTRAINT BuyDocumentStatusDocumentToAttach__BuyDocumentStatus_FK1
    FOREIGN KEY (BuyDocumentStatusId) REFERENCES dbo.BuyDocumentStatus (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.ComFolderStatusDocumentToAttach__AdmAttachedFileType_FK1', 'F') IS NULL
ALTER TABLE dbo.BuyDocumentStatusDocumentToAttach ADD CONSTRAINT ComFolderStatusDocumentToAttach__AdmAttachedFileType_FK1
    FOREIGN KEY (AdmAttachedFileTypeId) REFERENCES dbo.AdmAttachedFileType (PKey);
GO
