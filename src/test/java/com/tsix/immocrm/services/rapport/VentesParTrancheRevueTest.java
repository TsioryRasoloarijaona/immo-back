package com.tsix.immocrm.services.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.ColonneDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureEtatVentes.Nature;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.services.dossier.liste.MontantsParNatureDossier;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureRapports.SUIVI_GLOBAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue R0 : section {@code ventes} du n° 12 (FEATURE-tdb-vente-rapports-socle.md §6, RV2, RV3) — même calcul que
 * {@link MontantsParNatureDossier}, dossier sans statut gardé, désisté exclu, tranche hors périmètre jamais chiffrée.
 * <pre>
 *   TA (projet A) : dossier sans statut, avance 100 000 au crédit, réglée 40 000 ; dossier DÉSISTÉ, avance 555 555
 *   TB (projet B) : dossier en cours, avance 300 000
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VentesParTrancheRevueTest {

    private static final String RESULTAT = "/api/rapports/vente/" + SUIVI_GLOBAL + "/resultat";
    private static final String TRANCHE_ID = "trancheId";
    private static final String TRANCHE_LIBELLE = "trancheLibelle";
    private static final String AVANCE_MONTANT = "avanceMontant";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private MontantsParNatureDossier montants;
    @Autowired private ComFolderRepository dossiers;

    private RequestPostProcessor libre;
    private RequestPostProcessor perimetreA;
    private CfgTranche ta;
    private CfgTranche tb;
    private ComFolder sansStatut;

    @BeforeEach
    void setUp() {
        String prefixe = FixtureEtatVentes.prefixe("VRV");
        fixture.tables();
        fixture.rapportVente(SUIVI_GLOBAL, "SUIVI GLOBAL DE VENTE*", "", 810);
        libre = connecte(fixture.compte("vrv.l." + prefixe, fixture.role("Libre L " + prefixe, false)));
        AdmUser compteA = fixture.compte("vrv.a." + prefixe, fixture.role("Libre A " + prefixe, false));
        perimetreA = connecte(compteA);

        CfgProject projetA = ev.projet("PA-" + prefixe, true);
        CfgProject projetB = ev.projet("PB-" + prefixe, true);
        ev.affecter(compteA, projetA);
        ta = ev.tranche("TA-" + prefixe, projetA, true);
        tb = ev.tranche("TB-" + prefixe, projetB, true);
        StkItemCategory categorie = ev.categorie("CAT-" + prefixe);
        StkItemType type = ev.type("TYP-" + prefixe);

        sansStatut = ev.prix(ev.vendre(ev.article(prefixe + "-A1", ta, categorie, type), "DOS-" + prefixe + "-A1",
                null, null, null), new BigDecimal("1000000"));
        ComDocument avance = ev.echeance(sansStatut, Nature.AVANCE, true, new BigDecimal("100000"));
        ev.regler(avance, new BigDecimal("40000"), true, null);

        StrStatus desiste = ev.statutSortie("DES-" + prefixe, false);
        ComFolder dossierDesiste = ev.prix(ev.vendre(ev.article(prefixe + "-A2", ta, categorie, type),
                "DOS-" + prefixe + "-A2", desiste, null, null), new BigDecimal("555555"));
        ev.echeance(dossierDesiste, Nature.AVANCE, true, new BigDecimal("555555"));

        ComFolder enCours = ev.prix(ev.vendre(ev.article(prefixe + "-B1", tb, categorie, type),
                "DOS-" + prefixe + "-B1", ev.statutEnCours("EC-" + prefixe), null, null), new BigDecimal("900000"));
        ev.echeance(enCours, Nature.AVANCE, true, new BigDecimal("300000"));
        fixture.relire();
    }

    private List<Map<String, Object>> ventes(RequestPostProcessor qui, CfgTranche... tranches) throws Exception {
        MockHttpServletRequestBuilder requete = get(RESULTAT).with(qui);
        for (CfgTranche t : tranches) {
            requete.param(TRANCHE_ID, t.getId().toString());
        }
        String corps = mockMvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        return JsonPath.read(corps, "$.sections[?(@.id == 'ventes')].lignes[*]");
    }

    private static BigDecimal nombre(Object valeur) {
        return valeur == null ? null : new BigDecimal(String.valueOf(valeur));
    }

    /** Un seul calcul (règle 6) : la ligne de TA = MontantsParNatureDossier du seul dossier retenu (désisté exclu). */
    @Test
    void ventes_egalMontantsParNatureDossier_sansStatutGarde_desisteExclu() throws Exception {
        List<Map<String, Object>> lignes = ventes(libre, ta);
        Map<ColonneDossier, BigDecimal> attendu = montants.pour(List.of(dossiers.findById(sansStatut.getId())
                .orElseThrow())).get(sansStatut.getId());

        assertThat(lignes).hasSize(1);
        Map<String, Object> ligne = lignes.get(0);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(ligne.get(AVANCE_MONTANT))).isEqualByComparingTo(attendu.get(ColonneDossier.AVANCE))
                .isEqualByComparingTo("100000");
        soft.assertThat(nombre(ligne.get("avanceRegle"))).isEqualByComparingTo(attendu.get(ColonneDossier.AVANCE_REGLEE))
                .isEqualByComparingTo("40000");
        soft.assertThat(nombre(ligne.get("avanceReste"))).isEqualByComparingTo(attendu.get(ColonneDossier.AVANCE_RESTE));
        soft.assertThat(nombre(ligne.get("prixRegle"))).isEqualByComparingTo(attendu.get(ColonneDossier.PRIX_REGLE));
        soft.assertThat(nombre(ligne.get("prixReste"))).isEqualByComparingTo(attendu.get(ColonneDossier.PRIX_RESTE));
        soft.assertThat(nombre(ligne.get("prixMontant"))).isEqualByComparingTo("1000000");
        soft.assertAll();
    }

    /** RV2 sur l'argent : un compte au périmètre « projet A » ne reçoit jamais les montants de TB. */
    @Test
    void ventes_perimetreRestreint_trancheHorsPerimetreJamaisChiffree() throws Exception {
        List<Map<String, Object>> demandeeHors = ventes(perimetreA, tb);
        List<Map<String, Object>> sansFiltre = ventes(perimetreA);

        assertThat(demandeeHors).isEmpty();
        assertThat(sansFiltre).extracting(l -> l.get(TRANCHE_LIBELLE))
                .contains(ta.getDescription()).doesNotContain(tb.getDescription());
    }
}
