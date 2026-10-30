package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.dto.administration.EmailDetailDto;
import com.tsix.immocrm.dto.administration.LigneEmailDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.services.administration.EmailHistoriqueService;
import com.tsix.immocrm.services.administration.FiltresEmail;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "EMail Historique (administration)")
@RestController
@RequestMapping("/api/administration/emails")
@PreAuthorize("hasRole('Administrateur')")
public class EmailHistoriqueController {

    private static final String PAS_ADMIN = "L'appelant n'a pas le rôle Administrateur.";

    private final EmailHistoriqueService service;

    public EmailHistoriqueController(EmailHistoriqueService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Journal des e-mails, page par page")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de lignes, date décroissante puis PKey croissante."),
            @ApiResponse(responseCode = "400", description = "Date illisible, dateDebut après dateFin, page ou size "
                    + "hors bornes.", content = @Content),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN, content = @Content)
    })
    public PageDto<LigneEmailDto> lister(
            @ParameterObject @ModelAttribute FiltresEmail filtres,
            @Parameter(description = "Numéro de page, à partir de 0.")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Lignes par page, de 1 à 200.")
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        return service.lister(filtres, page, size);
    }

    @GetMapping("/utilisateurs")
    @Operation(summary = "Utilisateurs présents au journal, pour la liste déroulante")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tableau de chaînes distinctes, triées, sans null ni vide."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN, content = @Content)
    })
    public List<String> utilisateurs() {
        return service.utilisateurs();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fiche d'un e-mail")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ligne, copies et corps."),
            @ApiResponse(responseCode = "400", description = "Identifiant illisible.", content = @Content),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN, content = @Content),
            @ApiResponse(responseCode = "404", description = "Aucun e-mail avec cet id.", content = @Content)
    })
    public EmailDetailDto detail(
            @Parameter(description = "Identifiant de l'e-mail (AdmEMailHistory.PKey).") @PathVariable UUID id) {
        return service.detail(id);
    }
}
