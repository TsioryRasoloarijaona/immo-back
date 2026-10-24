package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.dossier.PieceJointeDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.DemandeTransitionEtudeDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.EtudeFaisabiliteDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.HistoriqueStatutEtudeDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.TransitionPossibleDto;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.patrimoine.ExportEtudeFaisabilite;
import com.tsix.immocrm.services.patrimoine.PiecesJointesEtudeService;
import com.tsix.immocrm.services.patrimoine.TransitionFaisabiliteService;
import com.tsix.immocrm.services.piecejointe.SaisiePieceJointe;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.patrimoine.EtudeFaisabiliteController.ADMIN;
import static com.tsix.immocrm.controller.patrimoine.EtudeFaisabiliteController.BLOQUEE;
import static com.tsix.immocrm.controller.patrimoine.EtudeFaisabiliteController.ID_ETUDE;
import static com.tsix.immocrm.controller.patrimoine.EtudeFaisabiliteController.INTROUVABLE;
import static com.tsix.immocrm.controller.patrimoine.EtudeFaisabiliteController.PAS_ADMIN;
import static com.tsix.immocrm.controller.patrimoine.EtudeFaisabiliteController.PAS_LECTEUR;

@Tag(name = "Études de faisabilité — statuts, pièces jointes, export")
@RestController
@RequestMapping("/api/etudes-faisabilite/{id}")
@PreAuthorize(EtudeFaisabiliteController.LECTURE)
public class TransitionFaisabiliteController {

    private static final String PIECES = "/pieces-jointes";
    private static final String PIECE = PIECES + "/{pieceId}";
    private static final String ID_PIECE = "Id de la pièce jointe (AdmAttachedFile.PKey).";

    private final TransitionFaisabiliteService transitions;
    private final PiecesJointesEtudeService pieces;
    private final ExportEtudeFaisabilite export;

    public TransitionFaisabiliteController(TransitionFaisabiliteService transitions, PiecesJointesEtudeService pieces,
                                           ExportEtudeFaisabilite export) {
        this.transitions = transitions;
        this.pieces = pieces;
        this.export = export;
    }

    @GetMapping("/transitions")
    @Operation(summary = "Transitions empruntables depuis le statut courant par un rôle de l'appelant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transitions par ordre du statut d'arrivée (peut être vide)."),
            @ApiResponse(responseCode = "403", description = PAS_LECTEUR),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public List<TransitionPossibleDto> transitions(@Parameter(description = ID_ETUDE) @PathVariable UUID id) {
        return transitions.lister(id);
    }

    @PostMapping("/transitions")
    @Operation(summary = "Exécute une transition : contrôles OMRAN, statut, historique, recalcul")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fiche au nouveau statut."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "403", description = PAS_LECTEUR),
            @ApiResponse(responseCode = "409"),
            @ApiResponse(responseCode = "404", description = "Étude inconnue ou de société inactive, ou transition inconnue.")
    })
    public EtudeFaisabiliteDto executerTransition(@Parameter(description = ID_ETUDE) @PathVariable UUID id,
                                                  @RequestBody DemandeTransitionEtudeDto demande) {
        return transitions.modifierStatut(id, demande);
    }

    @GetMapping("/historique")
    @Operation(summary = "Historique des statuts de l'étude, par date")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lignes par date croissante."),
            @ApiResponse(responseCode = "403", description = PAS_LECTEUR),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public List<HistoriqueStatutEtudeDto> historique(@Parameter(description = ID_ETUDE) @PathVariable UUID id) {
        return transitions.historique(id);
    }

    @GetMapping(PIECES)
    @Operation(summary = "Pièces jointes de l'étude, la plus récente d'abord")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pièces vivantes (peut être vide)."),
            @ApiResponse(responseCode = "403", description = PAS_LECTEUR),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public List<PieceJointeDto> pieces(@Parameter(description = ID_ETUDE) @PathVariable UUID id) {
        return pieces.lister(id);
    }

    @GetMapping(PIECE + "/fichier")
    @Operation(summary = "Télécharge une pièce jointe de l'étude (redirection vers l'URL présignée)")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirection vers l'URL présignée du fichier."),
            @ApiResponse(responseCode = "403", description = PAS_LECTEUR),
            @ApiResponse(responseCode = "404", description = "Étude inconnue ou masquée, pièce d'un autre porteur ou sans fichier.")
    })
    public ResponseEntity<Void> fichier(@Parameter(description = ID_ETUDE) @PathVariable UUID id,
                                        @Parameter(description = ID_PIECE) @PathVariable UUID pieceId) {
        return ResponseEntity.status(HttpStatus.FOUND).location(pieces.urlFichier(id, pieceId)).build();
    }

    @PostMapping(value = PIECES, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMIN)
    @Operation(summary = "Dépose une pièce jointe sur l'étude, avec son type de document")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pièce jointe créée."),
            @ApiResponse(responseCode = "400", description = "Fichier absent ou vide, sans extension ou d'extension non admise."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = "Étude inconnue ou de société inactive, ou typeId inconnu."),
            @ApiResponse(responseCode = "409", description = BLOQUEE),
            @ApiResponse(responseCode = "413", description = "Fichier de plus de 50 Mo.")
    })
    public PieceJointeDto ajouterPiece(
            @Parameter(description = ID_ETUDE) @PathVariable UUID id,
            @Parameter(description = "Contenu du fichier à déposer.") @RequestParam("fichier") MultipartFile fichier,
            @Parameter(description = "Légende libre. Absent = nom de fichier d'origine (assaini).")
            @RequestParam(required = false) String description,
            @Parameter(description = "Type de document (AdmAttachedFileType.PKey) : c'est lui qui satisfait un document exigé.")
            @RequestParam(required = false) UUID typeId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return pieces.ajouterPieceJointe(id, fichier, new SaisiePieceJointe(description, typeId, null), principal);
    }

    @DeleteMapping(PIECE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ADMIN)
    @Operation(summary = "Supprime (logiquement) une pièce jointe de l'étude")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pièce retirée des listes ; elle ne compte plus pour un document exigé."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = "Étude inconnue ou masquée, pièce d'un autre porteur ou déjà supprimée."),
            @ApiResponse(responseCode = "409", description = BLOQUEE)
    })
    public void supprimerPiece(@Parameter(description = ID_ETUDE) @PathVariable UUID id,
                               @Parameter(description = ID_PIECE) @PathVariable UUID pieceId,
                               @AuthenticationPrincipal AuthenticatedUser principal) {
        pieces.supprimerPieceJointe(id, pieceId, principal);
    }

    @GetMapping("/export.xlsx")
    @Operation(summary = "Export Excel de l'étude en 11 feuilles (étude bloquée comprise)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Classeur etude-<code>_<horodatage>.xlsx."),
            @ApiResponse(responseCode = "403", description = PAS_LECTEUR),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public ResponseEntity<byte[]> exporter(@Parameter(description = ID_ETUDE) @PathVariable UUID id) {
        return export.exporter(id);
    }
}
