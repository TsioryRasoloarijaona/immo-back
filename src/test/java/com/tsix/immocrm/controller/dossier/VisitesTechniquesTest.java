package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.mail.EnvoiEmailMemoire;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Calendrier des visites techniques — FEATURE-vente-fiche.md §B4. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class VisitesTechniquesTest {

    private static final String VISITES = "/api/dossiers/{id}/visites-techniques";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private CfgTrancheRepository trancheRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private CfgProjectRepository projetRepository;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private EnvoiEmailMemoire envoiEmail;

    private CfgTranche tranche;
    private LocalDateTime dansTroisJours;

    @BeforeEach
    void setUp() {
        envoiEmail.vider();
        tranche = fixture.tranche("VIS-TR", true);
        CfgTier sav = fixture.tier("SAV");
        sav.setEMail("sav.test@ayad.test");
        tierRepository.save(sav);
        tranche.setCfgClaimsManager(sav);
        tranche.setInterventionMinimumDelai(2);
        tranche.setMaxVisitNumber(1);
        trancheRepository.save(tranche);
        dansTroisJours = LocalDate.now().plusDays(3).atTime(10, 0);
    }

    private ResultActions programmer(ComFolder dossier, LocalDateTime debut) throws Exception {
        String corps = "{\"debut\":\"" + debut + "\",\"fin\":\"" + debut.plusHours(1) + "\",\"note\":\"RDV\"}";
        return mockMvc.perform(post(VISITES, dossier.getId()).contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    @Test
    void programmer_enregistreLaVisite_dateDeReception_etMailAuSav() throws Exception {
        ComFolder dossier = fixture.dossier("DOS-VIS", tranche, StatutDossier.REMISE_DES_CLEFS);

        programmer(dossier, dansTroisJours)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailEnvoye").value(true))
                .andExpect(jsonPath("$.visite.dossierId").value(dossier.getId().toString()));

        assertThat(folderRepository.findById(dossier.getId()).orElseThrow().getReceptionDate()).isEqualTo(dansTroisJours);
        assertThat(envoiEmail.messages()).singleElement()
                .satisfies(m -> assertThat(m.destinataire()).isEqualTo("sav.test@ayad.test"))
                .satisfies(m -> assertThat(m.copies()).isEmpty());

        mockMvc.perform(get(VISITES, dossier.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxVisitesParJour").value(1))
                .andExpect(jsonPath("$.creneaux.length()").value(1));
    }

    @Test
    void programmer_metEnCopieLeDirecteurTechniqueDuProgramme() throws Exception {
        CfgTier directeur = fixture.tier("DIRTECH");
        directeur.setEMail("directeur.technique@ayad.test");
        tierRepository.save(directeur);
        CfgProject projet = tranche.getCfgProject();
        projet.setCfgTechnicalManager(directeur);
        projetRepository.save(projet);
        ComFolder dossier = fixture.dossier("DOS-VIS6", tranche, StatutDossier.REMISE_DES_CLEFS);

        programmer(dossier, dansTroisJours).andExpect(status().isCreated());

        assertThat(envoiEmail.messages()).singleElement()
                .satisfies(m -> assertThat(m.destinataire()).isEqualTo("sav.test@ayad.test"))
                .satisfies(m -> assertThat(m.copies()).containsExactly("directeur.technique@ayad.test"));
    }

    @Test
    void programmer_quotaDuJourAtteintSurLaTranche_409() throws Exception {
        ComFolder premier = fixture.dossier("DOS-VIS1", tranche, StatutDossier.REMISE_DES_CLEFS);
        ComFolder second = fixture.dossier("DOS-VIS2", tranche, StatutDossier.REMISE_DES_CLEFS);
        programmer(premier, dansTroisJours).andExpect(status().isCreated());

        programmer(second, dansTroisJours.plusHours(3))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Vous ne devez pas programmer plus que 1 visites technique par jour"));
    }

    @Test
    void programmer_delaiMinimumNonRespecte_409() throws Exception {
        ComFolder dossier = fixture.dossier("DOS-VIS3", tranche, StatutDossier.REMISE_DES_CLEFS);

        programmer(dossier, LocalDateTime.now().plusMinutes(30))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Vous devez programmer la visite technique 2 heures à l'avance"));
    }

    @Test
    void programmer_trancheSansResponsableSav_409() throws Exception {
        tranche.setCfgClaimsManager(null);
        trancheRepository.save(tranche);
        ComFolder dossier = fixture.dossier("DOS-VIS4", tranche, StatutDossier.REMISE_DES_CLEFS);

        programmer(dossier, dansTroisJours).andExpect(status().isConflict());
    }

    @Test
    void programmer_finAvantDebut_400() throws Exception {
        ComFolder dossier = fixture.dossier("DOS-VIS5", tranche, StatutDossier.REMISE_DES_CLEFS);
        String corps = "{\"debut\":\"" + dansTroisJours + "\",\"fin\":\"" + dansTroisJours.minusHours(1) + "\"}";

        mockMvc.perform(post(VISITES, dossier.getId()).contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isBadRequest());
    }

    @Test
    void calendrier_dossierInconnu_404() throws Exception {
        mockMvc.perform(get(VISITES, UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
