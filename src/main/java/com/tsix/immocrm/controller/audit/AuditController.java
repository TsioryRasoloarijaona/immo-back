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

@Tag(name = "Audit", description = "Journal d'audit OMRAN (AdmWebHistorics) : patrimoine, entités juridiques, "
        + "prospections terrain, études, rendez-vous.")
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "Journal d'un objet (patrimoine, prospection, étude, rendez-vous) et de ses lignes enfants")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Une page, éventuellement vide."),
            @ApiResponse(responseCode = "400", description = "`objetId` absent ou mal formé, `page` < 0, `size` hors 1..200."),
            @ApiResponse(responseCode = "404")
    })
    public PageDto<LigneAuditDto> lister(
            @RequestParam UUID objetId,
            @Parameter(description = "Page, à partir de 0.") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page, 1 à 200.") @RequestParam(defaultValue = "50") int size) {
        return auditService.lister(objetId, page, size);
    }
}
