package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Lignes {@code CfgTier} et leurs parents (FEATURE-admin-tiers.md), pour toutes les familles du socle. Tout en JDBC : les
 * lignes rejoignent la transaction d'un test {@code @Transactional} (rollback), sinon elles sont validées et
 * {@link #nettoyer(Jeu)} les efface, avec les tiers créés par l'import dont la raison sociale porte le jeton.
 */
@Component
public class FixtureTiers {

    private static final Logger log = LoggerFactory.getLogger(FixtureTiers.class);

    /** Familles du socle : {@code famille} de l'URL, {@code code} de {@code CfgTierType}, PKey de production. */
    public enum TypeTiers {
        FOURNISSEURS("fournisseurs", "FOU", "00685105-DFD9-497B-8830-DBF0FBE91668"),
        CONTACTS("contacts", "CNT", "37E49449-5066-46B8-B7EF-5D69F57F2BB1"),
        BANQUES("banques", "BAN", "2ECF2CE9-3EA8-47C6-95EE-2CB63D8A02C2"),
        ORGANISMES("organismes", "ORG", "37E49449-5066-46B8-B7EF-5D69F57F2BB0"),
        PERSONNELS("personnels", "EMP", "EEB3FCFF-15BE-484D-B785-923D7587E649");

        private final String famille;
        private final String code;
        private final UUID pkeyProd;

        TypeTiers(String famille, String code, String pkeyProd) {
            this.famille = famille;
            this.code = code;
            this.pkeyProd = UUID.fromString(pkeyProd);
        }

        public String famille() {
            return famille;
        }

        public String code() {
            return code;
        }
    }

    // Absentes de yesRemsTest, présentes en CI : définition de ci/schema/010_tables.sql.
    private static final List<String> TABLES = List.of("""
            IF OBJECT_ID('dbo.AdmLiability', 'U') IS NULL
            CREATE TABLE dbo.AdmLiability (
                PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(255) NULL, Description nvarchar(255) NULL,
                Sorting int NULL, IsExonarated bit NULL, IsMajorated bit NULL, MajorationValue decimal(26, 6) NULL,
                UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL,
                UserUpdateDateTime datetime NULL)""", """
            IF OBJECT_ID('dbo.AdmRank', 'U') IS NULL
            CREATE TABLE dbo.AdmRank (PKey uniqueidentifier NOT NULL PRIMARY KEY, [Rank] int NULL, Code nvarchar(255) NULL)""");

    // Les FK de la CI manquent à yesRemsTest : sans elles ni le 409 ni la cascade ne se jouent en local.
    private static final List<String[]> CLES = List.of(
            new String[]{"CfgTier", "CfgCommercialId", ""},
            new String[]{"CfgTierContact", "CfgContactId", ""},
            new String[]{"CfgTierContact", "CfgTierId", " ON DELETE CASCADE"},
            new String[]{"ComActionMessage", "CfgTierId", " ON DELETE CASCADE"});

    private static final String LIGNE = """
            SELECT Code, Description, SocialReason, FirstName, LastName, Mobile, EMail, Notes, UserCreate, UserUpdate,
                   SocialSecurityNumber, SalaryBase, BaseMonthNumber,
                   CONVERT(varchar(23), UserCreateDateTime, 126) AS UserCreateDateTime,
                   CONVERT(varchar(23), UserUpdateDateTime, 126) AS UserUpdateDateTime,
                   CONVERT(varchar(23), HireDate, 126) AS HireDate,
                   LOWER(CONVERT(varchar(36), CfgTierTypeId)) AS CfgTierTypeId,
                   LOWER(CONVERT(varchar(36), CfgTierKindId)) AS CfgTierKindId,
                   LOWER(CONVERT(varchar(36), AdmCountryResidenceId)) AS AdmCountryResidenceId,
                   LOWER(CONVERT(varchar(36), StlCurrencyId)) AS StlCurrencyId,
                   LOWER(CONVERT(varchar(36), AdmSocialFormId)) AS AdmSocialFormId,
                   LOWER(CONVERT(varchar(36), ActAccountId)) AS ActAccountId,
                   LOWER(CONVERT(varchar(36), CfgContactId)) AS CfgContactId,
                   CAST(IsProspect AS int) AS IsProspect, CAST(IsOldCustomer AS int) AS IsOldCustomer,
                   CAST(IsRntCustomer AS int) AS IsRntCustomer, CAST(IsVisibleInList AS int) AS IsVisibleInList,
                   CAST(IsFundReader AS int) AS IsFundReader, CAST(IsExtern AS int) AS IsExtern
            FROM CfgTier WHERE PKey = ?""";

    private static final String SEQUENCE_TIER = "Tier";
    private static final String TABLE_TIERS = "CfgTier";

    private final JdbcTemplate jdbc;
    private final EntityManager em;
    private final FixturePatrimoine patrimoine;
    private final PlatformTransactionManager transactionManager;

    public FixtureTiers(DataSource dataSource, EntityManager em, FixturePatrimoine patrimoine,
                        PlatformTransactionManager transactionManager) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.em = em;
        this.patrimoine = patrimoine;
        this.transactionManager = transactionManager;
    }

    /** Administrateur, types de tiers et référentiels obligatoires d'une fiche, propres au test. */
    public Jeu jeu() {
        TABLES.forEach(jdbc::execute);
        AdmUser admin = new TransactionTemplate(transactionManager).execute(s -> patrimoine.administrateur());
        return new Jeu(admin);
    }

    // ------------------------------------------------------------------ lecture (écritures JPA du test d'abord)

    /** Colonnes de {@link #LIGNE} ; les PKey en minuscules comme sur le fil. */
    public Map<String, Object> ligne(UUID id) {
        synchroniser();
        return jdbc.queryForMap(LIGNE, id.toString());
    }

    public boolean existe(UUID id) {
        return compter("SELECT COUNT(*) FROM CfgTier WHERE PKey = ?", id) == 1;
    }

    public int liens(UUID tiers) {
        return compter("SELECT COUNT(*) FROM CfgTierContact WHERE CfgTierId = ?", tiers);
    }

    public int lignesLien(UUID tiers, UUID contact, UUID relation) {
        return compter("""
                SELECT COUNT(*) FROM CfgTierContact WHERE CfgTierId = ? AND CfgContactId = ? AND CfgRelationTypeId = ?""",
                tiers, contact, relation);
    }

    public int actions(UUID tiers) {
        return compter("SELECT COUNT(*) FROM ComActionMessage WHERE CfgTierId = ?", tiers);
    }

    /** Tiers dont la raison sociale est exactement celle-ci. */
    public List<UUID> parRaisonSociale(String raison) {
        synchroniser();
        return jdbc.queryForList("SELECT PKey FROM CfgTier WHERE SocialReason = ?", UUID.class, raison);
    }

    /** Ids en minuscules, dans l'ordre SQL Server de {@code uniqueidentifier}. */
    public List<String> ordreParPKey(UUID a, UUID b) {
        return jdbc.queryForList("""
                SELECT LOWER(CONVERT(varchar(36), PKey)) FROM CfgTier WHERE PKey IN (?, ?) ORDER BY PKey ASC""",
                String.class, a.toString(), b.toString());
    }

    public int compteurTiers() {
        Integer valeur = jdbc.queryForObject("SELECT COALESCE(MAX(SeqValue), 0) FROM AdmSequence WHERE Code = ?",
                Integer.class, SEQUENCE_TIER);
        return valeur == null ? 0 : valeur;
    }

    private int compter(String sql, UUID... ids) {
        synchroniser();
        Integer n = jdbc.queryForObject(sql, Integer.class, (Object[]) texte(ids));
        return n == null ? 0 : n;
    }

    private static String[] texte(UUID... ids) {
        String[] valeurs = new String[ids.length];
        for (int k = 0; k < ids.length; k++) {
            valeurs[k] = ids[k].toString();
        }
        return valeurs;
    }

    private void synchroniser() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            em.flush();
        }
    }

    /** Après un refus levé au flush : oublie l'écriture ratée, sinon chaque lecture la rejoue. */
    public void oublierEcritureRefusee() {
        em.clear();
    }

    // ------------------------------------------------------------------ préparation globale

    /** Dans un test {@code @Transactional} seulement : fixe {@code SeqValue} du compteur tiers (ligne verrouillée). */
    public void fixerCompteurTiers(int valeur) {
        int lignes = jdbc.update("UPDATE AdmSequence SET SeqValue = ? WHERE Code = ?", valeur, SEQUENCE_TIER);
        if (lignes == 0) {
            jdbc.update("""
                    INSERT INTO AdmSequence (PKey, Code, SeqYearNumber, SeqValue, SeqValueNumber, Separator, BasedOnSystemYear)
                    VALUES (?, ?, 2, ?, 5, N'/', 1)""", UUID.randomUUID().toString(), SEQUENCE_TIER, valeur);
        }
    }

    /** Dans un test {@code @Transactional} seulement (DDL annulé au rollback) : FK de suppression de la CI. */
    public void poserClesEtrangeres() {
        CLES.forEach(c -> jdbc.execute(("""
                IF NOT EXISTS (SELECT 1 FROM sys.foreign_key_columns
                               WHERE parent_object_id = OBJECT_ID('dbo.%1$s')
                                 AND referenced_object_id = OBJECT_ID('dbo.CfgTier')
                                 AND parent_column_id = COLUMNPROPERTY(OBJECT_ID('dbo.%1$s'), '%2$s', 'ColumnId'))
                ALTER TABLE dbo.%1$s WITH NOCHECK ADD CONSTRAINT FK_TEST_%1$s_%2$s
                    FOREIGN KEY (%2$s) REFERENCES dbo.CfgTier (PKey)%3$s""").formatted((Object[]) c)));
    }

    // ------------------------------------------------------------------ nettoyage

    public void nettoyer(Jeu j) {
        if (j == null) {
            return;
        }
        String motif = "%" + j.jeton() + "%";
        effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN (SELECT PKey FROM CfgTier WHERE SocialReason LIKE ?)", motif);
        effacer("DELETE FROM CfgTier WHERE SocialReason LIKE ?", motif);
        for (String[] suivi : j.suivis) {
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ? OR ParentObjectId = ?", suivi[1], suivi[1]);
            effacer("DELETE FROM " + suivi[0] + " WHERE PKey = ?", suivi[1]);
        }
    }

    private void effacer(String sql, Object... parametres) {
        try {
            jdbc.update(sql, parametres);
        } catch (DataAccessException e) {
            log.warn("Nettoyage incomplet ({}) : {}", sql, e.getMessage());
        }
    }

    // ------------------------------------------------------------------ jeu d'un test

    /** Parents d'une fiche de tiers ; chaque ligne porte le jeton et se retrouve par {@link #codeDe} / {@link #libelleDe}. */
    public final class Jeu {

        private final String jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        private final AdmUser admin;
        private final Deque<String[]> suivis = new ArrayDeque<>();
        private final Map<UUID, String> codes = new HashMap<>();
        private final Map<TypeTiers, UUID> types = new EnumMap<>(TypeTiers.class);
        private final UUID pays;
        private final UUID devise;
        private final UUID formeMorale;
        private final UUID formePhysique;
        private final UUID titre;
        private final UUID sexe;
        private final UUID assujettissement;
        private final UUID note;

        private Jeu(AdmUser admin) {
            this.admin = admin;
            suivis.push(new String[]{"AdmUser", admin.getId().toString()});
            for (TypeTiers type : TypeTiers.values()) {
                types.put(type, assurerType(type));
            }
            pays = parent("AdmCountry");
            devise = parent("StlCurrency");
            formeMorale = forme(false);
            formePhysique = forme(true);
            titre = parent("AdmTierTitle");
            sexe = parent("AdmSexType");
            assujettissement = parent("AdmLiability");
            note = rang();
        }

        private UUID assurerType(TypeTiers type) {
            List<UUID> existants = jdbc.queryForList(
                    "SELECT PKey FROM CfgTierType WHERE Code = ? ORDER BY PKey ASC", UUID.class, type.code());
            if (!existants.isEmpty()) {
                return existants.getFirst();
            }
            jdbc.update("INSERT INTO CfgTierType (PKey, Code, Description) VALUES (?, ?, ?)", type.pkeyProd.toString(),
                    type.code(), type.code());
            suivre("CfgTierType", type.pkeyProd);
            return type.pkeyProd;
        }

        private UUID forme(boolean personne) {
            UUID id = UUID.randomUUID();
            String code = code("FOR" + (personne ? "P" : "M"));
            jdbc.update("INSERT INTO AdmSocialForm (PKey, Code, Description, IsPerson) VALUES (?, ?, ?, ?)", id.toString(),
                    code, libelle(code), personne);
            return retenir("AdmSocialForm", id, code);
        }

        private UUID rang() {
            UUID id = UUID.randomUUID();
            String code = code("RNG");
            jdbc.update("INSERT INTO AdmRank (PKey, [Rank], Code) VALUES (?, 1, ?)", id.toString(), code);
            return retenir("AdmRank", id, code);
        }

        /** Ligne d'un référentiel à colonnes {@code Code} et {@code Description}, parent d'une FK de la CI. */
        public UUID parent(String table) {
            UUID id = UUID.randomUUID();
            String code = code(table);
            jdbc.update("INSERT INTO " + table + " (PKey, Code, Description) VALUES (?, ?, ?)", id.toString(), code,
                    libelle(code));
            return retenir(table, id, code);
        }

        /** Nature ({@code CfgTierKind}) rattachée à ce type. */
        public UUID nature(TypeTiers type) {
            UUID id = UUID.randomUUID();
            String code = code("NAT" + type.code());
            jdbc.update("INSERT INTO CfgTierKind (PKey, Code, Description, CfgTierTypeId) VALUES (?, ?, ?, ?)", id.toString(),
                    code, libelle(code), typeId(type).toString());
            return retenir("CfgTierKind", id, code);
        }

        public UUID relation() {
            return parent("CfgRelationType");
        }

        /** Tiers du type, avec {@code Code}, {@code Description} et {@code SocialReason} portant le jeton. */
        public Ligne tiers(TypeTiers type, String suffixe) {
            String code = code(suffixe);
            return new Ligne().avec("Code", code).avec("Description", "Tiers " + code).avec("SocialReason", "Tiers " + code)
                    .avec("CfgTierTypeId", typeId(type));
        }

        /** Tiers sans type : invisible partout (règle 1). */
        public UUID tiersSansType(String suffixe) {
            String code = code(suffixe);
            return new Ligne().avec("Code", code).avec("Description", "Tiers " + code).avec("SocialReason", "Tiers " + code)
                    .inserer();
        }

        public UUID lien(UUID tiers, UUID contact, UUID relation) {
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO CfgTierContact (PKey, CfgTierId, CfgContactId, CfgRelationTypeId) VALUES (?, ?, ?, ?)",
                    id.toString(), tiers.toString(), contact.toString(), relation.toString());
            suivre("CfgTierContact", id);
            return id;
        }

        public UUID action(UUID tiers) {
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO ComActionMessage (PKey, CfgTierId) VALUES (?, ?)", id.toString(), tiers.toString());
            suivre("ComActionMessage", id);
            return id;
        }

        /** Crée un client dont {@code CfgCommercialId} pointe {@code tiers} : FK NO_ACTION qui bloque sa suppression. */
        public UUID referencer(UUID tiers) {
            return new Ligne().avec("Code", code("REF")).avec("SocialReason", "Ref " + jeton)
                    .avec("CfgCommercialId", tiers).inserer();
        }

        public String code(String prefixe) {
            return prefixe + "-" + jeton;
        }

        public String codeDe(UUID id) {
            return codes.get(id);
        }

        public String libelleDe(UUID id) {
            return libelle(codes.get(id));
        }

        private String libelle(String code) {
            return "Libelle " + code;
        }

        public String jeton() {
            return jeton;
        }

        public AdmUser admin() {
            return admin;
        }

        public UUID typeId(TypeTiers type) {
            return types.get(type);
        }

        public UUID pays() {
            return pays;
        }

        public UUID devise() {
            return devise;
        }

        public UUID formeMorale() {
            return formeMorale;
        }

        public UUID formePhysique() {
            return formePhysique;
        }

        public UUID titre() {
            return titre;
        }

        public UUID sexe() {
            return sexe;
        }

        public UUID assujettissement() {
            return assujettissement;
        }

        public UUID note() {
            return note;
        }

        private UUID retenir(String table, UUID id, String code) {
            codes.put(id, code);
            suivre(table, id);
            return id;
        }

        private void suivre(String table, UUID id) {
            suivis.push(new String[]{table, id.toString()});
        }

        /** Une ligne {@code CfgTier} : seules les colonnes posées partent dans l'INSERT. */
        public final class Ligne {

            private final Map<String, Object> colonnes = new LinkedHashMap<>();

            private Ligne() {
                colonnes.put("PKey", UUID.randomUUID());
            }

            public Ligne avec(String colonne, Object valeur) {
                colonnes.put(colonne, valeur);
                return this;
            }

            public UUID inserer() {
                List<Object> valeurs = new ArrayList<>();
                colonnes.values().forEach(v -> valeurs.add(enSql(v)));
                String marques = String.join(", ", valeurs.stream().map(v -> "?").toList());
                jdbc.update("INSERT INTO CfgTier (" + String.join(", ", colonnes.keySet()) + ") VALUES (" + marques + ")",
                        valeurs.toArray());
                UUID id = (UUID) colonnes.get("PKey");
                suivre(TABLE_TIERS, id);
                return id;
            }

            private Object enSql(Object valeur) {
                if (valeur instanceof UUID id) {
                    return id.toString();
                }
                return valeur instanceof LocalDateTime date ? Timestamp.valueOf(date) : valeur;
            }
        }
    }
}
