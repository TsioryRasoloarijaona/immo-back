package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.dto.projet.ChoixProjetDto;
import com.tsix.immocrm.dto.projet.CritereAvancementDto;
import com.tsix.immocrm.dto.projet.CritereAvancementEcritureDto;
import com.tsix.immocrm.dto.projet.ExecutionTransitionProjetDto;
import com.tsix.immocrm.dto.projet.HistoriqueProjetDto;
import com.tsix.immocrm.dto.projet.ImportCriteresDto;
import com.tsix.immocrm.dto.projet.ProjetConfigDto;
import com.tsix.immocrm.dto.projet.TransitionProjetDto;
import com.tsix.immocrm.dto.projet.VentilationTrancheDto;
import com.tsix.immocrm.dto.projet.VentilationTrancheEcritureDto;
import com.tsix.immocrm.services.projet.OngletsProjetConfigService;
import com.tsix.immocrm.services.projet.ProjetConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Projet — configuration, onglets")
@RestController
@RequestMapping("/api/projet/config/projets/{id}")
@PreAuthorize("hasRole('Administrateur')")
public class OngletsProjetConfigController {

    private static final String ID_PROJET = "Id du projet (PrjProject.PKey).";
    private static final String ID_LIGNE = "Id de la ligne de l'onglet.";
    private static final String PAS_ADMIN = "L'appelant n'a pas le rôle Administrateur.";
    private static final String INTROUVABLE = "Projet introuvable.";
    private static final String LIGNE_INTROUVABLE = "Projet introuvable, ou ligne absente de ce projet.";
    private static final String VENTILATION_REFUSEE =
            "Tranche absente, inconnue, hors société du projet ou déjà ventilée ; taux absent ; Σ arrondie > 1.";
    private static final String CRITERE_REFUSE =
            "Description absente ou > 255 caractères ; catégorie absente, inconnue ou de recette.";
    private static final String CITE_PAR_PV = "Critère cité par un PV de réunion.";

    private final OngletsProjetConfigService service;
    private final ProjetConfigService projets;

    public OngletsProjetConfigController(OngletsProjetConfigService service, ProjetConfigService projets) {
        this.service = service;
        this.projets = projets;
    }

    @GetMapping("/choix")
    @Operation(summary = "Listes de choix du projet : tranches ventilables et lots (gestionnaire du compte prorata)")
    @ApiResponses({
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public ChoixProjetDto choix(@Parameter(description = ID_PROJET) @PathVariable UUID id) {
        return service.choix(id);
    }

    // ------------------------------------------------------------------ statut

    @GetMapping("/transitions")
    @Operation(summary = "Transitions partant du statut courant du projet")
    @ApiResponses({
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public List<TransitionProjetDto> transitions(@Parameter(description = ID_PROJET) @PathVariable UUID id) {
        return projets.transitions(id);
    }

    @PostMapping("/transition")
    @Operation(summary = "Exécute une transition de statut et écrit sa ligne d'historique")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Commentaire absent alors que la transition l'exige."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public ProjetConfigDto executerTransition(@Parameter(description = ID_PROJET) @PathVariable UUID id,
                                              @RequestBody ExecutionTransitionProjetDto saisie) {
        return projets.executerTransition(id, saisie);
    }

    @GetMapping("/historique")
    @Operation(summary = "Historique des statuts du projet (StrHistory), le plus récent d'abord")
    @ApiResponses({
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public List<HistoriqueProjetDto> historique(@Parameter(description = ID_PROJET) @PathVariable UUID id) {
        return projets.historique(id);
    }

    // ------------------------------------------------------------------ ventilation par tranches

    @GetMapping("/ventilations")
    @Operation(summary = "Ventilation du projet par tranche, par code de tranche")
    @ApiResponses({
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public List<VentilationTrancheDto> ventilations(@Parameter(description = ID_PROJET) @PathVariable UUID id) {
        return service.ventilations(id);
    }

    @PostMapping("/ventilations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ajoute une tranche à la ventilation du projet")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ligne créée."),
            @ApiResponse(responseCode = "400", description = VENTILATION_REFUSEE),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public VentilationTrancheDto ajouterVentilation(@Parameter(description = ID_PROJET) @PathVariable UUID id,
                                                    @RequestBody VentilationTrancheEcritureDto saisie) {
        return service.ajouterVentilation(id, saisie);
    }

    @PutMapping("/ventilations/{ligneId}")
    @Operation(summary = "Remplace la tranche et le taux d'une ligne de ventilation")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = VENTILATION_REFUSEE),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = LIGNE_INTROUVABLE)
    })
    public VentilationTrancheDto modifierVentilation(@Parameter(description = ID_PROJET) @PathVariable UUID id,
                                                     @Parameter(description = ID_LIGNE) @PathVariable UUID ligneId,
                                                     @RequestBody VentilationTrancheEcritureDto saisie) {
        return service.modifierVentilation(id, ligneId, saisie);
    }

    @DeleteMapping("/ventilations/{ligneId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Retire une tranche de la ventilation (suppression libre, comme OMRAN)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ligne supprimée."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = LIGNE_INTROUVABLE)
    })
    public void supprimerVentilation(@Parameter(description = ID_PROJET) @PathVariable UUID id,
                                     @Parameter(description = ID_LIGNE) @PathVariable UUID ligneId) {
        service.supprimerVentilation(id, ligneId);
    }

    // ------------------------------------------------------------------ critères d'avancement

    @GetMapping("/criteres")
    @Operation(summary = "Critères d'avancement du projet, par O.D.T.")
    @ApiResponses({
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public List<CritereAvancementDto> criteres(@Parameter(description = ID_PROJET) @PathVariable UUID id) {
        return service.criteres(id);
    }

    @PostMapping("/criteres")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ajoute un critère d'avancement")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Critère créé."),
            @ApiResponse(responseCode = "400", description = CRITERE_REFUSE),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public CritereAvancementDto ajouterCritere(@Parameter(description = ID_PROJET) @PathVariable UUID id,
                                               @RequestBody CritereAvancementEcritureDto saisie) {
        return service.ajouterCritere(id, saisie);
    }

    @PutMapping("/criteres/{ligneId}")
    @Operation(summary = "Modifie l'O.D.T., la description et la catégorie d'un critère")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = CRITERE_REFUSE),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = LIGNE_INTROUVABLE)
    })
    public CritereAvancementDto modifierCritere(@Parameter(description = ID_PROJET) @PathVariable UUID id,
                                                @Parameter(description = ID_LIGNE) @PathVariable UUID ligneId,
                                                @RequestBody CritereAvancementEcritureDto saisie) {
        return service.modifierCritere(id, ligneId, saisie);
    }

    @DeleteMapping("/criteres/{ligneId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprime un critère qu'aucun PV de réunion ne cite")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Critère supprimé."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = LIGNE_INTROUVABLE),
            @ApiResponse(responseCode = "409", description = CITE_PAR_PV)
    })
    public void supprimerCritere(@Parameter(description = ID_PROJET) @PathVariable UUID id,
                                 @Parameter(description = ID_LIGNE) @PathVariable UUID ligneId) {
        service.supprimerCritere(id, ligneId);
    }

    @PostMapping("/criteres/importer")
    @Operation(summary = "Remplace tous les critères du projet par des copies de ceux d'un autre projet")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Projet source absent, inconnu ou égal au projet importateur."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Un critère actuel du projet est cité par un PV de réunion.")
    })
    public List<CritereAvancementDto> importerCriteres(@Parameter(description = ID_PROJET) @PathVariable UUID id,
                                                       @RequestBody ImportCriteresDto saisie) {
        return service.importerCriteres(id, saisie);
    }
}
