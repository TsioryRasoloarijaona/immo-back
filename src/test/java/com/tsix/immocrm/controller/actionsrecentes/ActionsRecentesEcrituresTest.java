package com.tsix.immocrm.controller.actionsrecentes;

import com.tsix.immocrm.dto.prospection.CreerPreProspectionRequest;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionClosingReason;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.entities.str.StrHistory;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComProspectionClosingReasonRepository;
import com.tsix.immocrm.repositories.com.ComProspectionKindRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.str.StrHistoryRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.prospection.ProspectionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Partie A de FEATURE-actions-recentes.md : auteur (R1) et nouveaux événements (R2). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ActionsRecentesEcrituresTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ProspectionService prospectionService;
    @Autowired private AdmUserRepository userRepository;
    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private ComProspectionKindRepository kindRepository;
    @Autowired private ComProspectionClosingReasonRepository closingReasonRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private CfgProjectRepository projectRepository;
    @Autowired private CfgTrancheRepository trancheRepository;
    @Autowired private StrHistoryRepository historyRepository;

    private AdmUser moi;

    @BeforeEach
    void setUp() {
        moi = new AdmUser();
        moi.setId(UUID.randomUUID());
        moi.setCode("ar.ecriture." + UUID.randomUUID());
        moi.setDescription("Agent Récent");
        moi.setDisabled(false);
        userRepository.save(moi);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private RequestPostProcessor connecte(AdmUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private List<StrHistory> historique(UUID objectId) {
        return historyRepository.findByObjectIdOrderByDocDateDesc(objectId, Pageable.unpaged()).getContent();
    }

    private List<String> descriptions(UUID objectId) {
        return historique(objectId).stream().map(StrHistory::getDescription).toList();
    }

    private ComProspection newProspection(String code) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        return prospectionRepository.save(p);
    }

    private ComProspectionKind newEtape(String code) {
        ComProspectionKind kind = new ComProspectionKind();
        kind.setId(UUID.randomUUID());
        kind.setCode(code);
        kind.setDescription(code);
        return kindRepository.save(kind);
    }

    // ------------------------------------------------------------------ R1

    @Test
    void r1_ecrituresExistantes_portentLAuteur() throws Exception {
        mockMvc.perform(post("/api/pre-prospections").with(connecte(moi))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nom\":\"Auteur Test\"}"))
                .andExpect(status().isCreated());
        StrHistory creation = historyRepository.findAll().stream()
                .filter(h -> "Pré-prospection créée : Auteur Test".equals(h.getDescription()))
                .findFirst().orElseThrow();
        assertThat(creation.getAdmUserId()).isEqualTo(moi.getId());
        assertThat(creation.getAdmUserDescription()).isEqualTo("Agent Récent");
        assertThat(creation.getUserDate()).isNotNull();

        ComProspection pre = newProspection("PSN-AR-R1");
        mockMvc.perform(post("/api/pre-prospections/{id}/creer-prospect", pre.getId()).with(connecte(moi))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"prenom\":\"Jean\",\"nom\":\"Rakoto\"}"))
                .andExpect(status().isCreated());

        CfgProject projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        projet.setCode("PRJ-AR-R1");
        projectRepository.save(projet);
        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-AR-R1");
        tranche.setCfgProject(projet);
        trancheRepository.save(tranche);
        CfgTier commercial = new CfgTier();
        commercial.setId(UUID.randomUUID());
        commercial.setCode("COMM-AR-R1");
        commercial.setLastName("Commercial AR");
        tierRepository.save(commercial);

        mockMvc.perform(post("/api/prospections/{id}/transformer", pre.getId()).with(connecte(moi))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"commercialId\":\"" + commercial.getId() + "\",\"trancheId\":\"" + tranche.getId() + "\"}"))
                .andExpect(status().isOk());

        List<StrHistory> lignes = historique(pre.getId());
        assertThat(lignes).hasSize(2).allSatisfy(h -> {
            assertThat(h.getAdmUserId()).isEqualTo(moi.getId());
            assertThat(h.getAdmUserDescription()).isEqualTo("Agent Récent");
        });
    }

    @Test
    void r1_sansUtilisateurAuthentifie_auteurNullSansException() {
        SecurityContextHolder.clearContext();
        CreerPreProspectionRequest request = new CreerPreProspectionRequest(
                "Formulaire Public", null, null, null, null, null, null, null, null, null, null, null);

        ComProspection p = prospectionService.ecrirePreProspection(request, LocalDateTime.now());

        List<StrHistory> lignes = historique(p.getId());
        assertThat(lignes).hasSize(1);
        assertThat(lignes.get(0).getDescription()).isEqualTo("Pré-prospection créée : Formulaire Public");
        assertThat(lignes.get(0).getAdmUserId()).isNull();
        assertThat(lignes.get(0).getAdmUserDescription()).isNull();
    }

    // ------------------------------------------------------------------ R2

    @Test
    void r2_changerEtape_ecritAncienEtNouveau_etRienSiInchangee() throws Exception {
        ComProspection p = newProspection("PSN-AR-ETAPE");
        ComProspectionKind a = newEtape("AR-ETAPE-A");
        ComProspectionKind b = newEtape("AR-ETAPE-B");

        for (ComProspectionKind etape : List.of(a, b, b)) {
            mockMvc.perform(patch("/api/prospections/{id}/etape", p.getId()).with(connecte(moi))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"etapeId\":\"" + etape.getId() + "\"}"))
                    .andExpect(status().isOk());
        }

        assertThat(descriptions(p.getId())).containsExactlyInAnyOrder(
                "Étape changée : — → AR-ETAPE-A",
                "Étape changée : AR-ETAPE-A → AR-ETAPE-B");
        assertThat(historique(p.getId())).allSatisfy(h -> assertThat(h.getAdmUserId()).isEqualTo(moi.getId()));
    }

    @Test
    void r2_archiverAvecRaison_puisDesarchiver() throws Exception {
        ComProspection p = newProspection("PSN-AR-ARCH");
        ComProspectionClosingReason raison = new ComProspectionClosingReason();
        raison.setId(UUID.randomUUID());
        raison.setCode("AR-RAISON");
        raison.setDescription("Budget insuffisant");
        closingReasonRepository.save(raison);

        mockMvc.perform(patch("/api/prospections/{id}/archiver", p.getId()).with(connecte(moi))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"raisonId\":\"" + raison.getId() + "\",\"raisonTexte\":\"détail\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/prospections/{id}/desarchiver", p.getId()).with(connecte(moi)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fermee").value(false));

        assertThat(descriptions(p.getId())).containsExactlyInAnyOrder("Archivée : Budget insuffisant", "Désarchivée");
    }

    @Test
    void r2_archiverAvecTexteSeul_ecritLeTexte() throws Exception {
        ComProspection p = newProspection("PSN-AR-ARCH-TXT");

        mockMvc.perform(patch("/api/prospections/{id}/archiver", p.getId()).with(connecte(moi))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"raisonTexte\":\"Ne répond plus\"}"))
                .andExpect(status().isOk());

        assertThat(descriptions(p.getId())).containsExactly("Archivée : Ne répond plus");
    }

    @Test
    void r2_echecDeLArchivage_nEcritRien() throws Exception {
        ComProspection p = newProspection("PSN-AR-ARCH-400");

        mockMvc.perform(patch("/api/prospections/{id}/archiver", p.getId()).with(connecte(moi))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/prospections/{id}/desarchiver", p.getId()).with(connecte(moi)))
                .andExpect(status().isConflict());

        assertThat(historique(p.getId())).isEmpty();
    }
}
