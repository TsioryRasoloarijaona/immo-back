package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureModelesSimulation.Ligne;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/** Routes du lot L3a-ter (FEATURE-lot-previsions-reel.md §3), noms du fil exacts. */
final class PrevisionsReelSupport {

    static final String MODELES = "/api/projet/modeles-simulation";
    static final String MODE = "mode";
    static final String DATE_FIN = "dateFin";
    static final String INCHANGEE = "INCHANGEE";
    static final String FIXEE = "FIXEE";
    static final String SYSTEME = "SYSTEME";
    static final String CODE = "code";
    static final String DESCRIPTION = "description";
    static final String CATEGORIE_ID = "categorieId";
    static final String LIGNES = "lignes";
    static final String DETAIL = "$.detail";

    private final MockMvc mockMvc;
    private final AppelsProjet appels;

    PrevisionsReelSupport(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
        this.appels = new AppelsProjet(mockMvc);
    }

    private static String previsions(UUID lot) {
        return AppelsProjet.LOTS + "/" + lot + "/previsions";
    }

    // ------------------------------------------------------------------ lot

    /** GET /lots/{id}/previsions/alignement. */
    Reponse ecarts(RequestPostProcessor qui, UUID lot) throws Exception {
        return appels.lire(qui, previsions(lot) + "/alignement");
    }

    /** POST /lots/{id}/previsions/alignement. */
    Reponse aligner(RequestPostProcessor qui, UUID lot, CorpsJson corps) throws Exception {
        return envoyer(HttpMethod.POST, previsions(lot) + "/alignement", qui, corps);
    }

    /** POST /lots/{id}/previsions/appliquer-modele/{modeleId}. */
    Reponse appliquer(RequestPostProcessor qui, UUID lot, UUID modele) throws Exception {
        return envoyer(HttpMethod.POST, previsions(lot) + "/appliquer-modele/" + modele, qui, null);
    }

    /** GET /lots/{id}/previsions/modele-propose. */
    Reponse modelePropose(RequestPostProcessor qui, UUID lot) throws Exception {
        return appels.lire(qui, previsions(lot) + "/modele-propose");
    }

    /** Fiche du lot : même message 404 que les routes du lot. */
    Reponse fiche(RequestPostProcessor qui, UUID lot) throws Exception {
        return appels.lire(qui, AppelsProjet.LOTS + "/" + lot);
    }

    static CorpsJson mode(String mode, LocalDate dateFin) {
        return corps().avec(MODE, mode).avec(DATE_FIN, dateFin);
    }

    // ------------------------------------------------------------------ modèles

    Reponse modeles(RequestPostProcessor qui, String... params) throws Exception {
        return appels.lire(qui, MODELES, params);
    }

    Reponse modele(RequestPostProcessor qui, UUID id) throws Exception {
        return appels.lire(qui, MODELES + "/" + id);
    }

    Reponse creer(RequestPostProcessor qui, CorpsJson corps) throws Exception {
        return envoyer(HttpMethod.POST, MODELES, qui, corps);
    }

    Reponse modifier(RequestPostProcessor qui, UUID id, CorpsJson corps) throws Exception {
        return envoyer(HttpMethod.PUT, MODELES + "/" + id, qui, corps);
    }

    Reponse supprimer(RequestPostProcessor qui, UUID id) throws Exception {
        return envoyer(HttpMethod.DELETE, MODELES + "/" + id, qui, null);
    }

    /** Corps {@code ModeleSimulationSaisieDto}. */
    static CorpsJson saisieModele(String code, String description, UUID categorie, Ligne... lignes) {
        List<CorpsJson> l = Arrays.stream(lignes).map(PrevisionsReelSupport::ligne).toList();
        return corps().avec(CODE, code).avec(DESCRIPTION, description).avec(CATEGORIE_ID, categorie).avec(LIGNES, l);
    }

    static CorpsJson ligne(Ligne l) {
        return corps().avec("temps", new BigDecimal(l.temps())).avec("avancement", new BigDecimal(l.avancement()))
                .avec("paiement", new BigDecimal(l.paiement()));
    }

    /** {@code detail} du ProblemDetail ; vide si le corps n'en a pas (route absente avant le code). */
    static String detail(Reponse r) {
        try {
            return r.corps().isBlank() ? "" : String.valueOf(r.valeur(DETAIL));
        } catch (RuntimeException e) {
            return "";
        }
    }

    private Reponse envoyer(HttpMethod methode, String url, RequestPostProcessor qui, CorpsJson corps) throws Exception {
        MockHttpServletRequestBuilder requete = request(methode, url).with(qui);
        if (corps != null) {
            requete = requete.contentType(MediaType.APPLICATION_JSON).content(corps.json());
        }
        MockHttpServletResponse r = mockMvc.perform(requete).andReturn().getResponse();
        return new Reponse(r.getStatus(), r.getContentAsString(StandardCharsets.UTF_8));
    }
}
