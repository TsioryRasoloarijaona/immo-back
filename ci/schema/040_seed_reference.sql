-- ============================================================================
-- Donnees de reference minimales, requises par la suite de tests.
--
-- Ce fichier est ecrit a la main (contrairement aux 010/020/030, generes) : il
-- ne contient QUE des lignes de configuration que le logiciel legataire cree a
-- l'installation, et qu'aucun code applicatif n'insere. Aucune donnee client.
-- ============================================================================

-- StrEntity « ComFolder » : la definition de workflow des dossiers de vente.
-- Les fixtures de test creent des StrStatus en renseignant StrConfigEntityId
-- avec la constante StatutDossier.CONFIG_ENTITY_ID ; sans cette ligne parente,
-- la contrainte StrStatus__StrConfigEntity_FK1 rejette chacun de ces INSERT.
--
-- Les trois colonnes FK de StrEntity (AdmModuleId, StlCategoryId, ActAccountId)
-- sont nullables : la ligne se suffit a elle-meme, aucun parent a semer.
IF NOT EXISTS (SELECT 1 FROM dbo.StrEntity WHERE PKey = 'E7C73F70-7507-40D9-AA78-A2825C42B600')
    INSERT INTO dbo.StrEntity (PKey, Code, Description)
    VALUES ('E7C73F70-7507-40D9-AA78-A2825C42B600', N'ComFolder', N'Dossier de vente');
GO

-- References qu'OMRAN ecrit sur les dossiers, echeances et reglements
-- (FEATURE-interop-omran-ecritures.md, sections 2.4 et 9). Le code les resout par Code ; ces lignes,
-- avec les PKey de production, satisfont les FK StlSettlement -> StrEntity / AdmModule /
-- StlCurrency et ComDocument -> AdmModule. Identifiants repris dans ReferentielSeme.
IF NOT EXISTS (SELECT 1 FROM dbo.StrEntity WHERE PKey = 'D7061C59-A039-4EF8-A6ED-B0E886F8EDFF')
    INSERT INTO dbo.StrEntity (PKey, Code, Description)
    VALUES ('D7061C59-A039-4EF8-A6ED-B0E886F8EDFF', N'StlSettlementIn', N'Encaissement');
GO

IF NOT EXISTS (SELECT 1 FROM dbo.StrEntity WHERE PKey = '8287D7A9-2D51-4F2E-8F47-0F1B738F1A11')
    INSERT INTO dbo.StrEntity (PKey, Code, Description)
    VALUES ('8287D7A9-2D51-4F2E-8F47-0F1B738F1A11', N'StlSettlementOut', N'Decaissement');
GO

IF NOT EXISTS (SELECT 1 FROM dbo.AdmModule WHERE PKey = '00002C2D-BDFA-4541-94F7-C708A0E00001')
    INSERT INTO dbo.AdmModule (PKey, Code, Description)
    VALUES ('00002C2D-BDFA-4541-94F7-C708A0E00001', N'COM', N'Vente');
GO

IF NOT EXISTS (SELECT 1 FROM dbo.StlCurrency WHERE PKey = '24EB36A2-2E81-4A12-AF02-6259CFA25BD6')
    INSERT INTO dbo.StlCurrency (PKey, Code, Description)
    VALUES ('24EB36A2-2E81-4A12-AF02-6259CFA25BD6', N'MAD', N'MAD');
GO

-- AdmParamsGuid n'est pointee par aucune FK : hors de la fermeture de 010, d'ou sa creation ici
-- (une regeneration de 010 l'effacerait). Cle primaire = Code, comme en production.
IF OBJECT_ID('dbo.AdmParamsGuid') IS NULL
    CREATE TABLE dbo.AdmParamsGuid (
        [Code] [nvarchar](255) COLLATE French_CI_AS NOT NULL,
        [CodeValue] [uniqueidentifier] NULL,
        [UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
        [UserCreateDateTime] [datetime] NULL,
        [UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
        [UserUpdateDateTime] [datetime] NULL,
        [PKey] [uniqueidentifier] NULL,
        [StrConfigEntityId] [uniqueidentifier] NULL,
        CONSTRAINT [AdmParamsGuid_PK] PRIMARY KEY CLUSTERED ([Code] ASC)
    );
GO

IF NOT EXISTS (SELECT 1 FROM dbo.AdmParamsGuid WHERE Code = N'DefaultCurrency')
    INSERT INTO dbo.AdmParamsGuid (Code, CodeValue, PKey)
    VALUES (N'DefaultCurrency', '24EB36A2-2E81-4A12-AF02-6259CFA25BD6', '5DE193B6-E125-4CF0-A503-19AEBF15849B');
GO

-- PrjProject de reference, cible de AdmUserHabilitation.PrjProjectId.
--
-- AdmUserHabilitation est polymorphe : une portee par ligne, parmi societe / projet / tranche /
-- PrjProject. La feature « Perimetre commercial » n'exploite que la portee projet, mais ses tests
-- verifient que les trois autres SURVIVENT a une revision — il leur faut donc une ligne de portee
-- PrjProject valide. PrjProject appartient au module « Projets travaux » du logiciel legataire :
-- aucune entite JPA ne le modelise, la colonne est un UUID brut cote Java, et rien ne peut donc
-- creer cette ligne depuis le code de test.
--
-- L'identifiant est repris dans ReferentielSeme.PRJ_PROJECT ; les deux doivent rester alignes.
-- Toutes les FK de PrjProject sont nullables : la ligne se suffit a elle-meme.
IF NOT EXISTS (SELECT 1 FROM dbo.PrjProject WHERE PKey = 'B4E9D2C1-3A78-4F60-8D15-6E90C7B2A833')
    INSERT INTO dbo.PrjProject (PKey, Code, Description)
    VALUES ('B4E9D2C1-3A78-4F60-8D15-6E90C7B2A833', N'PRJ-FIXTURE', N'Projet travaux de reference');
GO
