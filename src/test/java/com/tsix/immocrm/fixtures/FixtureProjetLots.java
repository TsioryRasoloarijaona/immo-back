package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lot P1 Projet — lots et décomptes en lecture (FEATURE-projet-lots-decomptes.md §3). Tout en SQL natif : les tests ne
 * dépendent d'aucune entité que le lot crée. Parents avant enfants (FK de la CI) ; annulé avec la transaction du test.
 */
@Component
public class FixtureProjetLots {

    /** Colonnes de {@code PrjWorkingOut} qui entrent dans le net recalculé (§2.3), plus {@code AmountToPay}. */
    public static final String TTC = "AmountTTC";
    public static final String AVANCE = "AdvanceAmount";
    public static final String AVANCE_DECOMPTE = "WorkingOutAdvanceAmount";
    public static final String RG = "WarrantyAmount";
    public static final String RETENUE_NON_REMBOURSABLE = "nonRefundableRetainer";
    public static final String EXTRA = "extraAmount";
    public static final String PRORATA = "ProrataRefundAmount";
    public static final String STOCK = "StockAmountToPay";
    public static final String FLUCTUATIONS = "FluctuationsToPay";
    public static final String A_PAYER = "AmountToPay";

    private static final String P_ID = "id";
    private static final String P_CODE = "code";

    private final EntityManager em;
    private final JdbcTemplate jdbc;
    private final FixtureDossierVente vente;

    public FixtureProjetLots(EntityManager em, JdbcTemplate jdbc, FixtureDossierVente vente) {
        this.em = em;
        this.jdbc = jdbc;
        this.vente = vente;
    }

    /** Suffixe propre au test : yesRemsTest est partagée. */
    public static String jeton() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    // ------------------------------------------------------------------ comptes et habilitations

    /**
     * Compte actif sans aucune ligne d'habilitation, porteur d'un rôle exempté de la clause de statut des lots
     * ({@code WithSequenceFreeRestriction = 0}, FEATURE-visibilite-omran.md R1) : seule l'habilitation projet joue.
     */
    public AdmUser compte(String code) {
        String role = "LIBRE-" + code;
        AdmUser user = vente.compte(code, role);
        em.flush();
        jdbc.update("UPDATE AdmRole SET WithSequenceFreeRestriction = 0 WHERE Code = ?", role);
        return user;
    }

    /** Ligne {@code AdmUserHabilitation.PrjProjectId} : le compte voit ce projet de chantier. */
    public void habiliter(AdmUser compte, UUID prjProjectId) {
        executer(em.createNativeQuery("""
                        INSERT INTO AdmUserHabilitation (PKey, AdmUserId, PrjProjectId) VALUES (:id, :user, :projet)""")
                .setParameter(P_ID, UUID.randomUUID()).setParameter("user", compte.getId())
                .setParameter("projet", prjProjectId));
    }

    // ------------------------------------------------------------------ projet, fournisseur, lot

    public UUID projet(String code) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("INSERT INTO PrjProject (PKey, Code, Description) VALUES (:id, :code, :code)")
                .setParameter(P_ID, id).setParameter(P_CODE, code));
        return id;
    }

    public UUID fournisseur(String code) {
        UUID id = vente.tier(code).getId();
        em.flush();
        return id;
    }

    /** Lot ({@code PrjMarket}) ; {@code projetId} nul = lot sans projet. */
    public UUID lot(UUID projetId, String code, UUID fournisseurId) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO PrjMarket (PKey, Code, Description, PrjProjectId, CfgSupplierId, WarrantyRatio)
                        VALUES (:id, :code, :description, :projet, :fournisseur, 0.1)""")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter("description", "Lot " + code)
                .setParameter("projet", projetId).setParameter("fournisseur", fournisseurId));
        return id;
    }

    // ------------------------------------------------------------------ décomptes

    /**
     * Décompte ({@code PrjWorkingOut}) : colonnes de montant absentes de {@code montants} = NULL. Typé par JDBC : un NULL
     * lié sans type par Hibernate part en varbinary, que SQL Server refuse pour une colonne decimal.
     */
    public UUID decompte(UUID lotId, String code, int numero, Map<String, BigDecimal> montants) {
        UUID id = UUID.randomUUID();
        List<String> colonnes = List.of(TTC, AVANCE, AVANCE_DECOMPTE, RG, RETENUE_NON_REMBOURSABLE, EXTRA, PRORATA, STOCK,
                FLUCTUATIONS, A_PAYER);
        List<Object> valeurs = new ArrayList<>(List.of(id.toString(), code, numero,
                Timestamp.valueOf(LocalDateTime.of(2026, 3, numero, 0, 0)), lotId.toString()));
        colonnes.forEach(c -> valeurs.add(montants.get(c)));
        int[] types = new int[valeurs.size()];
        Arrays.fill(types, Types.DECIMAL);
        types[0] = Types.VARCHAR;
        types[1] = Types.NVARCHAR;
        types[2] = Types.INTEGER;
        types[3] = Types.TIMESTAMP;
        types[4] = Types.VARCHAR;
        em.flush();
        jdbc.update("""
                INSERT INTO PrjWorkingOut (PKey, Code, Sorting, DocDate, PrjMarketId,
                    AmountTTC, AdvanceAmount, WorkingOutAdvanceAmount, WarrantyAmount, nonRefundableRetainer,
                    extraAmount, ProrataRefundAmount, StockAmountToPay, FluctuationsToPay, AmountToPay)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", valeurs.toArray(), types);
        return id;
    }

    // ------------------------------------------------------------------ factures et règlements

    /** Facture du décompte par {@code PrjWorkingOut.BuyDocumentId} (le seul sens retenu, §2.4). */
    public UUID facture(UUID decompteId, String code, BigDecimal aPayer) {
        UUID id = document(code, aPayer, null);
        executer(em.createNativeQuery("UPDATE PrjWorkingOut SET BuyDocumentId = :facture WHERE PKey = :id")
                .setParameter("facture", id).setParameter(P_ID, decompteId));
        return id;
    }

    /** Pièce qui pointe le décompte par {@code BuyDocument.PrjWorkingOutId}, sans réciproque. */
    public UUID pieceSansReciproque(UUID decompteId, String code, BigDecimal aPayer) {
        return document(code, aPayer, decompteId);
    }

    /** Règlement fournisseur ({@code IsCredit = 0}) d'une ligne sur la facture ; {@code statut} : NULL, IN_WORK, POINTED, CANCELED. */
    public UUID reglement(UUID factureId, String code, BigDecimal montant, String statut) {
        UUID reglement = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO StlSettlement (PKey, Code, DocDate, Amount, Status, IsCredit)
                        VALUES (:id, :code, :date, :montant, :statut, 0)""")
                .setParameter(P_ID, reglement).setParameter(P_CODE, code)
                .setParameter("date", LocalDateTime.of(2026, 4, 1, 0, 0)).setParameter("montant", montant)
                .setParameter("statut", statut));
        executer(em.createNativeQuery("""
                        INSERT INTO StlSettlementLine (PKey, StlSettlementId, BuyDocumentId, Amount)
                        VALUES (:id, :reglement, :facture, :montant)""")
                .setParameter(P_ID, UUID.randomUUID()).setParameter("reglement", reglement)
                .setParameter("facture", factureId).setParameter("montant", montant));
        return reglement;
    }

    private UUID document(String code, BigDecimal aPayer, UUID prjWorkingOutId) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO BuyDocument (PKey, Code, DocType, DocDate, AmountHT, AmountTTC, AmountToPay, PrjWorkingOutId)
                        VALUES (:id, :code, N'INVOICE', :date, :montant, :montant, :montant, :decompte)""")
                .setParameter(P_ID, id).setParameter(P_CODE, code)
                .setParameter("date", LocalDateTime.of(2026, 3, 20, 0, 0)).setParameter("montant", aPayer)
                .setParameter("decompte", prjWorkingOutId));
        return id;
    }

    private void executer(Query query) {
        em.flush();
        query.executeUpdate();
    }
}
