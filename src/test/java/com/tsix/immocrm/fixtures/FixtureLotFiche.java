package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lot L2a (FEATURE-lot-fiche-parite.md E1, E5, E6, E7, E8) : colonnes de fiche de {@code PrjMarket}, devise, type de règlement,
 * demandes internes d'achat ({@code BuySelection}), groupes de bordereau, comptes Administrateur restreints, statut en lecture
 * seule. SQL natif, parents avant enfants (FK de la CI) ; annulé avec la transaction du test. Dates liées en {@code Timestamp}.
 */
@Component
public class FixtureLotFiche {

    public static final String DROIT_ESTIMATION = "Yes.Prj.Front.Market.MarketMonitoring.ManualEstimationAdministrator";

    private static final String ADMINISTRATEUR = "Administrateur";

    private final EntityManager em;
    private final JdbcTemplate jdbc;
    private final FixtureDossierVente vente;
    private final FixtureProjetLots lots;

    public FixtureLotFiche(EntityManager em, JdbcTemplate jdbc, FixtureDossierVente vente, FixtureProjetLots lots) {
        this.em = em;
        this.jdbc = jdbc;
        this.vente = vente;
        this.lots = lots;
    }

    private void maj(String sql, Object... parametres) {
        em.flush();
        jdbc.update(sql, parametres);
        em.clear();
    }

    private static String s(UUID id) {
        return id == null ? null : id.toString();
    }

    public static Timestamp jour(LocalDate date) {
        return date == null ? null : Timestamp.valueOf(date.atStartOfDay());
    }

    // ------------------------------------------------------------------ référentiels

    /** Devise ({@code StlCurrency}) ; code = libellé. */
    public UUID devise(String code) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO StlCurrency (PKey, Code, Description) VALUES (?, ?, ?)", s(id), code, code);
        return id;
    }

    /** Type de règlement ({@code StlSettlementType}) ; code = libellé. */
    public UUID typePaiement(String code) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO StlSettlementType (PKey, Code, Description) VALUES (?, ?, ?)", s(id), code, code);
        return id;
    }

    // ------------------------------------------------------------------ colonnes de fiche (E1)

    /** En-tête : devise, catégorie, estimation de base, délai prévisionnel. */
    public void entete(UUID lot, UUID devise, UUID categorie, LocalDate debutBase, int delaiBase, LocalDate finBase, int delai) {
        maj("""
                UPDATE PrjMarket SET StlCurrencyId = ?, StlCategoryId = ?, StartDateManual = ?, DelayManual = ?, EndDateManual = ?,
                    Delay = ? WHERE PKey = ?""",
                s(devise), s(categorie), jour(debutBase), delaiBase, jour(finBase), delai, s(lot));
    }

    /** Onglet Dates : les 4 dates qu'E1 ajoute. */
    public void datesNouvelles(UUID lot, LocalDate report, LocalDate rapport, LocalDate achevement, LocalDate recollement) {
        maj("""
                UPDATE PrjMarket SET TendersResortDate = ?, TendersReportDate = ?, WorkCompletionDate = ?, ReattachmentDate = ?
                WHERE PKey = ?""", jour(report), jour(rapport), jour(achevement), jour(recollement), s(lot));
    }

    /** Onglet Options : ratios en 0–1 comme en base. */
    public void options(UUID lot, BigDecimal variationMasse, BigDecimal remboursementAppro, int totalPoints, UUID typePaiement) {
        maj("""
                UPDATE PrjMarket SET MassChangeRatio = ?, StockPileRatio = ?, TotalPoints = ?, StlSettlementTypeId = ?,
                    ApplyDiscountOnAdditionalWork = 1, ApplyWithholdingTaxOnAdditionalWork = 1, ExecutedQuantityBasedInsert = 1,
                    CanEditWorkingOutLineByAchivement = 1
                WHERE PKey = ?""", variationMasse, remboursementAppro, totalPoints, s(typePaiement), s(lot));
    }

    /** Onglets Résiliation et Autres. */
    public void resiliationEtAutres(UUID lot, LocalDate resiliation, String cause, String descriptionLongue, String notes) {
        maj("UPDATE PrjMarket SET CancellationDate = ?, CancellationCause = ?, DescriptionLong = ?, Notes = ? WHERE PKey = ?",
                jour(resiliation), cause, descriptionLongue, notes, s(lot));
    }

    /** Taux d'avance ({@code AdvanceRatio}, 0–1) et date de signature du marché. */
    public void avanceEtSignature(UUID lot, BigDecimal tauxAvance, LocalDate signature) {
        maj("UPDATE PrjMarket SET AdvanceRatio = ?, ContractSignatureDate = ? WHERE PKey = ?", tauxAvance, jour(signature), s(lot));
    }

    /** Date de signature du marché seule ({@code ContractSignatureDate}). */
    public void signature(UUID lot, LocalDate signature) {
        maj("UPDATE PrjMarket SET ContractSignatureDate = ? WHERE PKey = ?", jour(signature), s(lot));
    }

    /** Dates prévisionnelles (verrouillées dès que le lot existe, E6). */
    public void previsionnel(UUID lot, LocalDate debut, int delai, LocalDate fin) {
        maj("UPDATE PrjMarket SET StartDate = ?, Delay = ?, EndDate = ? WHERE PKey = ?", jour(debut), delai, jour(fin), s(lot));
    }

    public void fournisseurEtType(UUID lot, UUID fournisseur, UUID type) {
        maj("UPDATE PrjMarket SET CfgSupplierId = ?, PrjMarketTypeId = ? WHERE PKey = ?", s(fournisseur), s(type), s(lot));
    }

    /** Lot clôturé ({@code IsClosed = 1}). */
    public void clore(UUID lot) {
        maj("UPDATE PrjMarket SET IsClosed = 1 WHERE PKey = ?", s(lot));
    }

    /** {@code StrStatus.IsReadOnly = 1} (R11). */
    public void lectureSeule(UUID statut) {
        maj("UPDATE StrStatus SET IsReadOnly = 1 WHERE PKey = ?", s(statut));
    }

    // ------------------------------------------------------------------ comptes (E6, E7)

    /**
     * Compte au rôle « Administrateur » (code du rôle et du jeton), mais restreint : {@code WithSequenceFreeRestriction = 1}, ou
     * NULL si {@code drapeauNul} (lu comme restreint). Habilité sur ces projets.
     */
    public AdmUser administrateurRestreint(String code, boolean drapeauNul, UUID... projets) {
        AdmUser compte = vente.compte(code, ADMINISTRATEUR);
        em.flush();
        if (drapeauNul) {
            maj("UPDATE AdmRole SET WithSequenceFreeRestriction = NULL WHERE PKey IN (SELECT AdmRoleId FROM AdmUserRole WHERE AdmUserId = ?)",
                    s(compte.getId()));
        } else {
            maj("UPDATE AdmRole SET WithSequenceFreeRestriction = 1 WHERE PKey IN (SELECT AdmRoleId FROM AdmUserRole WHERE AdmUserId = ?)",
                    s(compte.getId()));
        }
        for (UUID projet : projets) {
            lots.habiliter(compte, projet);
        }
        return compte;
    }

    /** Ajoute au compte un second rôle propre, exempté ({@code WithSequenceFreeRestriction = 0}). */
    public UUID roleLibreEnPlus(AdmUser compte, String code) {
        UUID role = UUID.randomUUID();
        maj("INSERT INTO AdmRole (PKey, Code, Description, WithSequenceFreeRestriction) VALUES (?, ?, ?, 0)", s(role), code, code);
        maj("INSERT INTO AdmUserRole (PKey, AdmUserId, AdmRoleId) VALUES (?, ?, ?)", s(UUID.randomUUID()), s(compte.getId()), s(role));
        return role;
    }

    /** Rôles du compte ({@code AdmUserRole}). */
    public List<UUID> rolesDe(AdmUser compte) {
        em.flush();
        return jdbc.queryForList("SELECT CAST(AdmRoleId AS varchar(36)) FROM AdmUserRole WHERE AdmUserId = ?", String.class,
                s(compte.getId())).stream().map(UUID::fromString).toList();
    }

    /** Intervenant « compte » ({@code StrIntervener.AdmUserId}) sur la transition, sans rôle. */
    public void intervenantCompte(UUID genre, AdmUser compte, UUID transition) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO StrIntervener (PKey, Code, Description, AdmUserId, StrConfigEntityId) VALUES (?, ?, ?, ?, ?)",
                s(id), "INTU-" + id.toString().substring(0, 8), "Intervenant compte de test", s(compte.getId()), s(genre));
        maj("INSERT INTO StrWorkFlowIntervener (PKey, StrIntervenerId, StrWorkFlowId, WithPassword) VALUES (?, ?, ?, 0)",
                s(UUID.randomUUID()), s(id), s(transition));
    }

    // ------------------------------------------------------------------ bordereau (E8)

    /** Groupe de bordereau ({@code PrjMarketLineGrouping}) du lot. */
    public UUID groupeBordereau(UUID lot, String code) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO PrjMarketLineGrouping (PKey, Code, Description, PrjMarketId, IsStock) VALUES (?, ?, ?, ?, 0)",
                s(id), code, code, s(lot));
        return id;
    }

    public void rattacher(UUID ligne, UUID groupe) {
        maj("UPDATE PrjMarketLine SET PrjMarketLineGroupingId = ?, IsStock = 0 WHERE PKey = ?", s(groupe), s(ligne));
    }

    // ------------------------------------------------------------------ demandes internes d'achat (E5)

    // yesRemsTest n'a que PKey, PrjProjectId, PrjMarketTypeId dans BuySelection et pas de BuySelectionKind : colonnes de la CI
    // ajoutées si absentes, dans la transaction du test (annulées avec elle).
    private static final String COLONNES_DIA = """
            IF COL_LENGTH('dbo.BuySelection', 'Sorting') IS NULL ALTER TABLE dbo.BuySelection ADD Sorting int NULL;
            IF COL_LENGTH('dbo.BuySelection', 'Code') IS NULL ALTER TABLE dbo.BuySelection ADD Code nvarchar(255) NULL;
            IF COL_LENGTH('dbo.BuySelection', 'Description') IS NULL ALTER TABLE dbo.BuySelection ADD Description nvarchar(255) NULL;
            IF COL_LENGTH('dbo.BuySelection', 'DescriptionLong') IS NULL ALTER TABLE dbo.BuySelection ADD DescriptionLong nvarchar(500) NULL;
            IF COL_LENGTH('dbo.BuySelection', 'DefaultVat') IS NULL ALTER TABLE dbo.BuySelection ADD DefaultVat decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.BuySelection', 'PriceEstimatedHT') IS NULL ALTER TABLE dbo.BuySelection ADD PriceEstimatedHT decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.BuySelection', 'PriceEstimatedTTC') IS NULL ALTER TABLE dbo.BuySelection ADD PriceEstimatedTTC decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.BuySelection', 'DocDate') IS NULL ALTER TABLE dbo.BuySelection ADD DocDate datetime NULL;
            IF COL_LENGTH('dbo.BuySelection', 'LaunchOfTenderDate') IS NULL ALTER TABLE dbo.BuySelection ADD LaunchOfTenderDate datetime NULL;
            IF COL_LENGTH('dbo.BuySelection', 'WithdrawalTenderDocumentsDate') IS NULL ALTER TABLE dbo.BuySelection ADD WithdrawalTenderDocumentsDate datetime NULL;
            IF COL_LENGTH('dbo.BuySelection', 'TenderMaturityDate') IS NULL ALTER TABLE dbo.BuySelection ADD TenderMaturityDate datetime NULL;
            IF COL_LENGTH('dbo.BuySelection', 'TendersResortDate') IS NULL ALTER TABLE dbo.BuySelection ADD TendersResortDate datetime NULL;
            IF COL_LENGTH('dbo.BuySelection', 'BidOpeningDate') IS NULL ALTER TABLE dbo.BuySelection ADD BidOpeningDate datetime NULL;
            IF COL_LENGTH('dbo.BuySelection', 'TendersReportDate') IS NULL ALTER TABLE dbo.BuySelection ADD TendersReportDate datetime NULL;
            IF COL_LENGTH('dbo.BuySelection', 'TenderDate') IS NULL ALTER TABLE dbo.BuySelection ADD TenderDate datetime NULL;
            IF COL_LENGTH('dbo.BuySelection', 'ContractSignatureDate') IS NULL ALTER TABLE dbo.BuySelection ADD ContractSignatureDate datetime NULL;
            IF COL_LENGTH('dbo.BuySelection', 'Notes') IS NULL ALTER TABLE dbo.BuySelection ADD Notes nvarchar(1000) NULL;
            IF COL_LENGTH('dbo.BuySelection', 'TextLetterSelection') IS NULL ALTER TABLE dbo.BuySelection ADD TextLetterSelection nvarchar(max) NULL;
            IF COL_LENGTH('dbo.BuySelection', 'IsTheLast') IS NULL ALTER TABLE dbo.BuySelection ADD IsTheLast bit NULL;
            IF COL_LENGTH('dbo.BuySelection', 'UserCreate') IS NULL ALTER TABLE dbo.BuySelection ADD UserCreate nvarchar(255) NULL;
            IF COL_LENGTH('dbo.BuySelection', 'UserCreateDateTime') IS NULL ALTER TABLE dbo.BuySelection ADD UserCreateDateTime datetime NULL;
            IF COL_LENGTH('dbo.BuySelection', 'UserUpdate') IS NULL ALTER TABLE dbo.BuySelection ADD UserUpdate nvarchar(255) NULL;
            IF COL_LENGTH('dbo.BuySelection', 'UserUpdateDateTime') IS NULL ALTER TABLE dbo.BuySelection ADD UserUpdateDateTime datetime NULL;
            IF COL_LENGTH('dbo.BuySelection', 'StrEntityId') IS NULL ALTER TABLE dbo.BuySelection ADD StrEntityId uniqueidentifier NULL;
            IF COL_LENGTH('dbo.BuySelection', 'StrStatusId') IS NULL ALTER TABLE dbo.BuySelection ADD StrStatusId uniqueidentifier NULL;
            IF COL_LENGTH('dbo.BuySelection', 'CfgCompanyId') IS NULL ALTER TABLE dbo.BuySelection ADD CfgCompanyId uniqueidentifier NULL;
            IF COL_LENGTH('dbo.BuySelection', 'StlCurrencyId') IS NULL ALTER TABLE dbo.BuySelection ADD StlCurrencyId uniqueidentifier NULL;
            IF COL_LENGTH('dbo.BuySelection', 'PrjMarketId') IS NULL ALTER TABLE dbo.BuySelection ADD PrjMarketId uniqueidentifier NULL;
            IF COL_LENGTH('dbo.BuySelection', 'StlCategoryId') IS NULL ALTER TABLE dbo.BuySelection ADD StlCategoryId uniqueidentifier NULL;
            IF COL_LENGTH('dbo.BuySelection', 'BuySelectionKindId') IS NULL ALTER TABLE dbo.BuySelection ADD BuySelectionKindId uniqueidentifier NULL;
            IF COL_LENGTH('dbo.BuySelection', 'CfgTierId') IS NULL ALTER TABLE dbo.BuySelection ADD CfgTierId uniqueidentifier NULL;
            IF COL_LENGTH('dbo.BuySelection', 'GroupId') IS NULL ALTER TABLE dbo.BuySelection ADD GroupId uniqueidentifier NULL;
            IF COL_LENGTH('dbo.BuySelection', 'StlSettlementTypeId') IS NULL ALTER TABLE dbo.BuySelection ADD StlSettlementTypeId uniqueidentifier NULL;
            IF COL_LENGTH('dbo.BuySelection', 'PaymentDelay') IS NULL ALTER TABLE dbo.BuySelection ADD PaymentDelay int NULL;
            IF COL_LENGTH('dbo.BuySelection', 'AdmModuleId') IS NULL ALTER TABLE dbo.BuySelection ADD AdmModuleId uniqueidentifier NULL;
            IF COL_LENGTH('dbo.BuySelection', 'CfgTierTriggerId') IS NULL ALTER TABLE dbo.BuySelection ADD CfgTierTriggerId uniqueidentifier NULL;
            IF OBJECT_ID('dbo.BuySelectionKind', 'U') IS NULL
                CREATE TABLE dbo.BuySelectionKind (PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(500) NULL,
                    Description nvarchar(500) NULL, Notes nvarchar(1000) NULL, UserCreate nvarchar(255) NULL,
                    UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL,
                    MaxAmount decimal(26, 6) NULL, IsConfidential bit NULL, StrEntityManagmentId uniqueidentifier NULL,
                    IsMultiSupplier bit NULL, IsWithoutVerification bit NULL);""";

    public void tablesDemandesAchat() {
        maj(COLONNES_DIA);
    }

    /** Procédure d'achat ({@code BuySelectionKind}) ; code = libellé. */
    public UUID procedureAchat(String code) {
        tablesDemandesAchat();
        UUID id = UUID.randomUUID();
        maj("INSERT INTO BuySelectionKind (PKey, Code, Description) VALUES (?, ?, ?)", s(id), code, code);
        return id;
    }

    /** Demande interne d'achat du lot ; {@code statut} : un {@code StrStatus} existant (code = libellé). */
    public UUID demandeAchat(UUID lot, UUID projet, String code, UUID procedure, UUID statut, UUID fournisseur, LocalDate date,
                             String notes) {
        tablesDemandesAchat();
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO BuySelection (PKey, Code, Description, DocDate, Notes, PrjMarketId, PrjProjectId, BuySelectionKindId,
                    StrStatusId, CfgTierId)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                s(id), code, "DIA " + code, jour(date), notes, s(lot), s(projet), s(procedure), s(statut), s(fournisseur));
        return id;
    }

    // ------------------------------------------------------------------ montants de la fiche (L2b, FEATURE-lot-fiche-suivi.md)

    /** Genres de pièce que lit {@code PrjMarketMonitoringView} (PKeys de prod) ; facture d'avenant = seule exclue des factures. */
    public static final UUID ENTITE_FACTURE_DECOMPTE = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B604");
    public static final UUID ENTITE_FACTURE_AVENANT = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B622");
    public static final UUID ENTITE_AVANCE_MARCHE = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B601");
    public static final UUID ENTITE_AVANCE_DECOMPTE = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B603");

    private static final String INVOICE = "INVOICE";
    private static final String DOCUMENT_TO_PAY = "DOCUMENT_TO_PAY";

    private static final Map<UUID, String> ENTITES_PIECES = Map.of(
            ENTITE_FACTURE_DECOMPTE, "PrjInvoiceDebitWorkingOut", ENTITE_FACTURE_AVENANT, "PrjInvoiceDebitMarketClause",
            ENTITE_AVANCE_MARCHE, "PrjDocumentDebitMarketAdvance", ENTITE_AVANCE_DECOMPTE, "PrjDocumentDebitMarketWorkingOutAdvance");

    // PrjClause est dans la CI (010_tables.sql), pas dans yesRemsTest : créée si absente, dans la transaction du test.
    private static final String TABLE_AVENANTS = """
            IF OBJECT_ID('dbo.PrjClause', 'U') IS NULL
                CREATE TABLE dbo.PrjClause (PKey uniqueidentifier NOT NULL PRIMARY KEY, Sorting int NULL, Code nvarchar(255) NULL,
                    Description nvarchar(255) NULL, DocDate datetime NULL, DocRef nvarchar(255) NULL,
                    SupplierDescription nvarchar(255) NULL, Notes nvarchar(1000) NULL, AmountHT decimal(26, 6) NULL,
                    AmountVat decimal(26, 6) NULL, AmountTTC decimal(26, 6) NULL, AmountToPay decimal(26, 6) NULL,
                    WarrantyBasedOnAmount bit NULL, WarrantyRatio decimal(26, 6) NULL, WarrantyAmount decimal(26, 6) NULL,
                    DECBasedOnAmount bit NULL, InsuranceDECRatio decimal(26, 6) NULL, InsuranceDECAmount decimal(26, 6) NULL,
                    TRCBasedOnAmount bit NULL, InsuranceTRCAmount decimal(26, 6) NULL, InsuranceTRCRatio decimal(26, 6) NULL,
                    ProrataBasedOnAmount bit NULL, ProportionRatio decimal(26, 6) NULL, ProportionAmount decimal(26, 6) NULL,
                    CfgTierId uniqueidentifier NULL, PrjMarketId uniqueidentifier NULL, UserCreate nvarchar(255) NULL,
                    UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL);""";

    /** Les {@code StrEntity} des pièces du lot, créées si absentes (yesRemsTest et CI n'ont pas les lignes de prod). */
    public void entitesPiecesLot() {
        ENTITES_PIECES.forEach((id, code) -> maj("""
                IF NOT EXISTS (SELECT 1 FROM StrEntity WHERE PKey = ?)
                    INSERT INTO StrEntity (PKey, Code, Description) VALUES (?, ?, ?)""", s(id), s(id), code, code));
    }

    /** Pièce d'achat du lot ({@code BuyDocument.PrjMarketId}) ; {@code entite} et {@code reference} peuvent être nuls. */
    public UUID pieceLot(UUID lot, String code, String type, String reference, UUID entite, BigDecimal ht, BigDecimal ttc) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO BuyDocument (PKey, Code, DocType, ReferenceType, StrEntityId, DocDate, AmountHT, AmountTTC, AmountToPay,
                    PrjMarketId) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                s(id), code, type, reference, s(entite), jour(LocalDate.of(2026, 3, 20)), ht, ttc, ttc, s(lot));
        return id;
    }

    /** Facture de décompte du lot (genre {@code PrjInvoiceDebitWorkingOut}). */
    public UUID factureLot(UUID lot, String code, String ht, String ttc) {
        return pieceLot(lot, code, INVOICE, null, ENTITE_FACTURE_DECOMPTE, new BigDecimal(ht), new BigDecimal(ttc));
    }

    /** Avoir du lot ({@code DocType = REFUND}). */
    public UUID avoirLot(UUID lot, String code, String ht, String ttc) {
        return pieceLot(lot, code, "REFUND", null, null, new BigDecimal(ht), new BigDecimal(ttc));
    }

    /** Pièce d'avance sur marché validée ({@code ReferenceType = MARKET_ADVANCE}), montant TTC. */
    public UUID avanceMarche(UUID lot, String code, String ttc) {
        return pieceLot(lot, code, DOCUMENT_TO_PAY, "MARKET_ADVANCE", ENTITE_AVANCE_MARCHE, new BigDecimal(ttc), new BigDecimal(ttc));
    }

    /** Pièce d'avance sur décompte validée ({@code ReferenceType = WORKINGOUT_ADVANCE}), montant TTC. */
    public UUID avanceDecompte(UUID lot, String code, String ttc) {
        return pieceLot(lot, code, DOCUMENT_TO_PAY, "WORKINGOUT_ADVANCE", ENTITE_AVANCE_DECOMPTE, new BigDecimal(ttc),
                new BigDecimal(ttc));
    }

    /** Devis retenu du lot ({@code QUOTE}, {@code IsSelected = 1}) : le « Marché » de la fiche. */
    public UUID devisRetenu(UUID lot, String code, String ht, String ttc) {
        UUID id = pieceLot(lot, code, "QUOTE", null, null, new BigDecimal(ht), new BigDecimal(ttc));
        maj("UPDATE BuyDocument SET IsSelected = 1 WHERE PKey = ?", s(id));
        return id;
    }

    /** Avenant du lot ({@code PrjClause}). */
    public UUID avenant(UUID lot, String code, String ht, String tva, String ttc) {
        maj(TABLE_AVENANTS);
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO PrjClause (PKey, Sorting, Code, Description, DocDate, AmountHT, AmountVat, AmountTTC, AmountToPay, PrjMarketId)
                VALUES (?, 1, ?, ?, ?, ?, ?, ?, ?, ?)""",
                s(id), code, "Avenant " + code, jour(LocalDate.of(2026, 3, 10)), new BigDecimal(ht), new BigDecimal(tva),
                new BigDecimal(ttc), new BigDecimal(ttc), s(lot));
        return id;
    }

    /** Facture d'avenant (genre {@code PrjInvoiceDebitMarketClause}, {@code PrjClauseId}). */
    public UUID factureAvenant(UUID lot, UUID avenant, String code, String ht, String ttc) {
        UUID id = pieceLot(lot, code, INVOICE, null, ENTITE_FACTURE_AVENANT, new BigDecimal(ht), new BigDecimal(ttc));
        maj("UPDATE BuyDocument SET PrjClauseId = ? WHERE PKey = ?", s(avenant), s(id));
        return id;
    }

    /** Ligne du bordereau estimatif ({@code PrjMarketLineEstimation}), TVA 20 %, FODEC 0. */
    public void ligneEstimative(UUID lot, String code, String quantite, String puHT) {
        BigDecimal pu = new BigDecimal(puHT);
        maj("""
                INSERT INTO PrjMarketLineEstimation (PKey, Code, Description, Sorting, Unity, Quantity, UnitPriceHT, VatRatio,
                    UnitPriceTTC, FODECRatio, IsStock, PrjMarketId)
                VALUES (?, ?, ?, 1, N'U', ?, ?, 0.2, ?, 0, 0, ?)""",
                s(UUID.randomUUID()), code, "Ligne " + code, new BigDecimal(quantite), pu, pu.multiply(new BigDecimal("1.2")), s(lot));
    }

    /** Retenue forfaitaire d'un décompte ({@code PrjWorkingOutRetainedLine}). */
    public void retenueForfaitaire(UUID decompte, String montant) {
        maj("INSERT INTO PrjWorkingOutRetainedLine (PKey, PrjWorkingOutId, Sorting, Description, Amount) VALUES (?, ?, 1, ?, ?)",
                s(UUID.randomUUID()), s(decompte), "Retenue forfaitaire", new BigDecimal(montant));
    }

    // ------------------------------------------------------------------ lecture

    /** Ligne {@code PrjMarket} après un appel accepté : la session est écrite avant la lecture. */
    public Map<String, Object> lot(UUID lot) {
        em.flush();
        return ligneLot(lot);
    }

    /**
     * Ligne {@code PrjMarket} après un refus : la session est vidée sans être écrite (hors test, le refus annule la transaction ;
     * ici elle est partagée et un vidage écrirait ce qu'un service aurait modifié avant de refuser).
     */
    public Map<String, Object> lotApresRefus(UUID lot) {
        em.clear();
        return ligneLot(lot);
    }

    private Map<String, Object> ligneLot(UUID lot) {
        List<Map<String, Object>> l = jdbc.queryForList("SELECT * FROM PrjMarket WHERE PKey = ?", s(lot));
        if (l.size() != 1) {
            throw new AssertionError("lot " + lot + " : " + l.size() + " ligne(s)");
        }
        return l.get(0);
    }

    public List<Map<String, Object>> lignes(String sql, Object... parametres) {
        em.flush();
        return jdbc.queryForList(sql, parametres);
    }
}
