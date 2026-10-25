package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.patrimoine.EcheanceSchemaDto;
import com.tsix.immocrm.dto.patrimoine.EntiteFinalisationDto;
import com.tsix.immocrm.dto.patrimoine.LigneCommissionDto;
import com.tsix.immocrm.dto.patrimoine.LigneFinalisationDto;
import com.tsix.immocrm.dto.patrimoine.LignesTrancheDto;
import com.tsix.immocrm.dto.patrimoine.MiseAJourSchemasDto;
import com.tsix.immocrm.dto.patrimoine.PrevisionsBaseDto;
import com.tsix.immocrm.dto.patrimoine.StockTrancheDto;
import com.tsix.immocrm.services.patrimoine.CommissionsTrancheService;
import com.tsix.immocrm.services.patrimoine.FinalisationsTrancheService;
import com.tsix.immocrm.services.patrimoine.MiseAJourSchemasService;
import com.tsix.immocrm.services.patrimoine.PrevisionsBaseService;
import com.tsix.immocrm.services.patrimoine.TrancheService;
import com.tsix.immocrm.services.patrimoine.fiche.FichePdf;
import com.tsix.immocrm.services.patrimoine.fiche.FichePrevisionsBasePdf;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
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

@Tag(name = "Règles de vente de la tranche", description = "Finalisations, commissions, prévisions de base et mise à "
        + "jour des schémas ; écriture Administrateur.")
@RestController
@RequestMapping("/api/tranches")
public class TrancheReglesController {

    private static final String ADMIN = "hasRole('Administrateur')";
    private static final String ID_TRANCHE = "Id de la tranche (CfgTranche.PKey).";
    private static final String LISTE = "Lignes de l'onglet.";
    private static final String LISTE_RELUE = "Lignes enregistrées, relues.";
    private static final String REFUS = "Saisie refusée (message OMRAN ou lignes[i].<champ> dans le détail) ; rien n'est écrit.";
    private static final String PAS_ADMIN = "L'appelant n'a pas le rôle Administrateur.";
    private static final String INTROUVABLE = "Tranche introuvable ou de son entité inactive.";
    private static final String SANS_DATE = "La tranche n'a pas de date de début des finalisations.";

    private final FinalisationsTrancheService finalisations;
    private final CommissionsTrancheService commissions;
    private final PrevisionsBaseService previsions;
    private final MiseAJourSchemasService schemas;
    private final TrancheService trancheService;
    private final FichePdf fichePdf;

    public TrancheReglesController(FinalisationsTrancheService finalisations, CommissionsTrancheService commissions,
                                   PrevisionsBaseService previsions, MiseAJourSchemasService schemas,
                                   TrancheService trancheService, FichePdf fichePdf) {
        this.finalisations = finalisations;
        this.commissions = commissions;
        this.previsions = previsions;
        this.schemas = schemas;
        this.trancheService = trancheService;
        this.fichePdf = fichePdf;
    }

    // ------------------------------------------------------------------ finalisations (§1)

    @GetMapping("/{id}/finalisations")
    @Operation(summary = "Onglet Finalisations : statuts où s'appliquent les commissions de finalisation")
    @ApiResponses({@ApiResponse(responseCode = "200", description = LISTE),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)})
    public LignesTrancheDto<LigneFinalisationDto> finalisations(@Parameter(description = ID_TRANCHE) @PathVariable UUID id) {
        return finalisations.finalisations(id);
    }

    @PutMapping("/{id}/finalisations")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Enregistre les finalisations (type de dossier en double → 400)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = LISTE_RELUE),
            @ApiResponse(responseCode = "400", description = REFUS),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)})
    public LignesTrancheDto<LigneFinalisationDto> enregistrerFinalisations(
            @Parameter(description = ID_TRANCHE) @PathVariable UUID id,
            @RequestBody LignesTrancheDto<LigneFinalisationDto> corps) {
        return finalisations.enregistrerFinalisations(id, corps);
    }

    @GetMapping("/entites-finalisation")
    @Operation(summary = "Choix de l'onglet Finalisations : les trois types de dossier de vente et leurs statuts")
    public List<EntiteFinalisationDto> entitesFinalisation() {
        return finalisations.entitesFinalisation();
    }

    // ------------------------------------------------------------------ commissions (§2)

    @GetMapping("/{id}/commissions")
    @Operation(summary = "Onglet Commission vendeur, trié par date début")
    @ApiResponses({@ApiResponse(responseCode = "200", description = LISTE),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)})
    public LignesTrancheDto<LigneCommissionDto> commissions(@Parameter(description = ID_TRANCHE) @PathVariable UUID id) {
        return commissions.commissions(id);
    }

    @PutMapping("/{id}/commissions")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Enregistre la liste complète des commissions vendeur")
    @ApiResponses({@ApiResponse(responseCode = "200", description = LISTE_RELUE),
            @ApiResponse(responseCode = "400", description = REFUS),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)})
    public LignesTrancheDto<LigneCommissionDto> enregistrerCommissions(
            @Parameter(description = ID_TRANCHE) @PathVariable UUID id,
            @RequestBody LignesTrancheDto<LigneCommissionDto> corps) {
        return commissions.enregistrerCommissions(id, corps);
    }

    // ------------------------------------------------------------------ prévisions de base (§3)

    @GetMapping("/{id}/previsions-base")
    @Operation(summary = "Prévisions de base : en-tête de la tranche et lignes mensuelles")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "En-tête et lignes."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)})
    public PrevisionsBaseDto previsionsBase(@Parameter(description = ID_TRANCHE) @PathVariable UUID id) {
        return previsions.previsions(id);
    }

    @GetMapping("/{id}/previsions-base/fiche.pdf")
    @Operation(summary = "Fiche prévision (PDF inline) : hypothèses et lignes mensuelles, visibilité des prévisions")
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    public ResponseEntity<byte[]> fichePrevisionsBase(@Parameter(description = ID_TRANCHE) @PathVariable UUID id) {
        PrevisionsBaseDto lues = previsions.previsions(id);
        return fichePdf.reponse(FichePrevisionsBasePdf.fiche(trancheService.findById(id), lues));
    }

    @GetMapping("/{id}/previsions-base/stock")
    @Operation(summary = "« Charger à partir du stock » : nombre d'articles et valorisation du stock de la tranche")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Stock recalculé sur les articles."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)})
    public StockTrancheDto stock(@Parameter(description = ID_TRANCHE) @PathVariable UUID id) {
        return previsions.stock(id);
    }

    @PostMapping("/{id}/previsions-base/generer")
    @PreAuthorize(ADMIN)
    @Operation(summary = "« Générer » : lignes réparties en cloche, sans rien écrire")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "En-tête reçu et lignes générées."),
            @ApiResponse(responseCode = "400", description = "Une des quatre dates manque."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)})
    public PrevisionsBaseDto calculerPrevisions(@Parameter(description = ID_TRANCHE) @PathVariable UUID id,
                                                @RequestBody PrevisionsBaseDto entete) {
        return previsions.calculerPrevisions(id, entete);
    }

    @PutMapping("/{id}/previsions-base")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Enregistre l'en-tête et remplace les lignes des prévisions de base")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Prévisions enregistrées, relues."),
            @ApiResponse(responseCode = "400", description = REFUS),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)})
    public PrevisionsBaseDto enregistrerPrevisions(@Parameter(description = ID_TRANCHE) @PathVariable UUID id,
                                                   @RequestBody PrevisionsBaseDto corps) {
        return previsions.enregistrerPrevisions(id, corps);
    }

    // ------------------------------------------------------------------ mise à jour des schémas (§4)

    @GetMapping("/{id}/mettre-a-jour-schemas/apercu")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Aperçu : échéances que la mise à jour des schémas déplacerait")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Échéances sélectionnées, rien n'est modifié."),
            @ApiResponse(responseCode = "400", description = SANS_DATE),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)})
    public List<EcheanceSchemaDto> apercuSchemas(@Parameter(description = ID_TRANCHE) @PathVariable UUID id) {
        return schemas.apercuSchemas(id);
    }

    @PostMapping("/{id}/mettre-a-jour-schemas")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Mettre à jour les schémas de financement : échéances ramenées au début des finalisations")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Nombre d'échéances et de dossiers modifiés."),
            @ApiResponse(responseCode = "400", description = SANS_DATE),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)})
    public MiseAJourSchemasDto modifierSchemas(@Parameter(description = ID_TRANCHE) @PathVariable UUID id) {
        return schemas.modifierSchemas(id);
    }
}
