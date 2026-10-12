package com.tsix.immocrm.controller.equipe;

import com.tsix.immocrm.dto.equipe.ADistribuerDto;
import com.tsix.immocrm.dto.equipe.AffectationsEquipeDto;
import com.tsix.immocrm.dto.equipe.AffectationsEquipeDto.MembreAffectationDto;
import com.tsix.immocrm.dto.equipe.CommercialEquipeDto;
import com.tsix.immocrm.dto.equipe.DistribuerRequest;
import com.tsix.immocrm.dto.equipe.EntonnoirDto;
import com.tsix.immocrm.dto.equipe.ModifierAffectationsRequest;
import com.tsix.immocrm.dto.equipe.ReaffecterRequest;
import com.tsix.immocrm.dto.equipe.ResultatLotDto;
import com.tsix.immocrm.services.equipe.EquipeCommercialeService;
import com.tsix.immocrm.services.equipe.EquipeEcrituresService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/equipe")
@PreAuthorize("hasAnyRole('Commercial Chef', 'Administrateur', 'Direction Générale')")
@Tag(name = "Mon équipe")
@ApiResponses({
        @ApiResponse(responseCode = "403", description = "Rôle sans accès, ou projet hors du périmètre de l'appelant.")
})
public class EquipeCommercialeController {

    private static final String DU = "Premier jour pris en compte, inclus. Défaut : il y a 30 jours.";
    private static final String AU = "Dernier jour pris en compte, inclus jusqu'à 23:59:59. Défaut : aujourd'hui.";
    private static final String PROJET = "Projet à isoler ; doit être dans le périmètre de l'appelant (sinon 403).";
    private static final String PERIODE_INVALIDE = "`du` postérieure à `au`, ou date non parsable.";

    private final EquipeCommercialeService lectures;
    private final EquipeEcrituresService ecritures;

    public EquipeCommercialeController(EquipeCommercialeService lectures, EquipeEcrituresService ecritures) {
        this.lectures = lectures;
        this.ecritures = ecritures;
    }

    @GetMapping("/entonnoir")
    @Operation(summary = "Entonnoir des leads de la période")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Six étapes, éventuellement à zéro."),
            @ApiResponse(responseCode = "400", description = PERIODE_INVALIDE)
    })
    public EntonnoirDto entonnoir(
            @Parameter(description = DU, example = "2026-09-12")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @Parameter(description = AU, example = "2026-10-11")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @Parameter(description = PROJET) @RequestParam(required = false) UUID projet) {
        return lectures.entonnoir(du, au, projet);
    }

    @GetMapping("/commerciaux")
    @Operation(summary = "Tableau « par commercial » de l'équipe")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Une ligne par commercial de l'équipe, triée par nom."),
            @ApiResponse(responseCode = "400", description = PERIODE_INVALIDE)
    })
    public List<CommercialEquipeDto> commerciaux(
            @Parameter(description = DU, example = "2026-09-12")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @Parameter(description = AU, example = "2026-10-11")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @Parameter(description = PROJET) @RequestParam(required = false) UUID projet) {
        return lectures.commerciaux(du, au, projet);
    }

    @GetMapping("/a-distribuer")
    @Operation(summary = "Pré-prospections en attente de commercial")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Compte et âge de la plus ancienne.")})
    public ADistribuerDto aDistribuer(@Parameter(description = PROJET) @RequestParam(required = false) UUID projet) {
        return lectures.aDistribuer(projet);
    }

    @PostMapping("/distribuer")
    @Operation(summary = "Distribue des pré-prospections à un commercial de l'équipe")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Toutes les lignes ont été transformées."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "404", description = "Une ligne demandée n'existe pas : rien n'est écrit."),
            @ApiResponse(responseCode = "409")
    })
    public ResultatLotDto distribuer(@RequestBody DistribuerRequest request) {
        return ecritures.distribuer(request);
    }

    @PostMapping("/reaffecter")
    @Operation(summary = "Réaffecte des prospections à un autre commercial de l'équipe")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lignes réaffectées (celles déjà à ce commercial sont ignorées)."),
            @ApiResponse(responseCode = "400", description = "Liste vide ou `commercialId` absent."),
            @ApiResponse(responseCode = "404", description = "Une ligne demandée n'existe pas : rien n'est écrit."),
            @ApiResponse(responseCode = "409")
    })
    public ResultatLotDto reaffecter(@RequestBody ReaffecterRequest request) {
        return ecritures.reaffecter(request);
    }

    @GetMapping("/affectations")
    @Operation(summary = "Projets de l'appelant × commerciaux de l'équipe")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Matrice, éventuellement vide.")})
    public AffectationsEquipeDto affectations() {
        return lectures.affectations();
    }

    @PutMapping("/affectations/{admUserId}")
    @Operation(summary = "Ajoute ou retire des projets à un commercial")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nouvel état du commercial."),
            @ApiResponse(responseCode = "400", description = "Rien à ajouter ni à retirer, ou projet inconnu."),
            @ApiResponse(responseCode = "404", description = "Aucun compte avec cet id."),
            @ApiResponse(responseCode = "409", description = "Le retrait lui enlèverait son dernier projet.")
    })
    public MembreAffectationDto modifierAffectations(
            @Parameter(description = "Compte (AdmUser) du commercial.") @PathVariable UUID admUserId,
            @RequestBody ModifierAffectationsRequest request) {
        return ecritures.modifierAffectations(admUserId, request);
    }
}
