package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComProspectionKindRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
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
 * Le filtre {@code ouvertes} est <b>ternaire</b> — voir {@code instructions/from_front.md},
 * demande 15.
 *
 * <p>Il était traité comme un drapeau : seul {@code true} posait un prédicat, {@code false}
 * valait « pas de filtre ». L'écran « Archivé : Oui » envoyait bien {@code ouvertes=false} et
 * recevait toute la liste. Les quatre endpoints qui acceptent le paramètre partagent le même
 * prédicat ({@code ProspectionService.buildSpec}) : ils sont testés tous les quatre, parce que
 * la liste et ses groupes doivent porter exactement les mêmes lignes.
 *
 * <p>Chaque test filtre sur son propre préfixe de code : la base de test partage ses tables.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectionFiltreOuvertesTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ComProspectionRepository prospectionRepository;
    @Autowired
    private ComProspectionKindRepository kindRepository;
    @Autowired
    private CfgTierRepository tierRepository;

    private final String prefixe = "OUV" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

    private ComProspectionKind newEtape() {
        ComProspectionKind kind = new ComProspectionKind();
        kind.setId(UUID.randomUUID());
        kind.setCode(prefixe + "-ETAPE");
        return kindRepository.save(kind);
    }

    private CfgTier newProspect() {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode(prefixe + "-T-" + UUID.randomUUID());
        tier.setLastName("Filtre");
        return tierRepository.save(tier);
    }

    private ComProspection newProspection(String suffixe, Boolean fermee, ComProspectionKind etape,
                                          boolean preProspection) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(prefixe + "-" + suffixe);
        p.setComProspectionKind(etape);
        p.setDocDate(LocalDateTime.now());
        p.setIsClosed(fermee);
        if (preProspection) {
            p.setStrEntityId(DiscriminantPreProspection.ID_PRE_PROSPECTION);
            p.setTempDescription("Lead " + suffixe);
        } else {
            p.setCfgProspect(newProspect());
        }
        return prospectionRepository.saveAndFlush(p);
    }

    /** Deux ouvertes — dont une à {@code IsClosed} NULL, comme 937 lignes réelles — et une archivée. */
    private ComProspection[] troisProspections(boolean pre) {
        ComProspectionKind etape = newEtape();
        return new ComProspection[]{
                newProspection("A", false, etape, pre),
                newProspection("B", null, etape, pre),
                newProspection("C", true, etape, pre)};
    }

    // ---------------------------------------------------------------- prospections

    @Test
    void liste_ouvertesFalse_neSertQueLesArchivees() throws Exception {
        ComProspection[] p = troisProspections(false);

        mockMvc.perform(get("/api/prospections").param("ouvertes", "false").param("search", prefixe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(p[2].getId().toString()))
                .andExpect(jsonPath("$.content[0].fermee").value(true));
    }

    @Test
    void liste_ouvertesTrue_sertLesOuvertes_nullCompris() throws Exception {
        ComProspection[] p = troisProspections(false);

        mockMvc.perform(get("/api/prospections").param("ouvertes", "true").param("search", prefixe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.id=='" + p[0].getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + p[1].getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + p[2].getId() + "')]").doesNotExist());
    }

    @Test
    void liste_sansParametre_sertTout() throws Exception {
        troisProspections(false);

        mockMvc.perform(get("/api/prospections").param("search", prefixe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    /** La somme des effectifs des groupes doit retomber sur le total de la liste, à filtre égal. */
    @Test
    void groupes_appliquentLeMemePredicatQueLaListe() throws Exception {
        troisProspections(false);
        String cle = prefixe + "-ETAPE";

        mockMvc.perform(get("/api/prospections/groupes").param("groupBy", "etape")
                        .param("ouvertes", "false").param("search", prefixe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.cle=='" + cle + "')].nombre").value(1));

        mockMvc.perform(get("/api/prospections/groupes").param("groupBy", "etape")
                        .param("ouvertes", "true").param("search", prefixe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.cle=='" + cle + "')].nombre").value(2));

        mockMvc.perform(get("/api/prospections/groupes").param("groupBy", "etape")
                        .param("search", prefixe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.cle=='" + cle + "')].nombre").value(3));
    }

    // ---------------------------------------------------------------- pré-prospections

    @Test
    void preProspections_liste_troisEtats() throws Exception {
        ComProspection[] p = troisProspections(true);

        mockMvc.perform(get("/api/pre-prospections").param("ouvertes", "false").param("search", prefixe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(p[2].getId().toString()));

        mockMvc.perform(get("/api/pre-prospections").param("ouvertes", "true").param("search", prefixe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/pre-prospections").param("search", prefixe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void preProspections_groupes_appliquentLeMemePredicat() throws Exception {
        troisProspections(true);
        String cle = prefixe + "-ETAPE";

        mockMvc.perform(get("/api/pre-prospections/groupes").param("groupBy", "etape")
                        .param("ouvertes", "false").param("search", prefixe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.cle=='" + cle + "')].nombre").value(1));

        mockMvc.perform(get("/api/pre-prospections/groupes").param("groupBy", "etape")
                        .param("ouvertes", "true").param("search", prefixe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.cle=='" + cle + "')].nombre").value(2));
    }
}
