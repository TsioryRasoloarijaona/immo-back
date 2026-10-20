package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.dossier.PieceJointeDto;
import com.tsix.immocrm.dto.patrimoine.BilanAttachementDto;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.patrimoine.AttachementMasseService;
import com.tsix.immocrm.services.patrimoine.PiecesJointesPatrimoineService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Pièces jointes du patrimoine",
        description = "Pièces jointes des articles, tranches et programmes (AdmAttachedFile, CategoryId = PKey du porteur).")
@RestController
@RequestMapping("/api")
public class PiecesJointesPatrimoineController {

    private static final String PIECES = "/{porteur:articles|tranches|projets}/{id}/pieces-jointes";
    private static final String ADMINISTRATEUR = "hasRole('Administrateur')";
    private static final String PORTEUR = "Genre du porteur : articles, tranches ou projets.";
    private static final String ID_PORTEUR = "PKey du porteur (StkItem, CfgTranche ou CfgProject).";
    private static final String ID_PIECE = "Id de la pièce jointe (AdmAttachedFile.PKey).";
    private static final String REPONSE_403 = "L'appelant n'a pas le rôle Administrateur.";
    private static final String TYPE = "Type de document (AdmAttachedFileType.PKey), voir GET /api/referentiels/types-piece-jointe.";
    private static final String GROUPE = "Groupe libre (FoderGroup), voir GET /api/articles/groupes-piece-jointe.";

    private final PiecesJointesPatrimoineService service;
    private final AttachementMasseService masse;

    public PiecesJointesPatrimoineController(PiecesJointesPatrimoineService service, AttachementMasseService masse) {
        this.service = service;
        this.masse = masse;
    }

    @GetMapping(PIECES)
    @Operation(summary = "Liste les pièces jointes d'un article, d'une tranche ou d'un programme")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pièces jointes, la plus récente d'abord (peut être vide)."),
            @ApiResponse(responseCode = "404", description = "Porteur inconnu, ou de l'entité juridique inactive.")
    })
    public List<PieceJointeDto> lister(
            @Parameter(description = PORTEUR) @PathVariable String porteur,
            @Parameter(description = ID_PORTEUR) @PathVariable UUID id) {
        return service.lister(porteur, id);
    }

    @GetMapping(PIECES + "/{pieceId}/fichier")
    @Operation(summary = "Affiche ou télécharge une pièce jointe (redirection vers l'URL présignée)")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirection vers l'URL présignée du fichier."),
            @ApiResponse(responseCode = "404", description = "Porteur inconnu ou masqué, pièce d'un autre porteur, ou sans fichier.")
    })
    public ResponseEntity<Void> fichier(
            @Parameter(description = PORTEUR) @PathVariable String porteur,
            @Parameter(description = ID_PORTEUR) @PathVariable UUID id,
            @Parameter(description = ID_PIECE) @PathVariable UUID pieceId) {
        return ResponseEntity.status(HttpStatus.FOUND).location(service.urlFichier(porteur, id, pieceId)).build();
    }

    @PostMapping(value = PIECES, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Dépose une pièce jointe sur un article, une tranche ou un programme")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pièce jointe créée."),
            @ApiResponse(responseCode = "400", description = "Fichier absent ou vide, sans extension ou d'extension non admise."),
            @ApiResponse(responseCode = "403", description = REPONSE_403),
            @ApiResponse(responseCode = "404", description = "Porteur inconnu ou masqué, ou typeId inconnu."),
            @ApiResponse(responseCode = "413", description = "Fichier de plus de 50 Mo.")
    })
    public PieceJointeDto ajouter(
            @Parameter(description = PORTEUR) @PathVariable String porteur,
            @Parameter(description = ID_PORTEUR) @PathVariable UUID id,
            @Parameter(description = "Contenu du fichier à déposer.") @RequestParam("fichier") MultipartFile fichier,
            @Parameter(description = "Légende libre. Absent = nom de fichier d'origine (assaini).")
            @RequestParam(required = false) String description,
            @Parameter(description = TYPE) @RequestParam(required = false) UUID typeId,
            @Parameter(description = GROUPE) @RequestParam(required = false) String groupe,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return service.ajouterPieceJointe(porteur, id, fichier,
                new SaisiePieceJointe(description, typeId, groupe), principal);
    }

    @DeleteMapping(PIECES + "/{pieceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Supprime (logiquement) une pièce jointe d'un article, d'une tranche ou d'un programme")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pièce retirée des listes."),
            @ApiResponse(responseCode = "403", description = REPONSE_403),
            @ApiResponse(responseCode = "404", description = "Porteur inconnu ou masqué, pièce d'un autre porteur ou déjà supprimée.")
    })
    public void supprimer(
            @Parameter(description = PORTEUR) @PathVariable String porteur,
            @Parameter(description = ID_PORTEUR) @PathVariable UUID id,
            @Parameter(description = ID_PIECE) @PathVariable UUID pieceId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        service.supprimerPieceJointe(porteur, id, pieceId, principal);
    }

    @PostMapping(value = "/tranches/{id}/pieces-jointes/masse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Attache des fichiers aux articles de la tranche dont le code est le nom du fichier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bilan, une ligne par fichier, dans l'ordre d'envoi."),
            @ApiResponse(responseCode = "400", description = "typeId ou fichiers absents."),
            @ApiResponse(responseCode = "403", description = REPONSE_403),
            @ApiResponse(responseCode = "404", description = "Tranche inconnue ou masquée, ou typeId inconnu."),
            @ApiResponse(responseCode = "413", description = "Requête de plus de 50 Mo.")
    })
    public List<BilanAttachementDto> attacherEnMasse(
            @Parameter(description = "Id de la tranche (CfgTranche.PKey).") @PathVariable UUID id,
            @Parameter(description = "Fichiers, partie répétée ; nom sans extension = code de l'article.")
            @RequestParam("fichiers") List<MultipartFile> fichiers,
            @Parameter(description = TYPE) @RequestParam UUID typeId,
            @Parameter(description = GROUPE) @RequestParam(required = false) String groupe,
            @Parameter(description = "Légende commune. Absent = nom de chaque fichier (assaini).")
            @RequestParam(required = false) String description,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return masse.importerPiecesEnMasse(id, fichiers,
                new SaisiePieceJointe(description, typeId, groupe), principal);
    }

    @GetMapping("/articles/groupes-piece-jointe")
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Groupes de pièce jointe déjà utilisés (suggestions de saisie)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Valeurs distinctes non vides de FoderGroup, triées."),
            @ApiResponse(responseCode = "403", description = REPONSE_403)
    })
    public List<String> groupes() {
        return service.groupes();
    }
}
