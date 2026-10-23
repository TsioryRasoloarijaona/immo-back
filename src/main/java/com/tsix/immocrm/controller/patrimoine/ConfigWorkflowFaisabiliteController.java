package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.faisabilite.ChampsObligatoiresFaisabilite;
import com.tsix.immocrm.domain.faisabilite.ChampsObligatoiresFaisabilite.ChampObligatoire;
import com.tsix.immocrm.dto.patrimoine.faisabilite.StatutEtudeConfigDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.TransitionEtudeConfigDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.services.patrimoine.ConfigWorkflowFaisabiliteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.patrimoine.EtudeFaisabiliteController.ADMIN;
import static com.tsix.immocrm.controller.patrimoine.EtudeFaisabiliteController.PAS_ADMIN;
import static com.tsix.immocrm.controller.patrimoine.EtudeFaisabiliteController.PAS_LECTEUR;

@Tag(name = "Études de faisabilité — configuration des statuts et transitions")
@RestController
@RequestMapping("/api/config-faisabilite")
@PreAuthorize(EtudeFaisabiliteController.LECTURE)
public class ConfigWorkflowFaisabiliteController {

    private static final String STATUTS = "/statuts";
    private static final String STATUT = STATUTS + "/{id}";
    private static final String TRANSITIONS = "/transitions";
    private static final String TRANSITION = TRANSITIONS + "/{id}";
    private static final String ID_STATUT = "Id du statut (StkFeasibilityStudyStatus.PKey).";
    private static final String ID_TRANSITION = "Id de la transition (StkFeasibilityStudyWorkFlow.PKey).";
    private static final String STATUT_INCONNU = "Statut inconnu.";
    private static final String TRANSITION_INCONNUE = "Transition inconnue.";
    private static final String SAISIE = "Champ obligatoire absent, référence ou nom de champ inconnu (detail nomme le champ).";

    private final ConfigWorkflowFaisabiliteService service;

    public ConfigWorkflowFaisabiliteController(ConfigWorkflowFaisabiliteService service) {
        this.service = service;
    }

    @GetMapping(STATUTS)
    @Operation(summary = "Statuts d'étude, par ordre")
    @ApiResponse(responseCode = "403", description = PAS_LECTEUR)
    public List<StatutEtudeConfigDto> statuts() {
        return service.listerStatuts();
    }

    @GetMapping(STATUT)
    @Operation(summary = "Un statut d'étude avec ses documents")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statut."),
            @ApiResponse(responseCode = "403", description = PAS_LECTEUR),
            @ApiResponse(responseCode = "404", description = STATUT_INCONNU)
    })
    public StatutEtudeConfigDto statut(@Parameter(description = ID_STATUT) @PathVariable UUID id) {
        return service.statut(id);
    }

    @PostMapping(STATUTS)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMIN)
    @Operation(summary = "Crée un statut d'étude")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Statut créé."),
            @ApiResponse(responseCode = "400", description = SAISIE),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "409", description = "Code déjà pris.")
    })
    public StatutEtudeConfigDto creerStatut(@RequestBody StatutEtudeConfigDto saisie) {
        return service.creerStatut(saisie);
    }

    @PatchMapping(STATUT)
    @PreAuthorize(ADMIN)
    @Operation(summary = "Modifie un statut d'étude ; champ absent = inchangé")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statut relu."),
            @ApiResponse(responseCode = "400", description = SAISIE),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = STATUT_INCONNU),
            @ApiResponse(responseCode = "409", description = "Code déjà pris.")
    })
    public StatutEtudeConfigDto modifierStatut(@Parameter(description = ID_STATUT) @PathVariable UUID id,
                                               @RequestBody StatutEtudeConfigDto saisie) {
        return service.modifierStatut(id, saisie);
    }

    @DeleteMapping(STATUT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ADMIN)
    @Operation(summary = "Supprime un statut inutilisé, avec ses documents")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Statut supprimé."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = STATUT_INCONNU),
            @ApiResponse(responseCode = "409", description = "Statut porté par une étude, une transition ou l'historique.")
    })
    public void supprimerStatut(@Parameter(description = ID_STATUT) @PathVariable UUID id) {
        service.supprimerStatut(id);
    }

    @GetMapping(TRANSITIONS)
    @Operation(summary = "Transitions d'étude, par ordre")
    @ApiResponse(responseCode = "403", description = PAS_LECTEUR)
    public List<TransitionEtudeConfigDto> transitions() {
        return service.listerTransitions();
    }

    @GetMapping(TRANSITION)
    @Operation(summary = "Une transition d'étude avec ses rôles et documents")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transition."),
            @ApiResponse(responseCode = "403", description = PAS_LECTEUR),
            @ApiResponse(responseCode = "404", description = TRANSITION_INCONNUE)
    })
    public TransitionEtudeConfigDto transition(@Parameter(description = ID_TRANSITION) @PathVariable UUID id) {
        return service.transition(id);
    }

    @PostMapping(TRANSITIONS)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMIN)
    @Operation(summary = "Crée une transition d'étude")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transition créée."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "409", description = "Une transition relie déjà ces deux statuts.")
    })
    public TransitionEtudeConfigDto creerTransition(@RequestBody TransitionEtudeConfigDto saisie) {
        return service.creerTransition(saisie);
    }

    @PatchMapping(TRANSITION)
    @PreAuthorize(ADMIN)
    @Operation(summary = "Modifie une transition d'étude ; champ absent = inchangé, liste reçue = remplace")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transition relue."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = TRANSITION_INCONNUE),
            @ApiResponse(responseCode = "409", description = "Une transition relie déjà ces deux statuts.")
    })
    public TransitionEtudeConfigDto modifierTransition(@Parameter(description = ID_TRANSITION) @PathVariable UUID id,
                                                       @RequestBody TransitionEtudeConfigDto saisie) {
        return service.modifierTransition(id, saisie);
    }

    @DeleteMapping(TRANSITION)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ADMIN)
    @Operation(summary = "Supprime une transition, ses rôles et ses documents")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Transition supprimée."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = TRANSITION_INCONNUE)
    })
    public void supprimerTransition(@Parameter(description = ID_TRANSITION) @PathVariable UUID id) {
        service.supprimerTransition(id);
    }

    @GetMapping("/champs-obligatoires")
    @Operation(summary = "Catalogue des champs cochables (nom OMRAN, champ du CRM, libellé)")
    @ApiResponse(responseCode = "403", description = PAS_LECTEUR)
    public List<ChampObligatoire> champsObligatoires() {
        return ChampsObligatoiresFaisabilite.CATALOGUE;
    }

    @GetMapping("/choix/types-etat")
    @Operation(summary = "Types d'état (AdmWorkFlowStateType) : Nouveau, Intermédiaire, Terminé…, par ordre")
    @ApiResponse(responseCode = "403", description = PAS_LECTEUR)
    public List<ReferentielItemDto> typesEtat() {
        return service.typesEtat();
    }
}
