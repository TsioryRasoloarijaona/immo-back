-- Lot admin-sessions (FEATURE-admin-sessions.md) : definition de yesRemsProd.
IF OBJECT_ID('dbo.AdmWebSessions', 'U') IS NULL
CREATE TABLE dbo.AdmWebSessions (
    PKey uniqueidentifier NOT NULL CONSTRAINT AdmWebSessions_PK PRIMARY KEY,
    WebSession nvarchar(max) NULL,
    UserId uniqueidentifier NULL,
    UserDesc nvarchar(max) NULL,
    StartDate datetime NOT NULL,
    EndDate datetime NULL,
    IPAddress nvarchar(max) NULL,
    State int NOT NULL,
    SystemVersion varchar(255) NULL,
    AdmSessionOriginId uniqueidentifier NULL
);
GO
