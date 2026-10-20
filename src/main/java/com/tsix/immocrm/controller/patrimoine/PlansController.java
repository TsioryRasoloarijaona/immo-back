package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.patrimoine.plans.BilanImportPlanDto;
import com.tsix.immocrm.dto.patrimoine.plans.PlanArticleDto;
import com.tsix.immocrm.dto.patrimoine.plans.PlanDto;
import com.tsix.immocrm.dto.patrimoine.plans.TraceDto;
import com.tsix.immocrm.dto.patrimoine.plans.TrancheAvecPlansDto;
import com.tsix.immocrm.services.patrimoine.plans.ImportPlansSvg;
import com.tsix.immocrm.services.patrimoine.plans.PlansService;
import com.tsix.immocrm.services.patrimoine.plans.PurgePlans;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Plans des tranches", description = "Plans d'étage interactifs : méta, image, tracés colorés par statut, "
        + "import SVG et purge (Administrateur).")
@RestController
public class PlansController {

    private static final String ADMIN = "hasRole('Administrateur')";
    private static final String ID_TRANCHE = "Id de la tranche (CfgTranche.PKey).";
    private static final String ID_PLAN = "Id du plan (StkHierarchyPlan.PKey).";
    private static final String TRANCHE_INTROUVABLE = "Tranche inconnue, d'une entité inactive ou hors du périmètre.";
    private static final String PLAN_INTROUVABLE = "Plan inconnu, ou sa tranche d'une entité inactive ou hors du périmètre.";
    private static final String PAS_ADMIN = "L'appelant n'a pas le rôle Administrateur.";

    private final PlansService plans;
    private final ImportPlansSvg importPlans;
    private final PurgePlans purgePlans;

    public PlansController(PlansService plans, ImportPlansSvg importPlans, PurgePlans purgePlans) {
        this.plans = plans;
        this.importPlans = importPlans;
        this.purgePlans = purgePlans;
    }

    @GetMapping("/api/plans/tranches")
    @Operation(summary = "Tranches visibles ayant au moins un plan (sélecteur de la page Plans)")
    public List<TrancheAvecPlansDto> tranchesAvecPlans() {
        return plans.tranchesAvecPlans();
    }

    @GetMapping("/api/tranches/{id}/plans")
    @Operation(summary = "Plans d'étage de la tranche, sans l'image")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Plans triés par étage ; liste vide si aucun."),
            @ApiResponse(responseCode = "404", description = TRANCHE_INTROUVABLE)})
    public List<PlanDto> plansDeLaTranche(@Parameter(description = ID_TRANCHE) @PathVariable UUID id) {
        return plans.plansDeLaTranche(id);
    }

    @GetMapping("/api/plans/{planId}/image")
    @Operation(summary = "Image du plan (PNG ou JPEG), avec ETag")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Octets de l'image ; `ETag` et cache privé à revalider."),
            @ApiResponse(responseCode = "304", description = "`If-None-Match` égal à l'ETag courant : image inchangée."),
            @ApiResponse(responseCode = "404", description = PLAN_INTROUVABLE + " Ou plan sans image lisible.")})
    public ResponseEntity<byte[]> image(@Parameter(description = ID_PLAN) @PathVariable UUID planId, WebRequest requete) {
        String etag = plans.etagImage(planId);
        if (requete.checkNotModified(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }
        PlansService.ImagePlan image = plans.image(planId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .eTag(etag)
                .cacheControl(CacheControl.noCache().cachePrivate())
                .body(image.octets());
    }

    @GetMapping("/api/plans/{planId}/traces")
    @Operation(summary = "Tracés du plan avec le statut, la couleur et le dossier occupant de chaque article")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Tracés triés par code d'article."),
            @ApiResponse(responseCode = "404", description = PLAN_INTROUVABLE)})
    public List<TraceDto> traces(@Parameter(description = ID_PLAN) @PathVariable UUID planId) {
        return plans.traces(planId);
    }

    @GetMapping("/api/articles/{id}/plans")
    @Operation(summary = "Plans sur lesquels l'article est tracé (onglet Plan de la fiche article)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Plans de l'article ; vide s'il n'est tracé nulle part."),
            @ApiResponse(responseCode = "404", description = "Article inconnu, d'une entité inactive ou hors du périmètre.")})
    public List<PlanArticleDto> plansDeLArticle(
            @Parameter(description = "Id de l'article (StkItem.PKey).") @PathVariable UUID id) {
        return plans.plansDeLArticle(id);
    }

    @PostMapping(value = "/api/tranches/{id}/plans/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(ADMIN)
    @Operation(summary = "Importe le SVG d'un étage : plan de l'étage et tracés des articles")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Bilan, une ligne par tracé."),
            @ApiResponse(responseCode = "400", description = "Fichier refusé (voir la description) ; rien n'est écrit."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = TRANCHE_INTROUVABLE + " Ou étage inconnu."),
            @ApiResponse(responseCode = "413", description = "Fichier de plus de 50 Mo.")})
    public BilanImportPlanDto importer(
            @Parameter(description = ID_TRANCHE) @PathVariable UUID id,
            @Parameter(description = "Fichier SVG de l'étage (50 Mo au plus).") @RequestParam("fichier") MultipartFile fichier,
            @Parameter(description = "Étage importé (StkFloor.PKey), obligatoire.") @RequestParam UUID etageId,
            @Parameter(description = "Vrai = bilan sans rien écrire (« Vérifier » d'OMRAN).")
            @RequestParam(defaultValue = "false") boolean apercu) {
        return importPlans.importer(id, etageId, fichier, apercu);
    }

    @DeleteMapping("/api/tranches/{id}/plans")
    @PreAuthorize(ADMIN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Purge les plans de la tranche (« Puger les plans »)")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Tracés puis plans de la tranche supprimés."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = TRANCHE_INTROUVABLE)})
    public void purger(@Parameter(description = ID_TRANCHE) @PathVariable UUID id) {
        purgePlans.purger(id);
    }
}
