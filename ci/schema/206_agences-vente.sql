-- Agences de vente externes (FEATURE-agences-vente.md §3) : données de configuration seules, idempotent.
-- A appliquer aussi a la main sur YesRems et sur la copie.

IF NOT EXISTS (SELECT 1 FROM CfgTierKind WHERE Code = N'AGV')
   AND EXISTS (SELECT 1 FROM CfgTierType WHERE Code = N'CNT')
    INSERT INTO CfgTierKind (PKey, Code, Description, Sorting, CfgTierTypeId, UserCreateDateTime)
    SELECT TOP 1 NEWID(), N'AGV', N'Agence de vente', 0, PKey, GETDATE()
    FROM CfgTierType WHERE Code = N'CNT' ORDER BY PKey ASC;

IF NOT EXISTS (SELECT 1 FROM CfgRelationType WHERE Code = N'Vendeur d''agence')
    INSERT INTO CfgRelationType (PKey, Code, Description, UserCreateDateTime)
    VALUES (NEWID(), N'Vendeur d''agence', N'Vendeur d''agence', GETDATE());

IF NOT EXISTS (SELECT 1 FROM AdmRole WHERE Code = N'Agence')
    INSERT INTO AdmRole (PKey, Code, Description, UserCreateDateTime, WithSequenceFreeRestriction,
                         WithReportRestriction, WithDeleteAttachementRestriction, WithAuditRestriction,
                         WithSpecificFieldsRestriction, WithAddActionMessageRestriction,
                         WithEditActionMessageRestriction, WithDeleteActionMessageRestriction,
                         WithEditAttachementRestriction, WithAddAttachementRestriction)
    VALUES (NEWID(), N'Agence', N'Agence de vente', GETDATE(), 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
