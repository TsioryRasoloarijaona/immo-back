package com.tsix.immocrm.fixtures;

import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Lot L3a (FEATURE-lot-sous-fenetres.md §4) : pièces d'achat du lot (avances, avoirs, remboursements), avenants, échéances et
 * réceptions de retenue de garantie, bordereaux estimatif et définitif groupés, prévisions de paiement. SQL natif, parents avant
 * enfants (FK de la CI) ; annulé avec la transaction du test. Dates liées en {@code Timestamp}.
 */
@Component
public class FixtureLotSousFenetres {

    public static final String MARKET_ADVANCE = "MARKET_ADVANCE";
    public static final String WORKINGOUT_ADVANCE = "WORKINGOUT_ADVANCE";
    public static final String MARKET_WARRANTY = "MARKET_WARRANTY";
    public static final String MARKET_PROPORTION = "MARKET_PROPORTION";
    public static final String DOCUMENT_TO_PAY = "DOCUMENT_TO_PAY";
    public static final String REFUND = "REFUND";
    public static final String INVOICE = "INVOICE";

    // Tables absentes de yesRemsTest et de la CI (définition de yesRemsProd) : créées si absentes, dans la transaction du test.
    private static final String TABLES = """
            IF OBJECT_ID('dbo.PrjMarketWarrantyLine', 'U') IS NULL
                CREATE TABLE dbo.PrjMarketWarrantyLine (PKey uniqueidentifier NOT NULL PRIMARY KEY, Sorting int NULL,
                    Description nvarchar(255) NULL, Delay int NULL, Ratio decimal(26, 6) NULL, EcheanceDate datetime NULL,
                    PrjMarketId uniqueidentifier NULL, UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL,
                    UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL);
            IF OBJECT_ID('dbo.PrjMarketReceptionType', 'U') IS NULL
                CREATE TABLE dbo.PrjMarketReceptionType (PKey uniqueidentifier NOT NULL PRIMARY KEY, sorting int NULL,
                    Code nvarchar(255) NULL, Description nvarchar(255) NULL, UserCreate nvarchar(255) NULL,
                    UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL);
            IF OBJECT_ID('dbo.PrjMarketReceptionKind', 'U') IS NULL
                CREATE TABLE dbo.PrjMarketReceptionKind (PKey uniqueidentifier NOT NULL PRIMARY KEY, sorting int NULL,
                    Code nvarchar(255) NULL, Description nvarchar(255) NULL, UserCreate nvarchar(255) NULL,
                    UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL,
                    IsFinal bit NULL);
            IF OBJECT_ID('dbo.PrjMarketReception', 'U') IS NULL
                CREATE TABLE dbo.PrjMarketReception (PKey uniqueidentifier NOT NULL PRIMARY KEY, sorting int NULL,
                    Code nvarchar(255) NULL, Description nvarchar(255) NULL, DocDate datetime NULL, MeetingPlace nvarchar(255) NULL,
                    RequiredBySupplier bit NULL, PrjMarketId uniqueidentifier NULL, PrjMarketReceptionTypeId uniqueidentifier NULL,
                    PrjMarketReceptionKindId uniqueidentifier NULL, UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL,
                    UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL);
            IF OBJECT_ID('dbo.PrjMarketPaymentPrevision', 'U') IS NULL
                CREATE TABLE dbo.PrjMarketPaymentPrevision (PKey uniqueidentifier NOT NULL PRIMARY KEY, PaymentDate datetime NULL,
                    Amount decimal(26, 6) NULL, Description nvarchar(255) NULL, PrjMarketId uniqueidentifier NULL,
                    Achievement decimal(26, 6) NULL, AmountHT decimal(26, 6) NULL, AmountTTC decimal(26, 6) NULL,
                    StrEntityId uniqueidentifier NULL);
            IF OBJECT_ID('dbo.PrjMarketPrevision', 'U') IS NULL
                CREATE TABLE dbo.PrjMarketPrevision (PKey uniqueidentifier NOT NULL PRIMARY KEY, PointTime decimal(26, 16) NULL,
                    PointDate smalldatetime NULL, PointAchivement decimal(26, 16) NULL, PointPayment decimal(26, 6) NULL,
                    PointAmount decimal(26, 6) NULL, PrjMarketId uniqueidentifier NULL);
            IF OBJECT_ID('dbo.PrjMarketUserPrevision', 'U') IS NULL
                CREATE TABLE dbo.PrjMarketUserPrevision (PKey uniqueidentifier NOT NULL PRIMARY KEY, PointTime decimal(26, 16) NULL,
                    PointDate smalldatetime NULL, PointAchivement decimal(26, 16) NULL, PointPayment decimal(26, 6) NULL,
                    PointAmount decimal(26, 6) NULL, PrjMarketId uniqueidentifier NULL);""";

    private final EntityManager em;
    private final JdbcTemplate jdbc;

    public FixtureLotSousFenetres(EntityManager em, JdbcTemplate jdbc) {
        this.em = em;
        this.jdbc = jdbc;
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

    private static BigDecimal montant(String valeur) {
        return valeur == null ? null : new BigDecimal(valeur);
    }

    /** Les 7 tables des sous-fenêtres, créées si absentes. */
    public void tables() {
        maj(TABLES);
    }

    // ------------------------------------------------------------------ référentiels

    /** Statut de pièce d'achat ({@code BuyDocumentStatus}) ; {@code couleur} = nom .NET ({@code ColorName}). */
    public UUID statutPiece(String code, String description, String couleur) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO BuyDocumentStatus (PKey, Code, Description, Sorting, IsOkForPayement, IsValidatedOnAccounting, ColorName)
                VALUES (?, ?, ?, 1, 0, 0, ?)""", s(id), code, description, couleur);
        return id;
    }

    /** Devise ({@code StlCurrency}) ; code = libellé. */
    public UUID devise(String code) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO StlCurrency (PKey, Code, Description) VALUES (?, ?, ?)", s(id), code, code);
        return id;
    }

    // ------------------------------------------------------------------ pièces d'achat du lot

    /**
     * Pièce d'achat du lot, payable ({@code CanBePayed = 1}) : HT = TTC = à payer, TVA 0, échéance = date. {@code societe} =
     * {@code CfgCompanyId}, {@code projet} = {@code PrjProjectId} de la pièce.
     */
    public UUID piece(UUID lot, UUID projet, UUID societe, String code, String nature, String reference, LocalDate date,
                      String aPayer) {
        UUID id = UUID.randomUUID();
        BigDecimal m = montant(aPayer);
        maj("""
                INSERT INTO BuyDocument (PKey, Code, DocType, ReferenceType, DocDate, MaturityDate, CfgCompanyId, PrjProjectId,
                    PrjMarketId, AmountHT, AmountVat, AmountTTC, AmountToPay, CanBePayed)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, 1)""",
                s(id), code, nature, reference, jour(date), jour(date), s(societe), s(projet), s(lot), m, m, m);
        return id;
    }

    /** Statut, devise et notes de la pièce. */
    public void entete(UUID piece, UUID statut, UUID devise, String notes) {
        maj("UPDATE BuyDocument SET BuyDocumentStatusId = ?, StlCurrencyId = ?, Notes = ? WHERE PKey = ?",
                s(statut), s(devise), notes, s(piece));
    }

    /** Montants HT / TVA / TTC (à payer = TTC). */
    public void montants(UUID piece, String ht, String tva, String ttc) {
        maj("UPDATE BuyDocument SET AmountHT = ?, AmountVat = ?, AmountTTC = ?, AmountToPay = ? WHERE PKey = ?",
                montant(ht), montant(tva), montant(ttc), montant(ttc), s(piece));
    }

    /** Champs propres à l'avoir : personne à contacter, référence bureau d'ordre. */
    public void avoir(UUID piece, String personne, String referenceBureauOrdre) {
        maj("UPDATE BuyDocument SET PersonToContact = ?, OfficeOrderRef = ? WHERE PKey = ?", personne, referenceBureauOrdre, s(piece));
    }

    /** Pièce non payable ({@code CanBePayed = 0}). */
    public void nonPayable(UUID piece) {
        maj("UPDATE BuyDocument SET CanBePayed = 0 WHERE PKey = ?", s(piece));
    }

    /** Échéance ({@code MaturityDate}) seule. */
    public void echeance(UUID piece, LocalDate date) {
        maj("UPDATE BuyDocument SET MaturityDate = ? WHERE PKey = ?", jour(date), s(piece));
    }

    /** Facture d'avenant : {@code BuyDocument.PrjClauseId}. */
    public void rattacherAvenant(UUID piece, UUID avenant) {
        maj("UPDATE BuyDocument SET PrjClauseId = ? WHERE PKey = ?", s(avenant), s(piece));
    }

    // ------------------------------------------------------------------ avenants

    /** Avenant du lot ({@code PrjClause}) ; net à payer distinct du TTC. */
    public UUID avenant(UUID lot, int ordre, String code, LocalDate date, String ht, String tva, String ttc, String netAPayer) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO PrjClause (PKey, Sorting, Code, Description, DocDate, AmountHT, AmountVat, AmountTTC, AmountToPay, PrjMarketId)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                s(id), ordre, code, "Avenant " + code, jour(date), montant(ht), montant(tva), montant(ttc), montant(netAPayer),
                s(lot));
        return id;
    }

    // ------------------------------------------------------------------ retenue de garantie : échéances et réceptions

    /** Échéance de remboursement de RG ({@code PrjMarketWarrantyLine}) ; {@code ratio} en fraction 0–1. */
    public UUID echeanceRg(UUID lot, int ordre, String description, int delai, LocalDate echeance, String ratio) {
        tables();
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO PrjMarketWarrantyLine (PKey, Sorting, Description, Delay, Ratio, EcheanceDate, PrjMarketId)
                VALUES (?, ?, ?, ?, ?, ?, ?)""", s(id), ordre, description, delai, montant(ratio), jour(echeance), s(lot));
        return id;
    }

    public UUID typeReception(String code) {
        tables();
        UUID id = UUID.randomUUID();
        maj("INSERT INTO PrjMarketReceptionType (PKey, sorting, Code, Description) VALUES (?, 1, ?, ?)", s(id), code, "Type " + code);
        return id;
    }

    public UUID genreReception(String code, boolean definitif) {
        tables();
        UUID id = UUID.randomUUID();
        maj("INSERT INTO PrjMarketReceptionKind (PKey, sorting, Code, Description, IsFinal) VALUES (?, 1, ?, ?, ?)",
                s(id), code, "Genre " + code, definitif);
        return id;
    }

    /** Réception du lot ({@code PrjMarketReception}). */
    public UUID reception(UUID lot, int ordre, String code, LocalDate date, boolean demandeeParFournisseur, String lieu,
                          UUID type, UUID genre) {
        tables();
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO PrjMarketReception (PKey, sorting, Code, Description, DocDate, MeetingPlace, RequiredBySupplier,
                    PrjMarketId, PrjMarketReceptionTypeId, PrjMarketReceptionKindId)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                s(id), ordre, code, "Réception " + code, jour(date), lieu, demandeeParFournisseur, s(lot), s(type), s(genre));
        return id;
    }

    // ------------------------------------------------------------------ bordereaux

    /** Groupe du bordereau estimatif ({@code PrjMarketLineGroupingEstimation}) ; {@code parent} nul = racine. */
    public UUID groupeEstimatif(UUID lot, UUID parent, int ordre, String code, String notes) {
        return groupe("PrjMarketLineGroupingEstimation", lot, parent, ordre, code, notes);
    }

    /** Groupe du bordereau définitif ({@code PrjMarketLineGrouping}) ; {@code parent} nul = racine. */
    public UUID groupeDefinitif(UUID lot, UUID parent, int ordre, String code, String notes) {
        return groupe("PrjMarketLineGrouping", lot, parent, ordre, code, notes);
    }

    // table fixe (jamais une saisie) : l'un des deux noms ci-dessus.
    private UUID groupe(String table, UUID lot, UUID parent, int ordre, String code, String notes) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO " + table + " (PKey, Code, Description, Sorting, ParentId, PrjMarketId, IsStock, Notes) "
                + "VALUES (?, ?, ?, ?, ?, ?, 0, ?)", s(id), code, "Groupe " + code, ordre, s(parent), s(lot), notes);
        return id;
    }

    /** Ligne de bordereau ; TVA 20 %, FODEC 0 ; {@code ponderation} = {@code WeightingRatio} stocké, nul possible. */
    public record Ligne(int ordre, String code, String quantite, String puHT, String puTTC, boolean stock, UUID groupe,
                        String ponderation) {
    }

    public UUID ligneEstimative(UUID lot, Ligne l) {
        return ligne("PrjMarketLineEstimation", lot, l);
    }

    public UUID ligneDefinitive(UUID lot, Ligne l) {
        return ligne("PrjMarketLine", lot, l);
    }

    // table fixe (jamais une saisie) : l'un des deux noms ci-dessus.
    private UUID ligne(String table, UUID lot, Ligne l) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO " + table + " (PKey, Code, Description, Sorting, Unity, Quantity, UnitPriceHT, VatRatio, UnitPriceTTC, "
                        + "FODECRatio, IsStock, PrjMarketId, PrjMarketLineGroupingId, WeightingRatio) "
                        + "VALUES (?, ?, ?, ?, N'U', ?, ?, 0.2, ?, 0, ?, ?, ?, ?)",
                s(id), l.code(), "Ligne " + l.code(), l.ordre(), montant(l.quantite()), montant(l.puHT()), montant(l.puTTC()),
                l.stock(), s(lot), s(l.groupe()), montant(l.ponderation()));
        return id;
    }

    // ------------------------------------------------------------------ prévisions

    /** Prévision de paiement ({@code PrjMarketPaymentPrevision}) ; {@code avancement} en fraction 0–1 comme en base. */
    public void prevision(UUID lot, LocalDate date, String montant, String avancement) {
        tables();
        maj("""
                INSERT INTO PrjMarketPaymentPrevision (PKey, PaymentDate, Amount, Description, PrjMarketId, Achievement)
                VALUES (?, ?, ?, N'Prévision de test', ?, ?)""",
                s(UUID.randomUUID()), jour(date), montant(montant), s(lot), montant(avancement));
    }
}
