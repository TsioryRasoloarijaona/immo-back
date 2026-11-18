package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.services.patrimoine.ReferentielAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Les trois référentiels de configuration des réunions, servis par le moteur {@code ReferentielAdmin} (droit par slug). */
@Tag(name = "Projet — configuration des réunions de chantier",
        description = "Types des réunions, statuts des réserves, plages des couleurs ; mêmes lignes que /api/referentiels-admin/{slug}.")
@RestController
@RequestMapping("/api/projet/config")
public class ConfigurationReunionsController {

    private static final String TABLE = "/{table:types-reunion|statuts-reserves|plages-couleurs}";
    private static final String LIGNE = TABLE + "/{id}";
    private static final String SLUG = "Référentiel : types-reunion, statuts-reserves ou plages-couleurs.";
    private static final String ID = "PKey de la ligne.";
    private static final String SANS_DROIT = "Sans le droit OMRAN de l'écran (Yes.Prj.Front.Config.*).";
    private static final String INVALIDE = "Champ inconnu, obligatoire manquant (R2) ou valeur invalide.";
    private static final String INTROUVABLE = "Ligne inconnue.";

    private final ReferentielAdminService service;

    public ConfigurationReunionsController(ReferentielAdminService service) {
        this.service = service;
    }

    @GetMapping(TABLE)
    @Operation(summary = "Lignes d'un référentiel de réunions, clés = champs du descripteur (GET /api/referentiels-admin)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lignes triées par ordre puis code."),
            @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content)
    })
    public List<Map<String, Object>> lignes(@Parameter(description = SLUG, schema = @Schema(allowableValues = {
            "types-reunion", "statuts-reserves", "plages-couleurs"})) @PathVariable String table) {
        return service.lignes(table, null, null, false);
    }

    @PostMapping(TABLE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crée une ligne")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ligne créée, relue (avec son id)."),
            @ApiResponse(responseCode = "400", description = INVALIDE),
            @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content),
            @ApiResponse(responseCode = "409", description = "Code déjà utilisé (types, statuts).")
    })
    public Map<String, Object> creer(@Parameter(description = SLUG) @PathVariable String table,
                                     @RequestBody Map<String, Object> corps) {
        return service.creer(table, corps);
    }

    @PutMapping(LIGNE)
    @Operation(summary = "Modifie une ligne (champ absent = inchangé)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ligne modifiée, relue."),
            @ApiResponse(responseCode = "400", description = INVALIDE),
            @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Code déjà utilisé (types, statuts).")
    })
    public Map<String, Object> modifier(@Parameter(description = SLUG) @PathVariable String table,
                                        @Parameter(description = ID) @PathVariable UUID id,
                                        @RequestBody Map<String, Object> corps) {
        return service.modifier(table, id, corps);
    }

    @DeleteMapping(LIGNE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprime une ligne (R7)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ligne supprimée."),
            @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Type cité par une réunion, ou statut cité par une décision.")
    })
    public void supprimer(@Parameter(description = SLUG) @PathVariable String table,
                          @Parameter(description = ID) @PathVariable UUID id) {
        service.supprimer(table, id);
    }
}
