package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.dto.administration.FiltresHistorique;
import com.tsix.immocrm.dto.administration.LigneHistoriqueDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.services.administration.HistoriqueService;
import com.tsix.immocrm.web.export.ColonneFixe;
import com.tsix.immocrm.web.export.ExportTableur;
import com.tsix.immocrm.web.export.FeuilleExport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Historique (administration)")
@RestController
@RequestMapping("/api/administration/historique")
@PreAuthorize("hasRole('Administrateur')")
public class HistoriqueController {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String INVALIDE = "Date illisible, dateDebut après dateFin, action inconnue, page ou size hors bornes.";
    private static final String PERIODE_INVALIDE = "Date illisible ou dateDebut après dateFin.";
    private static final String PAS_ADMIN = "L'appelant n'a pas le rôle Administrateur.";

    private static final List<ColonneFixe> COLONNES = List.of(ColonneFixe.texte("dateAction", "Date d'action"),
            ColonneFixe.texte("utilisateur", "Utilisateur"), ColonneFixe.texte("entite", "Entité"),
            ColonneFixe.texte("libelleEntite", "Libellé entité"), ColonneFixe.texte("action", "Action"),
            ColonneFixe.texte("objetId", "Id de l'objet"));

    private final HistoriqueService service;
    private final ExportTableur export;

    public HistoriqueController(HistoriqueService service, ExportTableur export) {
        this.service = service;
        this.export = export;
    }

    @GetMapping
    @Operation(summary = "Journal des actions, page par page")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de lignes, date décroissante puis PKey."),
            @ApiResponse(responseCode = "400", description = INVALIDE, content = @Content),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN, content = @Content)
    })
    public PageDto<LigneHistoriqueDto> lister(
            @ParameterObject @ModelAttribute FiltresHistorique filtres,
            @Parameter(description = "Numéro de page, à partir de 0.")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Lignes par page, de 1 à 200.")
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        return service.lister(filtres, page, size);
    }

    @GetMapping("/utilisateurs")
    @Operation(summary = "Utilisateurs présents sur la période, pour la liste déroulante")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Valeurs distinctes triées, sans null ni vide."),
            @ApiResponse(responseCode = "400", description = PERIODE_INVALIDE, content = @Content),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN, content = @Content)
    })
    public List<String> utilisateurs(
            @Parameter(description = "Premier jour inclus, ISO ; absent = dateFin moins 30 jours.", example = "2026-10-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @Parameter(description = "Dernier jour inclus, ISO ; absent = aujourd'hui.", example = "2026-10-30")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return service.utilisateurs(dateDebut, dateFin);
    }

    @GetMapping("/export")
    @Operation(summary = "Export Excel des lignes filtrées")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Classeur .xlsx.", content = @Content(mediaType = XLSX,
                    schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "400", description = INVALIDE, content = @Content),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN, content = @Content)
    })
    public ResponseEntity<byte[]> exporter(@ParameterObject @ModelAttribute FiltresHistorique filtres) {
        return export.reponseEnMemoire("historique", "Historique", List.of(new FeuilleExport(ExportTableur.FEUILLE,
                COLONNES, service.lignesExport(filtres), null)));
    }
}
