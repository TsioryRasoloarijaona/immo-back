package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.dto.projet.CatalogueStatutLotDto;
import com.tsix.immocrm.dto.projet.StatutLotDto;
import com.tsix.immocrm.dto.projet.StatutLotEcritureDto;
import com.tsix.immocrm.dto.projet.TransitionLotEcritureDto;
import com.tsix.immocrm.dto.projet.TransitionStatutLotDto;
import com.tsix.immocrm.services.projet.EcritureStatutsLotService;
import com.tsix.immocrm.services.projet.LectureStatutsLot;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

@Tag(name = "Projet — statuts et transitions de lot")
@RestController
@RequestMapping("/api/projet/config")
public class EcritureStatutsLotController {

    private static final String ID_STATUT = "StrStatus.PKey.";
    private static final String ID_TRANSITION = "StrWorkFlow.PKey.";
    private static final String SAISIE = "Champ absent, trop long ou inconnu ; le message le nomme.";
    private static final String SANS_DROIT_STATUTS = "Sans le droit OMRAN Yes.Prj.Front.Config.MarketStatusList.";
    private static final String SANS_DROIT_TRANSITIONS = "Sans le droit OMRAN Yes.Prj.Front.Config.MarketWorkFlowList.";
    private static final String STATUT_INCONNU = "Statut inconnu.";
    private static final String TRANSITION_INCONNUE = "Transition inconnue.";
    private static final String COUPLE_PRIS = "Même départ et même arrivée déjà dans ce genre.";

    private final LectureStatutsLot lecture;
    private final EcritureStatutsLotService service;

    public EcritureStatutsLotController(LectureStatutsLot lecture, EcritureStatutsLotService service) {
        this.lecture = lecture;
        this.service = service;
    }

    @GetMapping("/statuts-lot")
    @Operation(summary = "Statuts des 5 genres de lot, par genre puis par ordre (tout authentifié)")
    public List<StatutLotDto> statutsLot() {
        return lecture.statutsLot();
    }

    @GetMapping("/statuts-lot/catalogue")
    @Operation(summary = "Listes de choix de la fiche statut pour un genre de lot (tout authentifié)")
    @ApiResponse(responseCode = "404", description = "Genre inconnu ou qui n'est pas un genre de lot.", content = @Content)
    public CatalogueStatutLotDto catalogue(
            @Parameter(description = "Genre de lot (StrEntity.PKey).") @RequestParam UUID genreId) {
        return lecture.catalogue(genreId);
    }

    @PostMapping("/statuts-lot")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crée un statut de lot (fiche OMRAN « Statuts des lots »)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Le statut créé, tel que la liste le rend."),
            @ApiResponse(responseCode = "400", description = SAISIE, content = @Content),
            @ApiResponse(responseCode = "403", description = SANS_DROIT_STATUTS, content = @Content),
            @ApiResponse(responseCode = "409", description = "Code déjà pris dans le genre.", content = @Content)})
    public StatutLotDto creerStatut(@RequestBody StatutLotEcritureDto corps) {
        return service.creerStatut(corps);
    }

    @PutMapping("/statuts-lot/{id}")
    @Operation(summary = "Remplace la fiche entière d'un statut de lot ; le genre ne change pas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Le statut à jour, tel que la liste le rend."),
            @ApiResponse(responseCode = "400", description = SAISIE, content = @Content),
            @ApiResponse(responseCode = "403", description = SANS_DROIT_STATUTS, content = @Content),
            @ApiResponse(responseCode = "404", description = STATUT_INCONNU, content = @Content),
            @ApiResponse(responseCode = "409", description = "Code déjà pris par un autre statut du genre.", content = @Content)})
    public StatutLotDto modifierStatut(@Parameter(description = ID_STATUT) @PathVariable UUID id,
                                       @RequestBody StatutLotEcritureDto corps) {
        return service.modifierStatut(id, corps);
    }

    @DeleteMapping("/statuts-lot/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprime un statut de lot inutilisé, avec ses champs obligatoires")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Statut supprimé."),
            @ApiResponse(responseCode = "403", description = SANS_DROIT_STATUTS, content = @Content),
            @ApiResponse(responseCode = "404", description = STATUT_INCONNU, content = @Content),
            @ApiResponse(responseCode = "409", description = "Statut cité ; le message donne les nombres.", content = @Content)})
    public void supprimerStatut(@Parameter(description = ID_STATUT) @PathVariable UUID id) {
        service.supprimerStatut(id);
    }

    @GetMapping("/transitions-lot")
    @Operation(summary = "Transitions de statut des 5 genres de lot, par genre puis ordre (tout authentifié)")
    public List<TransitionStatutLotDto> transitions() {
        return lecture.transitions();
    }

    @PostMapping("/transitions-lot")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crée une transition de statut de lot, avec ses rôles")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "La transition créée, telle que la liste la rend."),
            @ApiResponse(responseCode = "400", description = SAISIE, content = @Content),
            @ApiResponse(responseCode = "403", description = SANS_DROIT_TRANSITIONS, content = @Content),
            @ApiResponse(responseCode = "409", description = COUPLE_PRIS, content = @Content)})
    public TransitionStatutLotDto creerTransition(@RequestBody TransitionLotEcritureDto corps) {
        return service.creerTransition(corps);
    }

    @PutMapping("/transitions-lot/{id}")
    @Operation(summary = "Remplace une transition de statut de lot, rôles compris")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "La transition à jour, telle que la liste la rend."),
            @ApiResponse(responseCode = "400", description = SAISIE, content = @Content),
            @ApiResponse(responseCode = "403", description = SANS_DROIT_TRANSITIONS, content = @Content),
            @ApiResponse(responseCode = "404", description = TRANSITION_INCONNUE, content = @Content),
            @ApiResponse(responseCode = "409", description = COUPLE_PRIS, content = @Content)})
    public TransitionStatutLotDto modifierTransition(@Parameter(description = ID_TRANSITION) @PathVariable UUID id,
                                                     @RequestBody TransitionLotEcritureDto corps) {
        return service.modifierTransition(id, corps);
    }

    @DeleteMapping("/transitions-lot/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprime une transition de lot et ses liens de rôles ; l'historique des lots est gardé")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Transition supprimée."),
            @ApiResponse(responseCode = "403", description = SANS_DROIT_TRANSITIONS, content = @Content),
            @ApiResponse(responseCode = "404", description = TRANSITION_INCONNUE, content = @Content)})
    public void supprimerTransition(@Parameter(description = ID_TRANSITION) @PathVariable UUID id) {
        service.supprimerTransition(id);
    }
}
