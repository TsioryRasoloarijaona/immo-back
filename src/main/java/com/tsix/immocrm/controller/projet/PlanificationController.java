package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.dto.projet.ActionPlanificationDto.ClonagePlanification;
import com.tsix.immocrm.dto.projet.ActionPlanificationDto.DateDebutPlanification;
import com.tsix.immocrm.dto.projet.ActionPlanificationDto.OrdreTachesPlanification;
import com.tsix.immocrm.dto.projet.IntervenantPlanificationDto;
import com.tsix.immocrm.dto.projet.PlanificationDetailDto;
import com.tsix.immocrm.dto.projet.PlanificationEcritureDto;
import com.tsix.immocrm.dto.projet.PlanificationListItemDto;
import com.tsix.immocrm.dto.projet.TacheEcritureDto;
import com.tsix.immocrm.dto.projet.TachePlanificationDto;
import com.tsix.immocrm.dto.projet.VersionSauvegardeeDto;
import com.tsix.immocrm.services.projet.PlanificationService;
import com.tsix.immocrm.services.projet.TachesPlanificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

@Tag(name = "Projet — planification")
@RestController
@RequestMapping("/api/projet")
public class PlanificationController {

    private static final String ID = "Id de la planification (CfgGanttProject.PKey).";
    private static final String ID_TACHE = "Id de la tâche (CfgGanttTask.PKey).";
    private static final String ID_INTERVENANT = "Id de l'intervenant (CfgGanttOwner.PKey).";
    private static final String ID_PROJET = "Id du projet (PrjProject.PKey).";
    private static final String SANS_DROIT = "Sans le droit OMRAN Yes.Cfg.Front.Gantt.GanttProject.";
    private static final String INTROUVABLE = "Planification inconnue ou d'un projet hors périmètre.";
    private static final String TACHE_INTROUVABLE = "Planification ou tâche inconnue, ou hors périmètre.";
    private static final String VERSION = "La planification est une version (lecture seule).";
    private static final String SAISIE = "Saisie invalide (message dans detail).";
    private static final String MODELES = "/planification/modeles";
    private static final String MODELE = MODELES + "/{id}";
    private static final String PLANIFICATION = "/planification/{id}";
    private static final String CHEMIN_TACHES = PLANIFICATION + "/taches";
    private static final String TACHE = CHEMIN_TACHES + "/{tacheId}";
    private static final String INTERVENANTS = PLANIFICATION + "/intervenants";
    private static final String INTERVENANT = INTERVENANTS + "/{intervenantId}";
    private static final String DU_PROJET = "/projets/{projetId}/planification";

    private final PlanificationService service;
    private final TachesPlanificationService taches;

    public PlanificationController(PlanificationService service, TachesPlanificationService taches) {
        this.service = service;
        this.taches = taches;
    }

    // ------------------------------------------------------------------ modèles

    @GetMapping(MODELES)
    @Operation(summary = "Modèles de planification, par code")
    public List<PlanificationListItemDto> listerModelesPlanification() {
        return service.listerModeles();
    }

    @PostMapping(MODELES)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crée un modèle de planification")
    @ApiResponse(responseCode = "400", description = "Code ou description absent, ou texte trop long.", content = @Content)
    @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content)
    @ApiResponse(responseCode = "409", description = "Code déjà pris par une autre planification.", content = @Content)
    public PlanificationDetailDto creerModelePlanification(@RequestBody PlanificationEcritureDto corps) {
        return service.creerModele(corps);
    }

    @PatchMapping(MODELE)
    @Operation(summary = "Modifie l'en-tête d'un modèle de planification")
    @ApiResponse(responseCode = "400", description = SAISIE, content = @Content)
    @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content)
    @ApiResponse(responseCode = "404", description = "Modèle inconnu.", content = @Content)
    @ApiResponse(responseCode = "409", description = "Code déjà pris par une autre planification.", content = @Content)
    public PlanificationDetailDto modifierModelePlanification(@Parameter(description = ID) @PathVariable UUID id,
                                                              @RequestBody PlanificationEcritureDto corps) {
        return service.modifierModele(id, corps);
    }

    @DeleteMapping(MODELE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprime un modèle, ses versions, tâches et intervenants")
    @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content)
    @ApiResponse(responseCode = "404", description = "Modèle inconnu.", content = @Content)
    public void supprimerModelePlanification(@Parameter(description = ID) @PathVariable UUID id) {
        service.supprimerModele(id);
    }

    // ------------------------------------------------------------------ détail, versions

    @GetMapping(PLANIFICATION)
    @Operation(summary = "Planification complète : en-tête, tâches, intervenants, versions")
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    public PlanificationDetailDto lirePlanification(@Parameter(description = ID) @PathVariable UUID id) {
        return service.lire(id);
    }

    @GetMapping(PLANIFICATION + "/versions")
    @Operation(summary = "Versions sauvegardées d'une planification, la plus récente d'abord")
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    public List<PlanificationListItemDto> listerVersionsPlanification(@Parameter(description = ID) @PathVariable UUID id) {
        return service.listerVersions(id);
    }

    @PostMapping(PLANIFICATION + "/versions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Sauvegarde une version datée de la planification d'un projet")
    @ApiResponse(responseCode = "403", description = "Sans le droit OMRAN Yes.Prj.Front.Tdb.ProjectPrevision.toolSaveVersion.",
            content = @Content)
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    @ApiResponse(responseCode = "409", description = "Modèle ou version : seule la planification d'un projet se versionne.",
            content = @Content)
    public VersionSauvegardeeDto sauvegarderVersionPlanification(@Parameter(description = ID) @PathVariable UUID id) {
        return service.sauvegarderVersion(id);
    }

    @PatchMapping(PLANIFICATION + "/date-debut")
    @Operation(summary = "Modifie la date de début et décale les tâches de l'écart")
    @ApiResponse(responseCode = "400", description = "Date de début absente.", content = @Content)
    @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content)
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    @ApiResponse(responseCode = "409", description = VERSION, content = @Content)
    public PlanificationDetailDto modifierDateDebutPlanification(@Parameter(description = ID) @PathVariable UUID id,
                                                                 @RequestBody DateDebutPlanification corps) {
        return taches.modifierDateDebut(id, corps.dateDebut());
    }

    // ------------------------------------------------------------------ tâches

    @PostMapping(CHEMIN_TACHES)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ajoute une tâche racine ou une sous-tâche")
    @ApiResponse(responseCode = "400", description = SAISIE, content = @Content)
    @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content)
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    @ApiResponse(responseCode = "409", description = VERSION, content = @Content)
    public PlanificationDetailDto creerTachePlanification(@Parameter(description = ID) @PathVariable UUID id,
                                                          @RequestBody TacheEcritureDto corps) {
        return taches.creerTache(id, corps);
    }

    @PatchMapping(TACHE)
    @Operation(summary = "Modifie une tâche (champs présents seulement)")
    @ApiResponse(responseCode = "400", description = SAISIE, content = @Content)
    @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content)
    @ApiResponse(responseCode = "404", description = TACHE_INTROUVABLE, content = @Content)
    @ApiResponse(responseCode = "409", description = VERSION, content = @Content)
    public PlanificationDetailDto modifierTachePlanification(@Parameter(description = ID) @PathVariable UUID id,
                                                             @Parameter(description = ID_TACHE) @PathVariable UUID tacheId,
                                                             @RequestBody TacheEcritureDto corps) {
        return taches.modifierTache(id, tacheId, corps);
    }

    @DeleteMapping(TACHE)
    @Operation(summary = "Supprime une tâche et ses descendants")
    @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content)
    @ApiResponse(responseCode = "404", description = TACHE_INTROUVABLE, content = @Content)
    @ApiResponse(responseCode = "409", description = VERSION, content = @Content)
    public PlanificationDetailDto supprimerTachePlanification(@Parameter(description = ID) @PathVariable UUID id,
                                                              @Parameter(description = ID_TACHE) @PathVariable UUID tacheId) {
        return taches.supprimerTache(id, tacheId);
    }

    @PutMapping(CHEMIN_TACHES + "/ordre")
    @Operation(summary = "Réordonne les sœurs d'un même parent")
    @ApiResponse(responseCode = "400", description = "La liste n'est pas exactement l'ensemble des sœurs d'un parent.",
            content = @Content)
    @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content)
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    @ApiResponse(responseCode = "409", description = VERSION, content = @Content)
    public PlanificationDetailDto ordonnerTachesPlanification(@Parameter(description = ID) @PathVariable UUID id,
                                                              @RequestBody OrdreTachesPlanification corps) {
        return taches.ordonnerTaches(id, corps.tacheIds());
    }

    // ------------------------------------------------------------------ intervenants

    @PostMapping(INTERVENANTS)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ajoute un intervenant à la planification")
    @ApiResponse(responseCode = "400", description = SAISIE, content = @Content)
    @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content)
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    @ApiResponse(responseCode = "409", description = VERSION, content = @Content)
    public PlanificationDetailDto creerIntervenantPlanification(@Parameter(description = ID) @PathVariable UUID id,
                                                                @RequestBody IntervenantPlanificationDto corps) {
        return taches.creerIntervenant(id, corps);
    }

    @PatchMapping(INTERVENANT)
    @Operation(summary = "Modifie un intervenant (champs présents seulement)")
    @ApiResponse(responseCode = "400", description = SAISIE, content = @Content)
    @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content)
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    @ApiResponse(responseCode = "409", description = VERSION, content = @Content)
    public PlanificationDetailDto modifierIntervenantPlanification(
            @Parameter(description = ID) @PathVariable UUID id,
            @Parameter(description = ID_INTERVENANT) @PathVariable UUID intervenantId,
            @RequestBody IntervenantPlanificationDto corps) {
        return taches.modifierIntervenant(id, intervenantId, corps);
    }

    @DeleteMapping(INTERVENANT)
    @Operation(summary = "Supprime un intervenant et le retire des tâches")
    @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content)
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    @ApiResponse(responseCode = "409", description = VERSION, content = @Content)
    public PlanificationDetailDto supprimerIntervenantPlanification(
            @Parameter(description = ID) @PathVariable UUID id,
            @Parameter(description = ID_INTERVENANT) @PathVariable UUID intervenantId) {
        return taches.supprimerIntervenant(id, intervenantId);
    }

    // ------------------------------------------------------------------ projet, lot

    @GetMapping(DU_PROJET)
    @Operation(summary = "Planification d'un projet (bouton Planification)")
    @ApiResponse(responseCode = "404", description = "Projet hors périmètre, ou code PLANIFICATION_ABSENTE.", content = @Content)
    public PlanificationDetailDto lirePlanificationProjet(@Parameter(description = ID_PROJET) @PathVariable UUID projetId) {
        return service.lireDuProjet(projetId);
    }

    @PostMapping(DU_PROJET)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crée la planification d'un projet en clonant un modèle")
    @ApiResponse(responseCode = "400", description = "Modèle absent, inconnu ou qui n'est pas un modèle.", content = @Content)
    @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content)
    @ApiResponse(responseCode = "404", description = "Projet inconnu ou hors périmètre.", content = @Content)
    @ApiResponse(responseCode = "409", description = "Le projet a déjà une planification, ou code de clone déjà pris.",
            content = @Content)
    public PlanificationDetailDto clonerPlanificationProjet(@Parameter(description = ID_PROJET) @PathVariable UUID projetId,
                                                            @RequestBody ClonagePlanification corps) {
        return service.clonerVersProjet(projetId, corps.modeleId());
    }

    @GetMapping("/lots/{lotId}/taches")
    @Operation(summary = "Tâches d'un lot (bouton Tâches), lecture seule")
    @ApiResponse(responseCode = "404", description = "Lot inconnu ou hors périmètre, ou code PLANIFICATION_ABSENTE.",
            content = @Content)
    public List<TachePlanificationDto> listerTachesLot(
            @Parameter(description = "Id du lot (PrjMarket.PKey).") @PathVariable UUID lotId) {
        return service.tachesDuLot(lotId);
    }
}
