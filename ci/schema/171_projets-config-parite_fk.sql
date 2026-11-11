-- Lot projets-config-parite : cles etrangeres de yesRemsProd (noms et actions) des colonnes ajoutees par 170. CI seulement.
IF OBJECT_ID('dbo.PrjProjectCfgTrancheVentilation_CfgTranche_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjProjectCfgTrancheVentilation ADD CONSTRAINT PrjProjectCfgTrancheVentilation_CfgTranche_FK1
    FOREIGN KEY (CfgTrancheId) REFERENCES dbo.CfgTranche (PKey);
GO
IF OBJECT_ID('dbo.PrjMarketWarrantyLine_StlCategory_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjProjectProgressCriteriaLine ADD CONSTRAINT PrjMarketWarrantyLine_StlCategory_FK1
    FOREIGN KEY (StlCategoryId) REFERENCES dbo.StlCategory (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.BulMeetingPrjProjectProgressCriteriaLine_BulMeeting_FK1', 'F') IS NULL
ALTER TABLE dbo.BulMeetingPrjProjectProgressCriteriaLine ADD CONSTRAINT BulMeetingPrjProjectProgressCriteriaLine_BulMeeting_FK1
    FOREIGN KEY (BulMeetingId) REFERENCES dbo.BulMeeting (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.BulMeetingPrjProjectProgressCriteriaLine_PrjProjectProgressCriteriaLine_FK1', 'F') IS NULL
ALTER TABLE dbo.BulMeetingPrjProjectProgressCriteriaLine
    ADD CONSTRAINT BulMeetingPrjProjectProgressCriteriaLine_PrjProjectProgressCriteriaLine_FK1
    FOREIGN KEY (PrjProjectProgressCriteriaLineId) REFERENCES dbo.PrjProjectProgressCriteriaLine (PKey);
GO
IF OBJECT_ID('dbo.BulMeetingPrjProjectProgressCriteriaLine_CfgTrancheLevelingdefinition_FK1', 'F') IS NULL
ALTER TABLE dbo.BulMeetingPrjProjectProgressCriteriaLine
    ADD CONSTRAINT BulMeetingPrjProjectProgressCriteriaLine_CfgTrancheLevelingdefinition_FK1
    FOREIGN KEY (CfgTrancheLevelingdefinitionId) REFERENCES dbo.CfgTrancheLevelingDefinition (PKey);
GO
