package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.dto.achat.DocumentAchatDetailDto;
import com.tsix.immocrm.dto.achat.ModelePieceAchatDto;
import com.tsix.immocrm.dto.achat.SaisiePieceAchatRequest;
import com.tsix.immocrm.services.achat.ecriture.SaisiePieceAchatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@Tag(name = "Achat")
@RestController
@RequestMapping("/api/achat/documents")
public class CreationPieceAchatController {

    private final SaisiePieceAchatService saisie;

    public CreationPieceAchatController(SaisiePieceAchatService saisie) {
        this.saisie = saisie;
    }

    @GetMapping("/modele")
    @Operation(summary = "Formulaire pré-rempli d'une nouvelle pièce d'achat, vierge ou généré depuis des pièces sources")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Le modèle pré-rempli."),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sans le droit de créer cette nature.", content = @Content),
            @ApiResponse(responseCode = "404", content = @Content),
            @ApiResponse(responseCode = "409", content = @Content),
            @ApiResponse(responseCode = "500", content = @Content)})
    public ModelePieceAchatDto modele(@Parameter(description = "Nature : QUOTE, ORDER, DELIVRY ou INVOICE.",
            example = "ORDER") @RequestParam(required = false) String nature,
                                      @RequestParam(required = false) List<UUID> sources) {
        return saisie.modele(nature, sources);
    }

    @PostMapping
    @Operation(summary = "Crée une pièce d'achat (devis, bon de commande, bon de livraison, facture)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "La fiche de la pièce créée ; en-tête Location."),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sans le droit de créer cette nature.", content = @Content),
            @ApiResponse(responseCode = "404", content = @Content),
            @ApiResponse(responseCode = "409", content = @Content),
            @ApiResponse(responseCode = "500", content = @Content)})
    public ResponseEntity<DocumentAchatDetailDto> creer(@RequestBody SaisiePieceAchatRequest corps) {
        DocumentAchatDetailDto piece = saisie.creer(corps);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(piece.document().id()).toUri()).body(piece);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifie l'en-tête, les lignes et les ventilations d'une pièce d'achat")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "La fiche relue."),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sans le droit de modifier cette nature.", content = @Content),
            @ApiResponse(responseCode = "404", content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)})
    public DocumentAchatDetailDto modifier(@Parameter(description = "Id de la pièce d'achat (BuyDocument.PKey).")
                                           @PathVariable UUID id, @RequestBody SaisiePieceAchatRequest corps) {
        return saisie.modifier(id, corps);
    }
}
