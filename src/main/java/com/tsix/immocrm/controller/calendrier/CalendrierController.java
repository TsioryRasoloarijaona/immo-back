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

@Tag(name = "Calendrier", description = "Rendez-vous et visites planifiés lors des interactions "
        + "de prospection, exposés pour affichage dans un calendrier côté front.")
@RestController
@RequestMapping("/api/calendrier")
public class CalendrierController {

    private final CalendrierService calendrierService;

    public CalendrierController(CalendrierService calendrierService) {
        this.calendrierService = calendrierService;
    }

    @GetMapping("/evenements")
    @Operation(
            summary = "Liste les rendez-vous/visites planifiés sur une période",
            description = "Renvoie chaque entrée calendrier (AdmEvent) créée par "
                    + "POST /api/prospections/{id}/interactions lorsque le résultat était "
                    + "\"Rendez vous pris\", \"RDV reporté\" ou \"Le prospect veut faire une "
                    + "visite du projet\", dont la date de début tombe entre `debut` et `fin` "
                    + "(bornes incluses), triées par date croissante. `resultatCode` permet au "
                    + "front de distinguer un rendez-vous d'une visite sans coder de libellé en dur."
    )
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
