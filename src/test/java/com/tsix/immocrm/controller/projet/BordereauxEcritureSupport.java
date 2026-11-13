package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Appels et corps des écritures de bordereau du lot L3b-1 (FEATURE-lot-ecritures.md §4), noms du fil exacts. */
final class BordereauxEcritureSupport {

    static final String ESTIMATIF = "bordereau-estimatif";
    static final String DEFINITIF = "bordereau";

    static final String ID = "id";
    static final String GROUPE_ID = "groupeId";
    static final String PARENT_ID = "parentId";
    static final String ORDRE = "ordre";
    static final String CODE = "code";
    static final String DESIGNATION = "designation";
    static final String DESCRIPTION = "description";
    static final String UNITE = "unite";
    static final String QUANTITE = "quantite";
    static final String PU_HT = "prixUnitaireHT";
    static final String TVA = "tauxTVA";
    static final String FODEC = "tauxFODEC";
    static final String CATEGORIE = "categorieId";
    static final String PONDERATION = "ponderation";
    static final String NOTES = "notes";
    static final String GROUPES = "groupes";
    static final String LIGNES = "lignes";

    private final MockMvc mockMvc;
    private final AppelsProjet appels;

    BordereauxEcritureSupport(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
        this.appels = new AppelsProjet(mockMvc);
    }

    /** PUT /lots/{id}/{route} ; {@code route} = {@link #ESTIMATIF} ou {@link #DEFINITIF}. */
    Reponse ecrire(RequestPostProcessor qui, UUID lot, String route, CorpsJson corps) throws Exception {
        MockHttpServletResponse r = mockMvc.perform(put(AppelsProjet.LOTS + "/" + lot + "/" + route).with(qui)
                .contentType(MediaType.APPLICATION_JSON).content(corps.json())).andReturn().getResponse();
        return new Reponse(r.getStatus(), r.getContentAsString(StandardCharsets.UTF_8));
    }

    /** GET /lots/{id}/{route}. */
    Reponse lire(RequestPostProcessor qui, UUID lot, String route) throws Exception {
        return appels.lire(qui, AppelsProjet.LOTS + "/" + lot + "/" + route);
    }

    /** {@code detail} du ProblemDetail, null si le corps n'en a pas (route absente : corps vide). */
    static String detail(Reponse r) {
        if (r.corps() == null || r.corps().isBlank()) {
            return null;
        }
        try {
            return JsonPath.read(r.corps(), "$.detail");
        } catch (JsonPathException e) {
            return null;
        }
    }

    static CorpsJson bordereau(List<CorpsJson> groupes, List<CorpsJson> lignes) {
        return corps().avec(GROUPES, groupes).avec(LIGNES, lignes);
    }

    static CorpsJson vide() {
        return bordereau(List.of(), List.of());
    }

    /** Groupe saisi : description « GROUPE code », sans notes. */
    static CorpsJson groupe(UUID id, UUID parent, int ordre, String code) {
        return corps().avec(ID, id).avec(PARENT_ID, parent).avec(ORDRE, ordre).avec(CODE, code)
                .avec(DESCRIPTION, "GROUPE " + code).avec(NOTES, null);
    }

    /** Ligne saisie : désignation « ART code », unité U, TVA 20 %, FODEC 0, sans pondération ni notes. */
    static CorpsJson ligne(UUID id, UUID groupe, int ordre, String code, String quantite, String puHT, UUID categorie) {
        return corps().avec(ID, id).avec(GROUPE_ID, groupe).avec(ORDRE, ordre).avec(CODE, code)
                .avec(DESIGNATION, "ART " + code).avec(UNITE, "U").avec(QUANTITE, new BigDecimal(quantite))
                .avec(PU_HT, new BigDecimal(puHT)).avec(TVA, new BigDecimal("20")).avec(FODEC, BigDecimal.ZERO)
                .avec(CATEGORIE, categorie).avec(PONDERATION, null).avec(NOTES, null);
    }

    static UUID guid(Object valeur) {
        return valeur == null ? null : UUID.fromString(String.valueOf(valeur));
    }
}
