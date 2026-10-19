package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.patrimoine.LigneEtageDto;
import com.tsix.immocrm.dto.patrimoine.LigneFraisDto;
import com.tsix.immocrm.dto.patrimoine.LignePlancherDto;
import com.tsix.immocrm.dto.patrimoine.LigneTerrainDto;
import com.tsix.immocrm.dto.patrimoine.LignesTrancheDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.services.patrimoine.OngletsTrancheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Onglets de la tranche", description = "Terrains, Étages, Plancher couvert et Frais de vente de la fiche "
        + "tranche ; lecture ouverte, écriture Administrateur par liste complète.")
@RestController
@RequestMapping("/api/tranches")
public class OngletsTrancheController {

    private static final String ADMIN = "hasRole('Administrateur')";
    private static final String ID_TRANCHE = "Id de la tranche (CfgTranche.PKey).";
    private static final String LISTE = "Lignes de l'onglet.";
    private static final String LISTE_RELUE = "Lignes enregistrées, relues.";
    private static final String LIGNE_INVALIDE = "Ligne invalide : le détail nomme lignes[i].<champ> ; rien n'est écrit.";
    private static final String PAS_ADMIN = "L'appelant n'a pas le rôle Administrateur.";
    private static final String INTROUVABLE = "Tranche introuvable ou de son entité inactive.";

    private final OngletsTrancheService service;

    public OngletsTrancheController(OngletsTrancheService service) {
        this.service = service;
    }

    @GetMapping("/{id}/terrains")
    @Operation(summary = "Onglet Terrains de la tranche, triés par rang")
    @ApiResponses({@ApiResponse(responseCode = "200", description = LISTE),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)})
    public LignesTrancheDto<LigneTerrainDto> terrains(@Parameter(description = ID_TRANCHE) @PathVariable UUID id) {
        return service.terrains(id);
    }

    @PutMapping("/{id}/terrains")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Enregistre la liste complète des terrains (doublon de terrain ignoré)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = LISTE_RELUE),
            @ApiResponse(responseCode = "400", description = LIGNE_INVALIDE),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)})
    public LignesTrancheDto<LigneTerrainDto> enregistrerTerrains(
            @Parameter(description = ID_TRANCHE) @PathVariable UUID id, @RequestBody LignesTrancheDto<LigneTerrainDto> corps) {
        return service.enregistrerTerrains(id, corps);
    }

    @GetMapping("/{id}/etages")
    @Operation(summary = "Onglet Étages de la tranche, triés par code de niveau")
    @ApiResponses({@ApiResponse(responseCode = "200", description = LISTE),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)})
    public LignesTrancheDto<LigneEtageDto> etages(@Parameter(description = ID_TRANCHE) @PathVariable UUID id) {
        return service.etages(id);
    }

    @PutMapping("/{id}/etages")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Enregistre la liste complète des étages (doublon de niveau ignoré)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = LISTE_RELUE),
            @ApiResponse(responseCode = "400", description = LIGNE_INVALIDE),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)})
    public LignesTrancheDto<LigneEtageDto> enregistrerEtages(
            @Parameter(description = ID_TRANCHE) @PathVariable UUID id, @RequestBody LignesTrancheDto<LigneEtageDto> corps) {
        return service.enregistrerEtages(id, corps);
    }

    @GetMapping("/{id}/plancher-couvert")
    @Operation(summary = "Onglet Plancher couvert de la tranche, trié par code de catégorie")
    @ApiResponses({@ApiResponse(responseCode = "200", description = LISTE),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)})
    public LignesTrancheDto<LignePlancherDto> plancherCouvert(@Parameter(description = ID_TRANCHE) @PathVariable UUID id) {
        return service.plancherCouvert(id);
    }

    @PutMapping("/{id}/plancher-couvert")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Enregistre le plancher couvert ; plancherCouvert de la tranche = Σ des surfaces")
    @ApiResponses({@ApiResponse(responseCode = "200", description = LISTE_RELUE),
            @ApiResponse(responseCode = "400", description = LIGNE_INVALIDE),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)})
    public LignesTrancheDto<LignePlancherDto> enregistrerPlancherCouvert(
            @Parameter(description = ID_TRANCHE) @PathVariable UUID id, @RequestBody LignesTrancheDto<LignePlancherDto> corps) {
        return service.enregistrerPlancherCouvert(id, corps);
    }

    @GetMapping("/{id}/frais")
    @Operation(summary = "Onglet Frais de vente de la tranche, triés par O.D.T.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = LISTE),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)})
    public LignesTrancheDto<LigneFraisDto> frais(@Parameter(description = ID_TRANCHE) @PathVariable UUID id) {
        return service.frais(id);
    }

    @PutMapping("/{id}/frais")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Enregistre les frais de vente ; le serveur calcule HT ↔ TTC (doublon de catégorie ignoré)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = LISTE_RELUE),
            @ApiResponse(responseCode = "400", description = LIGNE_INVALIDE),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)})
    public LignesTrancheDto<LigneFraisDto> enregistrerFrais(
            @Parameter(description = ID_TRANCHE) @PathVariable UUID id, @RequestBody LignesTrancheDto<LigneFraisDto> corps) {
        return service.enregistrerFrais(id, corps);
    }

    @GetMapping("/terrains-disponibles")
    @Operation(summary = "Choix de l'onglet Terrains : articles de nature Terrain et de vocation Projet, par code")
    public List<ReferentielItemDto> terrainsDisponibles() {
        return service.terrainsDisponibles();
    }

    @GetMapping("/categories-frais")
    @Operation(summary = "Choix de l'onglet Frais : catégories crédit et frais de vente, hors les trois par défaut")
    public List<ReferentielItemDto> categoriesFrais() {
        return service.categoriesFrais();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ADMIN)
    @Tag(name = "Tranches")
    @Operation(summary = "Supprime une tranche sans article ni autre référence bloquante")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tranche supprimée avec ses onglets, habilitations, pièces et notes."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Article, prospection, offre… référencent la tranche : le détail les compte.")
    })
    public void supprimer(@Parameter(description = ID_TRANCHE) @PathVariable UUID id) {
        service.supprimerTranche(id);
    }
}
