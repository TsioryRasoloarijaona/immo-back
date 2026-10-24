package com.tsix.immocrm.controller.dossier;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.web.export.ExportTableur;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Appels communs aux tests de l'Historique des ventes : même filtre sur la liste, les groupes, les Σ et l'export. */
final class HistoriqueVentesSupport {

    static final String LISTE = "/api/dossiers";
    static final RequestPostProcessor SESSION_DU_TEST = requete -> requete;

    private HistoriqueVentesSupport() {
    }

    /** Requête GET dont les paramètres sont donnés en paires nom, valeur (un nom peut se répéter). */
    static MockHttpServletRequestBuilder requete(String url, RequestPostProcessor appelant, String... paires) {
        MockHttpServletRequestBuilder requete = get(url).with(appelant);
        for (int i = 0; i < paires.length; i += 2) {
            requete.param(paires[i], paires[i + 1]);
        }
        return requete;
    }

    static String corps(MockMvc mockMvc, MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    /** Identifiants des lignes de la liste (page de 100). */
    static List<String> ids(MockMvc mockMvc, RequestPostProcessor appelant, String... paires) throws Exception {
        String json = corps(mockMvc, requete(LISTE, appelant, paires).param("size", "100"));
        return JsonPath.read(json, "$.content[*].id");
    }

    static int totalListe(MockMvc mockMvc, RequestPostProcessor appelant, String... paires) throws Exception {
        return JsonPath.read(corps(mockMvc, requete(LISTE, appelant, paires)), "$.totalElements");
    }

    static int lignesExport(MockMvc mockMvc, RequestPostProcessor appelant, String... paires) throws Exception {
        MvcResult asynchrone = mockMvc.perform(requete(LISTE + "/export", appelant, paires).param("colonnes", "code"))
                .andExpect(request().asyncStarted()).andReturn();
        byte[] contenu = mockMvc.perform(asyncDispatch(asynchrone)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(contenu))) {
            return classeur.getSheet(ExportTableur.FEUILLE).getLastRowNum();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Liste, groupes, totaux, totaux-colonnes et export comptent le même nombre de dossiers sous le même filtre. */
    static void verifierCoherence(MockMvc mockMvc, RequestPostProcessor appelant, int attendu, String... paires)
            throws Exception {
        assertThat(totalListe(mockMvc, appelant, paires)).as("liste").isEqualTo(attendu);

        String groupes = corps(mockMvc, requete(LISTE + "/groupes", appelant, paires)
                .param("groupBy", "statut").param("inclureSansValeur", "true").param("size", "100"));
        List<Number> effectifs = JsonPath.read(groupes, "$.content[*].nombre");
        assertThat(effectifs.stream().mapToLong(Number::longValue).sum()).as("somme des groupes").isEqualTo(attendu);
        assertThat(((Number) JsonPath.read(groupes, "$.nombreTotal")).longValue()).as("nombreTotal des groupes")
                .isEqualTo(attendu);

        String totaux = corps(mockMvc, requete(LISTE + "/totaux", appelant, paires));
        assertThat(((Number) JsonPath.read(totaux, "$.nombreDossiers")).longValue()).as("/totaux").isEqualTo(attendu);

        String colonnes = corps(mockMvc, requete(LISTE + "/totaux-colonnes", appelant, paires));
        assertThat(((Number) JsonPath.read(colonnes, "$.nombreTotal")).longValue()).as("/totaux-colonnes")
                .isEqualTo(attendu);

        assertThat(lignesExport(mockMvc, appelant, paires)).as("lignes de l'export").isEqualTo(attendu);
    }
}
