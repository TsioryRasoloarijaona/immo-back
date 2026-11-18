package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.dto.projet.reunions.AvancementSaisieDto;
import com.tsix.immocrm.dto.projet.reunions.ContactSaisieDto;
import com.tsix.immocrm.dto.projet.reunions.DecisionSaisieDto;
import com.tsix.immocrm.dto.projet.reunions.IntervenantSaisieDto;
import com.tsix.immocrm.dto.projet.reunions.MaitresOuvrageAjoutDto;
import com.tsix.immocrm.dto.projet.reunions.ReunionFicheDto;
import com.tsix.immocrm.services.projet.reunions.OngletsReunionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.tsix.immocrm.controller.projet.ReunionsChantierController.ID_REUNION;
import static com.tsix.immocrm.controller.projet.ReunionsChantierController.SANS_DROIT_MODIFIER;

/** Onglets de la fiche réunion ; chaque écriture renvoie la fiche entière relue. */
@Tag(name = "Projet")
@RestController
@RequestMapping("/api/projet/reunions/{id}")
public class OngletsReunionController {

    private static final String INCONNUE = "Réunion ou ligne inconnue, ou réunion de projet non habilité.";
    private static final String ID_LIGNE = "PKey de la ligne de l'onglet.";
    private static final String ID_INTERVENANT = "BulDelegate.PKey.";
    private static final String MAITRES = "/maitres-ouvrage";
    private static final String INTERVENANTS = "/intervenants";
    private static final String DECISIONS = "/decisions";
    private static final String LIGNE = "/{ligneId}";
    private static final String CONTACTS = "/intervenants/{intervenantId}/contacts";

    private final OngletsReunionService service;

    public OngletsReunionController(OngletsReunionService service) {
        this.service = service;
    }

    @PostMapping(MAITRES)
    @Operation(summary = "Ajoute des maîtres d'ouvrage (contacts déjà présents ignorés)")
    @ApiResponse(responseCode = "400", description = "Liste vide ou contact inconnu.", content = @Content)
    @ApiResponse(responseCode = "403", description = SANS_DROIT_MODIFIER, content = @Content)
    @ApiResponse(responseCode = "404", description = INCONNUE, content = @Content)
    public ReunionFicheDto ajouterMaitresOuvrage(@Parameter(description = ID_REUNION) @PathVariable UUID id,
                                                 @RequestBody MaitresOuvrageAjoutDto corps) {
        return service.ajouterMaitresOuvrage(id, corps);
    }

    @PatchMapping(MAITRES + LIGNE)
    @Operation(summary = "Coche « Présent » et « À notifier » d'un maître d'ouvrage")
    @ApiResponse(responseCode = "403", description = SANS_DROIT_MODIFIER, content = @Content)
    @ApiResponse(responseCode = "404", description = INCONNUE, content = @Content)
    public ReunionFicheDto modifierMaitreOuvrage(@Parameter(description = ID_REUNION) @PathVariable UUID id,
                                                 @Parameter(description = ID_LIGNE) @PathVariable UUID ligneId,
                                                 @RequestBody ContactSaisieDto corps) {
        return service.modifierMaitreOuvrage(id, ligneId, corps);
    }

    @DeleteMapping(MAITRES + LIGNE)
    @Operation(summary = "Retire un maître d'ouvrage")
    @ApiResponse(responseCode = "403", description = SANS_DROIT_MODIFIER, content = @Content)
    @ApiResponse(responseCode = "404", description = INCONNUE, content = @Content)
    public ReunionFicheDto supprimerMaitreOuvrage(@Parameter(description = ID_REUNION) @PathVariable UUID id,
                                                  @Parameter(description = ID_LIGNE) @PathVariable UUID ligneId) {
        return service.supprimerMaitreOuvrage(id, ligneId);
    }

    @PostMapping(INTERVENANTS)
    @Operation(summary = "Ajoute un intervenant")
    @ApiResponse(responseCode = "400", description = "Lot hors projet ou fournisseur inconnu.", content = @Content)
    @ApiResponse(responseCode = "403", description = SANS_DROIT_MODIFIER, content = @Content)
    @ApiResponse(responseCode = "404", description = INCONNUE, content = @Content)
    @ApiResponse(responseCode = "409", description = "Intervenant déjà présent pour ce lot et ce fournisseur.",
            content = @Content)
    public ReunionFicheDto creerIntervenant(@Parameter(description = ID_REUNION) @PathVariable UUID id,
                                            @RequestBody IntervenantSaisieDto corps) {
        return service.creerIntervenant(id, corps);
    }

    @PutMapping(INTERVENANTS + LIGNE)
    @Operation(summary = "Modifie un intervenant")
    @ApiResponse(responseCode = "400", description = "Lot hors projet ou fournisseur inconnu.", content = @Content)
    @ApiResponse(responseCode = "403", description = SANS_DROIT_MODIFIER, content = @Content)
    @ApiResponse(responseCode = "404", description = INCONNUE, content = @Content)
    @ApiResponse(responseCode = "409", description = "Intervenant déjà présent pour ce lot et ce fournisseur.",
            content = @Content)
    public ReunionFicheDto modifierIntervenant(@Parameter(description = ID_REUNION) @PathVariable UUID id,
                                               @Parameter(description = ID_INTERVENANT) @PathVariable UUID ligneId,
                                               @RequestBody IntervenantSaisieDto corps) {
        return service.modifierIntervenant(id, ligneId, corps);
    }

    @DeleteMapping(INTERVENANTS + LIGNE)
    @Operation(summary = "Supprime un intervenant et ses contacts")
    @ApiResponse(responseCode = "403", description = SANS_DROIT_MODIFIER, content = @Content)
    @ApiResponse(responseCode = "404", description = INCONNUE, content = @Content)
    public ReunionFicheDto supprimerIntervenant(@Parameter(description = ID_REUNION) @PathVariable UUID id,
                                                @Parameter(description = ID_INTERVENANT) @PathVariable UUID ligneId) {
        return service.supprimerIntervenant(id, ligneId);
    }

    @PostMapping(CONTACTS)
    @Operation(summary = "Ajoute un contact à un intervenant")
    @ApiResponse(responseCode = "400", description = "Contact absent ou inconnu.", content = @Content)
    @ApiResponse(responseCode = "403", description = SANS_DROIT_MODIFIER, content = @Content)
    @ApiResponse(responseCode = "404", description = INCONNUE, content = @Content)
    public ReunionFicheDto creerContactIntervenant(@Parameter(description = ID_REUNION) @PathVariable UUID id,
                                                   @Parameter(description = ID_INTERVENANT) @PathVariable UUID intervenantId,
                                                   @RequestBody ContactSaisieDto corps) {
        return service.creerContactIntervenant(id, intervenantId, corps);
    }

    @PutMapping(CONTACTS + LIGNE)
    @Operation(summary = "Modifie « Présent » et « À notifier » d'un contact d'intervenant")
    @ApiResponse(responseCode = "403", description = SANS_DROIT_MODIFIER, content = @Content)
    @ApiResponse(responseCode = "404", description = INCONNUE, content = @Content)
    public ReunionFicheDto modifierContactIntervenant(@Parameter(description = ID_REUNION) @PathVariable UUID id,
                                                      @Parameter(description = ID_INTERVENANT) @PathVariable UUID intervenantId,
                                                      @Parameter(description = ID_LIGNE) @PathVariable UUID ligneId,
                                                      @RequestBody ContactSaisieDto corps) {
        return service.modifierContactIntervenant(id, intervenantId, ligneId, corps);
    }

    @DeleteMapping(CONTACTS + LIGNE)
    @Operation(summary = "Retire un contact d'un intervenant")
    @ApiResponse(responseCode = "403", description = SANS_DROIT_MODIFIER, content = @Content)
    @ApiResponse(responseCode = "404", description = INCONNUE, content = @Content)
    public ReunionFicheDto supprimerContactIntervenant(@Parameter(description = ID_REUNION) @PathVariable UUID id,
                                                       @Parameter(description = ID_INTERVENANT) @PathVariable UUID intervenantId,
                                                       @Parameter(description = ID_LIGNE) @PathVariable UUID ligneId) {
        return service.supprimerContactIntervenant(id, intervenantId, ligneId);
    }

    @PostMapping(DECISIONS)
    @Operation(summary = "Ajoute une décision")
    @ApiResponse(responseCode = "400", description = "Description ou observation manquante, lot hors projet.",
            content = @Content)
    @ApiResponse(responseCode = "403", description = SANS_DROIT_MODIFIER, content = @Content)
    @ApiResponse(responseCode = "404", description = INCONNUE, content = @Content)
    public ReunionFicheDto creerDecision(@Parameter(description = ID_REUNION) @PathVariable UUID id,
                                         @RequestBody DecisionSaisieDto corps) {
        return service.creerDecision(id, corps);
    }

    @PutMapping(DECISIONS + LIGNE)
    @Operation(summary = "Modifie une décision")
    @ApiResponse(responseCode = "400", description = "Description ou observation manquante, lot hors projet.",
            content = @Content)
    @ApiResponse(responseCode = "403", description = SANS_DROIT_MODIFIER, content = @Content)
    @ApiResponse(responseCode = "404", description = INCONNUE, content = @Content)
    public ReunionFicheDto modifierDecision(@Parameter(description = ID_REUNION) @PathVariable UUID id,
                                            @Parameter(description = "BulMeetingLine.PKey.") @PathVariable UUID ligneId,
                                            @RequestBody DecisionSaisieDto corps) {
        return service.modifierDecision(id, ligneId, corps);
    }

    @DeleteMapping(DECISIONS + LIGNE)
    @Operation(summary = "Supprime une décision")
    @ApiResponse(responseCode = "403", description = SANS_DROIT_MODIFIER, content = @Content)
    @ApiResponse(responseCode = "404", description = INCONNUE, content = @Content)
    @ApiResponse(responseCode = "409", description = "Décision reprise par une réunion suivante.", content = @Content)
    public ReunionFicheDto supprimerDecision(@Parameter(description = ID_REUNION) @PathVariable UUID id,
                                             @Parameter(description = "BulMeetingLine.PKey.") @PathVariable UUID ligneId) {
        return service.supprimerDecision(id, ligneId);
    }

    @PutMapping("/avancements")
    @Operation(summary = "Renseigne une cellule de l'onglet Avancements")
    @ApiResponse(responseCode = "400", description = "Critère ou niveau absent, hors projet, ou pourcentage hors 0-100.",
            content = @Content)
    @ApiResponse(responseCode = "403", description = SANS_DROIT_MODIFIER, content = @Content)
    @ApiResponse(responseCode = "404", description = INCONNUE, content = @Content)
    public ReunionFicheDto modifierAvancement(@Parameter(description = ID_REUNION) @PathVariable UUID id,
                                              @RequestBody AvancementSaisieDto corps) {
        return service.modifierAvancement(id, corps);
    }
}
