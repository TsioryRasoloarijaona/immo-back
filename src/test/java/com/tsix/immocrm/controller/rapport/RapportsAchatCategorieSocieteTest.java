package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureRapportsAchat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.controller.rapport.RapportsAchatSupport.ACHAT;
import static com.tsix.immocrm.controller.rapport.RapportsAchatSupport.ANNEE;
import static com.tsix.immocrm.controller.rapport.RapportsAchatSupport.lignesAvec;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureRapportsAchat.DEPENSES_PAR_MOIS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** R12 : une prévision par catégorie ({@code StlCategoryPrevision}) d'une société non habilitée n'entre pas dans {@code prevu}. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsAchatCategorieSocieteTest {

    private static final String PREVU = "prevu";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat achat;
    @Autowired private FixtureRapportsAchat fixture;

    private AppelsRapports appels;
    private String jeton;
    private UUID habilitee;
    private UUID autre;
    private AdmUser compte;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        jeton = FixtureAchat.jeton("N7C");
        fixture.rapports();
        habilitee = achat.societe("SH-" + jeton);
        autre = achat.societe("SA-" + jeton);
        compte = fixture.compte("N7C-" + jeton, false, habilitee);
    }

    private List<Map<String, Object>> mois() throws Exception {
        MockHttpServletRequestBuilder requete = get(resultat(ACHAT, DEPENSES_PAR_MOIS))
                .with(connecte(compte, FixtureRapportsAchat.role()))
                .param(ANNEE, String.valueOf(FixtureRapportsAchat.ANNEE));
        List<Map<String, Object>> lignes = lignesAvec(appels.sections(requete), PREVU);
        assertThat(lignes).hasSize(12);
        return lignes;
    }

    @Test
    void r12_previsionCategorie_societeHabilitee_presente() throws Exception {
        fixture.previsionCategorie(FixtureRapportsAchat.ANNEE, 3, "17", achat.categorie("CH-" + jeton), habilitee);

        assertThat(somme(mois(), PREVU)).isEqualByComparingTo("17");
    }

    @Test
    void r12_previsionCategorie_societeNonHabilitee_absente() throws Exception {
        UUID categorie = achat.categorie("CA-" + jeton);
        fixture.previsionCategorie(FixtureRapportsAchat.ANNEE, 3, "17", categorie, habilitee);
        fixture.previsionCategorie(FixtureRapportsAchat.ANNEE, 3, "9000", categorie, autre);

        assertThat(somme(mois(), PREVU)).as("seule la prévision de la société habilitée").isEqualByComparingTo("17");
    }
}
