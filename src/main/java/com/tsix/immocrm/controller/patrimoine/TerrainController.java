package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.patrimoine.FiltresTerrains;
import com.tsix.immocrm.dto.patrimoine.TerrainDetailDto;
import com.tsix.immocrm.dto.patrimoine.TerrainEcritureDto;
import com.tsix.immocrm.dto.patrimoine.TerrainListeDto;
import com.tsix.immocrm.domain.TypeExport;
import com.tsix.immocrm.services.patrimoine.fiche.FichePdf;
import com.tsix.immocrm.services.patrimoine.fiche.FichesPatrimoine;
import com.tsix.immocrm.services.patrimoine.terrain.TerrainService;
import com.tsix.immocrm.web.export.ColonneFixe;
import com.tsix.immocrm.web.export.DemandeExport;
import com.tsix.immocrm.web.export.ExportListe;
import com.tsix.immocrm.web.export.LignesExport;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import java.util.List;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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

@Tag(name = "Terrains")
@RestController
@RequestMapping("/api/terrains")
public class TerrainController {

    static final String ID = "Id du terrain (StkItem.PKey).";
    static final String INTROUVABLE = "Terrain inconnu, d'une autre nature, ou d'une société inactive.";
    private static final String SAISIE_INVALIDE = "Champ obligatoire absent, référence inconnue ou d'une autre nature.";

    /** Colonnes de {@code TerrainsList.tsx} ; la colonne Tranche n'est que dans les lotissements. */
    private static final List<ColonneFixe> COLONNES_TERRAINS = List.of(ColonneFixe.texte("code", "Code"),
            ColonneFixe.texte("description", "Description"), ColonneFixe.texte("entite", "Société"),
            ColonneFixe.texte("ville", "Ville"), ColonneFixe.texte("statut", "Statut"),
            ColonneFixe.texte("vocation", "Vocation"), ColonneFixe.texte("tranche", "Tranche"),
            ColonneFixe.de("surfaceDefinitive", "Surface définitive", TypeExport.NOMBRE),
            ColonneFixe.de("prixDefinitifTTC", "Prix définitif TTC", TypeExport.MONTANT),
            ColonneFixe.texte("titreFoncier", "Titre foncier"), ColonneFixe.de("occupe", "Occupé", TypeExport.BOOLEEN));

    private final TerrainService terrains;
    private final ExportListe export;
    private final FichePdf fichePdf;

    public TerrainController(TerrainService terrains, ExportListe export, FichePdf fichePdf) {
        this.terrains = terrains;
        this.export = export;
        this.fichePdf = fichePdf;
    }

    @GetMapping
    @Operation(summary = "Liste des terrains (nature TER), lotissements compris")
    @ApiResponse(responseCode = "200", description = "Page de terrains des sociétés actives.")
    public PageDto<TerrainListeDto> lister(
            @ParameterObject @ModelAttribute FiltresTerrains filtres,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "code", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageDto.of(terrains.lister(filtres, false, pageable));
    }

    @GetMapping("/export")
    @Operation(summary = "Export xlsx ou pdf de la liste des terrains", description = ExportListe.DESCRIPTION)
    @ApiResponse(responseCode = "400", description = ExportListe.REFUS, content = @Content)
    public ResponseEntity<byte[]> exporter(
            @ParameterObject @ModelAttribute FiltresTerrains filtres,
            @ParameterObject @ModelAttribute DemandeExport demande,
            @Parameter(hidden = true) @SortDefault(sort = "code", direction = Sort.Direction.ASC) Sort sort) {
        return exporter(export, terrains, filtres, demande, sort, false);
    }

    /** Export partagé avec {@code /api/lotissements/export} : même lecture que la liste. */
    static ResponseEntity<byte[]> exporter(ExportListe export, TerrainService terrains, FiltresTerrains filtres,
                                                          DemandeExport demande, Sort sort, boolean lotissements) {
        List<ColonneFixe> catalogue = COLONNES_TERRAINS.stream()
                .filter(c -> lotissements || !"tranche".equals(c.nom())).toList();
        return export.reponse(demande, lotissements ? "lotissements" : "terrains",
                lotissements ? "Lotissements" : "Terrains", ColonneFixe.choisir(demande.colonnes(), catalogue),
                () -> LignesExport.toutes(sort, page -> terrains.lister(filtres, lotissements, page)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fiche terrain, coût total et coût unitaire final calculés à la lecture")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fiche du terrain."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public TerrainDetailDto lire(@Parameter(description = ID) @PathVariable UUID id) {
        return terrains.lire(id);
    }

    @GetMapping("/{id}/fiche.pdf")
    @Operation(summary = "Fiche PDF du terrain (inline), mêmes droit et visibilité que le détail")
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    public ResponseEntity<byte[]> fichePdf(@Parameter(description = ID) @PathVariable UUID id) {
        return fichePdf.reponse(FichesPatrimoine.terrain(terrains.lire(id)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Crée un terrain au premier statut de l'entité LandProject")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Terrain créé."),
            @ApiResponse(responseCode = "400", description = SAISIE_INVALIDE),
            @ApiResponse(responseCode = "403", description = REPONSE_403),
            @ApiResponse(responseCode = "409", description = "Code déjà pris dans la société."),
            @ApiResponse(responseCode = "500", description = "Aucun statut pour l'entité LandProject : rien n'est écrit.")
    })
    public TerrainDetailDto creer(@RequestBody TerrainEcritureDto saisie) {
        return terrains.creer(saisie);
    }

    @PatchMapping("/{id}")
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Modifie un terrain (champ absent ou null = inchangé)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Terrain modifié."),
            @ApiResponse(responseCode = "400", description = SAISIE_INVALIDE),
            @ApiResponse(responseCode = "403", description = REPONSE_403),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Code déjà pris, ou champ protégé d'un terrain occupé.")
    })
    public TerrainDetailDto modifier(@Parameter(description = ID) @PathVariable UUID id,
                                     @RequestBody TerrainEcritureDto saisie) {
        return terrains.modifier(id, saisie);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Supprime un terrain et ses frais non validés")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Terrain supprimé avec ses frais non validés."),
            @ApiResponse(responseCode = "403", description = REPONSE_403),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Frais validé ou facturé, terrain occupé ou encore référencé.")
    })
    public void supprimer(@Parameter(description = ID) @PathVariable UUID id) {
        terrains.supprimer(id);
    }

    @PostMapping("/{id}/sous-terrains")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Crée un sous-terrain si le statut du parent l'autorise")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sous-terrain créé."),
            @ApiResponse(responseCode = "400", description = SAISIE_INVALIDE),
            @ApiResponse(responseCode = "403", description = REPONSE_403),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Statut du parent sans « sous-terrain », ou code déjà pris.")
    })
    public TerrainDetailDto creerSousTerrain(@Parameter(description = ID) @PathVariable UUID id,
                                             @RequestBody TerrainEcritureDto saisie) {
        return terrains.creerSousTerrain(id, saisie);
    }
}
