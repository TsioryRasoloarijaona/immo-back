package com.tsix.immocrm.fixtures;

import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lot L3b-2a (FEATURE-lot-avenants-avances.md) : avenants et leurs lignes, factures d'avenant, séquence « Avenant », réserves et
 * tiers invités des réceptions. SQL natif, parents avant enfants (FK de la CI) ; annulé avec la transaction du test, sauf les
 * tables créées par {@link #tables()} (DDL hors transaction, idempotent).
 */
@Component
public class FixtureLotAvenants {

    public static final String AVENANTS = "PrjClause";
    public static final String LIGNES_AVENANT = "PrjClauseLine";
    public static final String ECHEANCES = "PrjMarketWarrantyLine";
    public static final String RECEPTIONS = "PrjMarketReception";
    public static final String RESERVES = "PrjMarketReceptionReserve";
    public static final String TIERS_INVITES = "PrjMarketReceptionCfgTier";

    /** Ligne {@code AdmSequence} des avenants en prod ({@code Sequence.cs} l.411, {@code Code = 'Avenant'}). */
    public static final UUID SEQUENCE_AVENANT_ID = UUID.fromString("50C4F768-1478-5897-1245-F784D28F2011");
    public static final String SEQUENCE_AVENANT = "Avenant";

    // Absentes de yesRemsTest et de la CI avant le lot (colonnes de yesRemsProd, casse « Pkey » de PrjClauseLine comprise).
    public static final String TABLES = """
            IF OBJECT_ID('dbo.PrjClauseLine', 'U') IS NULL
                CREATE TABLE dbo.PrjClauseLine (Pkey uniqueidentifier NOT NULL PRIMARY KEY, Sorting int NULL,
                    Code nvarchar(255) NULL, Description nvarchar(500) NULL, Unity nvarchar(255) NULL, Quantity decimal(26, 6) NULL,
                    UnitPriceHT decimal(26, 6) NULL, VatRatio decimal(26, 6) NULL, UnitPriceTTC decimal(26, 6) NULL,
                    Notes nvarchar(1000) NULL, PrjClauseId uniqueidentifier NULL, UserCreate nvarchar(255) NULL,
                    UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL,
                    StlCategoryId uniqueidentifier NULL, PrjMarketLineId uniqueidentifier NULL, FODECRatio decimal(26, 16) NULL,
                    BuyConsultationLineId uniqueidentifier NULL);
            IF OBJECT_ID('dbo.PrjMarketReceptionReserve', 'U') IS NULL
                CREATE TABLE dbo.PrjMarketReceptionReserve (PKey uniqueidentifier NOT NULL PRIMARY KEY, sorting int NULL,
                    Description nvarchar(255) NULL, IsFixed bit NULL, PrjMarketReceptionId uniqueidentifier NULL,
                    UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL,
                    UserUpdateDateTime datetime NULL);
            IF OBJECT_ID('dbo.PrjMarketReceptionCfgTier', 'U') IS NULL
                CREATE TABLE dbo.PrjMarketReceptionCfgTier (PKey uniqueidentifier NOT NULL PRIMARY KEY, sorting int NULL,
                    PrjMarketReceptionId uniqueidentifier NULL, CfgTierId uniqueidentifier NULL);""";

    private final EntityManager em;
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;
    private final FixtureLotSousFenetres sousFenetres;

    public FixtureLotAvenants(EntityManager em, JdbcTemplate jdbc, DataSource dataSource, FixtureLotSousFenetres sousFenetres) {
        this.em = em;
        this.jdbc = jdbc;
        this.dataSource = dataSource;
        this.sousFenetres = sousFenetres;
    }

    private void maj(String sql, Object... parametres) {
        em.flush();
        jdbc.update(sql, parametres);
        em.clear();
    }

    private static String s(UUID id) {
        return id == null ? null : id.toString();
    }

    private static Timestamp jour(LocalDate date) {
        return date == null ? null : Timestamp.valueOf(date.atStartOfDay());
    }

    private static BigDecimal n(String valeur) {
        return valeur == null ? null : new BigDecimal(valeur);
    }

    /**
     * Les 3 tables absentes, créées hors transaction (connexion propre, autocommit : un DDL dans la transaction du test serait
     * annulé pour les tests validés), puis les tables des sous-fenêtres L3a. À appeler en tête du {@code setUp}.
     */
    public void tables() {
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            c.setAutoCommit(true);
            st.execute(TABLES);
        } catch (SQLException e) {
            throw new IllegalStateException("Création des tables du lot avenants", e);
        }
        sousFenetres.tables();
    }

    /** Ligne {@code AdmSequence} « Avenant » (format de prod {@code yy/nnnnn}, année système) posée sur {@code valeur}. */
    public void sequenceAvenant(int valeur) {
        maj("""
                IF EXISTS (SELECT 1 FROM AdmSequence WHERE Code = ?)
                    UPDATE AdmSequence SET SeqValue = ?, SeqYearNumber = 2, SeqValueNumber = 5, Separator = N'/',
                        BasedOnSystemYear = 1, SeqPrefix = NULL, SeqSuffix = NULL WHERE Code = ?
                ELSE
                    INSERT INTO AdmSequence (PKey, Code, SeqValue, SeqYearNumber, SeqValueNumber, Separator, BasedOnSystemYear)
                    VALUES (?, ?, ?, 2, 5, N'/', 1)""",
                SEQUENCE_AVENANT, valeur, SEQUENCE_AVENANT, s(SEQUENCE_AVENANT_ID), SEQUENCE_AVENANT, valeur);
    }

    /** {@code PrjMarket.EndDate} (date de fin du lot, repli de la date de réception définitive). */
    public void finDuLot(UUID lot, LocalDate fin) {
        maj("UPDATE PrjMarket SET EndDate = ? WHERE PKey = ?", jour(fin), s(lot));
    }

    /** Les 4 taux de retenue du lot (fractions 0–1). */
    public void tauxDuLot(UUID lot, String garantie, String prorata, String dec, String trc) {
        maj("UPDATE PrjMarket SET WarrantyRatio = ?, ProportionRatio = ?, InsuranceDECRatio = ?, InsuranceTRCRatio = ? WHERE PKey = ?",
                n(garantie), n(prorata), n(dec), n(trc), s(lot));
    }

    // ------------------------------------------------------------------ avenants

    /** Montants stockés d'un avenant ; garantie au taux ({@code ratio} fraction) et montant stockés, autres retenues à 0. */
    public record Avenant(UUID lot, int ordre, String code, LocalDate date, String ht, String tva, String ttc, String net,
                          String ratioGarantie, String montantGarantie) {
    }

    /** Avenant ({@code PrjClause}) avec ses colonnes de retenue ; description « Avenant N° code ». */
    public UUID avenant(Avenant a) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO PrjClause (PKey, Sorting, Code, Description, DocDate, DocRef, Notes, AmountHT, AmountVat, AmountTTC,
                    AmountToPay, WarrantyBasedOnAmount, WarrantyRatio, WarrantyAmount, DECBasedOnAmount, InsuranceDECRatio,
                    InsuranceDECAmount, TRCBasedOnAmount, InsuranceTRCRatio, InsuranceTRCAmount, ProrataBasedOnAmount, ProportionRatio,
                    ProportionAmount, PrjMarketId)
                VALUES (?, ?, ?, ?, ?, N'REF', N'Notes', ?, ?, ?, ?, 0, ?, ?, 0, 0, 0, 0, 0, 0, 0, 0, 0, ?)""",
                s(id), a.ordre(), a.code(), "Avenant N° " + a.code(), jour(a.date()), n(a.ht()), n(a.tva()), n(a.ttc()),
                n(a.net()), n(a.ratioGarantie()), n(a.montantGarantie()), s(a.lot()));
        return id;
    }

    /** Ligne d'avenant : désignation « ART code », unité U, FODEC 0, PU TTC = PU × (1 + TVA), sans notes. */
    public UUID ligneAvenant(UUID avenant, int ordre, String code, String quantite, String puHT, String tvaRatio, UUID categorie,
                             UUID ligneBordereau) {
        UUID id = UUID.randomUUID();
        BigDecimal pu = n(puHT);
        maj("""
                INSERT INTO PrjClauseLine (Pkey, Sorting, Code, Description, Unity, Quantity, UnitPriceHT, VatRatio, UnitPriceTTC,
                    FODECRatio, PrjClauseId, StlCategoryId, PrjMarketLineId)
                VALUES (?, ?, ?, ?, N'U', ?, ?, ?, ?, 0, ?, ?, ?)""",
                s(id), ordre, code, "ART " + code, n(quantite), pu, n(tvaRatio), pu.multiply(BigDecimal.ONE.add(n(tvaRatio))),
                s(avenant), s(categorie), s(ligneBordereau));
        return id;
    }

    /** Facture ({@code INVOICE}) rattachée à l'avenant par {@code BuyDocument.PrjClauseId} : l'avenant devient facturé. */
    public UUID facturer(UUID avenant, UUID lot, UUID projet, UUID societe, String code) {
        UUID facture = sousFenetres.piece(lot, projet, societe, code, FixtureLotSousFenetres.INVOICE, null,
                LocalDate.of(2026, 4, 1), "100");
        sousFenetres.rattacherAvenant(facture, avenant);
        return facture;
    }

    // ------------------------------------------------------------------ réceptions : réserves et tiers invités

    public UUID reserve(UUID reception, int ordre, String description, boolean levee) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO PrjMarketReceptionReserve (PKey, sorting, Description, IsFixed, PrjMarketReceptionId) VALUES (?, ?, ?, ?, ?)",
                s(id), ordre, description, levee, s(reception));
        return id;
    }

    public UUID tiersInvite(UUID reception, int ordre, UUID tiers) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO PrjMarketReceptionCfgTier (PKey, sorting, PrjMarketReceptionId, CfgTierId) VALUES (?, ?, ?, ?)",
                s(id), ordre, s(reception), s(tiers));
        return id;
    }

    // ------------------------------------------------------------------ lecture (session écrite avant)

    public List<Map<String, Object>> lignes(String sql, Object... parametres) {
        em.flush();
        em.clear();
        return jdbc.queryForList(sql, parametres);
    }

    /** Ligne de clé {@code id} de {@code table} (colonne {@code PKey}, {@code Pkey} pour {@code PrjClauseLine}), ou vide. */
    public List<Map<String, Object>> parId(String table, UUID id) {
        String cle = LIGNES_AVENANT.equals(table) ? "Pkey" : "PKey";
        return lignes("SELECT * FROM " + table + " WHERE " + cle + " = ?", s(id));
    }

    public int compter(String sql, Object... parametres) {
        em.flush();
        Integer total = jdbc.queryForObject(sql, Integer.class, parametres);
        return total == null ? 0 : total;
    }
}
