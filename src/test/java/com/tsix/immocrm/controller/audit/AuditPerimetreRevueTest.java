package com.tsix.immocrm.controller.audit;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.adm.AdmWebHistorics;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.adm.AdmWebHistoricsRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixturePatrimoine.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue FEATURE-patrimoine-audit-actions.md §2.3 et §7 : le journal d'audit ne doit pas rouvrir, par l'id d'un objet,
 * ce que le périmètre commercial ferme sur la route de cet objet (FEATURE-perimetre-commercial.md §6.5).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuditPerimetreRevueTest {

    private static final String SECRET = "MONTANT-CONFIDENTIEL-REVUE";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private AdmUserHabilitationRepository habilitationRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private AdmWebHistoricsRepository historiques;

    @Test
    void commercialRestreint_auditDUneProspectionHorsPerimetre_neFuitPas() throws Exception {
        CfgCompany entite = fixture.entite("ENT-AUD-PER", true);
        CfgProject projetA = fixture.projet("PRJ-AUD-PER-A", entite);
        CfgTranche trancheB = fixture.tranche("TR-AUD-PER-B", fixture.projet("PRJ-AUD-PER-B", entite));

        AdmUser commercial = fixture.utilisateur("COM-AUD-PER");
        AdmUserHabilitation habilitation = new AdmUserHabilitation();
        habilitation.setId(UUID.randomUUID());
        habilitation.setAdmUser(commercial);
        habilitation.setCfgProject(projetA);
        habilitationRepository.save(habilitation);

        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("CLI-AUD-PER-B");
        tierRepository.save(prospect);
        ComProspection prospectionB = new ComProspection();
        prospectionB.setId(UUID.randomUUID());
        prospectionB.setCode("PSN-AUD-PER-B");
        prospectionB.setCfgProspect(prospect);
        prospectionB.setStkHierarchy(trancheB);
        prospectionRepository.save(prospectionB);

        // Ligne telle qu'OMRAN l'écrit pour une prospection (ObjectType mesuré en production).
        AdmWebHistorics ligne = new AdmWebHistorics();
        ligne.setId(UUID.randomUUID());
        ligne.setObjectId(prospectionB.getId());
        ligne.setActionDateTime(LocalDateTime.of(2026, 10, 1, 9, 0));
        ligne.setAction("Modified");
        ligne.setObjectType("Yes.Com.Prospection.Prospection");
        ligne.setObjectTypeLabel("Prospection");
        ligne.setObjectLabel("PSN-AUD-PER-B");
        ligne.setDescription("PSN-AUD-PER-B");
        ligne.setDetails("[[\"BudgetMax\",\"\",\"" + SECRET + "\"]]");
        historiques.save(ligne);

        // Témoin : la route de la prospection elle-même est fermée à ce commercial.
        mockMvc.perform(get("/api/prospections/{id}", prospectionB.getId()).with(connecte(commercial, "Commercial")))
                .andExpect(status().isNotFound());

        String corps = mockMvc.perform(get("/api/audit").param("objetId", prospectionB.getId().toString())
                        .with(connecte(commercial, "Commercial")))
                .andReturn().getResponse().getContentAsString();

        assertThat(corps)
                .as("le journal d'audit sert les détails d'une prospection hors périmètre")
                .doesNotContain(SECRET)
                .doesNotContain("PSN-AUD-PER-B");
    }
}
