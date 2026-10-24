package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComAction;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureEtatVentes.Nature;
import com.tsix.immocrm.repositories.com.ComActionRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementTypeRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Rapports R3b (FEATURE-tdb-vente-rapports-commercial.md §3, §6, §9.1) : statuts de vente à seuils {@code AdmParamsGuid},
 * prospections typées, dossiers datés, actions commerciales, versements. Tout est annulé avec la transaction du test.
 */
@Component
public class FixtureTransformationVente {

    public static final String SEUIL_CONCRETISATION = "DefaultComFolderStatusConcretisation";
    public static final String SEUIL_FINALISATION = "DefaultComFolderStatusFinalisation";
    public static final String SEUIL_LIVRAISON = "DefaultComFolderStatusDelevery";

    /** Statuts propres au test, {@code Sorting} de la prod : 1, 2, 5, 7, 8 ; sorties 30 (annulation) et 31 (désistement). */
    public record StatutsVente(StrStatus reservation, StrStatus concretisation, StrStatus credit, StrStatus minute,
                               StrStatus livre, StrStatus annule, StrStatus desiste) {
    }

    /** Regroupe les dépôts écrits par la fixture (S107). */
    @Component
    public record DepotsTransformation(StrStatusRepository statuts, ComProspectionRepository prospections,
                                       ComActionRepository actions, StlSettlementTypeRepository typesReglement,
                                       StlSettlementRepository reglements, StlSettlementLineRepository lignes) {
    }

    private final FixtureEtatVentes ev;
    private final FixtureSuiviCommerciaux sc;
    private final DepotsTransformation depots;
    private final JdbcTemplate jdbc;
    private final EntityManager entityManager;

    public FixtureTransformationVente(FixtureEtatVentes ev, FixtureSuiviCommerciaux sc,
                                      DepotsTransformation depots, JdbcTemplate jdbc, EntityManager entityManager) {
        this.ev = ev;
        this.sc = sc;
        this.depots = depots;
        this.jdbc = jdbc;
        this.entityManager = entityManager;
    }

    // ------------------------------------------------------------------ statuts et seuils

    /** Crée les statuts et pose les trois seuils {@code AdmParamsGuid} (absents de yesRemsTest). */
    public StatutsVente statutsVente(String prefixe) {
        StatutsVente s = new StatutsVente(enCours("RES-" + prefixe, 1), enCours("CONC-" + prefixe, 2),
                enCours("CRED-" + prefixe, 5), enCours("MIN-" + prefixe, 7), enCours("CLES-" + prefixe, 8),
                sortie("ANN-" + prefixe, true, 30), sortie("DES-" + prefixe, false, 31));
        parametre(SEUIL_CONCRETISATION, s.concretisation().getId());
        parametre(SEUIL_FINALISATION, s.minute().getId());
        parametre(SEUIL_LIVRAISON, s.livre().getId());
        return s;
    }

    private StrStatus enCours(String code, int ordre) {
        StrStatus s = ev.statutEnCours(code);
        s.setSorting(ordre);
        return depots.statuts().save(s);
    }

    private StrStatus sortie(String code, boolean annulation, int ordre) {
        StrStatus s = ev.statutSortie(code, annulation);
        s.setSorting(ordre);
        return depots.statuts().save(s);
    }

    public void parametre(String code, UUID valeur) {
        entityManager.flush();
        jdbc.update("""
                IF EXISTS (SELECT 1 FROM AdmParamsGuid WHERE Code = ?)
                    UPDATE AdmParamsGuid SET CodeValue = ? WHERE Code = ?
                ELSE
                    INSERT INTO AdmParamsGuid (Code, CodeValue, PKey) VALUES (?, ?, NEWID())""",
                code, valeur.toString(), code, code, valeur.toString());
    }

    public void retirerParametre(String code) {
        entityManager.flush();
        jdbc.update("DELETE FROM AdmParamsGuid WHERE Code = ?", code);
    }

    // ------------------------------------------------------------------ prospections, actions

    /** Type d'interaction de code et libellé {@code code}. */
    public ComActionMessageType type(String code) {
        return sc.type(code);
    }

    /** Prospection de vente du jour {@code jour} ; {@code commercial}, {@code type} et {@code action} peuvent être nuls. */
    public ComProspection prospection(CfgTranche tranche, AdmUser commercial, ComActionMessageType type,
                                      LocalDate jour, ComAction action) {
        String code = "PRS-" + UUID.randomUUID().toString().substring(0, 8);
        ComProspection p = sc.prospection(tranche, sc.personne(code), commercial, jour.atTime(10, 0));
        p.setComActionMessageType(type);
        p.setComAction(action);
        return depots.prospections().save(p);
    }

    public ComAction action(String code, LocalDate debut, LocalDate fin, BigDecimal budget) {
        ComAction a = new ComAction();
        a.setId(UUID.randomUUID());
        a.setCode(code);
        a.setDescription(code);
        a.setStartDate(debut == null ? null : debut.atStartOfDay());
        a.setEndDate(fin == null ? null : fin.atStartOfDay());
        a.setBudget(budget);
        a.setIsActive(true);
        return depots.actions().save(a);
    }

    // ------------------------------------------------------------------ dossiers

    /**
     * Dossier VENTE sur un nouvel article racine de {@code tranche} (une seule vente vivante par article : trigger
     * {@code trgAfterComFolderInsert}), client renseigné ; {@code commercial} et {@code statut} peuvent être nuls.
     */
    public ComFolder dossier(String code, CfgTranche tranche, AdmUser commercial, StrStatus statut,
                             ComProspection prospection) {
        StkItem article = ev.article(code + "-L", tranche, ev.categorie("CAT-" + code), ev.type("TYP-" + code));
        ComFolder d = ev.vendre(article, code, statut, ev.client(code), null);
        d.setCfgCommercial(commercial == null ? null : commercial.getCfgEmployee());
        d.setComProspection(prospection);
        return ev.enregistrer(d);
    }

    public ComFolder enregistrer(ComFolder dossier) {
        return ev.enregistrer(dossier);
    }

    /** Statut de la table {@link StatutDossier} (Sorting = ordinal), pour un dossier hors seuils du test. */
    public StrStatus statut(StatutDossier statut) {
        return ev.statut(statut);
    }

    // ------------------------------------------------------------------ versements

    /**
     * Ligne d'affectation de {@code montant} sur une nouvelle échéance de {@code nature} ; règlement daté par son
     * échéance seule ({@code SettlementDate} NULL ⇒ date du mouvement = {@code EcheanceDate}). {@code encaissement} =
     * {@code StlSettlement.IsCredit}, nul possible ; {@code comptabilise} = NOT {@code IsNotContabilized}.
     */
    public StlSettlementLine versement(ComFolder dossier, Nature nature, Boolean encaissement, boolean comptabilise,
                                       LocalDate date, BigDecimal montant) {
        ComDocument echeance = ev.echeance(dossier, nature, true, montant);

        StlSettlementType type = new StlSettlementType();
        type.setId(UUID.randomUUID());
        type.setCode("TYP-REG-" + UUID.randomUUID().toString().substring(0, 8));
        type.setIsNotContabilized(!comptabilise);
        depots.typesReglement().save(type);

        StlSettlement s = new StlSettlement();
        s.setId(UUID.randomUUID());
        s.setCode("REG-" + UUID.randomUUID().toString().substring(0, 8));
        s.setStlSettlementType(type);
        s.setTier(dossier.getCfgCustomer());
        s.setIsCredit(encaissement);
        s.setEcheanceDate(date.atTime(10, 0));
        s.setDocDate(date.atTime(10, 0));
        s.setAmount(montant);
        s = depots.reglements().save(s);

        StlSettlementLine l = new StlSettlementLine();
        l.setId(UUID.randomUUID());
        l.setStlSettlement(s);
        l.setComDocument(echeance);
        l.setAmount(montant);
        return depots.lignes().save(l);
    }

    public void relire() {
        ev.relire();
    }
}
