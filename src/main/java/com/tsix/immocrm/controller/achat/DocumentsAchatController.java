package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.domain.TypeExport;
import com.tsix.immocrm.dto.achat.DocumentAchatDetailDto;
import com.tsix.immocrm.dto.achat.DocumentAchatListItemDto;
import com.tsix.immocrm.dto.achat.FiltresDocumentsAchat;
import com.tsix.immocrm.dto.achat.FiltresSoldesFournisseurs;
import com.tsix.immocrm.dto.achat.ReferentielsAchatDto;
import com.tsix.immocrm.dto.achat.SoldesFournisseursDto;
import com.tsix.immocrm.dto.achat.TotauxDocumentsAchatDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.services.achat.DocumentsAchatService;
import com.tsix.immocrm.services.achat.SoldesFournisseursService;
import com.tsix.immocrm.web.export.ColonneFixe;
import com.tsix.immocrm.web.export.ExportTableur;
import com.tsix.immocrm.web.export.LignesExport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Achat")
@RestController
@RequestMapping("/api/achat")
public class DocumentsAchatController {

    private static final List<ColonneFixe> COLONNES_EXPORT = List.of(ColonneFixe.texte("code", "N°"),
            ColonneFixe.texte("natureLibelle", "Nature"), ColonneFixe.de("dateDocument", "Date", TypeExport.DATE),
            ColonneFixe.de("dateEcheance", "Échéance", TypeExport.DATE), ColonneFixe.texte("reference", "Référence"),
            ColonneFixe.texte("description", "Description"), ColonneFixe.texte("fournisseurNom", "Fournisseur"),
            ColonneFixe.texte("projetCode", "Projet"), ColonneFixe.texte("lotCode", "Lot"),
            ColonneFixe.texte("societeCode", "Société"), ColonneFixe.texte("statutDescription", "Statut"),
            ColonneFixe.de("montantHT", "HT", TypeExport.MONTANT), ColonneFixe.de("montantTVA", "TVA", TypeExport.MONTANT),
            ColonneFixe.de("montantTTC", "TTC", TypeExport.MONTANT),
            ColonneFixe.de("montantAPayer", "À payer", TypeExport.MONTANT),
            ColonneFixe.de("montantRegle", "Réglé", TypeExport.MONTANT),
            ColonneFixe.de("resteARegler", "Reste", TypeExport.MONTANT));

    private static final List<ColonneFixe> COLONNES_SOLDES = List.of(
            ColonneFixe.texte("fournisseurNom", "Fournisseur"), ColonneFixe.texte("societeCode", "Société"),
            ColonneFixe.de("nombrePieces", "Pièces", TypeExport.NOMBRE),
            ColonneFixe.de("montantAPayer", "À payer", TypeExport.MONTANT),
            ColonneFixe.de("montantRegle", "Réglé", TypeExport.MONTANT),
            ColonneFixe.de("resteARegler", "Reste", TypeExport.MONTANT),
            ColonneFixe.de("nombreValideesARegler", "Validées à régler", TypeExport.NOMBRE),
            ColonneFixe.de("resteValideARegler", "Reste validé", TypeExport.MONTANT),
            ColonneFixe.de("montantADeduire", "À déduire", TypeExport.MONTANT));

    private final DocumentsAchatService service;
    private final SoldesFournisseursService soldesFournisseurs;
    private final ExportTableur exportTableur;

    public DocumentsAchatController(DocumentsAchatService service, SoldesFournisseursService soldesFournisseurs,
                                    ExportTableur exportTableur) {
        this.service = service;
        this.soldesFournisseurs = soldesFournisseurs;
        this.exportTableur = exportTableur;
    }

    @GetMapping("/referentiels")
    @Operation(summary = "Listes de choix des filtres : natures, statuts, projets et fournisseurs visibles")
    public ReferentielsAchatDto referentiels() {
        return service.referentiels();
    }

    @GetMapping("/documents")
    @Operation(summary = "Liste paginée des pièces d'achat visibles (société et projet habilités)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de pièces ; vide si l'appelant n'a aucun projet."),
            @ApiResponse(responseCode = "400", description = "Filtre mal formé (UUID, date, nature) ou tri inconnu.",
                    content = @Content)
    })
    public PageDto<DocumentAchatListItemDto> liste(@ParameterObject @ModelAttribute FiltresDocumentsAchat filtres,
                                                   @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(service.liste(filtres, pageable));
    }

    @GetMapping("/documents/totaux")
    @Operation(summary = "Totaux de la liste des pièces d'achat : mêmes filtres, toutes pages")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Totaux ; à zéro si l'appelant n'a aucun projet."),
            @ApiResponse(responseCode = "400", description = "Filtre mal formé (UUID, date, nature, booléen).",
                    content = @Content)
    })
    public TotauxDocumentsAchatDto totaux(@ParameterObject @ModelAttribute FiltresDocumentsAchat filtres) {
        return service.totaux(filtres);
    }

    @GetMapping("/documents/export")
    @Operation(summary = "Export Excel de la liste des pièces d'achat, mêmes filtres et même requête que la liste")
    @Parameter(name = "sort", in = ParameterIn.QUERY, example = "dateDocument,desc",
            description = "Tri `propriete,asc|desc`, répétable, comme la liste.",
            array = @ArraySchema(schema = @Schema(type = "string")))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Classeur .xlsx, feuille DONNEES.",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "400", description = "Filtre mal formé ou format autre que xlsx.",
                    content = @Content)
    })
    public ResponseEntity<StreamingResponseBody> exporter(
            @Parameter(example = ExportTableur.FORMAT_XLSX)
            @RequestParam(defaultValue = ExportTableur.FORMAT_XLSX) String format,
            @ParameterObject @ModelAttribute FiltresDocumentsAchat filtres,
            @Parameter(hidden = true) Sort sort) {
        ExportTableur.exigerXlsx(format);
        List<DocumentAchatListItemDto> lignes = LignesExport.toutes(sort, page -> service.liste(filtres, page));
        return exportTableur.reponse("pieces-achat", "Pièces d'achat", COLONNES_EXPORT, lignes);
    }

    @GetMapping("/documents/{id}")
    @Operation(summary = "Fiche d'une pièce d'achat : lignes, ventilation, historique, règlements")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pièce visible."),
            @ApiResponse(responseCode = "404", description = "Pièce introuvable ou hors périmètre (société, projet).",
                    content = @Content)
    })
    public DocumentAchatDetailDto fiche(@Parameter(description = "Id de la pièce (BuyDocument.PKey).")
                                        @PathVariable UUID id) {
        return service.fiche(id);
    }

    @GetMapping("/fournisseurs/soldes")
    @Operation(summary = "Solde par fournisseur et société, sur les pièces visibles (non paginé)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lignes et total ; vides si l'appelant n'a aucun projet."),
            @ApiResponse(responseCode = "400", description = "Filtre mal formé (UUID).", content = @Content)
    })
    public SoldesFournisseursDto soldes(@ParameterObject @ModelAttribute FiltresSoldesFournisseurs filtres) {
        return soldesFournisseurs.soldes(filtres);
    }

    @GetMapping("/fournisseurs/soldes/export")
    @Operation(summary = "Export Excel du solde fournisseurs, mêmes filtres et mêmes lignes")
    @ApiResponses({
            @ApiResponse(responseCode = "400", content = @Content,
                    description = "Filtre mal formé ou format autre que xlsx."),
            @ApiResponse(responseCode = "200", description = "Classeur .xlsx, feuille DONNEES.",
                    content = @Content(schema = @Schema(type = "string", format = "binary"),
                            mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
    })
    public ResponseEntity<StreamingResponseBody> exporterSoldes(
            @RequestParam(defaultValue = ExportTableur.FORMAT_XLSX) @Parameter(example = ExportTableur.FORMAT_XLSX)
            String format, @ParameterObject @ModelAttribute FiltresSoldesFournisseurs filtres) {
        ExportTableur.exigerXlsx(format);
        return exportTableur.reponse("soldes-fournisseurs", "Solde fournisseurs", COLONNES_SOLDES,
                soldesFournisseurs.soldes(filtres).lignes());
    }
}
