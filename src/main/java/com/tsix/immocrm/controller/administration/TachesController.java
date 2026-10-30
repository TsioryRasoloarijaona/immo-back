package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.dto.administration.ChangerStatutTacheRequest;
import com.tsix.immocrm.dto.administration.FiltresTaches;
import com.tsix.immocrm.dto.administration.StatutTacheDto;
import com.tsix.immocrm.dto.administration.TacheDto;
import com.tsix.immocrm.dto.administration.TacheEcritureDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.services.administration.TachesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Tâches (administration)")
@ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur.", content = @Content)
@RestController
@RequestMapping("/api/administration/taches")
@PreAuthorize("hasRole('Administrateur')")
public class TachesController {

    private static final String FICHE = "/{id}";
    private static final String ID = "PKey de la tâche.";
    private static final String INTROUVABLE = "Tâche inconnue, ou rendez-vous.";
    private static final String INVALIDE = "Description manquante ou trop longue, ou référence inconnue.";

    private final TachesService service;

    public TachesController(TachesService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Liste paginée des tâches, triée par date prévisionnelle")
    public PageDto<TacheDto> lister(@ParameterObject @ModelAttribute FiltresTaches filtres,
                                    @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return service.lister(filtres, pageable);
    }

    @GetMapping("/statuts")
    @Operation(summary = "Statuts de tâche, triés, avec leurs transitions")
    public List<StatutTacheDto> statuts() {
        return service.statuts();
    }

    @GetMapping("/modules")
    @Operation(summary = "Modules (AdmModule), sélecteur de la fiche, triés par code")
    public List<ReferentielItemDto> modules() {
        return service.modules();
    }

    @GetMapping(FICHE)
    @Operation(summary = "Fiche d'une tâche")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "La tâche."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public TacheDto detail(@Parameter(description = ID) @PathVariable UUID id) {
        return service.detail(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crée une tâche")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tâche créée, statut Non commencée."),
            @ApiResponse(responseCode = "400", description = INVALIDE, content = @Content)
    })
    public TacheDto creer(@RequestBody TacheEcritureDto corps) {
        return service.creer(corps);
    }

    @PatchMapping(FICHE)
    @Operation(summary = "Modifie une tâche (champ absent ou null = inchangé)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tâche modifiée."),
            @ApiResponse(responseCode = "400", description = INVALIDE, content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public TacheDto modifier(@Parameter(description = ID) @PathVariable UUID id, @RequestBody TacheEcritureDto corps) {
        return service.modifier(id, corps);
    }

    @DeleteMapping(FICHE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprime une tâche (son historique de statuts est conservé)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tâche supprimée."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public void supprimer(@Parameter(description = ID) @PathVariable UUID id) {
        service.supprimer(id);
    }

    @PostMapping(FICHE + "/statut")
    @Operation(summary = "Passe une tâche à un autre statut")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tâche au nouveau statut."),
            @ApiResponse(responseCode = "400", description = "statutId absent, ou commentaire exigé manquant.",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content),
            @ApiResponse(responseCode = "409", description = "Transition non permise ; transitionsAutorisees liste les "
                    + "codes des statuts permis.", content = @Content)
    })
    public TacheDto changerStatut(@Parameter(description = ID) @PathVariable UUID id,
                                  @RequestBody ChangerStatutTacheRequest corps) {
        return service.changerStatut(id, corps);
    }
}
