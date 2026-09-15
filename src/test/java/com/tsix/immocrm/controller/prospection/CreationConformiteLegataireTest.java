package com.tsix.immocrm.controller.prospection;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Conformité des créations au logiciel de bureau — voir
 * {@code instructions/FIX-creation-prospect-prospection.md}.
 *
 * <p>Les deux écarts couverts ici viennent de la trace d'audit d'OMRAN : sa création de prospect
 * incrémente {@code AdmSequence} avant son {@code INSERT}, et sa création de prospection écrit un
 * {@code ComActionMessage} juste après le sien. Nous ne faisions ni l'un ni l'autre.
 *
 * <p>Les corps JSON sont écrits à la main, comme ailleurs dans ce dépôt : Spring Boot 4.1 sérialise
 * avec Jackson&nbsp;3 et n'expose aucun {@code ObjectMapper} Jackson&nbsp;2 injectable.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class CreationConformiteLegataireTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CfgTierRepository tierRepository;

    @Autowired
    private ComActionMessageRepository actionMessageRepository;

    // ---------------------------------------------------------------- le code du prospect

    /**
     * Sur 13 913 prospects et clients réels, 12 seulement n'ont pas de code. Chaque prospect créé
     * sans code par l'API rejoignait cette anomalie marginale.
     */
    @Test
    void creationProspect_attribueUnCode() throws Exception {
        String body = """
                {"prenom":"Ines","nom":"ALAMI-CODE-TEST"}""";

        String reponse = mockMvc.perform(post("/api/prospects")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        // Format du légataire : « 26/14431 » — année sur 2 chiffres, numéro sur 5.
        assertThat(JsonPath.parse(reponse).read("$.code", String.class)).matches("\\d{2}/\\d{5}");
    }

    /** Deux prospects successifs ne partagent pas le même code : le compteur avance. */
    @Test
    void creationProspect_deuxCodesDistincts() throws Exception {
        assertThat(codeDuProspectCree("UN")).isNotEqualTo(codeDuProspectCree("DEUX"));
    }

    private String codeDuProspectCree(String suffixe) throws Exception {
        String body = """
                {"prenom":"Test","nom":"SEQUENCE-%s"}""".formatted(suffixe);
        String reponse = mockMvc.perform(post("/api/prospects")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.parse(reponse).read("$.code", String.class);
    }

    // ---------------------------------------------------------------- l'interaction de création

    /**
     * Le logiciel de bureau écrit systématiquement une interaction juste après la prospection.
     * Sans elle, la prospection naît sans aucun échange et tout ce qui se fonde sur les
     * interactions est faussé dès la création — dernier échange, délai de prise en charge,
     * compteurs d'activité.
     */
    @Test
    void creationProspection_ecritSonInteraction() throws Exception {
        CfgTier prospect = newProspect();
        UUID prospectionId = creerProspection(prospect, "\"notes\":\"note de creation\"");

        List<ComActionMessage> interactions = interactionsDe(prospectionId);
        assertThat(interactions).hasSize(1);
        ComActionMessage creation = interactions.get(0);

        // Les notes de la prospection sont reprises telles quelles — mesuré sur 1 507 des 2 634
        // interactions de création réelles, les écarts restants n'étant que des espaces.
        assertThat(creation.getAlertMessage()).isEqualTo("note de creation");
        // Pas une relance : aucune échéance, et désarmée d'emblée.
        assertThat(creation.getAlertValidityDate()).isNull();
        assertThat(creation.getWithoutRevival()).isTrue();
        // Le tiers est repris, comme sur les interactions manuelles.
        assertThat(creation.getCfgTier()).isNotNull();
        assertThat(creation.getCfgTier().getId()).isEqualTo(prospect.getId());
    }

    /**
     * {@code IsSystem = 1}, à l'inverse de ce que pose {@code creerInteraction} pour une saisie
     * humaine. Les deux conventions cohabitent volontairement : {@code ComSaleProspectionView}
     * calcule le dernier échange avec {@code AND IsSystem = 0}, et créer une prospection n'est pas
     * un échange avec le client. C'est le choix du légataire, mesuré sur ses 2 634 interactions de
     * création.
     */
    @Test
    void interactionDeCreation_estExclueDuDernierEchange() throws Exception {
        UUID prospectionId = creerProspection(newProspect(), null);
        ComActionMessage creation = interactionsDe(prospectionId).get(0);

        assertThat(creation.getIsSystem()).isTrue();
        assertThat(creation.getIsNotRead()).isFalse();
        // Notes absentes : le message l'est aussi, plutôt qu'une chaîne vide.
        assertThat(creation.getAlertMessage()).isNull();
    }

    // ---------------------------------------------------------------- fixtures

    private UUID creerProspection(CfgTier prospect, String champSupplementaire) throws Exception {
        String body = "{\"prospectId\":\"" + prospect.getId() + "\""
                + (champSupplementaire != null ? "," + champSupplementaire : "") + "}";
        String reponse = mockMvc.perform(post("/api/prospections")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.parse(reponse).read("$.id", String.class));
    }

    private List<ComActionMessage> interactionsDe(UUID prospectionId) {
        return actionMessageRepository.findAll().stream()
                .filter(m -> m.getComProspection() != null
                        && prospectionId.equals(m.getComProspection().getId()))
                .toList();
    }

    private CfgTier newProspect() {
        CfgTier t = new CfgTier();
        t.setId(UUID.randomUUID());
        t.setCode("CC-TIER-" + UUID.randomUUID());
        t.setFirstName("Prospect");
        t.setLastName("CREATION TEST");
        return tierRepository.save(t);
    }
}
