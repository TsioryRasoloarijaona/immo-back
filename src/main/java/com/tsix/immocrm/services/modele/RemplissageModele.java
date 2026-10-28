package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.domain.TypeChamp;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.services.modele.ContenuModele.CHAMP;
import static com.tsix.immocrm.services.modele.ContenuModele.NUMERO;
import static com.tsix.immocrm.services.modele.ContenuModele.REPETER;
import static com.tsix.immocrm.services.modele.ContenuModele.SAUT_PAGE;
import static com.tsix.immocrm.services.modele.ContenuModele.SI;
import static com.tsix.immocrm.services.modele.ContenuModele.VALEUR;

/**
 * Remplit un contenu validé avec les valeurs d'une fiche (FEATURE-modeles-documents.md §7.2) : lignes répétées,
 * conditions, champs (valeur posée en nœud texte, donc échappée à la sortie), puis numérotation des titres.
 */
final class RemplissageModele {

    private final DescriptionChamps catalogue;
    private final ValeursFiche valeurs;

    private RemplissageModele(DescriptionChamps catalogue, ValeursFiche valeurs) {
        this.catalogue = catalogue;
        this.valeurs = valeurs;
    }

    /** {@code corps} est modifié en place : passer une copie du résultat de {@link ContenuModele#analyser}. */
    static Element remplir(Element corps, DescriptionChamps catalogue, ValeursFiche valeurs) {
        new RemplissageModele(catalogue, valeurs).traiterEnfants(corps, null, null);
        numeroter(corps);
        return corps;
    }

    private void traiterEnfants(Element parent, Map<String, Object> element, String liste) {
        for (Element enfant : new ArrayList<>(parent.children())) {
            traiter(enfant, element, liste);
        }
    }

    private void traiter(Element e, Map<String, Object> element, String liste) {
        if (e.hasAttr(REPETER)) {
            repeter(e);
            return;
        }
        if (e.hasAttr(SI) && !conditionVraie(e, element, liste)) {
            e.remove();
            return;
        }
        e.removeAttr(SI);
        e.removeAttr(VALEUR);
        if (e.hasAttr(CHAMP)) {
            String cle = e.attr(CHAMP);
            e.replaceWith(new TextNode(FormatChamp.formater(type(cle, liste), valeur(cle, element))));
            return;
        }
        if (e.hasAttr(SAUT_PAGE)) {
            e.removeAttr(SAUT_PAGE);
            e.attr("style", "page-break-before: always");
        }
        traiterEnfants(e, element, liste);
    }

    private void repeter(Element modele) {
        String cle = modele.attr(REPETER);
        modele.removeAttr(REPETER);
        List<Map<String, Object>> elements = valeurs.listes().getOrDefault(cle, List.of());
        boolean premier = true;
        for (Map<String, Object> element : elements) {
            Element copie = modele.clone();
            if (premier) {
                // Page répétée : saut entre deux éléments, jamais avant le premier.
                copie.removeAttr(SAUT_PAGE);
                premier = false;
            }
            modele.before(copie);
            traiter(copie, element, cle);
        }
        modele.remove();
    }

    private boolean conditionVraie(Element e, Map<String, Object> element, String liste) {
        String cle = e.attr(SI);
        Object valeur = valeur(cle, element);
        if (type(cle, liste) == TypeChamp.BOOLEEN) {
            boolean attendu = !e.hasAttr(VALEUR) || Boolean.parseBoolean(e.attr(VALEUR));
            return Boolean.valueOf(attendu).equals(valeur);
        }
        return FormatChamp.renseigne(valeur);
    }

    private Object valeur(String cle, Map<String, Object> element) {
        if (element != null && element.containsKey(cle)) {
            return element.get(cle);
        }
        return valeurs.champs().get(cle);
    }

    private TypeChamp type(String cle, String liste) {
        return catalogue.type(cle, liste).orElse(TypeChamp.TEXTE);
    }

    /** « 1- », « 2- »… dans l'ordre du document, une fois les blocs masqués retirés. */
    private static void numeroter(Element corps) {
        int numero = 0;
        for (Element titre : corps.select("h2[" + NUMERO + "]")) {
            numero++;
            titre.removeAttr(NUMERO);
            titre.prependText(titre.text().isBlank() ? numero + "-" : numero + "- ");
        }
    }
}
