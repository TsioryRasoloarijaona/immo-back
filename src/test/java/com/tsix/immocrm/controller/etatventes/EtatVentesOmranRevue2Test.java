package com.tsix.immocrm.controller.etatventes;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Second tour (règle 6) de FEATURE-etat-ventes-omran.md : alerte / sousArticles sans chemin, synthèse et entiteId. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class EtatVentesOmranRevue2Test {

    private static final String URL = "/api/etat-des-ventes";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureEtatVentes fixture;
    @Autowired private FixtureDossierVente fixtureDossier;

    private String prefixe;
    private CfgProject projet;
    private CfgTranche tranche;
    private StkItemCategory appartement;
    private StkItemType type;

    @BeforeEach
    void setUp() {
        prefixe = FixtureEtatVentes.prefixe("EDV2");
        projet = fixture.projet("PRJ-" + prefixe, true);
        tranche = fixture.tranche("TR-" + prefixe, projet, true);
        appartement = fixture.categorie("APP-" + prefixe);
        type = fixture.type("T-" + prefixe);
    }

    /** §5 : alerte et sousArticles n'ont pas de chemin, donc tri refusé (400). */
    @ParameterizedTest
    @ValueSource(strings = {"alerte", "sousArticles"})
    void triSurColonneSansChemin_400(String colonne) throws Exception {
        mockMvc.perform(get(URL).param("sort", colonne + ",asc")).andExpect(status().isBadRequest());
    }

    /** §5 : ni groupables (400). */
    @ParameterizedTest
    @ValueSource(strings = {"alerte", "sousArticles"})
    void groupeSurColonneSansChemin_400(String colonne) throws Exception {
        mockMvc.perform(get(URL + "/groupes").param("groupBy", colonne)).andExpect(status().isBadRequest());
    }

    /** §5 : ni sommables (400). */
    @ParameterizedTest
    @ValueSource(strings = {"alerte", "sousArticles"})
    void agregatSurColonneSansChemin_400(String colonne) throws Exception {
        mockMvc.perform(get(URL + "/totaux-colonnes").param("agregats", colonne)).andExpect(status().isBadRequest());
    }

    /** Règle 6 : entiteId d'une société hors périmètre ne rend aucune ligne de synthèse au commercial restreint. */
    @Test
    void synthese_entiteIdHorsPerimetre_vide() throws Exception {
        StkItem a = fixture.article(prefixe + "-A", tranche, appartement, type);
        fixture.vendre(a, "DOS-" + a.getCode(), fixture.statut(StatutDossier.RESERVATION), null,
                LocalDateTime.of(2025, 3, 15, 10, 0));
        CfgProject projetB = fixture.projet("PRJB-" + prefixe, true);
        CfgTranche trancheB = fixture.tranche("TRB-" + prefixe, projetB, true);
        fixture.article(prefixe + "-B", trancheB, appartement, type);
        AdmUser commercial = fixtureDossier.compte("edv2.com." + prefixe);
        fixture.affecter(commercial, projet);
        fixture.relire();

        String corps = mockMvc.perform(get(URL + "/synthese")
                        .param("trancheId", tranche.getId().toString(), trancheB.getId().toString())
                        .param("entiteId", projetB.getCfgCompany().getId().toString())
                        .with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Object> lignes = JsonPath.read(corps, "$.lignes");
        Number total = JsonPath.read(corps, "$.totaux.general.total");

        assertThat(lignes).isEmpty();
        assertThat(total.longValue()).isZero();
    }
}
