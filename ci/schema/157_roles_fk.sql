-- Lot admin-roles (FEATURE-admin-roles.md) : noms et actions de suppression de yesRemsProd.
IF OBJECT_ID('dbo.BuyDocumentWorkFlowAdmRole__AdmRole_FK1', 'F') IS NULL
    ALTER TABLE dbo.BuyDocumentWorkFlowAdmRole ADD CONSTRAINT BuyDocumentWorkFlowAdmRole__AdmRole_FK1
        FOREIGN KEY (AdmRoleId) REFERENCES dbo.AdmRole (PKey);
GO
IF OBJECT_ID('dbo.BuyDocumentWorkFlowAdmRole__BuyDocumentWorkFlow_FK1', 'F') IS NULL
    ALTER TABLE dbo.BuyDocumentWorkFlowAdmRole ADD CONSTRAINT BuyDocumentWorkFlowAdmRole__BuyDocumentWorkFlow_FK1
        FOREIGN KEY (WorkFlowId) REFERENCES dbo.BuyDocumentWorkFlow (PKey);
GO
IF OBJECT_ID('dbo.AdmWebRights_AdmRole_FK1', 'F') IS NULL
    ALTER TABLE dbo.AdmWebRights ADD CONSTRAINT AdmWebRights_AdmRole_FK1
        FOREIGN KEY (RoleId) REFERENCES dbo.AdmRole (PKey) ON DELETE CASCADE;
GO
