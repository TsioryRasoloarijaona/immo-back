package com.tsix.immocrm.controller.parcours;

import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.parcours.AvancerProspectionRequest;
import com.tsix.immocrm.dto.parcours.JourneeDto;
import com.tsix.immocrm.dto.parcours.LigneTriDto;
import com.tsix.immocrm.dto.prospection.InteractionDto;
import com.tsix.immocrm.dto.prospection.ProspectionDetailDto;
import com.tsix.immocrm.services.parcours.AvancerProspectionService;
import com.tsix.immocrm.services.parcours.BoiteDeTriService;
import com.tsix.immocrm.services.parcours.JourneeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@RestController
@Tag(name = "Parcours commercial")
@ApiResponses({
        @ApiResponse(responseCode = "403", description = "Compte applicatif non identifiable ; sur `/api/journee`, `commercialId` non autorisé.")
})
public class ParcoursCommercialController {

    private static final String COMMERCIAL_ID = "Commercial (CfgTier) à consulter ; réservé aux exemptés et aux chefs (membre de leur équipe).";
    private static final String PROSPECTION_ID = "Identifiant de la pré-prospection (ComProspection.PKey).";

    private final JourneeService journeeService;
    private final BoiteDeTriService boiteDeTriService;
    private final AvancerProspectionService avancerService;

    public ParcoursCommercialController(JourneeService journeeService, BoiteDeTriService boiteDeTriService,
                                        AvancerProspectionService avancerService) {
        this.journeeService = journeeService;
        this.boiteDeTriService = boiteDeTriService;
        this.avancerService = avancerService;
    }

    @GetMapping("/api/journee")
    @Operation(summary = "« Ma journée » du commercial")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quatre blocs, éventuellement vides.")
    })
    public JourneeDto journee(
            @Parameter(description = COMMERCIAL_ID) @RequestParam(required = false) UUID commercialId) {
        return journeeService.journee(commercialId);
    }

    @GetMapping("/api/boite-de-tri")
    @Operation(summary = "Boîte de tri : mes pré-prospections ouvertes, les plus récentes d'abord")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Page de lignes, éventuellement vide.")})
    public PageDto<LigneTriDto> boiteDeTri(
            @Parameter(description = "Vrai pour lever la limite des 6 mois.", example = "false")
            @RequestParam(defaultValue = "false") boolean toutes,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(boiteDeTriService.lister(toutes, pageable));
    }

    @PostMapping("/api/boite-de-tri/{id}/injoignable")
    @Operation(summary = "« Pas joignable » : relance demain 10:00, la ligne reste dans la boîte")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Interaction créée, porteuse de la relance."),
            @ApiResponse(responseCode = "404", description = "Ligne inconnue, hors périmètre, ou d'un autre commercial."),
            @ApiResponse(responseCode = "409")
    })
    public InteractionDto injoignable(@Parameter(description = PROSPECTION_ID) @PathVariable UUID id) {
        return boiteDeTriService.marquerInjoignable(id);
    }

    @PostMapping("/api/prospections/{id}/avancer")
    @Operation(summary = "Kanban : change l'étape et pose la prochaine action, en une transaction")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prospection à jour (étape et prochaine relance)."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "404", description = "Prospection inconnue, hors périmètre, ou d'un autre commercial."),
            @ApiResponse(responseCode = "409")
    })
    public ProspectionDetailDto avancer(
            @Parameter(description = "Identifiant de la prospection (ComProspection.PKey).") @PathVariable UUID id,
            @RequestBody AvancerProspectionRequest request) {
        return avancerService.avancer(id, request);
    }
}
