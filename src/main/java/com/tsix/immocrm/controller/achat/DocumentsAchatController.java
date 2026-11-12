package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.domain.TypeExport;
import com.tsix.immocrm.dto.achat.DocumentAchatDetailDto;
import com.tsix.immocrm.dto.achat.DocumentAchatListItemDto;
import com.tsix.immocrm.dto.achat.FiltresDocumentsAchat;
import com.tsix.immocrm.dto.achat.FiltresPositionsFournisseurs;
import com.tsix.immocrm.dto.achat.FiltresSoldesFournisseurs;
import com.tsix.immocrm.dto.achat.LignePositionDto;
import com.tsix.immocrm.dto.achat.PositionsFournisseursDto;
import com.tsix.immocrm.dto.achat.ReferentielsAchatDto;
import com.tsix.immocrm.dto.achat.SoldesFournisseursDto;
import com.tsix.immocrm.dto.achat.TotauxDocumentsAchatDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.services.achat.DocumentsAchatService;
import com.tsix.immocrm.services.achat.FicheAchatService;
import com.tsix.immocrm.services.achat.PositionsFournisseursService;
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

    private static final String AGREGATS = "agregats.";

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
            ColonneFixe.de("resteARegler", "Reste", TypeExport.MONTANT),
            ColonneFixe.texte("societeNom", "Entité"), ColonneFixe.texte("moduleLibelle", "Module"),
            ColonneFixe.texte("typeLotLibelle", "Type"), ColonneFixe.de("archive", "Archivé", TypeExport.BOOLEEN),
            ColonneFixe.de("comptabilise", "Comptabilisé", TypeExport.BOOLEEN),
            ColonneFixe.texte("referenceBureauOrdre", "Réf. bureau d'ordre"),
            ColonneFixe.texte("personneAContacter", "Personne à contacter"),
            ColonneFixe.texte("dossierGroupement", "Dossier de groupement"),
            ColonneFixe.texte("categories", "Catégorie"), ColonneFixe.texte("deviseCode", "Devise"),
            ColonneFixe.de("aCloturer", "À clôturer", TypeExport.BOOLEEN),
            bc("factureHT", "Montant facturé HT"), bc("factureTVA", "Montant facturé TVA"),
            bc("factureTTC", "Montant facturé TTC"), bc("factureNetAPayer", "Montant facturé net à payer"),
            bc("livreHT", "Montant livré HT"), bc("livreTTC", "Montant livré TTC"),
            bc("livreNetAPayer", "Montant livré net à payer"), bc("resteALivrerHT", "Reste à livrer HT"),
            bc("resteALivrerTTC", "Reste à livrer TTC"), bc("resteALivrerNetAPayer", "Reste à livrer net à payer"),
            bc("avance", "Avance"), ColonneFixe.de(AGREGATS + "avanceTauxPrevu", "Avance ratio", TypeExport.NOMBRE),
            ColonneFixe.de(AGREGATS + "avanceTauxEffectif", "Pourcentage effectif d'avance", TypeExport.NOMBRE),
            bc("avanceAmortie", "Avance amortie"), bc("avanceRegle", "Avance réglé"), bc("avanceReste", "Avance reste"),
            bc("avanceResteAAmortir", "Avance reste à amortir"), bc("garantieRetenue", "Retenue de garantie retenue"),
            bc("garantieRestituee", "Retenue de garantie restituée"), bc("garantieReste", "Retenue de garantie reste"),
            bc("regle", "Montant réglé"), bc("resteAPayerSurFacture", "Reste à payer sur facturé"),
            bc("resteAPayerTotal", "Reste à payer total"),
            ColonneFixe.texte(AGREGATS + "bonsLivraisonCodes", "Bons de livraison"),
            ColonneFixe.texte(AGREGATS + "facturesCodes", "Factures"), ColonneFixe.texte(AGREGATS + "devisCode", "Devis"));

    private static final List<ColonneFixe> COLONNES_SOLDES = List.of(
            ColonneFixe.texte("fournisseurNom", "Fournisseur"), ColonneFixe.texte("societeCode", "Société"),
            ColonneFixe.de("nombrePieces", "Pièces", TypeExport.NOMBRE),
            ColonneFixe.de("montantAPayer", "À payer", TypeExport.MONTANT),
            ColonneFixe.de("montantRegle", "Réglé", TypeExport.MONTANT),
            ColonneFixe.de("resteARegler", "Reste", TypeExport.MONTANT),
            ColonneFixe.de("nombreValideesARegler", "Validées à régler", TypeExport.NOMBRE),
            ColonneFixe.de("resteValideARegler", "Reste validé", TypeExport.MONTANT),
            ColonneFixe.de("montantADeduire", "À déduire", TypeExport.MONTANT));

    private static final List<ColonneFixe> COLONNES_POSITIONS = List.of(
            ColonneFixe.texte("fournisseurNom", "Fournisseur"), ColonneFixe.texte("societeCode", "Société"),
            ColonneFixe.de("soldeDebut", "Solde début", TypeExport.MONTANT),
            ColonneFixe.de("netAPayer", "Net à payer", TypeExport.MONTANT),
            ColonneFixe.de("regle", "Réglé", TypeExport.MONTANT),
            ColonneFixe.de("soldeFin", "Solde fin", TypeExport.MONTANT));

    /** Montant de l'objet {@code agregats} (bons de commande ; vide pour les autres pièces). */
    private static ColonneFixe bc(String nom, String libelle) {
        return ColonneFixe.de(AGREGATS + nom, libelle, TypeExport.MONTANT);
    }

    private final DocumentsAchatService service;
    private final FicheAchatService fiches;
    private final SoldesFournisseursService soldesFournisseurs;
    private final ExportTableur exportTableur;
    private final PositionsFournisseursService positionsFournisseurs;

    public DocumentsAchatController(DocumentsAchatService service, FicheAchatService fiches,
                                    SoldesFournisseursService soldesFournisseurs, ExportTableur exportTableur,
                                    PositionsFournisseursService positionsFournisseurs) {
        this.service = service;
        this.fiches = fiches;
        this.soldesFournisseurs = soldesFournisseurs;
        this.exportTableur = exportTableur;
        this.positionsFournisseurs = positionsFournisseurs;
    }

    @GetMapping("/referentiels")
    @Operation(summary = "Listes de choix des filtres : natures, statuts, projets, fournisseurs, sociétés, modules, types de lot")
    public ReferentielsAchatDto referentiels() {
        return service.referentiels();
    }

    @GetMapping("/documents")
    @Operation(summary = "Liste paginée des pièces d'achat visibles (société et projet habilités)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de pièces ; vide si aucune pièce visible (société, projet)."),
            @ApiResponse(responseCode = "400", content = @Content,
                    description = "Filtre mal formé (UUID, date, nature, module, champDate, montant) ou tri inconnu.")
    })
    public PageDto<DocumentAchatListItemDto> liste(@ParameterObject @ModelAttribute FiltresDocumentsAchat filtres,
                                                   @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(service.liste(filtres, pageable));
    }

    @GetMapping("/documents/totaux")
    @Operation(summary = "Totaux de la liste des pièces d'achat : mêmes filtres, toutes pages")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Totaux ; à zéro si aucune pièce visible (société, projet)."),
            @ApiResponse(responseCode = "400", content = @Content,
                    description = "Filtre mal formé (UUID, date, nature, module, champDate, montant, booléen).")
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
    @Operation(summary = "Fiche d'une pièce d'achat : lignes, ventilation, liens, historique, règlements")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pièce visible."),
            @ApiResponse(responseCode = "404",
                    description = "Pièce introuvable, hors périmètre (société, projet) ou bon de commande d'un autre déclencheur.",
                    content = @Content)
    })
    public DocumentAchatDetailDto fiche(@Parameter(description = "Id de la pièce (BuyDocument.PKey).")
                                        @PathVariable UUID id) {
        return fiches.fiche(id);
    }

    @GetMapping("/fournisseurs/soldes")
    @Operation(summary = "Solde par fournisseur et société, sur les pièces visibles (non paginé)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lignes et total ; vides si aucune pièce visible (société, projet)."),
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

    @GetMapping("/fournisseurs/positions")
    @Operation(summary = "Soldes fournisseurs comme OMRAN : solde début, net à payer, réglé, solde fin (non paginé)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lignes et total ; vides si aucun mouvement visible (société, projet)."),
            @ApiResponse(responseCode = "400", description = "Filtre mal formé (UUID, date).", content = @Content)
    })
    public PositionsFournisseursDto positions(@ParameterObject @ModelAttribute FiltresPositionsFournisseurs filtres) {
        return positionsFournisseurs.positions(filtres);
    }

    @GetMapping("/fournisseurs/positions/export")
    @Operation(summary = "Export Excel des soldes fournisseurs, mêmes filtres et mêmes lignes")
    @ApiResponses({
            @ApiResponse(responseCode = "400", content = @Content,
                    description = "Filtre mal formé ou format autre que xlsx."),
            @ApiResponse(responseCode = "200", description = "Classeur .xlsx, feuille DONNEES.",
                    content = @Content(schema = @Schema(type = "string", format = "binary"),
                            mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
    })
    public ResponseEntity<StreamingResponseBody> exporterPositions(
            @RequestParam(defaultValue = ExportTableur.FORMAT_XLSX) @Parameter(example = ExportTableur.FORMAT_XLSX)
            String format, @ParameterObject @ModelAttribute FiltresPositionsFournisseurs filtres) {
        ExportTableur.exigerXlsx(format);
        return exportTableur.reponse("soldes-fournisseurs", "Soldes fournisseurs", COLONNES_POSITIONS,
                positionsFournisseurs.positions(filtres).lignes());
    }

    @GetMapping("/fournisseurs/{fournisseurId}/position")
    @Operation(summary = "Lignes de solde d'un fournisseur dans une société (fenêtre « Ligne de solde » d'OMRAN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lignes ; vide si fournisseur inconnu ou hors périmètre."),
            @ApiResponse(responseCode = "400", description = "societeId absent ou filtre mal formé (UUID, date).",
                    content = @Content)
    })
    public List<LignePositionDto> lignesPosition(
            @Parameter(description = "Fournisseur (CfgTier.id).") @PathVariable UUID fournisseurId,
            @Parameter(description = "Société (CfgCompany.id) de la ligne de solde.") @RequestParam UUID societeId,
            @ParameterObject @ModelAttribute FiltresPositionsFournisseurs filtres) {
        return positionsFournisseurs.lignes(fournisseurId, societeId, filtres);
    }
}
