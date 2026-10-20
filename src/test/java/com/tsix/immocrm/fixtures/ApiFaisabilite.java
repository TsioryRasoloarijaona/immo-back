package com.tsix.immocrm.fixtures;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Appels HTTP des études de faisabilité et de leur configuration, au nom d'un Administrateur (FEATURE-patrimoine-faisabilite.md). */
public final class ApiFaisabilite {

    private final MockMvc mockMvc;
    private final AdmUser admin;

    public ApiFaisabilite(MockMvc mockMvc, AdmUser admin) {
        this.mockMvc = mockMvc;
        this.admin = admin;
    }

    public ResultActions poster(String url, String json) throws Exception {
        return mockMvc.perform(post(url).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    public ResultActions creer(CorpsJson corps) throws Exception {
        return poster(RACINE, corps.json());
    }

    public UUID etude(CorpsJson corps) throws Exception {
        String json = creer(corps).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    public ResultActions calculer(CorpsJson corps) throws Exception {
        return poster(CALCUL, corps.json());
    }

    public ResultActions modifier(UUID id, String json) throws Exception {
        return mockMvc.perform(patch(FICHE, id).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    public ResultActions unites(UUID id, String json) throws Exception {
        return mockMvc.perform(put(UNITES, id).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    public ResultActions tranches(UUID id, String json) throws Exception {
        return mockMvc.perform(put(TRANCHES, id).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    public ResultActions bloquer(UUID id) throws Exception {
        return mockMvc.perform(post(BLOQUER, id).with(connecte(admin)));
    }

    public ResultActions debloquer(UUID id) throws Exception {
        return mockMvc.perform(post(DEBLOQUER, id).with(connecte(admin)));
    }

    // ------------------------------------------------------------------ configuration (patron P6)

    private UUID referentiel(String slug, CorpsJson corps) throws Exception {
        String json = mockMvc.perform(post(REFERENTIELS, slug).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(corps.json()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    /** Fonction spécifique : drapeaux dans l'ordre vendable, couvert, nette, brute, emprise ; jamais « total ». */
    public UUID fonction(String code, boolean vendable, boolean couvert, boolean nette, boolean brute, boolean emprise)
            throws Exception {
        return referentiel(SLUG_FONCTIONS, corpsFonction(code, false)
                .avec(SURFACE_VENDABLE, vendable).avec(F_PLANCHER_COUVERT, couvert).avec(SURFACE_NETTE, nette)
                .avec(SURFACE_BRUTE, brute).avec(F_EMPRISE_AU_SOL, emprise));
    }

    public UUID fonctionTotal(String code) throws Exception {
        return referentiel(SLUG_FONCTIONS, corpsFonction(code, true).avec(SURFACE_VENDABLE, true)
                .avec(F_PLANCHER_COUVERT, false).avec(SURFACE_NETTE, false).avec(SURFACE_BRUTE, false)
                .avec(F_EMPRISE_AU_SOL, false));
    }

    private static CorpsJson corpsFonction(String code, boolean total) {
        return corps().avec(SORTING, 1).avec(CODE, code).avec(DESCRIPTION, "Fonction " + code).avec(EST_TOTAL, total);
    }

    /** Unité de base ; {@code fonctionId} null = sans fonction spécifique. */
    public UUID unite(String code, boolean surface, UUID fonctionId) throws Exception {
        return referentiel(SLUG_UNITES_BASE, corps().avec(SORTING, 1).avec(CODE, code)
                .avec(DESCRIPTION, "Unite " + code).avec(EST_SURFACE, surface).avec(FONCTION_SPECIFIQUE_ID, fonctionId));
    }

    public ResultActions modifierUnite(UUID uniteId, CorpsJson corps) throws Exception {
        return mockMvc.perform(patch(REFERENTIEL, SLUG_UNITES_BASE, uniteId).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    public static BigDecimal d(String valeur) {
        return new BigDecimal(valeur);
    }
}
