package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.dto.prospection.InteractionDto;
import com.tsix.immocrm.dto.prospection.ProspectionDetailDto;
import com.tsix.immocrm.dto.prospection.ProspectionFicheData;
import com.tsix.immocrm.dto.prospection.ProspectionFicheData.Action;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Gathers the data behind the "Fiche Prospection" document (rendered by {@code ProspectionFichePdfRenderer}),
 * modeled on the legacy "FICHE" prospection template.
 */
@Service
@Transactional(readOnly = true)
public class ProspectionFicheService {

    private static final int NOMBRE_ACTIONS_AFFICHEES = 15;

    private final ProspectionService prospectionService;
    private final ComProspectionRepository prospectionRepository;

    public ProspectionFicheService(ProspectionService prospectionService, ComProspectionRepository prospectionRepository) {
        this.prospectionService = prospectionService;
        this.prospectionRepository = prospectionRepository;
    }

    public ProspectionFicheData collect(UUID prospectionId) {
        ProspectionDetailDto detail = prospectionService.findById(prospectionId);
        ComProspection prospection = prospectionRepository.findById(prospectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + prospectionId));

        var actions = prospectionService.findInteractions(prospectionId, PageRequest.of(0, NOMBRE_ACTIONS_AFFICHEES))
                .map(ProspectionFicheService::toAction)
                .getContent();

        return new ProspectionFicheData(
                detail.code(),
                detail.dateDocument(),
                detail.prospectNom(),
                detail.commercialNom(),
                detail.origine(),
                detail.origineSupport(),
                detail.actionCommerciale(),
                detail.vocation(),
                detail.trancheDescription(),
                detail.articleCode(),
                detail.surface(),
                detail.surfaceJusqua(),
                detail.prix(),
                detail.prixJusqua(),
                detail.avance(),
                detail.financementFondsPropres(),
                detail.financementCredit(),
                prospection.getAlertValidityDate(),
                prospection.getAlertMessage(),
                detail.fermee(),
                detail.notes(),
                actions
        );
    }

    private static Action toAction(InteractionDto i) {
        return new Action(i.date(), i.description(), i.actionType(), i.resultat());
    }
}
