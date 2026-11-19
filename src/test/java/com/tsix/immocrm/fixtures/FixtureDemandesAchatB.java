package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat.LigneAchat;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat.Cadre;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * N6b (FEATURE-n6-demandes-achat.md R15-R20) : filles de la DIA (présélection, groupes et lignes du bordereau, modalités),
 * offres ({@code BuyDocument} QUOTE), bordereaux du lot. Tables filles absentes ou réduites dans yesRemsTest : créées / complétées
 * dans la transaction du test (même colonnes que la prod). SQL natif, parents avant enfants.
 */
@Component
public class FixtureDemandesAchatB {

    public static final String DROIT_AJOUT_FOURNISSEUR = FixtureDemandesAchat.DROIT_LECTURE + ".toolAddSupplier";
    public static final String DROIT_SUPPRESSION_FOURNISSEUR = FixtureDemandesAchat.DROIT_LECTURE + ".toolDeleteSupplier";
    public static final String DROIT_ECARTER_FOURNISSEUR = FixtureDemandesAchat.DROIT_LECTURE + ".toolSelectDeselectSupplier";
    public static final String DROIT_AJOUT_OFFRE = FixtureDemandesAchat.DROIT_LECTURE + ".toolAddQuote";
    public static final String DROIT_SUPPRESSION_OFFRE = FixtureDemandesAchat.DROIT_LECTURE + ".toolDeleteQuote";
    public static final String DROIT_SELECTION_OFFRE = FixtureDemandesAchat.DROIT_LECTURE + ".toolSelectDeselectQuote";
    public static final String DROIT_BC = "Yes.Buy.Front.Order.OrderList.toolNew";

    /** Tous les droits DIA (N6a + N6b), confidentialité comprise. */
    public static final List<String> TOUS = tous();

    public static final UUID GESTION_DEVIS_LOT = UUID.fromString("15C10DF0-61D0-4A74-AE69-DDDCD4301118");
    public static final UUID GESTION_DEVIS_MULTI = UUID.fromString("39E9AE05-D862-444A-BA77-73D57E09801F");

    private static final String TABLES_FILLES = """
            IF OBJECT_ID('dbo.BuySelectionSupplier', 'U') IS NULL
                CREATE TABLE dbo.BuySelectionSupplier (PKey uniqueidentifier NOT NULL PRIMARY KEY, OfficeOrderRef nvarchar(255) NULL,
                    SelectionLetterSendDate datetime NULL, TenderAcquisitionDate datetime NULL, TenderDepositDate datetime NULL,
                    IsRejected bit NULL, Notes nvarchar(1000) NULL, UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL,
                    UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL, BuySelectionId uniqueidentifier NULL,
                    CfgTierId uniqueidentifier NULL);
            IF OBJECT_ID('dbo.BuySelectionGroup', 'U') IS NULL
                CREATE TABLE dbo.BuySelectionGroup (PKey uniqueidentifier NOT NULL PRIMARY KEY, Sorting int NULL,
                    Code nvarchar(255) NULL, Description nvarchar(255) NULL, Notes nvarchar(1000) NULL, UserCreate nvarchar(255) NULL,
                    UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL,
                    ParentId uniqueidentifier NULL, BuySelectionId uniqueidentifier NULL);
            IF OBJECT_ID('dbo.BuySelectionPaymentTerm', 'U') IS NULL
                CREATE TABLE dbo.BuySelectionPaymentTerm (PKey uniqueidentifier NOT NULL PRIMARY KEY, Sorting int NULL,
                    Code nvarchar(255) NULL, Ratio decimal(26, 6) NULL, UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL,
                    UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL, BuySelectionId uniqueidentifier NULL);
            IF OBJECT_ID('dbo.BuySelectionLine', 'U') IS NULL
                CREATE TABLE dbo.BuySelectionLine (PKey uniqueidentifier NOT NULL PRIMARY KEY, BuySelectionId uniqueidentifier NULL,
                    StlCategoryId uniqueidentifier NULL, BuyItemId uniqueidentifier NULL, BuySelectionGroupId uniqueidentifier NULL);
            IF OBJECT_ID('dbo.BuySelectionStockLine', 'U') IS NULL
                CREATE TABLE dbo.BuySelectionStockLine (PKey uniqueidentifier NOT NULL PRIMARY KEY, BuyItemId uniqueidentifier NULL,
                    BuySelectionId uniqueidentifier NULL);""";

    private static final String COLONNES_LIGNES = """
            IF COL_LENGTH('dbo.%1$s', 'Sorting') IS NULL ALTER TABLE dbo.%1$s ADD Sorting int NULL;
            IF COL_LENGTH('dbo.%1$s', 'Code') IS NULL ALTER TABLE dbo.%1$s ADD Code nvarchar(255) NULL;
            IF COL_LENGTH('dbo.%1$s', 'Description') IS NULL ALTER TABLE dbo.%1$s ADD Description nvarchar(255) NULL;
            IF COL_LENGTH('dbo.%1$s', 'Unity') IS NULL ALTER TABLE dbo.%1$s ADD Unity nvarchar(%2$s) NULL;
            IF COL_LENGTH('dbo.%1$s', 'Quantity') IS NULL ALTER TABLE dbo.%1$s ADD Quantity decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.%1$s', 'UnitPriceHT') IS NULL ALTER TABLE dbo.%1$s ADD UnitPriceHT decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.%1$s', 'FODECRatio') IS NULL ALTER TABLE dbo.%1$s ADD FODECRatio decimal(26, 16) NULL;
            IF COL_LENGTH('dbo.%1$s', 'VatRatio') IS NULL ALTER TABLE dbo.%1$s ADD VatRatio decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.%1$s', 'UnitPriceTTC') IS NULL ALTER TABLE dbo.%1$s ADD UnitPriceTTC decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.%1$s', 'Notes') IS NULL ALTER TABLE dbo.%1$s ADD Notes nvarchar(%3$s) NULL;
            IF COL_LENGTH('dbo.%1$s', 'UserCreate') IS NULL ALTER TABLE dbo.%1$s ADD UserCreate nvarchar(255) NULL;
            IF COL_LENGTH('dbo.%1$s', 'UserCreateDateTime') IS NULL ALTER TABLE dbo.%1$s ADD UserCreateDateTime datetime NULL;
            IF COL_LENGTH('dbo.%1$s', 'UserUpdate') IS NULL ALTER TABLE dbo.%1$s ADD UserUpdate nvarchar(255) NULL;
            IF COL_LENGTH('dbo.%1$s', 'UserUpdateDateTime') IS NULL ALTER TABLE dbo.%1$s ADD UserUpdateDateTime datetime NULL;""";

    private final EntityManager em;
    private final JdbcTemplate jdbc;
    private final FixtureDemandesAchat demandes;
    private final FixtureAchat achat;
    private final FixtureLotFiche lotFiche;
    private final FixtureLotsParite parite;

    public FixtureDemandesAchatB(EntityManager em, JdbcTemplate jdbc, FixtureDemandesAchat demandes, FixtureAchat achat,
                                 FixtureLotFiche lotFiche, FixtureLotsParite parite) {
        this.em = em;
        this.jdbc = jdbc;
        this.demandes = demandes;
        this.achat = achat;
        this.lotFiche = lotFiche;
        this.parite = parite;
    }

    private static List<String> tous() {
        List<String> droits = new ArrayList<>(FixtureDemandesAchat.TOUS_LES_DROITS);
        droits.addAll(List.of(DROIT_AJOUT_FOURNISSEUR, DROIT_SUPPRESSION_FOURNISSEUR, DROIT_ECARTER_FOURNISSEUR, DROIT_AJOUT_OFFRE,
                DROIT_SUPPRESSION_OFFRE, DROIT_SELECTION_OFFRE, DROIT_BC));
        return List.copyOf(droits);
    }

    /** Tous les droits sauf {@code retire}. */
    public static List<String> sauf(String retire) {
        return TOUS.stream().filter(d -> !d.equals(retire)).toList();
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

    // ------------------------------------------------------------------ cadre

    /** Cadre N6a + tables filles + gestions de devis (PKeys de prod) ; les 4 phases ouvertes sur Préparation. */
    public Cadre cadre(String prefixe) {
        Cadre c = demandes.cadre(prefixe);
        tablesFilles();
        gestionsDevis(c.jeton());
        demandes.phases(c.prep(), true, true, true, true);
        return c;
    }

    public void tablesFilles() {
        maj(TABLES_FILLES);
        maj(COLONNES_LIGNES.formatted("BuySelectionLine", "50", "1000"));
        maj(COLONNES_LIGNES.formatted("BuySelectionStockLine", "255", "255"));
    }

    // Séquences « Devis entrant »…, devise, type de marché, gestion BuyQuote (configuration A2b) ; gestions de devis du lot.
    private void gestionsDevis(String jeton) {
        achat.configurationSaisie(jeton);
        achat.gestionVide("PrjMarketQuote", GESTION_DEVIS_LOT);
        achat.gestionVide("PrjMarketMultiSupplierQuote", GESTION_DEVIS_MULTI);
        em.flush();
    }

    /** Compte libre habilité, portant tous les droits DIA. */
    public AdmUser libre(Cadre c, String code) {
        return demandes.libre(c, code, TOUS);
    }

    // ------------------------------------------------------------------ présélection

    /** Fournisseur présélectionné (non écarté) ; {@code depot} nul possible ; rend la PKey de {@code BuySelectionSupplier}. */
    public UUID preselection(UUID demande, UUID tiers, LocalDate depot) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO BuySelectionSupplier (PKey, BuySelectionId, CfgTierId, OfficeOrderRef, SelectionLetterSendDate,
                    TenderAcquisitionDate, TenderDepositDate, IsRejected, Notes)
                VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?)""",
                s(id), s(demande), s(tiers), "ODT-" + id.toString().substring(0, 6), jour(LocalDate.of(2026, 3, 1)),
                jour(LocalDate.of(2026, 3, 2)), jour(depot), "Présélection");
        return id;
    }

    public void ecarter(UUID preselection) {
        maj("UPDATE BuySelectionSupplier SET IsRejected = 1 WHERE PKey = ?", s(preselection));
    }

    // ------------------------------------------------------------------ offres

    /** Offre ({@code BuyDocument} QUOTE) du tiers sur la DIA ; {@code IsRejected} 0. */
    public UUID offre(UUID demande, UUID tiers, String code, boolean selectionnee) {
        UUID id = demandes.pieceLiee(demande, code);
        achat.poser(id, "CfgTierId", tiers);
        achat.poser(id, "IsSelected", selectionnee);
        achat.poser(id, "IsRejected", false);
        return id;
    }

    /** Pièce non devis ({@code DocType} donné) liée à la DIA. */
    public UUID pieceNonDevis(UUID demande, String code, String type) {
        UUID id = demandes.pieceLiee(demande, code);
        achat.poser(id, "DocType", type);
        return id;
    }

    /** Ligne d'offre ; {@code remise} en fraction ; PU HT après remise = {@code puHT × (1 − remise)}. */
    public UUID ligneOffre(UUID offre, int ordre, String code, String puHT, String remise) {
        BigDecimal pu = new BigDecimal(puHT);
        return achat.ligne(offre, new LigneAchat(ordre, code, "Offre " + code, "U", BigDecimal.ONE, pu, new BigDecimal("0.2"),
                new BigDecimal(remise), BigDecimal.ZERO, pu.multiply(new BigDecimal("1.2"))));
    }

    // ------------------------------------------------------------------ bordereau de la DIA

    public UUID groupe(UUID demande, String code, UUID parent, int ordre) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO BuySelectionGroup (PKey, Sorting, Code, Description, ParentId, BuySelectionId)
                VALUES (?, ?, ?, ?, ?, ?)""", s(id), ordre, code, "Groupe " + code, s(parent), s(demande));
        return id;
    }

    /** Ligne du bordereau de la DIA : quantité 2, unité « M2 », TVA 0,2, FODEC 0. */
    public UUID ligne(UUID demande, UUID groupe, int ordre, String code, String puHT, UUID categorie) {
        UUID id = UUID.randomUUID();
        BigDecimal pu = new BigDecimal(puHT);
        maj("""
                INSERT INTO BuySelectionLine (PKey, Sorting, Code, Description, Unity, Quantity, UnitPriceHT, FODECRatio, VatRatio,
                    UnitPriceTTC, BuySelectionId, StlCategoryId, BuySelectionGroupId)
                VALUES (?, ?, ?, ?, N'M2', 2, ?, 0, 0.2, ?, ?, ?, ?)""",
                s(id), ordre, code, "Ligne " + code, pu, pu.multiply(new BigDecimal("1.2")), s(demande), s(categorie), s(groupe));
        return id;
    }

    // ------------------------------------------------------------------ modalités

    public UUID modalite(UUID demande, int ordre, String code, String ratio) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO BuySelectionPaymentTerm (PKey, Sorting, Code, Ratio, BuySelectionId) VALUES (?, ?, ?, ?, ?)",
                s(id), ordre, code, new BigDecimal(ratio), s(demande));
        return id;
    }

    // ------------------------------------------------------------------ lot

    /** Lot du projet du cadre, rattaché à la DIA ({@code BuySelection.PrjMarketId}). */
    public UUID lotDe(Cadre c, UUID demande, String code, boolean multi) {
        UUID lot = demandes.lot(c, code, multi);
        demandes.poser(FixtureDemandesAchat.DEMANDE, demande, "PrjMarketId", lot);
        return lot;
    }

    /** {@code BoolField005} (« Avec DIA ») du statut courant du lot. */
    public void lotAvecDia(UUID lot, boolean avecDia) {
        maj("UPDATE StrStatus SET BoolField005 = ? WHERE PKey = (SELECT StrStatusId FROM PrjMarket WHERE PKey = ?)",
                avecDia, s(lot));
    }

    /** Groupe du bordereau définitif du lot ({@code PrjMarketLineGrouping}). */
    public UUID groupeDefinitif(UUID lot, String code) {
        return lotFiche.groupeBordereau(lot, code);
    }

    /** Ligne du bordereau définitif du lot ({@code PrjMarketLine}). */
    public UUID ligneDefinitive(UUID lot, String code) {
        return parite.ligneBordereau(lot, code);
    }

    /** Décompte du lot dont une ligne cite la ligne définitive ({@code PrjWorkingOutLine.PrjMarketLineId}, NO_ACTION). */
    public UUID decompteCitant(UUID lot, UUID ligneDefinitive, String code) {
        UUID decompte = parite.decompteDuLot(lot, code);
        maj("""
                INSERT INTO PrjWorkingOutLine (Pkey, Sorting, Code, Description, Quantity, UnitPriceHT, VatRatio, UnitPriceTTC,
                    PrjWorkingOutId, PrjMarketLineId) VALUES (?, 1, ?, ?, 1, 100, 0.2, 120, ?, ?)""",
                s(UUID.randomUUID()), code, code, s(decompte), s(ligneDefinitive));
        return decompte;
    }

    /** Groupe du bordereau estimatif du lot ; {@code parent} nul à la racine. */
    public UUID groupeEstimatif(UUID lot, String code, UUID parent, int ordre) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO PrjMarketLineGroupingEstimation (PKey, Code, Description, Sorting, ParentId, PrjMarketId, IsStock)
                VALUES (?, ?, ?, ?, ?, ?, 0)""", s(id), code, "Groupe " + code, ordre, s(parent), s(lot));
        return id;
    }

    /** Ligne du bordereau estimatif du lot (quantité 3, TVA 0,2, FODEC 0). */
    public UUID ligneEstimative(UUID lot, UUID groupe, int ordre, String code, String puHT, UUID categorie) {
        UUID id = UUID.randomUUID();
        BigDecimal pu = new BigDecimal(puHT);
        maj("""
                INSERT INTO PrjMarketLineEstimation (PKey, Code, Description, Sorting, Unity, Quantity, UnitPriceHT, VatRatio,
                    UnitPriceTTC, FODECRatio, IsStock, PrjMarketId, PrjMarketLineGroupingId, StlCategoryId)
                VALUES (?, ?, ?, ?, N'U', 3, ?, 0.2, ?, 0, 0, ?, ?, ?)""",
                s(id), code, "Estimation " + code, ordre, pu, pu.multiply(new BigDecimal("1.2")), s(lot), s(groupe),
                s(categorie));
        return id;
    }

    // ------------------------------------------------------------------ lecture

    /** Lecture après une réponse 2xx : la session est écrite avant. */
    public List<Map<String, Object>> lignes(String sql, Object... parametres) {
        em.flush();
        return jdbc.queryForList(sql, parametres);
    }

    /** Lecture après un refus : la session est vidée sans écriture (rien de l'appel refusé ne doit partir en base). */
    public List<Map<String, Object>> lignesApresRefus(String sql, Object... parametres) {
        em.clear();
        return jdbc.queryForList(sql, parametres);
    }

    public int compter(String sql, Object... parametres) {
        em.flush();
        Integer n = jdbc.queryForObject(sql, Integer.class, parametres);
        return n == null ? 0 : n;
    }

    public int compterApresRefus(String sql, Object... parametres) {
        em.clear();
        Integer n = jdbc.queryForObject(sql, Integer.class, parametres);
        return n == null ? 0 : n;
    }
}
