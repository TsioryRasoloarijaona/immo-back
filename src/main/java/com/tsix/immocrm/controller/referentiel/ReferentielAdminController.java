package com.tsix.immocrm.controller.referentiel;

import com.tsix.immocrm.dto.referentiel.DescripteurReferentielDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.services.patrimoine.ReferentielAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Référentiels (administration)")
@RestController
@RequestMapping("/api/referentiels-admin")
public class ReferentielAdminController {

    private static final String ADMIN = "hasRole('Administrateur')";
    private static final String TABLE = "/{table}";
    private static final String LIGNE = "/{table}/{id}";
    private static final String SLUG = "Slug du référentiel (voir GET /api/referentiels-admin).";
    private static final String ID = "PKey de la ligne.";
    private static final String PAS_ADMIN = "L'appelant n'a pas le rôle Administrateur.";
    private static final String INTROUVABLE = "Slug inconnu, ligne inconnue, hors écran ou d'une tranche non visible.";
    private static final String INVALIDE = "Champ inconnu, obligatoire manquant, valeur ou référence invalide.";
    private static final String CONFLIT = "Doublon de la clé d'unicité, ou code d'une ligne protégée modifié.";

    private final ReferentielAdminService service;

    public ReferentielAdminController(ReferentielAdminService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Descripteurs des référentiels administrables (champs, types, choix)")
    public List<DescripteurReferentielDto> descripteurs() {
        return service.descripteurs();
    }

    @GetMapping("/choix/{liste}")
    @Operation(summary = "Liste de choix sans route existante (catégories comptables, types de marché, statuts, règlements)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lignes triées par ordre puis code."),
            @ApiResponse(responseCode = "404", description = "Liste inconnue.")
    })
    public List<ReferentielItemDto> choix(
            @Parameter(description = "Slug de la liste, tel que donné par ChampReferentielDto.choix.",
                    example = "statuts-vente") @PathVariable String liste) {
        return service.choix(liste);
    }

    @GetMapping(TABLE)
    @Operation(summary = "Lignes d'un référentiel")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lignes visibles, clés = noms des champs du descripteur."),
            @ApiResponse(responseCode = "404", description = "Slug inconnu, ou tranche filtrée inconnue ou non visible.")
    })
    public List<Map<String, Object>> lignes(
            @Parameter(description = SLUG, example = "orientations") @PathVariable String table,
            @Parameter(description = "Texte cherché dans les valeurs textuelles (casse ignorée).")
            @RequestParam(required = false) String search,
            @Parameter(description = "Référentiel par tranche : lignes de cette tranche seulement.")
            @RequestParam(required = false) UUID trancheId,
            @Parameter(description = "Référentiel par tranche : vrai = lignes sans tranche seulement.")
            @RequestParam(required = false) Boolean global) {
        return service.lignes(table, search, trancheId, Boolean.TRUE.equals(global));
    }

    @GetMapping(LIGNE)
    @Operation(summary = "Une ligne d'un référentiel")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "La ligne."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public Map<String, Object> ligne(@Parameter(description = SLUG) @PathVariable String table,
                                     @Parameter(description = ID) @PathVariable UUID id) {
        return service.ligne(table, id);
    }

    @PostMapping(TABLE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMIN)
    @Operation(summary = "Crée une ligne")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ligne créée, relue (avec son id)."),
            @ApiResponse(responseCode = "400", description = INVALIDE),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = "Slug inconnu, ou tranche inconnue ou non visible."),
            @ApiResponse(responseCode = "409", description = CONFLIT)
    })
    public Map<String, Object> creerLigne(@Parameter(description = SLUG) @PathVariable String table,
                                     @RequestBody Map<String, Object> corps) {
        return service.creer(table, corps);
    }

    @PatchMapping(LIGNE)
    @PreAuthorize(ADMIN)
    @Operation(summary = "Modifie une ligne (champ absent = inchangé)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ligne modifiée, relue."),
            @ApiResponse(responseCode = "400", description = INVALIDE),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = CONFLIT)
    })
    public Map<String, Object> modifierLigne(@Parameter(description = SLUG) @PathVariable String table,
                                        @Parameter(description = ID) @PathVariable UUID id,
                                        @RequestBody Map<String, Object> corps) {
        return service.modifier(table, id, corps);
    }

    @DeleteMapping(LIGNE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ADMIN)
    @Operation(summary = "Supprime une ligne")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ligne supprimée (et ses lignes enfants propres)."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Ligne protégée, référencée ailleurs, ou porteuse de lignes en cascade.")
    })
    public void supprimerLigne(@Parameter(description = SLUG) @PathVariable String table,
                          @Parameter(description = ID) @PathVariable UUID id) {
        service.supprimer(table, id);
    }
}
