package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.fixtures.FixtureTiers.Jeu;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Compléments de {@link FixtureTiers} pour banques et organismes (FEATURE-admin-banques-organismes.md). */
@Component
public class FixtureBanques {

    public static final String PARAMETRE_BANQUE = "DefaultSocialFormForBANK";
    public static final String PARAMETRE_ORGANISME = "DefaultSocialFormForORGANISM";

    // Absente de yesRemsTest, présente en CI : définition de ci/schema/010_tables.sql.
    private static final String TABLE_JOURNAL = """
            IF OBJECT_ID('dbo.ActJournal', 'U') IS NULL
            CREATE TABLE dbo.ActJournal (
                PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(255) NULL, Description nvarchar(255) NULL,
                Notes nvarchar(500) NULL, ActJournalTypeId uniqueidentifier NULL, UserCreate nvarchar(255) NULL,
                UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL,
                StrEntityId uniqueidentifier NULL, CfgCompanyId uniqueidentifier NULL)""";

    private static final String COLONNES = """
            SELECT IDNumber, FiscalCode, FiscalRegistry, FiscalDouane, ICE, Address, Address2, Address3, ZipCode, City,
                   Phone, Phone1, Phone2, Fax, WebSite, MobileIndicatif, PhoneIndicatif, Phone1Indicatif, Phone2Indicatif,
                   FaxIndicatif, Rib, BankAgency, CodeCountable,
                   LOWER(CONVERT(varchar(36), AdmCountryId)) AS AdmCountryId,
                   LOWER(CONVERT(varchar(36), AdmCityId)) AS AdmCityId,
                   LOWER(CONVERT(varchar(36), ActJournalId)) AS ActJournalId
            FROM CfgTier WHERE PKey = ?""";

    private final FixtureTiers tiers;
    private final JdbcTemplate jdbc;
    private final PlatformTransactionManager transactionManager;

    public FixtureBanques(FixtureTiers tiers, DataSource dataSource, PlatformTransactionManager transactionManager) {
        this.tiers = tiers;
        this.jdbc = new JdbcTemplate(dataSource);
        this.transactionManager = transactionManager;
    }

    /** Jeu du socle ; la table des journaux est validée à part, un rollback de test ne la retire pas. */
    public Jeu jeu() {
        TransactionTemplate propre = new TransactionTemplate(transactionManager);
        propre.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        propre.executeWithoutResult(statut -> jdbc.execute(TABLE_JOURNAL));
        return tiers.jeu();
    }

    /** Écriture réduite minimale : les seuls champs requis, modifiable par le test. */
    public static Map<String, Object> corps(Jeu j, String code, String description) {
        Map<String, Object> corps = new HashMap<>();
        corps.put("code", code);
        corps.put("description", description);
        corps.put("paysId", j.pays());
        return corps;
    }

    /** {@link FixtureTiers#ligne} plus les colonnes de l'écriture réduite. */
    public Map<String, Object> colonnes(UUID id) {
        Map<String, Object> ligne = new HashMap<>(tiers.ligne(id));
        ligne.putAll(jdbc.queryForMap(COLONNES, id.toString()));
        return ligne;
    }

    /** Dans un test {@code @Transactional} seulement : paramètre OMRAN de forme sociale, {@code null} = ligne absente. */
    public void formeParDefaut(String parametre, UUID forme) {
        jdbc.update("DELETE FROM AdmParamsGuid WHERE Code = ?", parametre);
        if (forme != null) {
            jdbc.update("INSERT INTO AdmParamsGuid (Code, CodeValue, PKey) VALUES (?, ?, ?)", parametre, forme.toString(),
                    UUID.randomUUID().toString());
        }
    }

    /** Dans un test {@code @Transactional} seulement (DDL annulé au rollback) : FK NO_ACTION de la CI vers {@code CfgTier}. */
    public void poserCle(String table, String colonne) {
        jdbc.execute("""
                IF NOT EXISTS (SELECT 1 FROM sys.foreign_key_columns
                               WHERE parent_object_id = OBJECT_ID('dbo.%1$s')
                                 AND referenced_object_id = OBJECT_ID('dbo.CfgTier')
                                 AND parent_column_id = COLUMNPROPERTY(OBJECT_ID('dbo.%1$s'), '%2$s', 'ColumnId'))
                ALTER TABLE dbo.%1$s WITH NOCHECK ADD CONSTRAINT FK_TEST_%1$s_%2$s
                    FOREIGN KEY (%2$s) REFERENCES dbo.CfgTier (PKey)""".formatted(table, colonne));
    }

    public void compteBancaire(UUID banque) {
        jdbc.update("INSERT INTO StlAccount (PKey, CfgBankId) VALUES (?, ?)", UUID.randomUUID().toString(), banque.toString());
    }

    public void dossierACredit(Jeu j, UUID banque) {
        jdbc.update("INSERT INTO ComFolder (PKey, StkVocationId, CreditBankId) VALUES (?, ?, ?)",
                UUID.randomUUID().toString(), j.parent("StkVocation").toString(), banque.toString());
    }
}
