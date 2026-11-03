package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.domain.TypeExport;
import com.tsix.immocrm.dto.administration.FiltresFichiersAttachesDto;
import com.tsix.immocrm.dto.administration.LigneFichierAttacheDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.services.administration.FichiersAttachesService;
import com.tsix.immocrm.services.administration.FiltresFichiersAttaches;
import com.tsix.immocrm.services.piecejointe.PieceJointeSupport;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Fichiers attachés (administration)")
@RestController
@RequestMapping("/api/administration/fichiers-attaches")
@PreAuthorize("hasRole('Administrateur')")
public class FichiersAttachesController {

    private static final String PAS_ADMIN = "L'appelant n'a pas le rôle Administrateur.";
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final List<ColonneFixe> COLONNES = List.of(
            ColonneFixe.texte("categorieLibelle", "Objet rattaché"), ColonneFixe.texte("description", "Description"),
            ColonneFixe.texte("groupe", "Groupe"), ColonneFixe.texte("extension", "Extension"),
            ColonneFixe.texte("type", "Type"), ColonneFixe.de("avecFichier", "Fichier associé", TypeExport.BOOLEEN),
            ColonneFixe.de("creeLe", "Créé le", TypeExport.DATE), ColonneFixe.texte("creePar", "Créé par"),
            ColonneFixe.texte("modifiePar", "Modifié par"), ColonneFixe.de("modifieLe", "Modifié le", TypeExport.DATE));

    private final FichiersAttachesService service;
    private final PieceJointeSupport pieces;
    private final ExportTableur export;

    public FichiersAttachesController(FichiersAttachesService service, PieceJointeSupport pieces,
                                      ExportTableur export) {
        this.service = service;
        this.pieces = pieces;
        this.export = export;
    }

    @GetMapping
    @Operation(summary = "Fichiers attachés, page par page, du plus récent au plus ancien")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de lignes, création décroissante puis PKey croissante."),
            @ApiResponse(responseCode = "400", description = "Filtre illisible, période inversée, page ou taille hors "
                    + "bornes.", content = @Content),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN, content = @Content)
    })
    public PageDto<LigneFichierAttacheDto> lister(
            @ParameterObject @ModelAttribute FiltresFichiersAttaches filtres,
            @Parameter(description = "Numéro de page, à partir de 0.")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Lignes par page, de 1 à 200.")
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int taille) {
        return service.lister(filtres, page, taille);
    }

    @GetMapping("/filtres")
    @Operation(summary = "Valeurs des filtres : types, catégories normalisées, groupes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listes des filtres."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN, content = @Content)
    })
    public FiltresFichiersAttachesDto filtres() {
        return service.filtres();
    }

    @GetMapping("/export")
    @Operation(summary = "Export Excel des lignes filtrées, dans l'ordre de la liste")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Classeur .xlsx.", content = @Content(mediaType = XLSX,
                    schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "400", description = "Filtre illisible ou période inversée.", content = @Content),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN, content = @Content)
    })
    public ResponseEntity<byte[]> exporter(@ParameterObject @ModelAttribute FiltresFichiersAttaches filtres) {
        return export.reponseEnMemoire("fichiers-attaches", "Fichiers attachés", List.of(new FeuilleExport(
                ExportTableur.FEUILLE, COLONNES, service.lignesExport(filtres), null)));
    }

    @GetMapping("/{id}/fichier")
    @Operation(summary = "Ouvre le fichier d'une pièce (redirection vers l'URL présignée)")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirection vers l'URL présignée du fichier."),
            @ApiResponse(responseCode = "400", description = "Identifiant illisible.", content = @Content),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN, content = @Content),
            @ApiResponse(responseCode = "404", description = "Pièce inconnue ou sans fichier stocké vivant.",
                    content = @Content)
    })
    public ResponseEntity<Void> fichier(
            @Parameter(description = "Identifiant de la pièce (AdmAttachedFile.PKey).") @PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.FOUND).location(pieces.urlTelechargement(id)).build();
    }
}
