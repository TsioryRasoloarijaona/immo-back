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

@Tag(name = "Audit", description = "Journal d'audit OMRAN (AdmWebHistorics) des programmes, tranches, articles et entités "
        + "juridiques.")
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "Journal d'un objet du patrimoine et de ses lignes enfants, plus récent d'abord")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Une page, éventuellement vide."),
            @ApiResponse(responseCode = "400", description = "`objetId` absent ou mal formé, `page` < 0, `size` hors 1..200."),
            @ApiResponse(responseCode = "404", description = "Ni entité juridique, ni programme, tranche, article visible, ni prospection terrain.")
    })
    public PageDto<LigneAuditDto> lister(
            @Parameter(description = "PKey de l'entité juridique (même inactive), du programme, de la tranche ou de l'article.")
            @RequestParam UUID objetId,
            @Parameter(description = "Page, à partir de 0.") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page, 1 à 200.") @RequestParam(defaultValue = "50") int size) {
        return auditService.lister(objetId, page, size);
    }
}
