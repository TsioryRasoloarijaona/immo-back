package com.tsix.immocrm.domain;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Marqueur de tri posé à la main par l'équipe commerciale dans {@code ComProspection.TempField1}.
 *
 * <p><b>La colonne n'est pas une énumération</b>, et c'est tout l'intérêt de cette classe. Sur les
 * 3 817 pré-prospections réelles, {@code TempField1} sert de marqueur de tri (« Doublon » 770,
 * « spam » 30, « Traité » 9). Sur les 14 118 prospections, la <em>même</em> colonne sert à noter
 * la typologie recherchée — {@code salon_+_2_chambres} (518 lignes), {@code salon_+_1_chambre}
 * (213) — ainsi que des villes et des budgets. 1 239 lignes au total portent du texte libre qui
 * n'est pas un marqueur.
 *
 * <p>D'où le contrat : {@link #depuis(String)} rend {@code null} dès que le contenu n'est pas un
 * marqueur reconnu. Le texte brut reste servi à part
 * ({@code ProspectionDetailDto.noteQualification}), donc rien n'est perdu.
 *
 * <p><b>Correspondance exacte, jamais par préfixe.</b> Une ligne réelle vaut
 * {@code "doublon tribeca/bel air"} : la reconnaître comme {@link #DOUBLON} reviendrait à décider
 * à la place de l'utilisateur ce qu'il a voulu dire. Elle ressort donc non reconnue, avec son
 * texte intact.
 *
 * <p>La normalisation, elle, est nécessaire : la casse varie (« Doublon » / « doublon »), les
 * accents aussi (« Traité » / « traite »), et une faute de frappe est suffisamment répandue pour
 * être traitée comme une variante (« doublan », 2 lignes).
 *
 * <p>Voir {@code instructions/FEATURE-marqueurs-tri-pre-prospection.md} §2.
 */
public enum MarqueurTriProspection {

    /** Lead en doublon d'un tiers existant. 921 lignes (770 pré-prospections + 151 prospections). */
    DOUBLON("doublon", "doublan"),

    /** Lead identifié comme indésirable. 32 lignes. */
    SPAM("spam"),

    /** Lead déjà traité par ailleurs. 10 lignes. */
    TRAITE("traite");

    private final String[] variantes;

    MarqueurTriProspection(String... variantes) {
        this.variantes = variantes;
    }

    /**
     * Le marqueur correspondant au contenu brut de {@code TempField1}, ou {@code null} si ce
     * contenu n'en est pas un — cas majoritaire, voir la javadoc de classe.
     */
    public static MarqueurTriProspection depuis(String valeurBrute) {
        String normalisee = normaliser(valeurBrute);
        if (normalisee.isEmpty()) {
            return null;
        }
        for (MarqueurTriProspection marqueur : values()) {
            for (String variante : marqueur.variantes) {
                if (variante.equals(normalisee)) {
                    return marqueur;
                }
            }
        }
        return null;
    }

    /**
     * Minuscules, sans accent, sans espaces de bordure.
     *
     * <p>{@code Locale.ROOT} et non la locale par défaut : en turc, {@code "SPAM".toLowerCase()}
     * rend {@code "spam"} mais {@code "I".toLowerCase()} rend {@code "ı"}. Une correspondance de
     * référentiel ne doit pas dépendre de la locale de la JVM.
     */
    private static String normaliser(String valeur) {
        if (valeur == null) {
            return "";
        }
        String sansAccent = Normalizer.normalize(valeur.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sansAccent.toLowerCase(Locale.ROOT);
    }
}
