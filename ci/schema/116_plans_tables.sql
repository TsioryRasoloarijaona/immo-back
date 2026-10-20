-- Lot P3 (FEATURE-patrimoine-plans.md) : plans d'etage des tranches, comme en production (StkHierarchyPath existe deja, 110).
IF OBJECT_ID('dbo.StkHierarchyPlan', 'U') IS NULL
CREATE TABLE dbo.StkHierarchyPlan (
    PKey uniqueidentifier NOT NULL,
    Code nvarchar(255) COLLATE French_CI_AS NULL,
    Description nvarchar(255) COLLATE French_CI_AS NULL,
    PlanImage nvarchar(max) COLLATE French_CI_AS NULL,
    StkHierarchyId uniqueidentifier NULL,
    StkFloorId uniqueidentifier NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    width decimal(26, 6) NULL,
    height decimal(26, 6) NULL,
    X decimal(26, 6) NULL,
    Y decimal(26, 6) NULL,
    CONSTRAINT StkHierarchyPlan_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO
