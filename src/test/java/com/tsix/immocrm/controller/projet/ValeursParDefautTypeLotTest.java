package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProjetConfig;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureProjetConfig.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Types de lot, onglet « Valeurs par défaut » — FEATURE-projets-config-parite.md « Règles OMRAN confirmées » §6 et « Contrat » :
 * {@code GET / PUT /api/projet/config/types-lot/defaut} {@code {typeLotId}} lit / écrit {@code AdmParamsGuid.DefaultMarketType.CodeValue} ;
 * type inconnu → 400, vide → NULL ; écriture Administrateur. La base est relue en SQL natif ({@code AdmParamsGuid} est
 * {@code @Immutable} et resterait en cache dans la transaction du test) ; jamais un GET après un PUT dans le même test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ValeursParDefautTypeLotTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureProjetConfig fixture;

    private AdmUser admin;
    private AdmUser commercial;
    private UUID typeLot;
    private UUID autreType;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        commercial = patrimoine.administrateur();
        String jeton = FixtureReferentielsAdmin.jeton();
        typeLot = fixture.typeLot("TL-" + jeton);
        autreType = fixture.typeLot("TL2-" + jeton);
    }

    private static RequestPostProcessor enCommercial(AdmUser compte) {
        return FixtureDossierVente.connecte(compte, "Commercial");
    }

    private ResultActions enregistrer(CorpsJson corps, RequestPostProcessor qui) throws Exception {
        return mockMvc.perform(put(TYPE_LOT_DEFAUT).with(qui).contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    @Test
    void s6_lecture_typeParDefautEnBase() throws Exception {
        fixture.typeLotParDefaut(typeLot);

        mockMvc.perform(get(TYPE_LOT_DEFAUT).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + TYPE_LOT_ID).value(typeLot.toString()));
    }

    @Test
    void s6_lecture_aucunTypeParDefaut_null() throws Exception {
        fixture.typeLotParDefaut(null);

        String json = mockMvc.perform(get(TYPE_LOT_DEFAUT).with(connecte(admin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Champ null ou absent : les deux disent « aucun type par défaut »
        Object valeur = JsonPath.using(Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL))
                .parse(json).read("$." + TYPE_LOT_ID);
        assertThat(valeur).isNull();
    }

    @Test
    void s6_enregistrement_2xx_codeValueEcrit() throws Exception {
        fixture.typeLotParDefaut(autreType);

        enregistrer(corps().avec(TYPE_LOT_ID, typeLot), connecte(admin)).andExpect(status().is2xxSuccessful());

        assertThat(fixture.typeLotParDefautEnBase()).isEqualTo(typeLot.toString());
    }

    @Test
    void s6_enregistrementVide_2xx_codeValueNull() throws Exception {
        fixture.typeLotParDefaut(typeLot);

        enregistrer(corps().avec(TYPE_LOT_ID, null), connecte(admin)).andExpect(status().is2xxSuccessful());

        assertThat(fixture.typeLotParDefautEnBase()).isNull();
    }

    @Test
    void s6_typeInconnu_400_valeurInchangee() throws Exception {
        fixture.typeLotParDefaut(typeLot);

        enregistrer(corps().avec(TYPE_LOT_ID, UUID.randomUUID()), connecte(admin)).andExpect(status().isBadRequest());

        assertThat(fixture.typeLotParDefautEnBase()).isEqualTo(typeLot.toString());
    }

    @Test
    void s6_enregistrement_nonAdministrateur_403_valeurInchangee() throws Exception {
        fixture.typeLotParDefaut(typeLot);

        enregistrer(corps().avec(TYPE_LOT_ID, autreType), enCommercial(commercial)).andExpect(status().isForbidden());

        assertThat(fixture.typeLotParDefautEnBase()).isEqualTo(typeLot.toString());
    }
}
