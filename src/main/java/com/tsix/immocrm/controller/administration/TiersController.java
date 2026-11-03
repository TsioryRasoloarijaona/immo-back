package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.dto.administration.tiers.EcritureTiersRequest;
import com.tsix.immocrm.dto.administration.tiers.FicheTiersDto;
import com.tsix.immocrm.dto.administration.tiers.FiltresTiers;
import com.tsix.immocrm.dto.administration.tiers.LienTiersDto;
import com.tsix.immocrm.dto.administration.tiers.LienTiersRequest;
import com.tsix.immocrm.dto.administration.tiers.LigneTiersDto;
import com.tsix.immocrm.dto.administration.tiers.NatureTiersDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.services.administration.tiers.LiensTiersService;
import com.tsix.immocrm.services.administration.tiers.TiersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

@Tag(name = "Tiers (administration)")
@ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur.", content = @Content)
@RestController
@RequestMapping("/api/administration/tiers")
@PreAuthorize("hasRole('Administrateur')")
public class TiersController {

    private static final String LISTE = "/{famille}";
    private static final String FICHE = LISTE + "/{id}";
    private static final String CHEMIN_LIENS = "/{id}/liens";
    private static final String ID = "PKey du tiers.";
    private static final String FAMILLE_INCONNUE = "Famille inconnue.";
    private static final String INTROUVABLE = "Famille inconnue, ou tiers inconnu ou d'un autre type.";
    private static final String TIERS_INCONNU = "Tiers inconnu.";
    private static final String INVALIDE = "Champ requis manquant ou trop long, ou identifiant inconnu ; detail nomme le champ.";

    private final TiersService service;
    private final LiensTiersService liens;

    public TiersController(TiersService service, LiensTiersService liens) {
        this.service = service;
        this.liens = liens;
    }

    @GetMapping(LISTE)
    @Operation(summary = "Tiers d'une famille, par code puis PKey")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de lignes."),
            @ApiResponse(responseCode = "400", description = "natureId illisible, page ou size hors bornes.", content = @Content),
            @ApiResponse(responseCode = "404", description = FAMILLE_INCONNUE, content = @Content)
    })
    public PageDto<LigneTiersDto> lister(
            @Parameter(description = "Famille.", schema = @Schema(allowableValues = {"fournisseurs", "contacts"}))
            @PathVariable String famille,
            @ParameterObject @ModelAttribute FiltresTiers filtres,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000)
            @Parameter(description = "Numéro de page, de 0 à 1 000 000.") int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200)
            @Parameter(description = "Lignes par page, de 1 à 200.") int size) {
        return service.lister(famille, filtres, page, size);
    }

    @GetMapping(LISTE + "/natures")
    @Operation(summary = "Natures du type de la famille")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Natures, par code."),
            @ApiResponse(responseCode = "404", description = FAMILLE_INCONNUE, content = @Content)
    })
    public List<NatureTiersDto> natures(@PathVariable String famille) {
        return service.natures(famille);
    }

    @GetMapping("/referentiels/{liste}")
    @Operation(summary = "Liste de choix de la fiche")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Éléments, par rang puis code ; description vaut le code pour notes."),
            @ApiResponse(responseCode = "404", description = "Liste inconnue.", content = @Content)
    })
    public List<ReferentielItemDto> referentiel(
            @Parameter(description = "Liste.", schema = @Schema(allowableValues = {"formes-sociales", "assujettissements",
                    "notes"}))
            @PathVariable String liste) {
        return service.referentiel(liste);
    }

    @GetMapping(FICHE)
    @Operation(summary = "Fiche d'un tiers")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "La fiche."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public FicheTiersDto lire(@PathVariable String famille, @Parameter(description = ID) @PathVariable UUID id) {
        return service.lire(famille, id);
    }

    @PostMapping(LISTE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crée un tiers, code issu du compteur partagé")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tiers créé."),
            @ApiResponse(responseCode = "400", description = INVALIDE, content = @Content),
            @ApiResponse(responseCode = "404", description = FAMILLE_INCONNUE, content = @Content)
    })
    public FicheTiersDto creer(@PathVariable String famille, @RequestBody EcritureTiersRequest corps) {
        return service.creer(famille, corps);
    }

    @PutMapping(FICHE)
    @Operation(summary = "Remplace les champs de la fiche (champ omis = vidé)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tiers modifié ; code, type et auteur de création inchangés."),
            @ApiResponse(responseCode = "400", description = INVALIDE, content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public FicheTiersDto modifier(@PathVariable String famille, @Parameter(description = ID) @PathVariable UUID id,
                                  @RequestBody EcritureTiersRequest corps) {
        return service.modifier(famille, id, corps);
    }

    @DeleteMapping(FICHE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprime un tiers libre")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tiers supprimé, ses liens et actions avec lui."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content),
            @ApiResponse(responseCode = "409", description = "Tiers référencé ailleurs : rien n'est modifié.", content = @Content)
    })
    public void supprimer(@PathVariable String famille, @Parameter(description = ID) @PathVariable UUID id) {
        service.supprimer(famille, id);
    }

    @GetMapping(CHEMIN_LIENS)
    @Operation(summary = "Contacts liés à un tiers, de toute famille")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liens, du plus récent au plus ancien."),
            @ApiResponse(responseCode = "404", description = TIERS_INCONNU, content = @Content)
    })
    public List<LienTiersDto> liens(@Parameter(description = ID) @PathVariable UUID id) {
        return liens.lister(id);
    }

    @PostMapping(CHEMIN_LIENS)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Lie un contact à un tiers")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lien créé."),
            @ApiResponse(responseCode = "400", description = "contactId absent ou inconnu, relationTypeId inconnu.", content = @Content),
            @ApiResponse(responseCode = "404", description = TIERS_INCONNU, content = @Content),
            @ApiResponse(responseCode = "409", description = "Ce contact est déjà lié à ce tiers.", content = @Content)
    })
    public LienTiersDto lier(@Parameter(description = ID) @PathVariable UUID id, @RequestBody LienTiersRequest corps) {
        return liens.creer(id, corps);
    }

    @DeleteMapping(CHEMIN_LIENS +"/{lienId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Retire un lien, le contact reste")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Lien retiré."),
            @ApiResponse(responseCode = "404", description = "Tiers inconnu, ou lien inconnu ou d'un autre tiers.", content = @Content)
    })
    public void delier(@Parameter(description = ID) @PathVariable UUID id,
                       @Parameter(description = "PKey du lien.") @PathVariable UUID lienId) {
        liens.supprimer(id, lienId);
    }
}
