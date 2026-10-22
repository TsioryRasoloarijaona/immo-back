package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementTypeRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Flux de vente mensuels (FEATURE-tdb-vente-rapports-flux.md §3, §12.1) : statuts aux {@code Sorting} de la prod, paramètres
 * {@code AdmParamsGuid}, dossiers datés, échéances et règlements. Parents avant enfants (FK de la CI) ; un dossier vivant par
 * article (trigger {@code trgAfterComFolderInsert}) ; tout est annulé avec la transaction du test.
 */
@Component
public class FixtureFluxVente {

    public static final String SEUIL_CONCRETISATION = "DefaultComFolderStatusConcretisation";
    public static final String SEUIL_FINALISATION = "DefaultComFolderStatusFinalisation";
    public static final String SEUIL_LIVRAISON = "DefaultComFolderStatusDelevery";
    public static final String FRAIS_DOSSIER = "DefaultFeeDocumentType";
    public static final String FRAIS_AUTRES = "DefaultOtherFeeDocumentType";
    public static final String FRAIS_TRANSFERT_CLIENT = "DefaultCustomerTransfertDocumentType";
    public static final String FRAIS_TRANSFERT_PRODUIT = "DefaultProductTransfertDocumentType";
    public static final String FRAIS_SYNDIC = "DefaultSyndicFeeDocumentType";
    public static final String FRAIS_ANNULATION = "DefaultCancellationDocumentType";

    public static final String IN_WORK = "IN_WORK";
    public static final String POINTED = "POINTED";
    public static final String CANCELED = "CANCELED";

    /** Nature d'une catégorie d'échéance ; tous les autres drapeaux sont posés à faux (jamais NULL). */
    public enum Genre { AVANCE, AVANCE_HORS_ACCORD, AUTOFINANCEMENT, CREDIT, FRAIS, RESILIATION }

    /** Statuts du jeu, {@code Sorting} de la prod (spec §1). */
    public record Statuts(StrStatus reservation, StrStatus concretisation, StrStatus minute, StrStatus remiseClefs,
                          StrStatus annulation, StrStatus desistement) {
    }

    /** Dépôts écrits par la fixture (S107). */
    @Component
    public record Depots(StrStatusRepository statuts, ComSettlementCategoryRepository categories,
                         ComDocumentRepository echeances, StlSettlementTypeRepository types,
                         StlSettlementRepository reglements, StlSettlementLineRepository lignes) {
    }

    private final FixtureEtatVentes ev;
    private final Depots depots;
    private final JdbcTemplate jdbc;
    private final EntityManager entityManager;

    public FixtureFluxVente(FixtureEtatVentes ev, Depots depots, JdbcTemplate jdbc, EntityManager entityManager) {
        this.ev = ev;
        this.depots = depots;
        this.jdbc = jdbc;
        this.entityManager = entityManager;
    }

    public Jeu jeu(String racine) {
        return new Jeu(FixtureEtatVentes.prefixe(racine));
    }

    // ------------------------------------------------------------------ paramètres AdmParamsGuid (clé = Code)

    /** Pose (ou remplace) le paramètre ; {@code valeur} nulle = paramètre présent sans valeur. */
    public void parametre(String code, UUID valeur) {
        entityManager.flush();
        jdbc.update("DELETE FROM AdmParamsGuid WHERE Code = ?", code);
        jdbc.update("INSERT INTO AdmParamsGuid (Code, CodeValue, PKey) VALUES (?, ?, ?)", code,
                valeur == null ? null : valeur.toString(), UUID.randomUUID().toString());
    }

    public void retirerParametre(String code) {
        entityManager.flush();
        jdbc.update("DELETE FROM AdmParamsGuid WHERE Code = ?", code);
    }

    public void relire() {
        ev.relire();
    }

    /** Jeu d'un test : préfixe unique, statuts et seuils posés, référentiel d'article commun. */
    public final class Jeu {

        private final String prefixe;
        private final Statuts statuts;
        private final StkItemCategory categorieArticle;
        private final StkItemType typeArticle;
        private final StlSettlementType typeComptabilise;
        private int compteur;

        private Jeu(String prefixe) {
            this.prefixe = prefixe;
            this.statuts = new Statuts(statut("RES", 1, false, false), statut("CONC", 2, false, false),
                    statut("MIN", 7, false, false), statut("CLEFS", 8, false, false),
                    statut("ANN", 30, true, false), statut("DES", 31, false, true));
            parametre(SEUIL_CONCRETISATION, statuts.concretisation().getId());
            parametre(SEUIL_FINALISATION, statuts.minute().getId());
            parametre(SEUIL_LIVRAISON, statuts.remiseClefs().getId());
            this.categorieArticle = ev.categorie("CAT-" + prefixe);
            this.typeArticle = ev.type("TYP-" + prefixe);
            this.typeComptabilise = typeReglement(true);
        }

        public String prefixe() {
            return prefixe;
        }

        public Statuts statuts() {
            return statuts;
        }

        private String code(String nature) {
            compteur++;
            return nature + "-" + prefixe + "-" + compteur;
        }

        private StrStatus statut(String code, int sorting, boolean annulation, boolean desistement) {
            StrStatus s = new StrStatus();
            s.setId(UUID.randomUUID());
            s.setCode(code + "-" + prefixe);
            s.setDescription(code + "-" + prefixe);
            s.setSorting(sorting);
            s.setStrConfigEntityId(StatutDossier.CONFIG_ENTITY_ID);
            s.setVenteEffective(!annulation && !desistement && sorting > 1);
            s.setAnnulation(annulation);
            s.setResiliation(desistement);
            return depots.statuts().save(s);
        }

        // -------------------------------------------------------------- patrimoine

        /** Projet d'une entité active ou non ; libellé « Programme {suffixe}-{préfixe} ». */
        public CfgProject projet(String suffixe, boolean entiteActive) {
            return ev.projet(suffixe + "-" + prefixe, entiteActive);
        }

        /** Tranche en vente ({@code IsForSale = 1}) ; libellé « Tranche {suffixe}-{préfixe} ». */
        public CfgTranche tranche(String suffixe, CfgProject projet) {
            return ev.tranche(suffixe + "-" + prefixe, projet, true);
        }

        // -------------------------------------------------------------- dossiers

        /** Dossier de vente (vocation VENTE) sur un article racine propre, prix 1 200 000 ; {@code statut} peut être nul. */
        public ComFolder dossier(CfgTranche tranche, StrStatus statut) {
            StkItem article = ev.article(code("ART"), tranche, categorieArticle, typeArticle);
            return ev.vendre(article, code("DOS"), statut, null, null);
        }

        /** Dossier sur un sous-article ({@code IsUnderItem = 1}) d'un article racine libre de la tranche. */
        public ComFolder dossierSousArticle(CfgTranche tranche, StrStatus statut) {
            StkItem parent = ev.article(code("ART"), tranche, categorieArticle, typeArticle);
            StkItem sous = ev.sousArticle(code("SOUS"), parent, categorieArticle, typeArticle);
            return ev.vendre(sous, code("DOS"), statut, null, null);
        }

        /** Dates du dossier : concrétisation, acte, livraison réelle, sortie ({@code CancelDate}) ; nulles permises. */
        public ComFolder dater(ComFolder dossier, LocalDate avance, LocalDate acte, LocalDate livraison,
                               LocalDate sortie) {
            dossier.setAdvanceDate(instant(avance));
            dossier.setAgreementSavingDate(instant(acte));
            dossier.setDeliveryDateReal(instant(livraison));
            dossier.setCancelDate(instant(sortie));
            return ev.enregistrer(dossier);
        }

        /** {@code Price} (TTC) et {@code PriceHT} ; nuls permis. */
        public ComFolder prix(ComFolder dossier, BigDecimal ttc, BigDecimal ht) {
            dossier.setPrice(ttc);
            dossier.setPriceHT(ht);
            return ev.enregistrer(dossier);
        }

        public ComFolder remise(ComFolder dossier, BigDecimal montant) {
            dossier.setDiscountAmount(montant);
            return ev.enregistrer(dossier);
        }

        // -------------------------------------------------------------- échéances et règlements

        /** Catégorie propre au test ; {@code IsInAgrementContext} vrai pour {@link Genre#AVANCE} seulement. */
        public ComSettlementCategory categorie(Genre genre, boolean auCredit) {
            ComSettlementCategory c = new ComSettlementCategory();
            c.setId(UUID.randomUUID());
            c.setCode(code(genre.name().substring(0, 3)));
            c.setDescription(c.getCode());
            c.setIsCredit(auCredit);
            c.setIsSaleAdvance(genre == Genre.AVANCE || genre == Genre.AVANCE_HORS_ACCORD);
            c.setIsSaleAutoFinancment(genre == Genre.AUTOFINANCEMENT);
            c.setIsSaleCredit(genre == Genre.CREDIT);
            c.setIsSaleExpense(genre == Genre.FRAIS);
            c.setIsSaleModification(false);
            c.setIsSalePenality(false);
            c.setIsSaleResiliation(genre == Genre.RESILIATION);
            ComSettlementCategory sauvee = depots.categories().save(c);
            // Colonne non mappée par l'entité : posée en SQL après l'insertion.
            entityManager.flush();
            jdbc.update("UPDATE ComSettlementCategory SET IsInAgrementContext = ? WHERE PKey = ?",
                    genre == Genre.AVANCE, sauvee.getId().toString());
            return sauvee;
        }

        public ComDocument echeance(ComFolder dossier, ComSettlementCategory categorie, BigDecimal montant,
                                    LocalDate date) {
            ComDocument d = new ComDocument();
            d.setId(UUID.randomUUID());
            d.setComFolder(dossier);
            d.setComSettlementCategory(categorie);
            d.setAmount(montant);
            d.setDocDate(instant(date));
            d.setExcludeFromPrice(Boolean.TRUE.equals(categorie.getIsSaleExpense()));
            return depots.echeances().save(d);
        }

        public StlSettlementType typeReglement(boolean comptabilise) {
            StlSettlementType type = new StlSettlementType();
            type.setId(UUID.randomUUID());
            type.setCode(code("TREG"));
            type.setIsNotContabilized(!comptabilise);
            return depots.types().save(type);
        }

        /** Ligne de règlement (pièce comptabilisée) ; {@code encaissement} = {@code StlSettlement.IsCredit}, nul permis. */
        public StlSettlementLine regler(ComDocument echeance, BigDecimal montant, Boolean encaissement, String statut,
                                        LocalDate dateReglement, LocalDate dateEcheance) {
            return regler(echeance, montant, encaissement, statut, dateReglement, dateEcheance, typeComptabilise);
        }

        public StlSettlementLine regler(ComDocument echeance, BigDecimal montant, Boolean encaissement, String statut,
                                        LocalDate dateReglement, LocalDate dateEcheance, StlSettlementType type) {
            StlSettlement s = new StlSettlement();
            s.setId(UUID.randomUUID());
            s.setCode(code("RG"));
            s.setStlSettlementType(type);
            s.setIsCredit(encaissement);
            s.setStatus(statut);
            s.setSettlementDate(instant(dateReglement));
            s.setEcheanceDate(instant(dateEcheance));
            s.setDocDate(instant(dateEcheance != null ? dateEcheance : dateReglement));
            s.setAmount(montant);
            depots.reglements().save(s);
            StlSettlementLine l = new StlSettlementLine();
            l.setId(UUID.randomUUID());
            l.setStlSettlement(s);
            l.setComDocument(echeance);
            l.setAmount(montant);
            return depots.lignes().save(l);
        }

        /** Encaissement simple : échéance d'autofinancement au crédit réglée en {@code IN_WORK}, datée par l'échéance. */
        public StlSettlementLine encaisser(ComFolder dossier, String montant, LocalDate date) {
            BigDecimal valeur = new BigDecimal(montant);
            ComDocument e = echeance(dossier, categorie(Genre.AUTOFINANCEMENT, true), valeur, date);
            return regler(e, valeur, true, IN_WORK, null, date);
        }
    }

    private static LocalDateTime instant(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }
}
