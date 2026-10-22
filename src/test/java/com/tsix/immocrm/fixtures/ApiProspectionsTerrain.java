package com.tsix.immocrm.fixtures;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.Contexte;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Appels HTTP du lot P7b au nom d'un Administrateur (FEATURE-patrimoine-prospections-terrain.md §5). */
public final class ApiProspectionsTerrain {

    private final MockMvc mockMvc;
    private final AdmUser admin;

    public ApiProspectionsTerrain(MockMvc mockMvc, AdmUser admin) {
        this.mockMvc = mockMvc;
        this.admin = admin;
    }

    /** Corps de création de la ligne de production (§1.1, §3.1) : Aménagé, 1 200 m², voies 100, COS 0,30, CUF 2,5. */
    public static CorpsJson corpsProspection(Contexte c, String code) {
        return corps().avec(CODE, code).avec(DESCRIPTION, "Prospection " + code)
                .avec(CATEGORIE_ID, c.categorie()).avec(TYPE_ID, c.type()).avec(NIVEAU_ID, c.niveau())
                .avec(VOCATION_ID, c.vocation()).avec(PAYS_ID, c.pays().getId()).avec(VILLE_ID, c.ville())
                .avec(EST_AMENAGE, true).avec(SUPERFICIE, d("1200")).avec(SUPERFICIE_EQUIPEMENTS, d("100"))
                .avec(COS, d("0.30")).avec(CUF, d("2.5")).avec(COEF_VENDABLE, d("1")).avec(PRIX_M2, d("4000"))
                .avec(PRIX_PROPOSE, d("4000000"));
    }

    public static BigDecimal d(String valeur) {
        return new BigDecimal(valeur);
    }

    public static BigDecimal lireDecimal(String json, String chemin) {
        Object valeur = JsonPath.read(json, chemin);
        return valeur == null ? null : new BigDecimal(valeur.toString());
    }

    /** Corps {@code {lignes:[…]}} du PUT des zones ; chaque ligne est un objet plat. */
    public static String grille(List<CorpsJson> lignes) {
        StringJoiner tableau = new StringJoiner(",", "[", "]");
        lignes.forEach(l -> tableau.add(l.json()));
        return "{\"" + LIGNES + "\":" + tableau + "}";
    }

    public ResultActions creer(CorpsJson corps) throws Exception {
        return mockMvc.perform(post(RACINE).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    public UUID creerOk(CorpsJson corps) throws Exception {
        String json = creer(corps).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    public ResultActions fiche(UUID id) throws Exception {
        return mockMvc.perform(get(FICHE, id).with(connecte(admin)));
    }

    public String ficheJson(UUID id) throws Exception {
        return fiche(id).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    public ResultActions liste(Map<String, String> filtres) throws Exception {
        var requete = get(RACINE).with(connecte(admin)).param("size", "200");
        filtres.forEach(requete::param);
        return mockMvc.perform(requete);
    }

    public ResultActions modifier(UUID id, CorpsJson corps) throws Exception {
        return mockMvc.perform(patch(FICHE, id).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    public ResultActions supprimer(UUID id) throws Exception {
        return mockMvc.perform(delete(FICHE, id).with(connecte(admin)));
    }

    public ResultActions zones(UUID id, List<CorpsJson> lignes) throws Exception {
        return mockMvc.perform(put(ZONES, id).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(grille(lignes)));
    }

    public ResultActions apercu(CorpsJson corps) throws Exception {
        return mockMvc.perform(post(APERCU).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    public ResultActions transitions(UUID id) throws Exception {
        return mockMvc.perform(get(TRANSITIONS, id).with(connecte(admin)));
    }

    public ResultActions executer(UUID id, CorpsJson corps) throws Exception {
        return mockMvc.perform(post(TRANSITION, id).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    public ResultActions executer(UUID id, UUID transitionId) throws Exception {
        return executer(id, corps().avec(TRANSITION_ID, transitionId));
    }

    public ResultActions historique(UUID id) throws Exception {
        return mockMvc.perform(get(HISTORIQUE, id).with(connecte(admin)));
    }

    public ResultActions transformer(UUID id, CorpsJson corps) throws Exception {
        return mockMvc.perform(post(TRANSFORMER, id).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }
}
