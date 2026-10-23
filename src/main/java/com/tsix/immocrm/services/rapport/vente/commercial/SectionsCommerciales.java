package com.tsix.immocrm.services.rapport.vente.commercial;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.FonctionTotal;
import com.tsix.immocrm.domain.rapport.GraphiqueRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import com.tsix.immocrm.domain.rapport.TypeGraphique;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Formes de section partagées par les rapports commerciaux (FEATURE-tdb-vente-rapports-commercial.md §4, §5, §7) :
 * répartition avec regroupement « AUTRES &lt; 2% », série par période, matrice commercial × période.
 */
public final class SectionsCommerciales {

    public static final String NON_DEFINIE = "Non définie";
    public static final String AUTRES = "AUTRES < 2%";
    public static final String COMMERCIAL = "commercial";
    public static final String NOMBRE = "nombre";

    private static final String LIBELLE = "libelle";
    private static final String PART = "part";
    private static final String TOTAL = "total";
    private static final long SEUIL_AUTRES_POURCENT = 2;
    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    private SectionsCommerciales() {
    }

    /** Une ligne de matrice : nom du commercial et valeur par nom de colonne de période. */
    public record LigneCommerciale(String commercial, Map<String, Long> valeurs) {
    }

    /**
     * Effectifs par libellé triés par nombre décroissant ; les libellés sous 2 % du total (part non arrondie, comme
     * le modèle) fondus en « AUTRES &lt; 2% », en dernier : la somme est conservée. Un camembert.
     */
    public static SectionRapport repartition(String id, String titre, Map<String, Long> comptes) {
        long total = comptes.values().stream().mapToLong(Long::longValue).sum();
        List<Map<String, Object>> lignes = new ArrayList<>();
        long autres = 0;
        List<Map.Entry<String, Long>> tries = comptes.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .toList();
        for (Map.Entry<String, Long> e : tries) {
            if (e.getValue() * 100 < total * SEUIL_AUTRES_POURCENT) {
                autres += e.getValue();
            } else {
                lignes.add(ligneRepartition(e.getKey(), e.getValue(), total));
            }
        }
        if (autres > 0) {
            lignes.add(ligneRepartition(AUTRES, autres, total));
        }
        List<ColonneRapport> colonnes = List.of(ColonneRapport.texte(LIBELLE, titre),
                ColonneRapport.effectif(NOMBRE, "Nombre", null),
                new ColonneRapport(PART, "Part", TypeColonne.POURCENTAGE, null, FonctionTotal.AUCUNE, true, 1));
        return new SectionRapport(id, titre, colonnes, lignes, null, null,
                List.of(new GraphiqueRapport(TypeGraphique.SECTEURS, titre, LIBELLE, List.of(NOMBRE))));
    }

    private static Map<String, Object> ligneRepartition(String libelle, long nombre, long total) {
        Map<String, Object> ligne = new LinkedHashMap<>();
        ligne.put(LIBELLE, libelle);
        ligne.put(NOMBRE, nombre);
        ligne.put(PART, BigDecimal.valueOf(nombre).multiply(CENT)
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP));
        return ligne;
    }

    /** Une ligne par période ({@code categorie} → {@code nombre}), dans l'ordre donné, et son graphique. */
    public static SectionRapport serie(String id, String titre, String categorie, String libelleCategorie,
                                       Map<String, Long> parPeriode, TypeGraphique graphique) {
        List<Map<String, Object>> lignes = new ArrayList<>();
        parPeriode.forEach((periode, n) -> {
            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put(categorie, periode);
            ligne.put(NOMBRE, n);
            lignes.add(ligne);
        });
        return new SectionRapport(id, titre, List.of(ColonneRapport.texte(categorie, libelleCategorie),
                ColonneRapport.effectif(NOMBRE, "Nombre", null)), lignes, null, null,
                List.of(new GraphiqueRapport(graphique, titre, categorie, List.of(NOMBRE))));
    }

    /** Matrice commercial × période, colonne {@code total} en fin de ligne ; totaux SOMME calculés par le socle. */
    public static SectionRapport matrice(String id, String titre, List<ColonneRapport> periodes,
                                         List<LigneCommerciale> lignes) {
        List<ColonneRapport> colonnes = new ArrayList<>();
        colonnes.add(ColonneRapport.texte(COMMERCIAL, "Commercial"));
        colonnes.addAll(periodes);
        colonnes.add(ColonneRapport.effectif(TOTAL, "Total", null));
        List<Map<String, Object>> rangees = new ArrayList<>();
        for (LigneCommerciale l : lignes) {
            Map<String, Object> rangee = new LinkedHashMap<>();
            rangee.put(COMMERCIAL, l.commercial());
            long total = 0;
            for (ColonneRapport p : periodes) {
                long n = l.valeurs().getOrDefault(p.nom(), 0L);
                rangee.put(p.nom(), n);
                total += n;
            }
            rangee.put(TOTAL, total);
            rangees.add(rangee);
        }
        return new SectionRapport(id, titre, colonnes, rangees, null, null, List.of());
    }
}
