package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.dto.projet.reunions.ChoixReunionsDto;
import com.tsix.immocrm.dto.projet.reunions.FiltresReunions;
import com.tsix.immocrm.dto.projet.reunions.ReunionCreationDto;
import com.tsix.immocrm.dto.projet.reunions.ReunionFicheDto;
import com.tsix.immocrm.dto.projet.reunions.ReunionLigneDto;
import com.tsix.immocrm.dto.projet.reunions.ReunionModificationDto;
import com.tsix.immocrm.services.projet.reunions.ReunionsService;
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
@RequestMapping("/api/projet/reunions")
public class ReunionsChantierController {

    static final String ID_REUNION = "BulMeeting.PKey.";
    static final String INCONNUE = "Réunion inconnue ou de projet non habilité.";
    static final String SANS_DROIT_LIRE = "Sans le droit OMRAN Yes.Prj.Front.Bul.MeetingList.";
    static final String SANS_DROIT_MODIFIER = "Sans le droit OMRAN Yes.Prj.Front.Bul.MeetingList.toolNew.";
    private static final String SAISIE = "Champ obligatoire manquant ou date de prochaine réunion trop proche.";

    private final ReunionsService service;

    public ReunionsChantierController(ReunionsService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Réunions de chantier des projets habilités, par date décroissante")
    @ApiResponse(responseCode = "200", description = "Réunions filtrées ; vide sans projet habilité.")
    @ApiResponse(responseCode = "403", description = SANS_DROIT_LIRE, content = @Content)
    public List<ReunionLigneDto> listeReunions(@ParameterObject @ModelAttribute FiltresReunions filtres) {
        return service.liste(filtres);
    }

    @GetMapping("/choix")
    @Operation(summary = "Types de réunion, statuts de réserve et lots d'un projet")
    @ApiResponse(responseCode = "403", description = SANS_DROIT_LIRE, content = @Content)
    public ChoixReunionsDto choixReunions(
            @Parameter(description = "Projet (PrjProject.PKey) dont on veut les lots ; ignoré s'il n'est pas habilité.")
            @RequestParam(required = false) UUID projetId) {
        return service.choix(projetId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fiche d'une réunion avec ses quatre onglets")
    @ApiResponse(responseCode = "403", description = SANS_DROIT_LIRE, content = @Content)
    @ApiResponse(responseCode = "404", description = INCONNUE, content = @Content)
    public ReunionFicheDto reunion(@Parameter(description = ID_REUNION) @PathVariable UUID id) {
        return service.fiche(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crée une réunion et reporte le P.V. précédent du même projet et type")
    @ApiResponse(responseCode = "400", description = SAISIE, content = @Content)
    @ApiResponse(responseCode = "403", description = SANS_DROIT_MODIFIER, content = @Content)
    @ApiResponse(responseCode = "404", description = "Projet non habilité.", content = @Content)
    @ApiResponse(responseCode = "409", description = "Référence P.V. déjà prise.", content = @Content)
    public ReunionFicheDto creerReunion(@RequestBody ReunionCreationDto corps) {
        return service.creer(corps);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifie l'en-tête d'une réunion (projet et type non modifiables)")
    @ApiResponse(responseCode = "400", description = SAISIE, content = @Content)
    @ApiResponse(responseCode = "403", description = SANS_DROIT_MODIFIER, content = @Content)
    @ApiResponse(responseCode = "404", description = INCONNUE, content = @Content)
    @ApiResponse(responseCode = "409", description = "Référence P.V. déjà prise.", content = @Content)
    public ReunionFicheDto modifierReunion(@Parameter(description = ID_REUNION) @PathVariable UUID id,
                                           @RequestBody ReunionModificationDto corps) {
        return service.modifier(id, corps);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprime une réunion et toutes ses lignes")
    @ApiResponse(responseCode = "403", description = "Sans le droit OMRAN Yes.Prj.Front.Bul.MeetingList.toolDelete.",
            content = @Content)
    @ApiResponse(responseCode = "404", description = INCONNUE, content = @Content)
    @ApiResponse(responseCode = "409", description = "Une décision est reprise par une réunion suivante.", content = @Content)
    public void supprimerReunion(@Parameter(description = ID_REUNION) @PathVariable UUID id) {
        service.supprimer(id);
    }
}
