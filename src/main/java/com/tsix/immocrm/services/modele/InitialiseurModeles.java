package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.domain.CategorieModele;
import com.tsix.immocrm.dto.modele.CreerModeleDocumentRequest;
import com.tsix.immocrm.dto.modele.MiseEnPageDto;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.repositories.app.AppModeleDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Crée la version 1 des modèles livrés (acte de réservation, lettre de 1re relance) depuis {@code modeles/…} s'ils
 * n'existent pas encore (FEATURE-modeles-documents.md §8). Le script 017 n'insère rien.
 */
@Component
public class InitialiseurModeles {

    static final String RESSOURCE_ACTE = "modeles/dossier-vente/acte-reservation.html";
    public static final String RESSOURCE_RELANCE = "modeles/relance-titre-foncier/premiere-relance.html";
    public static final String CODE_PREMIERE_RELANCE = "PREMIERE_RELANCE";
    /** Marges de l'acte codé en dur (45 pt côtés, 35 pt haut et bas), arrondies au mm. */
    static final MiseEnPageDto MISE_EN_PAGE_ACTE = new MiseEnPageDto("A4", 12, 12, 16, 16, null, null);

    private static final Logger log = LoggerFactory.getLogger(InitialiseurModeles.class);

    private final AppModeleDocumentRepository modeleRepository;
    private final ModeleDocumentService modeleService;

    public InitialiseurModeles(AppModeleDocumentRepository modeleRepository, ModeleDocumentService modeleService) {
        this.modeleRepository = modeleRepository;
        this.modeleService = modeleService;
    }

    public void initialiserActeReservation() {
        initialiser(new CreerModeleDocumentRequest(CategorieModele.DOSSIER_VENTE.name(), ActeReservationModele.CODE,
                "Acte de réservation", 10, contenuActe(), MISE_EN_PAGE_ACTE));
    }

    /** Texte neutre de remplacement, à réécrire par l'utilisateur dans les modèles de documents. */
    public void initialiserPremiereRelance() {
        initialiser(new CreerModeleDocumentRequest(CategorieModele.RELANCE_TITRE_FONCIER.name(), CODE_PREMIERE_RELANCE,
                "Lettre de 1re relance", 10, contenu(RESSOURCE_RELANCE), null));
    }

    private void initialiser(CreerModeleDocumentRequest modele) {
        if (modeleRepository.existsByCategorieAndCode(modele.categorie(), modele.code())) {
            return;
        }
        try {
            modeleService.creer(modele);
            log.info("Modèle {}/{} créé (version 1).", modele.categorie(), modele.code());
        } catch (ConflictException e) {
            // Créé entre-temps par une autre instance : rien à faire.
            log.debug("Modèle {}/{} déjà présent.", modele.categorie(), modele.code());
        }
    }

    static String contenuActe() {
        return contenu(RESSOURCE_ACTE);
    }

    public static String contenu(String ressource) {
        try (InputStream flux = new ClassPathResource(ressource).getInputStream()) {
            return new String(flux.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Ressource " + ressource + " illisible", e);
        }
    }
}
