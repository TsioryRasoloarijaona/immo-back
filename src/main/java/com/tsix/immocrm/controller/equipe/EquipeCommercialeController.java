package com.tsix.immocrm.controller.equipe;

import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.equipe.ADistribuerDto;
import com.tsix.immocrm.dto.equipe.FiltreADistribuer;
import com.tsix.immocrm.dto.equipe.LigneADistribuerDto;
import com.tsix.immocrm.dto.equipe.AffectationsEquipeDto;
import com.tsix.immocrm.dto.equipe.AffectationsEquipeDto.MembreAffectationDto;
import com.tsix.immocrm.dto.equipe.CommercialEquipeDto;
import com.tsix.immocrm.dto.equipe.DistribuerRequest;
import com.tsix.immocrm.dto.equipe.EntonnoirDto;
import com.tsix.immocrm.dto.equipe.ModifierAffectationsRequest;
import com.tsix.immocrm.dto.equipe.ReaffecterRequest;
import com.tsix.immocrm.dto.equipe.RepartirRequest;
import com.tsix.immocrm.dto.equipe.ResultatRepartitionDto;
import com.tsix.immocrm.dto.equipe.ResultatLotDto;
import com.tsix.immocrm.services.equipe.ADistribuerService;
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
    private static final String TOUTES = "`true` : aussi les pré-prospections déjà traitées. Défaut : jamais traitées seulement.";

    private final EquipeCommercialeService lectures;
    private final ADistribuerService aDistribuer;
    private final EquipeEcrituresService ecritures;

    public EquipeCommercialeController(EquipeCommercialeService lectures, ADistribuerService aDistribuer,
                                       EquipeEcrituresService ecritures) {
        this.lectures = lectures;
        this.aDistribuer = aDistribuer;
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
    @Operation(summary = "Pré-prospections en attente de commercial, reçues dans la période")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte et âge de la plus ancienne de la période."),
            @ApiResponse(responseCode = "400", description = PERIODE_INVALIDE)
    })
    public ADistribuerDto aDistribuer(
            @Parameter(description = DU, example = "2026-09-12")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @Parameter(description = AU, example = "2026-10-11")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @Parameter(description = PROJET) @RequestParam(required = false) UUID projet,
            @Parameter(description = TOUTES) @RequestParam(defaultValue = "false") boolean toutes) {
        return aDistribuer.compter(new FiltreADistribuer(du, au, projet, toutes));
    }

    @GetMapping("/a-distribuer/lignes")
    @Operation(summary = "Liste des pré-prospections à distribuer (mêmes règles que le compteur)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page triée par date de document croissante."),
            @ApiResponse(responseCode = "400", description = PERIODE_INVALIDE + " Ou `page` < 0, `size` < 1.")
    })
    public PageDto<LigneADistribuerDto> lignesADistribuer(
            @Parameter(description = DU, example = "2026-09-12")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @Parameter(description = AU, example = "2026-10-11")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @Parameter(description = PROJET) @RequestParam(required = false) UUID projet,
            @Parameter(description = TOUTES) @RequestParam(defaultValue = "false") boolean toutes,
            @Parameter(description = "Numéro de page, à partir de 0.") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page ; ramenée à 500 au-delà.")
            @RequestParam(defaultValue = "100") int size) {
        return aDistribuer.lignes(new FiltreADistribuer(du, au, projet, toutes), page, size);
    }

    @PostMapping("/distribuer")
    @Operation(summary = "Distribue des pré-prospections à un commercial de l'équipe")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Commercial posé (celles déjà à ce commercial sont ignorées)."),
            @ApiResponse(responseCode = "400", description = "Liste vide ou `commercialId` absent."),
            @ApiResponse(responseCode = "404", description = "Une ligne demandée n'existe pas : rien n'est écrit."),
            @ApiResponse(responseCode = "409")
    })
    public ResultatLotDto distribuer(@RequestBody DistribuerRequest request) {
        return ecritures.distribuer(request);
    }

    @PostMapping("/reaffecter")
    @Operation(summary = "Réaffecte des prospections ou pré-prospections affectées à un autre commercial de l'équipe")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lignes réaffectées (celles déjà à ce commercial sont ignorées)."),
            @ApiResponse(responseCode = "400", description = "Liste vide ou `commercialId` absent."),
            @ApiResponse(responseCode = "404", description = "Une ligne demandée n'existe pas : rien n'est écrit."),
            @ApiResponse(responseCode = "409")
    })
    public ResultatLotDto reaffecter(@RequestBody ReaffecterRequest request) {
        return ecritures.reaffecter(request);
    }

    @PostMapping("/repartir")
    @Operation(summary = "Répartit automatiquement des pré-prospections entre les commerciaux de leur projet")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attributions (ou aperçu si `simulation`), lignes laissées et totaux."),
            @ApiResponse(responseCode = "400", description = "Ni ids ni `filtre`, les deux à la fois, ou plus de 5 000 lignes au filtre."),
            @ApiResponse(responseCode = "404", description = "Une ligne demandée n'existe pas : rien n'est écrit."),
            @ApiResponse(responseCode = "409", description = "Une ligne n'est plus une pré-prospection ou est clôturée : rien n'est écrit.")
    })
    public ResultatRepartitionDto repartir(@RequestBody RepartirRequest request) {
        return ecritures.repartir(request);
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
