package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.config.audit.AuditDeTest;
import com.tsix.immocrm.entities.adm.AdmAttachedFile;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.adm.AdmWebHistorics;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.CfgTrancheCoveredFloor;
import com.tsix.immocrm.entities.cfg.CfgTrancheStkItem;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureOngletsTranche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileRepository;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.adm.AdmWebHistoricsRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheCoveredFloorRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheStkItemRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixturePatrimoine.ROLE_ADMIN;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FEATURE-patrimoine-tranche-onglets.md §4 et §7. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuppressionTrancheTest {

    private static final String URL = "/api/tranches/{id}";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureOngletsTranche onglets;
    @Autowired private CfgTrancheRepository trancheRepository;
    @Autowired private CfgTrancheStkItemRepository lienRepository;
    @Autowired private CfgTrancheCoveredFloorRepository plancherRepository;
    @Autowired private AdmUserHabilitationRepository habilitationRepository;
    @Autowired private AdmAttachedFileRepository pieceJointeRepository;
    @Autowired private AdmWebHistoricsRepository historiques;
    @Autowired private EntityManager em;

    private AdmUser admin;

    @BeforeEach
    void setUp() {
        admin = fixture.utilisateur("ADM-TRA-DEL");
    }

    private ResultActions supprimer(UUID id, String role) throws Exception {
        return mockMvc.perform(delete(URL, id).with(connecte(admin, role)));
    }

    private CfgTranche trancheVide(String code) {
        return fixture.tranche(code, fixture.projet("PRJ-" + code, fixture.entite("ENT-" + code, true)));
    }

    private long messages(UUID categoryId) {
        return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM AdmMessage WHERE CategoryId = :id")
                .setParameter("id", categoryId).getSingleResult()).longValue();
    }

    @Test
    void trancheAvecArticle_409QuiListeLesTablesBloquantes() throws Exception {
        FixturePatrimoine.Chaine chaine = fixture.chaine("TRA-DEL-ART");
        fixture.article(chaine, "TST-ART-DEL");

        supprimer(chaine.tranche().getId(), ROLE_ADMIN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("1 article(s)")));
        assertThat(trancheRepository.findById(chaine.tranche().getId())).isPresent();
    }

    @Test
    void trancheVide_204_terrainsOngletsHabilitationsPiecesJointesEtNotesSupprimes() throws Exception {
        CfgTranche tranche = trancheVide("TRA-DEL-OK");
        StkItem terrain = onglets.terrain("TST-TER-DEL");
        CfgTrancheStkItem lien = new CfgTrancheStkItem();
        lien.setId(UUID.randomUUID());
        lien.setCfgTranche(tranche);
        lien.setStkItem(terrain);
        lienRepository.save(lien);
        CfgTrancheCoveredFloor plancher = new CfgTrancheCoveredFloor();
        plancher.setId(UUID.randomUUID());
        plancher.setCfgTranche(tranche);
        plancher.setStkItemCategory(fixture.categorie("TST-APP-DEL", fixture.natureBatiment(), false));
        plancher.setArea(BigDecimal.TEN);
        plancherRepository.save(plancher);
        AdmUserHabilitation habilitation = new AdmUserHabilitation();
        habilitation.setId(UUID.randomUUID());
        habilitation.setAdmUser(admin);
        habilitation.setCfgTranche(tranche);
        habilitationRepository.save(habilitation);
        AdmAttachedFile piece = new AdmAttachedFile();
        piece.setId(UUID.randomUUID());
        piece.setCategoryId(tranche.getId());
        piece.setDescription("Plan de masse");
        pieceJointeRepository.save(piece);
        em.createNativeQuery("INSERT INTO AdmMessage (PKey, CategoryId, Notes) VALUES (:pkey, :id, 'note')")
                .setParameter("pkey", UUID.randomUUID()).setParameter("id", tranche.getId()).executeUpdate();
        em.flush();

        supprimer(tranche.getId(), ROLE_ADMIN).andExpect(status().isNoContent());
        em.flush();
        em.clear();

        assertAll("tout part avec la tranche",
                () -> assertThat(trancheRepository.findById(tranche.getId())).isEmpty(),
                () -> assertThat(lienRepository.findById(lien.getId())).isEmpty(),
                () -> assertThat(plancherRepository.findById(plancher.getId())).isEmpty(),
                () -> assertThat(habilitationRepository.findById(habilitation.getId())).isEmpty(),
                () -> assertThat(pieceJointeRepository.findById(piece.getId())).isEmpty(),
                () -> assertThat(messages(tranche.getId())).isZero());
    }

    @Test
    void nonAdministrateur_403() throws Exception {
        supprimer(trancheVide("TRA-DEL-403").getId(), "Commercial").andExpect(status().isForbidden());
    }

    @Test
    void trancheInconnueOuEntiteInactive_404() throws Exception {
        supprimer(UUID.randomUUID(), ROLE_ADMIN).andExpect(status().isNotFound());
        CfgTranche masquee = fixture.tranche("TRA-DEL-INACT",
                fixture.projet("PRJ-DEL-INACT", fixture.entite("ENT-DEL-INACT", false)));
        supprimer(masquee.getId(), ROLE_ADMIN).andExpect(status().isNotFound());
    }

    @Test
    void audit_ligneDeletedSurLaTranche() throws Exception {
        CfgTranche tranche = trancheVide("TRA-DEL-AUD");
        AuditDeTest.oublierPreparation(em);

        supprimer(tranche.getId(), ROLE_ADMIN).andExpect(status().isNoContent());
        AuditDeTest.simulerCommit(em, admin);

        assertThat(historiques.findByObjectId(tranche.getId())).singleElement().satisfies(h -> assertThat(h)
                .extracting(AdmWebHistorics::getObjectType, AdmWebHistorics::getAction)
                .containsExactly("Yes.Cfg.CfgTranche", "Deleted"));
    }
}
