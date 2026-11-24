package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureTresorerie.Cadre;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lot N3-bis a — saisie d'un décaissement (FEATURE-n3bis-saisie-reglements.md). Sur {@link FixtureTresorerie} : devise
 * par défaut propre au test (pièces et comptes dans cette devise), compte bancaire de la société et d'une autre société,
 * types de règlement à drapeaux, rapprochement, tables absentes de yesRemsTest ({@code StlSettlementHistory},
 * {@code StlInternalTransfer}) créées dans la transaction du test.
 */
@Component
public class FixtureDecaissements {

    public static final String DROIT_LIRE = FixtureTresorerie.DROIT_DECAISSEMENTS;
    public static final String DROIT_NOUVEAU = DROIT_LIRE + ".toolNew";
    public static final String DROIT_SUPPRIMER = DROIT_LIRE + ".toolDelete";
    public static final String DROIT_SANS_AFFECTATION = DROIT_LIRE + ".CanSaveWithoutAffectation";
    public static final String DROIT_CHANGER_SOCIETE = DROIT_LIRE + ".CanModifyCompany";

    private static final String P_ID = "id";
    private static final String REGLEMENT = "StlSettlement";

    // Définition de yesRemsProd (base/tables/StlSettlementHistory.md) ; absente de yesRemsTest et de la CI au 22/11.
    private static final String TABLE_HISTORIQUE = """
            IF OBJECT_ID('dbo.StlSettlementHistory', 'U') IS NULL
                CREATE TABLE dbo.StlSettlementHistory (PKey uniqueidentifier NOT NULL PRIMARY KEY,
                    AdmUserId uniqueidentifier NULL, DocDate datetime NULL, EcheanceDate datetime NULL,
                    StlSettlementId uniqueidentifier NULL, StlSettlementTypeId uniqueidentifier NULL,
                    Reference nvarchar(255) NULL, UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL,
                    UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL)""";
    // Présente en CI (010_tables.sql), absente de yesRemsTest.
    private static final String TABLE_VIREMENT_INTERNE = """
            IF OBJECT_ID('dbo.StlInternalTransfer', 'U') IS NULL
                CREATE TABLE dbo.StlInternalTransfer (PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(255) NULL,
                    DocDate datetime NULL, Amount decimal(26, 6) NULL, CfgCompanyId uniqueidentifier NULL)""";

    private final EntityManager em;
    private final FixtureTresorerie tresorerie;
    private final FixtureAchat achat;

    public FixtureDecaissements(EntityManager em, FixtureTresorerie tresorerie, FixtureAchat achat) {
        this.em = em;
        this.tresorerie = tresorerie;
        this.achat = achat;
    }

    /** Cadre N3 + devise par défaut du test, compte bancaire de la société, compte d'une autre société. */
    public record Saisie(Cadre c, UUID devise, UUID compte, UUID autreSociete, UUID compteAutreSociete) {

        public String code(String suffixe) {
            return c.code(suffixe);
        }
    }

    public Saisie saisie(String prefixe) {
        executer(em.createNativeQuery(TABLE_HISTORIQUE));
        Cadre c = tresorerie.cadre(prefixe);
        UUID devise = achat.devise("D" + c.jeton());
        achat.parametreGuid("DefaultCurrency", devise);
        UUID autreSociete = achat.societe("SOC2-" + c.jeton());
        return new Saisie(c, devise, compteBancaire("CPT-" + c.jeton(), c.societe(), devise), autreSociete,
                compteBancaire("CPT2-" + c.jeton(), autreSociete, devise));
    }

    /** Compte habilité à la société et au projet habilité du cadre, droits OMRAN donnés. */
    public AdmUser compte(Saisie s, String code, String... droits) {
        return tresorerie.compte(s.c(), code, droits);
    }

    /** Compte bancaire {@code StlAccount} de la société, dans la devise donnée. */
    public UUID compteBancaire(String code, UUID societe, UUID devise) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO StlAccount (PKey, Code, Description, CfgCompanyId, StlCurrencyId)
                        VALUES (:id, :code, :code, :societe, :devise)""")
                .setParameter(P_ID, id).setParameter("code", code).setParameter("societe", societe)
                .setParameter("devise", devise));
        return id;
    }

    /** Drapeaux d'un type de règlement ({@code StlSettlementType}). */
    public record Drapeaux(boolean referenceObligatoire, boolean referenceUnique, boolean banqueObligatoire,
                           boolean notaireObligatoire) {
    }

    public UUID type(String code, Drapeaux d) {
        UUID id = achat.typeReglement(code);
        executer(em.createNativeQuery("""
                        UPDATE StlSettlementType SET IsReferenceMandatory = :ref, IsReferenceUnique = :unique,
                            IsBankMandatory = :banque, IsLawyerMandatory = :notaire, IsTierUnique = 0, IsDeposit = 0
                        WHERE PKey = :id""")
                .setParameter(P_ID, id).setParameter("ref", d.referenceObligatoire())
                .setParameter("unique", d.referenceUnique()).setParameter("banque", d.banqueObligatoire())
                .setParameter("notaire", d.notaireObligatoire()));
        return id;
    }

    /** Facture du fournisseur du cadre, au module PRJ, dans la devise du test ; {@code projet} nul = sans projet. */
    public UUID piece(Saisie s, String suffixe, UUID projet, String montant) {
        return piece(s, suffixe, projet, s.c().fournisseur(), s.c().modulePrj(), montant);
    }

    /** Pièce au statut validé pour paiement ({@code IsOkForPayement = 1}, G-e de FEATURE-n3bis-b1-reglements.md). */
    public UUID piece(Saisie s, String suffixe, UUID projet, UUID fournisseur, UUID module, String montant) {
        return piece(s, suffixe, projet, fournisseur, module, montant, true);
    }

    /** Facture du cadre dont le statut a {@code IsOkForPayement = 0} (G-a / G-e). */
    public UUID pieceNonValidee(Saisie s, String suffixe, UUID projet, String montant) {
        return piece(s, suffixe, projet, s.c().fournisseur(), s.c().modulePrj(), montant, false);
    }

    private UUID piece(Saisie s, String suffixe, UUID projet, UUID fournisseur, UUID module, String montant,
                       boolean valideePourPaiement) {
        UUID id = tresorerie.piece(s.code(suffixe), s.c().societe(), projet, fournisseur, "Fournisseur " + suffixe,
                module, montant);
        achat.poser(id, "StlCurrencyId", s.devise());
        achat.poser(id, "BuyDocumentStatusId",
                achat.statut("ST" + s.code(suffixe), valideePourPaiement, false, valideePourPaiement ? 1 : 0));
        return id;
    }

    /** Dernier rapprochement ({@code StlAccountPosition}) du compte à la date donnée. */
    public void rapprochement(UUID compte, LocalDate date) {
        executer(em.createNativeQuery("""
                        INSERT INTO StlAccountPosition (PKey, PositionDate, Position, StlAccountId)
                        VALUES (:id, :date, 0, :compte)""")
                .setParameter(P_ID, UUID.randomUUID()).setParameter("date", date.atStartOfDay())
                .setParameter("compte", compte));
    }

    /** {@code IsAccounted = 1} posé en base, contexte vidé (l'entité en mémoire ne doit pas le réécrire). */
    public void marquerComptabilise(UUID reglement) {
        achat.poserSur(REGLEMENT, reglement, "IsAccounted", true);
        em.clear();
    }

    /** Le règlement devient la jambe d'un virement interne ({@code StlInternalTransferId}). */
    public void lierVirementInterne(UUID reglement) {
        executer(em.createNativeQuery(TABLE_VIREMENT_INTERNE));
        UUID virement = UUID.randomUUID();
        executer(em.createNativeQuery("INSERT INTO StlInternalTransfer (PKey, Code) VALUES (:id, :code)")
                .setParameter(P_ID, virement).setParameter("code", "VI-" + virement.toString().substring(0, 8)));
        achat.poserSur(REGLEMENT, reglement, "StlInternalTransferId", virement);
        em.clear();
    }

    /** Écriture {@code StlAccountingEntry} citant le règlement (table créée si absente). */
    public void ecritureComptable(UUID reglement) {
        achat.tablesComptables();
        executer(em.createNativeQuery("INSERT INTO StlAccountingEntry (PKey, StlSettlementId) VALUES (:id, :reglement)")
                .setParameter(P_ID, UUID.randomUUID()).setParameter("reglement", reglement));
        em.clear();
    }

    /** La ligne {@code StlSettlement} (colonnes relues par les tests), absente = vide. */
    public List<Map<String, Object>> reglementEnBase(UUID id) {
        return achat.lire("""
                SELECT IsCredit AS credit, IsAccounted AS comptabilise, Amount AS montant, AmountBase AS base,
                    Status AS statut, CAST(AdmModuleId AS varchar(36)) AS module, CAST(TierId AS varchar(36)) AS tiers,
                    CAST(TierPayerId AS varchar(36)) AS payeur, CAST(StlAccountId AS varchar(36)) AS compte
                FROM StlSettlement WHERE PKey = :id""", id);
    }

    /** Les lignes {@code StlSettlementLine} du règlement. */
    public List<Map<String, Object>> lignesEnBase(UUID reglement) {
        return achat.lire("""
                SELECT CAST(BuyDocumentId AS varchar(36)) AS piece, Amount AS montant,
                    CAST(ComDocumentId AS varchar(36)) AS com, CAST(StlDocumentId AS varchar(36)) AS stl
                FROM StlSettlementLine WHERE StlSettlementId = :id ORDER BY Amount ASC""", reglement);
    }

    /** Les lignes {@code StlSettlementHistory} du règlement, de la plus ancienne à la plus récente. */
    public List<Map<String, Object>> historique(UUID reglement) {
        return achat.lire("""
                SELECT CAST(AdmUserId AS varchar(36)) AS utilisateur, EcheanceDate AS echeance,
                    CAST(StlSettlementTypeId AS varchar(36)) AS type, Reference AS reference
                FROM StlSettlementHistory WHERE StlSettlementId = :id ORDER BY DocDate ASC, PKey ASC""", reglement);
    }

    public int nombre(String table, String colonne, UUID id) {
        return achat.nombre(table, colonne, id);
    }

    // --- Lot b2a : retenue à la source et codes de feuille (FEATURE-n3bis-b2a-retenue.md) ---

    public static final String MANAGE_RS = "ManageRS";
    public static final UUID SEQUENCE_PP = UUID.fromString("60C4F768-BE0C-49AA-A5FD-F784D28F2F0A");
    public static final String CODE_SEQUENCE_PP = "Proposition de paiement";
    public static final String CODE_SEQUENCE_CRAS = "Certificat de retenue à la source";
    public static final String TYPE_SEQUENCE_CRAS = "RSSheetCode";
    private static final String P_CODE = "code";
    private static final String P_SOCIETE = "societe";
    private static final String P_VALEUR = "valeur";

    // Présentes en CI (010_tables.sql), absentes de yesRemsTest.
    private static final String TABLE_TYPE_RETENUE = """
            IF OBJECT_ID('dbo.StlDeductionType', 'U') IS NULL
                CREATE TABLE dbo.StlDeductionType (PKey uniqueidentifier NOT NULL PRIMARY KEY, Sorting int NULL,
                    Code nvarchar(255) NULL, Description nvarchar(255) NULL, RASYear int NULL, RASValue decimal(26, 6) NULL,
                    IsDeduction bit NULL, IsDeductionTva bit NULL, IsOther bit NULL, Notes nvarchar(1000) NULL,
                    UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL,
                    UserUpdateDateTime datetime NULL)""";
    private static final String TABLE_RETENUE = """
            IF OBJECT_ID('dbo.StlDeduction', 'U') IS NULL
                CREATE TABLE dbo.StlDeduction (PKey uniqueidentifier NOT NULL PRIMARY KEY, AmountBase decimal(26, 6) NULL,
                    RsRatio decimal(26, 6) NULL, AmountRS decimal(26, 6) NULL, Amount decimal(26, 6) NULL,
                    StlSettlementId uniqueidentifier NULL, Notes nvarchar(max) NULL, UserCreate nvarchar(255) NULL,
                    UserUpdate nvarchar(255) NULL, UserCreateDateTime datetime NULL, UserUpdateDateTime datetime NULL,
                    StlDeductionTypeId uniqueidentifier NULL)""";

    /** {@code StlDeductionType} et {@code StlDeduction} créées si absentes (idempotent). */
    public void tablesRetenue() {
        executer(em.createNativeQuery(TABLE_TYPE_RETENUE));
        executer(em.createNativeQuery(TABLE_RETENUE));
    }

    /** Type de retenue ({@code StlDeductionType}) sans année RAS. */
    public UUID typeRetenue(String code, String taux) {
        return typeRetenue(code, taux, null);
    }

    /** Type de retenue ({@code IsDeduction = 1}, Sorting 1), taux en fraction, année RAS nulle permise. */
    public UUID typeRetenue(String code, String taux, Integer anneeRas) {
        tablesRetenue();
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO StlDeductionType (PKey, Sorting, Code, Description, RASYear, RASValue, IsDeduction,
                            IsDeductionTva, IsOther)
                        VALUES (:id, 1, :code, :code, :annee, :taux, 1, 0, 0)""")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter("annee", anneeRas)
                .setParameter("taux", new BigDecimal(taux)));
        return id;
    }

    /** Paramètre {@code ManageRS}. */
    public void gestionRs(boolean active) {
        achat.parametreBooleen(MANAGE_RS, active);
    }

    /** Le fournisseur du cadre porte une retenue ({@code CfgTier.RsRatio}, {@code StlDeductionTypeId}). */
    public void fournisseurAvecRs(Saisie s, UUID type, String taux) {
        executer(em.createNativeQuery("UPDATE CfgTier SET RsRatio = :taux, StlDeductionTypeId = :type WHERE PKey = :id")
                .setParameter(P_ID, s.c().fournisseur()).setParameter("taux", new BigDecimal(taux))
                .setParameter("type", type));
        em.clear();
    }

    /** Séquence P.P (PKey de prod, sans société, {@code 21/%05d}) à la valeur donnée ; les autres lignes P.P retirées. */
    public void sequencePp(int valeur) {
        executer(em.createNativeQuery("DELETE FROM AdmSequence WHERE Code = :code AND PKey <> :id")
                .setParameter(P_CODE, CODE_SEQUENCE_PP).setParameter(P_ID, SEQUENCE_PP));
        executer(em.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM AdmSequence WHERE PKey = :id)
                            INSERT INTO AdmSequence (PKey, Code, SeqYear, SeqYearNumber, SeqValue, SeqValueNumber, Separator,
                                BasedOnSystemYear, CfgCompanyId, TypeCode, SeqPrefix, SeqSuffix)
                            VALUES (:id, :code, 2021, 2, 0, 5, N'/', 0, NULL, NULL, N'', N'')""")
                .setParameter(P_ID, SEQUENCE_PP).setParameter(P_CODE, CODE_SEQUENCE_PP));
        executer(em.createNativeQuery("""
                        UPDATE AdmSequence SET Code = :code, SeqValue = :valeur, SeqYear = 2021, SeqYearNumber = 2,
                            SeqValueNumber = 5, Separator = N'/', BasedOnSystemYear = 0, CfgCompanyId = NULL, TypeCode = NULL,
                            SeqPrefix = N'', SeqSuffix = N''
                        WHERE PKey = :id""")
                .setParameter(P_ID, SEQUENCE_PP).setParameter(P_CODE, CODE_SEQUENCE_PP).setParameter(P_VALEUR, valeur));
    }

    /** Aucune séquence P.P en base. */
    public void sansSequencePp() {
        executer(em.createNativeQuery("DELETE FROM AdmSequence WHERE PKey = :id OR Code = :code")
                .setParameter(P_ID, SEQUENCE_PP).setParameter(P_CODE, CODE_SEQUENCE_PP));
    }

    /** Séquence C.R.A.S de la société ({@code TypeCode = RSSheetCode}, {@code 21%03d}), remplacée. */
    public void sequenceCras(UUID societe, int valeur) {
        sansSequenceCras(societe);
        executer(em.createNativeQuery("""
                        INSERT INTO AdmSequence (PKey, Code, SeqYear, SeqYearNumber, SeqValue, SeqValueNumber, Separator,
                            BasedOnSystemYear, CfgCompanyId, TypeCode, SeqPrefix, SeqSuffix)
                        VALUES (:id, :code, 2021, 2, :valeur, 3, N'', 0, :societe, :type, N'', N'')""")
                .setParameter(P_ID, UUID.randomUUID()).setParameter(P_CODE, CODE_SEQUENCE_CRAS)
                .setParameter(P_VALEUR, valeur).setParameter(P_SOCIETE, societe).setParameter("type", TYPE_SEQUENCE_CRAS));
    }

    /** Aucune séquence C.R.A.S pour la société. */
    public void sansSequenceCras(UUID societe) {
        executer(em.createNativeQuery("DELETE FROM AdmSequence WHERE TypeCode = :type AND CfgCompanyId = :societe")
                .setParameter("type", TYPE_SEQUENCE_CRAS).setParameter(P_SOCIETE, societe));
    }

    /** {@code SeqValue} de la séquence P.P. */
    public int valeurSequencePp() {
        return entier(achat.lire("SELECT SeqValue AS v FROM AdmSequence WHERE PKey = :id", SEQUENCE_PP));
    }

    /** {@code SeqValue} de la séquence C.R.A.S de la société. */
    public int valeurSequenceCras(UUID societe) {
        return entier(achat.lire("SELECT SeqValue AS v FROM AdmSequence WHERE TypeCode = N'" + TYPE_SEQUENCE_CRAS
                + "' AND CfgCompanyId = :id", societe));
    }

    private static int entier(List<Map<String, Object>> lignes) {
        if (lignes.size() != 1) {
            throw new AssertionError("Séquence : " + lignes.size() + " ligne(s) en base");
        }
        return ((Number) lignes.get(0).get("v")).intValue();
    }

    /** Codes de feuille posés en base, contexte vidé. */
    public void poserCodes(UUID reglement, String codePp, String codeCras) {
        achat.poserSur(REGLEMENT, reglement, "PropositionSheetCode", codePp);
        achat.poserSur(REGLEMENT, reglement, "RSSheetCode", codeCras);
        em.clear();
    }

    /** Lignes {@code StlDeduction} du règlement, par base croissante. */
    public List<Map<String, Object>> retenuesEnBase(UUID reglement) {
        return achat.lire("""
                SELECT CAST(StlDeductionTypeId AS varchar(36)) AS type, AmountBase AS base, RsRatio AS taux,
                    AmountRS AS montantRs, Amount AS montant
                FROM StlDeduction WHERE StlSettlementId = :id ORDER BY AmountBase ASC""", reglement);
    }

    /** Montants RS et codes de feuille de la ligne {@code StlSettlement}. */
    public Map<String, Object> rsEnBase(UUID reglement) {
        List<Map<String, Object>> lignes = achat.lire("""
                SELECT Amount AS montant, AmountBase AS base, AmountRS AS montantRs, RsRatio AS taux,
                    PropositionSheetCode AS codePp, RSSheetCode AS codeCras
                FROM StlSettlement WHERE PKey = :id""", reglement);
        if (lignes.size() != 1) {
            throw new AssertionError("Règlement " + reglement + " : " + lignes.size() + " ligne(s) en base");
        }
        return lignes.get(0);
    }

    /** Codes de feuille ({@code codePp}, {@code codeCras}) du règlement. */
    public Map<String, Object> codesEnBase(UUID reglement) {
        Map<String, Object> rs = rsEnBase(reglement);
        Map<String, Object> codes = new java.util.LinkedHashMap<>();
        codes.put("codePp", rs.get("codePp"));
        codes.put("codeCras", rs.get("codeCras"));
        return codes;
    }

    public static String texte(UUID id) {
        return id.toString().toUpperCase();
    }

    private void executer(Query query) {
        em.flush();
        query.executeUpdate();
    }
}
