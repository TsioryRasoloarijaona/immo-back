package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.dto.modele.MiseEnPageDto;
import org.jsoup.nodes.Element;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Contenu, en-tête et pied validés ensemble (texte ≤ 1 Mo, images citées par id) : seule voie vers l'enregistrement et
 * la génération. L'existence des images citées est vérifiée par {@link ImagesModeleService}.
 */
record ModelePrepare(Element corps, Element enTete, Element pied, MiseEnPageDto miseEnPage, Set<UUID> images) {

    static final String FOND = "data-fond";

    /** {@code miseEnPage} déjà normalisée ({@link MiseEnPageModele#normaliser}). */
    static ModelePrepare preparer(DescriptionChamps catalogue, String contenu, MiseEnPageDto miseEnPage) {
        ContenuModele.verifierTailleTexte(longueur(contenu) + longueur(miseEnPage.enTete()) + longueur(miseEnPage.piedDePage()));
        ContenuModele.Analyse corps = ContenuModele.analyser(contenu, catalogue);
        ContenuModele.Analyse enTete = analyserOptionnel(miseEnPage.enTete(), catalogue);
        ContenuModele.Analyse pied = analyserOptionnel(miseEnPage.piedDePage(), catalogue);
        Set<UUID> images = new LinkedHashSet<>(corps.images());
        if (enTete != null) {
            images.addAll(enTete.images());
        }
        if (pied != null) {
            images.addAll(pied.images());
        }
        if (miseEnPage.fondImageId() != null) {
            images.add(miseEnPage.fondImageId());
        }
        return new ModelePrepare(corps.corps(), corps(enTete), corps(pied), miseEnPage, Set.copyOf(images));
    }

    /** Copie remplie : le résultat d'analyse reste intact. */
    ModelePrepare remplir(DescriptionChamps catalogue, ValeursFiche valeurs) {
        return new ModelePrepare(RemplissageModele.remplir(corps.clone(), catalogue, valeurs),
                enTete == null ? null : RemplissageModele.remplir(enTete.clone(), catalogue, valeurs),
                pied == null ? null : RemplissageModele.remplir(pied.clone(), catalogue, valeurs),
                miseEnPage, images);
    }

    /**
     * Pour le rendu seulement : chaque {@code img[data-image]} reçoit ses octets en data URI, en mémoire ; le fond de page
     * devient un {@code img[data-fond]} fixe, répété sur chaque page derrière le texte (retiré en Word).
     */
    ModelePrepare avecImages(Map<UUID, String> sources) {
        UUID fond = miseEnPage.fondImageId();
        if (fond != null) {
            String taille = Boolean.FALSE.equals(miseEnPage.fondPleinePage()) ? "" : " width: 210mm; height: 297mm;";
            corps.prependElement("img").attr(FOND, "").attr("src", sources.get(fond)).attr("style",
                    "position: fixed; z-index: -1; left: -%dmm; top: -%dmm; max-width: none;%s"
                            .formatted(miseEnPage.margeGauche(), miseEnPage.margeHaut(), taille));
        }
        for (Element zone : new Element[]{corps, enTete, pied}) {
            if (zone != null) {
                zone.select("img[" + ContenuModele.IMAGE + "]").forEach(img -> {
                    img.attr("src", sources.get(UUID.fromString(img.attr(ContenuModele.IMAGE))));
                    img.removeAttr(ContenuModele.IMAGE);
                });
            }
        }
        return this;
    }

    private static long longueur(String texte) {
        return texte == null ? 0 : texte.length();
    }

    private static ContenuModele.Analyse analyserOptionnel(String html, DescriptionChamps catalogue) {
        return html == null ? null : ContenuModele.analyser(html, catalogue);
    }

    private static Element corps(ContenuModele.Analyse analyse) {
        return analyse == null ? null : analyse.corps();
    }
}
