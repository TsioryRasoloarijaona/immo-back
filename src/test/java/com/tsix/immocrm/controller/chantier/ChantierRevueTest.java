package com.tsix.immocrm.controller.chantier;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureChantier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.tsix.immocrm.fixtures.FixtureChantier.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue : chemins non couverts par les tests de l'auteur (clés de tri imbriquées, filtres sur B hors périmètre). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChantierRevueTest {

    private static final String DECOMPTES = "/api/chantier/decomptes";
    private static final String TOTAL = "$.totalElements";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureChantier fixture;

    private FixtureChantier.Chaine chaine;

    @BeforeEach
    void setUp() {
        chaine = fixture.creer();
    }

    private String lire(MockHttpServletRequestBuilder requete, AdmUser user) throws Exception {
        return mockMvc.perform(requete.with(connecte(user)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @ParameterizedTest
    @ValueSource(strings = {"code", "description", "dateDebut", "dateFin", "statutOrdre", "typeCode", "fournisseurNom"})
    void lots_chaqueCleDeTriAdmise_garde3Lots(String cle) throws Exception {
        String json = lire(get("/api/chantier/projets/{id}/lots", chaine.projetA().getId())
                .param("sort", cle + ",asc"), fixture.administrateur());

        assertThat((Integer) JsonPath.read(json, TOTAL)).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(strings = {"dateDocument", "code", "numero", "montantHT", "montantTTC", "netAPayer", "lotCode",
            "projetCode", "fournisseurNom"})
    void decomptes_chaqueCleDeTriAdmise_garde3Decomptes(String cle) throws Exception {
        String json = lire(get(DECOMPTES).param("lotId", chaine.lotBordereau().getId().toString())
                .param("sort", cle + ",desc"), fixture.administrateur());
        List<String> ids = JsonPath.read(json, "$.content[*].id");

        assertThat(ids).hasSize(3).doesNotHaveDuplicates();
    }

    @Test
    void habiliteSurA_filtreLotDeB_etRechercheSurB_rienDeB() throws Exception {
        AdmUser conducteur = fixture.utilisateur("chantier.revue");
        fixture.habiliter(conducteur, chaine.projetA());

        String parLotB = lire(get(DECOMPTES).param("lotId", chaine.lotB().getId().toString()), conducteur);
        List<String> parRecherche = JsonPath.read(lire(get(DECOMPTES).param("search", chaine.lotB().getCode()),
                conducteur), "$.content[*].id");
        String sansFacture = lire(get(DECOMPTES).param("sansFacture", "true")
                .param("fournisseurId", chaine.fournisseurB().getId().toString()), conducteur);

        assertAll(
                () -> assertThat((Integer) JsonPath.read(parLotB, TOTAL)).isZero(),
                () -> assertThat(parRecherche).isEmpty(),
                () -> assertThat((Integer) JsonPath.read(sansFacture, TOTAL)).isZero());
    }
}
