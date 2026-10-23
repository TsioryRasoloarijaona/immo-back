package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComAction;
import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureRapportsCommerciaux;
import com.tsix.immocrm.services.equipe.RolesEquipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.controller.rapport.LecturesRapportsCommerciaux.repartition;
import static com.tsix.immocrm.controller.rapport.LecturesRapportsCommerciaux.typeParametre;
import static com.tsix.immocrm.controller.rapport.LecturesRapportsCommerciaux.typesGraphiques;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * N° 31 ANALYSE DES ACTIONS COMMERCIALES (FEATURE-tdb-vente-rapports-commercial.md §9.2, décision 8, §10). Année 2077 :
 * <pre>
 *   T  : S1 6 (dont 4 de l'action A1) + 1 le 31/12 à 23:30 ; S2 3 (dont 1 de A1) ; S3 1 (action A2) ;
 *        S1 le 01/01/2078 à 00:30 (hors période)
 *   T2 : 3 S1 (projet hors périmètre du commercial U)   TI : 2 S1 (société inactive)   sans tranche : 1 S2
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AnalyseActionsCommercialesRapportTest {

    private static final String CODE = "ComSaleDashboardAnalyseActionCommerciale";
    private static final String CODE_19 = "ComTdbProspectionState";
    private static final int ANNEE = 2077;
    private static final String DU = "du";
    private static final String AU = "au";
    private static final String TRANCHE_ID = "trancheId";
    private static final String ACTION_ID = "actionId";
    private static final String SOURCES = "sources";
    private static final String NOMBRE = "nombre";
    private static final String PREMIER_JANVIER = ANNEE + "-01-01";
    private static final String TRENTE_ET_UN_DECEMBRE = ANNEE + "-12-31";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureRapportsCommerciaux rc;

    private AppelsRapports appels;
    private String prefixe;
    private CfgProject projet;
    private CfgTranche tranche;
    private CfgTranche trancheP2;
    private CfgTranche trancheInactive;
    private AdmUser admin;
    private ComAction a1;

    private static LocalDateTime le(int mois, int jour) {
        return LocalDate.of(ANNEE, mois, jour).atTime(10, 0);
    }

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        prefixe = FixtureEtatVentes.prefixe("R31");
        fixture.tables();
        fixture.rapportVente(CODE, "ANALYSE DES ACTIONS COMMERCIALES*", "VENTE", 2080);
        fixture.rapportVente(CODE_19, "SUIVI DES PROSPECTIONS", "", 1700);

        projet = rc.projet("P-" + prefixe, true);
        tranche = rc.tranche("T-" + prefixe, projet);
        trancheP2 = rc.tranche("T2-" + prefixe, rc.projet("P2-" + prefixe, true));
        trancheInactive = rc.tranche("TI-" + prefixe, rc.projet("PI-" + prefixe, false));
        admin = rc.compte("r31.adm." + prefixe, new CfgProject[0], fixture.role("Administrateur", false));

        ComProspectionOrigin s1 = rc.origine("S1-" + prefixe);
        ComProspectionOrigin s2 = rc.origine("S2-" + prefixe);
        ComProspectionOrigin s3 = rc.origine("S3-" + prefixe);
        a1 = rc.action("A1-" + prefixe, le(1, 1), le(6, 30));
        ComAction a2 = rc.action("A2-" + prefixe, le(1, 1), le(6, 30));

        for (int i = 0; i < 6; i++) {
            rc.prospection(tranche, le(2, 10), null, s1, null, i < 4 ? a1 : null);
        }
        rc.prospection(tranche, LocalDate.of(ANNEE, 12, 31).atTime(23, 30), null, s1, null, null);
        rc.prospection(tranche, LocalDate.of(ANNEE + 1, 1, 1).atTime(0, 30), null, s1, null, null);
        for (int i = 0; i < 3; i++) {
            rc.prospection(tranche, le(2, 11), null, s2, null, i == 0 ? a1 : null);
        }
        rc.prospection(tranche, le(2, 12), null, s3, null, a2);
        for (int i = 0; i < 3; i++) {
            rc.prospection(trancheP2, le(2, 13), null, s1, null, null);
        }
        for (int i = 0; i < 2; i++) {
            rc.prospection(trancheInactive, le(2, 14), null, s1, null, null);
        }
        rc.prospection(null, le(2, 15), null, s2, null, null);
        rc.relire();
    }

    private MockHttpServletRequestBuilder annee(AdmUser qui, CfgTranche... tranches) {
        MockHttpServletRequestBuilder requete = get(resultat(VENTE, CODE)).param(DU, PREMIER_JANVIER)
                .param(AU, TRENTE_ET_UN_DECEMBRE).with(connecte(qui));
        for (CfgTranche t : tranches) {
            requete.param(TRANCHE_ID, t.getId().toString());
        }
        return requete;
    }

    private Map<String, Object> sources(MockHttpServletRequestBuilder requete) throws Exception {
        return section(appels.sections(requete), SOURCES);
    }

    // ------------------------------------------------------------------ descripteur, droits

    @Test
    void r31_descripteur_periode_tranches_actionsMultiples() throws Exception {
        String corps = appels.corps(get(descripteur(VENTE, CODE)).with(connecte(admin)));

        assertThat(typeParametre(corps, DU)).isEqualTo("DATE");
        assertThat(typeParametre(corps, AU)).isEqualTo("DATE");
        assertThat(typeParametre(corps, TRANCHE_ID)).isEqualTo("TRANCHES");
        assertThat(typeParametre(corps, ACTION_ID)).isEqualTo("CHOIX");
        List<Boolean> multiple = JsonPath.read(corps, "$.parametres[?(@.nom == 'actionId')].multiple");
        List<String> options = JsonPath.read(corps, "$.parametres[?(@.nom == 'actionId')].options[*].valeur");
        assertThat(multiple).containsExactly(true);
        assertThat(options).contains(a1.getId().toString());
    }

    @Test
    void transversal_roleNonLie_403() throws Exception {
        AdmUser restreint = rc.compte("r31.r." + prefixe, new CfgProject[0], fixture.role("Restreint " + prefixe, true));
        fixture.relire();

        assertThat(appels.statut(annee(restreint, tranche))).isEqualTo(403);
    }

    // ------------------------------------------------------------------ nominal

    /** Un seul camembert : prospections par source sur [du ; au], au inclus en jour entier. */
    @Test
    void r31_sources_unCamembert_periodeInclusive() throws Exception {
        Map<String, Object> sources = sources(annee(admin, tranche));

        assertThat(typesGraphiques(sources)).containsExactly("SECTEURS");
        assertThat(repartition(sources)).containsExactly(entry("S1-" + prefixe, 7L), entry("S2-" + prefixe, 3L),
                entry("S3-" + prefixe, 1L));
    }

    @Test
    void r31_auInclusEnJourEntier_lendemainExclu() throws Exception {
        Map<String, Object> sources = sources(get(resultat(VENTE, CODE)).param(DU, TRENTE_ET_UN_DECEMBRE)
                .param(AU, TRENTE_ET_UN_DECEMBRE).param(TRANCHE_ID, tranche.getId().toString()).with(connecte(admin)));

        assertThat(repartition(sources)).containsExactly(entry("S1-" + prefixe, 1L));
    }

    /** Q8 : actionId = prospections rattachées aux actions choisies. */
    @Test
    void r31_actionId_prospectionsDeLActionSeulement() throws Exception {
        Map<String, Object> sources = sources(annee(admin, tranche).param(ACTION_ID, a1.getId().toString()));

        assertThat(repartition(sources)).containsExactly(entry("S1-" + prefixe, 4L), entry("S2-" + prefixe, 1L));
    }

    /** Un seul calcul : n° 31 sur [1/1 ; 31/12] ≡ section prospections-source du n° 19 de la même année. */
    @Test
    void re_sources_egalesALaSectionSourceDuN19() throws Exception {
        Map<String, Object> n31 = sources(annee(admin, tranche));
        Map<String, Object> n19 = section(appels.sections(get(resultat(VENTE, CODE_19))
                .param("annee", String.valueOf(ANNEE)).param(TRANCHE_ID, tranche.getId().toString())
                .with(connecte(admin))), "prospections-source");

        assertThat(lignes(n31)).isNotEmpty().isEqualTo(lignes(n19));
    }

    // ------------------------------------------------------------------ RV-R3a

    @Test
    void rv_trancheSocieteInactive_aucunChiffre() throws Exception {
        assertThat(lignes(sources(annee(admin, trancheInactive)))).isEmpty();
    }

    /** Sans trancheId : T (11) + T2 (3) + sans tranche (1), jamais TI ; avec T : 11. */
    @Test
    void rv_prospectionSansTranche_compteeSeulementSansTrancheId() throws Exception {
        assertThat(somme(lignes(sources(annee(admin))), NOMBRE)).isEqualByComparingTo("15");
        assertThat(somme(lignes(sources(annee(admin, tranche))), NOMBRE)).isEqualByComparingTo("11");
    }

    @Test
    void rv_projetHorsPerimetre_aucunChiffre() throws Exception {
        AdmUser u = rc.compte("r31.u." + prefixe, new CfgProject[] {projet}, fixture.role("Libre " + prefixe, false),
                fixture.role(RolesEquipe.COMMERCIAL, true));
        fixture.relire();

        assertThat(lignes(sources(annee(u, trancheP2)))).isEmpty();
    }
}
