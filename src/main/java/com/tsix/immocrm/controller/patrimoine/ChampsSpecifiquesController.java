package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.patrimoine.champs.ChampSpecifiqueDto;
import com.tsix.immocrm.dto.patrimoine.champs.DefinitionChampDto;
import com.tsix.immocrm.dto.patrimoine.champs.EcritureValeursChampsDto;
import com.tsix.immocrm.services.patrimoine.champs.ChampsSpecifiquesService;
import com.tsix.immocrm.services.patrimoine.champs.DefinitionsChampsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

@Tag(name = "Champs spécifiques")
@RestController
@RequestMapping("/api/champs-specifiques")
public class ChampsSpecifiquesController {

    private static final String ADMIN = "hasRole('Administrateur')";
    private static final String PORTEUR = "projet, tranche, entite, article (sous-articles compris), terrain, lotissement, prospection-terrain ou etude.";
    private static final String ID_OBJET = "Id de l'objet porteur (PKey de sa table).";
    private static final String ID_CHAMP = "Id de la définition (AdmSpecificField.PKey).";
    private static final String NON_ADMIN = "L'appelant n'a pas le rôle Administrateur.";
    private static final String OBJET_INVISIBLE = "Objet inconnu ou invisible pour l'appelant (entité inactive, étude hors rôle).";
    private static final String CHAMP_INCONNU = "Définition inconnue pour ce porteur.";
    private static final String DEFINITION_REFUSEE = "Porteur inconnu, libellé vide, type inconnu, liste sans choix ou choix avec « ; ».";

    private final ChampsSpecifiquesService service;
    private final DefinitionsChampsService definitions;

    public ChampsSpecifiquesController(ChampsSpecifiquesService service, DefinitionsChampsService definitions) {
        this.service = service;
        this.definitions = definitions;
    }

    @GetMapping("/types")
    @Operation(summary = "Types de données d'un champ (AdmSpecificFieldDataType.Code), dans l'ordre OMRAN")
    public List<String> types() {
        return service.types();
    }

    @GetMapping("/{porteur}/{id}")
    @Operation(summary = "Champs spécifiques d'un objet et leurs valeurs, triés par groupe puis ordre")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Porteur inconnu.", content = @Content),
            @ApiResponse(responseCode = "404", description = OBJET_INVISIBLE, content = @Content)
    })
    public List<ChampSpecifiqueDto> lire(@Parameter(description = PORTEUR) @PathVariable String porteur,
                                         @Parameter(description = ID_OBJET) @PathVariable UUID id) {
        return service.lire(porteur, id);
    }

    @PutMapping("/{porteur}/{id}")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Enregistre des valeurs de champs spécifiques de l'objet (tout ou rien)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Champs relus après écriture."),
            @ApiResponse(responseCode = "400", description = "Valeur refusée : le message nomme le champ.", content = @Content),
            @ApiResponse(responseCode = "403", description = NON_ADMIN, content = @Content),
            @ApiResponse(responseCode = "404", description = OBJET_INVISIBLE, content = @Content)
    })
    public List<ChampSpecifiqueDto> enregistrerValeurs(@Parameter(description = PORTEUR) @PathVariable String porteur,
                                                       @Parameter(description = ID_OBJET) @PathVariable UUID id,
                                                       @RequestBody EcritureValeursChampsDto corps) {
        return service.enregistrerValeurs(porteur, id, corps);
    }

    @PostMapping("/{porteur}/definitions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMIN)
    @Operation(summary = "Crée une définition de champ pour tous les objets du porteur")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Définition créée (valeur = valeur par défaut)."),
            @ApiResponse(responseCode = "400", description = DEFINITION_REFUSEE, content = @Content),
            @ApiResponse(responseCode = "403", description = NON_ADMIN, content = @Content)
    })
    public ChampSpecifiqueDto creerDefinition(@Parameter(description = PORTEUR) @PathVariable String porteur,
                                              @RequestBody DefinitionChampDto corps) {
        return definitions.creerDefinition(porteur, corps);
    }

    @PatchMapping("/{porteur}/definitions/{champId}")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Remplace une définition de champ (corps complet, comme la création)")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = DEFINITION_REFUSEE, content = @Content),
            @ApiResponse(responseCode = "403", description = NON_ADMIN, content = @Content),
            @ApiResponse(responseCode = "404", description = CHAMP_INCONNU, content = @Content)
    })
    public ChampSpecifiqueDto modifierDefinition(@Parameter(description = PORTEUR) @PathVariable String porteur,
                                                 @Parameter(description = ID_CHAMP) @PathVariable UUID champId,
                                                 @RequestBody DefinitionChampDto corps) {
        return definitions.modifierDefinition(porteur, champId, corps);
    }

    @DeleteMapping("/{porteur}/definitions/{champId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ADMIN)
    @Operation(summary = "Supprime une définition et ses valeurs sur tous les objets du porteur")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Définition et valeurs supprimées."),
            @ApiResponse(responseCode = "403", description = NON_ADMIN, content = @Content),
            @ApiResponse(responseCode = "404", description = CHAMP_INCONNU, content = @Content)
    })
    public void supprimerDefinition(@Parameter(description = PORTEUR) @PathVariable String porteur,
                                    @Parameter(description = ID_CHAMP) @PathVariable UUID champId) {
        definitions.supprimerDefinition(porteur, champId);
    }
}
