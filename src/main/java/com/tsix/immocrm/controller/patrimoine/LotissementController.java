package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.patrimoine.FiltresTerrains;
import com.tsix.immocrm.dto.patrimoine.LotissementDetailDto;
import com.tsix.immocrm.dto.patrimoine.LotissementEcritureDto;
import com.tsix.immocrm.dto.patrimoine.TerrainListeDto;
import com.tsix.immocrm.services.patrimoine.fiche.FicheLotissement;
import com.tsix.immocrm.services.patrimoine.fiche.FichePdf;
import com.tsix.immocrm.services.patrimoine.terrain.LotissementService;
import com.tsix.immocrm.services.patrimoine.terrain.TerrainService;
import com.tsix.immocrm.web.export.DemandeExport;
import com.tsix.immocrm.web.export.ExportListe;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.tsix.immocrm.controller.patrimoine.ArticleController.ADMINISTRATEUR;
import static com.tsix.immocrm.controller.patrimoine.ArticleController.REPONSE_403;
import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Lotissements")
@RestController
@RequestMapping("/api/lotissements")
public class LotissementController {

    private static final String ID = "Id du lot (StkItem.PKey).";
    private static final String INTROUVABLE = "Pas un lot (terrain sans tranche, article…), ou lot d'une entité inactive.";
    private static final String SAISIE_INVALIDE = "Champ obligatoire absent, référence inconnue ou d'une autre nature.";

    private final TerrainService terrains;
    private final LotissementService lotissements;
    private final ExportListe export;
    private final FichePdf fichePdf;

    public LotissementController(TerrainService terrains, LotissementService lotissements, ExportListe export,
                                 FichePdf fichePdf) {
        this.terrains = terrains;
        this.lotissements = lotissements;
        this.export = export;
        this.fichePdf = fichePdf;
    }

    @GetMapping("/export")
    @Operation(summary = "Export xlsx ou pdf de la liste des lots de terrain", description = ExportListe.DESCRIPTION)
    @ApiResponse(responseCode = "400", description = ExportListe.REFUS, content = @Content)
    public ResponseEntity<byte[]> exporter(
            @ParameterObject @ModelAttribute FiltresTerrains filtres,
            @ParameterObject @ModelAttribute DemandeExport demande,
            @Parameter(hidden = true) @SortDefault(sort = "code", direction = Sort.Direction.ASC) Sort sort) {
        return TerrainController.exporter(export, terrains, filtres, demande, sort, true);
    }

    @GetMapping
    @Operation(summary = "Lots de terrain : terrains d'une tranche, entité « article », vocation autre que PROJET")
    @ApiResponse(responseCode = "200", description = "Page de lots de terrain des tranches visibles.")
    public PageDto<TerrainListeDto> lister(
            @ParameterObject @ModelAttribute FiltresTerrains filtres,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "code", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageDto.of(terrains.lister(filtres, true, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fiche d'un lot de terrain")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fiche du lot."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public LotissementDetailDto lire(@Parameter(description = ID) @PathVariable UUID id) {
        return lotissements.lire(id);
    }

    @GetMapping("/{id}/fiche.pdf")
    @Operation(summary = "Fiche PDF du lot (inline), mêmes droit et visibilité que le détail")
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    public ResponseEntity<byte[]> fichePdf(@Parameter(description = ID) @PathVariable UUID id) {
        return fichePdf.reponse(FicheLotissement.fiche(lotissements.lire(id)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Crée un lot de terrain dans une tranche (nature TER, entité « article », sans statut de workflow)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lot créé."),
            @ApiResponse(responseCode = "400", description = SAISIE_INVALIDE),
            @ApiResponse(responseCode = "403", description = REPONSE_403),
            @ApiResponse(responseCode = "409", description = "Code déjà pris dans la tranche.")
    })
    public LotissementDetailDto creer(@RequestBody LotissementEcritureDto saisie) {
        return lotissements.creer(saisie);
    }

    @PatchMapping("/{id}")
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Modifie un lot de terrain (champ absent ou null = inchangé)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lot modifié."),
            @ApiResponse(responseCode = "400", description = SAISIE_INVALIDE),
            @ApiResponse(responseCode = "403", description = REPONSE_403),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Code déjà pris, champ protégé d'un lot occupé, ou changement de "
                    + "tranche d'un lot occupé ou ayant des prospections.")
    })
    public LotissementDetailDto modifier(@Parameter(description = ID) @PathVariable UUID id,
                                         @RequestBody LotissementEcritureDto saisie) {
        return lotissements.modifier(id, saisie);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Supprime un lot avec ses contenances, tracés, notes et pièces jointes")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Lot supprimé."),
            @ApiResponse(responseCode = "403", description = REPONSE_403),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Lot occupé, frais validé, ou encore référencé (prospection…).")
    })
    public void supprimer(@Parameter(description = ID) @PathVariable UUID id) {
        lotissements.supprimer(id);
    }
}
