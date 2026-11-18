package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.PathNotFoundException;
import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.domain.FuseauMetier;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Routes du lot N4a (FEATURE-n4-previsions-chantier.md « Endpoints »), noms du fil exacts, et réglages de fixture. */
final class PrevisionsProjetSupport {

    static final String PREVISIONS = "/api/projet/previsions";
    static final String EXPORT = PREVISIONS + "/export";
    static final String PROJET_ID = "projetId";
    static final String DROIT_ECRAN = "Yes.Prj.Front.Tdb.ProjectPrevision";

    static final String PREVISION = "prevision";
    static final String AJUSTEE = "previsionAjustee";
    static final String REEL = "reel";
    static final String TOTAL = "total";
    static final String DUE = "due";
    static final String RESTE = "reste";
    static final String AVANCEMENT = "avancement";

    private final MockMvc mockMvc;
    private final AppelsProjet appels;
    private final JdbcTemplate jdbc;
    private final EntityManager em;

    PrevisionsProjetSupport(MockMvc mockMvc, JdbcTemplate jdbc, EntityManager em) {
        this.mockMvc = mockMvc;
        this.appels = new AppelsProjet(mockMvc);
        this.jdbc = jdbc;
        this.em = em;
    }

    // ------------------------------------------------------------------ routes

    /** GET /api/projet/previsions?projetId=… */
    Reponse lire(RequestPostProcessor qui, UUID projet) throws Exception {
        return appels.lire(qui, PREVISIONS, PROJET_ID, projet.toString());
    }

    /** GET /api/projet/previsions avec des paramètres bruts (cas 400). */
    Reponse lireBrut(RequestPostProcessor qui, String... params) throws Exception {
        return appels.lire(qui, PREVISIONS, params);
    }

    /** GET /api/projet/previsions/export?projetId=… (flux asynchrone suivi jusqu'au bout). */
    MockHttpServletResponse exporter(RequestPostProcessor qui, String... params) throws Exception {
        MockHttpServletRequestBuilder requete = get(EXPORT).with(qui);
        for (int i = 0; i + 1 < params.length; i += 2) {
            requete = requete.param(params[i], params[i + 1]);
        }
        MvcResult resultat = mockMvc.perform(requete).andReturn();
        if (resultat.getRequest().isAsyncStarted()) {
            resultat = mockMvc.perform(asyncDispatch(resultat)).andReturn();
        }
        return resultat.getResponse();
    }

    MockHttpServletResponse exporter(RequestPostProcessor qui, UUID projet) throws Exception {
        return exporter(qui, PROJET_ID, projet.toString());
    }

    // ------------------------------------------------------------------ fixtures

    /** {@code PrjMarket.IncludedInPaymentPrevision} : vrai, faux ou NULL. */
    void inclure(UUID lot, Boolean inclus) {
        if (inclus == null) {
            maj("UPDATE PrjMarket SET IncludedInPaymentPrevision = NULL WHERE PKey = ?", lot.toString());
        } else {
            maj("UPDATE PrjMarket SET IncludedInPaymentPrevision = ? WHERE PKey = ?", inclus, lot.toString());
        }
    }

    /** Statut du projet (visibilité par statut d'un compte restreint, si elle s'applique aussi au projet). */
    void statutProjet(UUID projet, UUID statut) {
        maj("UPDATE PrjProject SET StrStatusId = ? WHERE PKey = ?", statut.toString(), projet.toString());
    }

    /** Droit d'écran {@code ProjectPrevision} posé sur un rôle. */
    void droitEcran(UUID role) {
        maj("INSERT INTO AdmRight (PKey, Code, Description, AdmRoleId) VALUES (?, ?, ?, ?)",
                UUID.randomUUID().toString(), DROIT_ECRAN, DROIT_ECRAN, role.toString());
    }

    private void maj(String sql, Object... parametres) {
        em.flush();
        jdbc.update(sql, parametres);
        em.clear();
    }

    // ------------------------------------------------------------------ lecture du JSON

    static String totaux(String serie, String champ) {
        return "$.totaux." + serie + "." + champ;
    }

    /** Valeur d'un chemin, {@code null} si absente ou nulle. */
    static Object valeurOuNull(Reponse r, String chemin) {
        try {
            return r.valeur(chemin);
        } catch (PathNotFoundException e) {
            return null;
        }
    }

    /** Σ des nombres d'un chemin qui rend une liste ({@code null} compte 0). */
    static BigDecimal somme(Reponse r, String chemin) {
        List<?> valeurs = (List<?>) r.valeur(chemin);
        return valeurs.stream().filter(Objects::nonNull).map(v -> new BigDecimal(String.valueOf(v)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Liste d'un chemin, en textes. */
    static List<String> textes(Reponse r, String chemin) {
        List<?> valeurs = (List<?>) r.valeur(chemin);
        return valeurs.stream().map(String::valueOf).toList();
    }

    /** {@code detail} du ProblemDetail ; vide si le corps n'en a pas. */
    static String detail(Reponse r) {
        Object d = r.corps().isBlank() ? null : valeurOuNull(r, "$.detail");
        return d == null ? "" : String.valueOf(d);
    }

    // ------------------------------------------------------------------ horloge

    /** Horloge métier réglable test par test (« aujourdhui » du spec). */
    static final class HorlogeReglable extends Clock {

        private volatile Instant instant = jour(LocalDate.of(2026, 3, 15));

        void regler(LocalDate date) {
            instant = jour(date);
        }

        private static Instant jour(LocalDate date) {
            return date.atTime(10, 0).atZone(FuseauMetier.ZONE).toInstant();
        }

        @Override
        public ZoneId getZone() {
            return FuseauMetier.ZONE;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
