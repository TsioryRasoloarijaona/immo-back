/*
 * 001 — AppUserCredential
 *
 * Table applicative (hors schéma legacy) portant l'authentification : hash du mot de
 * passe, portes d'onboarding (changement de mot de passe imposé, complétion du
 * profil) et version de jeton pour la révocation JWT.
 *
 * Le legacy `AdmUser` reste la source d'identité (nom, rôles, habilitations) ; cette
 * table ne fait que lui adjoindre ce dont l'API REST a besoin et que l'application
 * Windows n'a jamais stocké.
 *
 * Rétro-ingéniérie de la table créée à la main en développement — mêmes colonnes,
 * mêmes types, mêmes contraintes, noms de contraintes explicites (voir README).
 *
 * Idempotent : ne fait rien si la table existe déjà.
 */

IF OBJECT_ID('dbo.AppUserCredential', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.AppUserCredential
    (
        PKey               uniqueidentifier NOT NULL,
        AdmUserId          uniqueidentifier NOT NULL,
        PasswordHash       nvarchar(255)    NOT NULL,
        MustChangePassword bit              NOT NULL,
        ProfileCompleted   bit              NOT NULL,
        TokenVersion       int              NOT NULL,
        UserCreateDateTime datetime2(7)     NOT NULL,
        UserUpdateDateTime datetime2(7)     NULL,

        CONSTRAINT PK_AppUserCredential
            PRIMARY KEY CLUSTERED (PKey),

        -- Un jeu d'identifiants au plus par utilisateur legacy.
        CONSTRAINT UQ_AppUserCredential_AdmUserId
            UNIQUE (AdmUserId),

        CONSTRAINT FK_AppUserCredential_AdmUser
            FOREIGN KEY (AdmUserId) REFERENCES dbo.AdmUser (PKey)
    );

    PRINT '001: table AppUserCredential creee.';
END
ELSE
BEGIN
    PRINT '001: table AppUserCredential deja presente, rien a faire.';
END
GO
