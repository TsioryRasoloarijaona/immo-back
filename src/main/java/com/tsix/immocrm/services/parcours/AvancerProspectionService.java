package com.tsix.immocrm.services.parcours;

import com.tsix.immocrm.dto.parcours.AvancerProspectionRequest;
import com.tsix.immocrm.dto.parcours.AvancerProspectionRequest.ProchaineActionRequest;
import com.tsix.immocrm.dto.prospection.ChangerEtapeRequest;
import com.tsix.immocrm.dto.prospection.CreerInteractionRequest;
import com.tsix.immocrm.dto.prospection.ProspectionDetailDto;
import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComActionMessageSubCategoryRepository;
import com.tsix.immocrm.repositories.com.ComProspectionKindRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
import com.tsix.immocrm.services.prospection.ProspectionService;
import com.tsix.immocrm.services.prospection.ReglesInteraction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Clock;
import java.util.UUID;

/**
 * Glisser une carte du Kanban : étape (règles de {@code PATCH /etape}) et prochaine action (règles de
 * {@code POST /interactions}) dans la même transaction — l'une échoue, rien n'est écrit (FEATURE-parcours-commercial.md §3).
 */
@Service
public class AvancerProspectionService {

    private final ComProspectionRepository prospectionRepository;
    private final ComProspectionKindRepository etapeRepository;
    private final ComActionMessageSubCategoryRepository resultatRepository;
    private final ProspectionService prospectionService;
    private final PorteeParcours portee;
    private final Clock clock;

    public AvancerProspectionService(ComProspectionRepository prospectionRepository,
                                     ComProspectionKindRepository etapeRepository,
                                     ComActionMessageSubCategoryRepository resultatRepository,
                                     ProspectionService prospectionService, PorteeParcours portee, Clock clock) {
        this.prospectionRepository = prospectionRepository;
        this.etapeRepository = etapeRepository;
        this.resultatRepository = resultatRepository;
        this.prospectionService = prospectionService;
        this.portee = portee;
        this.clock = clock;
    }

    @Transactional
    public ProspectionDetailDto avancer(UUID prospectionId, AvancerProspectionRequest request) {
        ComProspection prospection = prospectionRepository.findById(prospectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + prospectionId));
        portee.verifierLigne(prospection);
        ProchaineActionRequest action = verifierCorps(request);
        verifierEtatDeLaLigne(prospection);
        ComProspectionKind etape = etapeRepository.findById(request.etapeId())
                .orElseThrow(() -> new InvalidRequestException("Étape (ComProspectionKind) introuvable: " + request.etapeId()));
        if (Boolean.TRUE.equals(etape.getIsWithProjectMandatory()) && prospection.getStkHierarchy() == null) {
            throw new ConflictException("L'étape « " + etape.getCode()
                    + " » exige un projet : rattachez d'abord une tranche à la prospection " + prospection.getCode() + ".");
        }

        // Pas de note : « avancer » n'écrase jamais ComProspection.Notes.
        prospectionService.changerEtape(prospectionId, new ChangerEtapeRequest(etape.getId(), null));
        // Résultat « visite » : la date de la relance est aussi celle du rendez-vous (AdmEvent, donc le calendrier).
        LocalDateTime dateRdv = estUneVisite(action.sousCategorieId()) ? action.date() : null;
        prospectionService.addInteraction(prospectionId, new CreerInteractionRequest(action.message(), false,
                action.typeId(), action.sousCategorieId(), action.date(), dateRdv));
        return prospectionService.findById(prospectionId);
    }

    /** 400 avant toute écriture : la prochaine action et sa date sont obligatoires, jamais dans le passé. */
    private ProchaineActionRequest verifierCorps(AvancerProspectionRequest request) {
        if (request == null || request.etapeId() == null) {
            throw new InvalidRequestException("etapeId est requis");
        }
        ProchaineActionRequest action = request.prochaineAction();
        if (action == null || action.typeId() == null) {
            throw new InvalidRequestException("La prochaine action (prochaineAction.typeId) est obligatoire.");
        }
        if (action.date() == null) {
            throw new InvalidRequestException("La date de la prochaine action (prochaineAction.date) est obligatoire.");
        }
        if (action.date().toLocalDate().isBefore(LocalDate.now(clock))) {
            throw new InvalidRequestException("La date de la prochaine action ne peut pas être dans le passé.");
        }
        return action;
    }

    private static void verifierEtatDeLaLigne(ComProspection prospection) {
        if (DiscriminantPreProspection.estPreProspection(prospection)) {
            throw new ConflictException("La ligne " + prospection.getCode()
                    + " est une pré-prospection : elle se trie dans la Boîte de tri, pas dans le Kanban.");
        }
        if (Boolean.TRUE.equals(prospection.getIsClosed())) {
            throw new ConflictException("La prospection " + prospection.getCode() + " est archivée.");
        }
    }

    private boolean estUneVisite(UUID sousCategorieId) {
        if (sousCategorieId == null) {
            return false;
        }
        ComActionMessageSubCategory resultat = resultatRepository.findById(sousCategorieId).orElse(null);
        return ReglesInteraction.exigeUneDateDeRdv(resultat);
    }
}
