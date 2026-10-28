package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.dto.modele.MiseEnPageDto;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import org.jsoup.nodes.Element;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Blocs positionnés {@code div[data-bloc]} (FEATURE-vente-formulaire-barid.md §3.2) : centimètres depuis le coin haut gauche
 * de la page A4, bornés à la page, taille 6–72 pt, texte et champs seulement.
 */
final class BlocsPositionnes {

    static final String BLOC = "data-bloc";
    private static final String X = "data-x";
    private static final String Y = "data-y";
    private static final String L = "data-l";
    private static final String H = "data-h";
    private static final String TAILLE = "data-taille";
    private static final String GRAS = "data-gras";
    private static final String ALIGN = "data-align";
    static final Set<String> ATTRIBUTS = Set.of(BLOC, X, Y, L, H, TAILLE, GRAS, ALIGN);

    private static final BigDecimal LARGEUR_PAGE = new BigDecimal("21");
    private static final BigDecimal HAUTEUR_PAGE = new BigDecimal("29.7");
    private static final BigDecimal TAILLE_MIN = BigDecimal.valueOf(6);
    private static final BigDecimal TAILLE_MAX = BigDecimal.valueOf(72);
    private static final BigDecimal MM_PAR_CM = BigDecimal.TEN;
    private static final BigDecimal PX_PAR_PT = new BigDecimal("1.333333");
    private static final Pattern DECIMAL = Pattern.compile("\\d{1,3}(\\.\\d{1,2})?");
    private static final Set<String> ALIGNEMENTS = Set.of("left", "center", "right");
    private static final Set<String> BOOLEENS = Set.of("true", "false");
    // Hauteur non nulle : un saut de page en haut d'une page vide est ignoré, les pages suivantes se confondraient.
    private static final String RELATIF = "position: relative; min-height: 1px";

    private BlocsPositionnes() {
    }

    /** Sur un {@code div} déjà passé par la liste blanche des attributs. */
    static void verifier(Element div) {
        if (!div.hasAttr(BLOC)) {
            ATTRIBUTS.stream().filter(div::hasAttr).findFirst().ifPresent(a -> {
                throw new InvalidRequestException("%s sans data-bloc sur <div>.".formatted(a));
            });
            return;
        }
        String bloc = decrire(div);
        exiger(div.attr(BLOC).isEmpty(), "data-bloc ne prend pas de valeur : " + bloc);
        BigDecimal l = cm(div, L, bloc);
        BigDecimal h = cm(div, H, bloc);
        exiger(l.signum() > 0 && h.signum() > 0, "Bloc de largeur ou de hauteur nulle : " + bloc);
        exiger(cm(div, X, bloc).add(l).compareTo(LARGEUR_PAGE) <= 0
                        && cm(div, Y, bloc).add(h).compareTo(HAUTEUR_PAGE) <= 0,
                "Bloc hors de la page A4 : %s (x + l ≤ 21 cm, y + h ≤ 29,7 cm).".formatted(bloc));
        if (div.hasAttr(TAILLE)) {
            BigDecimal taille = nombre(div, TAILLE, bloc);
            exiger(taille.compareTo(TAILLE_MIN) >= 0 && taille.compareTo(TAILLE_MAX) <= 0,
                    "Taille de police hors de 6 à 72 pt dans le bloc " + bloc);
        }
        exiger(!div.hasAttr(GRAS) || BOOLEENS.contains(div.attr(GRAS)), "data-gras vaut true ou false : " + bloc);
        exiger(!div.hasAttr(ALIGN) || ALIGNEMENTS.contains(div.attr(ALIGN)),
                "data-align vaut left, center ou right : " + bloc);
        exiger(div.select("table, img, div").size() == 1,
                "Un bloc positionné ne contient que du texte et des champs (ni tableau, ni image, ni bloc) : " + bloc);
    }

    /** Style absolu dans la page de sa répétition ; les marges sont retranchées (origine = coin de la page). */
    static void positionner(Element bloc, MiseEnPageDto miseEnPage) {
        BigDecimal gauche = cm(bloc, X, "").subtract(mm(miseEnPage.margeGauche()));
        BigDecimal haut = cm(bloc, Y, "").subtract(mm(miseEnPage.margeHaut()));
        StringBuilder style = new StringBuilder("position: absolute; left: %scm; top: %scm; width: %scm; height: %scm;"
                .formatted(gauche.toPlainString(), haut.toPlainString(), bloc.attr(L), bloc.attr(H)));
        if (bloc.hasAttr(TAILLE)) {
            // openhtmltopdf tronque la taille au px entier (20 pt = 26,67 px → 19,5 pt) : px entiers arrondis au-dessus.
            BigDecimal px = new BigDecimal(bloc.attr(TAILLE)).multiply(PX_PAR_PT).setScale(0, RoundingMode.CEILING);
            style.append(" font-size: ").append(px.toPlainString()).append("px;");
        }
        style.append(" font-weight: ").append("true".equals(bloc.attr(GRAS)) ? "bold" : "normal").append(';');
        if (bloc.hasAttr(ALIGN)) {
            style.append(" text-align: ").append(bloc.attr(ALIGN)).append(';');
        }
        ATTRIBUTS.forEach(bloc::removeAttr);
        bloc.attr("style", style.toString());
        Element page = bloc.parent();
        String stylePage = page == null ? "" : page.attr("style");
        if (page != null && !"body".equals(page.normalName()) && !stylePage.contains(RELATIF)) {
            page.attr("style", stylePage.isEmpty() ? RELATIF : stylePage + "; " + RELATIF);
        }
    }

    private static BigDecimal mm(Integer millimetres) {
        return BigDecimal.valueOf(millimetres == null ? 0 : millimetres).divide(MM_PAR_CM);
    }

    private static BigDecimal cm(Element div, String attribut, String bloc) {
        exiger(div.hasAttr(attribut), "%s manquant dans le bloc %s".formatted(attribut, bloc));
        return nombre(div, attribut, bloc);
    }

    private static BigDecimal nombre(Element div, String attribut, String bloc) {
        String valeur = div.attr(attribut);
        exiger(DECIMAL.matcher(valeur).matches(),
                "%s invalide (%s) dans le bloc %s : nombre décimal avec un point.".formatted(attribut, valeur, bloc));
        return new BigDecimal(valeur);
    }

    private static String decrire(Element div) {
        return "x=%s y=%s l=%s h=%s".formatted(div.attr(X), div.attr(Y), div.attr(L), div.attr(H));
    }

    private static void exiger(boolean condition, String message) {
        if (!condition) {
            throw new InvalidRequestException(message);
        }
    }
}
