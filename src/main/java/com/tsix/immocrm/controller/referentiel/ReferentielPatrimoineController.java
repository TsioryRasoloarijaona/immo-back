package com.tsix.immocrm.controller.referentiel;

import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.services.referentiel.ReferentielPatrimoineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Référentiels patrimoine",
        description = "Listes déroulantes des fiches programme et tranche (standings, niveaux, personnels, comptes…).")
@RestController
@RequestMapping("/api/referentiels-patrimoine")
public class ReferentielPatrimoineController {

    private final ReferentielPatrimoineService service;

    public ReferentielPatrimoineController(ReferentielPatrimoineService service) {
        this.service = service;
    }

    @GetMapping("/standings")
    @Operation(summary = "Standings de tranche (CfgTrancheStanding), triés par code")
    public List<ReferentielItemDto> standings() {
        return service.standings();
    }

    @GetMapping("/niveaux-terrain")
    @Operation(summary = "Niveaux de construction (LndLevel), « Nbr. étage » de la tranche, triés par code")
    public List<ReferentielItemDto> niveauxTerrain() {
        return service.niveauxTerrain();
    }

    @GetMapping("/rubriques-tranche")
    @Operation(summary = "Rubriques de tranche (StkTrancheHeading), triées par ordre puis code")
    public List<ReferentielItemDto> rubriquesTranche() {
        return service.rubriquesTranche();
    }

    @GetMapping("/personnels")
    @Operation(summary = "Personnels (tiers de type EMP), triés par nom",
            description = "Sélecteur des signataire, responsables technique et commercial, responsable SAV.")
    public List<ReferentielItemDto> personnels() {
        return service.personnels();
    }

    @GetMapping("/plans-comptables")
    @Operation(summary = "Comptes du plan comptable (ActAccount), triés par code")
    public List<ReferentielItemDto> plansComptables() {
        return service.plansComptables();
    }

    @GetMapping("/projets-facturation")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Projets de travaux (PrjProject) proposés comme « Projet de facturation pour les dépenses »",
            description = "Réservé à l'Administrateur ; seuls les projets des entités actives.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Projets (liste vide si l'entité est inactive ou inconnue)."),
            @ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur.")
    })
    public List<ReferentielItemDto> projetsFacturation(
            @Parameter(description = "Entité juridique (CfgCompany.id) de la tranche ; absent = toutes les entités actives.")
            @RequestParam(required = false) UUID entiteId) {
        return service.projetsFacturation(entiteId);
    }

    @GetMapping("/tiers-autrui")
    @Operation(summary = "Fournisseurs (tiers de type FOU), sélecteur « Tiers autrui » de la tranche")
    public List<ReferentielItemDto> tiersAutrui() {
        return service.tiersAutrui();
    }
}
