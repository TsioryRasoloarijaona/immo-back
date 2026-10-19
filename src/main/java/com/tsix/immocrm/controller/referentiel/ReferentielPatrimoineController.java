package com.tsix.immocrm.controller.referentiel;

import com.tsix.immocrm.dto.referentiel.ContenanceDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.services.patrimoine.ReferencesFicheArticle;
import com.tsix.immocrm.services.patrimoine.ReferencesPatrimoine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Référentiels du patrimoine", description = "Listes de choix des fiches programme, tranche et article.")
@RestController
@RequestMapping("/api/referentiels-patrimoine")
public class ReferentielPatrimoineController {

    private final ReferencesPatrimoine references;
    private final ReferencesFicheArticle referencesArticle;

    public ReferentielPatrimoineController(ReferencesPatrimoine references, ReferencesFicheArticle referencesArticle) {
        this.references = references;
        this.referencesArticle = referencesArticle;
    }

    @GetMapping("/standings")
    @Operation(summary = "Standings de tranche (CfgTrancheStanding), triés par code")
    public List<ReferentielItemDto> standings() {
        return references.standings();
    }

    @GetMapping("/niveaux-terrain")
    @Operation(summary = "Niveaux (LndLevel), champ « Nbr. étage » de la tranche, triés par code")
    public List<ReferentielItemDto> niveauxTerrain() {
        return references.niveauxTerrain();
    }

    @GetMapping("/rubriques-tranche")
    @Operation(summary = "Rubriques de tranche (StkTrancheHeading), triées par Sorting")
    public List<ReferentielItemDto> rubriquesTranche() {
        return references.rubriquesTranche();
    }

    @GetMapping("/personnels")
    @Operation(summary = "Personnels (CfgTier de type EMP), sélecteur des signataires et responsables")
    public List<ReferentielItemDto> personnels() {
        return references.personnels();
    }

    @GetMapping("/plans-comptables")
    @Operation(summary = "Comptes du plan comptable (ActAccount), triés par code")
    public List<ReferentielItemDto> plansComptables() {
        return references.plansComptables();
    }

    @GetMapping("/projets-facturation")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Projets de facturation (PrjProject) des entités actives, pour l'Administrateur")
    @ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur.")
    public List<ReferentielItemDto> projetsFacturation(
            @Parameter(description = "Entité (CfgCompany) de la tranche. Absent = toutes les entités actives ; "
                    + "entité inactive = liste vide.")
            @RequestParam(required = false) UUID entiteId) {
        return references.projetsFacturation(entiteId);
    }

    @GetMapping("/tiers-autrui")
    @Operation(summary = "Fournisseurs (CfgTier de type FOU), sélecteur « Tiers autrui » de la tranche")
    public List<ReferentielItemDto> tiersAutrui() {
        return references.tiersAutrui();
    }

    // -- Fiche article (FEATURE-patrimoine-article-fiche.md §1, §2) --

    @GetMapping("/orientations")
    @Operation(summary = "Orientations d'article (StkOrientation), triées par code")
    public List<ReferentielItemDto> orientations() {
        return referencesArticle.orientations();
    }

    @GetMapping("/categories-charge")
    @Operation(summary = "Catégories de charge d'article (StkCostCategory), triées par code")
    public List<ReferentielItemDto> categoriesCharge() {
        return referencesArticle.categoriesCharge();
    }

    @GetMapping("/facades")
    @Operation(summary = "Façades d'article (StkFacade), triées par code")
    public List<ReferentielItemDto> facades() {
        return referencesArticle.facades();
    }

    @GetMapping("/vues")
    @Operation(summary = "Vues d'article (StkVue), triées par code")
    public List<ReferentielItemDto> vues() {
        return referencesArticle.vues();
    }

    @GetMapping("/contenances")
    @Operation(summary = "Contenances (StkContenance) et leurs options, triées par Sorting puis code")
    public List<ContenanceDto> contenances() {
        return referencesArticle.contenances();
    }
}
