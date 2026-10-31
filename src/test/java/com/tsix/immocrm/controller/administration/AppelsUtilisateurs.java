package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.controller.administration.ArbreHabilitations.Noeud;
import com.tsix.immocrm.fixtures.CorpsJson;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.COMPTES_DEPOT;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.COMPTE_IDS;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.FICHE;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.HABILITATIONS;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.NOEUDS;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.RACINE;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.ROLES;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.ROLE_IDS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Appels HTTP de l'écran Utilisateurs au nom d'un compte donné. FEATURE-admin-utilisateurs.md. */
final class AppelsUtilisateurs {

    private final MockMvc mockMvc;
    private final RequestPostProcessor compte;

    AppelsUtilisateurs(MockMvc mockMvc, RequestPostProcessor compte) {
        this.mockMvc = mockMvc;
        this.compte = compte;
    }

    static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder requete, String corps) {
        return requete.contentType(MediaType.APPLICATION_JSON).content(corps);
    }

    /** Corps de {@code PUT …/habilitations}, le type étant celui que l'arbre lu donne au nœud. */
    static CorpsJson corpsNoeuds(Noeud... noeuds) {
        return corps().avec(NOEUDS, Arrays.stream(noeuds).map(n -> corps().avec("type", n.type()).avec("id", n.id())).toList());
    }

    ResultActions creer(CorpsJson corps) throws Exception {
        return mockMvc.perform(json(post(RACINE), corps.json()).with(compte));
    }

    ResultActions modifier(UUID id, CorpsJson corps) throws Exception {
        return mockMvc.perform(json(patch(FICHE, id), corps.json()).with(compte));
    }

    ResultActions roles(UUID id, UUID... roles) throws Exception {
        return ecrire(ROLES, id, corps().avec(ROLE_IDS, List.of(roles)));
    }

    ResultActions comptesDepot(UUID id, UUID... depots) throws Exception {
        return ecrire(COMPTES_DEPOT, id, corps().avec(COMPTE_IDS, List.of(depots)));
    }

    ResultActions habilitations(UUID id, Noeud... noeuds) throws Exception {
        return habilitations(id, corpsNoeuds(noeuds));
    }

    ResultActions habilitations(UUID id, CorpsJson corps) throws Exception {
        return ecrire(HABILITATIONS, id, corps);
    }

    private ResultActions ecrire(String route, UUID id, CorpsJson corps) throws Exception {
        return mockMvc.perform(json(put(route, id), corps.json()).with(compte));
    }

    ResultActions fiche(UUID id) throws Exception {
        return mockMvc.perform(get(FICHE, id).with(compte));
    }

    ArbreHabilitations arbre(UUID id) throws Exception {
        return ArbreHabilitations.lu(mockMvc.perform(get(HABILITATIONS, id).with(compte)).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    ResultActions lire(String route, String... paires) throws Exception {
        MockHttpServletRequestBuilder requete = get(route).with(compte);
        for (int i = 0; i < paires.length; i += 2) {
            requete.param(paires[i], paires[i + 1]);
        }
        return mockMvc.perform(requete);
    }

    /** Corps d'une lecture qui doit répondre 200. */
    String corpsLu(String route, String... paires) throws Exception {
        return lire(route, paires).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    /** Résultat d'un export, dispatch asynchrone joué ; le statut reste à vérifier. */
    MvcResult exporter(String route, String... paires) throws Exception {
        MvcResult resultat = lire(route, paires).andReturn();
        return resultat.getRequest().isAsyncStarted() ? mockMvc.perform(asyncDispatch(resultat)).andReturn() : resultat;
    }

    /** PATCH ou PUT accepté : 200 avec la fiche ou l'arbre (réponse 13). */
    static void acceptee(ResultActions reponse) throws Exception {
        reponse.andExpect(status().isOk());
    }

    static int statut(ResultActions reponse) {
        return reponse.andReturn().getResponse().getStatus();
    }
}
