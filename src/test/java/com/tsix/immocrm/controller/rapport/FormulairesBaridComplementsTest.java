package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureFormulairesBarid;
import com.tsix.immocrm.fixtures.FixtureFormulairesBarid.Adresse;
import com.tsix.immocrm.fixtures.FixtureFormulairesBarid.Jeu;
import com.tsix.immocrm.fixtures.FixtureRapports;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.FormulairesBaridPdfTest.FORMULAIRES;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureFormulairesBarid.CODE_RAPPORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Compléments du n° 35 (réponses du manager du 27/10, point 6) : avertissements émis seulement si le compte est positif,
 * fond signalé seulement pour un modèle actif, modèle jamais semé = même 404 qu'inactif, filtre statut sans dossiers sans statut.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FormulairesBaridComplementsTest {

    private static final String TRANCHE_ID = "trancheId";
    private static final String AVERTISSEMENTS = "$.avertissements";
    private static final String MODELE_ABSENT = "Le modèle FORMULAIRE_BARID est absent ou inactif";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private FixtureFormulairesBarid fixture;

    private AppelsRapports appels;
    private RequestPostProcessor libre;
    private String p;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        p = FixtureEtatVentes.prefixe("BAMC");
        fixture.rapport();
        libre = connecte(rapports.compte("bam.compl." + p, rapports.role("Libre " + p, false)));
    }

    /** Tranche d'un seul dossier dont le client a titre, adresse et code postal. */
    private CfgTranche trancheComplete() {
        AdmCountry pays = fixture.pays("PAYS-" + p);
        CfgTranche t = fixture.tranche("TC-" + p, true, pays);
        fixture.dossier(t, "DC-" + p, ev.statutEnCours("SC-" + p), fixture.client("COMPLET " + p,
                fixture.titre("Mr" + p), new Adresse("1 RUE COMPLETE", null, null, "20000", fixture.ville("V" + p, pays),
                        pays)));
        fixture.relire();
        return t;
    }

    private List<String> avertissements(CfgTranche t) throws Exception {
        return JsonPath.read(appels.corps(get(resultat(VENTE, CODE_RAPPORT)).param(TRANCHE_ID, t.getId().toString())
                .with(libre)), AVERTISSEMENTS);
    }

    @Test
    void avertissements_aucunManque_aucunAvertissement() throws Exception {
        assertThat(avertissements(trancheComplete())).isEmpty();
    }

    @Test
    void avertissementFond_modeleInactif_absent() throws Exception {
        CfgTranche t = trancheComplete();
        UUID modele = fixture.semerModeles();
        mockMvc.perform(put("/api/modeles-documents/{id}", modele).with(user("admin").roles("Administrateur"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("actif", false))))
                .andExpect(status().isOk());

        assertThat(avertissements(t)).isEmpty();
    }

    @Test
    void pdf_modeleJamaisSeme_404CommeInactif() throws Exception {
        CfgTranche t = trancheComplete();

        mockMvc.perform(get(FORMULAIRES).param(TRANCHE_ID, t.getId().toString()).with(libre))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(MODELE_ABSENT));
    }

    @Test
    void filtreStatut_dossierSansStatutExclu() throws Exception {
        Jeu jeu = fixture.jeu(p);

        List<Map<String, Object>> lignes = lignes(section(appels.sections(get(resultat(VENTE, CODE_RAPPORT))
                .param(TRANCHE_ID, jeu.t1().getId().toString())
                .param("statutId", jeu.s1().getId().toString()).with(libre)), "destinataires"));

        assertThat(lignes).isNotEmpty().extracting(l -> Objects.toString(l.get("dossier"), ""))
                .doesNotContain(jeu.d5().getCode());
    }
}
