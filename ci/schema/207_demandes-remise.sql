-- Demandes de remise des agences (FEATURE-agences-vente.md A3) : table propre a Arpent, idempotent.
-- A appliquer aussi a la main sur YesRems et sur la copie.
IF OBJECT_ID(N'dbo.AppDemandeRemise', 'U') IS NULL
CREATE TABLE dbo.AppDemandeRemise (
    PKey uniqueidentifier NOT NULL CONSTRAINT AppDemandeRemise_PK PRIMARY KEY,
    ComFolderId uniqueidentifier NOT NULL,
    TauxDemande decimal(26, 6) NOT NULL,
    TauxAccorde decimal(26, 6) NULL,
    Motif nvarchar(1000) NOT NULL,
    MotifRefus nvarchar(1000) NULL,
    Statut nvarchar(20) NOT NULL
        CONSTRAINT AppDemandeRemise_Statut_CK CHECK (Statut IN (N'EN_ATTENTE', N'ACCEPTEE', N'REFUSEE', N'ANNULEE')),
    DemandeParUserId uniqueidentifier NOT NULL,
    DemandeLe datetime2 NOT NULL,
    TraiteParUserId uniqueidentifier NULL,
    TraiteLe datetime2 NULL);
GO
-- FK seulement si la table parente porte sa cle primaire (yesRemsTest n'en a pas).
IF OBJECT_ID(N'dbo.AppDemandeRemise_ComFolder_FK', 'F') IS NULL
   AND OBJECTPROPERTY(OBJECT_ID(N'dbo.ComFolder'), 'TableHasPrimaryKey') = 1
    ALTER TABLE dbo.AppDemandeRemise ADD CONSTRAINT AppDemandeRemise_ComFolder_FK
        FOREIGN KEY (ComFolderId) REFERENCES dbo.ComFolder (PKey);
IF OBJECT_ID(N'dbo.AppDemandeRemise_DemandePar_FK', 'F') IS NULL
   AND OBJECTPROPERTY(OBJECT_ID(N'dbo.AdmUser'), 'TableHasPrimaryKey') = 1
    ALTER TABLE dbo.AppDemandeRemise ADD CONSTRAINT AppDemandeRemise_DemandePar_FK
        FOREIGN KEY (DemandeParUserId) REFERENCES dbo.AdmUser (PKey);
IF OBJECT_ID(N'dbo.AppDemandeRemise_TraitePar_FK', 'F') IS NULL
   AND OBJECTPROPERTY(OBJECT_ID(N'dbo.AdmUser'), 'TableHasPrimaryKey') = 1
    ALTER TABLE dbo.AppDemandeRemise ADD CONSTRAINT AppDemandeRemise_TraitePar_FK
        FOREIGN KEY (TraiteParUserId) REFERENCES dbo.AdmUser (PKey);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'AppDemandeRemise_UneEnAttente_UX')
    CREATE UNIQUE INDEX AppDemandeRemise_UneEnAttente_UX ON dbo.AppDemandeRemise (ComFolderId)
        WHERE Statut = N'EN_ATTENTE';
GO
