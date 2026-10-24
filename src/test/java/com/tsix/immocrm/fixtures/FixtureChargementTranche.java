package com.tsix.immocrm.fixtures;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * « Charger à partir d'une autre tranche » (FEATURE-patrimoine-reliquat-v2.md §A) : colonnes de {@code CfgTranche} posées et
 * relues en SQL natif, lignes de frais complètes. Les noms de colonnes viennent des listes du test, jamais d'une entrée.
 */
@Component
public class FixtureChargementTranche {

    private static final String ID = "id";

    private final EntityManager entityManager;

    public FixtureChargementTranche(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** {@code UPDATE CfgTranche SET <colonne> = <valeur>…} ; aucune valeur nulle (liaison binaire refusée par SQL Server). */
    public void definir(UUID trancheId, Map<String, Object> valeurs) {
        List<String> affectations = new ArrayList<>();
        List<Object> parametres = new ArrayList<>();
        valeurs.forEach((colonne, valeur) -> {
            affectations.add(colonne + " = :p" + parametres.size());
            parametres.add(valeur);
        });
        Query query = entityManager.createNativeQuery(
                "UPDATE CfgTranche SET " + String.join(", ", affectations) + " WHERE PKey = :id");
        for (int i = 0; i < parametres.size(); i++) {
            query.setParameter("p" + i, parametres.get(i));
        }
        executer(query.setParameter(ID, trancheId));
    }

    public void vider(UUID trancheId, String colonne) {
        executer(entityManager.createNativeQuery("UPDATE CfgTranche SET " + colonne + " = NULL WHERE PKey = :id")
                .setParameter(ID, trancheId));
    }

    /** Valeurs normalisées (décimal sans zéros de fin, GUID en majuscules), {@code null} conservé. */
    public Map<String, String> valeurs(UUID trancheId, List<String> colonnes) {
        Tuple ligne = (Tuple) lire("SELECT " + String.join(", ", colonnes) + " FROM CfgTranche WHERE PKey = :id", trancheId)
                .getSingleResult();
        Map<String, String> valeurs = new LinkedHashMap<>();
        colonnes.forEach(c -> valeurs.put(c, normaliser(ligne.get(c))));
        return valeurs;
    }

    /** Lignes d'une requête native à paramètre {@code :id}, chaque ligne aplatie en {@code v1|v2|…}. */
    @SuppressWarnings("unchecked")
    public List<String> lignes(String sql, UUID id) {
        List<Tuple> tuples = lire(sql, id).getResultList();
        return tuples.stream().map(FixtureChargementTranche::aplatir).toList();
    }

    public BigDecimal decimal(String sql, UUID id) {
        Object valeur = lire(sql, id).getSingleResult();
        return valeur instanceof Tuple t ? FixturePatrimoine.decimal(t.get(0)) : FixturePatrimoine.decimal(valeur);
    }

    public String texte(String sql, UUID id) {
        Object valeur = lire(sql, id).getSingleResult();
        Object brut = valeur instanceof Tuple t ? t.get(0) : valeur;
        return brut == null ? null : brut.toString();
    }

    public int compter(String sql, UUID id) {
        entityManager.flush();
        Object n = entityManager.createNativeQuery(sql).setParameter(ID, id).getSingleResult();
        return ((Number) n).intValue();
    }

    public UUID ligneFrais(UUID trancheId, UUID categorieId, LigneFrais l) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO CfgTrancheSettlementCategory (PKey, CfgtrancheId, ComSettlementCategoryId, Sorting,
                            AmountHT, VatRatio, AmountTTC, PercentOf, Notes)
                        VALUES (:id, :tranche, :categorie, :sorting, :ht, :tva, :ttc, :pourcentage, :notes)""")
                .setParameter(ID, id).setParameter("tranche", trancheId).setParameter("categorie", categorieId)
                .setParameter("sorting", l.sorting()).setParameter("ht", l.ht()).setParameter("tva", l.tva())
                .setParameter("ttc", l.ttc()).setParameter("pourcentage", l.pourcentageDe())
                .setParameter("notes", l.notes()));
        return id;
    }

    public record LigneFrais(int sorting, BigDecimal ht, BigDecimal tva, BigDecimal ttc, BigDecimal pourcentageDe,
                             String notes) {
    }

    /** Ligne attendue au format de {@link #lignes}. */
    public static String ligne(Object... valeurs) {
        List<String> normalisees = new ArrayList<>();
        for (Object valeur : valeurs) {
            normalisees.add(normaliser(valeur));
        }
        return String.join("|", normalisees);
    }

    private static String aplatir(Tuple tuple) {
        List<String> valeurs = new ArrayList<>();
        for (int i = 0; i < tuple.getElements().size(); i++) {
            valeurs.add(normaliser(tuple.get(i)));
        }
        return String.join("|", valeurs);
    }

    public static String normaliser(Object valeur) {
        if (valeur == null) {
            return null;
        }
        if (valeur instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return valeur.toString().toUpperCase(Locale.ROOT);
    }

    private Query lire(String sql, UUID id) {
        entityManager.flush();
        entityManager.clear();
        return entityManager.createNativeQuery(sql, Tuple.class).setParameter(ID, id);
    }

    private void executer(Query query) {
        entityManager.flush();
        query.executeUpdate();
        entityManager.clear();
    }
}
