package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.patrimoine.faisabilite.EcheancierDemandeDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.FinancementFaisabiliteDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.GrilleEtudeDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.RemboursementFaisabiliteDto;
import com.tsix.immocrm.services.patrimoine.FinancementFaisabiliteService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.patrimoine.EtudeFaisabiliteController.ADMIN;
import static com.tsix.immocrm.controller.patrimoine.EtudeFaisabiliteController.BLOQUEE;
import static com.tsix.immocrm.controller.patrimoine.EtudeFaisabiliteController.ID_ETUDE;
import static com.tsix.immocrm.controller.patrimoine.EtudeFaisabiliteController.INTROUVABLE;
import static com.tsix.immocrm.controller.patrimoine.EtudeFaisabiliteController.PAS_ADMIN;
import static com.tsix.immocrm.controller.patrimoine.EtudeFaisabiliteController.PAS_LECTEUR;

@Tag(name = "Études de faisabilité — financement")
@RestController
@RequestMapping("/api/etudes-faisabilite/{id}/financements")
@PreAuthorize(EtudeFaisabiliteController.LECTURE)
public class FinancementFaisabiliteController {

    private static final String CHAMP = "Champ invalide (detail nomme le champ) : négatif, date absente.";

    private final FinancementFaisabiliteService service;

    public FinancementFaisabiliteController(FinancementFaisabiliteService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Crédits de l'étude et leurs remboursements, par date")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Crédits, montants calculés compris."),
            @ApiResponse(responseCode = "403", description = PAS_LECTEUR),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public List<FinancementFaisabiliteDto> lister(@Parameter(description = ID_ETUDE) @PathVariable UUID id) {
        return service.lister(id);
    }

    @PutMapping
    @PreAuthorize(ADMIN)
    @Operation(summary = "Remplace les crédits et leurs remboursements ; bilan et trésorerie recalculés")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Crédits relus après recalcul."),
            @ApiResponse(responseCode = "400", description = CHAMP),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = BLOQUEE)
    })
    public List<FinancementFaisabiliteDto> remplacer(@Parameter(description = ID_ETUDE) @PathVariable UUID id,
                                                     @RequestBody GrilleEtudeDto<FinancementFaisabiliteDto> grille) {
        return service.modifierFinancements(id, grille.lignes());
    }

    @PostMapping("/echeancier")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Génère l'échéancier d'un crédit sans rien écrire (bouton « Générer »)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Remboursements proposés, sans id."),
            @ApiResponse(responseCode = "400", description = "Nombre de remboursements ≤ 0, montant ou taux négatif, date absente."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public List<RemboursementFaisabiliteDto> echeancier(@Parameter(description = ID_ETUDE) @PathVariable UUID id,
                                                        @RequestBody EcheancierDemandeDto demande) {
        return service.echeancier(id, demande);
    }
}
