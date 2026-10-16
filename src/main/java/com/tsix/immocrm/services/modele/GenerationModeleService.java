package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.domain.CategorieModele;
import com.tsix.immocrm.dto.modele.ApercuModeleRequest;
import com.tsix.immocrm.entities.app.AppModeleDocument;
import com.tsix.immocrm.entities.app.AppModeleDocumentVersion;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
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

    public GenerationModeleService(LectureModeles lecture, CataloguesModeles catalogues,
                                   MiseEnPageModele miseEnPageModele, RenduModele rendu) {
        this.lecture = lecture;
        this.catalogues = catalogues;
        this.miseEnPageModele = miseEnPageModele;
        this.rendu = rendu;
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

    /** Modèle actif seulement (404 sinon) ; fiche hors périmètre = 404. */
    public DocumentProduit produire(UUID modeleId, UUID ficheId, FormatDocument format) {
        return produire(lecture.trouverActif(modeleId), ficheId, format);
    }

    /** Vide si aucun modèle actif de ce code : l'appelant garde alors son rendu codé en dur. */
    public Optional<DocumentProduit> produireParCode(CategorieModele categorie, String code, UUID ficheId,
                                                     FormatDocument format) {
        return lecture.actifParCode(categorie.name(), code)
                .filter(m -> m.getVersionCouranteId() != null)
                .map(m -> produire(m, ficheId, format));
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
        CatalogueFiche.FicheLue fiche = catalogue.lire(request.ficheId());
        ModelePrepare rempli = prepare.remplir(catalogue.catalogue(), fiche.valeurs());
        return rendu.pdf(rempli.corps(), rempli.enTete(), rempli.pied(), rempli.miseEnPage());
    }

    private DocumentProduit produire(AppModeleDocument modele, UUID ficheId, FormatDocument format) {
        AppModeleDocumentVersion version = lecture.versionCourante(modele)
                .orElseThrow(() -> new ResourceNotFoundException("Le modèle %s n'a aucune version.".formatted(modele.getCode())));
        CatalogueFiche catalogue = catalogues.de(modele.getCategorie());
        ModelePrepare prepare = ModelePrepare.preparer(catalogue.catalogue(), version.getContenu(),
                miseEnPageModele.lire(version.getMiseEnPage()));
        CatalogueFiche.FicheLue fiche = catalogue.lire(ficheId);
        ModelePrepare rempli = prepare.remplir(catalogue.catalogue(), fiche.valeurs());
        byte[] contenu = format == FormatDocument.DOCX
                ? rendu.docx(rempli.corps(), rempli.enTete(), rempli.pied(), rempli.miseEnPage())
                : rendu.pdf(rempli.corps(), rempli.enTete(), rempli.pied(), rempli.miseEnPage());
        String nom = (modele.getCode().replace('_', '-') + "-" + (fiche.code() != null ? fiche.code() : ficheId))
                .toLowerCase(Locale.ROOT);
        return new DocumentProduit(contenu, fiche.code(), nom);
    }
}
