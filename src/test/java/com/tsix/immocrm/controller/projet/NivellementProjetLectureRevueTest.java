package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureReferentielsPlanification;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Revue N9a : GET du nivellement ouvert à tout compte qui voit le projet (Réponses du manager 7), périmètre sinon. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NivellementProjetLectureRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureReferentielsPlanification fixture;
    @Autowired private FixtureLotsParite parite;

    @Test
    void lecture_nonAdministrateurHabilite_200_horsPerimetre_404() throws Exception {
        fixture.tables();
        String jeton = FixtureProjetLots.jeton();
        UUID societe = parite.entiteJuridique("NRE-" + jeton);
        UUID visible = parite.projet("NRV-" + jeton, societe);
        UUID cache = parite.projet("NRC-" + jeton, societe);
        UUID noeud = fixture.noeud(visible, null, "BLOC R", 1);
        fixture.noeud(cache, null, "BLOC C", 1);
        var commercial = connecte(parite.avecDroits("n9a.rev." + jeton, List.of(), visible), "Commercial");

        MockHttpServletResponse lu = mockMvc.perform(get("/api/projet/config/projets/" + visible + "/nivellement")
                .with(commercial)).andReturn().getResponse();
        MockHttpServletResponse refuse = mockMvc.perform(get("/api/projet/config/projets/" + cache + "/nivellement")
                .with(commercial)).andReturn().getResponse();

        String corps = lu.getContentAsString(StandardCharsets.UTF_8);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(lu.getStatus()).as(corps).isEqualTo(200);
            s.assertThat(corps).contains(noeud.toString(), "BLOC R").doesNotContain("BLOC C");
            s.assertThat(refuse.getStatus()).isEqualTo(404);
        });
    }
}
