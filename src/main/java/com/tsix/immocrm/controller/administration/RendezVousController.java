package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.domain.TypeExport;
import com.tsix.immocrm.dto.administration.FiltresRendezVous;
import com.tsix.immocrm.dto.administration.RendezVousDto;
import com.tsix.immocrm.dto.administration.RendezVousEcritureDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.services.administration.RendezVousService;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Rendez-vous (administration)")
@ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur.", content = @Content)
@RestController
@RequestMapping("/api/administration/rendez-vous")
@PreAuthorize("hasRole('Administrateur')")
public class RendezVousController {

    private static final String FICHE = "/{id}";
    private static final String ID = "PKey du rendez-vous.";
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PERIODE_INVALIDE = "Date illisible, ou dateDebut après dateFin.";
    private static final String INTROUVABLE = "Rendez-vous inconnu, ou tâche.";
    private static final String INVALIDE = "Champ obligatoire manquant, fin avant début, ou référence inconnue.";

    private static final List<ColonneFixe> COLONNES = List.of(ColonneFixe.texte("code", "Code"),
            ColonneFixe.texte("description", "Description"), ColonneFixe.texte("declencheurCode", "Déclencheur Code"),
            ColonneFixe.texte("declencheurNom", "Déclencheur"), ColonneFixe.texte("datePrevue", "Date Prévisionnelle"),
            ColonneFixe.de("dureePrevue", "Durée Prévisionnelle", TypeExport.NOMBRE),
            ColonneFixe.texte("dateFinPrevue", "Date fin Prévisionnelle"),
            ColonneFixe.texte("gestionnaireCode", "Personnel Code"), ColonneFixe.texte("gestionnaireNom", "Personnel"),
            ColonneFixe.texte("tierCode", "Tier Code"), ColonneFixe.texte("tierNom", "Tier"),
            ColonneFixe.texte("sourceLibelle", "Source"),
            ColonneFixe.texte("sourceEntite", "Entité fonctionnelle de la source"), ColonneFixe.texte("notes", "Notes"));

    private final RendezVousService service;
    private final ExportTableur export;

    public RendezVousController(RendezVousService service, ExportTableur export) {
        this.service = service;
        this.export = export;
    }

    @GetMapping
    @Operation(summary = "Liste paginée des rendez-vous, triée par date prévisionnelle puis code")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de rendez-vous."),
            @ApiResponse(responseCode = "400", description = PERIODE_INVALIDE, content = @Content)
    })
    public PageDto<RendezVousDto> lister(@ParameterObject @ModelAttribute FiltresRendezVous filtres,
                                         @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return service.lister(filtres, pageable);
    }

    @GetMapping("/calendrier")
    @Operation(summary = "Rendez-vous d'une plage de 62 jours au plus, non paginés")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rendez-vous de la plage, date croissante."),
            @ApiResponse(responseCode = "400", description = "Borne absente ou illisible, fin avant début, ou plage de "
                    + "plus de 62 jours.", content = @Content)
    })
    public List<RendezVousDto> calendrier(
            @Parameter(description = "Premier jour inclus, ISO.", example = "2026-10-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @Parameter(description = "Dernier jour inclus, ISO.", example = "2026-10-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @Parameter(description = "Personnel (CfgTier).") @RequestParam(required = false) UUID gestionnaireId) {
        return service.calendrier(debut, fin, gestionnaireId);
    }

    @GetMapping("/export")
    @Operation(summary = "Export Excel des rendez-vous filtrés, date croissante")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Classeur .xlsx.", content = @Content(mediaType = XLSX,
                    schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "400", description = PERIODE_INVALIDE, content = @Content)
    })
    public ResponseEntity<byte[]> exporter(@ParameterObject @ModelAttribute FiltresRendezVous filtres) {
        return export.reponseEnMemoire("rendez-vous", "Rendez-vous", List.of(new FeuilleExport(ExportTableur.FEUILLE,
                COLONNES, service.lignesExport(filtres), null)));
    }

    @GetMapping(FICHE)
    @Operation(summary = "Fiche d'un rendez-vous")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Le rendez-vous."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public RendezVousDto detail(@Parameter(description = ID) @PathVariable UUID id) {
        return service.detail(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crée un rendez-vous, code issu de la séquence AdmTask")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rendez-vous créé."),
            @ApiResponse(responseCode = "400", description = INVALIDE, content = @Content)
    })
    public RendezVousDto creer(@RequestBody RendezVousEcritureDto corps) {
        return service.creer(corps);
    }

    @PatchMapping(FICHE)
    @Operation(summary = "Modifie un rendez-vous (champ absent ou null = inchangé)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rendez-vous modifié."),
            @ApiResponse(responseCode = "400", description = INVALIDE, content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public RendezVousDto modifier(@Parameter(description = ID) @PathVariable UUID id,
                                  @RequestBody RendezVousEcritureDto corps) {
        return service.modifier(id, corps);
    }

    @DeleteMapping(FICHE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprime un rendez-vous")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Rendez-vous supprimé."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public void supprimer(@Parameter(description = ID) @PathVariable UUID id) {
        service.supprimer(id);
    }
}
