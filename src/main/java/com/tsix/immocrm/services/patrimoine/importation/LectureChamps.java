package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.ChampImport;
import com.tsix.immocrm.services.patrimoine.importation.LectureClasseur.LigneLue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Cellules d'une ligne → valeurs des champs d'une table d'en-têtes ({@link ChampImport}), codes résolus en ids. */
final class LectureChamps {

    private LectureChamps() {
    }

    /** Valeurs non vides par rang ({@link ChampImport#rang()}) ; cellule illisible ou code inconnu → {@code erreurs}. */
    static Map<Integer, Map<String, Object>> lire(List<ChampImport> champs, LigneLue l, ResolutionCodes codes,
                                                  List<String> avertissements, List<String> erreurs) {
        Map<Integer, Map<String, Object>> parRang = new TreeMap<>();
        for (ChampImport c : champs) {
            Object brut = l.valeurs().get(c.entete());
            if (brut != null && c.champ() != null) {
                try {
                    Object valeur = valeur(c, brut, codes, avertissements, l.numero());
                    parRang.computeIfAbsent(c.rang(), n -> new HashMap<>()).put(c.champ(), valeur);
                } catch (TypeCellule.Illisible e) {
                    erreurs.add(illisible(c.entete(), brut));
                } catch (InvalidRequestException e) {
                    erreurs.add(e.getMessage());
                }
            }
        }
        return parRang;
    }

    static String illisible(String entete, Object brut) {
        return "Colonne %s : valeur %s illisible".formatted(entete, TypeCellule.texte(brut));
    }

    static String introuvable(String code) {
        return "Le code " + code + " saisi est introuvable dans notre référenciel";
    }

    private static Object valeur(ChampImport c, Object brut, ResolutionCodes codes, List<String> avertissements,
                                 int numero) {
        Object valeur = c.type().lire(brut);
        if (c.reference() != null) {
            String code = valeur.toString();
            return codes.trouver(c.reference().classe(), code, c.reference().parLibelle())
                    .orElseThrow(() -> new InvalidRequestException(introuvable(code))).getId();
        }
        if (c.type() == TypeCellule.TVA && TypeCellule.NOMBRE.lire(brut) instanceof BigDecimal saisi
                && saisi.compareTo(BigDecimal.ONE) > 0) {
            avertissements.add("Ligne %d : %s %s lue comme un pourcentage (%s)."
                    .formatted(numero, c.entete(), saisi.toPlainString(), ((BigDecimal) valeur).toPlainString()));
        }
        return valeur;
    }

    /** Nature lue par code ou libellé ; seule {@code attendue} s'importe. Rend vrai si la cellule est renseignée. */
    static boolean controlerNature(Object brut, ResolutionCodes codes, String attendue, String libelle,
                                   List<String> erreurs) {
        if (brut == null) {
            return false;
        }
        String nature = TypeCellule.texte(brut);
        codes.trouver(StkKind.class, nature, true).ifPresentOrElse(
                k -> {
                    if (!attendue.equalsIgnoreCase(k.getCode())) {
                        erreurs.add("La nature %s n'est pas %s : seuls les %ss s'importent ici."
                                .formatted(nature, libelle, libelle.toLowerCase(Locale.ROOT)));
                    }
                },
                () -> erreurs.add(introuvable(nature)));
        return true;
    }

    static String texte(Object brut) {
        return brut == null ? null : TypeCellule.texte(brut);
    }
}
