package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lot N7b — 4 rapports Achat (FEATURE-n7-etats-achat.md R12-R15) : lignes {@code AdmReportData} du type
 * {@code Yes.Buy.Front.Tdb.Reports}, prêts / échéances / prévisions (tables absentes de yesRemsTest, créées ou complétées
 * dans la transaction), règlements par module, comptes libres pour les rapports. Tout s'annule avec la transaction du test.
 */
@Component
public class FixtureRapportsAchat {

    public static final String TYPE_ACHAT = "Yes.Buy.Front.Tdb.Reports";
    public static final String DEPENSES_PAR_MOIS = "BuyTdbActivityByMoth";
    public static final String RETENUES_LOTS = "BuyTdbMarketsRetainedAmounts";
    public static final String BONS_COMMANDE = "BuyOrderAndDeliveryLinesWithQuantity";
    public static final String ECHEANCIER_CREDITS = "BuyTdbLoanScheduleByYear";
    public static final List<String> CODES = List.of(DEPENSES_PAR_MOIS, RETENUES_LOTS, BONS_COMMANDE, ECHEANCIER_CREDITS);

    // BuyLoanSchedule.PaymentDate est un smalldatetime (max 06/06/2079) : année vide la plus lointaine admise, + 1 encore valide.
    public static final int ANNEE = 2078;
    public static final String DU = "2078-01-01";
    public static final String AU = "2078-12-31";

    private static final String ROLE = "Commercial";
    private static final String P_ID = "id";
    private static final String P_USER = "user";
    private static final String ZERO = "0";

    // Définitions de yesRemsProd ; absentes de yesRemsTest et de la CI avant ci/schema/198.
    private static final String TABLE_SOUS_PRETS = """
            IF OBJECT_ID('dbo.BuySubLoan', 'U') IS NULL
            CREATE TABLE dbo.BuySubLoan (PKey uniqueidentifier NOT NULL PRIMARY KEY, Amount decimal(26, 6) NULL,
                PaymentDate smalldatetime NULL, MonthsCount int NULL, GracePeriod int NULL, MonthsStep int NULL,
                Notes nvarchar(1000) NULL, BuyLoanId uniqueidentifier NULL, AnnualRate decimal(26, 6) NULL,
                AmortizationDeferredDelay int NULL, TotalDeferredDelay int NULL, IsMensualRate bit NULL, IsActoriel bit NULL)""";
    private static final String TABLE_ECHEANCES = """
            IF OBJECT_ID('dbo.BuyLoanSchedule', 'U') IS NULL
            CREATE TABLE dbo.BuyLoanSchedule (PKey uniqueidentifier NOT NULL PRIMARY KEY, AmountBase decimal(26, 6) NULL,
                InterestRate decimal(26, 6) NULL, AmountInterest decimal(26, 6) NULL, AmountToPay decimal(26, 6) NULL,
                PaymentDate smalldatetime NULL, Notes nvarchar(1000) NULL, BuySubLoanId uniqueidentifier NULL,
                Commision decimal(26, 6) NULL, AmountBaseHT decimal(26, 6) NULL, BaseVatRatio decimal(26, 6) NULL,
                AmountInterestHT decimal(26, 6) NULL, InterestVatRatio decimal(26, 6) NULL, CommisionHT decimal(26, 6) NULL,
                CommisionVatRatio decimal(26, 6) NULL, AmountToPayHT decimal(26, 6) NULL)""";
    private static final String TABLE_PREVISIONS_CATEGORIE = """
            IF OBJECT_ID('dbo.StlCategoryPrevision', 'U') IS NULL
            CREATE TABLE dbo.StlCategoryPrevision (PKey uniqueidentifier NOT NULL PRIMARY KEY, PrevisionYear int NULL,
                PrevisionMonth int NULL, PrevisionAmount decimal(26, 6) NULL, StlCategoryId uniqueidentifier NULL,
                CfgCompanyId uniqueidentifier NULL)""";
    // yesRemsTest (et ci/schema/165) réduisent BuyLoan et PrjPrevision à 2 colonnes : compléter comme en prod.
    private static final String COLONNES_PRETS = """
            IF COL_LENGTH('dbo.BuyLoan', 'DocDate') IS NULL ALTER TABLE dbo.BuyLoan ADD DocDate datetime NULL;
            IF COL_LENGTH('dbo.BuyLoan', 'Code') IS NULL ALTER TABLE dbo.BuyLoan ADD Code nvarchar(255) NULL;
            IF COL_LENGTH('dbo.BuyLoan', 'Description') IS NULL ALTER TABLE dbo.BuyLoan ADD Description nvarchar(255) NULL;
            IF COL_LENGTH('dbo.BuyLoan', 'Amount') IS NULL ALTER TABLE dbo.BuyLoan ADD Amount decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.BuyLoan', 'InterestRate') IS NULL ALTER TABLE dbo.BuyLoan ADD InterestRate decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.BuyLoan', 'Fees') IS NULL ALTER TABLE dbo.BuyLoan ADD Fees decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.BuyLoan', 'Notes') IS NULL ALTER TABLE dbo.BuyLoan ADD Notes nvarchar(1000) NULL;
            IF COL_LENGTH('dbo.BuyLoan', 'CfgTierId') IS NULL ALTER TABLE dbo.BuyLoan ADD CfgTierId uniqueidentifier NULL;
            IF COL_LENGTH('dbo.BuyLoan', 'FeesPrevention') IS NULL ALTER TABLE dbo.BuyLoan ADD FeesPrevention decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.BuyLoan', 'StkItemId') IS NULL ALTER TABLE dbo.BuyLoan ADD StkItemId uniqueidentifier NULL;
            IF COL_LENGTH('dbo.BuyLoan', 'CfgCompanyId') IS NULL ALTER TABLE dbo.BuyLoan ADD CfgCompanyId uniqueidentifier NULL;
            IF COL_LENGTH('dbo.BuyLoan', 'BuyLoanTypeId') IS NULL ALTER TABLE dbo.BuyLoan ADD BuyLoanTypeId uniqueidentifier NULL;
            IF COL_LENGTH('dbo.BuyLoan', 'BaseVatRatio') IS NULL ALTER TABLE dbo.BuyLoan ADD BaseVatRatio decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.BuyLoan', 'CommisionVatRatio') IS NULL ALTER TABLE dbo.BuyLoan ADD CommisionVatRatio decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.BuyLoan', 'InterestVatRatio') IS NULL ALTER TABLE dbo.BuyLoan ADD InterestVatRatio decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.BuyLoan', 'DefaultCommisionAmount') IS NULL
                ALTER TABLE dbo.BuyLoan ADD DefaultCommisionAmount decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.BuyLoan', 'IsClosed') IS NULL ALTER TABLE dbo.BuyLoan ADD IsClosed bit NULL;
            IF COL_LENGTH('dbo.BuyLoan', 'StrEntityId') IS NULL ALTER TABLE dbo.BuyLoan ADD StrEntityId uniqueidentifier NULL;""";
    private static final String COLONNES_PREVISIONS_PROJET = """
            IF COL_LENGTH('dbo.PrjPrevision', 'PrevisionYear') IS NULL ALTER TABLE dbo.PrjPrevision ADD PrevisionYear int NULL;
            IF COL_LENGTH('dbo.PrjPrevision', 'PrevisionMonth') IS NULL ALTER TABLE dbo.PrjPrevision ADD PrevisionMonth int NULL;
            IF COL_LENGTH('dbo.PrjPrevision', 'PrevisionAmount') IS NULL
                ALTER TABLE dbo.PrjPrevision ADD PrevisionAmount decimal(26, 6) NULL;""";

    private final EntityManager em;
    private final FixtureRapports rapports;
    private final FixtureAchat achat;

    public FixtureRapportsAchat(EntityManager em, FixtureRapports rapports, FixtureAchat achat) {
        this.em = em;
        this.rapports = rapports;
        this.achat = achat;
    }

    /** Prêt OMRAN : {@code BuyLoan} et son unique sous-prêt {@code BuySubLoan} (parent des échéances). */
    public record Pret(UUID id, UUID sousPret) {
    }

    // ------------------------------------------------------------------ socle : tables, rapports, comptes

    /** Tables du lot (idempotent) et les 4 lignes {@code AdmReportData} Achat du groupe « SUIVI » ; rend code → id. */
    public Map<String, UUID> rapports() {
        rapports.tables();
        executer(em.createNativeQuery(TABLE_SOUS_PRETS));
        executer(em.createNativeQuery(TABLE_ECHEANCES));
        executer(em.createNativeQuery(TABLE_PREVISIONS_CATEGORIE));
        executer(em.createNativeQuery(COLONNES_PRETS));
        executer(em.createNativeQuery(COLONNES_PREVISIONS_PROJET));
        Map<String, UUID> ids = new LinkedHashMap<>();
        int ordre = 1;
        for (String code : CODES) {
            ids.put(code, rapports.rapport(TYPE_ACHAT, code, "RAPPORT " + code, "SUIVI", ordre++));
        }
        return ids;
    }

    /**
     * Compte « Commercial » habilité à la société et aux projets donnés ; {@code restreint} = {@code WithReportRestriction}
     * de son rôle ; rôle exempté de la clause de statut des lots ({@code WithSequenceFreeRestriction = 0}).
     */
    public AdmUser compte(String code, boolean restreint, UUID societe, UUID... projets) {
        AdmUser compte = achat.compte(code, ROLE);
        if (societe != null) {
            achat.habiliterSociete(compte, societe);
        }
        for (UUID projet : projets) {
            achat.habiliter(compte, projet);
        }
        executer(em.createNativeQuery("""
                        UPDATE AdmRole SET WithReportRestriction = :restreint, WithSequenceFreeRestriction = 0
                        WHERE PKey IN (SELECT AdmRoleId FROM AdmUserRole WHERE AdmUserId = :user)""")
                .setParameter("restreint", restreint).setParameter(P_USER, compte.getId()));
        rapports.relire();
        return compte;
    }

    public static String role() {
        return ROLE;
    }

    /** Ligne {@code AdmReportDataRole} entre le rôle du compte et le rapport. */
    public void lier(AdmUser compte, UUID rapportId) {
        executer(em.createNativeQuery("""
                        INSERT INTO AdmReportDataRole (PKey, AdmRoleId, AdmReportDataId)
                        SELECT TOP (1) :id, ur.AdmRoleId, :rapport FROM AdmUserRole ur WHERE ur.AdmUserId = :user
                        ORDER BY ur.AdmRoleId ASC""")
                .setParameter(P_ID, UUID.randomUUID()).setParameter("rapport", rapportId)
                .setParameter(P_USER, compte.getId()));
        rapports.relire();
    }

    // ------------------------------------------------------------------ R12 / R15 : prévisions, prêts, règlements

    /** {@code StlCategoryPrevision} du mois. */
    public void previsionCategorie(int annee, int mois, String montant, UUID categorie, UUID societe) {
        executer(em.createNativeQuery("""
                        INSERT INTO StlCategoryPrevision (PKey, PrevisionYear, PrevisionMonth, PrevisionAmount, StlCategoryId,
                            CfgCompanyId) VALUES (:id, :annee, :mois, :montant, :categorie, :societe)""")
                .setParameter(P_ID, UUID.randomUUID()).setParameter("annee", annee).setParameter("mois", mois)
                .setParameter("montant", new BigDecimal(montant)).setParameter("categorie", categorie)
                .setParameter("societe", societe));
    }

    /** {@code PrjPrevision} du mois. */
    public void previsionProjet(int annee, int mois, String montant, UUID projet) {
        executer(em.createNativeQuery("""
                        INSERT INTO PrjPrevision (PKey, PrevisionYear, PrevisionMonth, PrevisionAmount, PrjProjectId)
                        VALUES (:id, :annee, :mois, :montant, :projet)""")
                .setParameter(P_ID, UUID.randomUUID()).setParameter("annee", annee).setParameter("mois", mois)
                .setParameter("montant", new BigDecimal(montant)).setParameter("projet", projet));
    }

    /** Prêt de la société, {@code projet} nul = sans projet ; Code et Description portent {@code code}. */
    public Pret pret(String code, UUID societe, UUID projet) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO BuyLoan (PKey, Code, Description, Amount, CfgCompanyId, PrjProjectId, IsClosed)
                        VALUES (:id, :code, :description, 0, :societe, :projet, 0)""")
                .setParameter(P_ID, id).setParameter("code", code).setParameter("description", code + " credit")
                .setParameter("societe", societe).setParameter("projet", projet));
        UUID sousPret = UUID.randomUUID();
        executer(em.createNativeQuery("INSERT INTO BuySubLoan (PKey, Amount, BuyLoanId) VALUES (:id, 0, :pret)")
                .setParameter(P_ID, sousPret).setParameter("pret", id));
        return new Pret(id, sousPret);
    }

    /** Échéance du prêt : seul {@code AmountToPay} vaut {@code montant}, les autres montants à 0 (jamais NULL). */
    public void echeance(Pret pret, LocalDate date, String montant) {
        executer(em.createNativeQuery("""
                        INSERT INTO BuyLoanSchedule (PKey, AmountBase, InterestRate, AmountInterest, AmountToPay, PaymentDate,
                            BuySubLoanId, Commision, AmountBaseHT, AmountInterestHT, CommisionHT, AmountToPayHT)
                        VALUES (:id, 0, 0, 0, :montant, :date, :sousPret, 0, 0, 0, 0, 0)""")
                .setParameter(P_ID, UUID.randomUUID()).setParameter("montant", new BigDecimal(montant))
                .setParameter("date", date.atStartOfDay()).setParameter("sousPret", pret.sousPret()));
    }

    /**
     * Règlement sans ligne du module ({@code BUY}, {@code PRJ}, {@code STL}) ; {@code echeance} nulle = seule la date de
     * règlement est posée. {@code DocDate} = date de règlement.
     */
    public UUID reglement(String code, String module, boolean credit, String montant, String statut, LocalDate echeance,
                          LocalDate dateReglement, UUID societe) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO StlSettlement (PKey, Code, DocDate, SettlementDate, Amount, IsCredit, Status, AdmModuleId,
                            CfgCompanyId) VALUES (:id, :code, :date, :date, :montant, :credit, :statut, :module, :societe)""")
                .setParameter(P_ID, id).setParameter("code", code).setParameter("date", dateReglement.atStartOfDay())
                .setParameter("montant", new BigDecimal(montant)).setParameter("credit", credit)
                .setParameter("statut", statut).setParameter("module", achat.module(module))
                .setParameter("societe", societe));
        if (echeance != null) {
            executer(em.createNativeQuery("UPDATE StlSettlement SET EcheanceDate = :echeance WHERE PKey = :id")
                    .setParameter(P_ID, id).setParameter("echeance", echeance.atStartOfDay()));
        }
        return id;
    }

    /** Facture d'achat payable ({@code CanBePayed = 1}), à payer = {@code montant}, autres montants sommables à 0. */
    public UUID facturePayable(String code, LocalDate date, UUID fournisseur, String nom, UUID societe, UUID projet,
                               String montant) {
        UUID id = achat.piece(FixtureAchat.Piece.facture(code, date, fournisseur, nom, societe, projet, new BigDecimal(montant)));
        achat.poser(id, "CanBePayed", true);
        achat.poser(id, "AdvanceAmount", new BigDecimal(ZERO));
        achat.poser(id, "WorkingOutAdvanceAmount", new BigDecimal(ZERO));
        achat.poser(id, "WarrantyAmount", new BigDecimal(ZERO));
        achat.poser(id, "AmountRegul", new BigDecimal(ZERO));
        return id;
    }

    // ------------------------------------------------------------------ R14 : bons de commande et de livraison

    /** Bon de commande ({@code ORDER}) daté, {@code projet} nul possible. */
    public UUID bonCommande(String code, LocalDate date, UUID fournisseur, String nom, UUID societe, UUID projet) {
        return achat.piece(FixtureAchat.Piece.facture(code, date, fournisseur, nom, societe, projet, new BigDecimal("100"))
                .nature(FixtureAchat.BON_COMMANDE));
    }

    /** Bon de livraison ({@code DELIVRY}) du BC ({@code BuyOrderId}) ; {@code reference} = {@code DocRef} (vide possible). */
    public UUID bonLivraison(String code, LocalDate date, UUID bonCommande, UUID fournisseur, String nom, UUID societe,
                             UUID projet, String reference) {
        UUID id = achat.piece(FixtureAchat.Piece.facture(code, date, fournisseur, nom, societe, projet, new BigDecimal("100"))
                .nature(FixtureAchat.BON_LIVRAISON).textes(reference, "Livraison " + code));
        achat.poser(id, "BuyOrderId", bonCommande);
        return id;
    }

    /** Ligne de pièce : {@code Code} = code article, quantité donnée, PU 10, TVA 20 %. */
    public void ligne(UUID piece, int ordre, String codeArticle, String designation, String quantite) {
        BigDecimal pu = BigDecimal.TEN;
        BigDecimal tva = new BigDecimal("0.2");
        achat.ligne(piece, new FixtureAchat.LigneAchat(ordre, codeArticle, designation, "U", new BigDecimal(quantite), pu, tva,
                BigDecimal.ZERO, BigDecimal.ZERO, pu.multiply(BigDecimal.ONE.add(tva))));
    }

    private static void executer(Query requete) {
        requete.executeUpdate();
    }
}
