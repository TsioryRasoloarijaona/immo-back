-- Lot P8a (FEATURE-patrimoine-faisabilite.md §1, §8 ter.13) : les 38 cles etrangeres de production des tables de 122,
-- actions de suppression de production ; idempotent. CI seulement (yesRemsTest n'a ni LndLand ni FK).
IF OBJECT_ID('dbo.FSFunding_StkFeasibilityStudy_FK1', 'F') IS NULL
ALTER TABLE dbo.FSFunding ADD CONSTRAINT FSFunding_StkFeasibilityStudy_FK1
    FOREIGN KEY (FeasibilityStudyId) REFERENCES dbo.StkFeasibilityStudy (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.FSPrevision__PrjMarketPrevisionModel_FK1', 'F') IS NULL
ALTER TABLE dbo.FSPrevision ADD CONSTRAINT FSPrevision__PrjMarketPrevisionModel_FK1
    FOREIGN KEY (PrjMarketPrevisionModelId) REFERENCES dbo.PrjMarketPrevisionModel (PKey);
GO
IF OBJECT_ID('dbo.FSPrevision_FeasibilityStudy_FK', 'F') IS NULL
ALTER TABLE dbo.FSPrevision ADD CONSTRAINT FSPrevision_FeasibilityStudy_FK
    FOREIGN KEY (FeasibilityStudyId) REFERENCES dbo.StkFeasibilityStudy (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.FSPrevision_StlCategory_FK', 'F') IS NULL
ALTER TABLE dbo.FSPrevision ADD CONSTRAINT FSPrevision_StlCategory_FK
    FOREIGN KEY (StlCategoryId) REFERENCES dbo.StlCategory (PKey);
GO
IF OBJECT_ID('dbo.FSReimbursement_FSFunding_FK1', 'F') IS NULL
ALTER TABLE dbo.FSReimbursement ADD CONSTRAINT FSReimbursement_FSFunding_FK1
    FOREIGN KEY (FSFundingId) REFERENCES dbo.FSFunding (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.PrjMarketPrevisionModel__StlCategory_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjMarketPrevisionModel ADD CONSTRAINT PrjMarketPrevisionModel__StlCategory_FK1
    FOREIGN KEY (StlCategoryId) REFERENCES dbo.StlCategory (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.StkFeasibilityStudy__PrvType_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFeasibilityStudy ADD CONSTRAINT StkFeasibilityStudy__PrvType_FK1
    FOREIGN KEY (PrvTypeId) REFERENCES dbo.PrvType (PKey);
GO
IF OBJECT_ID('dbo.StkFeasibilityStudy__PrvVersion_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFeasibilityStudy ADD CONSTRAINT StkFeasibilityStudy__PrvVersion_FK1
    FOREIGN KEY (PrvVersionId) REFERENCES dbo.PrvVersion (PKey);
GO
IF OBJECT_ID('dbo.StkFeasibilityStudy__StkFeasibilityStudyStatus_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFeasibilityStudy ADD CONSTRAINT StkFeasibilityStudy__StkFeasibilityStudyStatus_FK1
    FOREIGN KEY (StkFeasibilityStudyStatusId) REFERENCES dbo.StkFeasibilityStudyStatus (PKey);
GO
IF OBJECT_ID('dbo.StkFeasibilityStudy__StrEntity_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFeasibilityStudy ADD CONSTRAINT StkFeasibilityStudy__StrEntity_FK1
    FOREIGN KEY (StrEntityId) REFERENCES dbo.StrEntity (PKey);
GO
IF OBJECT_ID('dbo.StkFeasibilityStudy_CfgCompany_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFeasibilityStudy ADD CONSTRAINT StkFeasibilityStudy_CfgCompany_FK1
    FOREIGN KEY (CfgCompanyId) REFERENCES dbo.CfgCompany (PKey);
GO
IF OBJECT_ID('dbo.StkFeasibilityStudy_LndLand_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFeasibilityStudy ADD CONSTRAINT StkFeasibilityStudy_LndLand_FK1
    FOREIGN KEY (LndLandId) REFERENCES dbo.LndLand (PKey);
GO
IF OBJECT_ID('dbo.StkFeasibilityStudy_StkItem_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFeasibilityStudy ADD CONSTRAINT StkFeasibilityStudy_StkItem_FK1
    FOREIGN KEY (StkItemId) REFERENCES dbo.StkItem (PKey);
GO
IF OBJECT_ID('dbo.SStkFeasibilityStudyCfgTranche_CfgTranche_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFeasibilityStudyCfgTranche ADD CONSTRAINT SStkFeasibilityStudyCfgTranche_CfgTranche_FK1
    FOREIGN KEY (CfgTrancheId) REFERENCES dbo.CfgTranche (PKey);
GO
IF OBJECT_ID('dbo.SStkFeasibilityStudyCfgTranche_StkFeasibilityStudy_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFeasibilityStudyCfgTranche ADD CONSTRAINT SStkFeasibilityStudyCfgTranche_StkFeasibilityStudy_FK1
    FOREIGN KEY (FeasibilityStudyId) REFERENCES dbo.StkFeasibilityStudy (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.StkFeasibilityStudyCfgTrancheStkBaseUnit_StkFeasibilityStudyCfgTrancheId_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFeasibilityStudyCfgTrancheStkBaseUnit ADD CONSTRAINT StkFeasibilityStudyCfgTrancheStkBaseUnit_StkFeasibilityStudyCfgTrancheId_FK1
    FOREIGN KEY (StkFeasibilityStudyCfgTrancheId) REFERENCES dbo.StkFeasibilityStudyCfgTranche (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.StkFeasibilityStudyCfgTrancheStkBaseUnit_StkFSBaseUnitStkFeasibilityStudy_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFeasibilityStudyCfgTrancheStkBaseUnit ADD CONSTRAINT StkFeasibilityStudyCfgTrancheStkBaseUnit_StkFSBaseUnitStkFeasibilityStudy_FK1
    FOREIGN KEY (StkBaseUnitId) REFERENCES dbo.StkFSBaseUnitStkFeasibilityStudy (PKey);
GO
IF OBJECT_ID('dbo.StkFeasibilityStudyStatus__AdmWorkFlowStateType_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFeasibilityStudyStatus ADD CONSTRAINT StkFeasibilityStudyStatus__AdmWorkFlowStateType_FK1
    FOREIGN KEY (AdmWorkFlowStateTypeId) REFERENCES dbo.AdmWorkFlowStateType (PKey);
GO
IF OBJECT_ID('dbo.StkFeasibilityStudyStatus__CfgImage_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFeasibilityStudyStatus ADD CONSTRAINT StkFeasibilityStudyStatus__CfgImage_FK1
    FOREIGN KEY (CfgImageId) REFERENCES dbo.CfgImage (PKey);
GO
IF OBJECT_ID('dbo.StkFeasibilityStudyStatusDocumentToAttach__AdmAttachedFileType_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFeasibilityStudyStatusDocumentToAttach ADD CONSTRAINT StkFeasibilityStudyStatusDocumentToAttach__AdmAttachedFileType_FK1
    FOREIGN KEY (AdmAttachedFileTypeId) REFERENCES dbo.AdmAttachedFileType (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.StkFeasibilityStudyStatusDocumentToAttach__StkFeasibilityStudyStatus_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFeasibilityStudyStatusDocumentToAttach ADD CONSTRAINT StkFeasibilityStudyStatusDocumentToAttach__StkFeasibilityStudyStatus_FK1
    FOREIGN KEY (StkFeasibilityStudyStatusId) REFERENCES dbo.StkFeasibilityStudyStatus (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.StkFeasibilityStudyStatusHistory__AdmUser_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFeasibilityStudyStatusHistory ADD CONSTRAINT StkFeasibilityStudyStatusHistory__AdmUser_FK1
    FOREIGN KEY (AdmUserId) REFERENCES dbo.AdmUser (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.StkFeasibilityStudyStatusHistory__StkFeasibilityStudy_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFeasibilityStudyStatusHistory ADD CONSTRAINT StkFeasibilityStudyStatusHistory__StkFeasibilityStudy_FK1
    FOREIGN KEY (StkFeasibilityStudyId) REFERENCES dbo.StkFeasibilityStudy (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.StkFeasibilityStudyStatusHistory__StkFeasibilityStudyStatus_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFeasibilityStudyStatusHistory ADD CONSTRAINT StkFeasibilityStudyStatusHistory__StkFeasibilityStudyStatus_FK1
    FOREIGN KEY (StkFeasibilityStudyStatusId) REFERENCES dbo.StkFeasibilityStudyStatus (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.StkFeasibilityStudyWorkFlow__StkFeasibilityStudyStatus_End_FK', 'F') IS NULL
ALTER TABLE dbo.StkFeasibilityStudyWorkFlow ADD CONSTRAINT StkFeasibilityStudyWorkFlow__StkFeasibilityStudyStatus_End_FK
    FOREIGN KEY (EndStatusId) REFERENCES dbo.StkFeasibilityStudyStatus (PKey);
GO
IF OBJECT_ID('dbo.StkFeasibilityStudyWorkFlow__StkFeasibilityStudyStatus_Start_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFeasibilityStudyWorkFlow ADD CONSTRAINT StkFeasibilityStudyWorkFlow__StkFeasibilityStudyStatus_Start_FK1
    FOREIGN KEY (StartStatusId) REFERENCES dbo.StkFeasibilityStudyStatus (PKey);
GO
IF OBJECT_ID('dbo.StkFeasibilityStudyWorkFlow__StrEntity_FK', 'F') IS NULL
ALTER TABLE dbo.StkFeasibilityStudyWorkFlow ADD CONSTRAINT StkFeasibilityStudyWorkFlow__StrEntity_FK
    FOREIGN KEY (StrEntityId) REFERENCES dbo.StrEntity (PKey);
GO
IF OBJECT_ID('dbo.StkFeasibilityStudyWorkFlowAdmRole__AdmRole_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFeasibilityStudyWorkFlowAdmRole ADD CONSTRAINT StkFeasibilityStudyWorkFlowAdmRole__AdmRole_FK1
    FOREIGN KEY (AdmRoleId) REFERENCES dbo.AdmRole (PKey);
GO
IF OBJECT_ID('dbo.StkFeasibilityStudyWorkFlowAdmRole__StkFeasibilityStudyWorkFlow_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFeasibilityStudyWorkFlowAdmRole ADD CONSTRAINT StkFeasibilityStudyWorkFlowAdmRole__StkFeasibilityStudyWorkFlow_FK1
    FOREIGN KEY (WorkFlowId) REFERENCES dbo.StkFeasibilityStudyWorkFlow (PKey);
GO
IF OBJECT_ID('dbo.StkFeasibilityStudyWorkFlowDocumentToAttach__AdmAttachedFileTypeId_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFeasibilityStudyWorkFlowDocumentToAttach ADD CONSTRAINT StkFeasibilityStudyWorkFlowDocumentToAttach__AdmAttachedFileTypeId_FK1
    FOREIGN KEY (AdmAttachedFileTypeId) REFERENCES dbo.AdmAttachedFileType (PKey);
GO
IF OBJECT_ID('dbo.StkFeasibilityStudyWorkFlowDocumentToAttach__StkFeasibilityStudyWorkFlow_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFeasibilityStudyWorkFlowDocumentToAttach ADD CONSTRAINT StkFeasibilityStudyWorkFlowDocumentToAttach__StkFeasibilityStudyWorkFlow_FK1
    FOREIGN KEY (StkFeasibilityStudyWorkFlowId) REFERENCES dbo.StkFeasibilityStudyWorkFlow (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.StkFSBaseUnit__SpecificFunction_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFSBaseUnit ADD CONSTRAINT StkFSBaseUnit__SpecificFunction_FK1
    FOREIGN KEY (SpecificFunctionId) REFERENCES dbo.SpecificFunction (PKey);
GO
IF OBJECT_ID('dbo.StkFSBaseunit_StkItemCategory_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFSBaseUnit ADD CONSTRAINT StkFSBaseunit_StkItemCategory_FK1
    FOREIGN KEY (StkItemCategoryId) REFERENCES dbo.StkItemCategory (PKey);
GO
IF OBJECT_ID('dbo.StkFSBaseUnitStkFeasibilityStudy_StkFeasibilityStudy_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFSBaseUnitStkFeasibilityStudy ADD CONSTRAINT StkFSBaseUnitStkFeasibilityStudy_StkFeasibilityStudy_FK1
    FOREIGN KEY (FeasibilityStudyId) REFERENCES dbo.StkFeasibilityStudy (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.StkFSBaseUnitStkFeasibilityStudy_StkFSBaseUnity_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFSBaseUnitStkFeasibilityStudy ADD CONSTRAINT StkFSBaseUnitStkFeasibilityStudy_StkFSBaseUnity_FK1
    FOREIGN KEY (StkFSBaseUnitId) REFERENCES dbo.StkFSBaseUnit (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.StkFeasibilityStudyCfgTranche_StkFeasibilityStudy_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFSPrevisionLine ADD CONSTRAINT StkFeasibilityStudyCfgTranche_StkFeasibilityStudy_FK1
    FOREIGN KEY (StkFeasibilityStudyCfgTrancheId) REFERENCES dbo.StkFeasibilityStudyCfgTranche (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.StkFSPrevisionStkTranche_StkFeasibilityStudyCfgTranche_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFSPrevisionStkTranche ADD CONSTRAINT StkFSPrevisionStkTranche_StkFeasibilityStudyCfgTranche_FK1
    FOREIGN KEY (StkTrancheId) REFERENCES dbo.StkFeasibilityStudyCfgTranche (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.StkFSPrevisionStkTranche_StlCategory_FK1', 'F') IS NULL
ALTER TABLE dbo.StkFSPrevisionStkTranche ADD CONSTRAINT StkFSPrevisionStkTranche_StlCategory_FK1
    FOREIGN KEY (StlCategoryId) REFERENCES dbo.StlCategory (PKey);
GO
