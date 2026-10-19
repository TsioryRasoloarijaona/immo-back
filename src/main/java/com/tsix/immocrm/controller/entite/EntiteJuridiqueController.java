package com.tsix.immocrm.controller.entite;

import com.tsix.immocrm.dto.entite.EntiteJuridiqueDto;
import com.tsix.immocrm.dto.entite.EntiteJuridiqueEcritureDto;
import com.tsix.immocrm.dto.entite.LigneRepartitionDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.services.patrimoine.EntiteJuridiqueService;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

@Tag(name = "Entités juridiques", description = "Écran OMRAN « ENTITES JURIDIQUE » (CfgCompany) et répartition des parts ; "
        + "écriture Administrateur.")
@RestController
@RequestMapping("/api/entites-juridiques")
public class EntiteJuridiqueController {

    static final String ADMIN = "hasRole('Administrateur')";
    static final String PAS_ADMIN = "L'appelant n'a pas le rôle Administrateur.";
    private static final String ID_ENTITE = "Id de l'entité (CfgCompany.PKey).";
    private static final String INTROUVABLE = "Entité introuvable.";
    private static final String VERIF = "Doublon, actionnaire inconnu, ou Verif OMRAN (nombre de lignes / total des parts).";

    private static final TypeReference<List<LigneRepartitionDto>> LIGNES = new TypeReference<>() {
    };

    private final EntiteJuridiqueService service;
    private final JsonMapper json;

    public EntiteJuridiqueController(EntiteJuridiqueService service, JsonMapper json) {
        this.service = service;
        this.json = json;
    }

    @GetMapping
    @Operation(summary = "Liste des entités, actives ou non (colonnes de la liste OMRAN)")
    @ApiResponse(responseCode = "400", description = "actif ne vaut ni true, ni false, ni tous.")
    public List<EntiteJuridiqueDto> lister(
            @Parameter(description = "true, false (inactives et jamais renseignées) ou tous (défaut).")
            @RequestParam(required = false) String actif,
            @Parameter(description = "Code, libellé, matricule fiscal ou registre du commerce ; espaces et casse ignorés.")
            @RequestParam(required = false) String search) {
        return service.lister(actif, search);
    }

    @GetMapping("/devises")
    @Operation(summary = "Devises (StlCurrency) du sélecteur de la fiche, par code")
    public List<ReferentielItemDto> devises() {
        return service.devises();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fiche d'une entité, active ou non, avec sa répartition")
    @ApiResponse(responseCode = "404", description = INTROUVABLE)
    public EntiteJuridiqueDto findById(@Parameter(description = ID_ENTITE) @PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMIN)
    @Operation(summary = "Crée une entité (habilitation du créateur et 4 compteurs AdmSequence dans la même transaction)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Entité créée."),
            @ApiResponse(responseCode = "400", description = "Champ obligatoire manquant, référence inconnue, pas de devise MAD."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "409", description = "Code déjà pris par une autre entité, même inactive.")
    })
    public EntiteJuridiqueDto creer(@RequestBody EntiteJuridiqueEcritureDto saisie) {
        return service.creer(saisie);
    }

    @PatchMapping("/{id}")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Modifie une entité, Active comprise (effet immédiat sur la visibilité dans tout le CRM)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entité modifiée."),
            @ApiResponse(responseCode = "400", description = "Champ obligatoire vidé, référence inconnue, " + VERIF),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Code déjà pris par une autre entité.")
    })
    public EntiteJuridiqueDto modifier(@Parameter(description = ID_ENTITE) @PathVariable UUID id,
                                       @RequestBody EntiteJuridiqueEcritureDto saisie) {
        return service.modifier(id, saisie);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ADMIN)
    @Operation(summary = "Supprime une entité sans programme ni autre référence")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Entité supprimée avec ses compteurs, habilitations et répartition."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Un programme ou une autre table référence l'entité.")
    })
    public void supprimer(@Parameter(description = ID_ENTITE) @PathVariable UUID id) {
        service.supprimer(id);
    }

    @PutMapping("/{id}/repartition")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Remplace la répartition des parts ([] l'efface)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Répartition enregistrée, par code d'actionnaire."),
            @ApiResponse(responseCode = "400", description = VERIF),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public List<LigneRepartitionDto> remplacerRepartition(
            @Parameter(description = ID_ENTITE) @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Tableau [{actionnaireId, nombreParts}].",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = LigneRepartitionDto.class))))
            @RequestBody JsonNode corps) {
        // Corps lu en arbre : @PreAuthorize passe après la lecture, un corps non-tableau doit d'abord donner 403.
        if (!corps.isArray()) {
            throw new InvalidRequestException("Le corps doit être un tableau [{actionnaireId, nombreParts}].");
        }
        return service.remplacerRepartition(id, json.convertValue(corps, LIGNES));
    }

    @PostMapping("/{id}/repartition/depuis-groupe/{groupeId}")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Calcule la répartition depuis un groupe d'actionnaires, sans l'enregistrer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lignes calculées, à enregistrer par PUT ou PATCH."),
            @ApiResponse(responseCode = "400", description = "nombreParts négatif."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = "Entité ou groupe introuvable.")
    })
    public List<LigneRepartitionDto> depuisGroupe(
            @Parameter(description = ID_ENTITE) @PathVariable UUID id,
            @Parameter(description = "Id du groupe (CfgGroupShareholder.PKey).") @PathVariable UUID groupeId,
            @Parameter(description = "Nombre de parts saisi sur la fiche ; absent = celui enregistré.")
            @RequestParam(required = false) Integer nombreParts) {
        return service.depuisGroupe(id, groupeId, nombreParts);
    }
}
