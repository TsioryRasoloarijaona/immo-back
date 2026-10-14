package com.tsix.immocrm.controller.referentiel;

import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.services.referentiel.ReferentielService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Référentiel plat des supports, pour le filtre `origineSupportId` des prospections. */
@Tag(name = "Référentiels")
@RestController
@RequestMapping("/api/referentiels")
public class SupportsOrigineController {

    private final ReferentielService referentielService;

    public SupportsOrigineController(ReferentielService referentielService) {
        this.referentielService = referentielService;
    }

    @GetMapping("/supports-origine")
    @Operation(summary = "Tous les supports d'origine (ComProspectionOriginSupport), triés par libellé",
            description = "Toutes origines confondues ; par origine : `/origines-prospection/{origineId}/supports`.")
    public List<ReferentielItemDto> supports() {
        return referentielService.supportsOrigine();
    }
}
