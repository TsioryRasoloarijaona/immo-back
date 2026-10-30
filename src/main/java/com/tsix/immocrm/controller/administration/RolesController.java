package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.domain.TypeExport;
import com.tsix.immocrm.dto.administration.DroitsRoleEcritureDto;
import com.tsix.immocrm.dto.administration.FiltresRoles;
import com.tsix.immocrm.dto.administration.NoeudDroitDto;
import com.tsix.immocrm.dto.administration.RoleDto;
import com.tsix.immocrm.dto.administration.RoleEcritureDto;
import com.tsix.immocrm.dto.administration.RoleLigneDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.services.administration.RolesService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Rôles (administration)")
@ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur.", content = @Content)
@RestController
@RequestMapping("/api/administration/roles")
@PreAuthorize("hasRole('Administrateur')")
public class RolesController {

    private static final String FICHE = "/{id}";
    private static final String ID = "PKey du rôle.";
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String INTROUVABLE = "Rôle inconnu.";
    private static final String VERROUILLE = "Rôle Administrateur : non modifiable, avant tout contrôle du corps.";

    private static final List<ColonneFixe> COLONNES = List.of(ColonneFixe.texte("code", "Code"),
            ColonneFixe.texte("description", "Description"), ColonneFixe.de("utilisateurs", "Utilisateurs", TypeExport.NOMBRE),
            ColonneFixe.de("droits", "Droits", TypeExport.NOMBRE));

    private final RolesService service;
    private final ExportTableur export;

    public RolesController(RolesService service, ExportTableur export) {
        this.service = service;
        this.export = export;
    }

    @GetMapping
    @Operation(summary = "Liste paginée des rôles, triée par code")
    @ApiResponse(responseCode = "200", description = "Page de rôles.")
    public PageDto<RoleLigneDto> lister(@ParameterObject @ModelAttribute FiltresRoles filtres,
                                        @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return service.lister(filtres, pageable);
    }

    @GetMapping("/export")
    @Operation(summary = "Export Excel des rôles filtrés, triés par code")
    @ApiResponse(responseCode = "200", description = "Classeur .xlsx.", content = @Content(mediaType = XLSX,
            schema = @Schema(type = "string", format = "binary")))
    public ResponseEntity<byte[]> exporter(@ParameterObject @ModelAttribute FiltresRoles filtres) {
        return export.reponseEnMemoire("roles", "Rôles", List.of(new FeuilleExport(ExportTableur.FEUILLE,
                COLONNES, service.lignesExport(filtres), null)));
    }

    @GetMapping("/arbre-droits")
    @Operation(summary = "Arbre complet des droits, trié par AdmRightEntity.Sorting")
    @ApiResponse(responseCode = "200", description = "Les nœuds de l'arbre, à plat.")
    public List<NoeudDroitDto> arbreDroits() {
        return service.arbreDroits();
    }

    @GetMapping(FICHE)
    @Operation(summary = "Fiche d'un rôle")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Le rôle."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public RoleDto detail(@Parameter(description = ID) @PathVariable UUID id) {
        return service.detail(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crée un rôle")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rôle créé."),
            @ApiResponse(responseCode = "400", description = "Code manquant ou trop long, ou description trop longue.",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Code déjà pris, casse ignorée.", content = @Content)
    })
    public RoleDto creer(@RequestBody RoleEcritureDto corps) {
        return service.creer(corps);
    }

    @PatchMapping(FICHE)
    @Operation(summary = "Modifie description et restrictions (champ absent ou null = inchangé, code ignoré)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rôle modifié."),
            @ApiResponse(responseCode = "400", description = "Description trop longue.", content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content),
            @ApiResponse(responseCode = "409", description = VERROUILLE, content = @Content)
    })
    public RoleDto modifier(@Parameter(description = ID) @PathVariable UUID id, @RequestBody RoleEcritureDto corps) {
        return service.modifier(id, corps);
    }

    @PutMapping(FICHE + "/droits")
    @Operation(summary = "Remplace les droits de l'arbre du rôle")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rôle relu après écriture."),
            @ApiResponse(responseCode = "400", description = "codes absent, ou code absent de l'arbre.", content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content),
            @ApiResponse(responseCode = "409", description = VERROUILLE, content = @Content)
    })
    public RoleDto remplacerDroits(@Parameter(description = ID) @PathVariable UUID id,
                                   @RequestBody DroitsRoleEcritureDto corps) {
        return service.remplacerDroits(id, corps);
    }

    @DeleteMapping(FICHE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprime un rôle libre et ses droits")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Rôle supprimé."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content),
            @ApiResponse(responseCode = "409", description = "Rôle Administrateur, ou rôle lié : le message nomme les liens.",
                    content = @Content)
    })
    public void supprimer(@Parameter(description = ID) @PathVariable UUID id) {
        service.supprimer(id);
    }
}
