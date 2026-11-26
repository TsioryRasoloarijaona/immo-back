package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.dto.achat.demandes.BonCommandeDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.DemandeAchatFiche;
import com.tsix.immocrm.dto.achat.demandes.FournisseurDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.ImportOffreDemandeAchatDto;
import com.tsix.immocrm.dto.achat.demandes.ModaliteDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.OffreDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.PrixLigneOffre;
import com.tsix.immocrm.dto.achat.demandes.SaisieFournisseurDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.SaisieOffreDemandeAchat;
import com.tsix.immocrm.dto.projet.BordereauSaisieDto;
import com.tsix.immocrm.services.achat.demandes.OngletsDemandeAchatService;
import com.tsix.immocrm.web.export.ExportTableur;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.UUID;

/** Onglets de la fiche DIA (FEATURE-n6-demandes-achat.md « N6b ») : présélection, offres, modalités, bordereau, BC. */
@Tag(name = "Achat")
@RestController
@RequestMapping("/api/achat/demandes-achat/{id}")
public class DemandesAchatOngletsController {

    private static final String ID_DEMANDE = "Id de la demande (BuySelection.PKey).";
    private static final String ID_FOURNISSEUR = "Id de la présélection (BuySelectionSupplier.PKey).";
    private static final String ID_OFFRE = "Id du devis (BuyDocument.PKey).";
    private static final String HORS_PERIMETRE = "Demande inconnue ou hors périmètre (société, projet, statut).";
    private static final String CONFIDENTIEL = "Droit de lecture manquant, ou procédure confidentielle sans le droit IsConfidential.";

    private final OngletsDemandeAchatService service;

    public DemandesAchatOngletsController(OngletsDemandeAchatService service) {
        this.service = service;
    }

    // ------------------------------------------------------------------ présélection

    @GetMapping("/fournisseurs")
    @Operation(summary = "Fournisseurs présélectionnés de la demande")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fournisseurs, par raison sociale."),
            @ApiResponse(responseCode = "403", description = CONFIDENTIEL, content = @Content),
            @ApiResponse(responseCode = "404", description = HORS_PERIMETRE, content = @Content)
    })
    public List<FournisseurDemandeAchat> fournisseurs(@Parameter(description = ID_DEMANDE) @PathVariable UUID id) {
        return service.fournisseurs(id);
    }

    @PostMapping("/fournisseurs")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ajoute un fournisseur présélectionné")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Fournisseur ajouté."),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "403", content = @Content),
            @ApiResponse(responseCode = "404", description = HORS_PERIMETRE, content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)
    })
    public FournisseurDemandeAchat ajouterFournisseur(@Parameter(description = ID_DEMANDE) @PathVariable UUID id,
                                                      @RequestBody(required = false) SaisieFournisseurDemandeAchat saisie) {
        return service.ajouterFournisseur(id, saisie);
    }

    @PutMapping("/fournisseurs/{fid}")
    @Operation(summary = "Modifie un fournisseur présélectionné (références et dates)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fournisseur modifié."),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "403", content = @Content),
            @ApiResponse(responseCode = "404", content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)
    })
    public FournisseurDemandeAchat modifierFournisseur(@Parameter(description = ID_DEMANDE) @PathVariable UUID id,
                                                       @Parameter(description = ID_FOURNISSEUR) @PathVariable UUID fid,
                                                       @RequestBody(required = false) SaisieFournisseurDemandeAchat saisie) {
        return service.modifierFournisseur(id, fid, saisie);
    }

    @DeleteMapping("/fournisseurs/{fid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Retire un fournisseur présélectionné sans offre")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Fournisseur retiré."),
            @ApiResponse(responseCode = "403", content = @Content),
            @ApiResponse(responseCode = "404", content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)
    })
    public void supprimerFournisseur(@Parameter(description = ID_DEMANDE) @PathVariable UUID id,
                                     @Parameter(description = ID_FOURNISSEUR) @PathVariable UUID fid) {
        service.supprimerFournisseur(id, fid);
    }

    @PostMapping("/fournisseurs/{fid}/ecarter")
    @Operation(summary = "Écarte un fournisseur et toutes ses offres")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fournisseur écarté."),
            @ApiResponse(responseCode = "403", content = @Content),
            @ApiResponse(responseCode = "404", content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)
    })
    public FournisseurDemandeAchat ecarterFournisseur(@Parameter(description = ID_DEMANDE) @PathVariable UUID id,
                                                      @Parameter(description = ID_FOURNISSEUR) @PathVariable UUID fid) {
        return service.ecarterFournisseur(id, fid, true);
    }

    @PostMapping("/fournisseurs/{fid}/reactiver")
    @Operation(summary = "Réactive un fournisseur écarté et toutes ses offres")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fournisseur réactivé."),
            @ApiResponse(responseCode = "403", content = @Content),
            @ApiResponse(responseCode = "404", content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)
    })
    public FournisseurDemandeAchat reactiverFournisseur(@Parameter(description = ID_DEMANDE) @PathVariable UUID id,
                                                        @Parameter(description = ID_FOURNISSEUR) @PathVariable UUID fid) {
        return service.ecarterFournisseur(id, fid, false);
    }

    // ------------------------------------------------------------------ offres

    @GetMapping("/offres")
    @Operation(summary = "Offres (devis) de la demande, avec leurs lignes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offres, par fournisseur."),
            @ApiResponse(responseCode = "403", description = CONFIDENTIEL, content = @Content),
            @ApiResponse(responseCode = "404", description = HORS_PERIMETRE, content = @Content)
    })
    public List<OffreDemandeAchat> offres(@Parameter(description = ID_DEMANDE) @PathVariable UUID id) {
        return service.offres(id);
    }

    @PostMapping("/offres")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crée l'offre d'un fournisseur présélectionné : un devis recopiant le bordereau")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Devis créé."),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "403", content = @Content),
            @ApiResponse(responseCode = "404", description = HORS_PERIMETRE, content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)
    })
    public OffreDemandeAchat creerOffre(@Parameter(description = ID_DEMANDE) @PathVariable UUID id,
                                        @RequestBody(required = false) SaisieOffreDemandeAchat saisie) {
        return service.creerOffre(id, saisie);
    }

    @PutMapping("/offres/{oid}")
    @Operation(summary = "Saisit les prix et remises des lignes d'une offre")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offre relue."),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "403", content = @Content),
            @ApiResponse(responseCode = "404", content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)
    })
    public OffreDemandeAchat modifierOffre(@Parameter(description = ID_DEMANDE) @PathVariable UUID id,
                                           @Parameter(description = ID_OFFRE) @PathVariable UUID oid,
                                           @RequestBody(required = false) List<PrixLigneOffre> prix) {
        return service.modifierOffre(id, oid, prix);
    }

    @PostMapping(value = "/offres/{oid}/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importe les prix d'une offre depuis un classeur au format de l'export du bordereau")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offre relue et décompte des rangées."),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "403", content = @Content),
            @ApiResponse(responseCode = "404", content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)
    })
    public ImportOffreDemandeAchatDto importerOffre(@Parameter(description = ID_DEMANDE) @PathVariable UUID id,
                                                   @Parameter(description = ID_OFFRE) @PathVariable UUID oid,
                                                   @RequestPart("fichier") MultipartFile fichier) {
        return service.importerOffre(id, oid, fichier);
    }

    @DeleteMapping("/offres/{oid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprime une offre non sélectionnée")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Offre supprimée."),
            @ApiResponse(responseCode = "403", content = @Content),
            @ApiResponse(responseCode = "404", content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)
    })
    public void supprimerOffre(@Parameter(description = ID_DEMANDE) @PathVariable UUID id,
                               @Parameter(description = ID_OFFRE) @PathVariable UUID oid) {
        service.supprimerOffre(id, oid);
    }

    @PostMapping("/offres/{oid}/selection")
    @Operation(summary = "Retient une offre : seule sélectionnée, son fournisseur devient celui de la demande")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fiche de la demande relue."),
            @ApiResponse(responseCode = "403", content = @Content),
            @ApiResponse(responseCode = "404", content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)
    })
    public DemandeAchatFiche selectionnerOffre(@Parameter(description = ID_DEMANDE) @PathVariable UUID id,
                                               @Parameter(description = ID_OFFRE) @PathVariable UUID oid,
                                               @Parameter(description = "Confirme le choix de l'offre la plus disante ; défaut false.")
                                               @RequestParam(defaultValue = "false") boolean confirme) {
        return service.selectionnerOffre(id, oid, confirme);
    }

    @DeleteMapping("/offres/selection")
    @Operation(summary = "Désélectionne les offres et vide le bordereau de la demande")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fiche de la demande relue."),
            @ApiResponse(responseCode = "403", content = @Content),
            @ApiResponse(responseCode = "404", description = HORS_PERIMETRE, content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)
    })
    public DemandeAchatFiche deselectionnerOffres(@Parameter(description = ID_DEMANDE) @PathVariable UUID id) {
        return service.deselectionnerOffres(id);
    }

    @GetMapping("/comparaison")
    @Operation(summary = "Comparaison des offres non écartées, lignes rattachées au bordereau par le code")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offres non écartées avec leurs lignes."),
            @ApiResponse(responseCode = "403", description = CONFIDENTIEL, content = @Content),
            @ApiResponse(responseCode = "404", description = HORS_PERIMETRE, content = @Content),
            @ApiResponse(responseCode = "409", description = "Statut hors des phases offres et sélection.", content = @Content)
    })
    public List<OffreDemandeAchat> comparaison(@Parameter(description = ID_DEMANDE) @PathVariable UUID id) {
        return service.comparaison(id);
    }

    // ------------------------------------------------------------------ modalités de paiement

    @GetMapping("/modalites")
    @Operation(summary = "Modalités de paiement de la demande, par O.D.T.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Modalités."),
            @ApiResponse(responseCode = "403", description = CONFIDENTIEL, content = @Content),
            @ApiResponse(responseCode = "404", description = HORS_PERIMETRE, content = @Content)
    })
    public List<ModaliteDemandeAchat> modalites(@Parameter(description = ID_DEMANDE) @PathVariable UUID id) {
        return service.modalites(id);
    }

    @PostMapping("/modalites")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ajoute une modalité de paiement")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Modalité ajoutée."),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "403", content = @Content),
            @ApiResponse(responseCode = "404", description = HORS_PERIMETRE, content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)
    })
    public ModaliteDemandeAchat ajouterModalite(@Parameter(description = ID_DEMANDE) @PathVariable UUID id,
                                                @RequestBody(required = false) ModaliteDemandeAchat saisie) {
        return service.ajouterModalite(id, saisie);
    }

    @DeleteMapping("/modalites/{mid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Retire une modalité de paiement")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Modalité retirée."),
            @ApiResponse(responseCode = "403", content = @Content),
            @ApiResponse(responseCode = "404", content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)
    })
    public void supprimerModalite(@Parameter(description = ID_DEMANDE) @PathVariable UUID id,
                                  @Parameter(description = "Id de la modalité (BuySelectionPaymentTerm.PKey).") @PathVariable UUID mid) {
        service.supprimerModalite(id, mid);
    }

    // ------------------------------------------------------------------ bordereau

    @GetMapping("/bordereau")
    @Operation(summary = "Bordereau de la demande : groupes et lignes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bordereau, taux en pourcentage."),
            @ApiResponse(responseCode = "403", content = @Content),
            @ApiResponse(responseCode = "404", description = HORS_PERIMETRE, content = @Content)
    })
    public BordereauSaisieDto bordereau(@Parameter(description = ID_DEMANDE) @PathVariable UUID id) {
        return service.bordereau(id);
    }

    @PutMapping("/bordereau")
    @Operation(summary = "Remplace le bordereau de la demande (groupes et lignes)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bordereau relu."),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "403", content = @Content),
            @ApiResponse(responseCode = "404", description = HORS_PERIMETRE, content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)
    })
    public BordereauSaisieDto enregistrerBordereau(@Parameter(description = ID_DEMANDE) @PathVariable UUID id,
                                                   @RequestBody(required = false) BordereauSaisieDto bordereau) {
        return service.enregistrerBordereau(id, bordereau);
    }

    @PostMapping(value = "/bordereau/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Remplace le bordereau par celui d'un classeur au format de l'export")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bordereau relu."),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "403", content = @Content),
            @ApiResponse(responseCode = "404", description = HORS_PERIMETRE, content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)
    })
    public BordereauSaisieDto importerBordereau(@Parameter(description = ID_DEMANDE) @PathVariable UUID id,
                                                @RequestPart("fichier") MultipartFile fichier) {
        return service.importerBordereau(id, fichier);
    }

    @GetMapping("/bordereau/export")
    @Operation(summary = "Export Excel du bordereau de la demande")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Classeur .xlsx, feuille DONNEES, une ligne par ligne du bordereau.",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "400", description = "Format autre que xlsx.", content = @Content),
            @ApiResponse(responseCode = "403", content = @Content),
            @ApiResponse(responseCode = "404", description = HORS_PERIMETRE, content = @Content)
    })
    public ResponseEntity<StreamingResponseBody> exporterBordereau(
            @Parameter(description = ID_DEMANDE) @PathVariable UUID id,
            @Parameter(example = ExportTableur.FORMAT_XLSX) @RequestParam(defaultValue = ExportTableur.FORMAT_XLSX) String format) {
        ExportTableur.exigerXlsx(format);
        return service.exporterBordereau(id);
    }

    // ------------------------------------------------------------------ bons de commande, renouvellement

    @GetMapping("/bons-commande")
    @Operation(summary = "Pièces d'achat liées à la demande, hors devis")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pièces, plus récentes d'abord."),
            @ApiResponse(responseCode = "403", description = CONFIDENTIEL, content = @Content),
            @ApiResponse(responseCode = "404", description = HORS_PERIMETRE, content = @Content)
    })
    public List<BonCommandeDemandeAchat> bonsCommande(@Parameter(description = ID_DEMANDE) @PathVariable UUID id) {
        return service.bonsCommande(id);
    }

    @PostMapping("/renouvellement")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Lance un nouvel appel d'offre à partir d'une demande annulée")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Nouvelle demande créée."),
            @ApiResponse(responseCode = "403", content = @Content),
            @ApiResponse(responseCode = "404", description = HORS_PERIMETRE, content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)
    })
    public DemandeAchatFiche renouveler(@Parameter(description = ID_DEMANDE) @PathVariable UUID id) {
        return service.renouveler(id);
    }
}
