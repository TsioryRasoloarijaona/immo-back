package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.domain.TypeExport;
import com.tsix.immocrm.dto.achat.DocumentAchatDetailDto;
import com.tsix.immocrm.dto.achat.DocumentAchatListItemDto;
import com.tsix.immocrm.dto.achat.FiltresDocumentsAchat;
import com.tsix.immocrm.dto.achat.ReferentielsAchatDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.services.achat.DocumentsAchatService;
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

    private final DocumentsAchatService service;
    private final ExportTableur exportTableur;

    public DocumentsAchatController(DocumentsAchatService service, ExportTableur exportTableur) {
        this.service = service;
        this.exportTableur = exportTableur;
    }

    @GetMapping("/referentiels")
    @Operation(summary = "Listes de choix des filtres : natures, statuts, projets et fournisseurs visibles")
    public ReferentielsAchatDto referentiels() {
        return service.referentiels();
    }

    @GetMapping("/documents")
    @Operation(summary = "Liste paginée des pièces d'achat visibles (habilitations projet)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de pièces ; vide si l'appelant n'a aucun projet."),
            @ApiResponse(responseCode = "400", description = "Filtre mal formé (UUID, date, nature) ou tri inconnu.",
                    content = @Content)
    })
    public PageDto<DocumentAchatListItemDto> liste(@ParameterObject @ModelAttribute FiltresDocumentsAchat filtres,
                                                   @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(service.liste(filtres, pageable));
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
            @ApiResponse(responseCode = "404", description = "Pièce introuvable, sans projet ou hors périmètre.",
                    content = @Content)
    })
    public DocumentAchatDetailDto fiche(@Parameter(description = "Id de la pièce (BuyDocument.PKey).")
                                        @PathVariable UUID id) {
        return service.fiche(id);
    }
}
