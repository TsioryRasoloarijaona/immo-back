package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.patrimoine.EtageTrancheDto;
import com.tsix.immocrm.dto.patrimoine.EtageTrancheEcritureDto;
import com.tsix.immocrm.dto.patrimoine.FraisTrancheDto;
import com.tsix.immocrm.dto.patrimoine.FraisTrancheEcritureDto;
import com.tsix.immocrm.dto.patrimoine.LignesTrancheDto;
import com.tsix.immocrm.dto.patrimoine.PlancherCouvertDto;
import com.tsix.immocrm.dto.patrimoine.PlancherCouvertEcritureDto;
import com.tsix.immocrm.dto.patrimoine.TerrainDisponibleDto;
import com.tsix.immocrm.dto.patrimoine.TerrainTrancheDto;
import com.tsix.immocrm.dto.patrimoine.TerrainTrancheEcritureDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.services.patrimoine.EtagesTrancheService;
import com.tsix.immocrm.services.patrimoine.FraisTrancheService;
import com.tsix.immocrm.services.patrimoine.PlancherCouvertService;
import com.tsix.immocrm.services.patrimoine.TerrainsTrancheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Onglets de tranche", description = "Listes enfants de la fiche tranche : terrains, étages, plancher couvert, frais.")
@RestController
@RequestMapping("/api/tranches")
public class OngletsTrancheController {

    static final String ID_TRANCHE = "Identifiant de la tranche (CfgTranche.PKey).";
    static final String REFUS_ROLE = "L'appelant n'a pas le rôle Administrateur.";
    static final String INTROUVABLE = "Tranche inconnue, ou de son entité inactive.";
    static final String LIGNE_INVALIDE = "Ligne invalide : `champ` = `lignes[i].<champ>`.";
    static final String LISTE_RELUE = "Liste enregistrée, relue.";
    static final String ADMIN = "hasRole('Administrateur')";

    private final TerrainsTrancheService terrainsService;
    private final EtagesTrancheService etagesService;
    private final PlancherCouvertService plancherService;
    private final FraisTrancheService fraisService;

    public OngletsTrancheController(TerrainsTrancheService terrainsService, EtagesTrancheService etagesService,
                                    PlancherCouvertService plancherService, FraisTrancheService fraisService) {
        this.terrainsService = terrainsService;
        this.etagesService = etagesService;
        this.plancherService = plancherService;
        this.fraisService = fraisService;
    }

    @GetMapping("/terrains-disponibles")
    @Operation(summary = "Terrains proposés à l'onglet Terrains (nature TER, vocation PROJET), triés par code")
    public List<TerrainDisponibleDto> terrainsDisponibles() {
        return terrainsService.disponibles();
    }

    @GetMapping("/{id}/terrains")
    @Operation(summary = "Onglet Terrains de la tranche, par ordre")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Terrains de la tranche (éventuellement aucun)."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public List<TerrainTrancheDto> terrains(@Parameter(description = ID_TRANCHE) @PathVariable UUID id) {
        return terrainsService.lister(id);
    }

    @PutMapping("/{id}/terrains")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Enregistre l'onglet Terrains en une fois (liste complète, une transaction)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = LISTE_RELUE),
            @ApiResponse(responseCode = "400", description = LIGNE_INVALIDE),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public List<TerrainTrancheDto> enregistrerTerrains(@Parameter(description = ID_TRANCHE) @PathVariable UUID id,
                                                       @RequestBody LignesTrancheDto<TerrainTrancheEcritureDto> corps) {
        return terrainsService.enregistrerTerrains(id, corps.lignes());
    }

    @GetMapping("/{id}/etages")
    @Operation(summary = "Onglet Étages de la tranche, triés par code de niveau")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Niveaux de la tranche (éventuellement aucun)."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public List<EtageTrancheDto> etages(@Parameter(description = ID_TRANCHE) @PathVariable UUID id) {
        return etagesService.lister(id);
    }

    @PutMapping("/{id}/etages")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Enregistre l'onglet Étages en une fois (liste complète, une transaction)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = LISTE_RELUE),
            @ApiResponse(responseCode = "400", description = LIGNE_INVALIDE),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public List<EtageTrancheDto> enregistrerEtages(@Parameter(description = ID_TRANCHE) @PathVariable UUID id,
                                                   @RequestBody LignesTrancheDto<EtageTrancheEcritureDto> corps) {
        return etagesService.enregistrerEtages(id, corps.lignes());
    }

    @GetMapping("/{id}/plancher-couvert")
    @Operation(summary = "Onglet Plancher couvert de la tranche, trié par code de catégorie")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Superficies par catégorie (éventuellement aucune)."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public List<PlancherCouvertDto> plancherCouvert(@Parameter(description = ID_TRANCHE) @PathVariable UUID id) {
        return plancherService.lister(id);
    }

    @PutMapping("/{id}/plancher-couvert")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Enregistre l'onglet Plancher couvert et réécrit le plancher couvert de la tranche (Σ)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = LISTE_RELUE),
            @ApiResponse(responseCode = "400", description = LIGNE_INVALIDE),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public List<PlancherCouvertDto> enregistrerPlancherCouvert(
            @Parameter(description = ID_TRANCHE) @PathVariable UUID id,
            @RequestBody LignesTrancheDto<PlancherCouvertEcritureDto> corps) {
        return plancherService.enregistrerPlancher(id, corps.lignes());
    }

    @GetMapping("/categories-frais")
    @Operation(summary = "Catégories proposées à l'onglet Frais, triées par code")
    public List<ReferentielItemDto> categoriesFrais() {
        return fraisService.categories();
    }

    @GetMapping("/{id}/frais")
    @Operation(summary = "Onglet Frais de la tranche, par ordre")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Frais de vente de la tranche (éventuellement aucun)."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public List<FraisTrancheDto> frais(@Parameter(description = ID_TRANCHE) @PathVariable UUID id) {
        return fraisService.lister(id);
    }

    @PutMapping("/{id}/frais")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Enregistre l'onglet Frais en une fois ; le serveur calcule HT ou TTC")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = LISTE_RELUE),
            @ApiResponse(responseCode = "400", description = LIGNE_INVALIDE),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public List<FraisTrancheDto> enregistrerFrais(@Parameter(description = ID_TRANCHE) @PathVariable UUID id,
                                                  @RequestBody LignesTrancheDto<FraisTrancheEcritureDto> corps) {
        return fraisService.enregistrerFrais(id, corps.lignes());
    }
}
