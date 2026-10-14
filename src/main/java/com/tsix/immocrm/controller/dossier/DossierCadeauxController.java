package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.dto.dossier.cadeaux.CadeauCatalogueDto;
import com.tsix.immocrm.dto.dossier.cadeaux.CadeauDto;
import com.tsix.immocrm.dto.dossier.cadeaux.CadeauRequest;
import com.tsix.immocrm.services.dossier.cadeaux.CadeauxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Tag(name = "Dossiers — cadeaux")
@RestController
@RequestMapping("/api/dossiers/{id}/cadeaux")
public class DossierCadeauxController {

    private static final String DOSSIER = "Identifiant du dossier (ComFolder.PKey).";
    private static final String CADEAU = "Cadeau du dossier (ComFolderGift.PKey).";
    private static final String DESCRIPTION_404 = "Dossier inexistant ou hors périmètre.";
    private static final String DESCRIPTION_400 = "Livré sans date de livraison, ou cadeau / raison introuvable.";
    private static final String DESCRIPTION_403 = "Réservé aux rôles Administrateur et Commercial Chef.";
    private static final String DESCRIPTION_200 = "Cadeaux du dossier à jour.";
    /** Droit OMRAN {@code toolAddDeleteGifts}, ouvert au Chef et à l'Administrateur. */
    private static final Set<String> GESTION_CADEAUX = Set.of("ROLE_Administrateur", "ROLE_Commercial Chef");

    private final CadeauxService service;

    public DossierCadeauxController(CadeauxService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Cadeaux du dossier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cadeaux (peut être vide)."),
            @ApiResponse(responseCode = "404", description = DESCRIPTION_404)
    })
    public List<CadeauDto> lister(@Parameter(description = DOSSIER) @PathVariable UUID id) {
        return service.lister(id);
    }

    @GetMapping("/catalogue")
    @Operation(summary = "Catalogue des cadeaux, avec prix et stock")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Catalogue trié par ordre puis code."),
            @ApiResponse(responseCode = "404", description = DESCRIPTION_404)
    })
    public List<CadeauCatalogueDto> catalogue(@Parameter(description = DOSSIER) @PathVariable UUID id) {
        return service.catalogue(id);
    }

    @GetMapping("/raisons")
    @Operation(summary = "Raisons de cadeau")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Raisons triées par code."),
            @ApiResponse(responseCode = "404", description = DESCRIPTION_404)
    })
    public List<CadeauCatalogueDto> raisons(@Parameter(description = DOSSIER) @PathVariable UUID id) {
        return service.raisons(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Administrateur', 'Commercial Chef')")
    @Operation(summary = "Ajoute un cadeau au dossier")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = DESCRIPTION_200),
            @ApiResponse(responseCode = "400", description = DESCRIPTION_400),
            @ApiResponse(responseCode = "403", description = DESCRIPTION_403),
            @ApiResponse(responseCode = "404", description = DESCRIPTION_404)
    })
    public ResponseEntity<List<CadeauDto>> ajouter(@Parameter(description = DOSSIER) @PathVariable UUID id,
                                                   @RequestBody CadeauRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.ajouter(id, request));
    }

    @PutMapping("/{cadeauId}")
    @Operation(summary = "Modifie un cadeau (livraison, bon, notes ; la raison selon le droit)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = DESCRIPTION_200),
            @ApiResponse(responseCode = "400", description = DESCRIPTION_400),
            @ApiResponse(responseCode = "403", description = "Changement de raison hors Administrateur et Commercial Chef."),
            @ApiResponse(responseCode = "404", description = "Dossier ou cadeau introuvable.")
    })
    public List<CadeauDto> modifier(@Parameter(description = DOSSIER) @PathVariable UUID id,
                                    @Parameter(description = CADEAU) @PathVariable UUID cadeauId,
                                    @RequestBody CadeauRequest request, Authentication authentication) {
        return service.modifier(id, cadeauId, request, gereLesCadeaux(authentication));
    }

    @DeleteMapping("/{cadeauId}")
    @PreAuthorize("hasAnyRole('Administrateur', 'Commercial Chef')")
    @Operation(summary = "Supprime un cadeau du dossier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = DESCRIPTION_200),
            @ApiResponse(responseCode = "403", description = DESCRIPTION_403),
            @ApiResponse(responseCode = "404", description = "Dossier ou cadeau introuvable.")
    })
    public List<CadeauDto> supprimer(@Parameter(description = DOSSIER) @PathVariable UUID id,
                                     @Parameter(description = CADEAU) @PathVariable UUID cadeauId) {
        return service.supprimer(id, cadeauId);
    }

    private static boolean gereLesCadeaux(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> GESTION_CADEAUX.contains(a.getAuthority()));
    }
}
