package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixturePatrimoine.ROLE_ADMIN;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.connecte;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FEATURE-patrimoine-ecriture.md §3 et §11. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjetEcritureTest {

    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";
    private static final String ENTITE_ID = "entiteId";
    private static final String CHAMP = "$.champ";
    private static final String AUTEUR = "Compte ADM-PRJ-ECR";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private CfgProjectRepository projectRepository;
    @Autowired private AdmUserHabilitationRepository habilitationRepository;
    @Autowired private EntityManager em;

    private AdmUser admin;
    private CfgCompany entite;

    @BeforeEach
    void setUp() {
        admin = fixture.utilisateur("ADM-PRJ-ECR");
        entite = fixture.entite("ENT-PRJ-ECR", true);
    }

    private Map<String, Object> corpsMinimal(String code) {
        Map<String, Object> corps = new HashMap<>();
        corps.put(ENTITE_ID, entite.getId());
        corps.put(CODE, code);
        corps.put(DESCRIPTION, "Programme " + code);
        return corps;
    }

    private ResultActions creer(Map<String, Object> corps) throws Exception {
        return mockMvc.perform(post("/api/projets").with(connecte(admin, ROLE_ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(json(corps)));
    }

    private ResultActions modifier(UUID id, Map<String, Object> corps) throws Exception {
        return mockMvc.perform(patch("/api/projets/{id}", id).with(connecte(admin, ROLE_ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(json(corps)));
    }

    @Test
    void creation_201_traceTextesVidesEtHabilitationDuCreateur() throws Exception {
        AdmCountry pays = fixture.pays("MA-PRJ");
        Map<String, Object> corps = corpsMinimal("  PRJ-ECR-1  ");
        corps.put("paysId", pays.getId());

        creer(corps)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("PRJ-ECR-1"))
                .andExpect(jsonPath("$.entiteCode").value("ENT-PRJ-ECR"))
                .andExpect(jsonPath("$.paysLibelle").value("Pays MA-PRJ"))
                .andExpect(jsonPath("$.creePar").value(AUTEUR))
                .andExpect(jsonPath("$.modifiePar").value(AUTEUR));

        em.flush();
        CfgProject cree = projectRepository.findFirstByCodeAndIdNot("PRJ-ECR-1", UUID.randomUUID()).orElseThrow();
        assertAll(
                () -> assertThat(cree.getAddress()).isEmpty(),
                () -> assertThat(cree.getDescriptionLong()).isEmpty(),
                () -> assertThat(cree.getSignatureCity()).isEmpty(),
                () -> assertThat(cree.getWebSite()).isEmpty(),
                () -> assertThat(cree.getUserCreateDateTime()).isNotNull().isEqualTo(cree.getUserUpdateDateTime()));

        List<AdmUserHabilitation> habilitations = habilitationRepository.findAll().stream()
                .filter(h -> h.getCfgProject() != null && h.getCfgProject().getId().equals(cree.getId()))
                .toList();
        assertThat(habilitations).singleElement()
                .satisfies(h -> assertAll(
                        () -> assertThat(h.getAdmUser().getId()).isEqualTo(admin.getId()),
                        () -> assertThat(h.getCfgTranche()).isNull(),
                        () -> assertThat(h.getCfgCompany()).isNull()));
    }

    @Test
    void creation_400_codeManquant() throws Exception {
        Map<String, Object> corps = corpsMinimal("X");
        corps.remove(CODE);
        creer(corps).andExpect(status().isBadRequest()).andExpect(jsonPath(CHAMP).value(CODE));
    }

    @Test
    void creation_400_descriptionVide() throws Exception {
        Map<String, Object> corps = corpsMinimal("PRJ-ECR-DESC");
        corps.put(DESCRIPTION, "  ");
        creer(corps).andExpect(status().isBadRequest()).andExpect(jsonPath(CHAMP).value(DESCRIPTION));
    }

    @Test
    void creation_400_entiteManquante() throws Exception {
        Map<String, Object> corps = corpsMinimal("PRJ-ECR-ENT");
        corps.remove(ENTITE_ID);
        creer(corps).andExpect(status().isBadRequest()).andExpect(jsonPath(CHAMP).value(ENTITE_ID));
    }

    @Test
    void creation_400_entiteInactive() throws Exception {
        Map<String, Object> corps = corpsMinimal("PRJ-ECR-INA");
        corps.put(ENTITE_ID, fixture.entite("ENT-INACTIVE", false).getId());
        creer(corps).andExpect(status().isBadRequest()).andExpect(jsonPath(CHAMP).value(ENTITE_ID));
    }

    @Test
    void creation_400_finTravauxAvantDebut() throws Exception {
        Map<String, Object> corps = corpsMinimal("PRJ-ECR-DAT");
        corps.put("dateDebutTravaux", "2026-06-01T00:00:00");
        corps.put("dateFinTravaux", "2026-05-31T00:00:00");
        creer(corps).andExpect(status().isBadRequest()).andExpect(jsonPath(CHAMP).value("dateFinTravaux"));
    }

    @Test
    void creation_409_codeExistantCasseDifferente() throws Exception {
        fixture.projet("PRJ-ECR-DUP", entite);
        creer(corpsMinimal("prj-ecr-dup"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(CHAMP).value(CODE))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Programme PRJ-ECR-DUP")));
    }

    @Test
    void modification_404_idInconnu() throws Exception {
        modifier(UUID.randomUUID(), Map.of(DESCRIPTION, "x")).andExpect(status().isNotFound());
    }

    @Test
    void modification_champAbsentInchange_chaineVideVide_etTracePosee() throws Exception {
        CfgProject projet = fixture.projet("PRJ-ECR-PATCH", entite);
        projet.setCity("Casablanca");
        projet.setAddress("1 rue X");
        projet.setUserCreate("Createur initial");
        projectRepository.save(projet);

        modifier(projet.getId(), Map.of("adresse", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adresse").value(""))
                .andExpect(jsonPath("$.ville").value("Casablanca"))
                .andExpect(jsonPath("$.code").value("PRJ-ECR-PATCH"))
                .andExpect(jsonPath("$.creePar").value("Createur initial"))
                .andExpect(jsonPath("$.modifiePar").value(AUTEUR))
                .andExpect(jsonPath("$.modifieLe").isNotEmpty());
    }

    @Test
    void modification_400_codeVide() throws Exception {
        CfgProject projet = fixture.projet("PRJ-ECR-VIDE", entite);
        modifier(projet.getId(), Map.of(CODE, "")).andExpect(status().isBadRequest());
    }

    @Test
    void modification_dateFinControleeContreLaDateDebutStockee() throws Exception {
        CfgProject projet = fixture.projet("PRJ-ECR-DATE2", entite);
        modifier(projet.getId(), Map.of("dateDebutCommercialisation", "2026-06-01T00:00:00")).andExpect(status().isOk());
        modifier(projet.getId(), Map.of("dateFinCommercialisation", "2026-01-01T00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CHAMP).value("dateFinCommercialisation"));
    }

    @Test
    void modification_409_codeDUnAutreProgramme() throws Exception {
        fixture.projet("PRJ-ECR-A", entite);
        CfgProject b = fixture.projet("PRJ-ECR-B", entite);
        modifier(b.getId(), Map.of(CODE, "PRJ-ECR-A")).andExpect(status().isConflict());
    }

    @Test
    void modification_garderSonPropreCode_200() throws Exception {
        CfgProject projet = fixture.projet("PRJ-ECR-SELF", entite);
        modifier(projet.getId(), Map.of(CODE, "PRJ-ECR-SELF")).andExpect(status().isOk());
    }
}
