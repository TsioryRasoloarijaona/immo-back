package com.tsix.immocrm.services.modele;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.tsix.immocrm.dto.modele.MiseEnPageDto;
import com.tsix.immocrm.services.dossier.document.DocumentRenderingException;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.jaxb.Context;
import org.docx4j.model.structure.PageSizePaper;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.Part;
import org.docx4j.openpackaging.parts.WordprocessingML.FooterPart;
import org.docx4j.openpackaging.parts.WordprocessingML.HeaderPart;
import org.docx4j.relationships.Relationship;
import org.docx4j.wml.FooterReference;
import org.docx4j.wml.HdrFtrRef;
import org.docx4j.wml.HeaderReference;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.SectPr;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * HTML rempli → PDF (openhtmltopdf) ou Word (docx4j ImportXHTML), même feuille de style. Le document est construit par
 * jsoup puis passé en DOM ou en XHTML sérialisé : aucun DOCTYPE ni entité venant de l'utilisateur n'atteint un parseur XML.
 */
@Component
public class RenduModele {

    private static final String ENTETE = "entete";
    private static final String PIED = "pied";
    private static final double TWIPS_PAR_MM = 1440 / 25.4;
    private static final int A4_LARGEUR_TWIPS = 11906;
    private static final String STYLE_COMMUN = """
            body { font-family: sans-serif; font-size: 9.5pt; }
            h1 { font-size: 14pt; margin: 0 0 8pt 0; }
            h2 { font-size: 11pt; margin: 8pt 0 2pt 0; }
            h3 { font-size: 10pt; margin: 6pt 0 2pt 0; }
            p { margin: 0 0 6pt 0; }
            table { border-collapse: collapse; width: 100%; }
            td, th { border: 0.5pt solid #000000; padding: 2pt 4pt; vertical-align: top; }
            img { max-width: 100%; }
            """;

    public byte[] pdf(Element corps, Element enTete, Element pied, MiseEnPageDto miseEnPage) {
        Document document = document(STYLE_COMMUN + stylePage(miseEnPage, enTete != null, pied != null));
        if (enTete != null) {
            document.body().appendElement("div").attr("id", ENTETE).appendChildren(enTete.childNodesCopy());
        }
        if (pied != null) {
            document.body().appendElement("div").attr("id", PIED).appendChildren(pied.childNodesCopy());
        }
        document.body().appendChildren(corps.childNodesCopy());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            // Défense en profondeur : la liste blanche n'admet que des images data:, aucune ressource réseau ou disque.
            builder.useProtocolsStreamImplementation(url -> {
                throw new IllegalStateException("Ressource externe refusée : " + url);
            }, "http", "https", "file", "jar", "ftp");
            builder.withW3cDocument(new W3CDom().fromJsoup(pourXml(document)), null);
            builder.toStream(out);
            builder.run();
        } catch (IOException | RuntimeException e) {
            throw new DocumentRenderingException("Erreur lors de la génération du PDF du modèle", e);
        }
        return out.toByteArray();
    }

    public byte[] docx(Element corps, Element enTete, Element pied, MiseEnPageDto miseEnPage) {
        try {
            WordprocessingMLPackage paquet = WordprocessingMLPackage.createPackage(PageSizePaper.A4, false);
            SectPr section = paquet.getDocumentModel().getSections().get(0).getSectPr();
            section.getPgMar().setTop(twips(miseEnPage.margeHaut()));
            section.getPgMar().setBottom(twips(miseEnPage.margeBas()));
            section.getPgMar().setLeft(twips(miseEnPage.margeGauche()));
            section.getPgMar().setRight(twips(miseEnPage.margeDroite()));

            int largeurUtile = A4_LARGEUR_TWIPS - twips(miseEnPage.margeGauche()).intValue()
                    - twips(miseEnPage.margeDroite()).intValue();
            Conversion conversion = new Conversion(paquet, largeurUtile, new AtomicInteger());
            paquet.getMainDocumentPart().getContent().addAll(conversion.convertir(paquet.getMainDocumentPart(), corps));
            if (enTete != null) {
                ajouterEnTete(conversion, section, enTete);
            }
            if (pied != null) {
                ajouterPied(conversion, section, pied);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            paquet.save(out);
            return out.toByteArray();
        } catch (Docx4JException | RuntimeException e) {
            throw new DocumentRenderingException("Erreur lors de la génération du Word du modèle", e);
        }
    }

    /** Un import par partie : ses images doivent être rattachées à cette partie (voir {@link ImagesWordModele}). */
    private record Conversion(WordprocessingMLPackage paquet, int largeurMaxTwips, AtomicInteger identifiants) {

        List<Object> convertir(Part cible, Element contenu) throws Docx4JException {
            Document document = document(STYLE_COMMUN);
            document.body().appendChildren(contenu.childNodesCopy());
            XHTMLImporterImpl importeur = new XHTMLImporterImpl(paquet);
            importeur.setXHTMLImageHandler(new ImagesWordModele(cible, largeurMaxTwips, identifiants));
            return importeur.convert(pourXml(document).outerHtml(), null);
        }
    }

    /** La partie est ajoutée au paquet avant la conversion : ses images s'y rattachent. */
    private static void ajouterEnTete(Conversion conversion, SectPr section, Element contenu) throws Docx4JException {
        ObjectFactory fabrique = Context.getWmlObjectFactory();
        HeaderPart partie = new HeaderPart();
        partie.setJaxbElement(fabrique.createHdr());
        Relationship relation = conversion.paquet().getMainDocumentPart().addTargetPart(partie);
        partie.getContent().addAll(conversion.convertir(partie, contenu));
        HeaderReference reference = fabrique.createHeaderReference();
        reference.setId(relation.getId());
        reference.setType(HdrFtrRef.DEFAULT);
        section.getEGHdrFtrReferences().add(reference);
    }

    private static void ajouterPied(Conversion conversion, SectPr section, Element contenu) throws Docx4JException {
        ObjectFactory fabrique = Context.getWmlObjectFactory();
        FooterPart partie = new FooterPart();
        partie.setJaxbElement(fabrique.createFtr());
        Relationship relation = conversion.paquet().getMainDocumentPart().addTargetPart(partie);
        partie.getContent().addAll(conversion.convertir(partie, contenu));
        FooterReference reference = fabrique.createFooterReference();
        reference.setId(relation.getId());
        reference.setType(HdrFtrRef.DEFAULT);
        section.getEGHdrFtrReferences().add(reference);
    }

    /**
     * Le XHTML passe par un parseur XML (docx4j) : commentaires (« -- » interdit) et caractères de contrôle collés
     * depuis Word (U+000B…) y sont fatals. Retirés ici, pour le contenu comme pour les valeurs des champs.
     */
    static Document pourXml(Document document) {
        List<Node> commentaires = new ArrayList<>();
        NodeTraversor.traverse((noeud, profondeur) -> {
            if (noeud instanceof Comment) {
                commentaires.add(noeud);
            } else if (noeud instanceof TextNode texte) {
                texte.text(caracteresXml(texte.getWholeText()));
            } else if (noeud instanceof Element element) {
                element.attributes().forEach(a -> a.setValue(caracteresXml(a.getValue())));
            }
        }, document);
        commentaires.forEach(Node::remove);
        return document;
    }

    private static String caracteresXml(String texte) {
        StringBuilder propre = new StringBuilder(texte.length());
        texte.codePoints().map(c -> permisEnXml(c) ? c : ' ').forEach(propre::appendCodePoint);
        return propre.toString();
    }

    private static boolean permisEnXml(int c) {
        return c == 0x9 || c == 0xA || c == 0xD || (c >= 0x20 && c <= 0xD7FF)
                || (c >= 0xE000 && c <= 0xFFFD) || (c >= 0x10000 && c <= 0x10FFFF);
    }

    private static Document document(String style) {
        Document document = Document.createShell("");
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml).prettyPrint(false);
        document.head().appendElement("meta").attr("charset", "UTF-8");
        document.head().appendElement("style").appendChild(new DataNode(style));
        return document;
    }

    private static String stylePage(MiseEnPageDto mep, boolean avecEnTete, boolean avecPied) {
        StringBuilder style = new StringBuilder("@page { size: A4; margin: %dmm %dmm %dmm %dmm;"
                .formatted(mep.margeHaut(), mep.margeDroite(), mep.margeBas(), mep.margeGauche()));
        if (avecEnTete) {
            style.append(" @top-center { content: element(entete); }");
        }
        if (avecPied) {
            style.append(" @bottom-center { content: element(pied); }");
        }
        return style.append(" }\n#entete { position: running(entete); }\n#pied { position: running(pied); }\n")
                .toString();
    }

    private static BigInteger twips(int millimetres) {
        return BigInteger.valueOf(Math.round(millimetres * TWIPS_PAR_MM));
    }
}
