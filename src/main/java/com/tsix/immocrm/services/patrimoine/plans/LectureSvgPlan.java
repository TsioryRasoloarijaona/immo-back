package com.tsix.immocrm.services.patrimoine.plans;

import com.tsix.immocrm.exceptions.InvalidRequestException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Lecture en flux d'un SVG d'étage, durcie (FEATURE-patrimoine-plans.md, question 6) : DTD et entités désactivées et
 * refusées, XInclude refusé. Ne lit que {@code <image>} (le dernier, comme OMRAN) et les {@code <path id d>}.
 */
final class LectureSvgPlan {

    private static final String DOCTYPE_REFUSE = "Le fichier ne doit contenir ni DOCTYPE ni entité.";
    private static final String XINCLUDE = "http://www.w3.org/2001/XInclude";

    /** Attributs bruts de l'{@code <image>} ; {@code href} = {@code xlink:href} ou {@code href} (SVG 2). */
    record ImageSvg(String largeur, String hauteur, String x, String y, String href) {
    }

    record TraceSvg(String id, String d) {
    }

    record SvgPlan(ImageSvg image, List<TraceSvg> traces) {
    }

    private LectureSvgPlan() {
    }

    static SvgPlan lire(InputStream flux) {
        XMLInputFactory fabrique = XMLInputFactory.newFactory();
        fabrique.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        fabrique.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        ImageSvg image = null;
        List<TraceSvg> traces = new ArrayList<>();
        try {
            XMLStreamReader lecteur = fabrique.createXMLStreamReader(flux);
            try {
                while (lecteur.hasNext()) {
                    int evenement = lecteur.next();
                    if (evenement == XMLStreamConstants.DTD || evenement == XMLStreamConstants.ENTITY_REFERENCE) {
                        throw new InvalidRequestException(DOCTYPE_REFUSE);
                    }
                    if (evenement == XMLStreamConstants.START_ELEMENT) {
                        image = element(lecteur, image, traces);
                    }
                }
            } finally {
                lecteur.close();
            }
        } catch (XMLStreamException e) {
            throw new InvalidRequestException("Fichier SVG illisible (XML mal formé).");
        }
        return new SvgPlan(image, traces);
    }

    private static ImageSvg element(XMLStreamReader lecteur, ImageSvg image, List<TraceSvg> traces) {
        if (XINCLUDE.equals(lecteur.getNamespaceURI())) {
            throw new InvalidRequestException("Le fichier ne doit pas contenir d'inclusion XInclude.");
        }
        return switch (lecteur.getLocalName()) {
            case "image" -> new ImageSvg(attribut(lecteur, "width"), attribut(lecteur, "height"),
                    attribut(lecteur, "x"), attribut(lecteur, "y"), attribut(lecteur, "href"));
            case "path" -> {
                traces.add(new TraceSvg(attribut(lecteur, "id"), attribut(lecteur, "d")));
                yield image;
            }
            default -> image;
        };
    }

    /** Par nom local, quel que soit l'espace de noms : {@code xlink:href} et {@code href} (SVG 2) valent tous deux. */
    private static String attribut(XMLStreamReader lecteur, String nom) {
        for (int i = 0; i < lecteur.getAttributeCount(); i++) {
            if (nom.equals(lecteur.getAttributeLocalName(i))) {
                return lecteur.getAttributeValue(i);
            }
        }
        return null;
    }
}
