package com.tsix.immocrm.services.modele;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.tsix.immocrm.dto.modele.MiseEnPageDto;
import com.tsix.immocrm.services.dossier.document.DocumentRenderingException;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.jaxb.Context;
import org.docx4j.model.structure.PageSizePaper;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.FooterPart;
import org.docx4j.openpackaging.parts.WordprocessingML.HeaderPart;
import org.docx4j.relationships.Relationship;
import org.docx4j.wml.FooterReference;
import org.docx4j.wml.HdrFtrRef;
import org.docx4j.wml.HeaderReference;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.SectPr;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

/**
 * HTML rempli → PDF (openhtmltopdf) ou Word (docx4j ImportXHTML), même feuille de style. Le document est construit par
 * jsoup puis passé en DOM ou en XHTML sérialisé : aucun DOCTYPE ni entité venant de l'utilisateur n'atteint un parseur XML.
 */
@Component
public class RenduModele {

    private static final String ENTETE = "entete";
    private static final String PIED = "pied";
    private static final double TWIPS_PAR_MM = 1440 / 25.4;
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
            builder.withW3cDocument(new W3CDom().fromJsoup(document), null);
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

            paquet.getMainDocumentPart().getContent().addAll(convertir(paquet, corps));
            if (enTete != null) {
                ajouterEnTete(paquet, section, convertir(paquet, enTete));
            }
            if (pied != null) {
                ajouterPied(paquet, section, convertir(paquet, pied));
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            paquet.save(out);
            return out.toByteArray();
        } catch (Docx4JException | RuntimeException e) {
            throw new DocumentRenderingException("Erreur lors de la génération du Word du modèle", e);
        }
    }

    private static List<Object> convertir(WordprocessingMLPackage paquet, Element contenu) throws Docx4JException {
        Document document = document(STYLE_COMMUN);
        document.body().appendChildren(contenu.childNodesCopy());
        return new XHTMLImporterImpl(paquet).convert(document.outerHtml(), null);
    }

    private static void ajouterEnTete(WordprocessingMLPackage paquet, SectPr section, List<Object> contenu)
            throws Docx4JException {
        ObjectFactory fabrique = Context.getWmlObjectFactory();
        HeaderPart partie = new HeaderPart();
        partie.setJaxbElement(fabrique.createHdr());
        partie.getContent().addAll(contenu);
        Relationship relation = paquet.getMainDocumentPart().addTargetPart(partie);
        HeaderReference reference = fabrique.createHeaderReference();
        reference.setId(relation.getId());
        reference.setType(HdrFtrRef.DEFAULT);
        section.getEGHdrFtrReferences().add(reference);
    }

    private static void ajouterPied(WordprocessingMLPackage paquet, SectPr section, List<Object> contenu)
            throws Docx4JException {
        ObjectFactory fabrique = Context.getWmlObjectFactory();
        FooterPart partie = new FooterPart();
        partie.setJaxbElement(fabrique.createFtr());
        partie.getContent().addAll(contenu);
        Relationship relation = paquet.getMainDocumentPart().addTargetPart(partie);
        FooterReference reference = fabrique.createFooterReference();
        reference.setId(relation.getId());
        reference.setType(HdrFtrRef.DEFAULT);
        section.getEGHdrFtrReferences().add(reference);
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
