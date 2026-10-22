-- Clés des rapports (FEATURE-tdb-vente-rapports-socle.md §3.2), noms et actions de production. Idempotent.
IF OBJECT_ID('dbo.AdmReportDataRole_AdmRole_FK1', 'F') IS NULL
    ALTER TABLE dbo.AdmReportDataRole ADD CONSTRAINT AdmReportDataRole_AdmRole_FK1
        FOREIGN KEY (AdmRoleId) REFERENCES dbo.AdmRole (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.AdmReportDataRole_AdmReportData_FK1', 'F') IS NULL
    ALTER TABLE dbo.AdmReportDataRole ADD CONSTRAINT AdmReportDataRole_AdmReportData_FK1
        FOREIGN KEY (AdmReportDataId) REFERENCES dbo.AdmReportData (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.AdmReportData__CfgImage_FK1', 'F') IS NULL
    ALTER TABLE dbo.AdmReportData ADD CONSTRAINT AdmReportData__CfgImage_FK1
        FOREIGN KEY (CfgImageId) REFERENCES dbo.CfgImage (PKey) ON DELETE CASCADE;
GO
