package com.tsix.immocrm.fixtures;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixtureTiers.Jeu;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.json.JsonMapper;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Appels de {@code /api/administration/tiers} (FEATURE-admin-tiers.md, Back) et corps valides de {@code EcritureTiersRequest}. */
public final class AppelsTiers {

    public static final String LISTE = "/api/administration/tiers/{famille}";
    public static final String FICHE = LISTE + "/{id}";
    public static final String NATURES = LISTE + "/natures";
    public static final String LIENS = "/api/administration/tiers/{id}/liens";
    public static final String LIEN = LIENS + "/{lienId}";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private AppelsTiers() {
    }

    public static MockHttpServletRequestBuilder lister(String famille, RequestPostProcessor qui) {
        return get(LISTE, famille).with(qui);
    }

    public static MockHttpServletRequestBuilder lire(String famille, UUID id, RequestPostProcessor qui) {
        return get(FICHE, famille, id).with(qui);
    }

    public static MockHttpServletRequestBuilder creer(String famille, Map<String, Object> corps, RequestPostProcessor qui) {
        return post(LISTE, famille).contentType(MediaType.APPLICATION_JSON).content(json(corps)).with(qui);
    }

    public static MockHttpServletRequestBuilder modifier(String famille, UUID id, Map<String, Object> corps,
                                                         RequestPostProcessor qui) {
        return put(FICHE, famille, id).contentType(MediaType.APPLICATION_JSON).content(json(corps)).with(qui);
    }

    public static MockHttpServletRequestBuilder supprimer(String famille, UUID id, RequestPostProcessor qui) {
        return delete(FICHE, famille, id).with(qui);
    }

    public static MockHttpServletRequestBuilder lier(UUID tiers, UUID contact, UUID relation, RequestPostProcessor qui) {
        Map<String, Object> corps = new HashMap<>();
        corps.put("contactId", contact);
        corps.put("relationTypeId", relation);
        return post(LIENS, tiers).contentType(MediaType.APPLICATION_JSON).content(json(corps)).with(qui);
    }

    public static String json(Map<String, Object> corps) {
        return JSON.writeValueAsString(corps);
    }

    /** Personne morale : tous les champs obligatoires, rien d'autre ; modifiable par le test. */
    public static Map<String, Object> corpsMorale(Jeu j, String raisonSociale) {
        Map<String, Object> corps = communs(j);
        corps.put("personneMorale", true);
        corps.put("raisonSociale", raisonSociale);
        corps.put("formeSocialeId", j.formeMorale());
        return corps;
    }

    public static Map<String, Object> corpsPhysique(Jeu j, String prenom, String nom) {
        Map<String, Object> corps = communs(j);
        corps.put("personneMorale", false);
        corps.put("prenom", prenom);
        corps.put("nom", nom);
        corps.put("titreId", j.titre());
        corps.put("sexeId", j.sexe());
        corps.put("formeSocialeId", j.formePhysique());
        return corps;
    }

    private static Map<String, Object> communs(Jeu j) {
        Map<String, Object> corps = new HashMap<>();
        corps.put("paysResidenceId", j.pays());
        corps.put("deviseId", j.devise());
        corps.put("mobile", "0612345678");
        return corps;
    }

    public static UUID idDe(String fiche) {
        return UUID.fromString(JsonPath.read(fiche, "$.id"));
    }

    public static List<String> ids(String page) {
        return JsonPath.read(page, "$.content[*].id");
    }

    /** Sans casse, accents ni espaces : « Raison sociale » nomme {@code raisonSociale}. */
    public static String normalise(String texte) {
        return Normalizer.normalize(texte, Normalizer.Form.NFD).replaceAll("\\p{M}", "").replace(" ", "")
                .toLowerCase(Locale.ROOT);
    }

    public static String detailNormalise(String probleme) {
        String detail = JsonPath.read(probleme, "$.detail");
        return normalise(detail);
    }
}
