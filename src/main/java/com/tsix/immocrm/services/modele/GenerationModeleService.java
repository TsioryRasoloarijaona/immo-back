package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.domain.CategorieModele;
import com.tsix.immocrm.dto.modele.ApercuModeleRequest;
import com.tsix.immocrm.entities.app.AppModeleDocument;
import com.tsix.immocrm.entities.app.AppModeleDocumentVersion;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.jsoup.nodes.Element;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Génération d'un document depuis un modèle (FEATURE-modeles-documents.md §7) : fiche lue une fois, périmètre contrôlé
 * par le catalogue, contenu revalidé avant chaque génération. Aucun effet métier pour les modèles du lot 1.
 */
@Service
@Transactional(readOnly = true)
public class GenerationModeleService {

    private final LectureModeles lecture;
    private final CataloguesModeles catalogues;
    private final MiseEnPageModele miseEnPageModele;
    private final RenduModele rendu;
    private final ImagesModeleService images;
    private final List<ControleAccesModele> controles;

    public GenerationModeleService(LectureModeles lecture, CataloguesModeles catalogues,
                                   MiseEnPageModele miseEnPageModele, RenduModele rendu, ImagesModeleService images,
                                   List<ControleAccesModele> controles) {
        this.lecture = lecture;
        this.catalogues = catalogues;
        this.miseEnPageModele = miseEnPageModele;
        this.rendu = rendu;
        this.images = images;
        this.controles = controles;
    }

    /** Document produit ; {@code nomFichier} sans extension. */
    public static final class DocumentProduit {
        private final byte[] contenu;
        private final String codeFiche;
        private final String nomFichier;

        DocumentProduit(byte[] contenu, String codeFiche, String nomFichier) {
            this.contenu = contenu;
            this.codeFiche = codeFiche;
            this.nomFichier = nomFichier;
        }

        public byte[] contenu() {
            return contenu;
        }

        public String codeFiche() {
            return codeFiche;
        }

        public String nomFichier() {
            return nomFichier;
        }
    }

    /** Droit propre au modèle (403) avant son état actif et la fiche (404) ; un modèle PDF seul refuse le Word (400). */
    public DocumentProduit produire(UUID modeleId, UUID ficheId, FormatDocument format) {
        String code = lecture.trouver(modeleId).getCode();
        if (!controles.stream().allMatch(c -> c.autoriser(code))) {
            throw new ForbiddenException("Vous n'avez pas accès à ce formulaire.");
        }
        if (format == FormatDocument.DOCX && controles.stream().anyMatch(c -> c.pdfSeulement(code))) {
            throw new InvalidRequestException("Ce formulaire n'existe qu'en PDF.");
        }
        return produire(lecture.trouverActif(modeleId), ficheId, format);
    }

    /** PDF d'un modèle sur des valeurs déjà lues (lot de plusieurs fiches) ; mêmes règles que {@link #produire}. */
    public byte[] pdf(AppModeleDocument modele, ValeursFiche valeurs) {
        ModelePrepare rempli = rempli(modele, catalogues.de(modele.getCategorie()).catalogue(), valeurs);
        return rendu.pdf(rempli.corps(), rempli.enTete(), rempli.pied(), rempli.miseEnPage());
    }

    /** Vide si aucun modèle actif de ce code : l'appelant garde alors son rendu codé en dur. */
    public Optional<DocumentProduit> produireParCode(CategorieModele categorie, String code, UUID ficheId,
                                                     FormatDocument format) {
        return lecture.actifParCode(categorie.name(), code)
                .filter(m -> m.getVersionCouranteId() != null)
                .map(m -> produire(m, ficheId, format));
    }

    /**
     * Un PDF, une page par fiche déjà lue : modèle actif {@code code} de la catégorie, sinon {@code contenuParDefaut}.
     * En-tête et pied de la première fiche.
     */
    public byte[] pdfParPage(CategorieModele categorie, String code, String contenuParDefaut, List<ValeursFiche> fiches) {
        DescriptionChamps catalogue = catalogues.de(categorie.name()).catalogue();
        Optional<AppModeleDocumentVersion> version = lecture.actifParCode(categorie.name(), code)
                .flatMap(lecture::versionCourante);
        ModelePrepare prepare = ModelePrepare.preparer(catalogue,
                version.map(AppModeleDocumentVersion::getContenu).orElse(contenuParDefaut),
                version.map(v -> miseEnPageModele.lire(v.getMiseEnPage())).orElseGet(() -> miseEnPageModele.normaliser(null)));
        Map<UUID, String> sources = images.sourcesEnMemoire(prepare.images());
        Element corps = new Element("div");
        ModelePrepare premiere = null;
        for (ValeursFiche valeurs : fiches) {
            ModelePrepare rempli = prepare.remplir(catalogue, valeurs).avecImages(sources);
            Element page = corps.appendElement("div");
            if (premiere == null) {
                premiere = rempli;
            } else {
                page.attr("style", "page-break-before: always");
            }
            page.appendChildren(rempli.corps().childNodesCopy());
        }
        ModelePrepare reference = premiere != null ? premiere : prepare;
        return rendu.pdf(corps, reference.enTete(), reference.pied(), reference.miseEnPage());
    }

    /** Aperçu PDF d'un contenu non enregistré : mêmes règles que l'enregistrement. */
    public byte[] apercu(UUID modeleId, ApercuModeleRequest request) {
        AppModeleDocument modele = lecture.trouver(modeleId);
        if (request.ficheId() == null) {
            throw new InvalidRequestException("ficheId est requis pour l'aperçu.");
        }
        CatalogueFiche catalogue = catalogues.de(modele.getCategorie());
        ModelePrepare prepare = ModelePrepare.preparer(catalogue.catalogue(), request.contenu(),
                miseEnPageModele.normaliser(request.miseEnPage()));
        images.verifierConnues(prepare.images());
        CatalogueFiche.FicheLue fiche = catalogue.lire(request.ficheId());
        ModelePrepare rempli = prepare.remplir(catalogue.catalogue(), fiche.valeurs())
                .avecImages(images.sourcesEnMemoire(prepare.images()));
        return rendu.pdf(rempli.corps(), rempli.enTete(), rempli.pied(), rempli.miseEnPage());
    }

    private DocumentProduit produire(AppModeleDocument modele, UUID ficheId, FormatDocument format) {
        CatalogueFiche catalogue = catalogues.de(modele.getCategorie());
        CatalogueFiche.FicheLue fiche = catalogue.lire(ficheId);
        ModelePrepare rempli = rempli(modele, catalogue.catalogue(), fiche.valeurs());
        byte[] contenu = format == FormatDocument.DOCX
                ? rendu.docx(rempli.corps(), rempli.enTete(), rempli.pied(), rempli.miseEnPage())
                : rendu.pdf(rempli.corps(), rempli.enTete(), rempli.pied(), rempli.miseEnPage());
        String nom = (modele.getCode().replace('_', '-') + "-" + (fiche.code() != null ? fiche.code() : ficheId))
                .toLowerCase(Locale.ROOT);
        return new DocumentProduit(contenu, fiche.code(), nom);
    }

    /** Version courante remplie, images en mémoire ; des pages répétées sur une liste vide = aucun document (404). */
    private ModelePrepare rempli(AppModeleDocument modele, DescriptionChamps catalogue, ValeursFiche valeurs) {
        AppModeleDocumentVersion version = lecture.versionCourante(modele)
                .orElseThrow(() -> new ResourceNotFoundException("Le modèle %s n'a aucune version.".formatted(modele.getCode())));
        ModelePrepare prepare = ModelePrepare.preparer(catalogue, version.getContenu(),
                miseEnPageModele.lire(version.getMiseEnPage()));
        ModelePrepare rempli = prepare.remplir(catalogue, valeurs);
        if (!prepare.corps().select("div[" + ContenuModele.REPETER + "]").isEmpty()
                && rempli.corps().children().isEmpty() && rempli.corps().text().isBlank()) {
            throw new ResourceNotFoundException("Aucun destinataire : le document n'aurait aucune page.");
        }
        return rempli.avecImages(images.sourcesEnMemoire(prepare.images()));
    }
}
