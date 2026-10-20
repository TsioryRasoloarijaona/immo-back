package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.entities.str.StrStatus;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Dossier de vente prêt pour un désistement (FEATURE-desistements.md) : tranche paramétrée (taux, base, remise ancien
 * client), dossier RESERVATION au prix {@link #PRIX}, échéance d'avance réglée, frais « toujours payés » restant dus,
 * catégories « FRAIS DE DÉSISTEMENT » (paramètre {@code DefaultCancellationDocumentType}) et « REMBOURSEMENT POUR
 * DÉSISTEMENT ». Colonnes non mappées ({@code BoolField010}, {@code IsAlwaysPayed}) posées en SQL natif.
 */
@Component
public class FixtureDesistements {

    public static final String CODE_FRAIS_DESISTEMENT = "FRAIS DE DÉSISTEMENT";
    public static final String CODE_REMBOURSEMENT_DESISTEMENT = "REMBOURSEMENT POUR DÉSISTEMENT";
    public static final String CODE_REMBOURSEMENT_ANNULATION = "REMBOURSEMENT POUR ANNULATION";
    public static final String PARAMETRE_FRAIS = "DefaultCancellationDocumentType";
    public static final BigDecimal PRIX = new BigDecimal("1000000");
    public static final BigDecimal FRAIS_RESTANT_DUS = new BigDecimal("20000");
    public static final BigDecimal FRAIS_NON_TOUJOURS_PAYES = new BigDecimal("5000");
    private static final String ID = "id";

    private final FixtureDossierVente fixture;
    private final FixtureStatutsDossier statuts;
    private final FixtureTrancheOnglets onglets;
    private final EntityManager em;

    public FixtureDesistements(FixtureDossierVente fixture, FixtureStatutsDossier statuts,
                               FixtureTrancheOnglets onglets, EntityManager em) {
        this.fixture = fixture;
        this.statuts = statuts;
        this.onglets = onglets;
        this.em = em;
    }

    /** Paramètres d'une scène : taux et remise en fraction, {@code regle} = encaissé sur l'avance. */
    public record Parametres(BigDecimal taux, boolean basePrix, BigDecimal remise, boolean ancienClient,
                             BigDecimal regle, boolean statutEligible) {

        public static Parametres standard() {
            return new Parametres(new BigDecimal("0.10"), true, new BigDecimal("0.50"), false,
                    new BigDecimal("300000"), true);
        }

        public Parametres avecTaux(BigDecimal valeur) {
            return new Parametres(valeur, basePrix, remise, ancienClient, regle, statutEligible);
        }

        public Parametres avecBaseRegle() {
            return new Parametres(taux, false, remise, ancienClient, regle, statutEligible);
        }

        public Parametres avecAncienClient() {
            return new Parametres(taux, basePrix, remise, true, regle, statutEligible);
        }

        public Parametres avecRegle(BigDecimal valeur) {
            return new Parametres(taux, basePrix, remise, ancienClient, valeur, statutEligible);
        }

        public Parametres avecStatutNonEligible() {
            return new Parametres(taux, basePrix, remise, ancienClient, regle, false);
        }
    }

    /** Ce que la scène a créé ; le contexte de persistance est vidé à la fin, seuls les ids font foi. */
    public record Scene(UUID dossierId, UUID articleId, UUID trancheId, UUID projetId, UUID categorieFraisId,
                        UUID categorieRemboursementId, UUID categorieRemboursementAnnulationId,
                        UUID typeReglementId) {
    }

    public Scene scene(String code, Parametres p) {
        CfgTranche tranche = fixture.tranche("TR-" + code, true);
        tranche.setCancellationPenalityPercent(p.taux());
        tranche.setCancellationPenalityBasedOnItemPrice(p.basePrix());
        tranche.setCancellationPenalityPercentDiscount(p.remise());
        em.merge(tranche);

        StrStatus reservation = statuts.reservation();
        statuts.statut(StatutDossier.DESISTEMENT);
        ComFolder dossier = fixture.dossier("DES-" + code, tranche, null);
        dossier.setStrStatus(reservation);
        dossier.setPrice(PRIX);
        dossier.setPriceAfterDiscount(PRIX);
        dossier.setIsOldCustomer(p.ancienClient());
        dossier = em.merge(dossier);

        ComDocument avance = echeance(dossier, categorie("TST-DES-AVANCE", Drapeaux.AVANCE), PRIX);
        regler(avance, p.regle());
        ComSettlementCategory fraisDossier = categorie("TST-DES-FRAIS-DOSSIER", Drapeaux.FRAIS);
        echeance(dossier, fraisDossier, FRAIS_RESTANT_DUS);
        echeance(dossier, categorie("TST-DES-FRAIS-LIBRE", Drapeaux.FRAIS), FRAIS_NON_TOUJOURS_PAYES);

        ComSettlementCategory frais = categorie(CODE_FRAIS_DESISTEMENT, Drapeaux.FRAIS);
        ComSettlementCategory remboursementAnnulation = categorie(CODE_REMBOURSEMENT_ANNULATION, Drapeaux.RESILIATION_CREDIT);
        ComSettlementCategory remboursement = categorie(CODE_REMBOURSEMENT_DESISTEMENT, Drapeaux.RESILIATION_DEBIT);
        StlSettlementType type = typeReglement();
        UUID articleId = dossier.getStkItem().getId();
        UUID projetId = tranche.getCfgProject().getId();
        em.flush();

        toujoursPaye(fraisDossier.getId());
        toujoursPaye(frais.getId());
        onglets.parametreGuid(PARAMETRE_FRAIS, frais.getId());
        eligible(reservation.getId(), p.statutEligible());
        return new Scene(dossier.getId(), articleId, tranche.getId(), projetId, frais.getId(), remboursement.getId(),
                remboursementAnnulation.getId(), type.getId());
    }

    /** {@code StrStatus.BoolField010} (« Peut effectuer un désistement »), non mappé. */
    public void eligible(UUID statutId, boolean valeur) {
        em.flush();
        em.createNativeQuery("UPDATE StrStatus SET BoolField010 = :v WHERE PKey = :id")
                .setParameter("v", valeur).setParameter(ID, statutId).executeUpdate();
        em.clear();
    }

    /** Compte aux rôles donnés, habilité au projet {@code projetId} (périmètre commercial par projet). */
    public AdmUser compteHabilite(String code, UUID projetId, String... roles) {
        AdmUser compte = fixture.compte(code, roles);
        AdmUserHabilitation habilitation = new AdmUserHabilitation();
        habilitation.setId(UUID.randomUUID());
        habilitation.setAdmUser(em.getReference(AdmUser.class, compte.getId()));
        habilitation.setCfgProject(em.getReference(CfgProject.class, projetId));
        em.persist(habilitation);
        em.flush();
        return compte;
    }

    /** Dossier de syndic en cours rattaché au dossier (refus de sortie de {@code ControleSortieDossier}). */
    public void syndicEnCours(UUID dossierId) {
        UUID statut = UUID.randomUUID();
        em.createNativeQuery("INSERT INTO StrStatus (PKey, Code, BoolField001, BoolField002) VALUES (:id, :code, 0, 0)")
                .setParameter(ID, statut).setParameter("code", "TST-DES-SYN-" + statut).executeUpdate();
        em.createNativeQuery("INSERT INTO SynFolder (PKey, Code, ComFolderId, StrStatusId) "
                        + "VALUES (:id, 'TST-DES-SYN', :dossier, :statut)")
                .setParameter(ID, UUID.randomUUID()).setParameter("dossier", dossierId)
                .setParameter("statut", statut).executeUpdate();
    }

    /** Motif {@code ComSaleWithdrawalReason} (table créée par le script 136 dans yesRemsTest). */
    public UUID motif(String libelle) {
        UUID id = UUID.randomUUID();
        em.createNativeQuery("INSERT INTO ComSaleWithdrawalReason (PKey, Code, Description, Sorting) "
                        + "VALUES (:id, :libelle, :libelle, 1)")
                .setParameter(ID, id).setParameter("libelle", libelle).executeUpdate();
        return id;
    }

    /** Pièce générée d'un dossier : montant et désistement lié, lus en SQL natif. */
    public record Piece(String categorie, BigDecimal montant, String desistementId) {
    }

    public List<Piece> pieces(UUID dossierId, String... codesCategorie) {
        em.flush();
        List<?> lignes = em.createNativeQuery("""
                        SELECT c.Code, d.Amount, CAST(d.ComSaleWithdrawalCancellationId AS NVARCHAR(36))
                        FROM ComDocument d JOIN ComSettlementCategory c ON c.PKey = d.ComSettlementCategoryId
                        WHERE d.ComFolderId = :dossier AND c.Code IN (:codes)""")
                .setParameter("dossier", dossierId).setParameter("codes", List.of(codesCategorie)).getResultList();
        return lignes.stream().map(Object[].class::cast)
                .map(l -> new Piece((String) l[0], (BigDecimal) l[1], (String) l[2])).toList();
    }

    /** Lignes de règlement affectées aux pièces d'une catégorie du dossier : [IsCredit, Amount]. */
    public record LigneReglement(Boolean credit, BigDecimal montant) {
    }

    public List<LigneReglement> reglementsSurCategorie(UUID dossierId, String codeCategorie) {
        em.flush();
        List<?> lignes = em.createNativeQuery("""
                        SELECT s.IsCredit, l.Amount
                        FROM StlSettlementLine l
                        JOIN StlSettlement s ON s.PKey = l.StlSettlementId
                        JOIN ComDocument d ON d.PKey = l.ComDocumentId
                        JOIN ComSettlementCategory c ON c.PKey = d.ComSettlementCategoryId
                        WHERE d.ComFolderId = :dossier AND c.Code = :code""")
                .setParameter("dossier", dossierId).setParameter("code", codeCategorie).getResultList();
        return lignes.stream().map(Object[].class::cast)
                .map(l -> new LigneReglement((Boolean) l[0], (BigDecimal) l[1])).toList();
    }

    /** Désistement relu en SQL natif : [ComSaleId, ValidityDate]. */
    public record Desistement(String dossierId, Object dateValidation) {
    }

    public Desistement desistement(UUID id) {
        em.flush();
        Object[] l = (Object[]) em.createNativeQuery("""
                        SELECT CAST(ComSaleId AS NVARCHAR(36)), ValidityDate
                        FROM ComSaleWithdrawalCancellation WHERE PKey = :id""")
                .setParameter(ID, id).getSingleResult();
        return new Desistement((String) l[0], l[1]);
    }

    // ------------------------------------------------------------------ interne

    private enum Drapeaux { AVANCE, FRAIS, RESILIATION_DEBIT, RESILIATION_CREDIT }

    private ComSettlementCategory categorie(String code, Drapeaux drapeaux) {
        ComSettlementCategory c = new ComSettlementCategory();
        c.setId(UUID.randomUUID());
        c.setCode(code);
        c.setDescription(code);
        c.setIsCredit(drapeaux != Drapeaux.RESILIATION_DEBIT);
        c.setIsSaleAdvance(drapeaux == Drapeaux.AVANCE);
        c.setIsSaleExpense(drapeaux == Drapeaux.FRAIS);
        c.setIsSaleResiliation(drapeaux == Drapeaux.RESILIATION_DEBIT || drapeaux == Drapeaux.RESILIATION_CREDIT);
        c.setIsSalePenality(false);
        c.setIsSaleModification(false);
        em.persist(c);
        return c;
    }

    private ComDocument echeance(ComFolder dossier, ComSettlementCategory categorie, BigDecimal montant) {
        ComDocument d = new ComDocument();
        d.setId(UUID.randomUUID());
        d.setComFolder(dossier);
        d.setComSettlementCategory(categorie);
        d.setAmount(montant);
        d.setDocDate(LocalDateTime.of(2026, 1, 15, 0, 0));
        em.persist(d);
        return d;
    }

    private void regler(ComDocument echeance, BigDecimal montant) {
        StlSettlement reglement = new StlSettlement();
        reglement.setId(UUID.randomUUID());
        reglement.setAmount(montant);
        reglement.setIsCredit(true);
        reglement.setDocDate(LocalDateTime.of(2026, 2, 15, 0, 0));
        em.persist(reglement);
        StlSettlementLine ligne = new StlSettlementLine();
        ligne.setId(UUID.randomUUID());
        ligne.setStlSettlement(reglement);
        ligne.setComDocument(echeance);
        ligne.setAmount(montant);
        em.persist(ligne);
    }

    private StlSettlementType typeReglement() {
        StlSettlementType type = new StlSettlementType();
        type.setId(UUID.randomUUID());
        type.setCode("TST-DES-VIREMENT");
        type.setDescription("Virement désistement");
        type.setIsReferenceMandatory(false);
        type.setIsBankMandatory(false);
        em.persist(type);
        return type;
    }

    private void toujoursPaye(UUID categorieId) {
        em.createNativeQuery("UPDATE ComSettlementCategory SET IsAlwaysPayed = 1 WHERE PKey = :id")
                .setParameter(ID, categorieId).executeUpdate();
    }
}
