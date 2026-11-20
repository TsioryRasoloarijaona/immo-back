package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.domain.TypeExport;
import com.tsix.immocrm.dto.achat.demandes.DemandeAchatFiche;
import com.tsix.immocrm.dto.achat.demandes.DemandeAchatLigne;
import com.tsix.immocrm.dto.achat.demandes.ExecutionTransitionDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.FiltresDemandesAchat;
import com.tsix.immocrm.dto.achat.demandes.HistoriqueDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.SaisieDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.TransitionDemandeAchat;
import com.tsix.immocrm.services.achat.demandes.DemandesAchatService;
import com.tsix.immocrm.web.export.ColonneFixe;
import com.tsix.immocrm.web.export.ExportTableur;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.UUID;

@Tag(name = "Achat")
@RestController
@RequestMapping("/api/achat/demandes-achat")
public class DemandesAchatController {

    private static final String ID_DEMANDE = "Id de la demande (BuySelection.PKey).";
    private static final String HORS_PERIMETRE = "Demande inconnue ou hors périmètre (société, projet, statut).";
    private static final String SANS_DROIT = "Droit OMRAN manquant.";

    private static final List<ColonneFixe> COLONNES_EXPORT = List.of(ColonneFixe.texte("procedure", "Procédure"),
            ColonneFixe.texte("statut.libelle", "Statut"), ColonneFixe.texte("code", "Code"),
            ColonneFixe.de("date", "Date", TypeExport.DATE), ColonneFixe.texte("description", "Description"),
            ColonneFixe.texte("entite", "Entité"), ColonneFixe.texte("projet", "Projet"), ColonneFixe.texte("lot", "Lot"),
            ColonneFixe.texte("module", "Module"), ColonneFixe.texte("fournisseur", "Fournisseur"),
            ColonneFixe.texte("categorie", "Catégorie"), ColonneFixe.texte("declencheur", "Déclencheur"),
            ColonneFixe.texte("notes", "Notes"), ColonneFixe.de("montantEstimeTTC", "Estimé TTC", TypeExport.MONTANT));

    private final DemandesAchatService service;
    private final ExportTableur exportTableur;

    public DemandesAchatController(DemandesAchatService service, ExportTableur exportTableur) {
        this.service = service;
        this.exportTableur = exportTableur;
    }

    @GetMapping
    @Operation(summary = "Liste des demandes internes d'achat visibles")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demandes visibles retenues par les filtres, plus récentes d'abord."),
            @ApiResponse(responseCode = "400", description = "Filtre mal formé ou champDate inconnu.", content = @Content),
            @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content)
    })
    public List<DemandeAchatLigne> lister(@ParameterObject @ModelAttribute FiltresDemandesAchat filtres) {
        return service.liste(filtres);
    }

    @GetMapping("/export")
    @Operation(summary = "Export Excel de la liste des demandes internes d'achat, mêmes filtres")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Classeur .xlsx, feuille DONNEES.",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "400", description = "Filtre mal formé ou format autre que xlsx.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Droit de lecture ou d'export manquant.", content = @Content)
    })
    public ResponseEntity<StreamingResponseBody> exporter(
            @Parameter(example = ExportTableur.FORMAT_XLSX)
            @RequestParam(defaultValue = ExportTableur.FORMAT_XLSX) String format,
            @ParameterObject @ModelAttribute FiltresDemandesAchat filtres) {
        ExportTableur.exigerXlsx(format);
        return exportTableur.reponse("demandes-achat", "Demandes internes d'achat", COLONNES_EXPORT, service.export(filtres));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fiche d'une demande interne d'achat")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demande visible."),
            @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content),
            @ApiResponse(responseCode = "404", description = HORS_PERIMETRE, content = @Content)
    })
    public DemandeAchatFiche fiche(@Parameter(description = ID_DEMANDE) @PathVariable UUID id) {
        return service.fiche(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crée une demande interne d'achat au statut de départ de l'entité de sa procédure")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Demande créée."),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "403", content = @Content)
    })
    public DemandeAchatFiche creer(@RequestBody(required = false) SaisieDemandeAchat saisie) {
        return service.creer(saisie);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifie l'en-tête d'une demande interne d'achat")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demande modifiée."),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "403", description = "Droit toolNew manquant.", content = @Content),
            @ApiResponse(responseCode = "404", description = HORS_PERIMETRE, content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)
    })
    public DemandeAchatFiche modifier(@Parameter(description = ID_DEMANDE) @PathVariable UUID id,
                                      @RequestBody(required = false) SaisieDemandeAchat saisie) {
        return service.modifier(id, saisie);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprime une demande interne d'achat et ses lignes filles")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Demande supprimée."),
            @ApiResponse(responseCode = "403", description = "Droit toolDelete manquant.", content = @Content),
            @ApiResponse(responseCode = "404", description = HORS_PERIMETRE, content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)
    })
    public void supprimer(@Parameter(description = ID_DEMANDE) @PathVariable UUID id) {
        service.supprimer(id);
    }

    @GetMapping("/{id}/transitions")
    @Operation(summary = "Transitions de statut exécutables par l'appelant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transitions permises ; vide si aucune."),
            @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content),
            @ApiResponse(responseCode = "404", description = HORS_PERIMETRE, content = @Content)
    })
    public List<TransitionDemandeAchat> transitions(@Parameter(description = ID_DEMANDE) @PathVariable UUID id) {
        return service.transitions(id);
    }

    @PostMapping("/{id}/transitions")
    @Operation(summary = "Exécute une transition de statut")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transition exécutée ; fiche relue."),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "403", content = @Content),
            @ApiResponse(responseCode = "404", description = HORS_PERIMETRE, content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)
    })
    public DemandeAchatFiche executerTransition(@Parameter(description = ID_DEMANDE) @PathVariable UUID id,
                                                @RequestBody(required = false) ExecutionTransitionDemandeAchat execution) {
        return service.executer(id, execution);
    }

    @GetMapping("/{id}/historique")
    @Operation(summary = "Historique des statuts de la demande (StrHistory), le plus récent d'abord")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lignes d'historique."),
            @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content),
            @ApiResponse(responseCode = "404", description = HORS_PERIMETRE, content = @Content)
    })
    public List<HistoriqueDemandeAchat> historique(@Parameter(description = ID_DEMANDE) @PathVariable UUID id) {
        return service.historique(id);
    }
}
