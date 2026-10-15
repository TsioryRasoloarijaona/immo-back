package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.dto.dossier.AffecterReglementRequest;
import com.tsix.immocrm.dto.dossier.CreerReglementDossierRequest;
import com.tsix.immocrm.dto.dossier.ReglementDossierDto;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.dossier.ReglementsDossierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Dossiers — règlements")
@RestController
@RequestMapping("/api/dossiers/{id}/reglements")
public class DossierReglementsController {

    private static final String DOSSIER = "Identifiant du dossier (ComFolder.PKey).";

    private final ReglementsDossierService reglementsService;

    public DossierReglementsController(ReglementsDossierService reglementsService) {
        this.reglementsService = reglementsService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Enregistre un règlement réparti sur une ou plusieurs échéances du dossier")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Règlement enregistré au statut `IN_WORK`, avec ses lignes."),
            @ApiResponse(responseCode = "400", description = "En-tête invalide, répartition vide ou invalide, "
                    + "ligne au-delà du reste dû, ou somme au-delà du montant."),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable ou hors périmètre.")
    })
    public ReglementDossierDto creer(@Parameter(description = DOSSIER) @PathVariable UUID id,
                                     @RequestBody CreerReglementDossierRequest request,
                                     @AuthenticationPrincipal AuthenticatedUser principal) {
        return reglementsService.creer(id, request, principal);
    }

    @GetMapping
    @Operation(summary = "Règlements du dossier avec leur part affectée et leur reste à affecter")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste, vide si aucun règlement n'a de ligne sur ce dossier."),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable ou hors périmètre.")
    })
    public List<ReglementDossierDto> lister(@Parameter(description = DOSSIER) @PathVariable UUID id) {
        return reglementsService.lister(id);
    }

    @PostMapping("/{reglementId}/affectations")
    @Operation(summary = "Affecte le reste d'un règlement à d'autres échéances du dossier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lignes ajoutées ; le règlement à jour est renvoyé."),
            @ApiResponse(responseCode = "400", description = "Répartition vide ou invalide, échéance déjà dans ce "
                    + "règlement, ligne au-delà du reste dû, ou somme au-delà du reste à affecter."),
            @ApiResponse(responseCode = "404", description = "Dossier ou règlement introuvable, ou règlement sans "
                    + "ligne sur ce dossier."),
            @ApiResponse(responseCode = "409", description = "Règlement annulé (`CANCELED`).")
    })
    public ReglementDossierDto affecter(
            @Parameter(description = DOSSIER) @PathVariable UUID id,
            @Parameter(description = "Identifiant du règlement (StlSettlement.PKey), tel que renvoyé par `ReglementDossierDto.id`.")
            @PathVariable UUID reglementId,
            @RequestBody AffecterReglementRequest request) {
        return reglementsService.affecter(id, reglementId, request);
    }
}
