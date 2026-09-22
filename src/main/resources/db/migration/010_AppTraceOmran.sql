/*
 * 010 — AppTraceOmran : archive des requetes d'OMRAN Estate captees par l'audit SQL Server
 *
 * L'audit serveur (Audit_Global -> C:\backup\*.sqlaudit) tourne sur 20 fichiers de 100 Mo :
 * au-dela, SQL Server SUPPRIME les fichiers les plus anciens, et avec eux la trace des ecrans
 * d'Omran — la source de verite sur les colonnes reellement lues/ecrites par le legataire.
 * Cette table en conserve la part OMRAN (application_name = 'OMRAN estate'), alimentee par
 * ArchivageTraceOmran avant que la rotation ne detruise les fichiers.
 *
 * A APPLIQUER SUR LES COPIES DE LECTURE UNIQUEMENT : yesRemsProd (banc local) et yesRemsDev
 * (VPS). JAMAIS sur la source — l'archive appartient au consommateur, pas a la verite.
 *
 * PAS DE CLE NATURELLE, ET C'EST MESURE : une requete sur une vue produit un enregistrement
 * d'audit PAR OBJET reference (meme offset, meme instant, meme statement), et 18 848 lignes
 * du fichier brut sont strictement identiques sur toutes les colonnes (releve du 22/09/2026).
 * D'ou la cle technique PKey et AUCUNE contrainte d'unicite : l'idempotence est portee par le
 * filigrane strict du cycle (voir ArchivageTraceOmran), pas par une deduplication.
 *
 * EventTimeUtc : l'audit horodate en UTC (decalage d'une heure avec l'application, constate) ;
 * la colonne porte le suffixe pour que personne ne compare a l'heure locale sans conversion.
 * Statement : l'audit tronque a 4 000 caracteres — NVARCHAR(4000) suffit par construction.
 * FichierNom/FichierOffset/SequenceNo : position physique dans les fichiers d'audit, gardee
 * pour le diagnostic (retrouver l'enregistrement d'origine tant que le fichier existe).
 *
 * Idempotent : ne fait rien si la table existe deja.
 */

IF OBJECT_ID('dbo.AppTraceOmran') IS NULL
BEGIN
    CREATE TABLE dbo.AppTraceOmran (
        PKey          bigint IDENTITY(1,1) NOT NULL CONSTRAINT PK_AppTraceOmran PRIMARY KEY,
        EventTimeUtc  datetime2(7)   NOT NULL,
        ActionId      varchar(4)     NOT NULL,
        Succeeded     bit            NOT NULL,
        PrincipalNom  sysname        NULL,
        BaseNom       sysname        NULL,
        SchemaNom     sysname        NULL,
        ObjetNom      sysname        NULL,
        Statement     nvarchar(4000) NULL,
        ClientIp      nvarchar(48)   NULL,
        FichierNom    nvarchar(260)  NOT NULL,
        FichierOffset bigint         NOT NULL,
        SequenceNo    int            NOT NULL,
        ArchiveLe     datetime2(0)   NOT NULL CONSTRAINT DF_AppTraceOmran_ArchiveLe DEFAULT SYSDATETIME()
    );

    -- Le filigrane du cycle est MAX(EventTimeUtc) ; les analyses filtrent par ecran (objet).
    CREATE INDEX IX_AppTraceOmran_EventTimeUtc ON dbo.AppTraceOmran (EventTimeUtc);
    CREATE INDEX IX_AppTraceOmran_Objet ON dbo.AppTraceOmran (ObjetNom, EventTimeUtc);

    PRINT '010: table AppTraceOmran creee.';
END
ELSE
    PRINT '010: AppTraceOmran existe deja, rien a faire.';
