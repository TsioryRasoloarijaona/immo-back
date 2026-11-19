package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.dto.projet.travaux.FiltresTravaux;
import com.tsix.immocrm.dto.projet.travaux.JourTravailleEcritureDto;
import com.tsix.immocrm.dto.projet.travaux.JourTravailleFicheDto;
import com.tsix.immocrm.dto.projet.travaux.JourTravailleLigneDto;
import com.tsix.immocrm.dto.projet.travaux.ReferentielsTravauxDto;
import com.tsix.immocrm.services.projet.travaux.TravauxChantierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
@RequestMapping("/api/projet/travaux-chantier")
public class TravauxChantierController {

    static final String ID_JOUR = "GrhWorkedDay.PKey.";
    static final String INCONNU = "Jour inconnu, de statut non visible ou de projet non habilité.";
    static final String SANS_DROIT_LIRE = "Sans le droit OMRAN Yes.Prj.Front.Bul.GrhWorkedDayView.";
    static final String SANS_DROIT_MODIFIER = "Sans le droit OMRAN Yes.Prj.Front.Bul.GrhWorkedDayView.toolNew.";
    private static final String SAISIE = "Champ obligatoire manquant ou référence inconnue (le détail nomme le champ).";

    private final TravauxChantierService service;

    public TravauxChantierController(TravauxChantierService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Jours travaillés visibles (statut visible, projet habilité), par date décroissante")
    @ApiResponse(responseCode = "403", description = SANS_DROIT_LIRE, content = @Content)
    public List<JourTravailleLigneDto> listeTravaux(@ParameterObject @ModelAttribute FiltresTravaux filtres) {
        return service.liste(filtres);
    }

    @GetMapping("/referentiels")
    @Operation(summary = "Types, statuts, zones, niveaux du projet, articles et personnel")
    @ApiResponse(responseCode = "403", description = SANS_DROIT_LIRE, content = @Content)
    public ReferentielsTravauxDto referentielsTravaux(
            @Parameter(description = "Projet (PrjProject.PKey) dont on veut les niveaux ; ignoré s'il n'est pas habilité.")
            @RequestParam(required = false) UUID projetId) {
        return service.referentiels(projetId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fiche d'un jour travaillé")
    @ApiResponse(responseCode = "403", description = SANS_DROIT_LIRE, content = @Content)
    @ApiResponse(responseCode = "404", description = INCONNU, content = @Content)
    public JourTravailleFicheDto jourTravaille(@Parameter(description = ID_JOUR) @PathVariable UUID id) {
        return service.fiche(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crée un jour travaillé au statut de départ (« En cours »)")
    @ApiResponse(responseCode = "400", description = SAISIE, content = @Content)
    @ApiResponse(responseCode = "403", description = "Sans le droit toolNew, ou statut de départ non visible.",
            content = @Content)
    @ApiResponse(responseCode = "404", description = "Projet non habilité.", content = @Content)
    public JourTravailleFicheDto creerJourTravaille(@RequestBody JourTravailleEcritureDto corps) {
        return service.creer(corps);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifie un jour travaillé (le statut ne change pas ici)")
    @ApiResponse(responseCode = "400", description = SAISIE, content = @Content)
    @ApiResponse(responseCode = "403", description = SANS_DROIT_MODIFIER, content = @Content)
    @ApiResponse(responseCode = "404", description = "Jour non visible, ou nouveau projet non habilité.",
            content = @Content)
    public JourTravailleFicheDto modifierJourTravaille(@Parameter(description = ID_JOUR) @PathVariable UUID id,
                                                       @RequestBody JourTravailleEcritureDto corps) {
        return service.modifier(id, corps);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprime un jour travaillé")
    @ApiResponse(responseCode = "403", description = "Sans le droit OMRAN Yes.Prj.Front.Bul.GrhWorkedDayView.toolDelete.",
            content = @Content)
    @ApiResponse(responseCode = "404", description = INCONNU, content = @Content)
    public void supprimerJourTravaille(@Parameter(description = ID_JOUR) @PathVariable UUID id) {
        service.supprimer(id);
    }
}
