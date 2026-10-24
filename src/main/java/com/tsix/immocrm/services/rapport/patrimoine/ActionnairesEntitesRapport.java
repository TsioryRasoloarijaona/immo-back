package com.tsix.immocrm.services.rapport.patrimoine;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.Rapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * ACTIONNAIRES — ENTITÉS JURIDIQUES (FEATURE-patrimoine-rapports.md §5) : tableau croisé actionnaire × entité active
 * (Q7, sans paramètre) ; une part sur une entité inactive n'apparaît nulle part. Tous les actionnaires, même sans part.
 */
@Component
public class ActionnairesEntitesRapport implements Rapport {

    static final String CODE = "StkTdbCompanyShareholder";

    private static final String ACTIONNAIRE = "actionnaire";
    private static final String TOTAL = "total";

    private static final String PARTS = """
            SELECT sh.PKey AS ActionnaireId, sh.Description AS Actionnaire, cs.CfgCompanyId AS EntiteId,
                   ISNULL(cs.SharingNumber, 0) AS Parts
            FROM CfgShareholder sh
            LEFT JOIN CfgCompanyShareholder cs ON cs.CfgShareholderId = sh.PKey
            ORDER BY sh.Description, sh.PKey""";

    private final NamedParameterJdbcTemplate jdbc;
    private final EntitesActives entites;

    public ActionnairesEntitesRapport(NamedParameterJdbcTemplate jdbc, EntitesActives entites) {
        this.jdbc = jdbc;
        this.entites = entites;
    }

    record Part(String actionnaireId, String actionnaire, String entiteId, long parts) {
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public ModuleRapport module() {
        return ModuleRapport.PATRIMOINE;
    }

    @Override
    public List<ParametreRapport> parametres() {
        return List.of();
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        List<Part> parts = jdbc.query(PARTS, (rs, n) -> {
            String entite = rs.getString("EntiteId");
            return new Part(rs.getString("ActionnaireId"), rs.getString("Actionnaire"),
                    entite == null ? null : UUID.fromString(entite).toString(), rs.getLong("Parts"));
        });
        Set<String> avecParts = new HashSet<>();
        parts.forEach(p -> avecParts.add(p.entiteId()));
        // Colonnes : entités actives ayant au moins une ligne, dans l'ordre des libellés ; nommées entite1, entite2…
        Map<String, String> colonneParEntite = new LinkedHashMap<>();
        List<ColonneRapport> colonnes = new ArrayList<>(List.of(ColonneRapport.texte(ACTIONNAIRE, "Actionnaire")));
        for (ParametreRapport.Option entite : entites.lire()) {
            if (avecParts.contains(entite.valeur())) {
                String nom = "entite" + (colonneParEntite.size() + 1);
                colonneParEntite.put(entite.valeur(), nom);
                colonnes.add(ColonneRapport.effectif(nom, entite.libelle(), null));
            }
        }
        colonnes.add(ColonneRapport.effectif(TOTAL, "Total", null));
        return new ResultatRapport(List.of(new SectionRapport("repartition", "Répartition des parts", colonnes,
                lignes(parts, colonneParEntite), null, null, null)), List.of());
    }

    private static List<Map<String, Object>> lignes(List<Part> parts, Map<String, String> colonneParEntite) {
        Map<String, Map<String, Object>> parActionnaire = new LinkedHashMap<>();
        Map<String, Long> totaux = new HashMap<>();
        for (Part p : parts) {
            Map<String, Object> ligne = parActionnaire.computeIfAbsent(p.actionnaireId(), id -> {
                Map<String, Object> nouvelle = ColonnesStock.ligne(ACTIONNAIRE, p.actionnaire());
                colonneParEntite.values().forEach(nom -> nouvelle.put(nom, 0L));
                return nouvelle;
            });
            String colonne = p.entiteId() == null ? null : colonneParEntite.get(p.entiteId());
            if (colonne != null) {
                ligne.merge(colonne, p.parts(), (a, b) -> (Long) a + (Long) b);
                totaux.merge(p.actionnaireId(), p.parts(), Long::sum);
            }
        }
        parActionnaire.forEach((id, ligne) -> ligne.put(TOTAL, totaux.getOrDefault(id, 0L)));
        return List.copyOf(parActionnaire.values());
    }
}
