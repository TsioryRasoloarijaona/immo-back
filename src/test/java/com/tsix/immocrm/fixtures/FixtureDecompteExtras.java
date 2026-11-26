package com.tsix.immocrm.fixtures;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lot LC6 (FEATURE-lc6-decompte-extras-taux.md) : travaux supplémentaires et retenues forfaitaires posés et relus en SQL,
 * corps JSON du {@code PUT /api/projet/decomptes/{id}}. Parents avant enfants (FK de la CI : retenues → décompte NO_ACTION).
 */
@Component
public class FixtureDecompteExtras {

    private static final String DECOMPTE_ID = " WHERE PrjWorkingOutId = ?";

    private final JdbcTemplate jdbc;
    private final FixtureDecomptes decomptes;

    public FixtureDecompteExtras(JdbcTemplate jdbc, FixtureDecomptes decomptes) {
        this.jdbc = jdbc;
        this.decomptes = decomptes;
    }

    /** Extra libre : code « C ordre », quantité 1, PU HT 100, TVA 0,2, FODEC 0, sans catégorie. */
    public UUID extra(UUID decompte, int ordre, String designation) {
        UUID id = UUID.randomUUID();
        decomptes.flush();
        jdbc.update("""
                        INSERT INTO PrjWorkingOutLineExtra (Pkey, Sorting, Code, Description, Unity, Quantity, UnitPriceHT, VatRatio,
                            UnitPriceTTC, FODECRatio, PrjWorkingOutId) VALUES (?, ?, ?, ?, 'U', 1, 100, 0.2, 120, 0, ?)""",
                id.toString(), ordre, "C" + ordre, designation, decompte.toString());
        return id;
    }

    /** Extra avec toutes les colonnes de saisie : code XC, notes « Note XC », FODEC 0,05, catégorie et ligne de bordereau. */
    public UUID extraComplet(UUID decompte, UUID ligneMarche, UUID categorie) {
        UUID id = UUID.randomUUID();
        decomptes.flush();
        jdbc.update("""
                        INSERT INTO PrjWorkingOutLineExtra (Pkey, Sorting, Code, Description, Unity, Quantity, UnitPriceHT, VatRatio,
                            UnitPriceTTC, FODECRatio, Notes, StlCategoryId, PrjMarketLineId, PrjWorkingOutId)
                        VALUES (?, 1, 'XC', 'Extra complet', 'M2', 2, 100, 0.2, 120, 0.05, 'Note XC', ?, ?, ?)""",
                id.toString(), categorie.toString(), ligneMarche.toString(), decompte.toString());
        return id;
    }

    /** Retenue forfaitaire ({@code PrjWorkingOutRetainedLine}). */
    public UUID retenue(UUID decompte, int ordre, String libelle, String montant) {
        UUID id = UUID.randomUUID();
        decomptes.flush();
        jdbc.update("INSERT INTO PrjWorkingOutRetainedLine (PKey, PrjWorkingOutId, Sorting, Description, Amount) VALUES (?, ?, ?, ?, ?)",
                id.toString(), decompte.toString(), ordre, libelle, new BigDecimal(montant));
        return id;
    }

    /** Extras du décompte relus en base ; UUID en minuscules (id, categorie, ligneMarche). */
    public List<Map<String, Object>> extras(UUID decompte) {
        decomptes.flush();
        return jdbc.queryForList("""
                SELECT LOWER(CAST(Pkey AS varchar(36))) AS id, Code, Description, Unity, Quantity, UnitPriceHT, VatRatio,
                    UnitPriceTTC, FODECRatio, Notes, LOWER(CAST(StlCategoryId AS varchar(36))) AS categorie,
                    LOWER(CAST(PrjMarketLineId AS varchar(36))) AS ligneMarche
                FROM PrjWorkingOutLineExtra""" + DECOMPTE_ID + " ORDER BY Sorting ASC, Description ASC", decompte.toString());
    }

    /** Retenues forfaitaires du décompte relues en base. */
    public List<Map<String, Object>> retenues(UUID decompte) {
        decomptes.flush();
        return jdbc.queryForList("SELECT LOWER(CAST(PKey AS varchar(36))) AS id, Description, Amount FROM PrjWorkingOutRetainedLine"
                + DECOMPTE_ID + " ORDER BY Sorting ASC, Description ASC", decompte.toString());
    }

    /** Valeur décimale d'une colonne lue ; NULL rendu tel quel (l'assertion échoue). */
    public static BigDecimal nombre(Map<String, Object> ligne, String colonne) {
        Object v = ligne.get(colonne);
        return v == null ? null : new BigDecimal(String.valueOf(v));
    }

    /** Montant d'une colonne lue ; NULL = 0 (formule du net). */
    public static BigDecimal montant(Map<String, Object> ligne, String colonne) {
        Object v = ligne.get(colonne);
        return v == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(v));
    }

    /** Objet JSON ordonné ; valeurs nulles admises. */
    public static Map<String, Object> champs(Object... cleValeur) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < cleValeur.length; i += 2) {
            m.put((String) cleValeur[i], cleValeur[i + 1]);
        }
        return m;
    }

    /** Corps d'Enregistrer portant la seule liste {@code cle} (« extras » ou « retenues »). */
    public static String corps(String cle, List<?> lignes) {
        return json(champs(cle, lignes));
    }

    public static String json(Object v) {
        if (v == null) {
            return "null";
        }
        if (v instanceof Map<?, ?> m) {
            return m.entrySet().stream().map(e -> "\"" + e.getKey() + "\":" + json(e.getValue()))
                    .collect(Collectors.joining(",", "{", "}"));
        }
        if (v instanceof List<?> l) {
            return l.stream().map(FixtureDecompteExtras::json).collect(Collectors.joining(",", "[", "]"));
        }
        if (v instanceof Number || v instanceof Boolean) {
            return v.toString();
        }
        return "\"" + v.toString().replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
