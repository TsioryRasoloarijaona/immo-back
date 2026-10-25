package com.tsix.immocrm.fixtures;

import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Champs spécifiques (GEN0007, FEATURE-patrimoine-import-export.md §4.2, lot P9d) : les 4 tables {@code AdmSpecificField*}
 * absentes de yesRemsTest sont créées si absentes (colonnes, FK et actions de prod ; CI : ci/schema/144-145), dans la
 * transaction du test et annulées avec elle ; les 8 types de données à leurs PKey de prod ; entités et définitions du test.
 */
@Component
public class FixtureChampsSpecifiques {

    // Codes d'entité OMRAN (Current.GetType().FullName) ; terrain = LandProject (correction du manager 25/10).
    public static final String PROJET = "Yes.Cfg.CfgProject";
    public static final String TRANCHE = "Yes.Cfg.CfgTranche";
    public static final String ENTITE = "Yes.Cfg.CfgCompany";
    public static final String ARTICLE = "Yes.Stk.Building.Item";
    public static final String TERRAIN = "Yes.Stk.Land.LandProject";
    public static final String PROSPECTION_TERRAIN = "Yes.Lnd.Prospection";

    public static final String TEXTE = "String";
    public static final String DATE = "Date";
    public static final String DECIMAL = "Decimal";
    public static final String MONTANT = "Money";
    public static final String ENTIER = "Int";
    public static final String POURCENTAGE = "Percentage";
    public static final String BOOLEEN = "Bit";
    public static final String LISTE = "Drop down";

    /** PKey de prod des 8 {@code AdmSpecificFieldDataType}, dans l'ordre de {@code Sorting}. */
    public static final Map<String, UUID> TYPES = Map.of(
            TEXTE, UUID.fromString("664DED16-59E0-420B-883D-8FC3EFC8978F"),
            DATE, UUID.fromString("44CB9C1D-C9D3-4C0A-AE05-FCB3EBF4A294"),
            DECIMAL, UUID.fromString("97E816C5-AB01-495C-AE89-871BF8DCF979"),
            MONTANT, UUID.fromString("25E8B361-EB37-489B-A076-8B005B1A8022"),
            ENTIER, UUID.fromString("BCE36311-D7E7-40B7-83B0-F73A79AB4819"),
            POURCENTAGE, UUID.fromString("820BE51C-8D15-4133-A518-F7BD3A7F0C96"),
            BOOLEEN, UUID.fromString("83EEFBB3-C4A0-4D8F-9F38-0F143806FF1C"),
            LISTE, UUID.fromString("E39D601F-5D11-4A98-A87F-63DB6C5C5DF4"));
    private static final List<String> ORDRE_TYPES = List.of(TEXTE, DATE, DECIMAL, MONTANT, ENTIER, POURCENTAGE, BOOLEEN, LISTE);

    private static final String TRACE = """
                UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL,
                UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL""";
    private static final List<String> TABLES = List.of("""
            IF OBJECT_ID('dbo.AdmSpecificFieldEntity', 'U') IS NULL
            CREATE TABLE dbo.AdmSpecificFieldEntity (
                PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(255) NULL, Description nvarchar(255) NULL,
                Sorting int NULL,""" + TRACE + ")", """
            IF OBJECT_ID('dbo.AdmSpecificFieldDataType', 'U') IS NULL
            CREATE TABLE dbo.AdmSpecificFieldDataType (
                PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(255) NULL, Sorting int NULL,""" + TRACE + ")", """
            IF OBJECT_ID('dbo.AdmSpecificField', 'U') IS NULL
            CREATE TABLE dbo.AdmSpecificField (
                PKey uniqueidentifier NOT NULL PRIMARY KEY, AdmSpecificFieldEntityId uniqueidentifier NULL,
                Category nvarchar(255) NULL, Name nvarchar(255) NULL, Caption nvarchar(255) NULL,
                AdmSpecificFieldDataTypeId uniqueidentifier NULL, DefaultValue nvarchar(255) NULL, Required bit NULL,
                UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL,
                UserUpdateDateTime datetime NULL, DateValue datetime NULL, BoolValue bit NULL, DecimalValue decimal(26,6) NULL,
                IntValue int NULL, CategorySorting int NULL, Sorting int NULL, ComboValue nvarchar(255) NULL)""", """
            IF OBJECT_ID('dbo.AdmSpecificFieldValue', 'U') IS NULL
            CREATE TABLE dbo.AdmSpecificFieldValue (
                PKey uniqueidentifier NOT NULL PRIMARY KEY, AdmSpecificFieldId uniqueidentifier NULL,
                NameSpaceId uniqueidentifier NULL, Value nvarchar(1000) NULL, DateValue datetime NULL, BoolValue bit NULL,
                DecimalValue decimal(26,6) NULL, IntValue int NULL, ComboValue nvarchar(255) NULL,""" + TRACE + ")", """
            IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'AdmSpecificField_AdmSpecificFieldEntity_FK')
            ALTER TABLE dbo.AdmSpecificField ADD CONSTRAINT AdmSpecificField_AdmSpecificFieldEntity_FK
                FOREIGN KEY (AdmSpecificFieldEntityId) REFERENCES dbo.AdmSpecificFieldEntity (PKey)""", """
            IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'AdmSpecificField_AdmSpecificFieldDataType_FK')
            ALTER TABLE dbo.AdmSpecificField ADD CONSTRAINT AdmSpecificField_AdmSpecificFieldDataType_FK
                FOREIGN KEY (AdmSpecificFieldDataTypeId) REFERENCES dbo.AdmSpecificFieldDataType (PKey)""", """
            IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'AdmSpecificFieldValue_AdmSpecificField_FK1')
            ALTER TABLE dbo.AdmSpecificFieldValue ADD CONSTRAINT AdmSpecificFieldValue_AdmSpecificField_FK1
                FOREIGN KEY (AdmSpecificFieldId) REFERENCES dbo.AdmSpecificField (PKey) ON DELETE CASCADE""");

    private final JdbcTemplate jdbc;
    private final EntityManager em;

    public FixtureChampsSpecifiques(JdbcTemplate jdbc, EntityManager em) {
        this.jdbc = jdbc;
        this.em = em;
    }

    /** Une définition de champ : groupe, libellé, type (code de prod), défaut, obligatoire, ordres, choix. */
    public record Definition(String groupe, String libelle, String type, String defaut, boolean obligatoire,
                             int ordreGroupe, int ordre, String choix) {

        public static Definition simple(String libelle, String type) {
            return new Definition("GROUPE", libelle, type, null, false, 1, 1, null);
        }
    }

    /** Tables, FK et 8 types de données, créés s'ils manquent. */
    public void schema() {
        em.flush();
        TABLES.forEach(jdbc::execute);
        for (int i = 0; i < ORDRE_TYPES.size(); i++) {
            String code = ORDRE_TYPES.get(i);
            jdbc.update("""
                    IF NOT EXISTS (SELECT 1 FROM AdmSpecificFieldDataType WHERE PKey = ?)
                    INSERT INTO AdmSpecificFieldDataType (PKey, Code, Sorting) VALUES (?, ?, ?)""",
                    TYPES.get(code).toString(), TYPES.get(code).toString(), code, i + 1);
        }
    }

    /** Ligne {@code AdmSpecificFieldEntity} de ce code (la première si plusieurs), créée si absente. */
    public UUID entite(String code) {
        List<String> existantes = jdbc.queryForList(
                "SELECT CONVERT(varchar(36), PKey) FROM AdmSpecificFieldEntity WHERE Code = ?", String.class, code);
        if (!existantes.isEmpty()) {
            return UUID.fromString(existantes.getFirst());
        }
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO AdmSpecificFieldEntity (PKey, Code, Description) VALUES (?, ?, ?)", id.toString(), code,
                code);
        return id;
    }

    public UUID champ(String codeEntite, Definition d) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO AdmSpecificField (PKey, AdmSpecificFieldEntityId, Category, Name, Caption,
                    AdmSpecificFieldDataTypeId, DefaultValue, Required, CategorySorting, Sorting, ComboValue)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", id.toString(), entite(codeEntite).toString(), d.groupe(),
                d.libelle(), d.libelle(), TYPES.get(d.type()).toString(), d.defaut(), d.obligatoire(), d.ordreGroupe(),
                d.ordre(), d.choix());
        return id;
    }

    public void valeur(UUID champ, UUID objet, String texte) {
        jdbc.update("INSERT INTO AdmSpecificFieldValue (PKey, AdmSpecificFieldId, NameSpaceId, Value) VALUES (?, ?, ?, ?)",
                UUID.randomUUID().toString(), champ.toString(), objet.toString(), texte);
    }

    // ------------------------------------------------------------------ lecture

    /** Lignes de valeur d'un champ pour un objet : Value, DecimalValue, IntValue, DateValue, BoolValue, ComboValue. */
    public List<Map<String, Object>> valeurs(UUID champ, UUID objet) {
        em.flush();
        return jdbc.queryForList("""
                SELECT Value, DecimalValue, IntValue, DateValue, BoolValue, ComboValue FROM AdmSpecificFieldValue
                WHERE AdmSpecificFieldId = ? AND NameSpaceId = ?""", champ.toString(), objet.toString());
    }

    public int valeursDuChamp(UUID champ) {
        em.flush();
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM AdmSpecificFieldValue WHERE AdmSpecificFieldId = ?",
                Integer.class, champ.toString());
        return n == null ? 0 : n;
    }

    public int valeursDeLObjet(UUID objet) {
        em.flush();
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM AdmSpecificFieldValue WHERE NameSpaceId = ?",
                Integer.class, objet.toString());
        return n == null ? 0 : n;
    }

    public int entitesDeCode(String code) {
        em.flush();
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM AdmSpecificFieldEntity WHERE Code = ?", Integer.class, code);
        return n == null ? 0 : n;
    }

    /** Définitions de ce libellé ({@code Caption}) rattachées à une ligne d'entité existante, avec le code de l'entité. */
    public List<Map<String, Object>> definitions(String libelle) {
        em.flush();
        return jdbc.queryForList("""
                SELECT CONVERT(varchar(36), f.PKey) AS PKey, e.Code AS Entite, f.Category, f.Caption, f.DefaultValue,
                       f.Required, f.CategorySorting, f.Sorting, f.ComboValue, t.Code AS TypeDonnee
                FROM AdmSpecificField f
                JOIN AdmSpecificFieldEntity e ON e.PKey = f.AdmSpecificFieldEntityId
                LEFT JOIN AdmSpecificFieldDataType t ON t.PKey = f.AdmSpecificFieldDataTypeId
                WHERE f.Caption = ?""", libelle);
    }

    public boolean champExiste(UUID champ) {
        em.flush();
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM AdmSpecificField WHERE PKey = ?", Integer.class,
                champ.toString());
        return n != null && n > 0;
    }
}
