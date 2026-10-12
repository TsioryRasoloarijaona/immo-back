package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.patrimoine.DeviseDto;
import com.tsix.immocrm.dto.patrimoine.EntiteJuridiqueDto;
import com.tsix.immocrm.dto.patrimoine.EntiteJuridiqueEcritureDto;
import com.tsix.immocrm.dto.patrimoine.LigneRepartitionEcritureDto;
import com.tsix.immocrm.services.patrimoine.EntiteJuridiqueService;
import com.tsix.immocrm.services.patrimoine.RepartitionEntiteService;
import com.tsix.immocrm.services.patrimoine.SuppressionEntiteService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Entités juridiques")
@RestController
@RequestMapping("/api/entites-juridiques")
public class EntiteJuridiqueController {

    private static final String ID = "Identifiant de l'entité juridique (CfgCompany.PKey).";
    private static final String REFUS_ROLE = "L'appelant n'a pas le rôle Administrateur.";
    private static final String INTROUVABLE = "Aucune entité avec cet id parmi celles où l'appelant est habilité.";

    private final EntiteJuridiqueService service;
    private final RepartitionEntiteService repartitionService;
    private final SuppressionEntiteService suppressionService;

    public EntiteJuridiqueController(EntiteJuridiqueService service, RepartitionEntiteService repartitionService,
                                     SuppressionEntiteService suppressionService) {
        this.service = service;
        this.repartitionService = repartitionService;
        this.suppressionService = suppressionService;
    }

    @GetMapping
    @Operation(summary = "Liste des entités juridiques où l'appelant est habilité, actives ou non")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entités, triées par code (repartition = null)."),
            @ApiResponse(responseCode = "400", description = "actif ne vaut ni true, ni false, ni tous.")
    })
    public List<EntiteJuridiqueDto> lister(
            @Parameter(description = "true = actives, false = inactives, tous (défaut) = toutes.", example = "tous")
            @RequestParam(required = false) String actif,
            @Parameter(description = "Recherche sur code, libellé, matricule fiscal, registre du commerce, sans espaces.")
            @RequestParam(required = false) String search) {
        return service.lister(actif, search);
    }

    @GetMapping("/devises")
    @Operation(summary = "Devises proposées sur la fiche, la devise par défaut d'OMRAN marquée")
    @ApiResponse(responseCode = "200", description = "Devises, triées par code.")
    public List<DeviseDto> devises() {
        return service.devises();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fiche complète d'une entité juridique, répartition comprise")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fiche."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public EntiteJuridiqueDto fiche(@Parameter(description = ID) @PathVariable UUID id) {
        return service.fiche(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Crée une entité juridique, ses 4 séquences et l'habilitation du créateur")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Entité créée."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "409", description = "Code déjà utilisé par une autre entité (champ = code).")
    })
    public ResponseEntity<EntiteJuridiqueDto> creer(@RequestBody EntiteJuridiqueEcritureDto demande) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creer(demande));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Modifie une entité juridique (champs absents inchangés), drapeau « Active » compris")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fiche modifiée."),
            @ApiResponse(responseCode = "400", description = "Mêmes causes qu'à la création ; champsAVider invalide."),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Code déjà utilisé par une autre entité (champ = code).")
    })
    public EntiteJuridiqueDto modifier(@Parameter(description = ID) @PathVariable UUID id,
                                       @RequestBody EntiteJuridiqueEcritureDto demande) {
        return service.modifier(id, demande);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Supprime une entité juridique")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Entité supprimée avec ses séquences, habilitations, répartition."),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409")
    })
    public ResponseEntity<Void> supprimer(@Parameter(description = ID) @PathVariable UUID id) {
        suppressionService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/repartition")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Remplace la répartition de l'entité")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fiche, répartition mise à jour."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public EntiteJuridiqueDto modifierRepartition(@Parameter(description = ID) @PathVariable UUID id,
                                                  @RequestBody List<LigneRepartitionEcritureDto> lignes) {
        return repartitionService.modifierRepartition(id, lignes);
    }

    @PostMapping("/{id}/repartition/depuis-groupe/{groupeId}")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Charge la répartition depuis un groupe d'actionnaires")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fiche, groupe et répartition recalculée enregistrés."),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "404", description = "Entité introuvable (voir fiche) ou groupe inconnu.")
    })
    public EntiteJuridiqueDto modifierRepartitionDepuisGroupe(
            @Parameter(description = ID) @PathVariable UUID id,
            @Parameter(description = "Groupe d'actionnaires (CfgGroupShareholder.PKey).") @PathVariable UUID groupeId) {
        return repartitionService.modifierRepartitionDepuisGroupe(id, groupeId);
    }
}
