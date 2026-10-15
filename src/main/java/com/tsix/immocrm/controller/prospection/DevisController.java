package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.dto.prospection.DevisPageDto;
import com.tsix.immocrm.dto.prospection.FicheDevisDto;
import com.tsix.immocrm.dto.prospection.ModifierDevisRequest;
import com.tsix.immocrm.services.prospection.DevisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

/** Offres de toutes les prospections ; celles d'une seule : {@code GET /api/prospections/{id}/devis}. */
@Tag(name = "Devis")
@RestController
@RequestMapping("/api/devis")
public class DevisController {

    private static final String ID_OFFRE = "Id de l'offre (StlQuote.id).";
    private static final String OFFRE_INVISIBLE = "Offre inconnue ou hors du périmètre commercial.";

    private final DevisService devisService;

    public DevisController(DevisService devisService) {
        this.devisService = devisService;
    }

    @GetMapping
    @Operation(
            summary = "Liste paginée de toutes les offres (devis), tous prospections confondues"
    )
    public DevisPageDto findAll(
            @Parameter(description = "Recherche (casse ignorée) : code de l'offre, code/désignation de l'article, prospect, code de la prospection.")
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<UUID> trancheId,
            @Parameter(description = "Id du commercial (CfgTier.id, cfgCommercial).")
            @RequestParam(required = false) UUID commercial,
            @Parameter(description = "Borne inférieure (incluse) sur dateDocument, format ISO-8601.",
                    example = "2026-01-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @Parameter(description = "Borne supérieure (incluse) sur dateDocument, format ISO-8601.",
                    example = "2026-12-31T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return DevisPageDto.of(devisService.findAll(search, trancheId, commercial, dateDebut, dateFin, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fiche d'une offre : en-tête, montants, sous-articles, schéma, dossier lié, audit")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offre trouvée."),
            @ApiResponse(responseCode = "404", description = OFFRE_INVISIBLE,
                    content = @Content)
    })
    public FicheDevisDto findById(@Parameter(description = ID_OFFRE) @PathVariable UUID id) {
        return devisService.findFiche(id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Modifie une offre (champs fournis seulement) et renvoie la fiche à jour")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offre modifiée, fiche relue."),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "404", description = OFFRE_INVISIBLE,
                    content = @Content)
    })
    public FicheDevisDto modifier(@Parameter(description = ID_OFFRE) @PathVariable UUID id,
                                  @RequestBody ModifierDevisRequest request) {
        return devisService.modifierDevis(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprime une offre, ses sous-articles et son schéma de financement")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Offre supprimée."),
            @ApiResponse(responseCode = "404", description = OFFRE_INVISIBLE,
                    content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)
    })
    public void supprimer(@Parameter(description = ID_OFFRE) @PathVariable UUID id) {
        devisService.supprimerDevis(id);
    }
}
