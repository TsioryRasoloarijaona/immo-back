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
 * Crée la version 1 de l'acte de réservation depuis {@code modeles/dossier-vente/acte-reservation.html} s'il n'existe
 * pas encore (FEATURE-modeles-documents.md §8). Le script 017 n'insère rien.
 */
@Component
public class InitialiseurModeles {

    static final String RESSOURCE_ACTE = "modeles/dossier-vente/acte-reservation.html";
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
        String categorie = CategorieModele.DOSSIER_VENTE.name();
        if (modeleRepository.existsByCategorieAndCode(categorie, ActeReservationModele.CODE)) {
            return;
        }
        try {
            modeleService.creer(new CreerModeleDocumentRequest(categorie, ActeReservationModele.CODE,
                    "Acte de réservation", 10, contenuActe(), MISE_EN_PAGE_ACTE));
            log.info("Modèle {}/{} créé (version 1).", categorie, ActeReservationModele.CODE);
        } catch (ConflictException e) {
            // Créé entre-temps par une autre instance : rien à faire.
            log.debug("Modèle {}/{} déjà présent.", categorie, ActeReservationModele.CODE);
        }
    }

    static String contenuActe() {
        try (InputStream flux = new ClassPathResource(RESSOURCE_ACTE).getInputStream()) {
            return new String(flux.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Ressource " + RESSOURCE_ACTE + " illisible", e);
        }
    }
}
