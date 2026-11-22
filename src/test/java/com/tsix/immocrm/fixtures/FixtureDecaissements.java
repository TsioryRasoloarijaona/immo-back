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

    public UUID piece(Saisie s, String suffixe, UUID projet, UUID fournisseur, UUID module, String montant) {
        UUID id = tresorerie.piece(s.code(suffixe), s.c().societe(), projet, fournisseur, "Fournisseur " + suffixe,
                module, montant);
        achat.poser(id, "StlCurrencyId", s.devise());
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

    public static String texte(UUID id) {
        return id.toString().toUpperCase();
    }

    private void executer(Query query) {
        em.flush();
        query.executeUpdate();
    }
}
