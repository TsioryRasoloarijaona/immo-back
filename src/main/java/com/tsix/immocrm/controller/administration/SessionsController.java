package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.dto.administration.LigneSessionDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.services.administration.FiltresSessions;
import com.tsix.immocrm.services.administration.SessionsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Sessions (administration)")
@RestController
@RequestMapping("/api/administration/sessions")
@PreAuthorize("hasRole('Administrateur')")
public class SessionsController {

    private static final String PAS_ADMIN = "L'appelant n'a pas le rôle Administrateur.";

    private final SessionsService service;

    public SessionsController(SessionsService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Sessions OMRAN, page par page")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de lignes, date d'ouverture décroissante puis PKey."),
            @ApiResponse(responseCode = "400", description = "Date ou booléen illisible, dateDebut après dateFin, page ou "
                    + "size hors bornes.", content = @Content),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN, content = @Content)
    })
    public PageDto<LigneSessionDto> lister(
            @ParameterObject @ModelAttribute FiltresSessions filtres,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000)
            @Parameter(description = "Numéro de page, de 0 à 1 000 000.") int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200)
            @Parameter(description = "Lignes par page, de 1 à 200.") int size) {
        return service.lister(filtres, page, size);
    }

    @GetMapping("/utilisateurs")
    @Operation(summary = "Utilisateurs présents dans les sessions, pour la liste déroulante")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tableau de chaînes distinctes, triées, sans null ni vide."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN, content = @Content)
    })
    public List<String> utilisateurs() {
        return service.utilisateurs();
    }

    @PostMapping("/{id}/fermer")
    @Operation(summary = "Fermer une session")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session fermée, ligne à jour."),
            @ApiResponse(responseCode = "400", description = "Identifiant illisible.", content = @Content),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN, content = @Content),
            @ApiResponse(responseCode = "404", description = "Aucune session avec cet id.", content = @Content),
            @ApiResponse(responseCode = "409", description = "Session déjà fermée : rien n'est écrit.", content = @Content)
    })
    public LigneSessionDto fermer(
            @Parameter(description = "Identifiant de la session (AdmWebSessions.PKey).") @PathVariable UUID id) {
        return service.fermer(id);
    }
}
