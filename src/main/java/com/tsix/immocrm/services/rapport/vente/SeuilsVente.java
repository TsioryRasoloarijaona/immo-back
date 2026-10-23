package com.tsix.immocrm.services.rapport.vente;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Seuils de statut de vente des modèles OMRAN : paramètre {@code AdmParamsGuid} → {@code StrStatus.Sorting}
 * (FEATURE-tdb-vente-rapports-commercial.md §3). Un paramètre absent donne un seuil absent, jamais une exception.
 */
@Component
class SeuilsVente {

    static final String CONCRETISATION = "DefaultComFolderStatusConcretisation";
    static final String FINALISATION = "DefaultComFolderStatusFinalisation";
    static final String LIVRAISON = "DefaultComFolderStatusDelevery";

    // SQL natif : relu à chaque appel (une entité AdmParamsGuid resterait dans le contexte de persistance).
    private static final String SEUILS = """
            SELECT p.Code, s.Sorting FROM AdmParamsGuid p JOIN StrStatus s ON s.PKey = p.CodeValue
            WHERE p.Code IN (:codes) AND s.Sorting IS NOT NULL""";

    private final NamedParameterJdbcTemplate jdbc;

    SeuilsVente(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** {@code Sorting} du statut désigné par chaque paramètre trouvé ; un paramètre absent n'a pas d'entrée. */
    Map<String, Integer> lire() {
        Map<String, Integer> seuils = new HashMap<>();
        jdbc.query(SEUILS, Map.of("codes", List.of(CONCRETISATION, FINALISATION, LIVRAISON)),
                (rs, n) -> Map.entry(rs.getString("Code"), rs.getInt("Sorting")))
                .forEach(e -> seuils.put(e.getKey(), e.getValue()));
        return seuils;
    }

    static String absent(String parametre, String section) {
        return "Paramètre %s absent (AdmParamsGuid) : section « %s » vide.".formatted(parametre, section);
    }
}
