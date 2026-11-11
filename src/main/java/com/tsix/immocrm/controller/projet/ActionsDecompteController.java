package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.dto.projet.CreerDecompteRequest;
import com.tsix.immocrm.dto.projet.DecompteDetailDto;
import com.tsix.immocrm.dto.projet.EnregistrerDecompteRequest;
import com.tsix.immocrm.dto.projet.LigneDecompteDto;
import com.tsix.immocrm.services.projet.ActionsDecompteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Projet")
@RestController
@RequestMapping("/api/projet")
@ApiResponse(responseCode = "403", description = "Droit OMRAN (AdmRight) manquant pour cette action.", content = @Content)
@ApiResponse(responseCode = "404", description = "Décompte ou lot introuvable : id inconnu ou projet non habilité.",
        content = @Content)
public class ActionsDecompteController {

    private static final String ID_DECOMPTE = "Identifiant du décompte (PrjWorkingOut.PKey).";
    private static final String VERROU = "Décompte facturé ou qui n'est pas le dernier de son lot.";

    private final ActionsDecompteService service;

    public ActionsDecompteController(ActionsDecompteService service) {
        this.service = service;
    }

    @PostMapping("/lots/{lotId}/decomptes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un décompte sur un lot (numéro suivant, taux et tiers du lot)")
    @ApiResponse(responseCode = "201", description = "Fiche du décompte créé, sans ligne.")
    @ApiResponse(responseCode = "400", description = "Date absente ou antérieure au décompte précédent / à l'ordre de service.",
            content = @Content)
    @ApiResponse(responseCode = "409", description = "Le dernier décompte du lot est le décompte final.", content = @Content)
    public DecompteDetailDto creer(@Parameter(description = "Identifiant du lot (PrjMarket.PKey).") @PathVariable UUID lotId,
                                   @Valid @RequestBody CreerDecompteRequest demande) {
        return service.creer(lotId, demande);
    }

    @PutMapping("/decomptes/{id}")
    @Operation(summary = "Enregistrer un décompte (saisie puis recalcul de toutes les colonnes)")
    @ApiResponse(responseCode = "200", description = "Fiche relue après écriture.")
    @ApiResponse(responseCode = "400", description = "Date antérieure au décompte précédent ou à l'ordre de service du lot.",
            content = @Content)
    @ApiResponse(responseCode = "409", description = VERROU, content = @Content)
    public DecompteDetailDto enregistrer(@Parameter(description = ID_DECOMPTE) @PathVariable UUID id,
                                         @RequestBody EnregistrerDecompteRequest demande) {
        return service.enregistrer(id, demande);
    }

    @PostMapping("/decomptes/{id}/dernier")
    @Operation(summary = "Définir en tant que dernier décompte (lignes rechargées au total du bordereau)")
    @ApiResponse(responseCode = "200", description = "Fiche relue après écriture.")
    @ApiResponse(responseCode = "409", description = VERROU, content = @Content)
    public DecompteDetailDto definirDernier(@Parameter(description = ID_DECOMPTE) @PathVariable UUID id) {
        return service.definirDernier(id);
    }

    @PostMapping("/decomptes/{id}/recalcul")
    @Operation(summary = "Recalculer un décompte avec les taux et plafonds du lot")
    @ApiResponse(responseCode = "200", description = "Fiche relue après écriture.")
    @ApiResponse(responseCode = "409", description = VERROU, content = @Content)
    public DecompteDetailDto recalculer(@Parameter(description = ID_DECOMPTE) @PathVariable UUID id) {
        return service.recalculer(id);
    }

    @DeleteMapping("/decomptes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer un décompte, ses lignes, travaux supplémentaires, retenues et approvisionnements")
    @ApiResponse(responseCode = "204", description = "Décompte supprimé.")
    @ApiResponse(responseCode = "409", description = VERROU, content = @Content)
    public void supprimer(@Parameter(description = ID_DECOMPTE) @PathVariable UUID id) {
        service.supprimer(id);
    }

    @GetMapping("/decomptes/{id}/lignes-rechargees")
    @Operation(summary = "Aperçu des lignes rechargées depuis le bordereau, sans écriture")
    @ApiResponse(responseCode = "200", description = "Une ligne par ligne du bordereau, quantité saisie à 0.")
    @ApiResponse(responseCode = "409", description = VERROU, content = @Content)
    public List<LigneDecompteDto> lignesRechargees(
            @Parameter(description = ID_DECOMPTE) @PathVariable UUID id,
            @Parameter(description = "Date affichée « jusqu'à la date » ; par défaut DocDate.", example = "2026-03-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.lignesRechargees(id);
    }
}
