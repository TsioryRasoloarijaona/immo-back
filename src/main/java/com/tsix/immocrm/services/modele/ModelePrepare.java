package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.dto.modele.MiseEnPageDto;
import org.jsoup.nodes.Element;

/** Contenu, en-tête et pied validés ensemble (images comprises) : seule voie vers l'enregistrement et la génération. */
record ModelePrepare(Element corps, Element enTete, Element pied, MiseEnPageDto miseEnPage) {

    /** {@code miseEnPage} déjà normalisée ({@link MiseEnPageModele#normaliser}). */
    static ModelePrepare preparer(Catalogue<?> catalogue, String contenu, MiseEnPageDto miseEnPage) {
        ContenuModele.Analyse corps = ContenuModele.analyser(contenu, catalogue);
        ContenuModele.Analyse enTete = analyserOptionnel(miseEnPage.enTete(), catalogue);
        ContenuModele.Analyse pied = analyserOptionnel(miseEnPage.piedDePage(), catalogue);
        ContenuModele.verifierPoidsVersion(corps.octetsImages() + octets(enTete) + octets(pied));
        return new ModelePrepare(corps.corps(), corps(enTete), corps(pied), miseEnPage);
    }

    /** Copie remplie : le résultat d'analyse reste intact. */
    ModelePrepare remplir(Catalogue<?> catalogue, ValeursFiche valeurs) {
        return new ModelePrepare(RemplissageModele.remplir(corps.clone(), catalogue, valeurs),
                enTete == null ? null : RemplissageModele.remplir(enTete.clone(), catalogue, valeurs),
                pied == null ? null : RemplissageModele.remplir(pied.clone(), catalogue, valeurs),
                miseEnPage);
    }

    private static ContenuModele.Analyse analyserOptionnel(String html, Catalogue<?> catalogue) {
        return html == null ? null : ContenuModele.analyser(html, catalogue);
    }

    private static long octets(ContenuModele.Analyse analyse) {
        return analyse == null ? 0 : analyse.octetsImages();
    }

    private static Element corps(ContenuModele.Analyse analyse) {
        return analyse == null ? null : analyse.corps();
    }
}
