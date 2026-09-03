/*
 * 002 — AppFileObject
 *
 * Pointeur S3 d'une pièce jointe. C'est le contrat entre le script de migration
 * (Python) et le backend (Java) : le script écrit ces lignes, l'application les lit.
 *
 * PRINCIPE : `AdmAttachedFile` reste la ligne de métadonnées et de rattachement
 * (`Category`, `CategoryId`, `AdmAttachedFileTypeId`, `Description`, `UserCreate`) —
 * tous les consommateurs les y lisent déjà. Seule la *localisation du binaire* sort
 * de la base. `AdmAttachedFile.AttachedFile` devient :
 *   - en écriture : plus jamais alimenté par le backend ;
 *   - en lecture   : repli tant que la ligne AppFileObject correspondante n'existe pas.
 *
 * La clé S3 est stockée EN ENTIER, jamais recalculée. C'est délibéré : la migration
 * (Python) et les nouveaux dépôts (Java) peuvent donc produire des clés de forme
 * légèrement différente sans rien casser, puisque l'application ne fait que relire
 * `StorageKey`. Même raisonnement pour `ContentType`.
 *
 * Idempotent : ne fait rien si la table existe déjà.
 */

IF OBJECT_ID('dbo.AppFileObject', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.AppFileObject
    (
        -- 1:1 avec AdmAttachedFile.PKey. C'est aussi la clé primaire : une pièce
        -- jointe a au plus un objet stocké.
        AdmAttachedFileId uniqueidentifier NOT NULL,

        -- Clé S3 complète, sans le nom du bucket.
        -- Ex. 'prod/pieces-jointes/dossier/3f2b.../8c1d....pdf'
        StorageKey        nvarchar(512)    NOT NULL,

        -- Type MIME servi au téléchargement. Déduit de AdmAttachedFile.Extention au
        -- moment de la migration, puis figé — la table fait foi, pas l'extension.
        ContentType       nvarchar(255)    NULL,

        -- Taille en octets, telle que mesurée à la source (DATALENGTH). Sert de
        -- contrôle croisé avant de vider le binaire legacy (phase 3).
        SizeBytes         bigint           NOT NULL,

        -- SHA-256 hexadécimal minuscule du contenu, calculé avant l'envoi et transmis
        -- à S3 en ChecksumSHA256 (rejet côté serveur si le transfert est corrompu).
        Sha256            char(64)         NULL,

        UploadedAt        datetime2(7)     NOT NULL,

        -- Auteur du dépôt : nom affiché de l'utilisateur pour un dépôt via l'API,
        -- 'migration' pour une ligne produite par le script de reprise.
        UploadedBy        nvarchar(255)    NULL,

        -- Suppression logique. L'objet S3 n'est JAMAIS supprimé dans la transaction
        -- de la requête : on marque ici, un balayage hors ligne supprime ensuite.
        DeletedAt         datetime2(7)     NULL,

        CONSTRAINT PK_AppFileObject
            PRIMARY KEY CLUSTERED (AdmAttachedFileId)
    );

    -- Deux lignes ne peuvent pas désigner le même objet S3. C'est le garde-fou contre
    -- une double exécution de la migration qui écraserait un objet déjà en place.
    CREATE UNIQUE NONCLUSTERED INDEX UX_AppFileObject_StorageKey
        ON dbo.AppFileObject (StorageKey);

    PRINT '002: table AppFileObject creee.';
END
ELSE
BEGIN
    PRINT '002: table AppFileObject deja presente, rien a faire.';
END
GO

/*
 * PAS DE CLÉ ÉTRANGÈRE vers AdmAttachedFile, volontairement :
 *
 *  - le schéma legacy n'en déclare aucune vers cette table ("Aucune clé étrangère ne
 *    fait référence à la table 'AdmAttachedFile'", vérifié via sp_help) — le
 *    rattachement y est partout conventionnel, jamais contraint ;
 *  - le script de migration reste ainsi indépendant de l'ordre d'insertion et ne peut
 *    pas échouer en masse sur une ligne source disparue entre la constitution de la
 *    liste de travail et l'envoi.
 *
 * La contrepartie est qu'une ligne orpheline est possible ; elle est détectable par
 * un simple anti-jointure et sans gravité (elle ne sera jamais lue).
 */
