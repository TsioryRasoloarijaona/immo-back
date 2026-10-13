package com.tsix.immocrm.controller.equipe;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.services.equipe.RolesEquipe;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue FEATURE-repartition-notifications.md §3.2 : {@code simulation=true} est l'aperçu de ce que
 * {@code simulation=false} écrira. Deux leads du même lot au même numéro : l'aperçu doit annoncer la même
 * répartition que l'écriture réelle.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RepartitionApercuFideleTest {

    private static final String ADMIN = "Administrateur";

    @Autowired private MockMvc mockMvc;
    @Autowired private EquipeOutils outils;
    @Autowired private EntityManager em;

    @Test
    void repartir_deuxLeadsAuMemeNumero_lApercuAnnonceCeQueLEcritureFait() throws Exception {
        CfgProject projet = outils.projet("REV-A");
        CfgTranche tranche = outils.tranche("REV-TA", projet);
        AdmUser admin = outils.compte("rev.admin", ADMIN);
        AdmUser commercialA = outils.compte("rev.coma", RolesEquipe.COMMERCIAL);
        outils.affecter(commercialA, projet);
        AdmUser commercialB = outils.compte("rev.comb", RolesEquipe.COMMERCIAL);
        outils.affecter(commercialB, projet);

        String telephone = "06" + ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999);
        LocalDateTime hier = LocalDateTime.now().minusDays(1);
        ComProspection l1 = lead("REV-L1", tranche, hier, telephone);
        ComProspection l2 = lead("REV-L2", tranche, hier, telephone);
        List<UUID> ids = List.of(l1.getId(), l2.getId());

        List<String> apercu = commerciaux(repartir(admin, ids, true));
        List<String> ecrit = commerciaux(repartir(admin, ids, false));

        assertThat(ecrit).hasSize(2);
        assertThat(apercu).isEqualTo(ecrit);
    }

    private ComProspection lead(String code, CfgTranche tranche, LocalDateTime docDate, String telephone) {
        ComProspection p = outils.preProspection(code, tranche, docDate);
        p.setCfgProspect(null);
        p.setTempDescription("Lead " + code);
        p.setTempTel(telephone);
        return p;
    }

    private String repartir(AdmUser appelant, List<UUID> ids, boolean simulation) throws Exception {
        em.flush();
        String liste = ids.stream().map(id -> "\"" + id + "\"").collect(Collectors.joining(",", "[", "]"));
        return mockMvc.perform(post("/api/equipe/repartir")
                        .with(EquipeOutils.connecte(appelant, ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preProspectionIds\":" + liste + ",\"simulation\":" + simulation + "}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static List<String> commerciaux(String reponse) {
        return JsonPath.read(reponse, "$.attributions[*].commercialId");
    }
}
