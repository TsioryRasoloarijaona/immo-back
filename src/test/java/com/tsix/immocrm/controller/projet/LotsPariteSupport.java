package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Appels des routes du lot L1 (FEATURE-lots-liste-parite.md E1–E7), noms du fil exacts. */
final class LotsPariteSupport {

    static final String LOTS = AppelsProjet.LOTS;
    static final String TOTAUX = LOTS + "/totaux";
    static final String EXPORT = LOTS + "/export";
    static final String SIZE = "size";
    static final String PAGE = "page";
    static final String STATUT_IDS = "statutIds";
    static final String ENTITE_ID = "entiteId";
    static final String PROJET_IDS = "projetIds";
    static final String SEARCH = "search";
    static final String CONTENU_IDS = "$.content[*].id";
    static final String TOTAL = "$.totalElements";

    static final String GENRE = "genre";
    static final String DESCRIPTION = "description";
    static final String PROJET_ID = "projetId";
    static final String CATEGORIE_ID = "categorieId";
    static final String TYPE_ID = "typeId";
    static final String FOURNISSEUR_ID = "fournisseurId";
    static final String TRANSITION_ID = "transitionId";
    static final String NOTES = "notes";

    /** Les cinq montants de E1 / E3, dans l'ordre du spec. */
    static final List<String> MONTANTS = List.of("coutPrevisionnelHT", "coutPrevisionnelTTC", "coutPrevisionnelNetAPayer",
            "coutPrevisionnelRegle", "coutPrevisionnelReste");

    private final MockMvc mockMvc;
    private final AppelsProjet appels;

    LotsPariteSupport(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
        this.appels = new AppelsProjet(mockMvc);
    }

    Reponse lire(RequestPostProcessor qui, String url, String... params) throws Exception {
        return appels.lire(qui, url, params);
    }

    private Reponse envoyer(MockHttpServletRequestBuilder requete) throws Exception {
        MockHttpServletResponse r = mockMvc.perform(requete).andReturn().getResponse();
        return new Reponse(r.getStatus(), r.getContentAsString(StandardCharsets.UTF_8));
    }

    /** POST /lots ; renvoie aussi l'en-tête Location. */
    Creation creer(RequestPostProcessor qui, CorpsJson corps) throws Exception {
        MockHttpServletResponse r = mockMvc.perform(post(LOTS).with(qui).contentType(MediaType.APPLICATION_JSON)
                .content(corps.json())).andReturn().getResponse();
        return new Creation(new Reponse(r.getStatus(), r.getContentAsString(StandardCharsets.UTF_8)), r.getHeader("Location"));
    }

    record Creation(Reponse reponse, String location) {

        int statut() {
            return reponse.statut();
        }

        UUID id() {
            return UUID.fromString((String) reponse.valeur("$.id"));
        }
    }

    Reponse supprimer(RequestPostProcessor qui, UUID lot) throws Exception {
        return envoyer(delete(LOTS + "/" + lot).with(qui));
    }

    Reponse transitions(RequestPostProcessor qui, UUID lot) throws Exception {
        return appels.lire(qui, LOTS + "/" + lot + "/transitions");
    }

    Reponse executer(RequestPostProcessor qui, UUID lot, UUID transition) throws Exception {
        return envoyer(post(LOTS + "/" + lot + "/transition").with(qui).contentType(MediaType.APPLICATION_JSON)
                .content(corps().avec(TRANSITION_ID, transition).json()));
    }

    /** Export : le classeur part en flux (asynchrone) ; un refus reste synchrone. */
    MockHttpServletResponse exporter(RequestPostProcessor qui, String... params) throws Exception {
        MockHttpServletRequestBuilder requete = get(EXPORT).with(qui).param("format", "xlsx");
        for (int i = 0; i + 1 < params.length; i += 2) {
            requete = requete.param(params[i], params[i + 1]);
        }
        MvcResult resultat = mockMvc.perform(requete).andReturn();
        if (resultat.getRequest().isAsyncStarted()) {
            resultat = mockMvc.perform(asyncDispatch(resultat)).andReturn();
        }
        return resultat.getResponse();
    }

    /** Toutes les lignes de E1 pour ces filtres, page par page (taille 1). */
    List<Map<String, Object>> toutesLesLignes(RequestPostProcessor qui, String... filtres) throws Exception {
        List<Map<String, Object>> lignes = new ArrayList<>();
        int page = 0;
        int total;
        do {
            List<String> params = new ArrayList<>(List.of(filtres));
            params.addAll(List.of(SIZE, "1", PAGE, String.valueOf(page)));
            Reponse r = lire(qui, LOTS, params.toArray(String[]::new));
            if (r.statut() != 200) {
                throw new AssertionError("liste " + r.statut() + " : " + r.corps());
            }
            List<Map<String, Object>> contenu = JsonPath.read(r.corps(), "$.content");
            lignes.addAll(contenu);
            total = r.nombre(TOTAL).intValue();
            page++;
        } while (lignes.size() < total && page <= total);
        return lignes;
    }

    /** Ligne de E1 du lot (recherche par son code, page large). */
    Map<String, Object> ligne(RequestPostProcessor qui, UUID lot, String code) throws Exception {
        Reponse r = lire(qui, LOTS, SEARCH, code, SIZE, AppelsProjet.TAILLE);
        List<Map<String, Object>> trouvees = JsonPath.read(r.corps(), "$.content[?(@.id == '" + lot + "')]");
        if (trouvees.size() != 1) {
            throw new AssertionError("lot " + code + " : " + r.statut() + " " + r.corps());
        }
        return new LinkedHashMap<>(trouvees.get(0));
    }

    static BigDecimal nombre(Object valeur) {
        return valeur == null ? null : new BigDecimal(String.valueOf(valeur));
    }

    static BigDecimal somme(List<Map<String, Object>> lignes, String champ) {
        return lignes.stream().map(l -> nombre(l.get(champ))).reduce(BigDecimal.ZERO,
                (a, b) -> b == null ? a : a.add(b));
    }
}
