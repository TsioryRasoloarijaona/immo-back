package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.patrimoine.ZoneProjetDto;
import com.tsix.immocrm.dto.patrimoine.ZoneProjetEcritureDto;
import com.tsix.immocrm.services.patrimoine.ZoneProjetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Programmes", description = "Programmes immobiliers (CfgProject) : lecture, création, modification, "
        + "suppression et zones.")
@RestController
@RequestMapping("/api/projets/{id}/zones")
public class ZoneProjetController {

    private static final String ID_PROGRAMME = "Identifiant du programme (CfgProject.PKey).";
    private static final String ID_ZONE = "Identifiant de la zone (CfgProjectZone.PKey).";
    private static final String REFUS_ROLE = "L'appelant n'a pas le rôle Administrateur.";
    private static final String INTROUVABLE = "Programme ou zone introuvable.";

    private final ZoneProjetService zoneService;

    public ZoneProjetController(ZoneProjetService zoneService) {
        this.zoneService = zoneService;
    }

    @GetMapping
    @Operation(summary = "Zones du programme, à plat (parentId), triées par niveau puis ordre")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Zones (éventuellement aucune)."),
            @ApiResponse(responseCode = "404", description = "Aucun programme avec cet id.")
    })
    public List<ZoneProjetDto> findAll(@Parameter(description = ID_PROGRAMME) @PathVariable UUID id) {
        return zoneService.findAll(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Ajoute une zone au programme")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Zone créée."),
            @ApiResponse(responseCode = "400", description = "Code ou description manquant ; zone parente hors programme."),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "404", description = "Aucun programme avec cet id."),
            @ApiResponse(responseCode = "409", description = "Code déjà utilisé par une zone sœur.")
    })
    public ResponseEntity<ZoneProjetDto> create(@Parameter(description = ID_PROGRAMME) @PathVariable UUID id,
                                                @RequestBody ZoneProjetEcritureDto demande) {
        return ResponseEntity.status(HttpStatus.CREATED).body(zoneService.create(id, demande));
    }

    @PatchMapping("/{zoneId}")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Modifie une zone (champs absents inchangés)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Zone modifiée."),
            @ApiResponse(responseCode = "400", description = "Code ou description vide ; changement de zone parente."),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Code déjà utilisé par une zone sœur.")
    })
    public ZoneProjetDto update(@Parameter(description = ID_PROGRAMME) @PathVariable UUID id,
                                @Parameter(description = ID_ZONE) @PathVariable UUID zoneId,
                                @RequestBody ZoneProjetEcritureDto demande) {
        return zoneService.update(id, zoneId, demande);
    }

    @DeleteMapping("/{zoneId}")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Supprime une zone")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Zone supprimée."),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "La zone a des sous-zones ou une réclamation SAV la référence.")
    })
    public ResponseEntity<Void> delete(@Parameter(description = ID_PROGRAMME) @PathVariable UUID id,
                                       @Parameter(description = ID_ZONE) @PathVariable UUID zoneId) {
        zoneService.delete(id, zoneId);
        return ResponseEntity.noContent().build();
    }
}
