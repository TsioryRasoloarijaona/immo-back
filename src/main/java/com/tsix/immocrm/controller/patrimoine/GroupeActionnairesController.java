package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.patrimoine.GroupeActionnairesDto;
import com.tsix.immocrm.dto.patrimoine.GroupeActionnairesEcritureDto;
import com.tsix.immocrm.services.patrimoine.GroupeActionnairesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Groupes d'actionnaires", description = "Groupes d'actionnaires (CfgGroupShareholder et lignes) : "
        + "modèles de répartition chargés sur une entité juridique.")
@RestController
@RequestMapping("/api/groupes-actionnaires")
public class GroupeActionnairesController {

    private static final String ID = "Identifiant du groupe (CfgGroupShareholder.PKey).";
    private static final String REFUS_ROLE = "L'appelant n'a pas le rôle Administrateur.";
    private static final String INTROUVABLE = "Aucun groupe d'actionnaires avec cet id.";
    private static final String CODE_PRIS = "Code déjà utilisé par un autre groupe (champ = code).";

    private final GroupeActionnairesService service;

    public GroupeActionnairesController(GroupeActionnairesService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Liste des groupes d'actionnaires, triée par code, avec leurs lignes")
    @ApiResponse(responseCode = "200", description = "Groupes (éventuellement aucun).")
    public List<GroupeActionnairesDto> lister(
            @Parameter(description = "Recherche sur code et libellé, espaces ignorés des deux côtés, casse ignorée.")
            @RequestParam(required = false) String search) {
        return service.lister(search);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fiche d'un groupe d'actionnaires")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Groupe et ses lignes."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public GroupeActionnairesDto fiche(@Parameter(description = ID) @PathVariable UUID id) {
        return service.fiche(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Crée un groupe d'actionnaires et ses lignes (Σ = 100 %)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Groupe créé."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "409", description = CODE_PRIS)
    })
    public ResponseEntity<GroupeActionnairesDto> creer(@RequestBody GroupeActionnairesEcritureDto demande) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creer(demande));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Modifie un groupe ; lignes présentes = remplacent toutes les lignes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Groupe modifié."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = CODE_PRIS)
    })
    public GroupeActionnairesDto modifier(@Parameter(description = ID) @PathVariable UUID id,
                                         @RequestBody GroupeActionnairesEcritureDto demande) {
        return service.modifier(id, demande);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Supprime un groupe et ses lignes")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Groupe supprimé."),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Une entité juridique utilise ce groupe (la base la supprimerait).")
    })
    public ResponseEntity<Void> supprimer(@Parameter(description = ID) @PathVariable UUID id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
