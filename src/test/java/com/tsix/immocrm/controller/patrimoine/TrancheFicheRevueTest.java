package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureTrancheFiche;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ChampsAViderJson.avecChampsAVider;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue du lot P1a (FEATURE-patrimoine-tranche-fiche.md §1.1, §3.3, §3.4, §4.4) : cas non couverts par les tests verrouillés. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TrancheFicheRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureTrancheFiche fiche;

    private AdmUser admin;
    private CfgProject projet;
    private String jeton;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        jeton = "P1AR" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        CfgCompany entite = fixture.entite("ENT-" + jeton, true);
        projet = fixture.projet("PRJ-" + jeton, entite);
    }

    @Test
    void suppressionProjet_zonesImbriquees_204_toutesSupprimees() throws Exception {
        UUID parent = fiche.zone(projet.getId(), null, "ZP-" + jeton, 1);
        fiche.zone(projet.getId(), parent, "ZE-" + jeton, 1);

        mockMvc.perform(delete("/api/projets/{id}", projet.getId()).with(connecte(admin)))
                .andExpect(status().isNoContent());

        assertThat(fiche.compter("SELECT COUNT(*) FROM CfgProjectZone WHERE CfgProjectId = :id", projet.getId()))
                .isZero();
    }

    @Test
    void zone_modification_codeDUnFrere_409() throws Exception {
        fiche.zone(projet.getId(), null, "ZA-" + jeton, 1);
        UUID b = fiche.zone(projet.getId(), null, "ZB-" + jeton, 2);

        mockMvc.perform(patch("/api/projets/{id}/zones/{zoneId}", projet.getId(), b).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(corps().avec("code", "ZA-" + jeton).json()))
                .andExpect(status().isConflict());
    }

    @Test
    void listeTranches_colonnesDuProgramme_serviesSiDemandees() throws Exception {
        CfgTier signataire = fiche.employe("SIG-" + jeton);
        mockMvc.perform(patch("/api/projets/{id}", projet.getId()).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec("signataireId", signataire.getId()).avec("villeSignature", "Fes").json()))
                .andExpect(status().isOk());
        fixture.tranche("TR-" + jeton, projet);

        mockMvc.perform(get("/api/tranches").with(connecte(admin))
                        .param("projetId", projet.getId().toString())
                        .param("colonnes", "signataireLibelle,villeSignature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].signataireLibelle").value("Tiers SIG-" + jeton))
                .andExpect(jsonPath("$.content[0].villeSignature").value("Fes"))
                .andExpect(jsonPath("$.content[0].code").doesNotExist());
    }

    /** Tranche.cs:720 : projets de facturation de l'entité du programme seulement. */
    @Test
    void projetFacturation_dUneAutreEntite_400() throws Exception {
        CfgTranche tranche = fixture.tranche("TRF-" + jeton, projet);
        UUID ailleurs = fiche.projetFacturation("PRJF-" + jeton, fixture.entite("ENT-F-" + jeton, true).getId());

        mockMvc.perform(patch("/api/tranches/{id}", tranche.getId()).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(corps().avec("projetFacturationId", ailleurs).json()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("projetFacturationId")));
    }

    @Test
    void champsAVider_texteDeLaTranche_ecritVideJamaisNull() throws Exception {
        CfgTranche tranche = fixture.tranche("TRV-" + jeton, projet);
        mockMvc.perform(patch("/api/tranches/{id}", tranche.getId()).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(corps().avec("adresseSav", "Rue SAV").json()))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/tranches/{id}", tranche.getId()).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(avecChampsAVider(corps(), "adresseSav", "estEnVente")))
                .andExpect(status().isOk());

        assertThat(fiche.compter("SELECT COUNT(*) FROM CfgTranche WHERE PKey = :id AND SavAddress = N'' "
                + "AND IsForSale = 0", tranche.getId())).isEqualTo(1);
    }
}
