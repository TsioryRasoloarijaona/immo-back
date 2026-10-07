package com.tsix.immocrm.controller.calendrier;

import com.tsix.immocrm.dto.calendrier.EvenementCalendrierDto;
import com.tsix.immocrm.services.calendrier.CalendrierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Calendrier")
@RestController
@RequestMapping("/api/calendrier")
public class CalendrierController {

    private final CalendrierService calendrierService;

    public CalendrierController(CalendrierService calendrierService) {
        this.calendrierService = calendrierService;
    }

    @GetMapping("/evenements")
    @Operation(summary = "Liste les rendez-vous/visites planifiés sur une période")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des événements (peut être vide)."),
            @ApiResponse(responseCode = "400", description = "`debut`/`fin` manquants, ou `fin` "
                    + "antérieure à `debut`.")
    })
    public List<EvenementCalendrierDto> evenements(
            @Parameter(description = "Début de la période (inclus), format ISO-8601.",
                    example = "2026-09-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @Parameter(description = "Fin de la période (inclus), format ISO-8601.",
                    example = "2026-09-30T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return calendrierService.evenements(debut, fin);
    }
}
