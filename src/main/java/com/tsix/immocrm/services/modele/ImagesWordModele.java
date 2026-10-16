package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.services.dossier.document.DocumentRenderingException;
import org.docx4j.convert.in.xhtml.XHTMLImageHandler;
import org.docx4j.convert.in.xhtml.renderer.Docx4jUserAgent;
import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.Part;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage;
import org.docx4j.wml.Drawing;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.w3c.dom.Element;

import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Images d'un modèle en Word, rattachées à la partie qui les affiche (corps, en-tête ou pied). Le gestionnaire par
 * défaut de docx4j les rattache toujours au corps : une image d'en-tête y pointerait vers une relation absente.
 * Les sources sont déjà des data URI PNG/JPEG vérifiées par {@link ContenuModele}.
 */
final class ImagesWordModele implements XHTMLImageHandler {

    private final Part cible;
    private final int largeurMaxTwips;
    private final AtomicInteger identifiants;

    ImagesWordModele(Part cible, int largeurMaxTwips, AtomicInteger identifiants) {
        this.cible = cible;
        this.largeurMaxTwips = largeurMaxTwips;
        this.identifiants = identifiants;
    }

    @Override
    public void addImage(Docx4jUserAgent agent, WordprocessingMLPackage paquet, P paragraphe, Element img,
                         Long largeur, Long hauteur) {
        String src = img.getAttribute("src");
        byte[] octets = Base64.getMimeDecoder().decode(src.substring(src.indexOf(',') + 1));
        try {
            BinaryPartAbstractImage image = BinaryPartAbstractImage.createImagePart(paquet, cible, octets);
            int id = identifiants.incrementAndGet();
            Inline inline = image.createImageInline("image" + id, img.getAttribute("alt"), id, id, false, largeurMaxTwips);
            ObjectFactory fabrique = Context.getWmlObjectFactory();
            Drawing dessin = fabrique.createDrawing();
            dessin.getAnchorOrInline().add(inline);
            R run = fabrique.createR();
            run.getContent().add(dessin);
            paragraphe.getContent().add(run);
        } catch (Exception e) {
            throw new DocumentRenderingException("Image du modèle illisible pour Word", e);
        }
    }

    @Override
    public void setMaxWidth(int largeur, String styleTableau) {
        // Largeur imposée par la mise en page (marges), pas par docx4j.
    }
}
