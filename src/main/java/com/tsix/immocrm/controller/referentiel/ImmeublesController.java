package com.tsix.immocrm.controller.referentiel;

import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.services.referentiel.ReferentielService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Référentiels")
@RestController
@RequestMapping("/api/referentiels")
public class ImmeublesController {

    private final ReferentielService referentielService;

    public ImmeublesController(ReferentielService referentielService) {
        this.referentielService = referentielService;
    }

    @GetMapping("/immeubles")
    @Operation(summary = "Dropdown \"Immeuble\" (StkBuilding) du formulaire d'article, trié par Sorting puis code")
    public List<ReferentielItemDto> immeubles() {
        return referentielService.immeubles();
    }
}
