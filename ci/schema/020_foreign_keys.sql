-- ============================================================================
-- Genere depuis yesRemsProd (schema seul, aucune donnee) — NE PAS EDITER A LA MAIN.
-- Regeneration : voir ci/schema/README.md
-- Perimetre : cloture transitive des cles etrangeres des tables mappees par les
-- entites JPA. Les vues, procedures et types table de la base legataire sont
-- volontairement exclus : aucune requete native ni @Subselect ne les utilise.
-- ============================================================================
ALTER TABLE [dbo].[ActAccount]  WITH CHECK ADD  CONSTRAINT [ActAccount__CfgCompany_FK1] FOREIGN KEY([CfgCompanyId])
REFERENCES [dbo].[CfgCompany] ([PKey])
GO
ALTER TABLE [dbo].[ActAccount] CHECK CONSTRAINT [ActAccount__CfgCompany_FK1]
GO
ALTER TABLE [dbo].[ActAccount]  WITH CHECK ADD  CONSTRAINT [ActAccount__CfgTier_FK1] FOREIGN KEY([CfgTierId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[ActAccount] CHECK CONSTRAINT [ActAccount__CfgTier_FK1]
GO
ALTER TABLE [dbo].[ActAccount]  WITH CHECK ADD  CONSTRAINT [ActAccount__ComFolder_FK1] FOREIGN KEY([ComFolderId])
REFERENCES [dbo].[ComFolder] ([PKey])
GO
ALTER TABLE [dbo].[ActAccount] CHECK CONSTRAINT [ActAccount__ComFolder_FK1]
GO
ALTER TABLE [dbo].[ActAccount]  WITH CHECK ADD  CONSTRAINT [ActAccount__RntFolder_FK1] FOREIGN KEY([RntFolderId])
REFERENCES [dbo].[RntFolder] ([PKey])
GO
ALTER TABLE [dbo].[ActAccount] CHECK CONSTRAINT [ActAccount__RntFolder_FK1]
GO
ALTER TABLE [dbo].[ActAccount]  WITH CHECK ADD  CONSTRAINT [ActAccount__StrEntity_FK1] FOREIGN KEY([StrEntityId])
REFERENCES [dbo].[StrEntity] ([PKey])
GO
ALTER TABLE [dbo].[ActAccount] CHECK CONSTRAINT [ActAccount__StrEntity_FK1]
GO
ALTER TABLE [dbo].[ActAccount]  WITH CHECK ADD  CONSTRAINT [ActAccount__SynFolder_FK1] FOREIGN KEY([SynFolderId])
REFERENCES [dbo].[SynFolder] ([PKey])
GO
ALTER TABLE [dbo].[ActAccount] CHECK CONSTRAINT [ActAccount__SynFolder_FK1]
GO
ALTER TABLE [dbo].[ActAccount]  WITH CHECK ADD  CONSTRAINT [ActAccount_ActAccountType_FK1] FOREIGN KEY([ActAccountTypeId])
REFERENCES [dbo].[ActAccountType] ([PKey])
GO
ALTER TABLE [dbo].[ActAccount] CHECK CONSTRAINT [ActAccount_ActAccountType_FK1]
GO
ALTER TABLE [dbo].[ActJournal]  WITH CHECK ADD  CONSTRAINT [ActJournal__CfgCompany_FK1] FOREIGN KEY([CfgCompanyId])
REFERENCES [dbo].[CfgCompany] ([PKey])
GO
ALTER TABLE [dbo].[ActJournal] CHECK CONSTRAINT [ActJournal__CfgCompany_FK1]
GO
ALTER TABLE [dbo].[ActJournal]  WITH CHECK ADD  CONSTRAINT [ActJournal__StrEntity_FK1] FOREIGN KEY([StrEntityId])
REFERENCES [dbo].[StrEntity] ([PKey])
GO
ALTER TABLE [dbo].[ActJournal] CHECK CONSTRAINT [ActJournal__StrEntity_FK1]
GO
ALTER TABLE [dbo].[ActJournal]  WITH CHECK ADD  CONSTRAINT [ActJournal_ActJournalType_FK1] FOREIGN KEY([ActJournalTypeId])
REFERENCES [dbo].[ActJournalType] ([PKey])
GO
ALTER TABLE [dbo].[ActJournal] CHECK CONSTRAINT [ActJournal_ActJournalType_FK1]
GO
ALTER TABLE [dbo].[AdmAttachedFile]  WITH CHECK ADD  CONSTRAINT [AdmAttachedFile_AdmAttachedFileType_FK] FOREIGN KEY([AdmAttachedFileTypeId])
REFERENCES [dbo].[AdmAttachedFileType] ([PKey])
GO
ALTER TABLE [dbo].[AdmAttachedFile] CHECK CONSTRAINT [AdmAttachedFile_AdmAttachedFileType_FK]
GO
ALTER TABLE [dbo].[AdmCity]  WITH CHECK ADD  CONSTRAINT [AdmCity_AdmCountry_FK1] FOREIGN KEY([AdmCountryId])
REFERENCES [dbo].[AdmCountry] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[AdmCity] CHECK CONSTRAINT [AdmCity_AdmCountry_FK1]
GO
ALTER TABLE [dbo].[AdmEvent]  WITH CHECK ADD  CONSTRAINT [AdmEvent__ComActionMessageSubCategory_FK1] FOREIGN KEY([ComActionMessageSubCategoryId])
REFERENCES [dbo].[ComActionMessageSubCategory] ([PKey])
GO
ALTER TABLE [dbo].[AdmEvent] CHECK CONSTRAINT [AdmEvent__ComActionMessageSubCategory_FK1]
GO
ALTER TABLE [dbo].[AdmEvent]  WITH CHECK ADD  CONSTRAINT [AdmEvent_AdmEventType_FK1] FOREIGN KEY([AdmEventTypeId])
REFERENCES [dbo].[AdmEventType] ([PKey])
GO
ALTER TABLE [dbo].[AdmEvent] CHECK CONSTRAINT [AdmEvent_AdmEventType_FK1]
GO
ALTER TABLE [dbo].[AdmEvent]  WITH CHECK ADD  CONSTRAINT [AdmEvent_CfgEmloyee_FK1] FOREIGN KEY([CfgEmloyeeId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[AdmEvent] CHECK CONSTRAINT [AdmEvent_CfgEmloyee_FK1]
GO
ALTER TABLE [dbo].[AdmEvent]  WITH CHECK ADD  CONSTRAINT [AdmEvent_CfgTranche_FK] FOREIGN KEY([CfgTrancheId])
REFERENCES [dbo].[CfgTranche] ([PKey])
GO
ALTER TABLE [dbo].[AdmEvent] CHECK CONSTRAINT [AdmEvent_CfgTranche_FK]
GO
ALTER TABLE [dbo].[AdmEvent]  WITH CHECK ADD  CONSTRAINT [AdmEvent_ComFolder_FK1] FOREIGN KEY([ComFolderId])
REFERENCES [dbo].[ComFolder] ([PKey])
GO
ALTER TABLE [dbo].[AdmEvent] CHECK CONSTRAINT [AdmEvent_ComFolder_FK1]
GO
ALTER TABLE [dbo].[AdmEvent]  WITH CHECK ADD  CONSTRAINT [AdmEvent_StrEntity_FK1] FOREIGN KEY([StrEntityId])
REFERENCES [dbo].[StrEntity] ([PKey])
GO
ALTER TABLE [dbo].[AdmEvent] CHECK CONSTRAINT [AdmEvent_StrEntity_FK1]
GO
ALTER TABLE [dbo].[AdmSequence]  WITH CHECK ADD  CONSTRAINT [AdmSequence__CfgCompany_FK1] FOREIGN KEY([CfgCompanyId])
REFERENCES [dbo].[CfgCompany] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[AdmSequence] CHECK CONSTRAINT [AdmSequence__CfgCompany_FK1]
GO
ALTER TABLE [dbo].[AdmTierTitle]  WITH CHECK ADD  CONSTRAINT [AdmTierTitle_AdmSexType_FK1] FOREIGN KEY([AdmSexTypeId])
REFERENCES [dbo].[AdmSexType] ([PKey])
GO
ALTER TABLE [dbo].[AdmTierTitle] CHECK CONSTRAINT [AdmTierTitle_AdmSexType_FK1]
GO
ALTER TABLE [dbo].[AdmUser]  WITH CHECK ADD  CONSTRAINT [AdmUser_AdmRole_FK1] FOREIGN KEY([WebRoleId])
REFERENCES [dbo].[AdmRole] ([PKey])
GO
ALTER TABLE [dbo].[AdmUser] CHECK CONSTRAINT [AdmUser_AdmRole_FK1]
GO
ALTER TABLE [dbo].[AdmUser]  WITH CHECK ADD  CONSTRAINT [AdmUser_AdmWebAlertDefinition_FK1] FOREIGN KEY([WebAlertDefinitionId])
REFERENCES [dbo].[AdmWebAlertDefinitions] ([PKey])
GO
ALTER TABLE [dbo].[AdmUser] CHECK CONSTRAINT [AdmUser_AdmWebAlertDefinition_FK1]
GO
ALTER TABLE [dbo].[AdmUser]  WITH CHECK ADD  CONSTRAINT [AdmUser_AdmWebCulture_FK1] FOREIGN KEY([WebCultureId])
REFERENCES [dbo].[AdmWebCultures] ([PKey])
GO
ALTER TABLE [dbo].[AdmUser] CHECK CONSTRAINT [AdmUser_AdmWebCulture_FK1]
GO
ALTER TABLE [dbo].[AdmUser]  WITH CHECK ADD  CONSTRAINT [AdmUser_CfgTier_FK1] FOREIGN KEY([CfgEmployeeId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[AdmUser] CHECK CONSTRAINT [AdmUser_CfgTier_FK1]
GO
ALTER TABLE [dbo].[AdmUserHabilitation]  WITH CHECK ADD  CONSTRAINT [AdmUserHabilitation__AdmUser_FK1] FOREIGN KEY([AdmUserId])
REFERENCES [dbo].[AdmUser] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[AdmUserHabilitation] CHECK CONSTRAINT [AdmUserHabilitation__AdmUser_FK1]
GO
ALTER TABLE [dbo].[AdmUserHabilitation]  WITH CHECK ADD  CONSTRAINT [AdmUserHabilitation__CfgCompany_FK1] FOREIGN KEY([CfgCompanyId])
REFERENCES [dbo].[CfgCompany] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[AdmUserHabilitation] CHECK CONSTRAINT [AdmUserHabilitation__CfgCompany_FK1]
GO
ALTER TABLE [dbo].[AdmUserHabilitation]  WITH CHECK ADD  CONSTRAINT [AdmUserHabilitation__CfgProject_FK1] FOREIGN KEY([CfgProjectId])
REFERENCES [dbo].[CfgProject] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[AdmUserHabilitation] CHECK CONSTRAINT [AdmUserHabilitation__CfgProject_FK1]
GO
ALTER TABLE [dbo].[AdmUserHabilitation]  WITH CHECK ADD  CONSTRAINT [AdmUserHabilitation__CfgTranche_FK1] FOREIGN KEY([CfgTrancheId])
REFERENCES [dbo].[CfgTranche] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[AdmUserHabilitation] CHECK CONSTRAINT [AdmUserHabilitation__CfgTranche_FK1]
GO
ALTER TABLE [dbo].[AdmUserHabilitation]  WITH CHECK ADD  CONSTRAINT [AdmUserHabilitation__PrjProject_FK1] FOREIGN KEY([PrjProjectId])
REFERENCES [dbo].[PrjProject] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[AdmUserHabilitation] CHECK CONSTRAINT [AdmUserHabilitation__PrjProject_FK1]
GO
ALTER TABLE [dbo].[AdmUserRole]  WITH CHECK ADD  CONSTRAINT [AdmRole_AdmUserRole_FK1] FOREIGN KEY([AdmRoleId])
REFERENCES [dbo].[AdmRole] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[AdmUserRole] CHECK CONSTRAINT [AdmRole_AdmUserRole_FK1]
GO
ALTER TABLE [dbo].[AdmUserRole]  WITH CHECK ADD  CONSTRAINT [AdmUser_AdmUserRole_FK1] FOREIGN KEY([AdmUserId])
REFERENCES [dbo].[AdmUser] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[AdmUserRole] CHECK CONSTRAINT [AdmUser_AdmUserRole_FK1]
GO
ALTER TABLE [dbo].[AdmWebCultures]  WITH CHECK ADD  CONSTRAINT [FK_dbo.AdmWebCultures_dbo.AdmWebLanguages_LanguageId] FOREIGN KEY([LanguageId])
REFERENCES [dbo].[AdmWebLanguages] ([PKey])
GO
ALTER TABLE [dbo].[AdmWebCultures] CHECK CONSTRAINT [FK_dbo.AdmWebCultures_dbo.AdmWebLanguages_LanguageId]
GO
ALTER TABLE [dbo].[AppUserCredential]  WITH CHECK ADD  CONSTRAINT [FK_AppUserCredential_AdmUser] FOREIGN KEY([AdmUserId])
REFERENCES [dbo].[AdmUser] ([PKey])
GO
ALTER TABLE [dbo].[AppUserCredential] CHECK CONSTRAINT [FK_AppUserCredential_AdmUser]
GO
ALTER TABLE [dbo].[AppApiClient]  WITH CHECK ADD  CONSTRAINT [FK_AppApiClient_AdmUser] FOREIGN KEY([AdmUserId])
REFERENCES [dbo].[AdmUser] ([PKey])
GO
ALTER TABLE [dbo].[AppApiClient] CHECK CONSTRAINT [FK_AppApiClient_AdmUser]
GO
ALTER TABLE [dbo].[BuyDocument]  WITH CHECK ADD  CONSTRAINT [BuyDocument__BuyDocument_FK1] FOREIGN KEY([InvoiceSourceId])
REFERENCES [dbo].[BuyDocument] ([PKey])
GO
ALTER TABLE [dbo].[BuyDocument] CHECK CONSTRAINT [BuyDocument__BuyDocument_FK1]
GO
ALTER TABLE [dbo].[BuyDocument]  WITH CHECK ADD  CONSTRAINT [BuyDocument__BuyDocumentStatus_FK1] FOREIGN KEY([BuyDocumentStatusId])
REFERENCES [dbo].[BuyDocumentStatus] ([PKey])
GO
ALTER TABLE [dbo].[BuyDocument] CHECK CONSTRAINT [BuyDocument__BuyDocumentStatus_FK1]
GO
ALTER TABLE [dbo].[BuyDocument]  WITH CHECK ADD  CONSTRAINT [BuyDocument__BuyOrder_FK1] FOREIGN KEY([BuyOrderId])
REFERENCES [dbo].[BuyDocument] ([PKey])
GO
ALTER TABLE [dbo].[BuyDocument] CHECK CONSTRAINT [BuyDocument__BuyOrder_FK1]
GO
ALTER TABLE [dbo].[BuyDocument]  WITH CHECK ADD  CONSTRAINT [BuyDocument__CfgTier_FK1] FOREIGN KEY([CfgTierTriggerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[BuyDocument] CHECK CONSTRAINT [BuyDocument__CfgTier_FK1]
GO
ALTER TABLE [dbo].[BuyDocument]  WITH CHECK ADD  CONSTRAINT [BuyDocument__PrjMarketType_FK1] FOREIGN KEY([PrjMarketTypeId])
REFERENCES [dbo].[PrjMarketType] ([PKey])
GO
ALTER TABLE [dbo].[BuyDocument] CHECK CONSTRAINT [BuyDocument__PrjMarketType_FK1]
GO
ALTER TABLE [dbo].[BuyDocument]  WITH CHECK ADD  CONSTRAINT [BuyDocument__PrjWorkingOut_FK1] FOREIGN KEY([PrjWorkingOutId])
REFERENCES [dbo].[PrjWorkingOut] ([PKey])
GO
ALTER TABLE [dbo].[BuyDocument] CHECK CONSTRAINT [BuyDocument__PrjWorkingOut_FK1]
GO
ALTER TABLE [dbo].[BuyDocument]  WITH CHECK ADD  CONSTRAINT [BuyDocument__RntFolder_FK1] FOREIGN KEY([RntFolderId])
REFERENCES [dbo].[RntFolder] ([PKey])
GO
ALTER TABLE [dbo].[BuyDocument] CHECK CONSTRAINT [BuyDocument__RntFolder_FK1]
GO
ALTER TABLE [dbo].[BuyDocument]  WITH CHECK ADD  CONSTRAINT [BuyDocument__StlCurrency_FK1] FOREIGN KEY([StlCurrencyId])
REFERENCES [dbo].[StlCurrency] ([PKey])
GO
ALTER TABLE [dbo].[BuyDocument] CHECK CONSTRAINT [BuyDocument__StlCurrency_FK1]
GO
ALTER TABLE [dbo].[BuyDocument]  WITH CHECK ADD  CONSTRAINT [BuyDocument__StrEntity_FK1] FOREIGN KEY([StrEntityId])
REFERENCES [dbo].[StrEntity] ([PKey])
GO
ALTER TABLE [dbo].[BuyDocument] CHECK CONSTRAINT [BuyDocument__StrEntity_FK1]
GO
ALTER TABLE [dbo].[BuyDocument]  WITH CHECK ADD  CONSTRAINT [BuyDocument_AdmModule_FK1] FOREIGN KEY([AdmModuleId])
REFERENCES [dbo].[AdmModule] ([PKey])
GO
ALTER TABLE [dbo].[BuyDocument] CHECK CONSTRAINT [BuyDocument_AdmModule_FK1]
GO
ALTER TABLE [dbo].[BuyDocument]  WITH CHECK ADD  CONSTRAINT [BuyDocument_BuyDocumentInvoice_FK1] FOREIGN KEY([BuyInvoiceId])
REFERENCES [dbo].[BuyDocument] ([PKey])
GO
ALTER TABLE [dbo].[BuyDocument] CHECK CONSTRAINT [BuyDocument_BuyDocumentInvoice_FK1]
GO
ALTER TABLE [dbo].[BuyDocument]  WITH CHECK ADD  CONSTRAINT [BuyDocument_BuyDocumentQuote_FK1] FOREIGN KEY([BuyQuoteId])
REFERENCES [dbo].[BuyDocument] ([PKey])
GO
ALTER TABLE [dbo].[BuyDocument] CHECK CONSTRAINT [BuyDocument_BuyDocumentQuote_FK1]
GO
ALTER TABLE [dbo].[BuyDocument]  WITH CHECK ADD  CONSTRAINT [BuyDocument_BuySelection_FK] FOREIGN KEY([BuySelectionId])
REFERENCES [dbo].[BuySelection] ([PKey])
GO
ALTER TABLE [dbo].[BuyDocument] CHECK CONSTRAINT [BuyDocument_BuySelection_FK]
GO
ALTER TABLE [dbo].[BuyDocument]  WITH CHECK ADD  CONSTRAINT [BuyDocument_CfgCompany_FK1] FOREIGN KEY([CfgCompanyId])
REFERENCES [dbo].[CfgCompany] ([PKey])
GO
ALTER TABLE [dbo].[BuyDocument] CHECK CONSTRAINT [BuyDocument_CfgCompany_FK1]
GO
ALTER TABLE [dbo].[BuyDocument]  WITH CHECK ADD  CONSTRAINT [BuyDocument_ComFolder_FK1] FOREIGN KEY([ComFolderId])
REFERENCES [dbo].[ComFolder] ([PKey])
GO
ALTER TABLE [dbo].[BuyDocument] CHECK CONSTRAINT [BuyDocument_ComFolder_FK1]
GO
ALTER TABLE [dbo].[BuyDocument]  WITH CHECK ADD  CONSTRAINT [BuyDocument_PrjClause_FK1] FOREIGN KEY([PrjClauseId])
REFERENCES [dbo].[PrjClause] ([PKey])
GO
ALTER TABLE [dbo].[BuyDocument] CHECK CONSTRAINT [BuyDocument_PrjClause_FK1]
GO
ALTER TABLE [dbo].[BuyDocument]  WITH CHECK ADD  CONSTRAINT [BuyDocument_PrjProject_FK1] FOREIGN KEY([PrjProjectId])
REFERENCES [dbo].[PrjProject] ([PKey])
GO
ALTER TABLE [dbo].[BuyDocument] CHECK CONSTRAINT [BuyDocument_PrjProject_FK1]
GO
ALTER TABLE [dbo].[BuyDocument]  WITH CHECK ADD  CONSTRAINT [BuyDocument_StkExpense_FK1] FOREIGN KEY([StkExpenseId])
REFERENCES [dbo].[StkExpense] ([Pkey])
GO
ALTER TABLE [dbo].[BuyDocument] CHECK CONSTRAINT [BuyDocument_StkExpense_FK1]
GO
ALTER TABLE [dbo].[BuyDocument]  WITH CHECK ADD  CONSTRAINT [BuyDocument_SynFolder_FK1] FOREIGN KEY([SynFolderId])
REFERENCES [dbo].[SynFolder] ([PKey])
GO
ALTER TABLE [dbo].[BuyDocument] CHECK CONSTRAINT [BuyDocument_SynFolder_FK1]
GO
ALTER TABLE [dbo].[BuyDocument]  WITH CHECK ADD  CONSTRAINT [BuyFolder_BuyDocument_FK1] FOREIGN KEY([BuyFolderId])
REFERENCES [dbo].[BuyFolder] ([Pkey])
GO
ALTER TABLE [dbo].[BuyDocument] CHECK CONSTRAINT [BuyFolder_BuyDocument_FK1]
GO
ALTER TABLE [dbo].[BuyDocument]  WITH CHECK ADD  CONSTRAINT [BuyLoan_BuyDocument_FK1] FOREIGN KEY([BuyLoanId])
REFERENCES [dbo].[BuyLoan] ([PKey])
GO
ALTER TABLE [dbo].[BuyDocument] CHECK CONSTRAINT [BuyLoan_BuyDocument_FK1]
GO
ALTER TABLE [dbo].[BuyDocument]  WITH CHECK ADD  CONSTRAINT [BuyLoanSchedule_BuyDocument_FK1] FOREIGN KEY([BuyLoanScheduleId])
REFERENCES [dbo].[BuyLoanSchedule] ([PKey])
GO
ALTER TABLE [dbo].[BuyDocument] CHECK CONSTRAINT [BuyLoanSchedule_BuyDocument_FK1]
GO
ALTER TABLE [dbo].[BuyDocument]  WITH CHECK ADD  CONSTRAINT [CfgTier_BuyDocument_FK1] FOREIGN KEY([CfgTierId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[BuyDocument] CHECK CONSTRAINT [CfgTier_BuyDocument_FK1]
GO
ALTER TABLE [dbo].[BuyDocument]  WITH CHECK ADD  CONSTRAINT [PrjMarket_BuyDocument_FK1] FOREIGN KEY([PrjMarketId])
REFERENCES [dbo].[PrjMarket] ([PKey])
GO
ALTER TABLE [dbo].[BuyDocument] CHECK CONSTRAINT [PrjMarket_BuyDocument_FK1]
GO
ALTER TABLE [dbo].[BuyDocumentLine]  WITH CHECK ADD  CONSTRAINT [BuyDocument_BuyDocumentLine_FK1] FOREIGN KEY([BuyDocumentId])
REFERENCES [dbo].[BuyDocument] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[BuyDocumentLine] CHECK CONSTRAINT [BuyDocument_BuyDocumentLine_FK1]
GO
ALTER TABLE [dbo].[BuyDocumentLine]  WITH CHECK ADD  CONSTRAINT [BuyDocumentLine_StlCategory_FK1] FOREIGN KEY([StlCategoryId])
REFERENCES [dbo].[StlCategory] ([PKey])
GO
ALTER TABLE [dbo].[BuyDocumentLine] CHECK CONSTRAINT [BuyDocumentLine_StlCategory_FK1]
GO
ALTER TABLE [dbo].[BuyDocumentLine]  WITH CHECK ADD  CONSTRAINT [BuyItem_BuyDocumentLine_FK1] FOREIGN KEY([BuyItemId])
REFERENCES [dbo].[BuyItem] ([PKey])
GO
ALTER TABLE [dbo].[BuyDocumentLine] CHECK CONSTRAINT [BuyItem_BuyDocumentLine_FK1]
GO
ALTER TABLE [dbo].[BuyDocumentStatus]  WITH CHECK ADD  CONSTRAINT [BuyDocumentStatus__AdmWorkFlowStateType_FK1] FOREIGN KEY([AdmWorkFlowStateTypeId])
REFERENCES [dbo].[AdmWorkFlowStateType] ([PKey])
GO
ALTER TABLE [dbo].[BuyDocumentStatus] CHECK CONSTRAINT [BuyDocumentStatus__AdmWorkFlowStateType_FK1]
GO
ALTER TABLE [dbo].[BuyDocumentStatus]  WITH CHECK ADD  CONSTRAINT [BuyDocumentStatus__CfgImage_FK1] FOREIGN KEY([CfgImageId])
REFERENCES [dbo].[CfgImage] ([PKey])
GO
ALTER TABLE [dbo].[BuyDocumentStatus] CHECK CONSTRAINT [BuyDocumentStatus__CfgImage_FK1]
GO
ALTER TABLE [dbo].[BuyItem]  WITH CHECK ADD  CONSTRAINT [BuyItem_BuyItemUnderFamily_FK1] FOREIGN KEY([BuyItemUnderFamilyId])
REFERENCES [dbo].[BuyItemUnderFamily] ([Pkey])
GO
ALTER TABLE [dbo].[BuyItem] CHECK CONSTRAINT [BuyItem_BuyItemUnderFamily_FK1]
GO
ALTER TABLE [dbo].[BuyItem]  WITH CHECK ADD  CONSTRAINT [StlCategory_BuyItem_FK1] FOREIGN KEY([StlCategoryId])
REFERENCES [dbo].[StlCategory] ([PKey])
GO
ALTER TABLE [dbo].[BuyItem] CHECK CONSTRAINT [StlCategory_BuyItem_FK1]
GO
ALTER TABLE [dbo].[BuyItemFamily]  WITH CHECK ADD  CONSTRAINT [BuyItemFamily_StlCategory_FK1] FOREIGN KEY([StlCategoryId])
REFERENCES [dbo].[StlCategory] ([PKey])
GO
ALTER TABLE [dbo].[BuyItemFamily] CHECK CONSTRAINT [BuyItemFamily_StlCategory_FK1]
GO
ALTER TABLE [dbo].[BuyItemUnderFamily]  WITH CHECK ADD  CONSTRAINT [BuyItemUnderFamily_BuyItemFamily_FK1] FOREIGN KEY([BuyItemFamilyId])
REFERENCES [dbo].[BuyItemFamily] ([Pkey])
GO
ALTER TABLE [dbo].[BuyItemUnderFamily] CHECK CONSTRAINT [BuyItemUnderFamily_BuyItemFamily_FK1]
GO
ALTER TABLE [dbo].[BuyItemUnderFamily]  WITH CHECK ADD  CONSTRAINT [BuyItemUnderFamily_StlCategory_FK1] FOREIGN KEY([StlCategoryId])
REFERENCES [dbo].[StlCategory] ([PKey])
GO
ALTER TABLE [dbo].[BuyItemUnderFamily] CHECK CONSTRAINT [BuyItemUnderFamily_StlCategory_FK1]
GO
ALTER TABLE [dbo].[BuyLoan]  WITH CHECK ADD  CONSTRAINT [BuyLoan__BuyLoanType_FK1] FOREIGN KEY([BuyLoanTypeId])
REFERENCES [dbo].[BuyLoanType] ([PKey])
GO
ALTER TABLE [dbo].[BuyLoan] CHECK CONSTRAINT [BuyLoan__BuyLoanType_FK1]
GO
ALTER TABLE [dbo].[BuyLoan]  WITH CHECK ADD  CONSTRAINT [BuyLoan__CfgCompany_FK1] FOREIGN KEY([CfgCompanyId])
REFERENCES [dbo].[CfgCompany] ([PKey])
GO
ALTER TABLE [dbo].[BuyLoan] CHECK CONSTRAINT [BuyLoan__CfgCompany_FK1]
GO
ALTER TABLE [dbo].[BuyLoan]  WITH CHECK ADD  CONSTRAINT [BuyLoan__StrEntity_FK1] FOREIGN KEY([StrEntityId])
REFERENCES [dbo].[StrEntity] ([PKey])
GO
ALTER TABLE [dbo].[BuyLoan] CHECK CONSTRAINT [BuyLoan__StrEntity_FK1]
GO
ALTER TABLE [dbo].[BuyLoan]  WITH CHECK ADD  CONSTRAINT [PrjProject_BuyLoan_FK1] FOREIGN KEY([PrjProjectId])
REFERENCES [dbo].[PrjProject] ([PKey])
GO
ALTER TABLE [dbo].[BuyLoan] CHECK CONSTRAINT [PrjProject_BuyLoan_FK1]
GO
ALTER TABLE [dbo].[BuyLoan]  WITH CHECK ADD  CONSTRAINT [StkItem_BuyLoan_FK1] FOREIGN KEY([StkItemId])
REFERENCES [dbo].[StkItem] ([PKey])
GO
ALTER TABLE [dbo].[BuyLoan] CHECK CONSTRAINT [StkItem_BuyLoan_FK1]
GO
ALTER TABLE [dbo].[BuyLoanSchedule]  WITH CHECK ADD  CONSTRAINT [BuySubLoan_BuyLoanSchedule_FK1] FOREIGN KEY([BuySubLoanId])
REFERENCES [dbo].[BuySubLoan] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[BuyLoanSchedule] CHECK CONSTRAINT [BuySubLoan_BuyLoanSchedule_FK1]
GO
ALTER TABLE [dbo].[BuyLoanType]  WITH CHECK ADD  CONSTRAINT [BuyLoanType__BaseStlCategory_FK1] FOREIGN KEY([BaseStlCategoryId])
REFERENCES [dbo].[StlCategory] ([PKey])
GO
ALTER TABLE [dbo].[BuyLoanType] CHECK CONSTRAINT [BuyLoanType__BaseStlCategory_FK1]
GO
ALTER TABLE [dbo].[BuyLoanType]  WITH CHECK ADD  CONSTRAINT [BuyLoanType__CommissionStlCategory_FK1] FOREIGN KEY([CommissionStlCategoryId])
REFERENCES [dbo].[StlCategory] ([PKey])
GO
ALTER TABLE [dbo].[BuyLoanType] CHECK CONSTRAINT [BuyLoanType__CommissionStlCategory_FK1]
GO
ALTER TABLE [dbo].[BuyLoanType]  WITH CHECK ADD  CONSTRAINT [BuyLoanType__InterestStlCategory_FK1] FOREIGN KEY([InterestStlCategoryId])
REFERENCES [dbo].[StlCategory] ([PKey])
GO
ALTER TABLE [dbo].[BuyLoanType] CHECK CONSTRAINT [BuyLoanType__InterestStlCategory_FK1]
GO
ALTER TABLE [dbo].[BuySelection]  WITH CHECK ADD  CONSTRAINT [BuySelection__AdmModule_FK1] FOREIGN KEY([AdmModuleId])
REFERENCES [dbo].[AdmModule] ([PKey])
GO
ALTER TABLE [dbo].[BuySelection] CHECK CONSTRAINT [BuySelection__AdmModule_FK1]
GO
ALTER TABLE [dbo].[BuySelection]  WITH CHECK ADD  CONSTRAINT [BuySelection_BuySelectionKind_FK] FOREIGN KEY([BuySelectionKindId])
REFERENCES [dbo].[BuySelectionKind] ([PKey])
GO
ALTER TABLE [dbo].[BuySelection] CHECK CONSTRAINT [BuySelection_BuySelectionKind_FK]
GO
ALTER TABLE [dbo].[BuySelection]  WITH CHECK ADD  CONSTRAINT [BuySelection_CfgCompany_FK] FOREIGN KEY([CfgCompanyId])
REFERENCES [dbo].[CfgCompany] ([PKey])
GO
ALTER TABLE [dbo].[BuySelection] CHECK CONSTRAINT [BuySelection_CfgCompany_FK]
GO
ALTER TABLE [dbo].[BuySelection]  WITH CHECK ADD  CONSTRAINT [BuySelection_CfgTier_FK] FOREIGN KEY([CfgTierId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[BuySelection] CHECK CONSTRAINT [BuySelection_CfgTier_FK]
GO
ALTER TABLE [dbo].[BuySelection]  WITH CHECK ADD  CONSTRAINT [BuySelection_CfgTierTrigger_FK1] FOREIGN KEY([CfgTierTriggerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[BuySelection] CHECK CONSTRAINT [BuySelection_CfgTierTrigger_FK1]
GO
ALTER TABLE [dbo].[BuySelection]  WITH CHECK ADD  CONSTRAINT [BuySelection_PrjMarket_FK] FOREIGN KEY([PrjMarketId])
REFERENCES [dbo].[PrjMarket] ([PKey])
GO
ALTER TABLE [dbo].[BuySelection] CHECK CONSTRAINT [BuySelection_PrjMarket_FK]
GO
ALTER TABLE [dbo].[BuySelection]  WITH CHECK ADD  CONSTRAINT [BuySelection_PrjMarketType_FK] FOREIGN KEY([PrjMarketTypeId])
REFERENCES [dbo].[PrjMarketType] ([PKey])
GO
ALTER TABLE [dbo].[BuySelection] CHECK CONSTRAINT [BuySelection_PrjMarketType_FK]
GO
ALTER TABLE [dbo].[BuySelection]  WITH CHECK ADD  CONSTRAINT [BuySelection_PrjProject_FK] FOREIGN KEY([PrjProjectId])
REFERENCES [dbo].[PrjProject] ([PKey])
GO
ALTER TABLE [dbo].[BuySelection] CHECK CONSTRAINT [BuySelection_PrjProject_FK]
GO
ALTER TABLE [dbo].[BuySelection]  WITH CHECK ADD  CONSTRAINT [BuySelection_StlCategory_FK] FOREIGN KEY([StlCategoryId])
REFERENCES [dbo].[StlCategory] ([PKey])
GO
ALTER TABLE [dbo].[BuySelection] CHECK CONSTRAINT [BuySelection_StlCategory_FK]
GO
ALTER TABLE [dbo].[BuySelection]  WITH CHECK ADD  CONSTRAINT [BuySelection_StlCurrency_FK] FOREIGN KEY([StlCurrencyId])
REFERENCES [dbo].[StlCurrency] ([PKey])
GO
ALTER TABLE [dbo].[BuySelection] CHECK CONSTRAINT [BuySelection_StlCurrency_FK]
GO
ALTER TABLE [dbo].[BuySelection]  WITH CHECK ADD  CONSTRAINT [BuySelection_StlSettlementTyper_FK1] FOREIGN KEY([StlSettlementTypeId])
REFERENCES [dbo].[StlSettlementType] ([PKey])
GO
ALTER TABLE [dbo].[BuySelection] CHECK CONSTRAINT [BuySelection_StlSettlementTyper_FK1]
GO
ALTER TABLE [dbo].[BuySelection]  WITH CHECK ADD  CONSTRAINT [BuySelection_StrEntity_FK] FOREIGN KEY([StrEntityId])
REFERENCES [dbo].[StrEntity] ([PKey])
GO
ALTER TABLE [dbo].[BuySelection] CHECK CONSTRAINT [BuySelection_StrEntity_FK]
GO
ALTER TABLE [dbo].[BuySelection]  WITH CHECK ADD  CONSTRAINT [BuySelection_StrStatus_FK] FOREIGN KEY([StrStatusId])
REFERENCES [dbo].[StrStatus] ([PKey])
GO
ALTER TABLE [dbo].[BuySelection] CHECK CONSTRAINT [BuySelection_StrStatus_FK]
GO
ALTER TABLE [dbo].[BuySelectionKind]  WITH CHECK ADD  CONSTRAINT [BuySelectionKind__StrEntityManagment_FK1] FOREIGN KEY([StrEntityManagmentId])
REFERENCES [dbo].[StrEntity] ([PKey])
GO
ALTER TABLE [dbo].[BuySelectionKind] CHECK CONSTRAINT [BuySelectionKind__StrEntityManagment_FK1]
GO
ALTER TABLE [dbo].[BuySubLoan]  WITH CHECK ADD  CONSTRAINT [BuyLoan_BuySubLoan_FK1] FOREIGN KEY([BuyLoanId])
REFERENCES [dbo].[BuyLoan] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[BuySubLoan] CHECK CONSTRAINT [BuyLoan_BuySubLoan_FK1]
GO
ALTER TABLE [dbo].[CfgCompany]  WITH CHECK ADD  CONSTRAINT [CfgCompany__CfgTier_SalesManager_FK1] FOREIGN KEY([CfgSalesManagerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[CfgCompany] CHECK CONSTRAINT [CfgCompany__CfgTier_SalesManager_FK1]
GO
ALTER TABLE [dbo].[CfgCompany]  WITH CHECK ADD  CONSTRAINT [CfgCompany__CfgTier_TechnicalManager_FK1] FOREIGN KEY([CfgTechnicalManagerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[CfgCompany] CHECK CONSTRAINT [CfgCompany__CfgTier_TechnicalManager_FK1]
GO
ALTER TABLE [dbo].[CfgCompany]  WITH CHECK ADD  CONSTRAINT [CfgCompany__StlCurrency_FK1] FOREIGN KEY([StlCurrencyId])
REFERENCES [dbo].[StlCurrency] ([PKey])
GO
ALTER TABLE [dbo].[CfgCompany] CHECK CONSTRAINT [CfgCompany__StlCurrency_FK1]
GO
ALTER TABLE [dbo].[CfgCompany]  WITH CHECK ADD  CONSTRAINT [CfgCompany_AdmCountry_FK1] FOREIGN KEY([AdmCountryId])
REFERENCES [dbo].[AdmCountry] ([PKey])
GO
ALTER TABLE [dbo].[CfgCompany] CHECK CONSTRAINT [CfgCompany_AdmCountry_FK1]
GO
ALTER TABLE [dbo].[CfgCompany]  WITH CHECK ADD  CONSTRAINT [CfgCompany_CfgGroupShareholder_FK1] FOREIGN KEY([CfgGroupShareholderId])
REFERENCES [dbo].[CfgGroupShareholder] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[CfgCompany] CHECK CONSTRAINT [CfgCompany_CfgGroupShareholder_FK1]
GO
ALTER TABLE [dbo].[CfgCompany]  WITH CHECK ADD  CONSTRAINT [CfgCompany_CfgTier_FK1] FOREIGN KEY([CfgTierId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[CfgCompany] CHECK CONSTRAINT [CfgCompany_CfgTier_FK1]
GO
ALTER TABLE [dbo].[CfgCompany]  WITH CHECK ADD  CONSTRAINT [CfgCompany_CfgTier_SignatoryId_FK1] FOREIGN KEY([AdmSignatoryId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[CfgCompany] CHECK CONSTRAINT [CfgCompany_CfgTier_SignatoryId_FK1]
GO
ALTER TABLE [dbo].[CfgProject]  WITH CHECK ADD  CONSTRAINT [CfgProject__CfgTier_SalesManager_FK1] FOREIGN KEY([CfgSalesManagerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[CfgProject] CHECK CONSTRAINT [CfgProject__CfgTier_SalesManager_FK1]
GO
ALTER TABLE [dbo].[CfgProject]  WITH CHECK ADD  CONSTRAINT [CfgProject__CfgTier_TechnicalManager_FK1] FOREIGN KEY([CfgTechnicalManagerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[CfgProject] CHECK CONSTRAINT [CfgProject__CfgTier_TechnicalManager_FK1]
GO
ALTER TABLE [dbo].[CfgProject]  WITH CHECK ADD  CONSTRAINT [CfgProject_AdmCountry_FK1] FOREIGN KEY([AdmCountryId])
REFERENCES [dbo].[AdmCountry] ([PKey])
GO
ALTER TABLE [dbo].[CfgProject] CHECK CONSTRAINT [CfgProject_AdmCountry_FK1]
GO
ALTER TABLE [dbo].[CfgProject]  WITH CHECK ADD  CONSTRAINT [CfgProject_CfgCompany_FK1] FOREIGN KEY([CfgCompanyId])
REFERENCES [dbo].[CfgCompany] ([PKey])
GO
ALTER TABLE [dbo].[CfgProject] CHECK CONSTRAINT [CfgProject_CfgCompany_FK1]
GO
ALTER TABLE [dbo].[CfgProject]  WITH CHECK ADD  CONSTRAINT [CfgProject_CfgTier_SignatoryId_FK1] FOREIGN KEY([AdmSignatoryId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[CfgProject] CHECK CONSTRAINT [CfgProject_CfgTier_SignatoryId_FK1]
GO
ALTER TABLE [dbo].[CfgProjectZone]  WITH CHECK ADD  CONSTRAINT [CfgProjectZone__CfgProject_FK1] FOREIGN KEY([CfgProjectId])
REFERENCES [dbo].[CfgProject] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[CfgProjectZone] CHECK CONSTRAINT [CfgProjectZone__CfgProject_FK1]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgBank_CfgTier_FK1] FOREIGN KEY([CfgBankId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgBank_CfgTier_FK1]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgCommercial_CfgTier_FK1] FOREIGN KEY([CfgCommercialId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgCommercial_CfgTier_FK1]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgContact_CfgTier_FK1] FOREIGN KEY([CfgContactId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgContact_CfgTier_FK1]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgTier__ActJournal_FK1] FOREIGN KEY([ActJournalId])
REFERENCES [dbo].[ActJournal] ([PKey])
ON DELETE SET NULL
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgTier__ActJournal_FK1]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgTier__AdmRank_FK1] FOREIGN KEY([AdmRankId])
REFERENCES [dbo].[AdmRank] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgTier__AdmRank_FK1]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgTier__CfgCompany_FK1] FOREIGN KEY([CfgCompanyId])
REFERENCES [dbo].[CfgCompany] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgTier__CfgCompany_FK1]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgTier__GrhAgreementType_FK1] FOREIGN KEY([GrhAgreementTypeId])
REFERENCES [dbo].[GrhAgreementType] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgTier__GrhAgreementType_FK1]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgTier__GrhDepartment_FK1] FOREIGN KEY([GrhDepartmentId])
REFERENCES [dbo].[GrhDepartment] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgTier__GrhDepartment_FK1]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgTier__GrhEchelon_FK1] FOREIGN KEY([GrhEchelonId])
REFERENCES [dbo].[GrhEchelon] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgTier__GrhEchelon_FK1]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgTier__GrhEmployeeCategory_FK1] FOREIGN KEY([GrhEmployeeCategoryId])
REFERENCES [dbo].[GrhEmployeeCategory] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgTier__GrhEmployeeCategory_FK1]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgTier__GrhPaySlipModel_FK1] FOREIGN KEY([GrhPaySlipModelId])
REFERENCES [dbo].[GrhPaySlipModel] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgTier__GrhPaySlipModel_FK1]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgTier__StlCurrency_FK1] FOREIGN KEY([StlCurrencyId])
REFERENCES [dbo].[StlCurrency] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgTier__StlCurrency_FK1]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgTier_ActAccount_FK1] FOREIGN KEY([ActAccountId])
REFERENCES [dbo].[ActAccount] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgTier_ActAccount_FK1]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgTier_AdmCity_FK] FOREIGN KEY([AdmCityId])
REFERENCES [dbo].[AdmCity] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgTier_AdmCity_FK]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgTier_AdmCountry_FK] FOREIGN KEY([AdmCountryId])
REFERENCES [dbo].[AdmCountry] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgTier_AdmCountry_FK]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgTier_AdmCountry_Residence_FK] FOREIGN KEY([AdmCountryResidenceId])
REFERENCES [dbo].[AdmCountry] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgTier_AdmCountry_Residence_FK]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgTier_AdmFamilySituation_FK1] FOREIGN KEY([AdmFamilySituationId])
REFERENCES [dbo].[AdmFamilySituation] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgTier_AdmFamilySituation_FK1]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgTier_AdmIdentityDocumentType_FK1] FOREIGN KEY([AdmIdentityDocumentTypeId])
REFERENCES [dbo].[AdmIdentityDocumentType] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgTier_AdmIdentityDocumentType_FK1]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgTier_AdmLiability_FK1] FOREIGN KEY([AdmLiabilityId])
REFERENCES [dbo].[AdmLiability] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgTier_AdmLiability_FK1]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgTier_AdmMatrimonialRegime_FK1] FOREIGN KEY([AdmMatrimonialRegimeId])
REFERENCES [dbo].[AdmMatrimonialRegime] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgTier_AdmMatrimonialRegime_FK1]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgTier_AdmNationality_FK1] FOREIGN KEY([AdmNationalityId])
REFERENCES [dbo].[AdmNationality] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgTier_AdmNationality_FK1]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgTier_AdmProfession_FK1] FOREIGN KEY([AdmProfessionId])
REFERENCES [dbo].[AdmProfession] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgTier_AdmProfession_FK1]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgTier_AdmResidenceType_FK1] FOREIGN KEY([AdmResidenceTypeId])
REFERENCES [dbo].[AdmResidenceType] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgTier_AdmResidenceType_FK1]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgTier_AdmSalary_FK1] FOREIGN KEY([AdmSalaryId])
REFERENCES [dbo].[AdmSalary] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgTier_AdmSalary_FK1]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgTier_AdmSexType_FK1] FOREIGN KEY([AdmSexTypeId])
REFERENCES [dbo].[AdmSexType] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgTier_AdmSexType_FK1]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgTier_AdmSocialForm_FK1] FOREIGN KEY([AdmSocialFormId])
REFERENCES [dbo].[AdmSocialForm] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgTier_AdmSocialForm_FK1]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgTier_AdmTierTitle_FK] FOREIGN KEY([AdmTierTitleId])
REFERENCES [dbo].[AdmTierTitle] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgTier_AdmTierTitle_FK]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgTier_CfgTierKind_FK1] FOREIGN KEY([CfgTierKindId])
REFERENCES [dbo].[CfgTierKind] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgTier_CfgTierKind_FK1]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgTier_CfgTierType_FK1] FOREIGN KEY([CfgTierTypeId])
REFERENCES [dbo].[CfgTierType] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgTier_CfgTierType_FK1]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgTier_GrhEmployeeClass_FK1] FOREIGN KEY([GrhEmployeeClassId])
REFERENCES [dbo].[GrhEmployeeClass] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgTier_GrhEmployeeClass_FK1]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgTier_GrhEmployeeType_FK1] FOREIGN KEY([GrhEmployeeTypeId])
REFERENCES [dbo].[GrhEmployeeType] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgTier_GrhEmployeeType_FK1]
GO
ALTER TABLE [dbo].[CfgTier]  WITH CHECK ADD  CONSTRAINT [CfgTier_StlDeductionType_FK1] FOREIGN KEY([StlDeductionTypeId])
REFERENCES [dbo].[StlDeductionType] ([PKey])
GO
ALTER TABLE [dbo].[CfgTier] CHECK CONSTRAINT [CfgTier_StlDeductionType_FK1]
GO
ALTER TABLE [dbo].[CfgTierContact]  WITH CHECK ADD  CONSTRAINT [CfgTierContact__CfgContact_FK1] FOREIGN KEY([CfgContactId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[CfgTierContact] CHECK CONSTRAINT [CfgTierContact__CfgContact_FK1]
GO
ALTER TABLE [dbo].[CfgTierContact]  WITH CHECK ADD  CONSTRAINT [CfgTierContact__CfgTier_FK1] FOREIGN KEY([CfgTierId])
REFERENCES [dbo].[CfgTier] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[CfgTierContact] CHECK CONSTRAINT [CfgTierContact__CfgTier_FK1]
GO
ALTER TABLE [dbo].[CfgTierKind]  WITH CHECK ADD  CONSTRAINT [CfgTierKind__CfgCompany_FK1] FOREIGN KEY([CfgCompanyId])
REFERENCES [dbo].[CfgCompany] ([PKey])
GO
ALTER TABLE [dbo].[CfgTierKind] CHECK CONSTRAINT [CfgTierKind__CfgCompany_FK1]
GO
ALTER TABLE [dbo].[CfgTierKind]  WITH CHECK ADD  CONSTRAINT [CfgTierKind_CfgTierType_FK1] FOREIGN KEY([CfgTierTypeId])
REFERENCES [dbo].[CfgTierType] ([PKey])
GO
ALTER TABLE [dbo].[CfgTierKind] CHECK CONSTRAINT [CfgTierKind_CfgTierType_FK1]
GO
ALTER TABLE [dbo].[CfgTranche]  WITH CHECK ADD  CONSTRAINT [CfgTranche__CfgTierOther_FK1] FOREIGN KEY([CfgTierOtherId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[CfgTranche] CHECK CONSTRAINT [CfgTranche__CfgTierOther_FK1]
GO
ALTER TABLE [dbo].[CfgTranche]  WITH CHECK ADD  CONSTRAINT [CfgTranche_ActAccount_FK1] FOREIGN KEY([ActAccountId])
REFERENCES [dbo].[ActAccount] ([PKey])
GO
ALTER TABLE [dbo].[CfgTranche] CHECK CONSTRAINT [CfgTranche_ActAccount_FK1]
GO
ALTER TABLE [dbo].[CfgTranche]  WITH CHECK ADD  CONSTRAINT [CfgTranche_AdmCountry_FK1] FOREIGN KEY([AdmCountryId])
REFERENCES [dbo].[AdmCountry] ([PKey])
GO
ALTER TABLE [dbo].[CfgTranche] CHECK CONSTRAINT [CfgTranche_AdmCountry_FK1]
GO
ALTER TABLE [dbo].[CfgTranche]  WITH CHECK ADD  CONSTRAINT [Cfgtranche_CfgClaimsManager_FK1] FOREIGN KEY([CfgClaimsManagerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[CfgTranche] CHECK CONSTRAINT [Cfgtranche_CfgClaimsManager_FK1]
GO
ALTER TABLE [dbo].[CfgTranche]  WITH CHECK ADD  CONSTRAINT [CfgTranche_CfgCompany_FK1] FOREIGN KEY([CfgProjectId])
REFERENCES [dbo].[CfgProject] ([PKey])
GO
ALTER TABLE [dbo].[CfgTranche] CHECK CONSTRAINT [CfgTranche_CfgCompany_FK1]
GO
ALTER TABLE [dbo].[CfgTranche]  WITH CHECK ADD  CONSTRAINT [CfgTranche_CfgTrancheStanding_FK1] FOREIGN KEY([CfgTrancheStandingId])
REFERENCES [dbo].[CfgTrancheStanding] ([PKey])
GO
ALTER TABLE [dbo].[CfgTranche] CHECK CONSTRAINT [CfgTranche_CfgTrancheStanding_FK1]
GO
ALTER TABLE [dbo].[CfgTranche]  WITH CHECK ADD  CONSTRAINT [CfgTranche_LndLevel_FK1] FOREIGN KEY([LndLevelId])
REFERENCES [dbo].[LndLevel] ([PKey])
GO
ALTER TABLE [dbo].[CfgTranche] CHECK CONSTRAINT [CfgTranche_LndLevel_FK1]
GO
ALTER TABLE [dbo].[CfgTranche]  WITH CHECK ADD  CONSTRAINT [CfgTranche_PrjProject_FK2] FOREIGN KEY([PrjProjectId])
REFERENCES [dbo].[PrjProject] ([PKey])
GO
ALTER TABLE [dbo].[CfgTranche] CHECK CONSTRAINT [CfgTranche_PrjProject_FK2]
GO
ALTER TABLE [dbo].[CfgTranche]  WITH CHECK ADD  CONSTRAINT [CfgTranche_StkLeveling_FK] FOREIGN KEY([StkLevelingId])
REFERENCES [dbo].[StkLeveling] ([PKey])
GO
ALTER TABLE [dbo].[CfgTranche] CHECK CONSTRAINT [CfgTranche_StkLeveling_FK]
GO
ALTER TABLE [dbo].[CfgTranche]  WITH CHECK ADD  CONSTRAINT [CfgTranche_StkTrancheHeading_FK] FOREIGN KEY([StkTrancheHeadingId])
REFERENCES [dbo].[StkTrancheHeading] ([PKey])
GO
ALTER TABLE [dbo].[CfgTranche] CHECK CONSTRAINT [CfgTranche_StkTrancheHeading_FK]
GO
ALTER TABLE [dbo].[CfgTrancheLevel]  WITH CHECK ADD  CONSTRAINT [CfgTrancheLevel__CfgTranche_FK1] FOREIGN KEY([CfgTrancheId])
REFERENCES [dbo].[CfgTranche] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[CfgTrancheLevel] CHECK CONSTRAINT [CfgTrancheLevel__CfgTranche_FK1]
GO
ALTER TABLE [dbo].[CfgTrancheLevel]  WITH CHECK ADD  CONSTRAINT [CfgTrancheLevel__LndLevel_FK1] FOREIGN KEY([LndLevelId])
REFERENCES [dbo].[LndLevel] ([PKey])
GO
ALTER TABLE [dbo].[CfgTrancheLevel] CHECK CONSTRAINT [CfgTrancheLevel__LndLevel_FK1]
GO
ALTER TABLE [dbo].[CfgTrancheStatus]  WITH CHECK ADD  CONSTRAINT [CfgTrancheStatus__ComFolderStatusPaymentType_FK1] FOREIGN KEY([ComFolderStatusPaymentTypeId])
REFERENCES [dbo].[ComFolderStatusPaymentType] ([PKey])
GO
ALTER TABLE [dbo].[CfgTrancheStatus] CHECK CONSTRAINT [CfgTrancheStatus__ComFolderStatusPaymentType_FK1]
GO
ALTER TABLE [dbo].[CfgTrancheStatus]  WITH CHECK ADD  CONSTRAINT [CfgTrancheStatus__StrFolderEntity_FK1] FOREIGN KEY([StrFolderEntityId])
REFERENCES [dbo].[StrEntity] ([PKey])
GO
ALTER TABLE [dbo].[CfgTrancheStatus] CHECK CONSTRAINT [CfgTrancheStatus__StrFolderEntity_FK1]
GO
ALTER TABLE [dbo].[CfgTrancheStatus]  WITH CHECK ADD  CONSTRAINT [CfgTrancheStatus__StrStatus_FK1] FOREIGN KEY([StrStatusId])
REFERENCES [dbo].[StrStatus] ([PKey])
GO
ALTER TABLE [dbo].[CfgTrancheStatus] CHECK CONSTRAINT [CfgTrancheStatus__StrStatus_FK1]
GO
ALTER TABLE [dbo].[CfgTrancheStatus]  WITH CHECK ADD  CONSTRAINT [CfgTrancheStatus_CfgTranche_FK1] FOREIGN KEY([CfgTrancheId])
REFERENCES [dbo].[CfgTranche] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[CfgTrancheStatus] CHECK CONSTRAINT [CfgTrancheStatus_CfgTranche_FK1]
GO
ALTER TABLE [dbo].[CfgTrancheStkItem]  WITH CHECK ADD  CONSTRAINT [CfgTrancheStkItem_CfgTranche_FK1] FOREIGN KEY([CfgTrancheId])
REFERENCES [dbo].[CfgTranche] ([PKey])
GO
ALTER TABLE [dbo].[CfgTrancheStkItem] CHECK CONSTRAINT [CfgTrancheStkItem_CfgTranche_FK1]
GO
ALTER TABLE [dbo].[CfgTrancheStkItem]  WITH CHECK ADD  CONSTRAINT [CfgTrancheStkItem_StkItem_FK1] FOREIGN KEY([StkItemId])
REFERENCES [dbo].[StkItem] ([PKey])
GO
ALTER TABLE [dbo].[CfgTrancheStkItem] CHECK CONSTRAINT [CfgTrancheStkItem_StkItem_FK1]
GO
ALTER TABLE [dbo].[ComAction]  WITH CHECK ADD  CONSTRAINT [ComAction__BuyFolder_FK1] FOREIGN KEY([BuyFolderId])
REFERENCES [dbo].[BuyFolder] ([Pkey])
GO
ALTER TABLE [dbo].[ComAction] CHECK CONSTRAINT [ComAction__BuyFolder_FK1]
GO
ALTER TABLE [dbo].[ComAction]  WITH CHECK ADD  CONSTRAINT [ComAction__CfgTier_FK1] FOREIGN KEY([CfgEmployeeId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[ComAction] CHECK CONSTRAINT [ComAction__CfgTier_FK1]
GO
ALTER TABLE [dbo].[ComActionMessage]  WITH CHECK ADD  CONSTRAINT [ComActionMessage__PrmRequestOffer_FK3] FOREIGN KEY([PrmRequestOfferId])
REFERENCES [dbo].[PrmRequestOffer] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[ComActionMessage] CHECK CONSTRAINT [ComActionMessage__PrmRequestOffer_FK3]
GO
ALTER TABLE [dbo].[ComActionMessage]  WITH CHECK ADD  CONSTRAINT [ComActionMessage__RntFolder_FK3] FOREIGN KEY([RntFolderId])
REFERENCES [dbo].[RntFolder] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[ComActionMessage] CHECK CONSTRAINT [ComActionMessage__RntFolder_FK3]
GO
ALTER TABLE [dbo].[ComActionMessage]  WITH CHECK ADD  CONSTRAINT [ComActionMessage__StrFolderStatus_FK1] FOREIGN KEY([StrFolderStatusId])
REFERENCES [dbo].[StrStatus] ([PKey])
GO
ALTER TABLE [dbo].[ComActionMessage] CHECK CONSTRAINT [ComActionMessage__StrFolderStatus_FK1]
GO
ALTER TABLE [dbo].[ComActionMessage]  WITH CHECK ADD  CONSTRAINT [ComActionMessage__SynFolder_FK3] FOREIGN KEY([SynFolderId])
REFERENCES [dbo].[SynFolder] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[ComActionMessage] CHECK CONSTRAINT [ComActionMessage__SynFolder_FK3]
GO
ALTER TABLE [dbo].[ComActionMessage]  WITH CHECK ADD  CONSTRAINT [ComActionMessage_AdmEvent_FK1] FOREIGN KEY([AdmEventId])
REFERENCES [dbo].[AdmEvent] ([PKey])
GO
ALTER TABLE [dbo].[ComActionMessage] CHECK CONSTRAINT [ComActionMessage_AdmEvent_FK1]
GO
ALTER TABLE [dbo].[ComActionMessage]  WITH CHECK ADD  CONSTRAINT [ComActionMessage_AdmUser_FK] FOREIGN KEY([AdmUserId])
REFERENCES [dbo].[AdmUser] ([PKey])
GO
ALTER TABLE [dbo].[ComActionMessage] CHECK CONSTRAINT [ComActionMessage_AdmUser_FK]
GO
ALTER TABLE [dbo].[ComActionMessage]  WITH CHECK ADD  CONSTRAINT [ComActionMessage_CfgEmployee_FK] FOREIGN KEY([CfgEmployeeId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[ComActionMessage] CHECK CONSTRAINT [ComActionMessage_CfgEmployee_FK]
GO
ALTER TABLE [dbo].[ComActionMessage]  WITH CHECK ADD  CONSTRAINT [ComActionMessage_CfgTier_FK] FOREIGN KEY([CfgTierId])
REFERENCES [dbo].[CfgTier] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[ComActionMessage] CHECK CONSTRAINT [ComActionMessage_CfgTier_FK]
GO
ALTER TABLE [dbo].[ComActionMessage]  WITH CHECK ADD  CONSTRAINT [ComActionMessage_ComActionMessageCategory_FK] FOREIGN KEY([ComActionMessageCategoryId])
REFERENCES [dbo].[ComActionMessageCategory] ([PKey])
GO
ALTER TABLE [dbo].[ComActionMessage] CHECK CONSTRAINT [ComActionMessage_ComActionMessageCategory_FK]
GO
ALTER TABLE [dbo].[ComActionMessage]  WITH CHECK ADD  CONSTRAINT [ComActionMessage_ComActionMessageSubCategory_FK] FOREIGN KEY([ComActionMessageSubCategoryId])
REFERENCES [dbo].[ComActionMessageSubCategory] ([PKey])
GO
ALTER TABLE [dbo].[ComActionMessage] CHECK CONSTRAINT [ComActionMessage_ComActionMessageSubCategory_FK]
GO
ALTER TABLE [dbo].[ComActionMessage]  WITH CHECK ADD  CONSTRAINT [ComActionMessage_ComActionMessageType_FK] FOREIGN KEY([ComActionMessageTypeId])
REFERENCES [dbo].[ComActionMessageType] ([PKey])
GO
ALTER TABLE [dbo].[ComActionMessage] CHECK CONSTRAINT [ComActionMessage_ComActionMessageType_FK]
GO
ALTER TABLE [dbo].[ComActionMessage]  WITH CHECK ADD  CONSTRAINT [ComActionMessage_ComDocument_FK] FOREIGN KEY([ComDocumentId])
REFERENCES [dbo].[ComDocument] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[ComActionMessage] CHECK CONSTRAINT [ComActionMessage_ComDocument_FK]
GO
ALTER TABLE [dbo].[ComActionMessage]  WITH CHECK ADD  CONSTRAINT [ComActionMessage_ComFolder_FK] FOREIGN KEY([ComFolderId])
REFERENCES [dbo].[ComFolder] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[ComActionMessage] CHECK CONSTRAINT [ComActionMessage_ComFolder_FK]
GO
ALTER TABLE [dbo].[ComActionMessage]  WITH CHECK ADD  CONSTRAINT [ComActionMessage_ComProspection_FK] FOREIGN KEY([ComProspectionId])
REFERENCES [dbo].[ComProspection] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[ComActionMessage] CHECK CONSTRAINT [ComActionMessage_ComProspection_FK]
GO
ALTER TABLE [dbo].[ComActionMessage]  WITH CHECK ADD  CONSTRAINT [ComActionMessage_ComSaleWithdrawalCancellation_FK] FOREIGN KEY([ComSaleWithdrawalCancellationId])
REFERENCES [dbo].[ComSaleWithdrawalCancellation] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[ComActionMessage] CHECK CONSTRAINT [ComActionMessage_ComSaleWithdrawalCancellation_FK]
GO
ALTER TABLE [dbo].[ComActionMessage]  WITH CHECK ADD  CONSTRAINT [ComActionMessage_ComSaleWithdrawalClient_FK] FOREIGN KEY([ComSaleWithdrawalClientId])
REFERENCES [dbo].[ComSaleWithdrawalClient] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[ComActionMessage] CHECK CONSTRAINT [ComActionMessage_ComSaleWithdrawalClient_FK]
GO
ALTER TABLE [dbo].[ComActionMessage]  WITH CHECK ADD  CONSTRAINT [ComActionMessage_ComSaleWithdrawalProduct_FK] FOREIGN KEY([ComSaleWithdrawalProductId])
REFERENCES [dbo].[ComSaleWithdrawalProduct] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[ComActionMessage] CHECK CONSTRAINT [ComActionMessage_ComSaleWithdrawalProduct_FK]
GO
ALTER TABLE [dbo].[ComActionMessage]  WITH CHECK ADD  CONSTRAINT [ComActionMessage_Parent_FK] FOREIGN KEY([ComActionMessageId])
REFERENCES [dbo].[ComActionMessage] ([PKey])
GO
ALTER TABLE [dbo].[ComActionMessage] CHECK CONSTRAINT [ComActionMessage_Parent_FK]
GO
ALTER TABLE [dbo].[ComActionMessage]  WITH CHECK ADD  CONSTRAINT [ComActionMessage_SavClaim_FK] FOREIGN KEY([SavClaimId])
REFERENCES [dbo].[SavClaim] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[ComActionMessage] CHECK CONSTRAINT [ComActionMessage_SavClaim_FK]
GO
ALTER TABLE [dbo].[ComActionMessage]  WITH CHECK ADD  CONSTRAINT [ComActionMessage_SavClaimGroup_FK] FOREIGN KEY([SavClaimGroupId])
REFERENCES [dbo].[SavClaimGroup] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[ComActionMessage] CHECK CONSTRAINT [ComActionMessage_SavClaimGroup_FK]
GO
ALTER TABLE [dbo].[ComActionMessage]  WITH CHECK ADD  CONSTRAINT [ComActionMessage_StkItem_FK] FOREIGN KEY([StkItemId])
REFERENCES [dbo].[StkItem] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[ComActionMessage] CHECK CONSTRAINT [ComActionMessage_StkItem_FK]
GO
ALTER TABLE [dbo].[ComActionMessageSubCategory]  WITH CHECK ADD  CONSTRAINT [ComActionMessageSubCategory__ComActionMessageSubCategoryStatus_FK1] FOREIGN KEY([ComActionMessageSubCategoryStatusId])
REFERENCES [dbo].[ComActionMessageSubCategoryStatus] ([PKey])
GO
ALTER TABLE [dbo].[ComActionMessageSubCategory] CHECK CONSTRAINT [ComActionMessageSubCategory__ComActionMessageSubCategoryStatus_FK1]
GO
ALTER TABLE [dbo].[ComActionMessageSubCategory]  WITH CHECK ADD  CONSTRAINT [ComActionMessageSubCategory_ComActionMessageCategory_FK1] FOREIGN KEY([ComActionMessageCategoryId])
REFERENCES [dbo].[ComActionMessageCategory] ([PKey])
GO
ALTER TABLE [dbo].[ComActionMessageSubCategory] CHECK CONSTRAINT [ComActionMessageSubCategory_ComActionMessageCategory_FK1]
GO
ALTER TABLE [dbo].[ComDocument]  WITH CHECK ADD  CONSTRAINT [ComDocument__CfgContactId_FK1] FOREIGN KEY([CfgContactId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[ComDocument] CHECK CONSTRAINT [ComDocument__CfgContactId_FK1]
GO
ALTER TABLE [dbo].[ComDocument]  WITH CHECK ADD  CONSTRAINT [ComDocument__CfgTier_FK1] FOREIGN KEY([CfgDebtCollectorId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[ComDocument] CHECK CONSTRAINT [ComDocument__CfgTier_FK1]
GO
ALTER TABLE [dbo].[ComDocument]  WITH CHECK ADD  CONSTRAINT [ComDocument__ComPenaltyDocument_FK1] FOREIGN KEY([ComPenaltyDocumentId])
REFERENCES [dbo].[ComDocument] ([PKey])
GO
ALTER TABLE [dbo].[ComDocument] CHECK CONSTRAINT [ComDocument__ComPenaltyDocument_FK1]
GO
ALTER TABLE [dbo].[ComDocument]  WITH CHECK ADD  CONSTRAINT [ComDocument__StlAccountId_FK1] FOREIGN KEY([StlAccountId])
REFERENCES [dbo].[StlAccount] ([PKey])
GO
ALTER TABLE [dbo].[ComDocument] CHECK CONSTRAINT [ComDocument__StlAccountId_FK1]
GO
ALTER TABLE [dbo].[ComDocument]  WITH CHECK ADD  CONSTRAINT [ComDocument__StlSettlementTypeId_FK1] FOREIGN KEY([StlSettlementTypeId])
REFERENCES [dbo].[StlSettlementType] ([PKey])
GO
ALTER TABLE [dbo].[ComDocument] CHECK CONSTRAINT [ComDocument__StlSettlementTypeId_FK1]
GO
ALTER TABLE [dbo].[ComDocument]  WITH CHECK ADD  CONSTRAINT [ComDocument_AdmModule_FK1] FOREIGN KEY([AdmModuleId])
REFERENCES [dbo].[AdmModule] ([PKey])
GO
ALTER TABLE [dbo].[ComDocument] CHECK CONSTRAINT [ComDocument_AdmModule_FK1]
GO
ALTER TABLE [dbo].[ComDocument]  WITH CHECK ADD  CONSTRAINT [ComDocument_CfgCompany_FK1] FOREIGN KEY([CfgCompanyId])
REFERENCES [dbo].[CfgCompany] ([PKey])
GO
ALTER TABLE [dbo].[ComDocument] CHECK CONSTRAINT [ComDocument_CfgCompany_FK1]
GO
ALTER TABLE [dbo].[ComDocument]  WITH CHECK ADD  CONSTRAINT [ComDocument_ComFolder_FK1] FOREIGN KEY([ComFolderId])
REFERENCES [dbo].[ComFolder] ([PKey])
GO
ALTER TABLE [dbo].[ComDocument] CHECK CONSTRAINT [ComDocument_ComFolder_FK1]
GO
ALTER TABLE [dbo].[ComDocument]  WITH CHECK ADD  CONSTRAINT [ComDocument_ComSaleWithdrawalCancellation_FK] FOREIGN KEY([ComSaleWithdrawalCancellationId])
REFERENCES [dbo].[ComSaleWithdrawalCancellation] ([PKey])
GO
ALTER TABLE [dbo].[ComDocument] CHECK CONSTRAINT [ComDocument_ComSaleWithdrawalCancellation_FK]
GO
ALTER TABLE [dbo].[ComDocument]  WITH CHECK ADD  CONSTRAINT [ComDocument_ComSaleWithdrawalClient_FK] FOREIGN KEY([ComSaleWithdrawalClientId])
REFERENCES [dbo].[ComSaleWithdrawalClient] ([PKey])
GO
ALTER TABLE [dbo].[ComDocument] CHECK CONSTRAINT [ComDocument_ComSaleWithdrawalClient_FK]
GO
ALTER TABLE [dbo].[ComDocument]  WITH CHECK ADD  CONSTRAINT [ComDocument_ComSaleWithdrawalProduct_FK] FOREIGN KEY([ComSaleWithdrawalProductId])
REFERENCES [dbo].[ComSaleWithdrawalProduct] ([PKey])
GO
ALTER TABLE [dbo].[ComDocument] CHECK CONSTRAINT [ComDocument_ComSaleWithdrawalProduct_FK]
GO
ALTER TABLE [dbo].[ComDocument]  WITH CHECK ADD  CONSTRAINT [ComDocument_StlRecoveryFolder_FK1] FOREIGN KEY([StlRecoveryFolderId])
REFERENCES [dbo].[StlRecoveryFolder] ([PKey])
GO
ALTER TABLE [dbo].[ComDocument] CHECK CONSTRAINT [ComDocument_StlRecoveryFolder_FK1]
GO
ALTER TABLE [dbo].[ComDocument]  WITH CHECK ADD  CONSTRAINT [ComSettlementCategory_ComDocument_FK1] FOREIGN KEY([ComSettlementCategoryId])
REFERENCES [dbo].[ComSettlementCategory] ([PKey])
GO
ALTER TABLE [dbo].[ComDocument] CHECK CONSTRAINT [ComSettlementCategory_ComDocument_FK1]
GO
ALTER TABLE [dbo].[ComDocumentPaymentMethod]  WITH CHECK ADD  CONSTRAINT [ComDocumentPaymentMethod_CfgCompany_FK1] FOREIGN KEY([CfgCompanyId])
REFERENCES [dbo].[CfgCompany] ([PKey])
GO
ALTER TABLE [dbo].[ComDocumentPaymentMethod] CHECK CONSTRAINT [ComDocumentPaymentMethod_CfgCompany_FK1]
GO
ALTER TABLE [dbo].[ComDocumentPaymentMethod]  WITH CHECK ADD  CONSTRAINT [ComDocumentPaymentMethod_ComFolder_FK1] FOREIGN KEY([ComFolderId])
REFERENCES [dbo].[ComFolder] ([PKey])
GO
ALTER TABLE [dbo].[ComDocumentPaymentMethod] CHECK CONSTRAINT [ComDocumentPaymentMethod_ComFolder_FK1]
GO
ALTER TABLE [dbo].[ComDocumentPaymentMethod]  WITH CHECK ADD  CONSTRAINT [ComDocumentPaymentMethod_ComSettlementCategory_FK1] FOREIGN KEY([ComSettlementCategoryId])
REFERENCES [dbo].[ComSettlementCategory] ([PKey])
GO
ALTER TABLE [dbo].[ComDocumentPaymentMethod] CHECK CONSTRAINT [ComDocumentPaymentMethod_ComSettlementCategory_FK1]
GO
ALTER TABLE [dbo].[ComFolder]  WITH CHECK ADD  CONSTRAINT [ComFolder__CfgOwner_FK1] FOREIGN KEY([CfgOwnerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[ComFolder] CHECK CONSTRAINT [ComFolder__CfgOwner_FK1]
GO
ALTER TABLE [dbo].[ComFolder]  WITH CHECK ADD  CONSTRAINT [ComFolder__CfgSaleManager_FK1] FOREIGN KEY([CfgSaleManagerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[ComFolder] CHECK CONSTRAINT [ComFolder__CfgSaleManager_FK1]
GO
ALTER TABLE [dbo].[ComFolder]  WITH CHECK ADD  CONSTRAINT [ComFolder__CfgSaleManager2_FK1] FOREIGN KEY([CfgSaleManager2Id])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[ComFolder] CHECK CONSTRAINT [ComFolder__CfgSaleManager2_FK1]
GO
ALTER TABLE [dbo].[ComFolder]  WITH CHECK ADD  CONSTRAINT [ComFolder__ComLoanCompanyDecision_FK1] FOREIGN KEY([ComLoanCompanyDecisionId])
REFERENCES [dbo].[ComLoanCompanyDecision] ([PKey])
GO
ALTER TABLE [dbo].[ComFolder] CHECK CONSTRAINT [ComFolder__ComLoanCompanyDecision_FK1]
GO
ALTER TABLE [dbo].[ComFolder]  WITH CHECK ADD  CONSTRAINT [ComFolder__StrEntity_FK1] FOREIGN KEY([StrEntityId])
REFERENCES [dbo].[StrEntity] ([PKey])
GO
ALTER TABLE [dbo].[ComFolder] CHECK CONSTRAINT [ComFolder__StrEntity_FK1]
GO
ALTER TABLE [dbo].[ComFolder]  WITH CHECK ADD  CONSTRAINT [ComFolder__StrStatus_FK1] FOREIGN KEY([StrStatusId])
REFERENCES [dbo].[StrStatus] ([PKey])
GO
ALTER TABLE [dbo].[ComFolder] CHECK CONSTRAINT [ComFolder__StrStatus_FK1]
GO
ALTER TABLE [dbo].[ComFolder]  WITH CHECK ADD  CONSTRAINT [ComFolder_CfgCommercial_FK1] FOREIGN KEY([CfgCommercialId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[ComFolder] CHECK CONSTRAINT [ComFolder_CfgCommercial_FK1]
GO
ALTER TABLE [dbo].[ComFolder]  WITH CHECK ADD  CONSTRAINT [ComFolder_CfgCustomer_FK1] FOREIGN KEY([CfgCustomerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[ComFolder] CHECK CONSTRAINT [ComFolder_CfgCustomer_FK1]
GO
ALTER TABLE [dbo].[ComFolder]  WITH CHECK ADD  CONSTRAINT [ComFolder_CfgLawyer_FK] FOREIGN KEY([CfgLawyerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[ComFolder] CHECK CONSTRAINT [ComFolder_CfgLawyer_FK]
GO
ALTER TABLE [dbo].[ComFolder]  WITH CHECK ADD  CONSTRAINT [ComFolder_CfgTier_CreditBank] FOREIGN KEY([CreditBankId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[ComFolder] CHECK CONSTRAINT [ComFolder_CfgTier_CreditBank]
GO
ALTER TABLE [dbo].[ComFolder]  WITH CHECK ADD  CONSTRAINT [ComFolder_ComPaymentMethod_FK1] FOREIGN KEY([ComPaymentMethodId])
REFERENCES [dbo].[ComPaymentMethod] ([PKey])
GO
ALTER TABLE [dbo].[ComFolder] CHECK CONSTRAINT [ComFolder_ComPaymentMethod_FK1]
GO
ALTER TABLE [dbo].[ComFolder]  WITH CHECK ADD  CONSTRAINT [ComFolder_ComProspection_FK] FOREIGN KEY([ComProspectionId])
REFERENCES [dbo].[ComProspection] ([PKey])
GO
ALTER TABLE [dbo].[ComFolder] CHECK CONSTRAINT [ComFolder_ComProspection_FK]
GO
ALTER TABLE [dbo].[ComFolder]  WITH CHECK ADD  CONSTRAINT [ComFolder_StkItem_FK1] FOREIGN KEY([StkItemId])
REFERENCES [dbo].[StkItem] ([PKey])
GO
ALTER TABLE [dbo].[ComFolder] CHECK CONSTRAINT [ComFolder_StkItem_FK1]
GO
ALTER TABLE [dbo].[ComFolder]  WITH CHECK ADD  CONSTRAINT [ComFolder_StkVocation_FK1] FOREIGN KEY([StkVocationId])
REFERENCES [dbo].[StkVocation] ([PKey])
GO
ALTER TABLE [dbo].[ComFolder] CHECK CONSTRAINT [ComFolder_StkVocation_FK1]
GO
ALTER TABLE [dbo].[ComFolder]  WITH CHECK ADD  CONSTRAINT [ComFolder_StlQuote_FK2] FOREIGN KEY([StlQuoteId])
REFERENCES [dbo].[StlQuote] ([PKey])
GO
ALTER TABLE [dbo].[ComFolder] CHECK CONSTRAINT [ComFolder_StlQuote_FK2]
GO
ALTER TABLE [dbo].[ComFolderCustomer]  WITH CHECK ADD  CONSTRAINT [ComFolderCustomer_CfgTier_FK1] FOREIGN KEY([CfgTierId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[ComFolderCustomer] CHECK CONSTRAINT [ComFolderCustomer_CfgTier_FK1]
GO
ALTER TABLE [dbo].[ComFolderCustomer]  WITH CHECK ADD  CONSTRAINT [ComFolderCustomer_ComFolder_FK1] FOREIGN KEY([ComFolderId])
REFERENCES [dbo].[ComFolder] ([PKey])
GO
ALTER TABLE [dbo].[ComFolderCustomer] CHECK CONSTRAINT [ComFolderCustomer_ComFolder_FK1]
GO
ALTER TABLE [dbo].[ComFolderItem]  WITH CHECK ADD  CONSTRAINT [ComFolderItem_ComFolder_FK1] FOREIGN KEY([ComFolderId])
REFERENCES [dbo].[ComFolder] ([PKey])
GO
ALTER TABLE [dbo].[ComFolderItem] CHECK CONSTRAINT [ComFolderItem_ComFolder_FK1]
GO
ALTER TABLE [dbo].[ComFolderItem]  WITH CHECK ADD  CONSTRAINT [ComFolderItem_StkBuilding_FK1] FOREIGN KEY([StkBuildingId])
REFERENCES [dbo].[StkBuilding] ([PKey])
GO
ALTER TABLE [dbo].[ComFolderItem] CHECK CONSTRAINT [ComFolderItem_StkBuilding_FK1]
GO
ALTER TABLE [dbo].[ComFolderItem]  WITH CHECK ADD  CONSTRAINT [ComFolderItem_StkCostCategory_FK1] FOREIGN KEY([StkCostCategoryId])
REFERENCES [dbo].[StkCostCategory] ([PKey])
GO
ALTER TABLE [dbo].[ComFolderItem] CHECK CONSTRAINT [ComFolderItem_StkCostCategory_FK1]
GO
ALTER TABLE [dbo].[ComFolderItem]  WITH CHECK ADD  CONSTRAINT [ComFolderItem_StkFloor_FK1] FOREIGN KEY([StkFloorId])
REFERENCES [dbo].[StkFloor] ([PKey])
GO
ALTER TABLE [dbo].[ComFolderItem] CHECK CONSTRAINT [ComFolderItem_StkFloor_FK1]
GO
ALTER TABLE [dbo].[ComFolderItem]  WITH CHECK ADD  CONSTRAINT [ComFolderItem_StkItem_FK1] FOREIGN KEY([StkItemId])
REFERENCES [dbo].[StkItem] ([PKey])
GO
ALTER TABLE [dbo].[ComFolderItem] CHECK CONSTRAINT [ComFolderItem_StkItem_FK1]
GO
ALTER TABLE [dbo].[ComFolderItem]  WITH CHECK ADD  CONSTRAINT [ComFolderItem_StkItemCategory_FK1] FOREIGN KEY([StkItemCategoryId])
REFERENCES [dbo].[StkItemCategory] ([PKey])
GO
ALTER TABLE [dbo].[ComFolderItem] CHECK CONSTRAINT [ComFolderItem_StkItemCategory_FK1]
GO
ALTER TABLE [dbo].[ComFolderItem]  WITH CHECK ADD  CONSTRAINT [ComFolderItem_StkItemType_FK1] FOREIGN KEY([StkItemTypeId])
REFERENCES [dbo].[StkItemType] ([PKey])
GO
ALTER TABLE [dbo].[ComFolderItem] CHECK CONSTRAINT [ComFolderItem_StkItemType_FK1]
GO
ALTER TABLE [dbo].[ComFolderItem]  WITH CHECK ADD  CONSTRAINT [ComFolderItem_StkOrientation_FK1] FOREIGN KEY([StkOrientationId])
REFERENCES [dbo].[StkOrientation] ([PKey])
GO
ALTER TABLE [dbo].[ComFolderItem] CHECK CONSTRAINT [ComFolderItem_StkOrientation_FK1]
GO
ALTER TABLE [dbo].[ComProspection]  WITH CHECK ADD  CONSTRAINT [CfgTier_ComProspection_FK1] FOREIGN KEY([CfgProspectId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[ComProspection] CHECK CONSTRAINT [CfgTier_ComProspection_FK1]
GO
ALTER TABLE [dbo].[ComProspection]  WITH CHECK ADD  CONSTRAINT [ComProspection__AdmCountry_FK3] FOREIGN KEY([AdmCountryId])
REFERENCES [dbo].[AdmCountry] ([PKey])
GO
ALTER TABLE [dbo].[ComProspection] CHECK CONSTRAINT [ComProspection__AdmCountry_FK3]
GO
ALTER TABLE [dbo].[ComProspection]  WITH CHECK ADD  CONSTRAINT [ComProspection__CfgIntermediate_FK3] FOREIGN KEY([CfgIntermediateId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[ComProspection] CHECK CONSTRAINT [ComProspection__CfgIntermediate_FK3]
GO
ALTER TABLE [dbo].[ComProspection]  WITH CHECK ADD  CONSTRAINT [ComProspection__CfgProposer_FK1] FOREIGN KEY([CfgProposerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[ComProspection] CHECK CONSTRAINT [ComProspection__CfgProposer_FK1]
GO
ALTER TABLE [dbo].[ComProspection]  WITH CHECK ADD  CONSTRAINT [ComProspection__ComBudget_FK1] FOREIGN KEY([ComBudgetId])
REFERENCES [dbo].[ComBudget] ([PKey])
GO
ALTER TABLE [dbo].[ComProspection] CHECK CONSTRAINT [ComProspection__ComBudget_FK1]
GO
ALTER TABLE [dbo].[ComProspection]  WITH CHECK ADD  CONSTRAINT [ComProspection__ComUtilization_FK1] FOREIGN KEY([ComUtilizationId])
REFERENCES [dbo].[ComUtilization] ([PKey])
GO
ALTER TABLE [dbo].[ComProspection] CHECK CONSTRAINT [ComProspection__ComUtilization_FK1]
GO
ALTER TABLE [dbo].[ComProspection]  WITH CHECK ADD  CONSTRAINT [ComProspection_CfgCommercial_FK1] FOREIGN KEY([CfgCommercialId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[ComProspection] CHECK CONSTRAINT [ComProspection_CfgCommercial_FK1]
GO
ALTER TABLE [dbo].[ComProspection]  WITH CHECK ADD  CONSTRAINT [ComProspection_CfgTranche_FK1] FOREIGN KEY([StkHierarchyId])
REFERENCES [dbo].[CfgTranche] ([PKey])
GO
ALTER TABLE [dbo].[ComProspection] CHECK CONSTRAINT [ComProspection_CfgTranche_FK1]
GO
ALTER TABLE [dbo].[ComProspection]  WITH CHECK ADD  CONSTRAINT [ComProspection_ComAction_FK1] FOREIGN KEY([ComActionId])
REFERENCES [dbo].[ComAction] ([PKey])
GO
ALTER TABLE [dbo].[ComProspection] CHECK CONSTRAINT [ComProspection_ComAction_FK1]
GO
ALTER TABLE [dbo].[ComProspection]  WITH CHECK ADD  CONSTRAINT [ComProspection_ComActionMessageType_FK1] FOREIGN KEY([ComActionMessageTypeId])
REFERENCES [dbo].[ComActionMessageType] ([PKey])
GO
ALTER TABLE [dbo].[ComProspection] CHECK CONSTRAINT [ComProspection_ComActionMessageType_FK1]
GO
ALTER TABLE [dbo].[ComProspection]  WITH CHECK ADD  CONSTRAINT [ComProspection_ComPaymentMethod_FK1] FOREIGN KEY([ComPaymentMethodId])
REFERENCES [dbo].[ComPaymentMethod] ([PKey])
GO
ALTER TABLE [dbo].[ComProspection] CHECK CONSTRAINT [ComProspection_ComPaymentMethod_FK1]
GO
ALTER TABLE [dbo].[ComProspection]  WITH CHECK ADD  CONSTRAINT [ComProspection_ComProspectionClosingReason_FK1] FOREIGN KEY([ComProspectionClosingReasonId])
REFERENCES [dbo].[ComProspectionClosingReason] ([PKey])
GO
ALTER TABLE [dbo].[ComProspection] CHECK CONSTRAINT [ComProspection_ComProspectionClosingReason_FK1]
GO
ALTER TABLE [dbo].[ComProspection]  WITH CHECK ADD  CONSTRAINT [ComProspection_ComProspectionKind_FK1] FOREIGN KEY([ComProspectionKindId])
REFERENCES [dbo].[ComProspectionKind] ([PKey])
GO
ALTER TABLE [dbo].[ComProspection] CHECK CONSTRAINT [ComProspection_ComProspectionKind_FK1]
GO
ALTER TABLE [dbo].[ComProspection]  WITH CHECK ADD  CONSTRAINT [ComProspection_ComProspectionOriginSupport_FK] FOREIGN KEY([ComProspectionOriginSupportId])
REFERENCES [dbo].[ComProspectionOriginSupport] ([PKey])
GO
ALTER TABLE [dbo].[ComProspection] CHECK CONSTRAINT [ComProspection_ComProspectionOriginSupport_FK]
GO
ALTER TABLE [dbo].[ComProspection]  WITH CHECK ADD  CONSTRAINT [ComProspection_StkItem_FK1] FOREIGN KEY([StkItemId])
REFERENCES [dbo].[StkItem] ([PKey])
GO
ALTER TABLE [dbo].[ComProspection] CHECK CONSTRAINT [ComProspection_StkItem_FK1]
GO
ALTER TABLE [dbo].[ComProspection]  WITH CHECK ADD  CONSTRAINT [ComProspection_StkVocation_FK1] FOREIGN KEY([StkVocationId])
REFERENCES [dbo].[StkVocation] ([PKey])
GO
ALTER TABLE [dbo].[ComProspection] CHECK CONSTRAINT [ComProspection_StkVocation_FK1]
GO
ALTER TABLE [dbo].[ComProspection]  WITH CHECK ADD  CONSTRAINT [ComProspection_StrEntity_FK] FOREIGN KEY([StrEntityId])
REFERENCES [dbo].[StrEntity] ([PKey])
GO
ALTER TABLE [dbo].[ComProspection] CHECK CONSTRAINT [ComProspection_StrEntity_FK]
GO
ALTER TABLE [dbo].[ComProspection]  WITH CHECK ADD  CONSTRAINT [ComProspectionOrigin_ComProspection_FK1] FOREIGN KEY([ComProspectionOriginId])
REFERENCES [dbo].[ComProspectionOrigin] ([PKey])
GO
ALTER TABLE [dbo].[ComProspection] CHECK CONSTRAINT [ComProspectionOrigin_ComProspection_FK1]
GO
ALTER TABLE [dbo].[ComProspectionCategory]  WITH CHECK ADD  CONSTRAINT [ComProspection_ComProspectionCategory_FK1] FOREIGN KEY([ComProspectionId])
REFERENCES [dbo].[ComProspection] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[ComProspectionCategory] CHECK CONSTRAINT [ComProspection_ComProspectionCategory_FK1]
GO
ALTER TABLE [dbo].[ComProspectionCategory]  WITH CHECK ADD  CONSTRAINT [ComProspectionCategory__PrmRequestOffer_FK3] FOREIGN KEY([PrmRequestOfferId])
REFERENCES [dbo].[PrmRequestOffer] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[ComProspectionCategory] CHECK CONSTRAINT [ComProspectionCategory__PrmRequestOffer_FK3]
GO
ALTER TABLE [dbo].[ComProspectionCategory]  WITH CHECK ADD  CONSTRAINT [ComProspectionCategory_StkItemCategory_FK1] FOREIGN KEY([StkItemCategoryId])
REFERENCES [dbo].[StkItemCategory] ([PKey])
GO
ALTER TABLE [dbo].[ComProspectionCategory] CHECK CONSTRAINT [ComProspectionCategory_StkItemCategory_FK1]
GO
ALTER TABLE [dbo].[ComProspectionCity]  WITH CHECK ADD  CONSTRAINT [ComProspectionCity_AdmCity_FK] FOREIGN KEY([AdmCityId])
REFERENCES [dbo].[AdmCity] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[ComProspectionCity] CHECK CONSTRAINT [ComProspectionCity_AdmCity_FK]
GO
ALTER TABLE [dbo].[ComProspectionCity]  WITH CHECK ADD  CONSTRAINT [ComProspectionCity_ComProspection_FK] FOREIGN KEY([ComProspectionId])
REFERENCES [dbo].[ComProspection] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[ComProspectionCity] CHECK CONSTRAINT [ComProspectionCity_ComProspection_FK]
GO
ALTER TABLE [dbo].[ComProspectionKind]  WITH CHECK ADD  CONSTRAINT [ComProspectionKind__ComActionMessageSubCategory_FK1] FOREIGN KEY([ComActionMessageSubCategoryId])
REFERENCES [dbo].[ComActionMessageSubCategory] ([PKey])
GO
ALTER TABLE [dbo].[ComProspectionKind] CHECK CONSTRAINT [ComProspectionKind__ComActionMessageSubCategory_FK1]
GO
ALTER TABLE [dbo].[ComProspectionOriginSupport]  WITH CHECK ADD  CONSTRAINT [ComProspectionOriginSupport_ComProspectionOrigin_FK1] FOREIGN KEY([ComProspectionOriginId])
REFERENCES [dbo].[ComProspectionOrigin] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[ComProspectionOriginSupport] CHECK CONSTRAINT [ComProspectionOriginSupport_ComProspectionOrigin_FK1]
GO
ALTER TABLE [dbo].[ComProspectionProfile]  WITH CHECK ADD  CONSTRAINT [ComProspectionProfile__ComProspectionProfileCategory_FK1] FOREIGN KEY([ComProspectionProfileCategoryId])
REFERENCES [dbo].[ComProspectionProfileCategory] ([PKey])
GO
ALTER TABLE [dbo].[ComProspectionProfile] CHECK CONSTRAINT [ComProspectionProfile__ComProspectionProfileCategory_FK1]
GO
ALTER TABLE [dbo].[ComProspectionProject]  WITH CHECK ADD  CONSTRAINT [ComProspection_ComProspectionProject_FK1] FOREIGN KEY([ComProspectionId])
REFERENCES [dbo].[ComProspection] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[ComProspectionProject] CHECK CONSTRAINT [ComProspection_ComProspectionProject_FK1]
GO
ALTER TABLE [dbo].[ComProspectionProject]  WITH CHECK ADD  CONSTRAINT [ComProspectionProject_CfgTranche_FK1] FOREIGN KEY([StkHierarchyId])
REFERENCES [dbo].[CfgTranche] ([PKey])
GO
ALTER TABLE [dbo].[ComProspectionProject] CHECK CONSTRAINT [ComProspectionProject_CfgTranche_FK1]
GO
ALTER TABLE [dbo].[ComProspectionProject]  WITH CHECK ADD  CONSTRAINT [ComProspectionProject_StkItem_FK] FOREIGN KEY([StkItemId])
REFERENCES [dbo].[StkItem] ([PKey])
GO
ALTER TABLE [dbo].[ComProspectionProject] CHECK CONSTRAINT [ComProspectionProject_StkItem_FK]
GO
ALTER TABLE [dbo].[ComProspectionSelectedProfile]  WITH CHECK ADD  CONSTRAINT [ComProspection__ComProspectionSelectedProfile_FK1] FOREIGN KEY([ComProspectionId])
REFERENCES [dbo].[ComProspection] ([PKey])
GO
ALTER TABLE [dbo].[ComProspectionSelectedProfile] CHECK CONSTRAINT [ComProspection__ComProspectionSelectedProfile_FK1]
GO
ALTER TABLE [dbo].[ComProspectionSelectedProfile]  WITH CHECK ADD  CONSTRAINT [ComProspectionProfile__ComProspectionSelectedProfile_FK1] FOREIGN KEY([ComProspectionProfileId])
REFERENCES [dbo].[ComProspectionProfile] ([PKey])
GO
ALTER TABLE [dbo].[ComProspectionSelectedProfile] CHECK CONSTRAINT [ComProspectionProfile__ComProspectionSelectedProfile_FK1]
GO
ALTER TABLE [dbo].[ComProspectionSelectedProfile]  WITH CHECK ADD  CONSTRAINT [ComProspectionSelectedProfile__PrmRequestOffer_FK3] FOREIGN KEY([PrmRequestOfferId])
REFERENCES [dbo].[PrmRequestOffer] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[ComProspectionSelectedProfile] CHECK CONSTRAINT [ComProspectionSelectedProfile__PrmRequestOffer_FK3]
GO
ALTER TABLE [dbo].[ComProspectionType]  WITH CHECK ADD  CONSTRAINT [ComProspection_ComProspectionType_FK1] FOREIGN KEY([ComProspectionId])
REFERENCES [dbo].[ComProspection] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[ComProspectionType] CHECK CONSTRAINT [ComProspection_ComProspectionType_FK1]
GO
ALTER TABLE [dbo].[ComProspectionType]  WITH CHECK ADD  CONSTRAINT [ComProspectionType__PrmRequestOffer_FK3] FOREIGN KEY([PrmRequestOfferId])
REFERENCES [dbo].[PrmRequestOffer] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[ComProspectionType] CHECK CONSTRAINT [ComProspectionType__PrmRequestOffer_FK3]
GO
ALTER TABLE [dbo].[ComProspectionType]  WITH CHECK ADD  CONSTRAINT [ComProspectionType_StkItemType_FK1] FOREIGN KEY([StkItemTypeId])
REFERENCES [dbo].[StkItemType] ([PKey])
GO
ALTER TABLE [dbo].[ComProspectionType] CHECK CONSTRAINT [ComProspectionType_StkItemType_FK1]
GO
ALTER TABLE [dbo].[ComSaleWithdrawalCancellation]  WITH CHECK ADD  CONSTRAINT [ComSaleWithdrawalCancellation__CfgContactId_FK1] FOREIGN KEY([CfgContactId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[ComSaleWithdrawalCancellation] CHECK CONSTRAINT [ComSaleWithdrawalCancellation__CfgContactId_FK1]
GO
ALTER TABLE [dbo].[ComSaleWithdrawalCancellation]  WITH CHECK ADD  CONSTRAINT [ComSaleWithdrawalCancellation_CfgBank_FK1] FOREIGN KEY([CfgBankId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[ComSaleWithdrawalCancellation] CHECK CONSTRAINT [ComSaleWithdrawalCancellation_CfgBank_FK1]
GO
ALTER TABLE [dbo].[ComSaleWithdrawalCancellation]  WITH CHECK ADD  CONSTRAINT [ComSaleWithdrawalCancellation_CfgCustomer_FK] FOREIGN KEY([CfgOldCustomerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[ComSaleWithdrawalCancellation] CHECK CONSTRAINT [ComSaleWithdrawalCancellation_CfgCustomer_FK]
GO
ALTER TABLE [dbo].[ComSaleWithdrawalCancellation]  WITH CHECK ADD  CONSTRAINT [ComSaleWithdrawalCancellation_ComFolder_FK] FOREIGN KEY([ComSaleId])
REFERENCES [dbo].[ComFolder] ([PKey])
GO
ALTER TABLE [dbo].[ComSaleWithdrawalCancellation] CHECK CONSTRAINT [ComSaleWithdrawalCancellation_ComFolder_FK]
GO
ALTER TABLE [dbo].[ComSaleWithdrawalCancellation]  WITH CHECK ADD  CONSTRAINT [ComSaleWithdrawalCancellation_ComProspectionOrigin_FK] FOREIGN KEY([ComProspectionOriginId])
REFERENCES [dbo].[ComProspectionOrigin] ([PKey])
GO
ALTER TABLE [dbo].[ComSaleWithdrawalCancellation] CHECK CONSTRAINT [ComSaleWithdrawalCancellation_ComProspectionOrigin_FK]
GO
ALTER TABLE [dbo].[ComSaleWithdrawalCancellation]  WITH CHECK ADD  CONSTRAINT [ComSaleWithdrawalCancellation_ComSaleWithdrawalReason_FK] FOREIGN KEY([ComSaleWithdrawalReasonId])
REFERENCES [dbo].[ComSaleWithdrawalReason] ([PKey])
GO
ALTER TABLE [dbo].[ComSaleWithdrawalCancellation] CHECK CONSTRAINT [ComSaleWithdrawalCancellation_ComSaleWithdrawalReason_FK]
GO
ALTER TABLE [dbo].[ComSaleWithdrawalCancellation]  WITH CHECK ADD  CONSTRAINT [ComSaleWithdrawalCancellation_StkItem_FK] FOREIGN KEY([StkItemId])
REFERENCES [dbo].[StkItem] ([PKey])
GO
ALTER TABLE [dbo].[ComSaleWithdrawalCancellation] CHECK CONSTRAINT [ComSaleWithdrawalCancellation_StkItem_FK]
GO
ALTER TABLE [dbo].[ComSaleWithdrawalCancellation]  WITH CHECK ADD  CONSTRAINT [ComSaleWithdrawalCancellation_StkOldItem_FK] FOREIGN KEY([StkOldItemId])
REFERENCES [dbo].[StkItem] ([PKey])
GO
ALTER TABLE [dbo].[ComSaleWithdrawalCancellation] CHECK CONSTRAINT [ComSaleWithdrawalCancellation_StkOldItem_FK]
GO
ALTER TABLE [dbo].[ComSaleWithdrawalCancellation]  WITH CHECK ADD  CONSTRAINT [ComSaleWithdrawalCancellation_StlSettlementType_FK1] FOREIGN KEY([StlSettlementTypeId])
REFERENCES [dbo].[StlSettlementType] ([PKey])
GO
ALTER TABLE [dbo].[ComSaleWithdrawalCancellation] CHECK CONSTRAINT [ComSaleWithdrawalCancellation_StlSettlementType_FK1]
GO
ALTER TABLE [dbo].[ComSaleWithdrawalClient]  WITH CHECK ADD  CONSTRAINT [ComSaleWithdrawalClient_CfgNewCustomer_FK] FOREIGN KEY([CfgCustomerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[ComSaleWithdrawalClient] CHECK CONSTRAINT [ComSaleWithdrawalClient_CfgNewCustomer_FK]
GO
ALTER TABLE [dbo].[ComSaleWithdrawalClient]  WITH CHECK ADD  CONSTRAINT [ComSaleWithdrawalClient_CfgOldCustomer_FK] FOREIGN KEY([CfgOldCustomerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[ComSaleWithdrawalClient] CHECK CONSTRAINT [ComSaleWithdrawalClient_CfgOldCustomer_FK]
GO
ALTER TABLE [dbo].[ComSaleWithdrawalClient]  WITH CHECK ADD  CONSTRAINT [ComSaleWithdrawalClient_ComFolder_FK] FOREIGN KEY([ComSaleId])
REFERENCES [dbo].[ComFolder] ([PKey])
GO
ALTER TABLE [dbo].[ComSaleWithdrawalClient] CHECK CONSTRAINT [ComSaleWithdrawalClient_ComFolder_FK]
GO
ALTER TABLE [dbo].[ComSaleWithdrawalClient]  WITH CHECK ADD  CONSTRAINT [ComSaleWithdrawalClient_ComProspectionOrigin_FK] FOREIGN KEY([ComProspectionOriginId])
REFERENCES [dbo].[ComProspectionOrigin] ([PKey])
GO
ALTER TABLE [dbo].[ComSaleWithdrawalClient] CHECK CONSTRAINT [ComSaleWithdrawalClient_ComProspectionOrigin_FK]
GO
ALTER TABLE [dbo].[ComSaleWithdrawalClient]  WITH CHECK ADD  CONSTRAINT [ComSaleWithdrawalClient_ComSaleWithdrawalReason_FK] FOREIGN KEY([ComSaleWithdrawalReasonId])
REFERENCES [dbo].[ComSaleWithdrawalReason] ([PKey])
GO
ALTER TABLE [dbo].[ComSaleWithdrawalClient] CHECK CONSTRAINT [ComSaleWithdrawalClient_ComSaleWithdrawalReason_FK]
GO
ALTER TABLE [dbo].[ComSaleWithdrawalClient]  WITH CHECK ADD  CONSTRAINT [ComSaleWithdrawalClient_StkItem_FK] FOREIGN KEY([StkOldItemId])
REFERENCES [dbo].[StkItem] ([PKey])
GO
ALTER TABLE [dbo].[ComSaleWithdrawalClient] CHECK CONSTRAINT [ComSaleWithdrawalClient_StkItem_FK]
GO
ALTER TABLE [dbo].[ComSaleWithdrawalProduct]  WITH CHECK ADD  CONSTRAINT [ComSaleWithdrawalProduct_CfgCustomer_FK] FOREIGN KEY([CfgCustomerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[ComSaleWithdrawalProduct] CHECK CONSTRAINT [ComSaleWithdrawalProduct_CfgCustomer_FK]
GO
ALTER TABLE [dbo].[ComSaleWithdrawalProduct]  WITH CHECK ADD  CONSTRAINT [ComSaleWithdrawalProduct_ComFolder_FK] FOREIGN KEY([ComSaleId])
REFERENCES [dbo].[ComFolder] ([PKey])
GO
ALTER TABLE [dbo].[ComSaleWithdrawalProduct] CHECK CONSTRAINT [ComSaleWithdrawalProduct_ComFolder_FK]
GO
ALTER TABLE [dbo].[ComSaleWithdrawalProduct]  WITH CHECK ADD  CONSTRAINT [ComSaleWithdrawalProduct_ComProspectionOrigin_FK] FOREIGN KEY([ComProspectionOriginId])
REFERENCES [dbo].[ComProspectionOrigin] ([PKey])
GO
ALTER TABLE [dbo].[ComSaleWithdrawalProduct] CHECK CONSTRAINT [ComSaleWithdrawalProduct_ComProspectionOrigin_FK]
GO
ALTER TABLE [dbo].[ComSaleWithdrawalProduct]  WITH CHECK ADD  CONSTRAINT [ComSaleWithdrawalProduct_ComSaleWithdrawalReason_FK] FOREIGN KEY([ComSaleWithdrawalReasonId])
REFERENCES [dbo].[ComSaleWithdrawalReason] ([PKey])
GO
ALTER TABLE [dbo].[ComSaleWithdrawalProduct] CHECK CONSTRAINT [ComSaleWithdrawalProduct_ComSaleWithdrawalReason_FK]
GO
ALTER TABLE [dbo].[ComSaleWithdrawalProduct]  WITH CHECK ADD  CONSTRAINT [ComSaleWithdrawalProduct_StkItem_FK] FOREIGN KEY([StkItemId])
REFERENCES [dbo].[StkItem] ([PKey])
GO
ALTER TABLE [dbo].[ComSaleWithdrawalProduct] CHECK CONSTRAINT [ComSaleWithdrawalProduct_StkItem_FK]
GO
ALTER TABLE [dbo].[ComSaleWithdrawalProduct]  WITH CHECK ADD  CONSTRAINT [ComSaleWithdrawalProduct_StkOldItem_FK] FOREIGN KEY([StkOldItemId])
REFERENCES [dbo].[StkItem] ([PKey])
GO
ALTER TABLE [dbo].[ComSaleWithdrawalProduct] CHECK CONSTRAINT [ComSaleWithdrawalProduct_StkOldItem_FK]
GO
ALTER TABLE [dbo].[ComSettlementCategory]  WITH CHECK ADD  CONSTRAINT [ComSettlementCategory_StkVocation_FK1] FOREIGN KEY([StkVocationId])
REFERENCES [dbo].[StkVocation] ([PKey])
GO
ALTER TABLE [dbo].[ComSettlementCategory] CHECK CONSTRAINT [ComSettlementCategory_StkVocation_FK1]
GO
ALTER TABLE [dbo].[ComSettlementCategory]  WITH CHECK ADD  CONSTRAINT [StlCategory_ComSettlementCategory_FK1] FOREIGN KEY([StlCategoryId])
REFERENCES [dbo].[StlCategory] ([PKey])
GO
ALTER TABLE [dbo].[ComSettlementCategory] CHECK CONSTRAINT [StlCategory_ComSettlementCategory_FK1]
GO
ALTER TABLE [dbo].[GrhDepartment]  WITH CHECK ADD  CONSTRAINT [GrhDepartment__CfgCompany_FK1] FOREIGN KEY([CfgCompanyId])
REFERENCES [dbo].[CfgCompany] ([PKey])
GO
ALTER TABLE [dbo].[GrhDepartment] CHECK CONSTRAINT [GrhDepartment__CfgCompany_FK1]
GO
ALTER TABLE [dbo].[GrhDepartment]  WITH CHECK ADD  CONSTRAINT [GrhDepartment__GrhManager_FK1] FOREIGN KEY([GrhManagerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[GrhDepartment] CHECK CONSTRAINT [GrhDepartment__GrhManager_FK1]
GO
ALTER TABLE [dbo].[GrhEchelon]  WITH CHECK ADD  CONSTRAINT [GrhEchelon__CfgCompany_FK1] FOREIGN KEY([CfgCompanyId])
REFERENCES [dbo].[CfgCompany] ([PKey])
GO
ALTER TABLE [dbo].[GrhEchelon] CHECK CONSTRAINT [GrhEchelon__CfgCompany_FK1]
GO
ALTER TABLE [dbo].[GrhEmployeeCategory]  WITH CHECK ADD  CONSTRAINT [GrhEmployeeCategory__CfgCompany_FK1] FOREIGN KEY([CfgCompanyId])
REFERENCES [dbo].[CfgCompany] ([PKey])
GO
ALTER TABLE [dbo].[GrhEmployeeCategory] CHECK CONSTRAINT [GrhEmployeeCategory__CfgCompany_FK1]
GO
ALTER TABLE [dbo].[GrhEmployeeClass]  WITH CHECK ADD  CONSTRAINT [GrhEmployeeClass_CfgCompany_FK] FOREIGN KEY([CfgCompanyId])
REFERENCES [dbo].[CfgCompany] ([PKey])
GO
ALTER TABLE [dbo].[GrhEmployeeClass] CHECK CONSTRAINT [GrhEmployeeClass_CfgCompany_FK]
GO
ALTER TABLE [dbo].[GrhEmployeeType]  WITH CHECK ADD  CONSTRAINT [GrhEmployeeType_CfgCompany_FK] FOREIGN KEY([CfgCompanyId])
REFERENCES [dbo].[CfgCompany] ([PKey])
GO
ALTER TABLE [dbo].[GrhEmployeeType] CHECK CONSTRAINT [GrhEmployeeType_CfgCompany_FK]
GO
ALTER TABLE [dbo].[GrhPaySlipModel]  WITH CHECK ADD  CONSTRAINT [GrhPaySlipModel__GrhPaySlipModelKind_FK1] FOREIGN KEY([GrhPaySlipModelKindId])
REFERENCES [dbo].[GrhPaySlipModelKind] ([PKey])
GO
ALTER TABLE [dbo].[GrhPaySlipModel] CHECK CONSTRAINT [GrhPaySlipModel__GrhPaySlipModelKind_FK1]
GO
ALTER TABLE [dbo].[LndLand]  WITH CHECK ADD  CONSTRAINT [CfgTier_LndLand_FK1] FOREIGN KEY([CfgSupplierId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[LndLand] CHECK CONSTRAINT [CfgTier_LndLand_FK1]
GO
ALTER TABLE [dbo].[LndLand]  WITH CHECK ADD  CONSTRAINT [LndLand__AdmCity_FK1] FOREIGN KEY([AdmCityId])
REFERENCES [dbo].[AdmCity] ([PKey])
GO
ALTER TABLE [dbo].[LndLand] CHECK CONSTRAINT [LndLand__AdmCity_FK1]
GO
ALTER TABLE [dbo].[LndLand]  WITH CHECK ADD  CONSTRAINT [LndLand__AdmCountry_FK1] FOREIGN KEY([AdmCountryId])
REFERENCES [dbo].[AdmCountry] ([PKey])
GO
ALTER TABLE [dbo].[LndLand] CHECK CONSTRAINT [LndLand__AdmCountry_FK1]
GO
ALTER TABLE [dbo].[LndLand]  WITH CHECK ADD  CONSTRAINT [LndLand__StkItemCategory_FK1] FOREIGN KEY([LndCategoryId])
REFERENCES [dbo].[StkItemCategory] ([PKey])
GO
ALTER TABLE [dbo].[LndLand] CHECK CONSTRAINT [LndLand__StkItemCategory_FK1]
GO
ALTER TABLE [dbo].[LndLand]  WITH CHECK ADD  CONSTRAINT [LndLand__StkItemType_FK1] FOREIGN KEY([LndTypeId])
REFERENCES [dbo].[StkItemType] ([PKey])
GO
ALTER TABLE [dbo].[LndLand] CHECK CONSTRAINT [LndLand__StkItemType_FK1]
GO
ALTER TABLE [dbo].[LndLand]  WITH CHECK ADD  CONSTRAINT [LndLand__StrEntity_FK1] FOREIGN KEY([StrEntityId])
REFERENCES [dbo].[StrEntity] ([PKey])
GO
ALTER TABLE [dbo].[LndLand] CHECK CONSTRAINT [LndLand__StrEntity_FK1]
GO
ALTER TABLE [dbo].[LndLand]  WITH CHECK ADD  CONSTRAINT [LndLand__StrStatus_FK1] FOREIGN KEY([StrStatusId])
REFERENCES [dbo].[StrStatus] ([PKey])
GO
ALTER TABLE [dbo].[LndLand] CHECK CONSTRAINT [LndLand__StrStatus_FK1]
GO
ALTER TABLE [dbo].[LndLand]  WITH CHECK ADD  CONSTRAINT [LndLand_CfgLawyer_FK1] FOREIGN KEY([CfgLawyerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[LndLand] CHECK CONSTRAINT [LndLand_CfgLawyer_FK1]
GO
ALTER TABLE [dbo].[LndLand]  WITH CHECK ADD  CONSTRAINT [LndLand_CfgMiddleman_FK1] FOREIGN KEY([CfgMiddlemanId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[LndLand] CHECK CONSTRAINT [LndLand_CfgMiddleman_FK1]
GO
ALTER TABLE [dbo].[LndLand]  WITH CHECK ADD  CONSTRAINT [LndLand_StkOrientation_FK1] FOREIGN KEY([StkOrientationId])
REFERENCES [dbo].[StkOrientation] ([PKey])
GO
ALTER TABLE [dbo].[LndLand] CHECK CONSTRAINT [LndLand_StkOrientation_FK1]
GO
ALTER TABLE [dbo].[LndLand]  WITH CHECK ADD  CONSTRAINT [LndLevel_LndLand_FK1] FOREIGN KEY([LndLevelId])
REFERENCES [dbo].[LndLevel] ([PKey])
GO
ALTER TABLE [dbo].[LndLand] CHECK CONSTRAINT [LndLevel_LndLand_FK1]
GO
ALTER TABLE [dbo].[LndLand]  WITH CHECK ADD  CONSTRAINT [LndVocation_LndLand_FK1] FOREIGN KEY([LndVocationId])
REFERENCES [dbo].[LndVocation] ([PKey])
GO
ALTER TABLE [dbo].[LndLand] CHECK CONSTRAINT [LndVocation_LndLand_FK1]
GO
ALTER TABLE [dbo].[PrjClause]  WITH CHECK ADD  CONSTRAINT [PrjClause__CfgTier_FK1] FOREIGN KEY([CfgTierId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[PrjClause] CHECK CONSTRAINT [PrjClause__CfgTier_FK1]
GO
ALTER TABLE [dbo].[PrjClause]  WITH CHECK ADD  CONSTRAINT [PrjClause__PrjMarket_FK1] FOREIGN KEY([PrjMarketId])
REFERENCES [dbo].[PrjMarket] ([PKey])
GO
ALTER TABLE [dbo].[PrjClause] CHECK CONSTRAINT [PrjClause__PrjMarket_FK1]
GO
ALTER TABLE [dbo].[PrjMarket]  WITH CHECK ADD  CONSTRAINT [MarketssOfSettlementCategory] FOREIGN KEY([StlCategoryId])
REFERENCES [dbo].[StlCategory] ([PKey])
GO
ALTER TABLE [dbo].[PrjMarket] CHECK CONSTRAINT [MarketssOfSettlementCategory]
GO
ALTER TABLE [dbo].[PrjMarket]  WITH CHECK ADD  CONSTRAINT [PrjMarket__StlCurrency_FK1] FOREIGN KEY([StlCurrencyId])
REFERENCES [dbo].[StlCurrency] ([PKey])
GO
ALTER TABLE [dbo].[PrjMarket] CHECK CONSTRAINT [PrjMarket__StlCurrency_FK1]
GO
ALTER TABLE [dbo].[PrjMarket]  WITH CHECK ADD  CONSTRAINT [PrjMarket__StrEntity_FK1] FOREIGN KEY([StrEntityId])
REFERENCES [dbo].[StrEntity] ([PKey])
GO
ALTER TABLE [dbo].[PrjMarket] CHECK CONSTRAINT [PrjMarket__StrEntity_FK1]
GO
ALTER TABLE [dbo].[PrjMarket]  WITH CHECK ADD  CONSTRAINT [PrjMarket__StrStatus_FK1] FOREIGN KEY([StrStatusId])
REFERENCES [dbo].[StrStatus] ([PKey])
GO
ALTER TABLE [dbo].[PrjMarket] CHECK CONSTRAINT [PrjMarket__StrStatus_FK1]
GO
ALTER TABLE [dbo].[PrjMarket]  WITH CHECK ADD  CONSTRAINT [PrjMarket_StkLevelingLine_FK] FOREIGN KEY([StkLevelingLineId])
REFERENCES [dbo].[StkLevelingLine] ([PKey])
GO
ALTER TABLE [dbo].[PrjMarket] CHECK CONSTRAINT [PrjMarket_StkLevelingLine_FK]
GO
ALTER TABLE [dbo].[PrjMarket]  WITH CHECK ADD  CONSTRAINT [PrjMarketType_PrjMarket_FK1] FOREIGN KEY([PrjMarketTypeId])
REFERENCES [dbo].[PrjMarketType] ([PKey])
GO
ALTER TABLE [dbo].[PrjMarket] CHECK CONSTRAINT [PrjMarketType_PrjMarket_FK1]
GO
ALTER TABLE [dbo].[PrjMarket]  WITH CHECK ADD  CONSTRAINT [PrjProject_PrjMarket_FK1] FOREIGN KEY([PrjProjectId])
REFERENCES [dbo].[PrjProject] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[PrjMarket] CHECK CONSTRAINT [PrjProject_PrjMarket_FK1]
GO
ALTER TABLE [dbo].[PrjMarket]  WITH CHECK ADD  CONSTRAINT [StlSettlementType_PrjMarket_FK1] FOREIGN KEY([StlSettlementTypeId])
REFERENCES [dbo].[StlSettlementType] ([PKey])
GO
ALTER TABLE [dbo].[PrjMarket] CHECK CONSTRAINT [StlSettlementType_PrjMarket_FK1]
GO
ALTER TABLE [dbo].[PrjProject]  WITH CHECK ADD  CONSTRAINT [PrjProject__CfgTierManager_FK1] FOREIGN KEY([CfgTierManagerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[PrjProject] CHECK CONSTRAINT [PrjProject__CfgTierManager_FK1]
GO
ALTER TABLE [dbo].[PrjProject]  WITH CHECK ADD  CONSTRAINT [PrjProject__StrEntity_FK1] FOREIGN KEY([StrEntityId])
REFERENCES [dbo].[StrEntity] ([PKey])
GO
ALTER TABLE [dbo].[PrjProject] CHECK CONSTRAINT [PrjProject__StrEntity_FK1]
GO
ALTER TABLE [dbo].[PrjProject]  WITH CHECK ADD  CONSTRAINT [PrjProject__StrStatus_FK1] FOREIGN KEY([StrStatusId])
REFERENCES [dbo].[StrStatus] ([PKey])
GO
ALTER TABLE [dbo].[PrjProject] CHECK CONSTRAINT [PrjProject__StrStatus_FK1]
GO
ALTER TABLE [dbo].[PrjProject]  WITH CHECK ADD  CONSTRAINT [PrjProject_CfgCompany_FK1] FOREIGN KEY([CfgCompanyId])
REFERENCES [dbo].[CfgCompany] ([PKey])
GO
ALTER TABLE [dbo].[PrjProject] CHECK CONSTRAINT [PrjProject_CfgCompany_FK1]
GO
ALTER TABLE [dbo].[PrjProject]  WITH CHECK ADD  CONSTRAINT [PrjProject_PrjMarketId_FK1] FOREIGN KEY([ManageProrataPrjMarketId])
REFERENCES [dbo].[PrjMarket] ([PKey])
GO
ALTER TABLE [dbo].[PrjProject] CHECK CONSTRAINT [PrjProject_PrjMarketId_FK1]
GO
ALTER TABLE [dbo].[PrjProject]  WITH CHECK ADD  CONSTRAINT [PrjProject_PrjProject_FK1] FOREIGN KEY([ReferenceId])
REFERENCES [dbo].[PrjProject] ([PKey])
GO
ALTER TABLE [dbo].[PrjProject] CHECK CONSTRAINT [PrjProject_PrjProject_FK1]
GO
ALTER TABLE [dbo].[PrjProject]  WITH CHECK ADD  CONSTRAINT [PrjProject_StkLeveling_FK1] FOREIGN KEY([StkLevelingId])
REFERENCES [dbo].[StkLeveling] ([PKey])
GO
ALTER TABLE [dbo].[PrjProject] CHECK CONSTRAINT [PrjProject_StkLeveling_FK1]
GO
ALTER TABLE [dbo].[PrjWorkingOut]  WITH CHECK ADD  CONSTRAINT [CfgTier_PrjWorkingOut_FK1] FOREIGN KEY([CfgTierId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[PrjWorkingOut] CHECK CONSTRAINT [CfgTier_PrjWorkingOut_FK1]
GO
ALTER TABLE [dbo].[PrjWorkingOut]  WITH CHECK ADD  CONSTRAINT [PrjMarket_PrjWorkingOut_FK1] FOREIGN KEY([PrjMarketId])
REFERENCES [dbo].[PrjMarket] ([PKey])
GO
ALTER TABLE [dbo].[PrjWorkingOut] CHECK CONSTRAINT [PrjMarket_PrjWorkingOut_FK1]
GO
ALTER TABLE [dbo].[PrjWorkingOut]  WITH CHECK ADD  CONSTRAINT [PrjWorkingOut__StlCurrency_FK1] FOREIGN KEY([StlCurrencyId])
REFERENCES [dbo].[StlCurrency] ([PKey])
GO
ALTER TABLE [dbo].[PrjWorkingOut] CHECK CONSTRAINT [PrjWorkingOut__StlCurrency_FK1]
GO
ALTER TABLE [dbo].[PrjWorkingOut]  WITH CHECK ADD  CONSTRAINT [PrjWorkingOut_BuyDocument_FK1] FOREIGN KEY([BuyDocumentId])
REFERENCES [dbo].[BuyDocument] ([PKey])
ON DELETE SET NULL
GO
ALTER TABLE [dbo].[PrjWorkingOut] CHECK CONSTRAINT [PrjWorkingOut_BuyDocument_FK1]
GO
ALTER TABLE [dbo].[PrmRequestOffer]  WITH CHECK ADD  CONSTRAINT [PrmRequestOffer__CfgTier_FK1] FOREIGN KEY([CfgTierId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[PrmRequestOffer] CHECK CONSTRAINT [PrmRequestOffer__CfgTier_FK1]
GO
ALTER TABLE [dbo].[PrmRequestOffer]  WITH CHECK ADD  CONSTRAINT [PrmRequestOffer__CfgTier_FK2] FOREIGN KEY([CfgCommercialId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[PrmRequestOffer] CHECK CONSTRAINT [PrmRequestOffer__CfgTier_FK2]
GO
ALTER TABLE [dbo].[PrmRequestOffer]  WITH CHECK ADD  CONSTRAINT [PrmRequestOffer__CfgTier_FK3] FOREIGN KEY([CfgIntermediateId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[PrmRequestOffer] CHECK CONSTRAINT [PrmRequestOffer__CfgTier_FK3]
GO
ALTER TABLE [dbo].[PrmRequestOffer]  WITH CHECK ADD  CONSTRAINT [PrmRequestOffer__CfgTier_FK4] FOREIGN KEY([CfgOwnerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[PrmRequestOffer] CHECK CONSTRAINT [PrmRequestOffer__CfgTier_FK4]
GO
ALTER TABLE [dbo].[PrmRequestOffer]  WITH CHECK ADD  CONSTRAINT [PrmRequestOffer__CfgTier_FK5] FOREIGN KEY([CfgCustomerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[PrmRequestOffer] CHECK CONSTRAINT [PrmRequestOffer__CfgTier_FK5]
GO
ALTER TABLE [dbo].[PrmRequestOffer]  WITH CHECK ADD  CONSTRAINT [PrmRequestOffer__CfgTier_FK6] FOREIGN KEY([CfgProposerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[PrmRequestOffer] CHECK CONSTRAINT [PrmRequestOffer__CfgTier_FK6]
GO
ALTER TABLE [dbo].[PrmRequestOffer]  WITH CHECK ADD  CONSTRAINT [PrmRequestOffer__CfgTranche_FK1] FOREIGN KEY([CfgTrancheId])
REFERENCES [dbo].[CfgTranche] ([PKey])
GO
ALTER TABLE [dbo].[PrmRequestOffer] CHECK CONSTRAINT [PrmRequestOffer__CfgTranche_FK1]
GO
ALTER TABLE [dbo].[PrmRequestOffer]  WITH CHECK ADD  CONSTRAINT [PrmRequestOffer__ComAction_FK3] FOREIGN KEY([ComActionId])
REFERENCES [dbo].[ComAction] ([PKey])
GO
ALTER TABLE [dbo].[PrmRequestOffer] CHECK CONSTRAINT [PrmRequestOffer__ComAction_FK3]
GO
ALTER TABLE [dbo].[PrmRequestOffer]  WITH CHECK ADD  CONSTRAINT [PrmRequestOffer__ComActionMessageType_FK1] FOREIGN KEY([ComActionMessageTypeId])
REFERENCES [dbo].[ComActionMessageType] ([PKey])
GO
ALTER TABLE [dbo].[PrmRequestOffer] CHECK CONSTRAINT [PrmRequestOffer__ComActionMessageType_FK1]
GO
ALTER TABLE [dbo].[PrmRequestOffer]  WITH CHECK ADD  CONSTRAINT [PrmRequestOffer__ComProspectionKind_FK1] FOREIGN KEY([ComProspectionKindId])
REFERENCES [dbo].[ComProspectionKind] ([PKey])
GO
ALTER TABLE [dbo].[PrmRequestOffer] CHECK CONSTRAINT [PrmRequestOffer__ComProspectionKind_FK1]
GO
ALTER TABLE [dbo].[PrmRequestOffer]  WITH CHECK ADD  CONSTRAINT [PrmRequestOffer__ComProspectionOrigin_FK1] FOREIGN KEY([ComProspectionOriginId])
REFERENCES [dbo].[ComProspectionOrigin] ([PKey])
GO
ALTER TABLE [dbo].[PrmRequestOffer] CHECK CONSTRAINT [PrmRequestOffer__ComProspectionOrigin_FK1]
GO
ALTER TABLE [dbo].[PrmRequestOffer]  WITH CHECK ADD  CONSTRAINT [PrmRequestOffer__ComProspectionOriginSupport_FK1] FOREIGN KEY([ComProspectionOriginSupportId])
REFERENCES [dbo].[ComProspectionOriginSupport] ([PKey])
GO
ALTER TABLE [dbo].[PrmRequestOffer] CHECK CONSTRAINT [PrmRequestOffer__ComProspectionOriginSupport_FK1]
GO
ALTER TABLE [dbo].[PrmRequestOffer]  WITH CHECK ADD  CONSTRAINT [PrmRequestOffer__StkItem_FK1] FOREIGN KEY([PrmPropertyId])
REFERENCES [dbo].[StkItem] ([PKey])
GO
ALTER TABLE [dbo].[PrmRequestOffer] CHECK CONSTRAINT [PrmRequestOffer__StkItem_FK1]
GO
ALTER TABLE [dbo].[PrmRequestOffer]  WITH CHECK ADD  CONSTRAINT [PrmRequestOffer__StrEntity_FK1] FOREIGN KEY([StrEntityId])
REFERENCES [dbo].[StrEntity] ([PKey])
GO
ALTER TABLE [dbo].[PrmRequestOffer] CHECK CONSTRAINT [PrmRequestOffer__StrEntity_FK1]
GO
ALTER TABLE [dbo].[PrmRequestOffer]  WITH CHECK ADD  CONSTRAINT [PrmRequestOffer__StrStatus_FK1] FOREIGN KEY([StrStatusId])
REFERENCES [dbo].[StrStatus] ([PKey])
GO
ALTER TABLE [dbo].[PrmRequestOffer] CHECK CONSTRAINT [PrmRequestOffer__StrStatus_FK1]
GO
ALTER TABLE [dbo].[PrmRequestOffer]  WITH CHECK ADD  CONSTRAINT [PrmRequestOffer_AdmCity_FK] FOREIGN KEY([AdmCityId])
REFERENCES [dbo].[AdmCity] ([PKey])
GO
ALTER TABLE [dbo].[PrmRequestOffer] CHECK CONSTRAINT [PrmRequestOffer_AdmCity_FK]
GO
ALTER TABLE [dbo].[PrmRequestOffer]  WITH CHECK ADD  CONSTRAINT [PrmRequestOffer_AdmCountry_FK] FOREIGN KEY([AdmCountryId])
REFERENCES [dbo].[AdmCountry] ([PKey])
GO
ALTER TABLE [dbo].[PrmRequestOffer] CHECK CONSTRAINT [PrmRequestOffer_AdmCountry_FK]
GO
ALTER TABLE [dbo].[PrmRequestOffer]  WITH CHECK ADD  CONSTRAINT [PrmRequestOffer_RntPeriodicity_FK] FOREIGN KEY([RntPeriodicityId])
REFERENCES [dbo].[RntPeriodicity] ([PKey])
GO
ALTER TABLE [dbo].[PrmRequestOffer] CHECK CONSTRAINT [PrmRequestOffer_RntPeriodicity_FK]
GO
ALTER TABLE [dbo].[PrmRequestOffer]  WITH CHECK ADD  CONSTRAINT [PrmVisit__StkItemCategory_FK1] FOREIGN KEY([PrmPropertyCategoryId])
REFERENCES [dbo].[StkItemCategory] ([PKey])
GO
ALTER TABLE [dbo].[PrmRequestOffer] CHECK CONSTRAINT [PrmVisit__StkItemCategory_FK1]
GO
ALTER TABLE [dbo].[PrmRequestOffer]  WITH CHECK ADD  CONSTRAINT [PrmVisit__StkItemType_FK1] FOREIGN KEY([PrmPropertyTypeId])
REFERENCES [dbo].[StkItemType] ([PKey])
GO
ALTER TABLE [dbo].[PrmRequestOffer] CHECK CONSTRAINT [PrmVisit__StkItemType_FK1]
GO
ALTER TABLE [dbo].[RntDocument]  WITH CHECK ADD  CONSTRAINT [RntDocument__CfgTier_FK1] FOREIGN KEY([CfgDebtCollectorId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[RntDocument] CHECK CONSTRAINT [RntDocument__CfgTier_FK1]
GO
ALTER TABLE [dbo].[RntDocument]  WITH CHECK ADD  CONSTRAINT [RntDocument__RntFolder_FK1] FOREIGN KEY([RntFolderId])
REFERENCES [dbo].[RntFolder] ([PKey])
GO
ALTER TABLE [dbo].[RntDocument] CHECK CONSTRAINT [RntDocument__RntFolder_FK1]
GO
ALTER TABLE [dbo].[RntDocument]  WITH CHECK ADD  CONSTRAINT [Rntdocument__RntPeriodicity_FK1] FOREIGN KEY([RntPeriodicityId])
REFERENCES [dbo].[RntPeriodicity] ([PKey])
GO
ALTER TABLE [dbo].[RntDocument] CHECK CONSTRAINT [Rntdocument__RntPeriodicity_FK1]
GO
ALTER TABLE [dbo].[RntDocument]  WITH CHECK ADD  CONSTRAINT [RntDocument__RntReconductionHistory_FK1] FOREIGN KEY([RntReconductionHistoryId])
REFERENCES [dbo].[RntReconductionHistory] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[RntDocument] CHECK CONSTRAINT [RntDocument__RntReconductionHistory_FK1]
GO
ALTER TABLE [dbo].[RntDocument]  WITH CHECK ADD  CONSTRAINT [RntDocument__RntSettlementCategory_FK1] FOREIGN KEY([RntSettlementCategoryId])
REFERENCES [dbo].[RntSettlementCategory] ([PKey])
GO
ALTER TABLE [dbo].[RntDocument] CHECK CONSTRAINT [RntDocument__RntSettlementCategory_FK1]
GO
ALTER TABLE [dbo].[RntDocument]  WITH CHECK ADD  CONSTRAINT [RntDocument__SynFolder_FK1] FOREIGN KEY([SynFolderId])
REFERENCES [dbo].[SynFolder] ([PKey])
GO
ALTER TABLE [dbo].[RntDocument] CHECK CONSTRAINT [RntDocument__SynFolder_FK1]
GO
ALTER TABLE [dbo].[RntDocument]  WITH CHECK ADD  CONSTRAINT [RntDocument_AdmModule_FK1] FOREIGN KEY([AdmModuleId])
REFERENCES [dbo].[AdmModule] ([PKey])
GO
ALTER TABLE [dbo].[RntDocument] CHECK CONSTRAINT [RntDocument_AdmModule_FK1]
GO
ALTER TABLE [dbo].[RntDocument]  WITH CHECK ADD  CONSTRAINT [RntDocument_BuyDocumentLine_FK1] FOREIGN KEY([BuyDocumentLineId])
REFERENCES [dbo].[BuyDocumentLine] ([Pkey])
ON DELETE SET NULL
GO
ALTER TABLE [dbo].[RntDocument] CHECK CONSTRAINT [RntDocument_BuyDocumentLine_FK1]
GO
ALTER TABLE [dbo].[RntDocument]  WITH CHECK ADD  CONSTRAINT [RntDocument_CfgCompany_FK1] FOREIGN KEY([CfgCompanyId])
REFERENCES [dbo].[CfgCompany] ([PKey])
GO
ALTER TABLE [dbo].[RntDocument] CHECK CONSTRAINT [RntDocument_CfgCompany_FK1]
GO
ALTER TABLE [dbo].[RntDocument]  WITH CHECK ADD  CONSTRAINT [RntDocument_ComFolder_FK1] FOREIGN KEY([ComFolderId])
REFERENCES [dbo].[ComFolder] ([PKey])
GO
ALTER TABLE [dbo].[RntDocument] CHECK CONSTRAINT [RntDocument_ComFolder_FK1]
GO
ALTER TABLE [dbo].[RntDocument]  WITH CHECK ADD  CONSTRAINT [RntDocument_StlRecoveryFolder_FK1] FOREIGN KEY([StlRecoveryFolderId])
REFERENCES [dbo].[StlRecoveryFolder] ([PKey])
GO
ALTER TABLE [dbo].[RntDocument] CHECK CONSTRAINT [RntDocument_StlRecoveryFolder_FK1]
GO
ALTER TABLE [dbo].[RntFolder]  WITH CHECK ADD  CONSTRAINT [RntFolder__CfgCommercial_FK1] FOREIGN KEY([CfgCommercialId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[RntFolder] CHECK CONSTRAINT [RntFolder__CfgCommercial_FK1]
GO
ALTER TABLE [dbo].[RntFolder]  WITH CHECK ADD  CONSTRAINT [RntFolder__CfgCustomer_FK1] FOREIGN KEY([CfgCustomerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[RntFolder] CHECK CONSTRAINT [RntFolder__CfgCustomer_FK1]
GO
ALTER TABLE [dbo].[RntFolder]  WITH CHECK ADD  CONSTRAINT [RntFolder__CfgLawyer_FK1] FOREIGN KEY([CfgLawyerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[RntFolder] CHECK CONSTRAINT [RntFolder__CfgLawyer_FK1]
GO
ALTER TABLE [dbo].[RntFolder]  WITH CHECK ADD  CONSTRAINT [RntFolder__CfgOwner_FK1] FOREIGN KEY([CfgOwnerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[RntFolder] CHECK CONSTRAINT [RntFolder__CfgOwner_FK1]
GO
ALTER TABLE [dbo].[RntFolder]  WITH CHECK ADD  CONSTRAINT [RntFolder__CfgTier_FKBailiff] FOREIGN KEY([CfgTierBailiffId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[RntFolder] CHECK CONSTRAINT [RntFolder__CfgTier_FKBailiff]
GO
ALTER TABLE [dbo].[RntFolder]  WITH CHECK ADD  CONSTRAINT [RntFolder__RntPeriodicity_FK1] FOREIGN KEY([RntPeriodicityId])
REFERENCES [dbo].[RntPeriodicity] ([PKey])
GO
ALTER TABLE [dbo].[RntFolder] CHECK CONSTRAINT [RntFolder__RntPeriodicity_FK1]
GO
ALTER TABLE [dbo].[RntFolder]  WITH CHECK ADD  CONSTRAINT [RntFolder__RntPeriodicityReverse_FK1] FOREIGN KEY([RntPeriodicityReverseId])
REFERENCES [dbo].[RntPeriodicity] ([PKey])
GO
ALTER TABLE [dbo].[RntFolder] CHECK CONSTRAINT [RntFolder__RntPeriodicityReverse_FK1]
GO
ALTER TABLE [dbo].[RntFolder]  WITH CHECK ADD  CONSTRAINT [RntFolder__RntReconductionType_FK1] FOREIGN KEY([RntReconductionTypeId])
REFERENCES [dbo].[RntReconductionType] ([PKey])
GO
ALTER TABLE [dbo].[RntFolder] CHECK CONSTRAINT [RntFolder__RntReconductionType_FK1]
GO
ALTER TABLE [dbo].[RntFolder]  WITH CHECK ADD  CONSTRAINT [RntFolder__StkItem_FK1] FOREIGN KEY([StkItemId])
REFERENCES [dbo].[StkItem] ([PKey])
GO
ALTER TABLE [dbo].[RntFolder] CHECK CONSTRAINT [RntFolder__StkItem_FK1]
GO
ALTER TABLE [dbo].[RntFolder]  WITH CHECK ADD  CONSTRAINT [RntFolder__StkVocation_FK1] FOREIGN KEY([StkVocationId])
REFERENCES [dbo].[StkVocation] ([PKey])
GO
ALTER TABLE [dbo].[RntFolder] CHECK CONSTRAINT [RntFolder__StkVocation_FK1]
GO
ALTER TABLE [dbo].[RntFolder]  WITH CHECK ADD  CONSTRAINT [RntFolder__StrEntity_FK1] FOREIGN KEY([StrEntityId])
REFERENCES [dbo].[StrEntity] ([PKey])
GO
ALTER TABLE [dbo].[RntFolder] CHECK CONSTRAINT [RntFolder__StrEntity_FK1]
GO
ALTER TABLE [dbo].[RntFolder]  WITH CHECK ADD  CONSTRAINT [RntFolder__StrStatus_FK1] FOREIGN KEY([StrStatusId])
REFERENCES [dbo].[StrStatus] ([PKey])
GO
ALTER TABLE [dbo].[RntFolder] CHECK CONSTRAINT [RntFolder__StrStatus_FK1]
GO
ALTER TABLE [dbo].[RntFolder]  WITH CHECK ADD  CONSTRAINT [RntFolder_ComProspection_FK] FOREIGN KEY([ComProspectionId])
REFERENCES [dbo].[ComProspection] ([PKey])
GO
ALTER TABLE [dbo].[RntFolder] CHECK CONSTRAINT [RntFolder_ComProspection_FK]
GO
ALTER TABLE [dbo].[RntFolder]  WITH CHECK ADD  CONSTRAINT [RntFolder_StlQuote_FK2] FOREIGN KEY([StlQuoteId])
REFERENCES [dbo].[StlQuote] ([PKey])
GO
ALTER TABLE [dbo].[RntFolder] CHECK CONSTRAINT [RntFolder_StlQuote_FK2]
GO
ALTER TABLE [dbo].[RntReconductionHistory]  WITH CHECK ADD  CONSTRAINT [RntReconductionHistory__RntFolder_FK1] FOREIGN KEY([RntFolderId])
REFERENCES [dbo].[RntFolder] ([PKey])
GO
ALTER TABLE [dbo].[RntReconductionHistory] CHECK CONSTRAINT [RntReconductionHistory__RntFolder_FK1]
GO
ALTER TABLE [dbo].[RntReconductionHistory]  WITH CHECK ADD  CONSTRAINT [RntReconductionHistory__SynFolder_FK1] FOREIGN KEY([SynFolderId])
REFERENCES [dbo].[SynFolder] ([PKey])
GO
ALTER TABLE [dbo].[RntReconductionHistory] CHECK CONSTRAINT [RntReconductionHistory__SynFolder_FK1]
GO
ALTER TABLE [dbo].[RntSettlementCategory]  WITH CHECK ADD  CONSTRAINT [RntSettlementCategory__StlCategory_FK1] FOREIGN KEY([StlCategoryId])
REFERENCES [dbo].[StlCategory] ([PKey])
GO
ALTER TABLE [dbo].[RntSettlementCategory] CHECK CONSTRAINT [RntSettlementCategory__StlCategory_FK1]
GO
ALTER TABLE [dbo].[SavClaim]  WITH CHECK ADD  CONSTRAINT [SavClaim__CfgProjectZone_FK1] FOREIGN KEY([CfgProjectZoneId])
REFERENCES [dbo].[CfgProjectZone] ([PKey])
GO
ALTER TABLE [dbo].[SavClaim] CHECK CONSTRAINT [SavClaim__CfgProjectZone_FK1]
GO
ALTER TABLE [dbo].[SavClaim]  WITH CHECK ADD  CONSTRAINT [SavClaim__StrEntity_FK1] FOREIGN KEY([StrEntityId])
REFERENCES [dbo].[StrEntity] ([PKey])
GO
ALTER TABLE [dbo].[SavClaim] CHECK CONSTRAINT [SavClaim__StrEntity_FK1]
GO
ALTER TABLE [dbo].[SavClaim]  WITH CHECK ADD  CONSTRAINT [SavClaim__StrStatus_FK1] FOREIGN KEY([StrStatusId])
REFERENCES [dbo].[StrStatus] ([PKey])
GO
ALTER TABLE [dbo].[SavClaim] CHECK CONSTRAINT [SavClaim__StrStatus_FK1]
GO
ALTER TABLE [dbo].[SavClaim]  WITH CHECK ADD  CONSTRAINT [SavClaim_CfgClaimManager_FK1] FOREIGN KEY([CfgClaimManagerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[SavClaim] CHECK CONSTRAINT [SavClaim_CfgClaimManager_FK1]
GO
ALTER TABLE [dbo].[SavClaim]  WITH CHECK ADD  CONSTRAINT [SavClaim_CfgSupplier_FK1] FOREIGN KEY([CfgSupplierId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[SavClaim] CHECK CONSTRAINT [SavClaim_CfgSupplier_FK1]
GO
ALTER TABLE [dbo].[SavClaim]  WITH CHECK ADD  CONSTRAINT [SavClaim_PrjMarket_FK1] FOREIGN KEY([PrjMarketId])
REFERENCES [dbo].[PrjMarket] ([PKey])
GO
ALTER TABLE [dbo].[SavClaim] CHECK CONSTRAINT [SavClaim_PrjMarket_FK1]
GO
ALTER TABLE [dbo].[SavClaim]  WITH CHECK ADD  CONSTRAINT [SavClaim_PrjMarket_FK2] FOREIGN KEY([PrjMarketClaimId])
REFERENCES [dbo].[PrjMarket] ([PKey])
GO
ALTER TABLE [dbo].[SavClaim] CHECK CONSTRAINT [SavClaim_PrjMarket_FK2]
GO
ALTER TABLE [dbo].[SavClaim]  WITH CHECK ADD  CONSTRAINT [SavClaim_SavClaimGroup_FK1] FOREIGN KEY([SavClaimGroupId])
REFERENCES [dbo].[SavClaimGroup] ([PKey])
GO
ALTER TABLE [dbo].[SavClaim] CHECK CONSTRAINT [SavClaim_SavClaimGroup_FK1]
GO
ALTER TABLE [dbo].[SavClaim]  WITH CHECK ADD  CONSTRAINT [SavClaim_SavComplaintCategory_FK1] FOREIGN KEY([SavComplaintCategoryId])
REFERENCES [dbo].[SavComplaintCategory] ([PKey])
GO
ALTER TABLE [dbo].[SavClaim] CHECK CONSTRAINT [SavClaim_SavComplaintCategory_FK1]
GO
ALTER TABLE [dbo].[SavClaim]  WITH CHECK ADD  CONSTRAINT [SavClaim_SavComplaintType_FK1] FOREIGN KEY([SavComplaintTypeId])
REFERENCES [dbo].[SavComplaintType] ([PKey])
GO
ALTER TABLE [dbo].[SavClaim] CHECK CONSTRAINT [SavClaim_SavComplaintType_FK1]
GO
ALTER TABLE [dbo].[SavClaim]  WITH CHECK ADD  CONSTRAINT [SavClaim_StlCategory_FK1] FOREIGN KEY([StlCategoryId])
REFERENCES [dbo].[StlCategory] ([PKey])
GO
ALTER TABLE [dbo].[SavClaim] CHECK CONSTRAINT [SavClaim_StlCategory_FK1]
GO
ALTER TABLE [dbo].[SavClaim]  WITH CHECK ADD  CONSTRAINT [SavClaim_StlCurrency_FK1] FOREIGN KEY([StlCurrencyId])
REFERENCES [dbo].[StlCurrency] ([PKey])
GO
ALTER TABLE [dbo].[SavClaim] CHECK CONSTRAINT [SavClaim_StlCurrency_FK1]
GO
ALTER TABLE [dbo].[SavClaimGroup]  WITH CHECK ADD  CONSTRAINT [SavClaimGroup__CfgCompany_FK1] FOREIGN KEY([CfgCompanyId])
REFERENCES [dbo].[CfgCompany] ([PKey])
GO
ALTER TABLE [dbo].[SavClaimGroup] CHECK CONSTRAINT [SavClaimGroup__CfgCompany_FK1]
GO
ALTER TABLE [dbo].[SavClaimGroup]  WITH CHECK ADD  CONSTRAINT [SavClaimGroup__ComFolder_FK1] FOREIGN KEY([ComFolderId])
REFERENCES [dbo].[ComFolder] ([PKey])
GO
ALTER TABLE [dbo].[SavClaimGroup] CHECK CONSTRAINT [SavClaimGroup__ComFolder_FK1]
GO
ALTER TABLE [dbo].[SavClaimGroup]  WITH CHECK ADD  CONSTRAINT [SavClaimGroup_CfgClaimsManager_FK1] FOREIGN KEY([CfgClaimsManagerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[SavClaimGroup] CHECK CONSTRAINT [SavClaimGroup_CfgClaimsManager_FK1]
GO
ALTER TABLE [dbo].[SavClaimGroup]  WITH CHECK ADD  CONSTRAINT [SavClaimGroup_CfgCustomer_FK1] FOREIGN KEY([CfgCustomerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[SavClaimGroup] CHECK CONSTRAINT [SavClaimGroup_CfgCustomer_FK1]
GO
ALTER TABLE [dbo].[SavClaimGroup]  WITH CHECK ADD  CONSTRAINT [SavClaimGroup_CfgTierTrigger_FK1] FOREIGN KEY([CfgTierTriggerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[SavClaimGroup] CHECK CONSTRAINT [SavClaimGroup_CfgTierTrigger_FK1]
GO
ALTER TABLE [dbo].[SavClaimGroup]  WITH CHECK ADD  CONSTRAINT [SavClaimGroup_CfgTranche_FK1] FOREIGN KEY([CfgTrancheId])
REFERENCES [dbo].[CfgTranche] ([PKey])
GO
ALTER TABLE [dbo].[SavClaimGroup] CHECK CONSTRAINT [SavClaimGroup_CfgTranche_FK1]
GO
ALTER TABLE [dbo].[SavClaimGroup]  WITH CHECK ADD  CONSTRAINT [SavClaimGroup_RntFolder_FK1] FOREIGN KEY([RntFolderId])
REFERENCES [dbo].[RntFolder] ([PKey])
GO
ALTER TABLE [dbo].[SavClaimGroup] CHECK CONSTRAINT [SavClaimGroup_RntFolder_FK1]
GO
ALTER TABLE [dbo].[SavClaimGroup]  WITH CHECK ADD  CONSTRAINT [SavClaimGroup_SavOrigin_FK1] FOREIGN KEY([SavOriginId])
REFERENCES [dbo].[SavOrigin] ([PKey])
GO
ALTER TABLE [dbo].[SavClaimGroup] CHECK CONSTRAINT [SavClaimGroup_SavOrigin_FK1]
GO
ALTER TABLE [dbo].[SavClaimGroup]  WITH CHECK ADD  CONSTRAINT [SavClaimGroup_StkItem_FK1] FOREIGN KEY([StkItemId])
REFERENCES [dbo].[StkItem] ([PKey])
GO
ALTER TABLE [dbo].[SavClaimGroup] CHECK CONSTRAINT [SavClaimGroup_StkItem_FK1]
GO
ALTER TABLE [dbo].[SavClaimGroup]  WITH CHECK ADD  CONSTRAINT [SavClaimGroup_StkVocation_FK1] FOREIGN KEY([StkVocationId])
REFERENCES [dbo].[StkVocation] ([PKey])
GO
ALTER TABLE [dbo].[SavClaimGroup] CHECK CONSTRAINT [SavClaimGroup_StkVocation_FK1]
GO
ALTER TABLE [dbo].[SavComplaintCategory]  WITH CHECK ADD  CONSTRAINT [SavComplaintCategory_StlCategory_FK1] FOREIGN KEY([StlCategoryId])
REFERENCES [dbo].[StlCategory] ([PKey])
GO
ALTER TABLE [dbo].[SavComplaintCategory] CHECK CONSTRAINT [SavComplaintCategory_StlCategory_FK1]
GO
ALTER TABLE [dbo].[SavComplaintType]  WITH CHECK ADD  CONSTRAINT [SavComplaintCategory_SavComplaintType_FK1] FOREIGN KEY([SavComplaintCategoryId])
REFERENCES [dbo].[SavComplaintCategory] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[SavComplaintType] CHECK CONSTRAINT [SavComplaintCategory_SavComplaintType_FK1]
GO
ALTER TABLE [dbo].[StkExpense]  WITH CHECK ADD  CONSTRAINT [StkExpense_StkExpenseType_FK1] FOREIGN KEY([StkExpenseTypeId])
REFERENCES [dbo].[StkExpenseType] ([Pkey])
GO
ALTER TABLE [dbo].[StkExpense] CHECK CONSTRAINT [StkExpense_StkExpenseType_FK1]
GO
ALTER TABLE [dbo].[StkExpense]  WITH CHECK ADD  CONSTRAINT [StkExpense_StkItem_FK1] FOREIGN KEY([StkItemId])
REFERENCES [dbo].[StkItem] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[StkExpense] CHECK CONSTRAINT [StkExpense_StkItem_FK1]
GO
ALTER TABLE [dbo].[StkExpenseType]  WITH CHECK ADD  CONSTRAINT [StkExpenseType__PrjMarketType_FK1] FOREIGN KEY([PrjMarketTypeId])
REFERENCES [dbo].[PrjMarketType] ([PKey])
GO
ALTER TABLE [dbo].[StkExpenseType] CHECK CONSTRAINT [StkExpenseType__PrjMarketType_FK1]
GO
ALTER TABLE [dbo].[StkExpenseType]  WITH CHECK ADD  CONSTRAINT [StkExpenseType_StlCategory_FK1] FOREIGN KEY([StlCategoryId])
REFERENCES [dbo].[StlCategory] ([PKey])
GO
ALTER TABLE [dbo].[StkExpenseType] CHECK CONSTRAINT [StkExpenseType_StlCategory_FK1]
GO
ALTER TABLE [dbo].[StkItem]  WITH CHECK ADD  CONSTRAINT [StkItem__CfgCompanySyndic_FK1] FOREIGN KEY([CfgCompanySyndicId])
REFERENCES [dbo].[CfgCompany] ([PKey])
GO
ALTER TABLE [dbo].[StkItem] CHECK CONSTRAINT [StkItem__CfgCompanySyndic_FK1]
GO
ALTER TABLE [dbo].[StkItem]  WITH CHECK ADD  CONSTRAINT [StkItem__CfgTier_FK1] FOREIGN KEY([CfgMiddlemanId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[StkItem] CHECK CONSTRAINT [StkItem__CfgTier_FK1]
GO
ALTER TABLE [dbo].[StkItem]  WITH CHECK ADD  CONSTRAINT [StkItem__CfgTier_FK2] FOREIGN KEY([CfgOwnerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[StkItem] CHECK CONSTRAINT [StkItem__CfgTier_FK2]
GO
ALTER TABLE [dbo].[StkItem]  WITH CHECK ADD  CONSTRAINT [StkItem__LndLevel_FK1] FOREIGN KEY([LndLevelId])
REFERENCES [dbo].[LndLevel] ([PKey])
GO
ALTER TABLE [dbo].[StkItem] CHECK CONSTRAINT [StkItem__LndLevel_FK1]
GO
ALTER TABLE [dbo].[StkItem]  WITH CHECK ADD  CONSTRAINT [StkItem__LndVocation_FK1] FOREIGN KEY([LndVocationId])
REFERENCES [dbo].[LndVocation] ([PKey])
GO
ALTER TABLE [dbo].[StkItem] CHECK CONSTRAINT [StkItem__LndVocation_FK1]
GO
ALTER TABLE [dbo].[StkItem]  WITH CHECK ADD  CONSTRAINT [StkItem__RntFolder_FK1] FOREIGN KEY([RntFolderId])
REFERENCES [dbo].[RntFolder] ([PKey])
GO
ALTER TABLE [dbo].[StkItem] CHECK CONSTRAINT [StkItem__RntFolder_FK1]
GO
ALTER TABLE [dbo].[StkItem]  WITH CHECK ADD  CONSTRAINT [StkItem__RntPeriodicity_FK1] FOREIGN KEY([RntPeriodicityId])
REFERENCES [dbo].[RntPeriodicity] ([PKey])
GO
ALTER TABLE [dbo].[StkItem] CHECK CONSTRAINT [StkItem__RntPeriodicity_FK1]
GO
ALTER TABLE [dbo].[StkItem]  WITH CHECK ADD  CONSTRAINT [StkItem__StrEntity_FK1] FOREIGN KEY([StrEntityId])
REFERENCES [dbo].[StrEntity] ([PKey])
GO
ALTER TABLE [dbo].[StkItem] CHECK CONSTRAINT [StkItem__StrEntity_FK1]
GO
ALTER TABLE [dbo].[StkItem]  WITH CHECK ADD  CONSTRAINT [StkItem__StrStatus_FK1] FOREIGN KEY([StrStatusId])
REFERENCES [dbo].[StrStatus] ([PKey])
GO
ALTER TABLE [dbo].[StkItem] CHECK CONSTRAINT [StkItem__StrStatus_FK1]
GO
ALTER TABLE [dbo].[StkItem]  WITH CHECK ADD  CONSTRAINT [StkItem__SynFolder_FK1] FOREIGN KEY([SynFolderId])
REFERENCES [dbo].[SynFolder] ([PKey])
GO
ALTER TABLE [dbo].[StkItem] CHECK CONSTRAINT [StkItem__SynFolder_FK1]
GO
ALTER TABLE [dbo].[StkItem]  WITH CHECK ADD  CONSTRAINT [StkItem_AdmCity_FK1] FOREIGN KEY([AdmCityId])
REFERENCES [dbo].[AdmCity] ([PKey])
GO
ALTER TABLE [dbo].[StkItem] CHECK CONSTRAINT [StkItem_AdmCity_FK1]
GO
ALTER TABLE [dbo].[StkItem]  WITH CHECK ADD  CONSTRAINT [StkItem_AdmCountry_FK1] FOREIGN KEY([AdmCountryId])
REFERENCES [dbo].[AdmCountry] ([PKey])
GO
ALTER TABLE [dbo].[StkItem] CHECK CONSTRAINT [StkItem_AdmCountry_FK1]
GO
ALTER TABLE [dbo].[StkItem]  WITH CHECK ADD  CONSTRAINT [StkItem_CfgCompany_FK1] FOREIGN KEY([CfgCompanyId])
REFERENCES [dbo].[CfgCompany] ([PKey])
GO
ALTER TABLE [dbo].[StkItem] CHECK CONSTRAINT [StkItem_CfgCompany_FK1]
GO
ALTER TABLE [dbo].[StkItem]  WITH CHECK ADD  CONSTRAINT [StkItem_CfgLawyer_FK1] FOREIGN KEY([CfgLawyerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[StkItem] CHECK CONSTRAINT [StkItem_CfgLawyer_FK1]
GO
ALTER TABLE [dbo].[StkItem]  WITH CHECK ADD  CONSTRAINT [StkItem_CfgProject_FK1] FOREIGN KEY([CfgProjectId])
REFERENCES [dbo].[CfgProject] ([PKey])
GO
ALTER TABLE [dbo].[StkItem] CHECK CONSTRAINT [StkItem_CfgProject_FK1]
GO
ALTER TABLE [dbo].[StkItem]  WITH CHECK ADD  CONSTRAINT [StkItem_CfgTier_FK1] FOREIGN KEY([CfgSupplierId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[StkItem] CHECK CONSTRAINT [StkItem_CfgTier_FK1]
GO
ALTER TABLE [dbo].[StkItem]  WITH CHECK ADD  CONSTRAINT [StkItem_CfgTranche_FK1] FOREIGN KEY([StkHierarchyId])
REFERENCES [dbo].[CfgTranche] ([PKey])
GO
ALTER TABLE [dbo].[StkItem] CHECK CONSTRAINT [StkItem_CfgTranche_FK1]
GO
ALTER TABLE [dbo].[StkItem]  WITH CHECK ADD  CONSTRAINT [StkItem_LndLand_FK1] FOREIGN KEY([LndLandId])
REFERENCES [dbo].[LndLand] ([PKey])
GO
ALTER TABLE [dbo].[StkItem] CHECK CONSTRAINT [StkItem_LndLand_FK1]
GO
ALTER TABLE [dbo].[StkItem]  WITH CHECK ADD  CONSTRAINT [StkItem_StkBuilding_FK1] FOREIGN KEY([StkBuildingId])
REFERENCES [dbo].[StkBuilding] ([PKey])
GO
ALTER TABLE [dbo].[StkItem] CHECK CONSTRAINT [StkItem_StkBuilding_FK1]
GO
ALTER TABLE [dbo].[StkItem]  WITH CHECK ADD  CONSTRAINT [StkItem_StkCostCategory_FK1] FOREIGN KEY([StkCostCategoryId])
REFERENCES [dbo].[StkCostCategory] ([PKey])
GO
ALTER TABLE [dbo].[StkItem] CHECK CONSTRAINT [StkItem_StkCostCategory_FK1]
GO
ALTER TABLE [dbo].[StkItem]  WITH CHECK ADD  CONSTRAINT [StkItem_StkFacade_FK1] FOREIGN KEY([StkFacadeId])
REFERENCES [dbo].[StkFacade] ([PKey])
GO
ALTER TABLE [dbo].[StkItem] CHECK CONSTRAINT [StkItem_StkFacade_FK1]
GO
ALTER TABLE [dbo].[StkItem]  WITH CHECK ADD  CONSTRAINT [StkItem_StkFloor_FK1] FOREIGN KEY([StkFloorId])
REFERENCES [dbo].[StkFloor] ([PKey])
GO
ALTER TABLE [dbo].[StkItem] CHECK CONSTRAINT [StkItem_StkFloor_FK1]
GO
ALTER TABLE [dbo].[StkItem]  WITH CHECK ADD  CONSTRAINT [StkItem_StkItem_FK1] FOREIGN KEY([ParentId])
REFERENCES [dbo].[StkItem] ([PKey])
GO
ALTER TABLE [dbo].[StkItem] CHECK CONSTRAINT [StkItem_StkItem_FK1]
GO
ALTER TABLE [dbo].[StkItem]  WITH CHECK ADD  CONSTRAINT [StkItem_StkItemCategory_FK1] FOREIGN KEY([StkItemCategoryId])
REFERENCES [dbo].[StkItemCategory] ([PKey])
GO
ALTER TABLE [dbo].[StkItem] CHECK CONSTRAINT [StkItem_StkItemCategory_FK1]
GO
ALTER TABLE [dbo].[StkItem]  WITH CHECK ADD  CONSTRAINT [StkItem_StkItemType_FK1] FOREIGN KEY([StkItemTypeId])
REFERENCES [dbo].[StkItemType] ([PKey])
GO
ALTER TABLE [dbo].[StkItem] CHECK CONSTRAINT [StkItem_StkItemType_FK1]
GO
ALTER TABLE [dbo].[StkItem]  WITH CHECK ADD  CONSTRAINT [StkItem_StkKind_FK1] FOREIGN KEY([StkKindId])
REFERENCES [dbo].[StkKind] ([PKey])
GO
ALTER TABLE [dbo].[StkItem] CHECK CONSTRAINT [StkItem_StkKind_FK1]
GO
ALTER TABLE [dbo].[StkItem]  WITH CHECK ADD  CONSTRAINT [StkItem_StkOrientation_FK1] FOREIGN KEY([StkOrientationId])
REFERENCES [dbo].[StkOrientation] ([PKey])
GO
ALTER TABLE [dbo].[StkItem] CHECK CONSTRAINT [StkItem_StkOrientation_FK1]
GO
ALTER TABLE [dbo].[StkItem]  WITH CHECK ADD  CONSTRAINT [StkItem_StkPresentationType_FK] FOREIGN KEY([StkPresentationTypeId])
REFERENCES [dbo].[StkPresentationType] ([PKey])
GO
ALTER TABLE [dbo].[StkItem] CHECK CONSTRAINT [StkItem_StkPresentationType_FK]
GO
ALTER TABLE [dbo].[StkItem]  WITH CHECK ADD  CONSTRAINT [StkItem_StkVocation_FK1] FOREIGN KEY([StkVocationId])
REFERENCES [dbo].[StkVocation] ([PKey])
GO
ALTER TABLE [dbo].[StkItem] CHECK CONSTRAINT [StkItem_StkVocation_FK1]
GO
ALTER TABLE [dbo].[StkItemCategory]  WITH CHECK ADD  CONSTRAINT [StkItemCategory__StlCategory_FK1] FOREIGN KEY([StlCategoryId])
REFERENCES [dbo].[StlCategory] ([PKey])
GO
ALTER TABLE [dbo].[StkItemCategory] CHECK CONSTRAINT [StkItemCategory__StlCategory_FK1]
GO
ALTER TABLE [dbo].[StkItemCategory]  WITH CHECK ADD  CONSTRAINT [StkItemCategory_StkKind_FK1] FOREIGN KEY([StkKindId])
REFERENCES [dbo].[StkKind] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[StkItemCategory] CHECK CONSTRAINT [StkItemCategory_StkKind_FK1]
GO
ALTER TABLE [dbo].[StkItemType]  WITH CHECK ADD  CONSTRAINT [StkItemType__StlCategory_FK1] FOREIGN KEY([StlCategoryId])
REFERENCES [dbo].[StlCategory] ([PKey])
GO
ALTER TABLE [dbo].[StkItemType] CHECK CONSTRAINT [StkItemType__StlCategory_FK1]
GO
ALTER TABLE [dbo].[StkItemType]  WITH CHECK ADD  CONSTRAINT [StkItemType_StkKind_FK1] FOREIGN KEY([StkKindId])
REFERENCES [dbo].[StkKind] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[StkItemType] CHECK CONSTRAINT [StkItemType_StkKind_FK1]
GO
ALTER TABLE [dbo].[StkLevelingLine]  WITH CHECK ADD  CONSTRAINT [StkLeveling_StkLeveling_FK1] FOREIGN KEY([StkLevelId])
REFERENCES [dbo].[StkLeveling] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[StkLevelingLine] CHECK CONSTRAINT [StkLeveling_StkLeveling_FK1]
GO
ALTER TABLE [dbo].[StkPresentationType]  WITH CHECK ADD  CONSTRAINT [StkPresentationType_CfgTranche_FK1] FOREIGN KEY([StkHierarchyId])
REFERENCES [dbo].[CfgTranche] ([PKey])
GO
ALTER TABLE [dbo].[StkPresentationType] CHECK CONSTRAINT [StkPresentationType_CfgTranche_FK1]
GO
ALTER TABLE [dbo].[StlAccount]  WITH CHECK ADD  CONSTRAINT [CfgTier_StlAccount_FK1] FOREIGN KEY([CfgBankId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[StlAccount] CHECK CONSTRAINT [CfgTier_StlAccount_FK1]
GO
ALTER TABLE [dbo].[StlAccount]  WITH CHECK ADD  CONSTRAINT [StlAccount__StlCurrency_FK1] FOREIGN KEY([StlCurrencyId])
REFERENCES [dbo].[StlCurrency] ([PKey])
GO
ALTER TABLE [dbo].[StlAccount] CHECK CONSTRAINT [StlAccount__StlCurrency_FK1]
GO
ALTER TABLE [dbo].[StlAccount]  WITH CHECK ADD  CONSTRAINT [StlAccount_CfgCompany_FK] FOREIGN KEY([CfgCompanyId])
REFERENCES [dbo].[CfgCompany] ([PKey])
GO
ALTER TABLE [dbo].[StlAccount] CHECK CONSTRAINT [StlAccount_CfgCompany_FK]
GO
ALTER TABLE [dbo].[StlAccount]  WITH CHECK ADD  CONSTRAINT [StlSettlementKind_StlAccount_FK1] FOREIGN KEY([StlSettlementKindId])
REFERENCES [dbo].[StlSettlementKind] ([PKey])
GO
ALTER TABLE [dbo].[StlAccount] CHECK CONSTRAINT [StlSettlementKind_StlAccount_FK1]
GO
ALTER TABLE [dbo].[StlAccountPosition]  WITH CHECK ADD  CONSTRAINT [StlAccount_StlAccountPosition_FK1] FOREIGN KEY([StlAccountId])
REFERENCES [dbo].[StlAccount] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[StlAccountPosition] CHECK CONSTRAINT [StlAccount_StlAccountPosition_FK1]
GO
ALTER TABLE [dbo].[StlAccountPosition]  WITH CHECK ADD  CONSTRAINT [StlAccountPosition_StrEntity_FK] FOREIGN KEY([StrEntityId])
REFERENCES [dbo].[StrEntity] ([PKey])
GO
ALTER TABLE [dbo].[StlAccountPosition] CHECK CONSTRAINT [StlAccountPosition_StrEntity_FK]
GO
ALTER TABLE [dbo].[StlAcountDeposit]  WITH CHECK ADD  CONSTRAINT [StlAcountDeposit_GrhEmployee_FK1] FOREIGN KEY([GrhEmployeeId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[StlAcountDeposit] CHECK CONSTRAINT [StlAcountDeposit_GrhEmployee_FK1]
GO
ALTER TABLE [dbo].[StlAcountDeposit]  WITH CHECK ADD  CONSTRAINT [StlAcountDeposit_GrhEmployeeClosing_FK1] FOREIGN KEY([GrhEmployeeClosingId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[StlAcountDeposit] CHECK CONSTRAINT [StlAcountDeposit_GrhEmployeeClosing_FK1]
GO
ALTER TABLE [dbo].[StlAcountDeposit]  WITH CHECK ADD  CONSTRAINT [StlAcountDeposit_StlAccountTo_FK1] FOREIGN KEY([StlAccountToId])
REFERENCES [dbo].[StlAccount] ([PKey])
GO
ALTER TABLE [dbo].[StlAcountDeposit] CHECK CONSTRAINT [StlAcountDeposit_StlAccountTo_FK1]
GO
ALTER TABLE [dbo].[StlAcountDeposit]  WITH CHECK ADD  CONSTRAINT [StlAcountDeposit_StlDeposit_FK1] FOREIGN KEY([StlDepositId])
REFERENCES [dbo].[StlDeposit] ([PKey])
GO
ALTER TABLE [dbo].[StlAcountDeposit] CHECK CONSTRAINT [StlAcountDeposit_StlDeposit_FK1]
GO
ALTER TABLE [dbo].[StlAcountDeposit]  WITH CHECK ADD  CONSTRAINT [StlAcountDeposit_StlDepositTo_FK1] FOREIGN KEY([StlDepositToId])
REFERENCES [dbo].[StlDeposit] ([PKey])
GO
ALTER TABLE [dbo].[StlAcountDeposit] CHECK CONSTRAINT [StlAcountDeposit_StlDepositTo_FK1]
GO
ALTER TABLE [dbo].[StlAcountDeposit]  WITH CHECK ADD  CONSTRAINT [StlAcountDeposit_StrEntity_FK1] FOREIGN KEY([StrEntityId])
REFERENCES [dbo].[StrEntity] ([PKey])
GO
ALTER TABLE [dbo].[StlAcountDeposit] CHECK CONSTRAINT [StlAcountDeposit_StrEntity_FK1]
GO
ALTER TABLE [dbo].[StlCategory]  WITH CHECK ADD  CONSTRAINT [StlCategory_ActAccount_FK1] FOREIGN KEY([ActAccountId])
REFERENCES [dbo].[ActAccount] ([PKey])
GO
ALTER TABLE [dbo].[StlCategory] CHECK CONSTRAINT [StlCategory_ActAccount_FK1]
GO
ALTER TABLE [dbo].[StlCategory]  WITH CHECK ADD  CONSTRAINT [StlCategory_StlAccount_FK1] FOREIGN KEY([StlAccountId])
REFERENCES [dbo].[StlAccount] ([PKey])
GO
ALTER TABLE [dbo].[StlCategory] CHECK CONSTRAINT [StlCategory_StlAccount_FK1]
GO
ALTER TABLE [dbo].[StlDeposit]  WITH CHECK ADD  CONSTRAINT [StlDeposit__CfgCompany_FK1] FOREIGN KEY([CfgCompanyId])
REFERENCES [dbo].[CfgCompany] ([PKey])
GO
ALTER TABLE [dbo].[StlDeposit] CHECK CONSTRAINT [StlDeposit__CfgCompany_FK1]
GO
ALTER TABLE [dbo].[StlDocument]  WITH CHECK ADD  CONSTRAINT [StlDocument__AdmModule_FK1] FOREIGN KEY([AdmModuleId])
REFERENCES [dbo].[AdmModule] ([PKey])
GO
ALTER TABLE [dbo].[StlDocument] CHECK CONSTRAINT [StlDocument__AdmModule_FK1]
GO
ALTER TABLE [dbo].[StlDocument]  WITH CHECK ADD  CONSTRAINT [StlDocument__BuyFolder_FK1] FOREIGN KEY([BuyFolderId])
REFERENCES [dbo].[BuyFolder] ([Pkey])
GO
ALTER TABLE [dbo].[StlDocument] CHECK CONSTRAINT [StlDocument__BuyFolder_FK1]
GO
ALTER TABLE [dbo].[StlDocument]  WITH CHECK ADD  CONSTRAINT [StlDocument__CfgCompany_FK1] FOREIGN KEY([CfgCompanyId])
REFERENCES [dbo].[CfgCompany] ([PKey])
GO
ALTER TABLE [dbo].[StlDocument] CHECK CONSTRAINT [StlDocument__CfgCompany_FK1]
GO
ALTER TABLE [dbo].[StlDocument]  WITH CHECK ADD  CONSTRAINT [StlDocument__CfgTier_FK1] FOREIGN KEY([CfgTierId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[StlDocument] CHECK CONSTRAINT [StlDocument__CfgTier_FK1]
GO
ALTER TABLE [dbo].[StlDocument]  WITH CHECK ADD  CONSTRAINT [StlDocument__CfgTier_FK2] FOREIGN KEY([CfgDebtCollectorId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[StlDocument] CHECK CONSTRAINT [StlDocument__CfgTier_FK2]
GO
ALTER TABLE [dbo].[StlDocument]  WITH CHECK ADD  CONSTRAINT [StlDocument__StlPenaltyDocument_FK1] FOREIGN KEY([StlPenaltyDocumentId])
REFERENCES [dbo].[StlDocument] ([PKey])
GO
ALTER TABLE [dbo].[StlDocument] CHECK CONSTRAINT [StlDocument__StlPenaltyDocument_FK1]
GO
ALTER TABLE [dbo].[StlDocument]  WITH CHECK ADD  CONSTRAINT [StlDocument__StrEntity_FK1] FOREIGN KEY([StrEntityId])
REFERENCES [dbo].[StrEntity] ([PKey])
GO
ALTER TABLE [dbo].[StlDocument] CHECK CONSTRAINT [StlDocument__StrEntity_FK1]
GO
ALTER TABLE [dbo].[StlDocument]  WITH CHECK ADD  CONSTRAINT [StlDocument_StlCurrency_FK1] FOREIGN KEY([StlCurrencyId])
REFERENCES [dbo].[StlCurrency] ([PKey])
GO
ALTER TABLE [dbo].[StlDocument] CHECK CONSTRAINT [StlDocument_StlCurrency_FK1]
GO
ALTER TABLE [dbo].[StlDocument]  WITH CHECK ADD  CONSTRAINT [StlDocument_StlRecoveryFolder_FK1] FOREIGN KEY([StlRecoveryFolderId])
REFERENCES [dbo].[StlRecoveryFolder] ([PKey])
GO
ALTER TABLE [dbo].[StlDocument] CHECK CONSTRAINT [StlDocument_StlRecoveryFolder_FK1]
GO
ALTER TABLE [dbo].[StlInternalTransfer]  WITH CHECK ADD  CONSTRAINT [StlInternalTransfer__CfgTierEmission_FK1] FOREIGN KEY([CfgTierEmissionId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[StlInternalTransfer] CHECK CONSTRAINT [StlInternalTransfer__CfgTierEmission_FK1]
GO
ALTER TABLE [dbo].[StlInternalTransfer]  WITH CHECK ADD  CONSTRAINT [StlInternalTransfer__CfgTierReception_FK1] FOREIGN KEY([CfgTierReceptionId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[StlInternalTransfer] CHECK CONSTRAINT [StlInternalTransfer__CfgTierReception_FK1]
GO
ALTER TABLE [dbo].[StlInternalTransfer]  WITH CHECK ADD  CONSTRAINT [StlInternalTransfer__CfgTierTransmission_FK1] FOREIGN KEY([CfgTierTransmissionId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[StlInternalTransfer] CHECK CONSTRAINT [StlInternalTransfer__CfgTierTransmission_FK1]
GO
ALTER TABLE [dbo].[StlInternalTransfer]  WITH CHECK ADD  CONSTRAINT [StlInternalTransfer_CfgCompany_FK1] FOREIGN KEY([CfgCompanyId])
REFERENCES [dbo].[CfgCompany] ([PKey])
GO
ALTER TABLE [dbo].[StlInternalTransfer] CHECK CONSTRAINT [StlInternalTransfer_CfgCompany_FK1]
GO
ALTER TABLE [dbo].[StlInternalTransfer]  WITH CHECK ADD  CONSTRAINT [StlInternalTransfer_CfgCompanyTo_FK1] FOREIGN KEY([CfgCompanyToId])
REFERENCES [dbo].[CfgCompany] ([PKey])
GO
ALTER TABLE [dbo].[StlInternalTransfer] CHECK CONSTRAINT [StlInternalTransfer_CfgCompanyTo_FK1]
GO
ALTER TABLE [dbo].[StlInternalTransfer]  WITH CHECK ADD  CONSTRAINT [StlInternalTransfer_StlAccountId_FK1] FOREIGN KEY([StlAccountId])
REFERENCES [dbo].[StlAccount] ([PKey])
GO
ALTER TABLE [dbo].[StlInternalTransfer] CHECK CONSTRAINT [StlInternalTransfer_StlAccountId_FK1]
GO
ALTER TABLE [dbo].[StlInternalTransfer]  WITH CHECK ADD  CONSTRAINT [StlInternalTransfer_StlAccountToId_FK1] FOREIGN KEY([StlAccountToId])
REFERENCES [dbo].[StlAccount] ([PKey])
GO
ALTER TABLE [dbo].[StlInternalTransfer] CHECK CONSTRAINT [StlInternalTransfer_StlAccountToId_FK1]
GO
ALTER TABLE [dbo].[StlInternalTransfer]  WITH CHECK ADD  CONSTRAINT [StlInternalTransfer_StlSettlementTypeId_FK1] FOREIGN KEY([StlSettlementTypeId])
REFERENCES [dbo].[StlSettlementType] ([PKey])
GO
ALTER TABLE [dbo].[StlInternalTransfer] CHECK CONSTRAINT [StlInternalTransfer_StlSettlementTypeId_FK1]
GO
ALTER TABLE [dbo].[StlInternalTransfer]  WITH CHECK ADD  CONSTRAINT [StlInternalTransfer_StlSettlementTypeToId_FK1] FOREIGN KEY([StlSettlementTypeToId])
REFERENCES [dbo].[StlSettlementType] ([PKey])
GO
ALTER TABLE [dbo].[StlInternalTransfer] CHECK CONSTRAINT [StlInternalTransfer_StlSettlementTypeToId_FK1]
GO
ALTER TABLE [dbo].[StlInternalTransfer]  WITH CHECK ADD  CONSTRAINT [StlInternalTransfer_StrEntityId_FK1] FOREIGN KEY([StrEntityId])
REFERENCES [dbo].[StrEntity] ([PKey])
GO
ALTER TABLE [dbo].[StlInternalTransfer] CHECK CONSTRAINT [StlInternalTransfer_StrEntityId_FK1]
GO
ALTER TABLE [dbo].[StlInternalTransfer]  WITH CHECK ADD  CONSTRAINT [StlInternalTransfer_StrStatusId_FK1] FOREIGN KEY([StrStatusId])
REFERENCES [dbo].[StrStatus] ([PKey])
GO
ALTER TABLE [dbo].[StlInternalTransfer] CHECK CONSTRAINT [StlInternalTransfer_StrStatusId_FK1]
GO
ALTER TABLE [dbo].[StlQuote]  WITH CHECK ADD  CONSTRAINT [StlQuote__CfgProspect_FK1] FOREIGN KEY([CfgProspectId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[StlQuote] CHECK CONSTRAINT [StlQuote__CfgProspect_FK1]
GO
ALTER TABLE [dbo].[StlQuote]  WITH CHECK ADD  CONSTRAINT [StlQuote__ComProspection_FK1] FOREIGN KEY([ComProspectionId])
REFERENCES [dbo].[ComProspection] ([PKey])
GO
ALTER TABLE [dbo].[StlQuote] CHECK CONSTRAINT [StlQuote__ComProspection_FK1]
GO
ALTER TABLE [dbo].[StlQuote]  WITH CHECK ADD  CONSTRAINT [StlQuote__ComSettlementCategory_FK1] FOREIGN KEY([ComSettlementCategoryId])
REFERENCES [dbo].[ComSettlementCategory] ([PKey])
GO
ALTER TABLE [dbo].[StlQuote] CHECK CONSTRAINT [StlQuote__ComSettlementCategory_FK1]
GO
ALTER TABLE [dbo].[StlQuote]  WITH CHECK ADD  CONSTRAINT [StlQuote__RntPeriodicity_FK1] FOREIGN KEY([RntPeriodicityId])
REFERENCES [dbo].[RntPeriodicity] ([PKey])
GO
ALTER TABLE [dbo].[StlQuote] CHECK CONSTRAINT [StlQuote__RntPeriodicity_FK1]
GO
ALTER TABLE [dbo].[StlQuote]  WITH CHECK ADD  CONSTRAINT [StlQuote__SCfgTranche_FK1] FOREIGN KEY([CfgTrancheId])
REFERENCES [dbo].[CfgTranche] ([PKey])
GO
ALTER TABLE [dbo].[StlQuote] CHECK CONSTRAINT [StlQuote__SCfgTranche_FK1]
GO
ALTER TABLE [dbo].[StlQuote]  WITH CHECK ADD  CONSTRAINT [StlQuote__StkItem_FK1] FOREIGN KEY([StkItemId])
REFERENCES [dbo].[StkItem] ([PKey])
GO
ALTER TABLE [dbo].[StlQuote] CHECK CONSTRAINT [StlQuote__StkItem_FK1]
GO
ALTER TABLE [dbo].[StlQuote]  WITH CHECK ADD  CONSTRAINT [StlQuote__StrEntity_FK1] FOREIGN KEY([StrEntityId])
REFERENCES [dbo].[StrEntity] ([PKey])
GO
ALTER TABLE [dbo].[StlQuote] CHECK CONSTRAINT [StlQuote__StrEntity_FK1]
GO
ALTER TABLE [dbo].[StlQuote]  WITH CHECK ADD  CONSTRAINT [StlQuote_CfgCommercial_FK1] FOREIGN KEY([CfgCommercialId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[StlQuote] CHECK CONSTRAINT [StlQuote_CfgCommercial_FK1]
GO
ALTER TABLE [dbo].[StlQuote]  WITH CHECK ADD  CONSTRAINT [StlQuote_ComPaymentMethod_FK1] FOREIGN KEY([ComPaymentMethodId])
REFERENCES [dbo].[ComPaymentMethod] ([PKey])
GO
ALTER TABLE [dbo].[StlQuote] CHECK CONSTRAINT [StlQuote_ComPaymentMethod_FK1]
GO
ALTER TABLE [dbo].[StlQuote]  WITH CHECK ADD  CONSTRAINT [StlQuote_RntReconductionType_FK2] FOREIGN KEY([RntReconductionTypeId])
REFERENCES [dbo].[RntReconductionType] ([PKey])
GO
ALTER TABLE [dbo].[StlQuote] CHECK CONSTRAINT [StlQuote_RntReconductionType_FK2]
GO
ALTER TABLE [dbo].[StlQuote]  WITH CHECK ADD  CONSTRAINT [StlQuote_StkVocation_FK2] FOREIGN KEY([StkVocationId])
REFERENCES [dbo].[StkVocation] ([PKey])
GO
ALTER TABLE [dbo].[StlQuote] CHECK CONSTRAINT [StlQuote_StkVocation_FK2]
GO
ALTER TABLE [dbo].[StlQuoteItem]  WITH CHECK ADD  CONSTRAINT [StlQuoteItem_StkBuilding_FK1] FOREIGN KEY([StkBuildingId])
REFERENCES [dbo].[StkBuilding] ([PKey])
GO
ALTER TABLE [dbo].[StlQuoteItem] CHECK CONSTRAINT [StlQuoteItem_StkBuilding_FK1]
GO
ALTER TABLE [dbo].[StlQuoteItem]  WITH CHECK ADD  CONSTRAINT [StlQuoteItem_StkCostCategory_FK1] FOREIGN KEY([StkCostCategoryId])
REFERENCES [dbo].[StkCostCategory] ([PKey])
GO
ALTER TABLE [dbo].[StlQuoteItem] CHECK CONSTRAINT [StlQuoteItem_StkCostCategory_FK1]
GO
ALTER TABLE [dbo].[StlQuoteItem]  WITH CHECK ADD  CONSTRAINT [StlQuoteItem_StkFloor_FK1] FOREIGN KEY([StkFloorId])
REFERENCES [dbo].[StkFloor] ([PKey])
GO
ALTER TABLE [dbo].[StlQuoteItem] CHECK CONSTRAINT [StlQuoteItem_StkFloor_FK1]
GO
ALTER TABLE [dbo].[StlQuoteItem]  WITH CHECK ADD  CONSTRAINT [StlQuoteItem_StkItem_FK1] FOREIGN KEY([StkItemId])
REFERENCES [dbo].[StkItem] ([PKey])
GO
ALTER TABLE [dbo].[StlQuoteItem] CHECK CONSTRAINT [StlQuoteItem_StkItem_FK1]
GO
ALTER TABLE [dbo].[StlQuoteItem]  WITH CHECK ADD  CONSTRAINT [StlQuoteItem_StkItemCategory_FK1] FOREIGN KEY([StkItemCategoryId])
REFERENCES [dbo].[StkItemCategory] ([PKey])
GO
ALTER TABLE [dbo].[StlQuoteItem] CHECK CONSTRAINT [StlQuoteItem_StkItemCategory_FK1]
GO
ALTER TABLE [dbo].[StlQuoteItem]  WITH CHECK ADD  CONSTRAINT [StlQuoteItem_StkItemType_FK1] FOREIGN KEY([StkItemTypeId])
REFERENCES [dbo].[StkItemType] ([PKey])
GO
ALTER TABLE [dbo].[StlQuoteItem] CHECK CONSTRAINT [StlQuoteItem_StkItemType_FK1]
GO
ALTER TABLE [dbo].[StlQuoteItem]  WITH CHECK ADD  CONSTRAINT [StlQuoteItem_StkOrientation_FK1] FOREIGN KEY([StkOrientationId])
REFERENCES [dbo].[StkOrientation] ([PKey])
GO
ALTER TABLE [dbo].[StlQuoteItem] CHECK CONSTRAINT [StlQuoteItem_StkOrientation_FK1]
GO
ALTER TABLE [dbo].[StlQuoteItem]  WITH CHECK ADD  CONSTRAINT [StlQuoteItem_StlQuote_FK1] FOREIGN KEY([StlQuoteId])
REFERENCES [dbo].[StlQuote] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[StlQuoteItem] CHECK CONSTRAINT [StlQuoteItem_StlQuote_FK1]
GO
ALTER TABLE [dbo].[StlRecoveryFolder]  WITH CHECK ADD  CONSTRAINT [StlRecoveryFolder_CfgCompany_FK1] FOREIGN KEY([CfgCompanyId])
REFERENCES [dbo].[CfgCompany] ([PKey])
GO
ALTER TABLE [dbo].[StlRecoveryFolder] CHECK CONSTRAINT [StlRecoveryFolder_CfgCompany_FK1]
GO
ALTER TABLE [dbo].[StlRecoveryFolder]  WITH CHECK ADD  CONSTRAINT [StlRecoveryFolder_CfgCustomer_FK1] FOREIGN KEY([CfgCustomerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[StlRecoveryFolder] CHECK CONSTRAINT [StlRecoveryFolder_CfgCustomer_FK1]
GO
ALTER TABLE [dbo].[StlRecoveryFolder]  WITH CHECK ADD  CONSTRAINT [StlRecoveryFolder_CfgDebtCollector_FK1] FOREIGN KEY([CfgDebtCollectorId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[StlRecoveryFolder] CHECK CONSTRAINT [StlRecoveryFolder_CfgDebtCollector_FK1]
GO
ALTER TABLE [dbo].[StlRecoveryFolder]  WITH CHECK ADD  CONSTRAINT [StlRecoveryFolder_CfgDebtManager_FK1] FOREIGN KEY([CfgDebtManagerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[StlRecoveryFolder] CHECK CONSTRAINT [StlRecoveryFolder_CfgDebtManager_FK1]
GO
ALTER TABLE [dbo].[StlRecoveryFolder]  WITH CHECK ADD  CONSTRAINT [StlRecoveryFolder_ComFolder_FK1] FOREIGN KEY([ComFolderId])
REFERENCES [dbo].[ComFolder] ([PKey])
GO
ALTER TABLE [dbo].[StlRecoveryFolder] CHECK CONSTRAINT [StlRecoveryFolder_ComFolder_FK1]
GO
ALTER TABLE [dbo].[StlRecoveryFolder]  WITH CHECK ADD  CONSTRAINT [StlRecoveryFolder_RntFolder_FK1] FOREIGN KEY([RntFolderId])
REFERENCES [dbo].[RntFolder] ([PKey])
GO
ALTER TABLE [dbo].[StlRecoveryFolder] CHECK CONSTRAINT [StlRecoveryFolder_RntFolder_FK1]
GO
ALTER TABLE [dbo].[StlRecoveryFolder]  WITH CHECK ADD  CONSTRAINT [StlRecoveryFolder_StrEntity_FK1] FOREIGN KEY([StrEntityId])
REFERENCES [dbo].[StrEntity] ([PKey])
GO
ALTER TABLE [dbo].[StlRecoveryFolder] CHECK CONSTRAINT [StlRecoveryFolder_StrEntity_FK1]
GO
ALTER TABLE [dbo].[StlRecoveryFolder]  WITH CHECK ADD  CONSTRAINT [StlRecoveryFolder_StrStatus_FK1] FOREIGN KEY([StrStatusId])
REFERENCES [dbo].[StrStatus] ([PKey])
GO
ALTER TABLE [dbo].[StlRecoveryFolder] CHECK CONSTRAINT [StlRecoveryFolder_StrStatus_FK1]
GO
ALTER TABLE [dbo].[StlSettlement]  WITH CHECK ADD  CONSTRAINT [CfgTier_StlSettlement_FK1] FOREIGN KEY([CfgBankId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[StlSettlement] CHECK CONSTRAINT [CfgTier_StlSettlement_FK1]
GO
ALTER TABLE [dbo].[StlSettlement]  WITH CHECK ADD  CONSTRAINT [CfgTier_StlSettlement_FK2] FOREIGN KEY([TierId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[StlSettlement] CHECK CONSTRAINT [CfgTier_StlSettlement_FK2]
GO
ALTER TABLE [dbo].[StlSettlement]  WITH CHECK ADD  CONSTRAINT [StlAccount_StlSettlement_FK1] FOREIGN KEY([StlAccountId])
REFERENCES [dbo].[StlAccount] ([PKey])
GO
ALTER TABLE [dbo].[StlSettlement] CHECK CONSTRAINT [StlAccount_StlSettlement_FK1]
GO
ALTER TABLE [dbo].[StlSettlement]  WITH CHECK ADD  CONSTRAINT [StlCategory_StlSettlement_FK1] FOREIGN KEY([StlCategoryId])
REFERENCES [dbo].[StlCategory] ([PKey])
GO
ALTER TABLE [dbo].[StlSettlement] CHECK CONSTRAINT [StlCategory_StlSettlement_FK1]
GO
ALTER TABLE [dbo].[StlSettlement]  WITH CHECK ADD  CONSTRAINT [StlSettlement__BuyFolder_FK1] FOREIGN KEY([BuyFolderId])
REFERENCES [dbo].[BuyFolder] ([Pkey])
GO
ALTER TABLE [dbo].[StlSettlement] CHECK CONSTRAINT [StlSettlement__BuyFolder_FK1]
GO
ALTER TABLE [dbo].[StlSettlement]  WITH CHECK ADD  CONSTRAINT [StlSettlement__BuyLoanSchedule_FK1] FOREIGN KEY([BuyLoanScheduleId])
REFERENCES [dbo].[BuyLoanSchedule] ([PKey])
GO
ALTER TABLE [dbo].[StlSettlement] CHECK CONSTRAINT [StlSettlement__BuyLoanSchedule_FK1]
GO
ALTER TABLE [dbo].[StlSettlement]  WITH CHECK ADD  CONSTRAINT [StlSettlement__BuySubLoan_FK1] FOREIGN KEY([BuySubLoanId])
REFERENCES [dbo].[BuySubLoan] ([PKey])
GO
ALTER TABLE [dbo].[StlSettlement] CHECK CONSTRAINT [StlSettlement__BuySubLoan_FK1]
GO
ALTER TABLE [dbo].[StlSettlement]  WITH CHECK ADD  CONSTRAINT [StlSettlement__CfgTierOther_FK1] FOREIGN KEY([CfgTierOtherId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[StlSettlement] CHECK CONSTRAINT [StlSettlement__CfgTierOther_FK1]
GO
ALTER TABLE [dbo].[StlSettlement]  WITH CHECK ADD  CONSTRAINT [StlSettlement__StlCurrency_FK1] FOREIGN KEY([StlCurrencyId])
REFERENCES [dbo].[StlCurrency] ([PKey])
GO
ALTER TABLE [dbo].[StlSettlement] CHECK CONSTRAINT [StlSettlement__StlCurrency_FK1]
GO
ALTER TABLE [dbo].[StlSettlement]  WITH CHECK ADD  CONSTRAINT [StlSettlement__StlCurrencyAffectation_FK1] FOREIGN KEY([StlCurrencyAffectationId])
REFERENCES [dbo].[StlCurrency] ([PKey])
GO
ALTER TABLE [dbo].[StlSettlement] CHECK CONSTRAINT [StlSettlement__StlCurrencyAffectation_FK1]
GO
ALTER TABLE [dbo].[StlSettlement]  WITH CHECK ADD  CONSTRAINT [StlSettlement__StlDeposit_FK1] FOREIGN KEY([StlDepositId])
REFERENCES [dbo].[StlDeposit] ([PKey])
GO
ALTER TABLE [dbo].[StlSettlement] CHECK CONSTRAINT [StlSettlement__StlDeposit_FK1]
GO
ALTER TABLE [dbo].[StlSettlement]  WITH CHECK ADD  CONSTRAINT [StlSettlement__StlInternalTransfer_FK1] FOREIGN KEY([StlInternalTransferId])
REFERENCES [dbo].[StlInternalTransfer] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[StlSettlement] CHECK CONSTRAINT [StlSettlement__StlInternalTransfer_FK1]
GO
ALTER TABLE [dbo].[StlSettlement]  WITH CHECK ADD  CONSTRAINT [StlSettlement__TierPayer_FK1] FOREIGN KEY([TierPayerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[StlSettlement] CHECK CONSTRAINT [StlSettlement__TierPayer_FK1]
GO
ALTER TABLE [dbo].[StlSettlement]  WITH CHECK ADD  CONSTRAINT [StlSettlement_AdmModule_FK1] FOREIGN KEY([AdmModuleId])
REFERENCES [dbo].[AdmModule] ([PKey])
GO
ALTER TABLE [dbo].[StlSettlement] CHECK CONSTRAINT [StlSettlement_AdmModule_FK1]
GO
ALTER TABLE [dbo].[StlSettlement]  WITH CHECK ADD  CONSTRAINT [StlSettlement_BuyLoan_FK] FOREIGN KEY([BuyLoanId])
REFERENCES [dbo].[BuyLoan] ([PKey])
GO
ALTER TABLE [dbo].[StlSettlement] CHECK CONSTRAINT [StlSettlement_BuyLoan_FK]
GO
ALTER TABLE [dbo].[StlSettlement]  WITH CHECK ADD  CONSTRAINT [StlSettlement_CfgCompany_FK1] FOREIGN KEY([CfgCompanyId])
REFERENCES [dbo].[CfgCompany] ([PKey])
GO
ALTER TABLE [dbo].[StlSettlement] CHECK CONSTRAINT [StlSettlement_CfgCompany_FK1]
GO
ALTER TABLE [dbo].[StlSettlement]  WITH CHECK ADD  CONSTRAINT [StlSettlement_CfgLawyer_FK1] FOREIGN KEY([CfgLawyerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[StlSettlement] CHECK CONSTRAINT [StlSettlement_CfgLawyer_FK1]
GO
ALTER TABLE [dbo].[StlSettlement]  WITH CHECK ADD  CONSTRAINT [StlSettlement_StlAccountPosition_FK1] FOREIGN KEY([StlAccountPositionId])
REFERENCES [dbo].[StlAccountPosition] ([PKey])
GO
ALTER TABLE [dbo].[StlSettlement] CHECK CONSTRAINT [StlSettlement_StlAccountPosition_FK1]
GO
ALTER TABLE [dbo].[StlSettlement]  WITH CHECK ADD  CONSTRAINT [StlSettlement_StlAcountDepositId_FK] FOREIGN KEY([StlAcountDepositId])
REFERENCES [dbo].[StlAcountDeposit] ([PKey])
GO
ALTER TABLE [dbo].[StlSettlement] CHECK CONSTRAINT [StlSettlement_StlAcountDepositId_FK]
GO
ALTER TABLE [dbo].[StlSettlement]  WITH CHECK ADD  CONSTRAINT [StlSettlement_StlSlip_FK1] FOREIGN KEY([StlSlipId])
REFERENCES [dbo].[StlSlip] ([PKey])
GO
ALTER TABLE [dbo].[StlSettlement] CHECK CONSTRAINT [StlSettlement_StlSlip_FK1]
GO
ALTER TABLE [dbo].[StlSettlement]  WITH CHECK ADD  CONSTRAINT [StlSettlement_StrEntity_FK] FOREIGN KEY([StrEntityId])
REFERENCES [dbo].[StrEntity] ([PKey])
GO
ALTER TABLE [dbo].[StlSettlement] CHECK CONSTRAINT [StlSettlement_StrEntity_FK]
GO
ALTER TABLE [dbo].[StlSettlement]  WITH CHECK ADD  CONSTRAINT [StlSettlementType_StlSettlement_FK1] FOREIGN KEY([StlSettlementTypeId])
REFERENCES [dbo].[StlSettlementType] ([PKey])
GO
ALTER TABLE [dbo].[StlSettlement] CHECK CONSTRAINT [StlSettlementType_StlSettlement_FK1]
GO
ALTER TABLE [dbo].[StlSettlementLine]  WITH CHECK ADD  CONSTRAINT [StlSettlementLine_BuyDocument_FK1] FOREIGN KEY([BuyDocumentId])
REFERENCES [dbo].[BuyDocument] ([PKey])
GO
ALTER TABLE [dbo].[StlSettlementLine] CHECK CONSTRAINT [StlSettlementLine_BuyDocument_FK1]
GO
ALTER TABLE [dbo].[StlSettlementLine]  WITH CHECK ADD  CONSTRAINT [StlSettlementLine_ComDocument_FK1] FOREIGN KEY([ComDocumentId])
REFERENCES [dbo].[ComDocument] ([PKey])
GO
ALTER TABLE [dbo].[StlSettlementLine] CHECK CONSTRAINT [StlSettlementLine_ComDocument_FK1]
GO
ALTER TABLE [dbo].[StlSettlementLine]  WITH CHECK ADD  CONSTRAINT [StlSettlementLine_RntDocument_FK1] FOREIGN KEY([RntDocumentId])
REFERENCES [dbo].[RntDocument] ([PKey])
GO
ALTER TABLE [dbo].[StlSettlementLine] CHECK CONSTRAINT [StlSettlementLine_RntDocument_FK1]
GO
ALTER TABLE [dbo].[StlSettlementLine]  WITH CHECK ADD  CONSTRAINT [StlSettlementLine_StlDocument_FK1] FOREIGN KEY([StlDocumentId])
REFERENCES [dbo].[StlDocument] ([PKey])
GO
ALTER TABLE [dbo].[StlSettlementLine] CHECK CONSTRAINT [StlSettlementLine_StlDocument_FK1]
GO
ALTER TABLE [dbo].[StlSettlementLine]  WITH CHECK ADD  CONSTRAINT [StlSettlementLine_StlSettlement_FK1] FOREIGN KEY([StlSettlementId])
REFERENCES [dbo].[StlSettlement] ([PKey])
GO
ALTER TABLE [dbo].[StlSettlementLine] CHECK CONSTRAINT [StlSettlementLine_StlSettlement_FK1]
GO
ALTER TABLE [dbo].[StlSettlementRelation]  WITH CHECK ADD  CONSTRAINT [StlSettlementRelation__StlCategory_FK1] FOREIGN KEY([StlCategoryId])
REFERENCES [dbo].[StlCategory] ([PKey])
GO
ALTER TABLE [dbo].[StlSettlementRelation] CHECK CONSTRAINT [StlSettlementRelation__StlCategory_FK1]
GO
ALTER TABLE [dbo].[StlSettlementType]  WITH CHECK ADD  CONSTRAINT [StlSettlementType_ActAccountCredit_FK1] FOREIGN KEY([ActAccountCreditId])
REFERENCES [dbo].[ActAccount] ([PKey])
GO
ALTER TABLE [dbo].[StlSettlementType] CHECK CONSTRAINT [StlSettlementType_ActAccountCredit_FK1]
GO
ALTER TABLE [dbo].[StlSettlementType]  WITH CHECK ADD  CONSTRAINT [StlSettlementType_ActAccountDebit_FK1] FOREIGN KEY([ActAccountDebitId])
REFERENCES [dbo].[ActAccount] ([PKey])
GO
ALTER TABLE [dbo].[StlSettlementType] CHECK CONSTRAINT [StlSettlementType_ActAccountDebit_FK1]
GO
ALTER TABLE [dbo].[StlSlip]  WITH CHECK ADD  CONSTRAINT [StlSlip__CfgCompany_FK1] FOREIGN KEY([CfgCompanyId])
REFERENCES [dbo].[CfgCompany] ([PKey])
GO
ALTER TABLE [dbo].[StlSlip] CHECK CONSTRAINT [StlSlip__CfgCompany_FK1]
GO
ALTER TABLE [dbo].[StlSlip]  WITH CHECK ADD  CONSTRAINT [StlSlip__StlDeposit_FK1] FOREIGN KEY([StlDepositId])
REFERENCES [dbo].[StlDeposit] ([PKey])
GO
ALTER TABLE [dbo].[StlSlip] CHECK CONSTRAINT [StlSlip__StlDeposit_FK1]
GO
ALTER TABLE [dbo].[StlSlip]  WITH CHECK ADD  CONSTRAINT [StlSlip_CfgTierEmission_FK1] FOREIGN KEY([CfgTierEmissionId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[StlSlip] CHECK CONSTRAINT [StlSlip_CfgTierEmission_FK1]
GO
ALTER TABLE [dbo].[StlSlip]  WITH CHECK ADD  CONSTRAINT [StlSlip_CfgTierTransmission_FK1] FOREIGN KEY([CfgTierTransmissionId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[StlSlip] CHECK CONSTRAINT [StlSlip_CfgTierTransmission_FK1]
GO
ALTER TABLE [dbo].[StlSlip]  WITH CHECK ADD  CONSTRAINT [StlSlip_StlAccount_FK1] FOREIGN KEY([StlAccountId])
REFERENCES [dbo].[StlAccount] ([PKey])
GO
ALTER TABLE [dbo].[StlSlip] CHECK CONSTRAINT [StlSlip_StlAccount_FK1]
GO
ALTER TABLE [dbo].[StlSlip]  WITH CHECK ADD  CONSTRAINT [StlSlip_StrEntity_FK1] FOREIGN KEY([StrEntityId])
REFERENCES [dbo].[StrEntity] ([PKey])
GO
ALTER TABLE [dbo].[StlSlip] CHECK CONSTRAINT [StlSlip_StrEntity_FK1]
GO
ALTER TABLE [dbo].[StlSlip]  WITH CHECK ADD  CONSTRAINT [StlSlip_StrStatus_FK1] FOREIGN KEY([StrStatusId])
REFERENCES [dbo].[StrStatus] ([PKey])
GO
ALTER TABLE [dbo].[StlSlip] CHECK CONSTRAINT [StlSlip_StrStatus_FK1]
GO
ALTER TABLE [dbo].[StrEntity]  WITH CHECK ADD  CONSTRAINT [StrEntity__AdmModule_FK1] FOREIGN KEY([AdmModuleId])
REFERENCES [dbo].[AdmModule] ([PKey])
GO
ALTER TABLE [dbo].[StrEntity] CHECK CONSTRAINT [StrEntity__AdmModule_FK1]
GO
ALTER TABLE [dbo].[StrEntity]  WITH CHECK ADD  CONSTRAINT [StrEntity_ActAccount_FK1] FOREIGN KEY([ActAccountId])
REFERENCES [dbo].[ActAccount] ([PKey])
GO
ALTER TABLE [dbo].[StrEntity] CHECK CONSTRAINT [StrEntity_ActAccount_FK1]
GO
ALTER TABLE [dbo].[StrEntity]  WITH CHECK ADD  CONSTRAINT [StrEntity_StlCategory_FK1] FOREIGN KEY([StlCategoryId])
REFERENCES [dbo].[StlCategory] ([PKey])
GO
ALTER TABLE [dbo].[StrEntity] CHECK CONSTRAINT [StrEntity_StlCategory_FK1]
GO
ALTER TABLE [dbo].[StrField]  WITH CHECK ADD  CONSTRAINT [StrField__StrConfigEntity_FK1] FOREIGN KEY([StrConfigEntityId])
REFERENCES [dbo].[StrEntity] ([PKey])
GO
ALTER TABLE [dbo].[StrField] CHECK CONSTRAINT [StrField__StrConfigEntity_FK1]
GO
ALTER TABLE [dbo].[StrFolderStatusHistory]  WITH CHECK ADD  CONSTRAINT [ComFolderStatusHistory__StrFolderStatus_FK1] FOREIGN KEY([StrFolderStatusId])
REFERENCES [dbo].[StrStatus] ([PKey])
GO
ALTER TABLE [dbo].[StrFolderStatusHistory] CHECK CONSTRAINT [ComFolderStatusHistory__StrFolderStatus_FK1]
GO
ALTER TABLE [dbo].[StrFolderStatusHistory]  WITH CHECK ADD  CONSTRAINT [ComFolderStatusHistory__SynFolder_FK1] FOREIGN KEY([SynFolderId])
REFERENCES [dbo].[SynFolder] ([PKey])
GO
ALTER TABLE [dbo].[StrFolderStatusHistory] CHECK CONSTRAINT [ComFolderStatusHistory__SynFolder_FK1]
GO
ALTER TABLE [dbo].[StrFolderStatusHistory]  WITH CHECK ADD  CONSTRAINT [ComFolderStatusHistory_AdmUser_FK1] FOREIGN KEY([AdmUserId])
REFERENCES [dbo].[AdmUser] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[StrFolderStatusHistory] CHECK CONSTRAINT [ComFolderStatusHistory_AdmUser_FK1]
GO
ALTER TABLE [dbo].[StrFolderStatusHistory]  WITH CHECK ADD  CONSTRAINT [ComFolderStatusHistory_CfgTier_FK] FOREIGN KEY([CfgTierId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[StrFolderStatusHistory] CHECK CONSTRAINT [ComFolderStatusHistory_CfgTier_FK]
GO
ALTER TABLE [dbo].[StrFolderStatusHistory]  WITH CHECK ADD  CONSTRAINT [ComFolderStatusHistory_ComFolder_FK1] FOREIGN KEY([ComFolderId])
REFERENCES [dbo].[ComFolder] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[StrFolderStatusHistory] CHECK CONSTRAINT [ComFolderStatusHistory_ComFolder_FK1]
GO
ALTER TABLE [dbo].[StrFolderStatusHistory]  WITH CHECK ADD  CONSTRAINT [ComFolderStatusHistory_ComSaleWithdrawalCancellation_FK] FOREIGN KEY([ComSaleWithdrawalCancellationId])
REFERENCES [dbo].[ComSaleWithdrawalCancellation] ([PKey])
GO
ALTER TABLE [dbo].[StrFolderStatusHistory] CHECK CONSTRAINT [ComFolderStatusHistory_ComSaleWithdrawalCancellation_FK]
GO
ALTER TABLE [dbo].[StrFolderStatusHistory]  WITH CHECK ADD  CONSTRAINT [ComFolderStatusHistory_ComSaleWithdrawalClient_FK] FOREIGN KEY([ComSaleWithdrawalClientId])
REFERENCES [dbo].[ComSaleWithdrawalClient] ([PKey])
GO
ALTER TABLE [dbo].[StrFolderStatusHistory] CHECK CONSTRAINT [ComFolderStatusHistory_ComSaleWithdrawalClient_FK]
GO
ALTER TABLE [dbo].[StrFolderStatusHistory]  WITH CHECK ADD  CONSTRAINT [ComFolderStatusHistory_ComSaleWithdrawalProduct_FK] FOREIGN KEY([ComSaleWithdrawalProductId])
REFERENCES [dbo].[ComSaleWithdrawalProduct] ([PKey])
GO
ALTER TABLE [dbo].[StrFolderStatusHistory] CHECK CONSTRAINT [ComFolderStatusHistory_ComSaleWithdrawalProduct_FK]
GO
ALTER TABLE [dbo].[StrFolderStatusHistory]  WITH CHECK ADD  CONSTRAINT [ComFolderStatusHistory_RntFolder_FK] FOREIGN KEY([RntFolderId])
REFERENCES [dbo].[RntFolder] ([PKey])
GO
ALTER TABLE [dbo].[StrFolderStatusHistory] CHECK CONSTRAINT [ComFolderStatusHistory_RntFolder_FK]
GO
ALTER TABLE [dbo].[StrFolderStatusHistory]  WITH CHECK ADD  CONSTRAINT [ComFolderStatusHistory_StkItem_FK] FOREIGN KEY([StkItemId])
REFERENCES [dbo].[StkItem] ([PKey])
GO
ALTER TABLE [dbo].[StrFolderStatusHistory] CHECK CONSTRAINT [ComFolderStatusHistory_StkItem_FK]
GO
ALTER TABLE [dbo].[StrHistory]  WITH CHECK ADD  CONSTRAINT [StrHistory__StrConfigEntity_FK1] FOREIGN KEY([StrConfigEntityId])
REFERENCES [dbo].[StrEntity] ([PKey])
GO
ALTER TABLE [dbo].[StrHistory] CHECK CONSTRAINT [StrHistory__StrConfigEntity_FK1]
GO
ALTER TABLE [dbo].[StrStatus]  WITH CHECK ADD  CONSTRAINT [StrStatus__StrConfigEntity_FK1] FOREIGN KEY([StrConfigEntityId])
REFERENCES [dbo].[StrEntity] ([PKey])
GO
ALTER TABLE [dbo].[StrStatus] CHECK CONSTRAINT [StrStatus__StrConfigEntity_FK1]
GO
ALTER TABLE [dbo].[StrStatus]  WITH CHECK ADD  CONSTRAINT [StrStatus__StrFieldDate_FK1] FOREIGN KEY([StrFieldDateId])
REFERENCES [dbo].[StrField] ([PKey])
GO
ALTER TABLE [dbo].[StrStatus] CHECK CONSTRAINT [StrStatus__StrFieldDate_FK1]
GO
ALTER TABLE [dbo].[StrStatus]  WITH CHECK ADD  CONSTRAINT [StrStatus__StrStatusType_FK1] FOREIGN KEY([StrStatusTypeId])
REFERENCES [dbo].[StrStatusType] ([PKey])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[StrStatus] CHECK CONSTRAINT [StrStatus__StrStatusType_FK1]
GO
ALTER TABLE [dbo].[SynFolder]  WITH CHECK ADD  CONSTRAINT [SynFolder__CfgCommercial_FK1] FOREIGN KEY([CfgCommercialId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[SynFolder] CHECK CONSTRAINT [SynFolder__CfgCommercial_FK1]
GO
ALTER TABLE [dbo].[SynFolder]  WITH CHECK ADD  CONSTRAINT [SynFolder__CfgCustomer_FK1] FOREIGN KEY([CfgCustomerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[SynFolder] CHECK CONSTRAINT [SynFolder__CfgCustomer_FK1]
GO
ALTER TABLE [dbo].[SynFolder]  WITH CHECK ADD  CONSTRAINT [SynFolder__CfgLawyer_FK1] FOREIGN KEY([CfgLawyerId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[SynFolder] CHECK CONSTRAINT [SynFolder__CfgLawyer_FK1]
GO
ALTER TABLE [dbo].[SynFolder]  WITH CHECK ADD  CONSTRAINT [SynFolder__CfgTier_FKBailiff] FOREIGN KEY([CfgTierBailiffId])
REFERENCES [dbo].[CfgTier] ([PKey])
GO
ALTER TABLE [dbo].[SynFolder] CHECK CONSTRAINT [SynFolder__CfgTier_FKBailiff]
GO
ALTER TABLE [dbo].[SynFolder]  WITH CHECK ADD  CONSTRAINT [SynFolder__ComFolder_FK1] FOREIGN KEY([ComFolderId])
REFERENCES [dbo].[ComFolder] ([PKey])
GO
ALTER TABLE [dbo].[SynFolder] CHECK CONSTRAINT [SynFolder__ComFolder_FK1]
GO
ALTER TABLE [dbo].[SynFolder]  WITH CHECK ADD  CONSTRAINT [SynFolder__RntFolder_FK1] FOREIGN KEY([RntFolderId])
REFERENCES [dbo].[RntFolder] ([PKey])
GO
ALTER TABLE [dbo].[SynFolder] CHECK CONSTRAINT [SynFolder__RntFolder_FK1]
GO
ALTER TABLE [dbo].[SynFolder]  WITH CHECK ADD  CONSTRAINT [SynFolder__RntPeriodicity_FK1] FOREIGN KEY([RntPeriodicityId])
REFERENCES [dbo].[RntPeriodicity] ([PKey])
GO
ALTER TABLE [dbo].[SynFolder] CHECK CONSTRAINT [SynFolder__RntPeriodicity_FK1]
GO
ALTER TABLE [dbo].[SynFolder]  WITH CHECK ADD  CONSTRAINT [SynFolder__RntReconductionType_FK1] FOREIGN KEY([RntReconductionTypeId])
REFERENCES [dbo].[RntReconductionType] ([PKey])
GO
ALTER TABLE [dbo].[SynFolder] CHECK CONSTRAINT [SynFolder__RntReconductionType_FK1]
GO
ALTER TABLE [dbo].[SynFolder]  WITH CHECK ADD  CONSTRAINT [SynFolder__StkItem_FK1] FOREIGN KEY([StkItemId])
REFERENCES [dbo].[StkItem] ([PKey])
GO
ALTER TABLE [dbo].[SynFolder] CHECK CONSTRAINT [SynFolder__StkItem_FK1]
GO
ALTER TABLE [dbo].[SynFolder]  WITH CHECK ADD  CONSTRAINT [SynFolder__StkVocation_FK1] FOREIGN KEY([StkVocationId])
REFERENCES [dbo].[StkVocation] ([PKey])
GO
ALTER TABLE [dbo].[SynFolder] CHECK CONSTRAINT [SynFolder__StkVocation_FK1]
GO
ALTER TABLE [dbo].[SynFolder]  WITH CHECK ADD  CONSTRAINT [SynFolder__StrEntity_FK1] FOREIGN KEY([StrEntityId])
REFERENCES [dbo].[StrEntity] ([PKey])
GO
ALTER TABLE [dbo].[SynFolder] CHECK CONSTRAINT [SynFolder__StrEntity_FK1]
GO
ALTER TABLE [dbo].[SynFolder]  WITH CHECK ADD  CONSTRAINT [SynFolder__StrStatus_FK1] FOREIGN KEY([StrStatusId])
REFERENCES [dbo].[StrStatus] ([PKey])
GO
ALTER TABLE [dbo].[SynFolder] CHECK CONSTRAINT [SynFolder__StrStatus_FK1]
GO

