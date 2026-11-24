package com.tsix.immocrm.controller.tresorerie;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.Reponse;
import com.tsix.immocrm.fixtures.FixtureDecaissements.Saisie;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Appels d'écriture de {@code /api/tresorerie/decaissements} (FEATURE-n3bis-saisie-reglements.md §3-§4), noms du fil
 * du spec. Le corps est une {@code Map} sérialisée à la main : une clé absente n'est pas envoyée.
 */
final class AppelsSaisieDecaissement {

    static final String DECAISSEMENTS = AppelsTresorerie.DECAISSEMENTS;
    static final String REFERENTIELS = DECAISSEMENTS + "/referentiels";
    static final String PIECES_A_AFFECTER = DECAISSEMENTS + "/pieces-a-affecter";
    static final String IN_WORK = "IN_WORK";
    static final String POINTED = "POINTED";
    static final String LIGNES = "lignes";
    static final LocalDate DATE_PIECE = LocalDate.of(2026, 4, 1);
    static final LocalDate ECHEANCE = LocalDate.of(2078, 12, 31);

    private final MockMvc mockMvc;
    private final AppelsTresorerie lecture;

    AppelsSaisieDecaissement(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
        this.lecture = new AppelsTresorerie(mockMvc);
    }

    /** Corps complet valide (module PRJ, compte de la société, statut IN_WORK), lignes données. */
    static Map<String, Object> corps(Saisie s, String reference, String montant, List<Map<String, Object>> lignes) {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("module", AppelsTresorerie.PRJ);
        corps.put("societeId", s.c().societe());
        corps.put("dateDocument", DATE_PIECE);
        corps.put("dateEcheance", ECHEANCE);
        corps.put("typeId", s.c().type());
        corps.put("reference", reference);
        corps.put("compteId", s.compte());
        corps.put("montant", new BigDecimal(montant));
        corps.put("fournisseurId", s.c().fournisseur());
        corps.put("statut", IN_WORK);
        corps.put(LIGNES, new ArrayList<>(lignes));
        return corps;
    }

    static Map<String, Object> ligne(UUID pieceId, String montant) {
        Map<String, Object> ligne = new LinkedHashMap<>();
        ligne.put("pieceId", pieceId);
        ligne.put("montant", new BigDecimal(montant));
        return ligne;
    }

    /** Ligne de retenue pilotée par le taux (fraction, T6 de FEATURE-n3bis-b2a-retenue.md). */
    static Map<String, Object> retenueTaux(UUID type, String base, String taux) {
        Map<String, Object> retenue = retenue(type, base);
        retenue.put("taux", new BigDecimal(taux));
        return retenue;
    }

    /** Ligne de retenue pilotée par le montant RS. */
    static Map<String, Object> retenueMontant(UUID type, String base, String montantRs) {
        Map<String, Object> retenue = retenue(type, base);
        retenue.put("montantRs", new BigDecimal(montantRs));
        return retenue;
    }

    private static Map<String, Object> retenue(UUID type, String base) {
        Map<String, Object> retenue = new LinkedHashMap<>();
        retenue.put("typeRetenueId", type);
        retenue.put("base", new BigDecimal(base));
        return retenue;
    }

    /** {@code POST /decaissements/{id}/<route>} sans corps (codes de feuille, RS-m / RS-n). */
    Reponse attribuer(RequestPostProcessor qui, UUID id, String route) throws Exception {
        return envoyer(post(DECAISSEMENTS + "/{id}/" + route, id).with(qui));
    }

    /** POST d'attribution avec un corps JSON (RS-o : le corps est ignoré). */
    Reponse attribuer(RequestPostProcessor qui, UUID id, String route, Map<String, Object> corps) throws Exception {
        return envoyer(post(DECAISSEMENTS + "/{id}/" + route, id).with(qui).contentType(MediaType.APPLICATION_JSON)
                .content(json(corps)));
    }

    Reponse creer(RequestPostProcessor qui, Map<String, Object> corps) throws Exception {
        return envoyer(post(DECAISSEMENTS).with(qui).contentType(MediaType.APPLICATION_JSON).content(json(corps)));
    }

    /** Création attendue en 201 ; rend l'id du règlement créé. */
    UUID creerOk(RequestPostProcessor qui, Map<String, Object> corps) throws Exception {
        Reponse r = creer(qui, corps);
        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        return UUID.fromString(JsonPath.read(r.corps(), "$.decaissement.id"));
    }

    Reponse modifier(RequestPostProcessor qui, UUID id, Map<String, Object> corps) throws Exception {
        return envoyer(put(DECAISSEMENTS + "/{id}", id).with(qui).contentType(MediaType.APPLICATION_JSON)
                .content(json(corps)));
    }

    Reponse supprimer(RequestPostProcessor qui, UUID id) throws Exception {
        return envoyer(delete(DECAISSEMENTS + "/{id}", id).with(qui));
    }

    Reponse lire(RequestPostProcessor qui, String url, String... paramsNomValeur) throws Exception {
        return lecture.lire(qui, url, paramsNomValeur);
    }

    /** Fiche relue ({@code GET /decaissements/{id}}), attendue en 200. */
    Reponse fiche(RequestPostProcessor qui, UUID id) throws Exception {
        Reponse r = lecture.lire(qui, DECAISSEMENTS + "/" + id);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r;
    }

    private Reponse envoyer(MockHttpServletRequestBuilder requete) throws Exception {
        MockHttpServletResponse r = mockMvc.perform(requete).andReturn().getResponse();
        return new Reponse(r.getStatus(), r.getContentAsString(StandardCharsets.UTF_8));
    }

    /** Détail du ProblemDetail, chaîne vide s'il n'y en a pas. */
    static String detail(Reponse r) {
        if (r.corps() == null || !r.corps().contains("\"detail\"")) {
            return "";
        }
        return JsonPath.read(r.corps(), "$.detail");
    }

    static String json(Object valeur) {
        if (valeur == null) {
            return "null";
        }
        if (valeur instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .map(e -> "\"" + e.getKey() + "\":" + json(e.getValue()))
                    .collect(Collectors.joining(",", "{", "}"));
        }
        if (valeur instanceof List<?> liste) {
            return liste.stream().map(AppelsSaisieDecaissement::json).collect(Collectors.joining(",", "[", "]"));
        }
        if (valeur instanceof Boolean || valeur instanceof BigDecimal) {
            return valeur.toString();
        }
        return "\"" + valeur.toString().replace("\"", "\\\"") + "\"";
    }
}
