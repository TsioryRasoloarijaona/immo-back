package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.dto.dossier.modifications.ModificationRequest;
import com.tsix.immocrm.dto.projet.modifications.FiltresModifications;
import com.tsix.immocrm.dto.projet.modifications.ModificationProjetLigneDto;
import com.tsix.immocrm.dto.projet.modifications.ReferentielsModificationsDto;
import com.tsix.immocrm.services.projet.modifications.ModificationsProjetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Projet")
@RestController
@RequestMapping("/api/projet/modifications")
public class ModificationsProjetController {

    static final String ID_MODIFICATION = "ComModification.PKey.";
    static final String INCONNUE = "Demande inconnue, d'un sous-article ou d'une tranche non habilitée.";
    static final String SANS_DROIT_LIRE = "Sans le droit OMRAN Yes.Prj.Front.Modification.ModificationList.";

    private final ModificationsProjetService service;

    public ModificationsProjetController(ModificationsProjetService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Demandes de modification des tranches habilitées, par date de demande décroissante")
    @ApiResponse(responseCode = "403", description = SANS_DROIT_LIRE, content = @Content)
    public List<ModificationProjetLigneDto> listeModificationsProjet(
            @ParameterObject @ModelAttribute FiltresModifications filtres) {
        return service.liste(filtres);
    }

    @GetMapping("/referentiels")
    @Operation(summary = "Tranches habilitées et états des demandes")
    @ApiResponse(responseCode = "403", description = SANS_DROIT_LIRE, content = @Content)
    public ReferentielsModificationsDto referentielsModifications() {
        return service.referentiels();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fiche d'une demande de modification")
    @ApiResponse(responseCode = "403", description = SANS_DROIT_LIRE, content = @Content)
    @ApiResponse(responseCode = "404", description = INCONNUE, content = @Content)
    public ModificationProjetLigneDto modificationProjet(@Parameter(description = ID_MODIFICATION) @PathVariable UUID id) {
        return service.fiche(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifie une demande ; recalcule les montants du dossier comme l'onglet du dossier")
    @ApiResponse(responseCode = "400", description = "Saisie invalide.", content = @Content)
    @ApiResponse(responseCode = "403", description = "Sans le droit OMRAN Yes.Prj.Front.Modification.ModificationList.toolNew.",
            content = @Content)
    @ApiResponse(responseCode = "404", description = INCONNUE + " Ou dossier hors périmètre commercial.",
            content = @Content)
    public ModificationProjetLigneDto modifierModificationProjet(
            @Parameter(description = ID_MODIFICATION) @PathVariable UUID id,
            @RequestBody ModificationRequest corps) {
        return service.modifier(id, corps);
    }
}
