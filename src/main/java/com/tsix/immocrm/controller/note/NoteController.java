package com.tsix.immocrm.controller.note;

import com.tsix.immocrm.dto.note.CreerNoteRequest;
import com.tsix.immocrm.dto.note.NoteDto;
import com.tsix.immocrm.services.note.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Notes")
@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private static final String REFUS_ROLE = "L'appelant n'a pas le rôle Administrateur.";

    private final NoteService service;

    public NoteController(NoteService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Notes d'une fiche, plus récentes d'abord")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notes de la fiche (éventuellement aucune)."),
            @ApiResponse(responseCode = "400", description = "porteur hors de projet, tranche, article ; id mal formé."),
            @ApiResponse(responseCode = "404", description = "Aucune fiche de ce type avec cet id.")
    })
    public List<NoteDto> lister(
            @Parameter(description = "Type de fiche.", required = true,
                    schema = @Schema(allowableValues = {"projet", "tranche", "article"}))
            @RequestParam String porteur,
            @Parameter(description = "PKey de la fiche.", required = true) @RequestParam UUID id) {
        return service.lister(porteur, id);
    }

    @PostMapping
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Ajoute une note à une fiche")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Note créée ; renvoie la note."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "404", description = "Aucune fiche de ce type avec cet id.")
    })
    public ResponseEntity<NoteDto> creer(@RequestBody CreerNoteRequest demande) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creer(demande));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Supprime une note")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Note supprimée."),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "404", description = "Aucune note avec cet id.")
    })
    public ResponseEntity<Void> supprimer(@Parameter(description = "AdmMessage.PKey.") @PathVariable UUID id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
