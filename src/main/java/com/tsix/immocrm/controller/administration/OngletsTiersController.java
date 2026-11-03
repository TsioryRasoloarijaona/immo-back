package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.dto.administration.tiers.LigneOngletTiers;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.services.administration.tiers.OngletsTiersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Tiers (administration)")
@ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur.", content = @Content)
@RestController
@RequestMapping("/api/administration/tiers")
@PreAuthorize("hasRole('Administrateur')")
public class OngletsTiersController {

    private final OngletsTiersService service;

    public OngletsTiersController(OngletsTiersService service) {
        this.service = service;
    }

    @GetMapping("/{famille}/{id}/onglets/{onglet}")
    @Operation(summary = "Onglet de lecture de la fiche tiers, par date décroissante",
            description = "Lecture seule. Fournisseurs, contacts et personnels ; conges pour les personnels seuls.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de lignes ; leur forme dépend de l'onglet."),
            @ApiResponse(responseCode = "400", description = "page ou size hors bornes.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Famille, tiers ou onglet inconnu, ou onglet absent pour la famille.",
                    content = @Content)
    })
    public PageDto<LigneOngletTiers> lister(
            @Parameter(description = "Famille.", schema = @Schema(allowableValues = {"fournisseurs", "contacts", "personnels"}))
            @PathVariable String famille,
            @Parameter(description = "PKey du tiers.") @PathVariable UUID id,
            @Parameter(description = "Onglet.", schema = @Schema(allowableValues = {"prospections", "dossiers", "recettes",
                    "depenses", "reglements", "conges", "historique"}))
            @PathVariable String onglet,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000)
            @Parameter(description = "Numéro de page, de 0 à 1 000 000.") int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200)
            @Parameter(description = "Lignes par page, de 1 à 200.") int size) {
        return service.lister(famille, id, onglet, page, size);
    }
}
