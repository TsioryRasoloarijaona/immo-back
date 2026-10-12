package com.tsix.immocrm.controller.entite;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmSequence;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgShareholder;
import com.tsix.immocrm.fixtures.FixtureEntites;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.adm.AdmSequenceRepository;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyShareholderRepository;
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
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FEATURE-entites-juridiques.md §2 et §6 : écran « Entités juridiques ». */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EntiteJuridiqueControllerTest {

    private static final String LISTE = "/api/entites-juridiques";
    private static final String FICHE = "/api/entites-juridiques/{id}";
    private static final String CHAMP = "$.champ";
    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";
    private static final String DEVISE = "deviseId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureEntites entites;
    @Autowired private CfgCompanyRepository companyRepository;
    @Autowired private AdmSequenceRepository sequenceRepository;
    @Autowired private AdmUserHabilitationRepository habilitationRepository;
    @Autowired private CfgCompanyShareholderRepository repartitionRepository;
    @Autowired private EntityManager em;

    private AdmUser admin;
    private AdmUser commercial;
    private UUID devise;

    @BeforeEach
    void setUp() {
        admin = fixture.utilisateur("ADM-P5-ENT");
        entites.donnerRole(admin, ROLE_ADMIN);
        commercial = fixture.utilisateur("COM-P5-ENT");
        devise = entites.deviseMad();
    }

    private Map<String, Object> corps(String code) {
        Map<String, Object> corps = new HashMap<>();
        corps.put(CODE, code);
        corps.put(DESCRIPTION, "Société " + code);
        corps.put(DEVISE, devise);
        return corps;
    }

    private ResultActions creer(Map<String, Object> corps) throws Exception {
        return mockMvc.perform(post(LISTE).with(connecte(admin, ROLE_ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(json(corps)));
    }

    private ResultActions modifier(UUID id, Map<String, Object> corps) throws Exception {
        return mockMvc.perform(patch(FICHE, id).with(connecte(admin, ROLE_ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(json(corps)));
    }

    @Test
    void liste_nonFiltreeParActif_saufFiltreExplicite() throws Exception {
        fixture.entite("TST-P5-ACT", true);
        fixture.entite("TST-P5-INA", false);

        mockMvc.perform(get(LISTE).param("search", "TST-P5-").with(connecte(admin, ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'TST-P5-ACT')].actif").value(true))
                .andExpect(jsonPath("$[?(@.code == 'TST-P5-INA')].actif").value(false));
        mockMvc.perform(get(LISTE).param("actif", "false").param("search", "TST-P5-")
                        .with(connecte(admin, ROLE_ADMIN)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("TST-P5-INA"));
    }

    @Test
    void liste_400_actifInvalide() throws Exception {
        mockMvc.perform(get(LISTE).param("actif", "peut-etre").with(connecte(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void liste_nonExempte_seulementLesEntitesHabilitees() throws Exception {
        CfgCompany habilitee = fixture.entite("TST-P5-HAB", false);
        fixture.entite("TST-P5-NONHAB", true);
        entites.habilitation(commercial, habilitee);

        mockMvc.perform(get(LISTE).param("search", "TST-P5-").with(connecte(commercial)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("TST-P5-HAB"));
    }

    @Test
    void fiche_404_entiteHorsHabilitation() throws Exception {
        CfgCompany autre = fixture.entite("TST-P5-AUTRE", true);

        mockMvc.perform(get(FICHE, autre.getId()).with(connecte(commercial)))
                .andExpect(status().isNotFound());
    }

    @Test
    void liste_rechercheSansEspaces_surMatriculeFiscal() throws Exception {
        CfgCompany entite = fixture.entite("TST-P5-MF", true);
        entite.setFiscalNumber("40 123 456");
        companyRepository.saveAndFlush(entite);

        mockMvc.perform(get(LISTE).param("search", "40123 456").with(connecte(admin, ROLE_ADMIN)))
                .andExpect(jsonPath("$[?(@.code == 'TST-P5-MF')]").exists());
    }

    @Test
    void creer_201_quatreSequencesHabilitationEtDefauts() throws Exception {
        String reponse = creer(corps("TST-P5-NEW"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.actif").value(true))
                .andExpect(jsonPath("$.deviseCode").value("MAD"))
                .andExpect(jsonPath("$.matriculeFiscal").value(""))
                .andExpect(jsonPath("$.repartition.length()").value(0))
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(reponse, "$.id"));

        List<AdmSequence> sequences = sequenceRepository.findByCfgCompanyId(id);
        assertThat(sequences).hasSize(4)
                .allSatisfy(s -> assertThat(s)
                        .extracting(AdmSequence::getSeqYearNumber, AdmSequence::getSeqValueNumber,
                                AdmSequence::getSeqValue, AdmSequence::getBasedOnSystemYear,
                                AdmSequence::getSeqPrefix, AdmSequence::getSeparator)
                        .containsExactly(2, 3, 0, true, "", ""))
                .extracting(AdmSequence::getTypeCode, AdmSequence::getCode)
                .containsExactlyInAnyOrder(tuple("INVOICE", "Facture"), tuple("REFUND", "Avoir"),
                        tuple("RSSheetCode", "Certificat de retenue à la source"), tuple("Receipt", "Reçu"));
        assertThat(habilitationRepository.findByCfgCompanyId(id)).singleElement()
                .extracting(h -> h.getAdmUser().getId()).isEqualTo(admin.getId());
    }

    @Test
    void creer_avecRepartitionCoherente() throws Exception {
        CfgShareholder a = entites.actionnaire("TST-P5-RA");
        Map<String, Object> corps = corps("TST-P5-REPOK");
        corps.put("nombreActionnaires", 1);
        corps.put("nombreParts", 100);
        corps.put("repartition", List.of(Map.of("actionnaireId", a.getId(), "nombreParts", 100)));

        creer(corps).andExpect(status().isCreated())
                .andExpect(jsonPath("$.repartition[0].actionnaireCode").value("TST-P5-RA"))
                .andExpect(jsonPath("$.repartition[0].nombreParts").value(100));
    }

    @Test
    void creer_400_deviseManquante() throws Exception {
        Map<String, Object> corps = corps("TST-P5-SANSDEV");
        corps.remove(DEVISE);

        creer(corps).andExpect(status().isBadRequest()).andExpect(jsonPath(CHAMP).value(DEVISE));
    }

    @Test
    void creer_400_deviseInconnue() throws Exception {
        Map<String, Object> corps = corps("TST-P5-DEVKO");
        corps.put(DEVISE, UUID.randomUUID());

        creer(corps).andExpect(status().isBadRequest()).andExpect(jsonPath(CHAMP).value(DEVISE));
    }

    @Test
    void creer_409_codeDejaPris() throws Exception {
        fixture.entite("TST-P5-PRIS", false);

        creer(corps("tst-p5-pris")).andExpect(status().isConflict()).andExpect(jsonPath(CHAMP).value(CODE));
    }

    @Test
    void creer_403_nonAdministrateur() throws Exception {
        mockMvc.perform(post(LISTE).with(connecte(commercial)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(corps("TST-P5-403"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void modifier_desactiver_puisChampsAVider() throws Exception {
        CfgCompany entite = fixture.entite("TST-P5-MOD", true);
        entite.setFiscalNumber("MF-1");
        companyRepository.saveAndFlush(entite);

        modifier(entite.getId(), Map.of("actif", false, "champsAVider", List.of("matriculeFiscal")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actif").value(false))
                .andExpect(jsonPath("$.matriculeFiscal").value(""));
    }

    @Test
    void modifier_400_champsAViderObligatoire() throws Exception {
        CfgCompany entite = fixture.entite("TST-P5-VIDKO", true);

        modifier(entite.getId(), Map.of("champsAVider", List.of(DEVISE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CHAMP).value("champsAVider"));
    }

    @Test
    void modifier_404_idInconnu() throws Exception {
        modifier(UUID.randomUUID(), Map.of(DESCRIPTION, "X")).andExpect(status().isNotFound());
    }

    @Test
    void modifier_403_nonAdministrateur() throws Exception {
        CfgCompany entite = fixture.entite("TST-P5-MOD403", true);
        em.flush();

        mockMvc.perform(patch(FICHE, entite.getId()).with(connecte(commercial))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("actif", false))))
                .andExpect(status().isForbidden());

        em.clear();
        assertThat(companyRepository.findById(entite.getId())).get()
                .extracting(CfgCompany::getIsActive).isEqualTo(true);
    }

    @Test
    void supprimer_204_avecSequencesHabilitationsEtRepartition() throws Exception {
        String reponse = creer(corps("TST-P5-SUP")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(reponse, "$.id"));

        mockMvc.perform(delete(FICHE, id).with(connecte(admin, ROLE_ADMIN)))
                .andExpect(status().isNoContent());

        assertThat(companyRepository.findById(id)).isEmpty();
        assertThat(sequenceRepository.findByCfgCompanyId(id)).isEmpty();
        assertThat(habilitationRepository.findByCfgCompanyId(id)).isEmpty();
        assertThat(repartitionRepository.deLEntite(id)).isEmpty();
    }

    @Test
    void supprimer_409_entiteQuiPorteUnProgramme() throws Exception {
        CfgCompany entite = fixture.entite("TST-P5-SUPKO", true);
        fixture.projet("TST-P5-PRG", entite);

        mockMvc.perform(delete(FICHE, entite.getId()).with(connecte(admin, ROLE_ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("1 programme(s)")));

        assertThat(companyRepository.findById(entite.getId())).isPresent();
    }

    @Test
    void supprimer_403_nonAdministrateur() throws Exception {
        CfgCompany entite = fixture.entite("TST-P5-SUP403", true);

        mockMvc.perform(delete(FICHE, entite.getId()).with(connecte(commercial)))
                .andExpect(status().isForbidden());
    }

    @Test
    void devises_madMarqueeParDefaut() throws Exception {
        mockMvc.perform(get(LISTE + "/devises").with(connecte(commercial)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'MAD')].parDefaut").value(true));
    }

    @Test
    void fiche_200_nonExempteHabilite_entiteInactiveComprise() throws Exception {
        CfgCompany entite = fixture.entite("TST-P5-VISHAB", false);
        entites.habilitation(commercial, entite);

        mockMvc.perform(get(FICHE, entite.getId()).with(connecte(commercial)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TST-P5-VISHAB"))
                .andExpect(jsonPath("$.actif").value(false));
    }
}
