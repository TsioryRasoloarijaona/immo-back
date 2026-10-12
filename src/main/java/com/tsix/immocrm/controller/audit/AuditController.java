package com.tsix.immocrm.controller.audit;

import com.tsix.immocrm.dto.audit.LigneAuditDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.services.audit.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Audit")
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "Journal d'audit OMRAN d'un objet et de ses lignes enfants, du plus récent au plus ancien")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de lignes (éventuellement vide)."),
            @ApiResponse(responseCode = "400", description = "objetId absent ou mal formé ; page ou size hors bornes."),
            @ApiResponse(responseCode = "404",
                    description = "objetId ne désigne ni un programme, ni une tranche, ni un article, ni une entité.")
    })
    public PageDto<LigneAuditDto> lignes(
            @Parameter(description = "PKey de l'objet (programme, tranche, article…).", required = true)
            @RequestParam UUID objetId,
            @Parameter(description = "Numéro de page, à partir de 0.", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Lignes par page, 1 à 200.", example = "50")
            @RequestParam(defaultValue = "" + AuditService.TAILLE_PAR_DEFAUT) int size) {
        return PageDto.of(auditService.lignes(objetId, page, size));
    }
}
