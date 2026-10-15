package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stl.StlQuote;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.stl.StlQuoteRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Offre d'origine d'un dossier ({@code ComFolder.StlQuoteId}), comme {@code GenerateComFolderFromQuote} d'OMRAN. */
@Component
public class OffreOrigineDossier {

    private final StlQuoteRepository quoteRepository;

    public OffreOrigineDossier(StlQuoteRepository quoteRepository) {
        this.quoteRepository = quoteRepository;
    }

    /** L'id de l'offre si elle appartient à la prospection du dossier ; null sans offre ; 400 sinon. */
    public UUID verifier(UUID offreId, ComProspection prospection) {
        if (offreId == null) {
            return null;
        }
        StlQuote offre = quoteRepository.findById(offreId)
                .orElseThrow(() -> new InvalidRequestException("Offre introuvable: " + offreId));
        if (offre.getComProspection() == null || !prospection.getId().equals(offre.getComProspection().getId())) {
            throw new InvalidRequestException("L'offre " + offre.getCode() + " n'appartient pas à cette prospection");
        }
        return offre.getId();
    }
}
