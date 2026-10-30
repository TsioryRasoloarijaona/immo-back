-- Lot admin-email-historique (FEATURE-admin-email-historique.md) : definition de yesRemsProd, sans FK.
IF OBJECT_ID('dbo.AdmEMailHistory', 'U') IS NULL
CREATE TABLE dbo.AdmEMailHistory (
    PKey uniqueidentifier NOT NULL CONSTRAINT PK_AdmEMailHistory PRIMARY KEY,
    MailDate datetime NULL,
    AdmUserId uniqueidentifier NULL,
    AdmUserDescription nvarchar(255) NULL,
    DisplayFromName nvarchar(255) NULL,
    EmailFrom nvarchar(255) NULL,
    EmailsTo nvarchar(1000) NULL,
    EmailsCc nvarchar(1000) NULL,
    EmailsBcc nvarchar(1000) NULL,
    Subject nvarchar(1000) NULL,
    Priority int NULL,
    IsBodyHtml bit NULL,
    Body nvarchar(max) NULL,
    SmtpHost nvarchar(255) NULL,
    SmtpPort int NULL,
    EnableSsl bit NULL,
    ObjectId uniqueidentifier NULL,
    ObjectLabel nvarchar(1000) NULL,
    ObjectStrEntityId uniqueidentifier NULL,
    SessionId uniqueidentifier NULL,
    IsAutoMatic bit NULL,
    EmailsSent bit NULL,
    ErrorDescription nvarchar(max) NULL
);
GO
