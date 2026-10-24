package com.tsix.immocrm.controller.note;

import com.tsix.immocrm.dto.note.CreerNoteRequest;
import com.tsix.immocrm.dto.note.NoteDto;
import com.tsix.immocrm.services.note.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Notes", description = "Notes (AdmMessage) du patrimoine et des prospections terrain ; écriture Administrateur.")
@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private static final String ADMINISTRATEUR = "hasRole('Administrateur')";
    private static final String NON_ADMIN = "L'appelant n'a pas le rôle Administrateur.";

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    @Operation(summary = "Notes d'un programme, d'une tranche ou d'un article, plus récentes d'abord")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste, éventuellement vide."),
            @ApiResponse(responseCode = "400", description = "`porteur` absent ou hors projet, tranche, article, prospection-terrain, etude."),
            @ApiResponse(responseCode = "404", description = "Aucun objet visible de ce type avec cet id.")
    })
    public List<NoteDto> lister(
            @Parameter(description = "projet, tranche, article, prospection-terrain ou etude (étude de faisabilité).", example = "article") @RequestParam String porteur,
            @Parameter(description = "PKey du porteur.") @RequestParam UUID id) {
        return noteService.lister(porteur, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Ajoute une note en texte simple (échappée en HTML pour OMRAN)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Note créée."),
            @ApiResponse(responseCode = "400", description = "Texte vide (« Veuillez saisir une note ! ») ou porteur invalide."),
            @ApiResponse(responseCode = "403", description = NON_ADMIN),
            @ApiResponse(responseCode = "404", description = "Aucun objet visible de ce type avec cet id.")
    })
    public NoteDto creer(@RequestBody CreerNoteRequest saisie) {
        return noteService.creer(saisie);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Supprime une note d'un objet du patrimoine")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Note supprimée."),
            @ApiResponse(responseCode = "403", description = NON_ADMIN),
            @ApiResponse(responseCode = "404", description = "Note introuvable, ou d'un objet hors patrimoine.")
    })
    public void supprimer(@Parameter(description = "AdmMessage.PKey.") @PathVariable UUID id) {
        noteService.supprimer(id);
    }
}
