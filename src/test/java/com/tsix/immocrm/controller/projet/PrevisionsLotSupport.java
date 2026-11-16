package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Routes du lot L3a-bis (FEATURE-lot-previsions.md §4), noms du fil exacts. */
final class PrevisionsLotSupport {

    static final String DEBUT = "debut";
    static final String DUREE = "duree";
    static final String DEBUT_BASE = "debutBase";
    static final String DUREE_BASE = "dureeBase";
    static final String ESTIMATION_BASE_HT = "estimationBaseHT";
    static final String POINTS = "points";
    static final String POINTS_BASE = "pointsBase";
    static final String DATE = "date";
    static final String AVANCEMENT = "avancement";
    static final String MODIFIABLE = "$.modifiable";
    static final String MOIS = "$.mois[*].mois";

    /** Planning des fixtures : 01/01/2026 + 90 jours = 01/04/2026, pour les deux onglets. */
    static final LocalDate DEBUT_2026 = LocalDate.of(2026, 1, 1);
    static final int DUREE_90 = 90;
    static final LocalDate FIN_2026 = DEBUT_2026.plusDays(DUREE_90);

    private final MockMvc mockMvc;
    private final AppelsProjet appels;

    PrevisionsLotSupport(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
        this.appels = new AppelsProjet(mockMvc);
    }

    /** GET /lots/{id}/previsions (graphe). */
    Reponse graphe(RequestPostProcessor qui, UUID lot) throws Exception {
        return appels.lire(qui, AppelsProjet.LOTS + "/" + lot + "/previsions");
    }

    /** GET /lots/{id}/previsions/saisie (modale). */
    Reponse saisie(RequestPostProcessor qui, UUID lot) throws Exception {
        return appels.lire(qui, AppelsProjet.LOTS + "/" + lot + "/previsions/saisie");
    }

    /** PUT /lots/{id}/previsions. */
    Reponse enregistrer(RequestPostProcessor qui, UUID lot, CorpsJson corps) throws Exception {
        MockHttpServletResponse r = mockMvc.perform(put(AppelsProjet.LOTS + "/" + lot + "/previsions").with(qui)
                .contentType(MediaType.APPLICATION_JSON).content(corps.json())).andReturn().getResponse();
        return new Reponse(r.getStatus(), r.getContentAsString(StandardCharsets.UTF_8));
    }

    /** Corps qui reprend le planning des fixtures (base identique : 01/01/2026, 90 j, HT 1000), sans point. */
    static CorpsJson inchange() {
        return saisie(DEBUT_2026, DUREE_90, DEBUT_2026, DUREE_90, "1000", List.of(), List.of());
    }

    static CorpsJson saisie(LocalDate debut, Integer duree, LocalDate debutBase, Integer dureeBase, String ht,
                            List<CorpsJson> points, List<CorpsJson> pointsBase) {
        return corps().avec(DEBUT, debut).avec(DUREE, duree).avec(DEBUT_BASE, debutBase).avec(DUREE_BASE, dureeBase)
                .avec(ESTIMATION_BASE_HT, ht == null ? null : new BigDecimal(ht)).avec(POINTS, points)
                .avec(POINTS_BASE, pointsBase);
    }

    static CorpsJson point(LocalDate date, String avancement) {
        return corps().avec(DATE, date).avec(AVANCEMENT, new BigDecimal(avancement));
    }

    /** Valeur d'une ligne lue en JDBC, en {@code BigDecimal} ({@code null} conservé). */
    static BigDecimal nombre(Map<String, Object> ligne, String colonne) {
        Object v = ligne.get(colonne);
        return v == null ? null : new BigDecimal(String.valueOf(v));
    }

    static String texte(Map<String, Object> ligne, String colonne) {
        return String.valueOf(ligne.get(colonne));
    }

    /** Σ d'une colonne sur des lignes JDBC (les {@code null} comptent 0). */
    static BigDecimal somme(List<Map<String, Object>> lignes, String colonne) {
        return lignes.stream().map(l -> nombre(l, colonne)).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
