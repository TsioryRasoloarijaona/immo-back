package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.patrimoine.faisabilite.ApercuTresorerieDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.BilanFaisabilitePrevisionsDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.CategorieBilanDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.GrilleEtudeDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.SaisiePrevisionDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.TresorerieFaisabiliteDto;
import com.tsix.immocrm.services.patrimoine.PrevisionFaisabiliteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.patrimoine.EtudeFaisabiliteController.ADMIN;
import static com.tsix.immocrm.controller.patrimoine.EtudeFaisabiliteController.BLOQUEE;
import static com.tsix.immocrm.controller.patrimoine.EtudeFaisabiliteController.ID_ETUDE;
import static com.tsix.immocrm.controller.patrimoine.EtudeFaisabiliteController.INTROUVABLE;
import static com.tsix.immocrm.controller.patrimoine.EtudeFaisabiliteController.PAS_ADMIN;
import static com.tsix.immocrm.controller.patrimoine.EtudeFaisabiliteController.PAS_LECTEUR;

@Tag(name = "Études de faisabilité — bilan et trésorerie")
@RestController
@RequestMapping("/api/etudes-faisabilite")
@PreAuthorize(EtudeFaisabiliteController.LECTURE)
public class PrevisionFaisabiliteController {

    private static final String REFUS = "Ligne refusée : champ invalide, catégorie, unité ou référence interdite (detail).";
    private static final String CALCULE = "Arbre, totaux et bilan calculés.";

    private final PrevisionFaisabiliteService service;

    public PrevisionFaisabiliteController(PrevisionFaisabiliteService service) {
        this.service = service;
    }

    @GetMapping("/{id}/previsions")
    @Operation(summary = "Arbre du bilan de l'étude, totaux des racines et bilan")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = CALCULE),
            @ApiResponse(responseCode = "403", description = PAS_LECTEUR),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public BilanFaisabilitePrevisionsDto lire(@Parameter(description = ID_ETUDE) @PathVariable UUID id) {
        return service.lire(id);
    }

    @PutMapping("/{id}/previsions")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Remplace les lignes du bilan ; bilan, ventilation et trésorerie recalculés")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = CALCULE),
            @ApiResponse(responseCode = "400", description = REFUS),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = BLOQUEE)
    })
    public BilanFaisabilitePrevisionsDto remplacer(@Parameter(description = ID_ETUDE) @PathVariable UUID id,
                                                   @RequestBody GrilleEtudeDto<SaisiePrevisionDto> grille) {
        return service.modifierPrevisions(id, grille.lignes());
    }

    @PostMapping("/{id}/previsions/calcul")
    @Operation(summary = "Aperçu du bilan pour des lignes saisies, sans rien écrire")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = CALCULE),
            @ApiResponse(responseCode = "400", description = REFUS),
            @ApiResponse(responseCode = "403", description = PAS_LECTEUR),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public BilanFaisabilitePrevisionsDto calculer(@Parameter(description = ID_ETUDE) @PathVariable UUID id,
                                                  @RequestBody GrilleEtudeDto<SaisiePrevisionDto> grille) {
        return service.calculer(id, grille.lignes());
    }

    @GetMapping("/choix/categories-bilan")
    @Operation(summary = "Catégories de trésorerie sélectionnables dans le bilan, par ordre")
    @ApiResponse(responseCode = "403", description = PAS_LECTEUR)
    public List<CategorieBilanDto> categories() {
        return service.categories();
    }

    @GetMapping("/{id}/tresorerie")
    @Operation(summary = "Courbe mensuelle et lignes de trésorerie enregistrées de l'étude")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Série mensuelle et lignes."),
            @ApiResponse(responseCode = "403", description = PAS_LECTEUR),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public TresorerieFaisabiliteDto tresorerie(
            @Parameter(description = ID_ETUDE) @PathVariable UUID id,
            @Parameter(description = "Inclure crédits et remboursements.") @RequestParam(defaultValue = "true")
            boolean avecFinancement) {
        return service.tresorerie(id, avecFinancement);
    }

    @PostMapping("/{id}/tresorerie/calcul")
    @Operation(summary = "Aperçu de la trésorerie avec d'autres écarts-types, sans rien écrire")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Série mensuelle et lignes recalculées."),
            @ApiResponse(responseCode = "400", description = "Écart-type < 0,5, ou > 1 pour 3 ou 4 mois (message OMRAN)."),
            @ApiResponse(responseCode = "403", description = PAS_LECTEUR),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public TresorerieFaisabiliteDto calculerTresorerie(@Parameter(description = ID_ETUDE) @PathVariable UUID id,
                                                       @RequestBody ApercuTresorerieDto apercu) {
        return service.calculerTresorerie(id, apercu);
    }
}
