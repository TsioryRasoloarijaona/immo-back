package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.dto.projet.NoeudNivellementDto;
import com.tsix.immocrm.dto.projet.NoeudNivellementEcritureDto;
import com.tsix.immocrm.services.projet.NivellementProjetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Projet — configuration, nivellement")
@RestController
@RequestMapping("/api/projet/config/projets/{id}/nivellement")
public class NivellementProjetController {

    private static final String ADMIN = "hasRole('Administrateur')";
    private static final String ID_PROJET = "Id du projet (PrjProject.PKey).";
    private static final String ID_NOEUD = "Id du nœud (CfgTrancheLevelingDefinition.PKey).";
    private static final String REFUS = "Rôle Administrateur absent.";
    private static final String INTROUVABLE = "Projet hors périmètre ou inconnu, ou nœud d'un autre projet.";

    private final NivellementProjetService service;

    public NivellementProjetController(NivellementProjetService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Nœuds de la définition de nivellement du projet, à plat (parentId), par rang")
    @ApiResponse(responseCode = "404", description = INTROUVABLE)
    public List<NoeudNivellementDto> lister(@Parameter(description = ID_PROJET) @PathVariable UUID id) {
        return service.lister(id);
    }

    @PostMapping
    @PreAuthorize(ADMIN)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ajoute une racine ou un enfant ; code et rang calculés par le serveur")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Projet sans nivellement, ou niveau plus profond que le nivellement."),
            @ApiResponse(responseCode = "403", description = REFUS),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public NoeudNivellementDto creer(@Parameter(description = ID_PROJET) @PathVariable UUID id,
                                     @RequestBody NoeudNivellementEcritureDto saisie) {
        return service.creer(id, saisie.parentId());
    }

    @PatchMapping("/{noeudId}")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Modifie le code d'un nœud (parent et rang inchangés)")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Code absent, vide ou de plus de 255 caractères."),
            @ApiResponse(responseCode = "403", description = REFUS),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public NoeudNivellementDto modifier(@Parameter(description = ID_PROJET) @PathVariable UUID id,
                                        @Parameter(description = ID_NOEUD) @PathVariable UUID noeudId,
                                        @RequestBody NoeudNivellementEcritureDto saisie) {
        return service.modifier(id, noeudId, saisie.code());
    }

    @DeleteMapping("/{noeudId}")
    @PreAuthorize(ADMIN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprime un nœud et ses descendants")
    @ApiResponses({
            @ApiResponse(responseCode = "403", description = REFUS),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Le nœud ou un descendant est cité (lot, journée, avancement).")
    })
    public void supprimer(@Parameter(description = ID_PROJET) @PathVariable UUID id,
                          @Parameter(description = ID_NOEUD) @PathVariable UUID noeudId) {
        service.supprimer(id, noeudId);
    }
}
