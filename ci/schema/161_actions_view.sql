-- Lot admin-actions (FEATURE-admin-actions.md) : doublure de CI de la vue AdmActionsView, jamais en production.
IF OBJECT_ID(N'AdmActionsView') IS NULL
CREATE TABLE dbo.AdmActionsView (
    PKey uniqueidentifier NOT NULL,
    Code nvarchar(255) NULL,
    Description nvarchar(max) NULL,
    ActionLabel nvarchar(261) NULL,
    UserCreateDateTime datetime NULL,
    UserCreate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL
);
GO
