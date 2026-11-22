package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureBilansProjet.BILANS;
import static com.tsix.immocrm.fixtures.FixtureBilansProjet.EXPORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Appels des Bilans N5a et lecture d'un nœud de l'arbre par {@code categorieId}, à toute profondeur. */
final class AppelsBilans {

    static final String PROJETS = "projets";
    static final String TTC = "ttc";
    static final String COLONNE1 = "colonne1";
    static final String COLONNE2 = "colonne2";

    private final AppelsProjet appels;
    private final MockMvc mockMvc;

    AppelsBilans(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
        this.appels = new AppelsProjet(mockMvc);
    }

    Reponse lire(RequestPostProcessor qui, String... paramsNomValeur) throws Exception {
        return appels.lire(qui, BILANS, paramsNomValeur);
    }

    MockHttpServletResponse exporter(RequestPostProcessor qui, String... paramsNomValeur) throws Exception {
        MockHttpServletRequestBuilder requete = get(EXPORT).with(qui);
        for (int i = 0; i + 1 < paramsNomValeur.length; i += 2) {
            requete = requete.param(paramsNomValeur[i], paramsNomValeur[i + 1]);
        }
        return mockMvc.perform(requete).andReturn().getResponse();
    }

    static String filtre(UUID categorie) {
        return "$..[?(@.categorieId == '" + categorie + "')]";
    }

    /** Nœuds de la catégorie dans tout l'arbre (0 si masquée). */
    static int occurrences(Reponse r, UUID categorie) {
        List<Object> noeuds = JsonPath.read(r.corps(), filtre(categorie));
        return noeuds.size();
    }

    /** Valeur d'une colonne (ou {@code ecart} si {@code colonne} nul) du nœud de la catégorie, présent une seule fois. */
    static BigDecimal valeur(Reponse r, UUID categorie, String colonne) {
        String chemin = filtre(categorie) + (colonne == null ? ".ecart" : ".valeurs." + colonne);
        List<Object> valeurs = JsonPath.read(r.corps(), chemin);
        assertThat(valeurs).as("%s dans %s", chemin, r.corps()).hasSize(1);
        return new BigDecimal(String.valueOf(valeurs.get(0)));
    }

    /** Champ du corps, {@code null} s'il est absent (Jackson peut omettre les nuls). */
    static Object optionnel(Reponse r, String chemin) {
        return JsonPath.using(Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL))
                .parse(r.corps()).read(chemin);
    }

    static Object champ(Reponse r, UUID categorie, String champ) {
        List<Object> valeurs = JsonPath.read(r.corps(), filtre(categorie) + "." + champ);
        assertThat(valeurs).as(r.corps()).hasSize(1);
        return valeurs.get(0);
    }

    /** {@code categorieId} des racines, dans l'ordre, restreints à ceux du test. */
    static List<String> racines(Reponse r, List<UUID> duTest) {
        List<String> ids = JsonPath.read(r.corps(), "$.lignes[*].categorieId");
        List<String> attendus = duTest.stream().map(UUID::toString).toList();
        return new ArrayList<>(ids.stream().filter(attendus::contains).toList());
    }
}
