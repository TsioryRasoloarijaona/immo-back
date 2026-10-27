package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.str.StrField;
import com.tsix.immocrm.entities.str.StrStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lot COM — Vente › Commissions (FEATURE-vente-commissions.md §3, §4, §8.1). Lignes de commission, règles, documents et
 * règlements écrits en SQL natif : les tests ne dépendent pas des entités que le lot crée. Parents écrits avant les FK
 * (la CI les porte) ; tout est annulé avec la transaction du test.
 */
@Component
public class FixtureCommissions {

    // ------------------------------------------------------------------ droits AdmRight (§3.2)
    public static final String ECRAN = "Yes.Com.Front.Commission.CommissionList";
    public static final String NOUVEAU = ECRAN + ".toolNew";
    public static final String SUPPRIMER = ECRAN + ".toolDelete";
    public static final String IMPRIMER = ECRAN + ".toolPrint";

    // ------------------------------------------------------------------ valeurs stockées de CommissionType (§4)
    public static final String CONCRETISATION = "Concrétisation";
    public static final String FINALISATION_1 = "Finalisation 1";
    public static final String FINALISATION_2 = "Finalisation 2";
    public static final String DESISTEMENT = "Désistement";

    // ------------------------------------------------------------------ PKey de production (document à payer, §1)
    public static final UUID MODULE_COM = UUID.fromString("00002C2D-BDFA-4541-94F7-C708A0E00001");
    public static final UUID ENTITE_DOCUMENT_COMMISSION = UUID.fromString("E7999F70-0000-1111-5555-A2825C42B666");

    /** Rôle du jeton d'authentification : jamais Administrateur, les droits viennent d'AdmRight seul. */
    public static final String ROLE_JWT = "Assistante Commerciale";

    // Absentes de yesRemsTest et de la CI avant le lot (ci/schema 146) ; définition de yesRemsProd.
    private static final String TABLE_LIGNES = """
            IF OBJECT_ID('dbo.ComCommissionLine', 'U') IS NULL
            CREATE TABLE dbo.ComCommissionLine (
                PKey uniqueidentifier NOT NULL PRIMARY KEY,
                DocDate datetime NULL,
                CfgEmployeeId uniqueidentifier NULL,
                ComFolderId uniqueidentifier NULL,
                ComCommissionId uniqueidentifier NULL,
                CommissionType nvarchar(255) NULL,
                CommissionAmount decimal(26, 16) NULL,
                StkItemCategoryId uniqueidentifier NULL,
                StkItemId uniqueidentifier NULL,
                CfgTrancheId uniqueidentifier NULL,
                UserCreate nvarchar(255) NULL,
                UserCreateDateTime datetime NULL,
                UserUpdate nvarchar(255) NULL,
                UserUpdateDateTime datetime NULL,
                BuyDocumentId uniqueidentifier NULL,
                CalculationMethod nvarchar(255) NULL,
                StrFolderStatusId uniqueidentifier NULL)""";
    private static final String TABLE_VENTILATION = """
            IF OBJECT_ID('dbo.BuyVentilationExpense', 'U') IS NULL
            CREATE TABLE dbo.BuyVentilationExpense (
                Pkey uniqueidentifier NOT NULL PRIMARY KEY,
                PrjProjectId uniqueidentifier NULL,
                BuyDocumentId uniqueidentifier NULL,
                VentilationRate decimal(26, 6) NULL)""";

    private static final String P_ID = "id";

    private final EntityManager em;
    private final JdbcTemplate jdbc;
    private final FixtureEtatVentes ventes;
    private final FixtureRapports rapports;
    private final FixtureWorkflow workflow;
    private final FixtureTrancheRegles regles;
    private final FixtureTerrains terrains;

    @SuppressWarnings("java:S107")
    public FixtureCommissions(EntityManager em, JdbcTemplate jdbc, FixtureEtatVentes ventes, FixtureRapports rapports,
                              FixtureWorkflow workflow, FixtureTrancheRegles regles, FixtureTerrains terrains) {
        this.em = em;
        this.jdbc = jdbc;
        this.ventes = ventes;
        this.rapports = rapports;
        this.workflow = workflow;
        this.regles = regles;
        this.terrains = terrains;
    }

    /** Références d'un jeu ; {@code jeton} suffixe tous les codes (yesRemsTest est partagée). */
    public record Contexte(String jeton, CfgProject projet, CfgTranche tranche, StkItemCategory categorie,
                           StkItemType type, StrStatus statut) {
    }

    /** Dossier de vente et son article (un article par dossier : trigger {@code trgAfterComFolderInsert}). */
    public record Dossier(UUID id, String code, UUID articleId, UUID trancheId, UUID categorieId) {
    }

    /** Compte de test et son rôle propre, qui porte les droits {@code AdmRight} donnés. */
    public record Compte(AdmUser compte, AdmRole role) {

        public RequestPostProcessor qui() {
            return FixtureDossierVente.connecte(compte, ROLE_JWT);
        }
    }

    // ------------------------------------------------------------------ tables

    /** Idempotent ; dans la transaction du test (annulé avec elle) ou validé hors transaction (test d'audit). */
    public void tables() {
        jdbc.execute(TABLE_LIGNES);
        jdbc.execute(TABLE_VENTILATION);
    }

    // ------------------------------------------------------------------ patrimoine

    public Contexte contexte(String prefixe) {
        tables();
        String jeton = FixtureTerrains.jeton(prefixe);
        CfgProject projet = ventes.projet("PRJ-" + jeton, true);
        // Statut propre au jeu : relire un statut partagé par son code bloque face aux autres agents (deadlocks).
        return new Contexte(jeton, projet, ventes.tranche("TR-" + jeton, projet, true), ventes.categorie("CAT-" + jeton),
                ventes.type("TYP-" + jeton), ventes.statutEnCours("CONC-" + jeton));
    }

    /** Autre tranche en vente, sur un nouveau projet d'une entité active ou non. */
    public CfgTranche autreTranche(Contexte c, String suffixe, boolean entiteActive) {
        CfgProject projet = ventes.projet("PRJ-" + suffixe + "-" + c.jeton(), entiteActive);
        return ventes.tranche("TR-" + suffixe + "-" + c.jeton(), projet, true);
    }

    /** Projet de facturation ({@code PrjProject}) posé sur la tranche ({@code CfgTranche.PrjProjectId}). */
    public UUID chantier(CfgTranche tranche, String code) {
        UUID id = UUID.randomUUID();
        UUID entite = tranche.getCfgProject().getCfgCompany().getId();
        executer(em.createNativeQuery("""
                        INSERT INTO PrjProject (PKey, Code, Description, CfgCompanyId) VALUES (:id, :code, :code, :entite)""")
                .setParameter(P_ID, id).setParameter("code", code).setParameter("entite", entite));
        executer(em.createNativeQuery("UPDATE CfgTranche SET PrjProjectId = :projet WHERE PKey = :id")
                .setParameter("projet", id).setParameter(P_ID, tranche.getId()));
        return id;
    }

    // ------------------------------------------------------------------ personnes

    /** Tiers commissionné ({@code CfgTier}) ; libellés contenant {@code nom} (Description et LastName). */
    public UUID personne(String nom, boolean externe) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO CfgTier (PKey, Code, Description, FirstName, LastName, IsExtern)
                        VALUES (:id, :code, :description, N'Employe', :nom, :externe)""")
                .setParameter(P_ID, id).setParameter("code", "EMP-" + nom)
                .setParameter("description", "Employe " + nom).setParameter("nom", nom)
                .setParameter("externe", externe));
        return id;
    }

    // ------------------------------------------------------------------ dossiers

    /** Statut de sortie propre au jeu ({@code BoolField003}, désistement). */
    public StrStatus statutDesiste(Contexte c) {
        return ventes.statutSortie("DES-" + c.jeton(), false);
    }

    /** Dossier au statut en cours du jeu : commercial, prix TTC après remise, date de concrétisation. */
    public Dossier dossier(Contexte c, CfgTranche tranche, String code, UUID commercial, BigDecimal prix,
                           LocalDate avance) {
        return dossier(c, tranche, code, c.statut(), commercial, prix, avance);
    }

    public Dossier dossier(Contexte c, String code, UUID commercial, BigDecimal prix, LocalDate avance) {
        return dossier(c, c.tranche(), code, commercial, prix, avance);
    }

    /** {@code statut} peut être nul (dossier sans statut, Q8). */
    public Dossier dossier(Contexte c, CfgTranche tranche, String code, StrStatus statut, UUID commercial,
                           BigDecimal prix, LocalDate avance) {
        String codeDossier = code + "-" + c.jeton();
        StkItem article = ventes.article("ART-" + codeDossier, tranche, c.categorie(), c.type());
        ComFolder folder = ventes.vendre(article, codeDossier, statut, ventes.client("CLI-" + codeDossier), null);
        Dossier d = new Dossier(folder.getId(), codeDossier, article.getId(), tranche.getId(), c.categorie().getId());
        poser(d, "CfgCommercialId", commercial);
        poser(d, "PriceAfterDiscount", prix);
        poser(d, "AdvanceDate", avance == null ? null : avance.atStartOfDay());
        return d;
    }

    /** {@code colonne} : nom de colonne de {@code ComFolder} écrit par le test, jamais une saisie. */
    public void poser(Dossier d, String colonne, Object valeur) {
        if (valeur == null) {
            executer(em.createNativeQuery("UPDATE ComFolder SET " + colonne + " = NULL WHERE PKey = :id")
                    .setParameter(P_ID, d.id()));
            return;
        }
        Object v = valeur instanceof LocalDate jour ? jour.atStartOfDay() : valeur;
        executer(em.createNativeQuery("UPDATE ComFolder SET " + colonne + " = :valeur WHERE PKey = :id")
                .setParameter(P_ID, d.id()).setParameter("valeur", v));
    }

    // ------------------------------------------------------------------ règles ComSalesCommission

    /** Règle de la tranche : paires colonne / valeur de {@code ComSalesCommission} (noms écrits par le test). */
    public UUID regle(UUID trancheId, Object... colonnesValeurs) {
        UUID id = UUID.randomUUID();
        List<String> colonnes = new ArrayList<>(List.of("PKey", "CfgTrancheId"));
        List<Object> valeurs = new ArrayList<>(List.of(id.toString(), trancheId.toString()));
        for (int i = 0; i + 1 < colonnesValeurs.length; i += 2) {
            colonnes.add((String) colonnesValeurs[i]);
            valeurs.add(jdbcValeur(colonnesValeurs[i + 1]));
        }
        em.flush();
        String marques = String.join(",", java.util.Collections.nCopies(valeurs.size(), "?"));
        jdbc.update("INSERT INTO ComSalesCommission (" + String.join(",", colonnes) + ") VALUES (" + marques + ")",
                valeurs.toArray());
        return id;
    }

    /** Concrétisation à montant fixe, sans critère ni fenêtre. */
    public UUID regleMontant(UUID trancheId, BigDecimal montant) {
        return regle(trancheId, "SaleCommissionBasedOnAmount", true, "SaleCommissionAmount", montant);
    }

    private static Object jdbcValeur(Object valeur) {
        return switch (valeur) {
            case null -> null;
            case UUID u -> u.toString();
            case LocalDate d -> java.sql.Timestamp.valueOf(d.atStartOfDay());
            case LocalDateTime d -> java.sql.Timestamp.valueOf(d);
            default -> valeur;
        };
    }

    // ------------------------------------------------------------------ finalisations (R-F)

    /** Statut de finalisation propre au test ; {@code colonneDate} = {@code StrField.Code} (nul = statut sans date). */
    public UUID statutFinalisation(Contexte c, String code, String colonneDate) {
        regles.entiteDossierVente();
        StrStatus s = ventes.statutEnCours(code + "-" + c.jeton());
        if (colonneDate != null) {
            StrField champ = workflow.champ(StatutDossier.CONFIG_ENTITY_ID, colonneDate, colonneDate);
            em.flush();
            executer(em.createNativeQuery("UPDATE StrStatus SET StrFieldDateId = :champ WHERE PKey = :id")
                    .setParameter("champ", champ.getId()).setParameter(P_ID, s.getId()));
        }
        return s.getId();
    }

    /** Ligne {@code CfgTrancheFolderEntity} de la tranche ; statuts nuls possibles. */
    public void finalisations(CfgTranche tranche, UUID statut1, UUID statut2) {
        regles.ligneFinalisation(tranche.getId(), FixtureTrancheRegles.ENTITE_DOSSIER_VENTE, statut1, statut2);
    }

    // ------------------------------------------------------------------ lignes de commission, documents, règlements

    /** Ligne {@code ComCommissionLine} écrite directement ; {@code documentId} nul = sans document à payer. */
    public UUID ligne(Dossier d, String type, UUID personne, BigDecimal montant, LocalDate date, UUID documentId) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO ComCommissionLine (PKey, DocDate, CfgEmployeeId, ComFolderId, CommissionType,
                            CommissionAmount, StkItemCategoryId, StkItemId, CfgTrancheId, BuyDocumentId, CalculationMethod,
                            UserCreate, UserCreateDateTime)
                        VALUES (:id, :date, :personne, :dossier, :type, :montant, :categorie, :article, :tranche, :document,
                            N'Montant', N'TEST', :date)""")
                .setParameter(P_ID, id).setParameter("date", date.atStartOfDay()).setParameter("personne", personne)
                .setParameter("dossier", d.id()).setParameter("type", type).setParameter("montant", montant)
                .setParameter("categorie", d.categorieId()).setParameter("article", d.articleId())
                .setParameter("tranche", d.trancheId()).setParameter("document", documentId));
        return id;
    }

    /** Document à payer minimal ({@code BuyDocument}) du tiers, comme ceux que le lot écrit. */
    public UUID document(UUID personne, String code, BigDecimal montant) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO BuyDocument (PKey, Code, DocType, ReferenceType, CfgTierId, AmountTTC, AmountHT, AmountToPay)
                        VALUES (:id, :code, N'INVOICE', N'SALE_COMMISSION', :tiers, :montant, :montant, :montant)""")
                .setParameter(P_ID, id).setParameter("code", code).setParameter("tiers", personne)
                .setParameter("montant", montant));
        return id;
    }

    /** Règlement (décaissement) d'un document à payer : une ligne {@code StlSettlementLine.BuyDocumentId}. */
    public void reglement(UUID documentId, BigDecimal montant, String statut) {
        UUID reglement = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO StlSettlement (PKey, Code, DocDate, Amount, Status, IsCredit)
                        VALUES (:id, :code, :date, :montant, :statut, 0)""")
                .setParameter(P_ID, reglement).setParameter("code", "RGC-" + reglement.toString().substring(0, 8))
                .setParameter("date", LocalDateTime.of(2026, 3, 1, 0, 0)).setParameter("montant", montant)
                .setParameter("statut", statut));
        executer(em.createNativeQuery("""
                        INSERT INTO StlSettlementLine (PKey, StlSettlementId, BuyDocumentId, Amount)
                        VALUES (:id, :reglement, :document, :montant)""")
                .setParameter(P_ID, UUID.randomUUID()).setParameter("reglement", reglement)
                .setParameter("document", documentId).setParameter("montant", montant));
    }

    // ------------------------------------------------------------------ configuration du document à payer (R-G)

    /**
     * Séquence « Facture fournisseur » (valeur 3013), devise, statuts de facture (FixtureTerrains), module COM, entité
     * {@code ComInvoiceDebitCommission} et son workflow, {@code AdmParamsGuid.DefaultMarketType} (NULL comme en prod).
     */
    public void configurationDocument() {
        terrains.configurationFacture();
        terrains.sequence(FixtureTerrains.Sequence.commeEnProduction(3013));
        executer(em.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM AdmModule WHERE Code = N'COM')
                            INSERT INTO AdmModule (PKey, Code, Description) VALUES (:id, N'COM', N'Vente')""")
                .setParameter(P_ID, MODULE_COM));
        executer(em.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM StrEntity WHERE Code = N'ComInvoiceDebitCommission')
                            INSERT INTO StrEntity (PKey, Code, Description) VALUES (:id, N'ComInvoiceDebitCommission', N'Commission')""")
                .setParameter(P_ID, ENTITE_DOCUMENT_COMMISSION));
        if (terrains.tableExiste("BuyDocumentWorkFlow") && compter("""
                SELECT COUNT(*) FROM BuyDocumentWorkFlow w JOIN StrEntity e ON e.PKey = w.StrEntityId
                WHERE e.Code = N'ComInvoiceDebitCommission' AND w.PKey <> :id""", UUID.randomUUID()) == 0) {
            executer(em.createNativeQuery("""
                            INSERT INTO BuyDocumentWorkFlow (PKey, Sorting, StartStatusId, EndStatusId, StrEntityId, Description)
                            VALUES (:id, 10, :depuis, :vers, :entite, N'En cours => Validé(e)')""")
                    .setParameter(P_ID, UUID.randomUUID()).setParameter("depuis", FixtureTerrains.STATUT_EN_COURS)
                    .setParameter("vers", FixtureTerrains.STATUT_REGLE).setParameter("entite", entiteDocument()));
        }
        executer(em.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM AdmParamsGuid WHERE Code = N'DefaultMarketType')
                            INSERT INTO AdmParamsGuid (Code, CodeValue, PKey) VALUES (N'DefaultMarketType', NULL, :id)""")
                .setParameter(P_ID, UUID.randomUUID()));
    }

    public UUID entiteDocument() {
        return premierGuid("SELECT TOP 1 PKey FROM StrEntity WHERE Code = N'ComInvoiceDebitCommission' ORDER BY PKey ASC");
    }

    public UUID moduleCom() {
        return premierGuid("SELECT TOP 1 PKey FROM AdmModule WHERE Code = N'COM' ORDER BY PKey ASC");
    }

    public int valeurSequence() {
        return terrains.valeurSequence();
    }

    public void masquerSequence() {
        terrains.executer("UPDATE AdmSequence SET Code = N'Facture fournisseur-MASQUEE' WHERE Code = N'Facture fournisseur'");
    }

    // ------------------------------------------------------------------ comptes et droits (§3.2)

    /** Compte actif, rôle propre non exempté du périmètre, droits {@code AdmRight} donnés (aucun = sans l'écran). */
    public Compte compte(String jeton, String... droits) {
        String suffixe = UUID.randomUUID().toString().substring(0, 6);
        AdmRole role = rapports.role("Commissions " + jeton + "-" + suffixe, true);
        AdmUser user = rapports.compte("com." + jeton + "." + suffixe, role);
        for (String droit : droits) {
            workflow.droit(role.getId(), droit);
        }
        em.flush();
        return new Compte(user, role);
    }

    public void retirerDroit(AdmRole role, String droit) {
        executer(em.createNativeQuery("DELETE FROM AdmRight WHERE AdmRoleId = :id AND Code = :code")
                .setParameter(P_ID, role.getId()).setParameter("code", droit));
    }

    /** Périmètre commercial : le compte ne voit que les projets affectés. */
    public void affecter(Compte compte, CfgProject projet) {
        ventes.affecter(compte.compte(), projet);
        em.flush();
    }

    // ------------------------------------------------------------------ lecture

    public List<Tuple> lignesDuDossier(UUID dossierId) {
        return lignes("SELECT * FROM ComCommissionLine WHERE ComFolderId = :id", dossierId);
    }

    public List<Tuple> lignesDuDossier(UUID dossierId, String type) {
        return lignesDuDossier(dossierId).stream().filter(l -> type.equals(l.get("CommissionType"))).toList();
    }

    @SuppressWarnings("unchecked")
    public List<Tuple> lignes(String sql, UUID id) {
        em.flush();
        em.clear();
        return em.createNativeQuery(sql, Tuple.class).setParameter(P_ID, id).getResultList();
    }

    public Tuple ligneParId(UUID ligneId) {
        List<Tuple> l = lignes("SELECT * FROM ComCommissionLine WHERE PKey = :id", ligneId);
        if (l.size() != 1) {
            throw new IllegalStateException(l.size() + " ligne(s) de commission " + ligneId);
        }
        return l.get(0);
    }

    public int compter(String sql, UUID id) {
        em.flush();
        Object n = em.createNativeQuery(sql).setParameter(P_ID, id).getSingleResult();
        return ((Number) n).intValue();
    }

    public void relire() {
        em.flush();
        em.clear();
    }

    private UUID premierGuid(String sql) {
        em.flush();
        List<?> valeurs = em.createNativeQuery(sql).getResultList();
        return valeurs.isEmpty() || valeurs.get(0) == null ? null : UUID.fromString(valeurs.get(0).toString());
    }

    private void executer(Query query) {
        em.flush();
        query.executeUpdate();
    }
}
