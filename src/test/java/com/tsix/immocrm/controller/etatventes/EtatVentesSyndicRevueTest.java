package com.tsix.immocrm.controller.etatventes;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureRapportsDossiers;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue règle 6 de FEATURE-etat-ventes-omran.md : totaux syndic, entiteId hors périmètre, ligne libre d'un annulé. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class EtatVentesSyndicRevueTest {

    private static final String URL = "/api/etat-des-ventes";
    private static final String URL_TOTAUX = URL + "/totaux-colonnes";
    private static final String SEARCH = "search";
    private static final String AGREGATS = "agregats";
    private static final String ENTITE_ID = "entiteId";
    private static final String ROLE_COMMERCIAL = "Commercial";
    private static final String SYNDIC = "fraisSyndic,fraisSyndicRegles,fraisSyndicReste";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureEtatVentes fixture;
    @Autowired private FixtureRapportsDossiers rd;
    @Autowired private FixtureDossierVente fixtureDossier;

    private String prefixe;
    private CfgProject projet;
    private CfgTranche tranche;
    private StkItemCategory appartement;
    private StkItemCategory parking;
    private StkItemType type;

    @BeforeEach
    void setUp() {
        prefixe = FixtureEtatVentes.prefixe("EDVR");
        projet = fixture.projet("PRJ-" + prefixe, true);
        tranche = fixture.tranche("TR-" + prefixe, projet, true);
        appartement = fixture.categorie("APP-" + prefixe);
        parking = fixture.categorie("PAR-" + prefixe);
        type = fixture.type("T-" + prefixe);
    }

    private ComFolder vendu(StkItem a) {
        ComFolder d = fixture.vendre(a, "DOS-" + a.getCode(), fixture.statut(StatutDossier.RESERVATION), null,
                LocalDateTime.of(2025, 3, 15, 10, 0));
        return fixture.prix(d, new BigDecimal("1000000"));
    }

    private String corps(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static BigDecimal nombre(Object valeur) {
        return valeur == null ? null : new BigDecimal(String.valueOf(valeur));
    }

    private record Jeu(StkItem visible, CfgProject projetB, AdmUser commercial) {
    }

    /** A (périmètre du commercial) syndic 16 800, B (autre société) syndic 7 000. */
    private Jeu jeu() {
        ComSettlementCategory syndic = rd.categorieSyndic();
        StkItem a = fixture.article(prefixe + "-A", tranche, appartement, type);
        rd.echeance(vendu(a), syndic, new BigDecimal("16800"));
        CfgProject projetB = fixture.projet("PRJB-" + prefixe, true);
        CfgTranche trancheB = fixture.tranche("TRB-" + prefixe, projetB, true);
        StkItem b = fixture.article(prefixe + "-B", trancheB, appartement, type);
        rd.echeance(vendu(b), syndic, new BigDecimal("7000"));
        AdmUser commercial = fixtureDossier.compte("edvr.com." + prefixe);
        fixture.affecter(commercial, projet);
        fixture.relire();
        return new Jeu(a, projetB, commercial);
    }

    /** Totaux syndic = Σ des lignes : 23 800 pour tous, 16 800 pour le commercial restreint. */
    @Test
    void totauxColonnes_syndic_sommeDesLignesVisibles() throws Exception {
        Jeu j = jeu();

        String tous = corps(get(URL_TOTAUX).param(SEARCH, prefixe).param(AGREGATS, SYNDIC));
        String restreint = corps(get(URL_TOTAUX).param(SEARCH, prefixe).param(AGREGATS, SYNDIC)
                .with(connecte(j.commercial(), ROLE_COMMERCIAL)));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(JsonPath.read(tous, "$.totaux.fraisSyndic"))).isEqualByComparingTo("23800");
        soft.assertThat(nombre(JsonPath.read(tous, "$.totaux.fraisSyndicReste"))).isEqualByComparingTo("23800");
        soft.assertThat(nombre(JsonPath.read(tous, "$.totaux.fraisSyndicRegles"))).isEqualByComparingTo("0");
        soft.assertThat(nombre(JsonPath.read(restreint, "$.totaux.fraisSyndic"))).isEqualByComparingTo("16800");
        soft.assertAll();
    }

    /** entiteId hors périmètre : ni groupe, ni total, ni montant syndic pour le commercial restreint. */
    @Test
    void entiteIdHorsPerimetre_groupesEtTotauxVides() throws Exception {
        Jeu j = jeu();
        String entiteB = j.projetB().getCfgCompany().getId().toString();

        List<Object> groupes = JsonPath.read(corps(get(URL + "/groupes").param("groupBy", "trancheCode")
                .param(SEARCH, prefixe).param(ENTITE_ID, entiteB)
                .with(connecte(j.commercial(), ROLE_COMMERCIAL))), "$.content");
        String totaux = corps(get(URL_TOTAUX).param(SEARCH, prefixe).param(ENTITE_ID, entiteB)
                .param(AGREGATS, SYNDIC).with(connecte(j.commercial(), ROLE_COMMERCIAL)));
        Number total = JsonPath.read(totaux, "$.nombreTotal");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(groupes).isEmpty();
        soft.assertThat(total.longValue()).isZero();
        soft.assertThat(nombre(JsonPath.read(totaux, "$.totaux.fraisSyndic"))).isEqualByComparingTo("0");
        soft.assertAll();
    }

    /** Décision (3) : ligne libre parce que son dossier est annulé → syndic, alerte, sous-articles nuls. */
    @Test
    void ligneLibreDossierAnnule_valeursDuDossierNulles() throws Exception {
        ComSettlementCategory syndic = rd.categorieSyndic();
        StkItem a = fixture.article(prefixe + "-AN", tranche, appartement, type);
        ComFolder annule = fixture.vendre(a, "DOS-" + a.getCode(), fixture.statutSortie("ANN-" + prefixe, true),
                null, null);
        rd.echeance(annule, syndic, new BigDecimal("16800"));
        fixture.relire();

        List<Map<String, Object>> lignes = JsonPath.read(corps(get(URL).param("trancheId", tranche.getId().toString())
                .param("colonnes", "articleId,statutCode," + SYNDIC + ",alerte,sousArticles")), "$.content");

        assertThat(lignes).singleElement().satisfies(l -> assertThat(l)
                .containsEntry("statutCode", "LIBRE")
                .containsEntry("fraisSyndic", null).containsEntry("fraisSyndicRegles", null)
                .containsEntry("fraisSyndicReste", null).containsEntry("alerte", null)
                .containsEntry("sousArticles", null));
    }

    /** 1.1 sur /groupes : le parking racine libre n'y forme aucun groupe ni effectif. */
    @Test
    void groupes_parkingRacineLibreNonCompte() throws Exception {
        fixture.parkingAutonome(prefixe + "-PL", tranche, parking, type);
        fixture.article(prefixe + "-AL", tranche, appartement, type);
        fixture.relire();

        String corps = corps(get(URL + "/groupes").param("groupBy", "categorieCode")
                .param("trancheId", tranche.getId().toString()));
        List<Object> cles = JsonPath.read(corps, "$.content[*].cle");
        List<Integer> effectifs = JsonPath.read(corps, "$.content[*].nombre");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(cles).containsExactly(appartement.getCode());
        soft.assertThat(effectifs).containsExactly(1);
        soft.assertAll();
    }
}
