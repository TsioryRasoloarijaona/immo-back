package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.function.Executable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/** Lot A1 bis (FEATURE-achat-bc-agregats.md) : lecture de l'objet {@code agregats} des lignes et des totaux. */
final class AppelsAgregats {

    static final String AGREGATS = "agregats";

    /** Champs monétaires du tableau « Les colonnes OMRAN et leur source » (ratios et codes exclus). */
    static final List<String> MONTANTS = List.of("factureHT", "factureTVA", "factureTTC", "factureNetAPayer",
            "livreHT", "livreTTC", "livreNetAPayer", "resteALivrerHT", "resteALivrerTTC", "resteALivrerNetAPayer",
            "avance", "avanceAmortie", "avanceRegle", "avanceReste", "avanceResteAAmortir", "garantieRetenue",
            "garantieRestituee", "garantieReste", "regle", "resteAPayerSurFacture", "resteAPayerTotal");

    static final List<String> RATIOS_ET_CODES = List.of("avanceTauxPrevu", "avanceTauxEffectif", "bonsLivraisonCodes",
            "facturesCodes", "devisCode");

    private AppelsAgregats() {
    }

    /** {@code agregats.<champ>} de la ligne {@code code} (l'objet doit exister sur la ligne). */
    static Object agregat(String json, String code, String champ) {
        return AppelsAchat.champ(json, code, AGREGATS + "." + champ);
    }

    static BigDecimal montantAgregat(String json, String code, String champ) {
        return new BigDecimal(String.valueOf(agregat(json, code, champ)));
    }

    /** Vérifie {@code paires} = champ, valeur attendue (comparée en BigDecimal) sur la ligne {@code code}. */
    static void attendus(String json, String code, String... paires) {
        List<Executable> verifs = new ArrayList<>();
        for (int i = 0; i + 1 < paires.length; i += 2) {
            String champ = paires[i];
            String valeur = paires[i + 1];
            verifs.add(() -> assertThat(montantAgregat(json, code, champ)).as("%s.%s", code, champ)
                    .isEqualByComparingTo(valeur));
        }
        assertAll(verifs);
    }

    /** {@code agregats} d'une page de liste, lignes sans objet (non-BC) exclues. */
    static List<Map<String, Object>> agregatsDeLaPage(String json) {
        List<Map<String, Object>> tous = JsonPath.read(json, "$.content[*]." + AGREGATS);
        return tous.stream().filter(Objects::nonNull).toList();
    }

    /** Codes d'une liste « a, b » sans espaces ni vides ; vide si {@code null}. */
    static List<String> decouper(Object codes) {
        if (codes == null) {
            return List.of();
        }
        return Arrays.stream(String.valueOf(codes).split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
