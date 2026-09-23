package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Filtres avancés de {@code GET /api/prospections} — lot C, voir
 * {@code instructions/INVENTAIRE-colonnes-prospection.md} §3.
 *
 * <p>Deux familles y sont vérifiées séparément, parce qu'elles n'ont ni le même coût ni le même
 * comportement : les filtres portés par l'entité, et ceux traduits en sous-requête sur
 * {@code ComSaleProspectionView}.
 *
 * <p><b>Le piège de cette famille-là</b> : la vue exclut les pré-prospections et les lignes sans
 * {@code StrEntityId}. Activer un filtre « vue » les écarte donc toutes, silencieusement. C'est
 * cohérent — ces lignes n'ont aucun indicateur d'activité — mais ce n'est évident pour personne,
 * d'où le test dédié.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectionFiltresAvancesTest {

    private static final UUID ENTITE_PROSPECTION =
            UUID.fromString("e7c73f70-1111-40d9-aa78-a2825c42b601");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    private String prospection(UUID entite, Boolean ancienClient) {
        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("FA-TIER-" + UUID.randomUUID());
        prospect.setFirstName("Filtre");
        prospect.setLastName("TEST");
        tierRepository.save(prospect);

        String code = "PSN-FA-" + UUID.randomUUID();
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        // Hier, pas maintenant : InactivityTimeout = DATEDIFF(DAY, DocDate, GETDATE()) côté vue.
        // Sur ce poste Windows compte « Morocco Standard Time » à +00:00 (et SQL Server suit
        // GETDATE()), alors que la JVM résout la même zone en Africa/Casablanca à +01:00.
        // Entre 23 h et minuit heure serveur, now() de la JVM tombe donc le lendemain et le
        // DATEDIFF vaut -1 : les tests à inactiviteMin=0 échouaient une heure par jour.
        // Avec un DocDate d'hier l'inactivité vaut 1 ou 2 selon l'horloge, jamais négative.
        p.setDocDate(LocalDateTime.now().minusDays(1));
        p.setStrEntityId(entite);
        p.setCfgProspect(prospect);
        p.setIsOldCustomer(ancienClient);
        prospectionRepository.saveAndFlush(p);
        return code;
    }

    @Test
    void filtreEntite_ancienClient_discrimine() throws Exception {
        String ancien = prospection(ENTITE_PROSPECTION, true);
        String nouveau = prospection(ENTITE_PROSPECTION, false);

        mockMvc.perform(get("/api/prospections").param("search", ancien).param("ancienClient", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/prospections").param("search", ancien).param("ancienClient", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        mockMvc.perform(get("/api/prospections").param("search", nouveau).param("ancienClient", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    /**
     * {@code isOldCustomer} nul doit être traité comme « pas un ancien client » — sans le
     * {@code OR IS NULL}, un filtre à {@code false} perdrait ces lignes.
     */
    @Test
    void filtreEntite_ancienClientFalse_inclutLesValeursNulles() throws Exception {
        String sansValeur = prospection(ENTITE_PROSPECTION, null);

        mockMvc.perform(get("/api/prospections").param("search", sansValeur).param("ancienClient", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    /** Aucun filtre avancé : le comportement ne bouge pas. */
    @Test
    void sansFiltreAvance_leResultatEstInchange() throws Exception {
        String code = prospection(ENTITE_PROSPECTION, null);

        mockMvc.perform(get("/api/prospections").param("search", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    /**
     * Filtre porté par la vue. La fixture y figure (elle a une {@code StrEntity}) et n'a aucune
     * interaction, donc son inactivité est calculée : un seuil à 0 la retient.
     */
    @Test
    void filtreVue_inactivite_sExecuteEtRetientLaFixture() throws Exception {
        String code = prospection(ENTITE_PROSPECTION, null);

        mockMvc.perform(get("/api/prospections").param("search", code).param("inactiviteMin", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    /**
     * Le seuil est bien appliqué : une inactivité impossible ne retient rien. Vérifie que la
     * sous-requête filtre réellement, et ne se contente pas d'exister.
     */
    @Test
    void filtreVue_seuilInatteignable_neRetientRien() throws Exception {
        String code = prospection(ENTITE_PROSPECTION, null);

        mockMvc.perform(get("/api/prospections").param("search", code).param("inactiviteMin", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    /**
     * L'effet de bord à connaître : une prospection sans {@code StrEntityId} n'est dans aucune
     * vue. Elle reste visible sans filtre « vue », et disparaît dès qu'on en active un.
     */
    @Test
    void filtreVue_ecarteLesLignesAbsentesDeLaVue() throws Exception {
        String horsVue = prospection(null, null);

        mockMvc.perform(get("/api/prospections").param("search", horsVue))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/prospections").param("search", horsVue).param("inactiviteMin", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    /** Les deux familles se combinent en ET, dans une seule requête. */
    @Test
    void lesDeuxFamillesSeCombinent() throws Exception {
        String code = prospection(ENTITE_PROSPECTION, true);

        mockMvc.perform(get("/api/prospections")
                        .param("search", code)
                        .param("ancienClient", "true")
                        .param("inactiviteMin", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/prospections")
                        .param("search", code)
                        .param("ancienClient", "false")
                        .param("inactiviteMin", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }
}
