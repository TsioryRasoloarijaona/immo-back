package com.tsix.immocrm.domain.faisabilite;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * {@code StkFeasibilityStudy.Verif} ({@code omran-src/Yes.Rems/Yes.Stk/StkFeasibilityStudy.cs} l.2408-2494) : messages
 * OMRAN recopiés, dans l'ordre ; une écriture refuse au premier (FEATURE-patrimoine-faisabilite.md §2.5).
 */
public final class VerifFaisabilite {

    private static final String PREFIXE = "Veuillez vérifier les quantités des unités de base ; Il faut que la somme des surfaces ";
    private static final String TRANCHES = "Veuillez vérifier la ventilation des tranches!";

    /** Drapeaux de {@code SpecificFunction}, dans l'ordre de {@code VerifBaseUnits}, et la valeur de l'étude comparée. */
    public enum Surface {
        VENDABLE("vendables soit égale au plancher vendable.", ValeursFaisabilite::plancherVendable),
        BRUTE("brut soit égale au supérfécie terrain.", ValeursFaisabilite::superficie),
        NETTE("nette soit égale au total net.", ValeursFaisabilite::superficieNette),
        EMPRISE("Emprise au sol Batiments(m²) soit égale au Emprise au sol Batiments(m²) totale.",
                ValeursFaisabilite::empriseAuSol),
        COUVERTE("couverts soit égale au plancher couvert.", ValeursFaisabilite::plancherCouvert);

        private final String message;
        private final Function<ValeursFaisabilite, BigDecimal> attendu;

        Surface(String fin, Function<ValeursFaisabilite, BigDecimal> attendu) {
            this.message = PREFIXE + fin;
            this.attendu = attendu;
        }
    }

    /**
     * Unité de l'étude : {@code surfaces} vide si l'unité n'est pas une surface, sans fonction ou à fonction « total ».
     */
    public record Unite(UUID id, String code, BigDecimal quantite, Set<Surface> surfaces) {
    }

    private VerifFaisabilite() {
    }

    /** {@code poids} : un par tranche ; {@code ventile} : quantité ventilée sur toutes les tranches, par id d'unité. */
    public static List<String> erreurs(ValeursFaisabilite v, List<Unite> unites, List<BigDecimal> poids,
                                       Map<UUID, BigDecimal> ventile) {
        List<String> erreurs = new ArrayList<>();
        for (Surface s : Surface.values()) {
            List<Unite> concernees = unites.stream().filter(u -> u.surfaces().contains(s)).toList();
            BigDecimal somme = concernees.stream().map(u -> z(u.quantite())).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (!concernees.isEmpty() && differe(somme, s.attendu.apply(v))) {
                erreurs.add(s.message);
            }
        }
        if (poids.isEmpty()) {
            return erreurs;
        }
        if (poids.stream().map(VerifFaisabilite::z).reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(BigDecimal.ONE) != 0) {
            erreurs.add(TRANCHES);
        }
        unites.stream().filter(u -> differe(u.quantite(), ventile.get(u.id())))
                .forEach(u -> erreurs.add("Veuillez vérifier la ventilation de l'unité de base: '" + u.code()
                        + "' sur les tranches!"));
        return erreurs;
    }

    /** {@code Math.Round(x, 2)} de .NET : arrondi bancaire. */
    private static boolean differe(BigDecimal a, BigDecimal b) {
        return z(a).setScale(2, RoundingMode.HALF_EVEN).compareTo(z(b).setScale(2, RoundingMode.HALF_EVEN)) != 0;
    }

    private static BigDecimal z(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
