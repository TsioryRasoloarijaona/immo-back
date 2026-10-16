package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.domain.TypeChamp;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;

import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Map.entry;

/**
 * Liste blanche du HTML restreint des modèles (FEATURE-modeles-documents.md §5) : tout ce qui n'y figure pas → 400 en
 * nommant l'élément fautif. Appliquée à l'enregistrement, à l'aperçu et de nouveau à chaque génération.
 */
public final class ContenuModele {

    static final String CHAMP = "data-champ";
    static final String REPETER = "data-repeter";
    static final String SI = "data-si";
    static final String VALEUR = "data-valeur";
    static final String NUMERO = "data-numero";
    static final String SAUT_PAGE = "data-saut-page";

    static final int IMAGE_MAX_OCTETS = 500 * 1024;
    static final int IMAGES_VERSION_MAX_OCTETS = 2 * 1024 * 1024;
    private static final int CONTENU_MAX_CARACTERES = 4 * 1024 * 1024;

    private static final String STYLE = "style";
    private static final String COLSPAN = "colspan";
    private static final String ROWSPAN = "rowspan";
    private static final String START = "start";
    private static final Set<String> BLOC_CONDITIONNEL = Set.of(STYLE, REPETER, SI, VALEUR);
    private static final Set<String> CELLULE = Set.of(STYLE, COLSPAN, ROWSPAN);
    private static final Map<String, Set<String>> ATTRIBUTS = Map.ofEntries(
            entry("p", BLOC_CONDITIONNEL),
            entry("li", BLOC_CONDITIONNEL),
            entry("h1", Set.of(STYLE)),
            entry("h2", Set.of(STYLE, NUMERO)),
            entry("h3", Set.of(STYLE)),
            entry("div", Set.of(STYLE, SI, VALEUR, SAUT_PAGE)),
            entry("span", Set.of(CHAMP, REPETER, SI, VALEUR)),
            entry("tr", Set.of(REPETER)),
            entry("td", CELLULE),
            entry("th", CELLULE),
            entry("img", Set.of("src", "alt")),
            entry("ol", Set.of(START)),
            entry("ul", Set.of()), entry("strong", Set.of()), entry("em", Set.of()), entry("u", Set.of()),
            entry("br", Set.of()), entry("hr", Set.of()), entry("table", Set.of()), entry("thead", Set.of()),
            entry("tbody", Set.of()));
    private static final Set<String> CELLULES = Set.of("td", "th");
    private static final Set<String> ALIGNEMENTS = Set.of("left", "center", "right", "justify");
    private static final Map<String, byte[]> IMAGES = Map.of(
            "data:image/png;base64,", new byte[]{(byte) 0x89, 'P', 'N', 'G'},
            "data:image/jpeg;base64,", new byte[]{(byte) 0xFF, (byte) 0xD8});

    private final DescriptionChamps catalogue;
    private long octetsImages;

    private ContenuModele(DescriptionChamps catalogue) {
        this.catalogue = catalogue;
    }

    /** Résultat : le corps analysé (à cloner avant de le modifier) et le poids de ses images. */
    public record Analyse(Element corps, long octetsImages) {
    }

    public static Analyse analyser(String html, DescriptionChamps catalogue) {
        if (html == null) {
            throw new InvalidRequestException("Contenu absent.");
        }
        if (html.length() > CONTENU_MAX_CARACTERES) {
            throw new InvalidRequestException("Contenu trop volumineux (4 Mo de texte maximum).");
        }
        Element corps = Jsoup.parseBodyFragment(html).body();
        // Un <body> ou <html> dans le fragment fusionne ses attributs dans ces éléments, jamais parcourus ensuite.
        Element racine = corps.parent();
        if (!corps.attributes().asList().isEmpty() || (racine != null && !racine.attributes().asList().isEmpty())) {
            throw new InvalidRequestException("Élément interdit : <body> ou <html> dans le contenu.");
        }
        ContenuModele verification = new ContenuModele(catalogue);
        corps.children().forEach(enfant -> verification.verifier(enfant, null));
        return new Analyse(corps, verification.octetsImages);
    }

    /** Poids cumulé des images d'une version (contenu, en-tête, pied) : 2 Mo au plus. */
    public static void verifierPoidsVersion(long octets) {
        if (octets > IMAGES_VERSION_MAX_OCTETS) {
            throw new InvalidRequestException("Images trop lourdes pour un modèle : %d Ko (2 Mo maximum)."
                    .formatted(octets / 1024));
        }
    }

    private void verifier(Element element, String liste) {
        String balise = element.normalName();
        Set<String> permis = ATTRIBUTS.get(balise);
        if (permis == null) {
            throw new InvalidRequestException("Élément interdit : <%s>".formatted(balise));
        }
        for (Attribute attribut : element.attributes()) {
            if (!permis.contains(attribut.getKey())) {
                throw new InvalidRequestException("Attribut interdit : %s sur <%s>".formatted(attribut.getKey(), balise));
            }
            verifierValeur(balise, attribut);
        }
        String listeEnfants = verifierRepetition(element, liste);
        verifierChamp(element, listeEnfants);
        verifierCondition(element, listeEnfants);
        element.children().forEach(enfant -> verifier(enfant, listeEnfants));
    }

    private void verifierValeur(String balise, Attribute attribut) {
        String valeur = attribut.getValue();
        switch (attribut.getKey()) {
            case STYLE -> verifierStyle(balise, valeur);
            case COLSPAN, ROWSPAN, START -> exiger(entierCourt(valeur),
                    "Valeur invalide pour %s sur <%s> : %s".formatted(attribut.getKey(), balise, valeur));
            case "src" -> octetsImages += verifierImage(valeur);
            case NUMERO, SAUT_PAGE -> exiger(valeur.isEmpty(), "%s ne prend pas de valeur.".formatted(attribut.getKey()));
            case VALEUR -> exiger("true".equals(valeur) || "false".equals(valeur),
                    "data-valeur vaut true ou false (reçu : %s).".formatted(valeur));
            default -> {
                // alt, data-champ, data-si, data-repeter : vérifiés ailleurs ou texte libre échappé à la sortie.
            }
        }
    }

    private static void exiger(boolean condition, String message) {
        if (!condition) {
            throw new InvalidRequestException(message);
        }
    }

    private static void verifierStyle(String balise, String style) {
        for (String declaration : style.split(";")) {
            if (declaration.isBlank()) {
                continue;
            }
            int deuxPoints = declaration.indexOf(':');
            String propriete = deuxPoints < 0 ? declaration.trim() : declaration.substring(0, deuxPoints).trim();
            String valeur = deuxPoints < 0 ? "" : declaration.substring(deuxPoints + 1).trim().toLowerCase(Locale.ROOT);
            boolean permis = switch (propriete.toLowerCase(Locale.ROOT)) {
                case "text-align" -> ALIGNEMENTS.contains(valeur);
                case "width" -> CELLULES.contains(balise) && largeurEnPourcent(valeur);
                default -> false;
            };
            if (!permis) {
                throw new InvalidRequestException("Style interdit sur <%s> : %s".formatted(balise, declaration.trim()));
            }
        }
    }

    private static long verifierImage(String src) {
        String source = src.trim();
        for (Map.Entry<String, byte[]> type : IMAGES.entrySet()) {
            if (source.regionMatches(true, 0, type.getKey(), 0, type.getKey().length())) {
                return decoder(source.substring(type.getKey().length()), type.getValue());
            }
        }
        throw new InvalidRequestException(
                "Image refusée : seules les images intégrées data:image/png ou data:image/jpeg (base64) sont acceptées.");
    }

    private static long decoder(String base64, byte[] signature) {
        byte[] octets;
        try {
            octets = Base64.getMimeDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Image illisible : base64 invalide.");
        }
        if (octets.length > IMAGE_MAX_OCTETS) {
            throw new InvalidRequestException("Image trop lourde : %d Ko (500 Ko maximum).".formatted(octets.length / 1024));
        }
        for (int i = 0; i < signature.length; i++) {
            if (octets.length <= i || octets[i] != signature[i]) {
                throw new InvalidRequestException("Image illisible : le contenu ne correspond pas à son type.");
            }
        }
        return octets.length;
    }

    private String verifierRepetition(Element element, String liste) {
        if (!element.hasAttr(REPETER)) {
            return liste;
        }
        String cle = element.attr(REPETER);
        if (!catalogue.estListe(cle)) {
            throw new InvalidRequestException("Liste inconnue : %s".formatted(cle));
        }
        if (liste != null) {
            throw new InvalidRequestException("Ligne répétée imbriquée : %s dans %s".formatted(cle, liste));
        }
        return cle;
    }

    private void verifierChamp(Element element, String liste) {
        if (!element.hasAttr(CHAMP)) {
            return;
        }
        if (element.hasAttr(REPETER) || element.hasAttr(SI)) {
            throw new InvalidRequestException("Un champ ne porte ni condition ni répétition : placez-les sur un élément parent.");
        }
        typeVisible(element.attr(CHAMP), liste);
    }

    private void verifierCondition(Element element, String liste) {
        if (!element.hasAttr(SI)) {
            if (element.hasAttr(VALEUR)) {
                throw new InvalidRequestException("data-valeur sans data-si sur <%s>".formatted(element.normalName()));
            }
            return;
        }
        TypeChamp type = typeVisible(element.attr(SI), liste);
        if (element.hasAttr(VALEUR) && type != TypeChamp.BOOLEEN) {
            throw new InvalidRequestException("data-valeur n'a de sens que sur un champ booléen (%s)."
                    .formatted(element.attr(SI)));
        }
    }

    /** Type du champ s'il est visible ici ; un champ de liste n'est visible que dans sa ligne répétée. */
    private TypeChamp typeVisible(String cle, String liste) {
        Optional<String> listeDuChamp = catalogue.listeDuChamp(cle);
        if (listeDuChamp.isPresent() && !listeDuChamp.get().equals(liste)) {
            throw new InvalidRequestException("Champ de liste hors ligne répétée : %s".formatted(cle));
        }
        return catalogue.type(cle, liste)
                .orElseThrow(() -> new InvalidRequestException("Champ inconnu : %s".formatted(cle)));
    }

    private static boolean entierCourt(String valeur) {
        return !valeur.isEmpty() && valeur.length() <= 3 && chiffres(valeur);
    }

    private static boolean chiffres(String valeur) {
        return valeur.chars().allMatch(c -> c >= '0' && c <= '9');
    }

    private static boolean largeurEnPourcent(String valeur) {
        if (!valeur.endsWith("%")) {
            return false;
        }
        String nombre = valeur.substring(0, valeur.length() - 1);
        int point = nombre.indexOf('.');
        String entier = point < 0 ? nombre : nombre.substring(0, point);
        String decimales = point < 0 ? "0" : nombre.substring(point + 1);
        return entierCourt(entier) && !decimales.isEmpty() && decimales.length() <= 2 && chiffres(decimales);
    }
}
