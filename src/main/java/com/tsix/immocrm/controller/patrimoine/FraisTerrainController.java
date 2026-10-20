package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.patrimoine.FactureFraisTerrainDto;
import com.tsix.immocrm.dto.patrimoine.LigneFraisTerrainDto;
import com.tsix.immocrm.dto.patrimoine.LignesTrancheDto;
import com.tsix.immocrm.services.patrimoine.terrain.FactureFraisTerrain;
import com.tsix.immocrm.services.patrimoine.terrain.FraisTerrainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.tsix.immocrm.controller.patrimoine.ArticleController.ADMINISTRATEUR;
import static com.tsix.immocrm.controller.patrimoine.ArticleController.REPONSE_403;
import static com.tsix.immocrm.controller.patrimoine.TerrainController.ID;
import static com.tsix.immocrm.controller.patrimoine.TerrainController.INTROUVABLE;

@Tag(name = "Frais de terrain")
@RestController
@RequestMapping("/api/terrains/{id}/frais")
public class FraisTerrainController {

    private final FraisTerrainService frais;
    private final FactureFraisTerrain factures;

    public FraisTerrainController(FraisTerrainService frais, FactureFraisTerrain factures) {
        this.frais = frais;
        this.factures = factures;
    }

    @GetMapping
    @Operation(summary = "Frais du terrain, avec la facture des frais validés")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Frais du terrain, par date."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public LignesTrancheDto<LigneFraisTerrainDto> lire(@Parameter(description = ID) @PathVariable UUID id) {
        return frais.lire(id);
    }

    @PutMapping
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Remplace la liste des frais du terrain")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste enregistrée."),
            @ApiResponse(responseCode = "400", description = "Date, montant > 0 ou type absent, référence inconnue, "
                    + "fournisseur absent du frais et du terrain."),
            @ApiResponse(responseCode = "403", description = REPONSE_403),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Frais validé modifié ou retiré de la liste.")
    })
    public LignesTrancheDto<LigneFraisTerrainDto> enregistrer(@Parameter(description = ID) @PathVariable UUID id,
                                                              @RequestBody LignesTrancheDto<LigneFraisTerrainDto> corps) {
        return frais.enregistrer(id, corps);
    }

    @PostMapping("/{fraisId}/valider")
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Valide un frais pour règlement : crée sa facture fournisseur")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Facture créée, frais validé."),
            @ApiResponse(responseCode = "400", description = "Fournisseur absent du frais et du terrain, ou type de frais "
                    + "sans catégorie comptable ou type de marché."),
            @ApiResponse(responseCode = "403", description = REPONSE_403),
            @ApiResponse(responseCode = "404", description = "Terrain introuvable, ou frais d'un autre terrain."),
            @ApiResponse(responseCode = "409", description = "Frais déjà validé ou déjà facturé."),
            @ApiResponse(responseCode = "500", description = "Configuration absente : rien n'est écrit.")
    })
    public FactureFraisTerrainDto valider(@Parameter(description = ID) @PathVariable UUID id,
                                          @Parameter(description = "Id du frais (StkExpense.Pkey).")
                                          @PathVariable UUID fraisId) {
        return factures.valider(id, fraisId);
    }
}
