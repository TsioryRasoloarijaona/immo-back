package com.tsix.immocrm.controller.referentiel;

import com.tsix.immocrm.domain.ColonneTranche;
import com.tsix.immocrm.dto.referentiel.ColonneDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@Tag(name = "Référentiels")
@RestController
@RequestMapping("/api/referentiels")
public class ColonnesTrancheController {

    @GetMapping("/colonnes/tranches")
    @Operation(summary = "Catalogue des colonnes de la liste des tranches (POURCENTAGE = fraction)")
    public List<ColonneDto> getColonnesTranches() {
        return Arrays.stream(ColonneTranche.values())
                .map(c -> ColonneDto.de(c.nom(), c.libelle(), c.groupe().name(), c.type().name(),
                        c.parDefaut(), c.chemin()))
                .toList();
    }
}
