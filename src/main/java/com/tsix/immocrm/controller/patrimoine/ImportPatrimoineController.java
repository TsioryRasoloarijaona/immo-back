package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.patrimoine.importation.AnalyseImportDto;
import com.tsix.immocrm.dto.patrimoine.importation.BilanImportDto;
import com.tsix.immocrm.dto.patrimoine.importation.ColonneImportDto;
import com.tsix.immocrm.services.patrimoine.importation.ClasseurTropGrosException;
import com.tsix.immocrm.services.patrimoine.importation.ImportPatrimoineService;
import com.tsix.immocrm.services.patrimoine.importation.ModeleImport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.patrimoine.ArticleController.ADMINISTRATEUR;
import static com.tsix.immocrm.controller.patrimoine.ArticleController.REPONSE_403;
import static com.tsix.immocrm.services.patrimoine.importation.ImportPatrimoineService.ARTICLES;
import static com.tsix.immocrm.services.patrimoine.importation.ImportPatrimoineService.ENTITES_JURIDIQUES;
import static com.tsix.immocrm.services.patrimoine.importation.ImportPatrimoineService.LOTISSEMENTS;
import static com.tsix.immocrm.services.patrimoine.importation.ImportPatrimoineService.PRESENTATIONS;
import static com.tsix.immocrm.services.patrimoine.importation.ImportPatrimoineService.PREVISIONS;
import static com.tsix.immocrm.services.patrimoine.importation.ImportPatrimoineService.PROJETS;
import static com.tsix.immocrm.services.patrimoine.importation.ImportPatrimoineService.PROSPECTIONS_TERRAIN;
import static com.tsix.immocrm.services.patrimoine.importation.ImportPatrimoineService.SOUS_ARTICLES;
import static com.tsix.immocrm.services.patrimoine.importation.ImportPatrimoineService.TERRAINS;
import static com.tsix.immocrm.services.patrimoine.importation.ImportPatrimoineService.TRANCHES;

@Tag(name = "Import du patrimoine")
@RestController
@RequestMapping("/api/import-patrimoine/{type}")
public class ImportPatrimoineController {

    private static final String TYPE = "Type importé.";
    private static final String TRANCHE = "Tranche visée (CfgTranche.PKey) : articles, sous-articles, prévisions ; ignorée sinon.";
    private static final String REPONSE_404 = "Type inconnu, ou tranche inconnue ou d'une entité inactive.";
    private static final String REPONSE_400_TRANCHE = "« Vous devez sélectionner une Tranche ! » (trancheId absent, type à tranche).";
    private static final String REPONSE_404_MAPPING = "Type inconnu ou sans mapping (articles, sous-articles, présentations…).";
    private static final String TROP_GROS = "Fichier de plus de 10 Mo.";
    private static final String FICHIER = "Classeur .xlsx (10 Mo, 1 000 lignes de données au plus).";
    private static final String FEUILLE = "Nom de la feuille à lire ; absent = la première.";

    private final ImportPatrimoineService importation;
    private final ModeleImport modele;

    public ImportPatrimoineController(ImportPatrimoineService importation, ModeleImport modele) {
        this.importation = importation;
        this.modele = modele;
    }

    @GetMapping("/modele")
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Classeur modèle pré-rempli (lignes de la tranche, terrains visibles ou présentations)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Classeur .xlsx : feuille de données puis feuilles d'aide."),
            @ApiResponse(responseCode = "400", description = REPONSE_400_TRANCHE, content = @Content),
            @ApiResponse(responseCode = "403", description = REPONSE_403, content = @Content),
            @ApiResponse(responseCode = "404", description = REPONSE_404, content = @Content)
    })
    public ResponseEntity<byte[]> modele(
            @Parameter(description = TYPE, schema = @Schema(allowableValues = {ARTICLES, SOUS_ARTICLES, PRESENTATIONS,
                    TERRAINS, LOTISSEMENTS, PREVISIONS, ENTITES_JURIDIQUES, PROJETS, TRANCHES, PROSPECTIONS_TERRAIN}))
            @PathVariable String type,
            @Parameter(description = TRANCHE) @RequestParam(required = false) UUID trancheId) {
        return modele.classeur(type, trancheId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Importe un classeur .xlsx (simulation par défaut), tout ou rien")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bilan ; ecrit = vrai seulement sans erreur hors simulation."),
            @ApiResponse(responseCode = "400", description = "Fichier ou mapping refusé en entier (voir la description) ; "
                    + "rien n'est écrit.", content = @Content),
            @ApiResponse(responseCode = "403", description = REPONSE_403, content = @Content),
            @ApiResponse(responseCode = "404", description = REPONSE_404, content = @Content),
            @ApiResponse(responseCode = "413", description = TROP_GROS, content = @Content)
    })
    public BilanImportDto importer(
            @Parameter(description = TYPE, schema = @Schema(allowableValues = {ARTICLES, SOUS_ARTICLES, PRESENTATIONS,
                    TERRAINS, LOTISSEMENTS, PREVISIONS, ENTITES_JURIDIQUES, PROJETS, TRANCHES, PROSPECTIONS_TERRAIN}))
            @PathVariable String type,
            @Parameter(description = TRANCHE) @RequestParam(required = false) UUID trancheId,
            @Parameter(description = "Vrai (défaut) = « Vérifier les données » : même exécution, puis tout est annulé.")
            @RequestParam(defaultValue = "true") boolean simulation,
            @Parameter(description = FICHIER)
            @RequestParam("fichier") MultipartFile fichier,
            @Parameter(description = FEUILLE)
            @RequestParam(required = false) String feuille,
            @Parameter(description = "Champ du formulaire : MappingImportDto en JSON (types à mapping) ; absent = en-têtes.")
            @RequestParam(required = false) String mapping) {
        return importation.importer(type, trancheId, fichier, feuille, simulation, mapping);
    }

    @GetMapping("/colonnes")
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Colonnes cibles d'un type à mapping (écran de mapping)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Catalogue, dans l'ordre des colonnes OMRAN."),
            @ApiResponse(responseCode = "403", description = REPONSE_403, content = @Content),
            @ApiResponse(responseCode = "404", description = REPONSE_404_MAPPING, content = @Content)
    })
    public List<ColonneImportDto> colonnes(
            @Parameter(description = TYPE, schema = @Schema(allowableValues = {ENTITES_JURIDIQUES, PROJETS, TRANCHES,
                    PROSPECTIONS_TERRAIN}))
            @PathVariable String type) {
        return importation.colonnes(type);
    }

    @PostMapping(value = "/analyse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Lit un classeur et propose un mapping, sans rien écrire")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feuilles, en-têtes, mapping proposé, aperçu, avertissements."),
            @ApiResponse(responseCode = "400", description = "Fichier refusé en entier (mêmes motifs que l'import).",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = REPONSE_403, content = @Content),
            @ApiResponse(responseCode = "404", description = REPONSE_404_MAPPING, content = @Content),
            @ApiResponse(responseCode = "413", description = TROP_GROS, content = @Content)
    })
    public AnalyseImportDto analyser(
            @Parameter(description = TYPE, schema = @Schema(allowableValues = {ENTITES_JURIDIQUES, PROJETS, TRANCHES,
                    PROSPECTIONS_TERRAIN}))
            @PathVariable String type,
            @Parameter(description = FICHIER) @RequestParam("fichier") MultipartFile fichier,
            @Parameter(description = FEUILLE) @RequestParam(required = false) String feuille) {
        return importation.analyser(type, fichier, feuille);
    }

    @ExceptionHandler(ClasseurTropGrosException.class)
    public ProblemDetail tropGros(ClasseurTropGrosException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage());
    }
}
