package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.dto.projet.ModeleSimulationDetailDto;
import com.tsix.immocrm.dto.projet.ModeleSimulationListItemDto;
import com.tsix.immocrm.dto.projet.ModeleSimulationSaisieDto;
import com.tsix.immocrm.services.projet.ModelesSimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Projet")
@RestController
@RequestMapping("/api/projet/modeles-simulation")
public class ModelesSimulationController {

    private static final String ID_MODELE = "PrjMarketPrevisionModel.PKey.";
    private static final String INCONNU = "Modèle inconnu.";
    private static final String SANS_DROIT_LECTURE = "Ni le droit MarketPrevisionModelList ni le droit ProjectPrevision.toolNew.";
    private static final String SANS_DROIT = "Sans le droit OMRAN Yes.Prj.Front.Config.MarketPrevisionModelList.";
    private static final String SAISIE = "Code ou description vide, ou lignes invalides (message OMRAN dans detail).";
    private static final String CODE_PRIS = "Code déjà pris par un autre modèle.";

    private final ModelesSimulationService service;

    public ModelesSimulationController(ModelesSimulationService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Modèles de simulation lot, triés par code")
    @ApiResponse(responseCode = "200", description = "Modèles (id, code, description, libellé de catégorie).")
    @ApiResponse(responseCode = "403", description = SANS_DROIT_LECTURE, content = @Content)
    public List<ModeleSimulationListItemDto> listeModelesSimulation(
            @Parameter(description = "Catégorie de dépense (StlCategory.PKey) ; absent = tous les modèles.")
            @RequestParam(required = false) UUID categorieId) {
        return service.liste(categorieId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fiche d'un modèle de simulation lot")
    @ApiResponse(responseCode = "200", description = "En-tête et lignes du modèle, triées par temps.")
    @ApiResponse(responseCode = "403", description = SANS_DROIT_LECTURE, content = @Content)
    @ApiResponse(responseCode = "404", description = INCONNU, content = @Content)
    public ModeleSimulationDetailDto modeleSimulation(@Parameter(description = ID_MODELE) @PathVariable UUID id) {
        return service.detail(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crée un modèle de simulation lot")
    @ApiResponse(responseCode = "400", description = SAISIE, content = @Content)
    @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content)
    @ApiResponse(responseCode = "409", description = CODE_PRIS, content = @Content)
    public ModeleSimulationDetailDto creerModeleSimulation(@RequestBody ModeleSimulationSaisieDto corps) {
        return service.creer(corps);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifie un modèle de simulation lot (lignes remplacées)")
    @ApiResponse(responseCode = "400", description = SAISIE, content = @Content)
    @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content)
    @ApiResponse(responseCode = "404", description = INCONNU, content = @Content)
    @ApiResponse(responseCode = "409", description = CODE_PRIS, content = @Content)
    public ModeleSimulationDetailDto modifierModeleSimulation(@Parameter(description = ID_MODELE) @PathVariable UUID id,
                                                              @RequestBody ModeleSimulationSaisieDto corps) {
        return service.modifier(id, corps);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprime un modèle de simulation lot et ses lignes")
    @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content)
    @ApiResponse(responseCode = "404", description = INCONNU, content = @Content)
    @ApiResponse(responseCode = "409", description = "Modèle cité par une prévision de faisabilité.", content = @Content)
    public void supprimerModeleSimulation(@Parameter(description = ID_MODELE) @PathVariable UUID id) {
        service.supprimer(id);
    }
}
