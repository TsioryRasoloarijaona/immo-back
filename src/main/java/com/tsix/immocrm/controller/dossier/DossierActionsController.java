package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.dto.dossier.actions.ActionDossierDto;
import com.tsix.immocrm.dto.dossier.actions.ActionsDossierDto;
import com.tsix.immocrm.dto.dossier.actions.CreerActionDossierRequest;
import com.tsix.immocrm.dto.dossier.actions.CreerInstructionRequest;
import com.tsix.immocrm.dto.dossier.actions.InstructionCreeeDto;
import com.tsix.immocrm.services.dossier.fiche.DossierActionsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Dossiers — actions")
@RestController
@RequestMapping("/api/dossiers/{id}")
public class DossierActionsController {

    private static final String ID_DOSSIER = "Identifiant du dossier (ComFolder.PKey).";
    private static final String DOSSIER_404 = "Dossier inexistant, hors périmètre ou d'une entité inactive.";

    private final DossierActionsService actionsService;

    public DossierActionsController(DossierActionsService actionsService) {
        this.actionsService = actionsService;
    }

    @GetMapping("/actions")
    @Operation(summary = "Actions du dossier et leur nombre par type")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actions (liste éventuellement vide) et compteurs."),
            @ApiResponse(responseCode = "404", description = DOSSIER_404)
    })
    public ActionsDossierDto lister(
            @Parameter(description = ID_DOSSIER)
            @PathVariable UUID id) {
        return actionsService.lister(id);
    }

    @PostMapping("/actions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ajoute une action au dossier (GEN0005)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Action créée."),
            @ApiResponse(responseCode = "400", description = "typeId absent ou inconnu, note vide, ou type « Instruction »."),
            @ApiResponse(responseCode = "404", description = DOSSIER_404),
            @ApiResponse(responseCode = "409", description = "Dossier sans statut (ou statut inconnu) : catégorie indéterminée.")
    })
    public ActionDossierDto creer(
            @Parameter(description = ID_DOSSIER)
            @PathVariable UUID id,
            @RequestBody CreerActionDossierRequest request) {
        return actionsService.creer(id, request);
    }

    @PostMapping("/instructions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('Administrateur', 'Commercial Chef', 'Direction Générale')")
    @Operation(summary = "Adresse une instruction au commercial du dossier, avec un mail")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Instruction créée ; emailEnvoye dit si le mail est parti."),
            @ApiResponse(responseCode = "400", description = "note vide."),
            @ApiResponse(responseCode = "403", description = "Rôle requis : Administrateur, Commercial Chef ou Direction Générale."),
            @ApiResponse(responseCode = "404", description = DOSSIER_404),
            @ApiResponse(responseCode = "409", description = "Dossier sans commercial ou sans statut.")
    })
    public InstructionCreeeDto creerInstruction(
            @Parameter(description = ID_DOSSIER)
            @PathVariable UUID id,
            @RequestBody CreerInstructionRequest request) {
        return actionsService.creerInstruction(id, request);
    }

    @PatchMapping("/instructions/{instructionId}/lue")
    @Operation(summary = "Marque une instruction comme lue par son destinataire")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Instruction marquée lue (idempotent)."),
            @ApiResponse(responseCode = "403", description = "L'appelant n'est ni le commercial destinataire ni Administrateur."),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable, ou pas une instruction de ce dossier.")
    })
    public ActionDossierDto marquerLue(
            @Parameter(description = ID_DOSSIER)
            @PathVariable UUID id,
            @Parameter(description = "Identifiant de l'instruction (ComActionMessage.PKey).")
            @PathVariable UUID instructionId) {
        return actionsService.marquerInstructionLue(id, instructionId);
    }
}
