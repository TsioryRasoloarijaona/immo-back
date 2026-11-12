package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.dto.achat.config.StatutAchatConfigDto;
import com.tsix.immocrm.dto.achat.config.StatutAchatEcritureDto;
import com.tsix.immocrm.dto.achat.config.TransitionAchatDto;
import com.tsix.immocrm.dto.achat.config.TransitionAchatEcritureDto;
import com.tsix.immocrm.services.achat.config.EcritureConfigurationAchatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Achat — configuration")
@RestController
@RequestMapping("/api/achat/config")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Champ absent, trop long, inconnu ou en double ; le message le nomme.",
                content = @Content),
        @ApiResponse(responseCode = "403", description = "Sans le droit OMRAN Yes.Buy.Front.Config.BuyDocumentStatus "
                + "(Administrateur exempté) ; vérifié avant le corps.", content = @Content)})
public class EcritureConfigurationAchatController {

    private static final String ID_STATUT = "BuyDocumentStatus.PKey.";
    private static final String ID_TRANSITION = "BuyDocumentWorkFlow.PKey.";

    private final EcritureConfigurationAchatService service;

    public EcritureConfigurationAchatController(EcritureConfigurationAchatService service) {
        this.service = service;
    }

    @PostMapping("/statuts")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crée un statut de pièce d'achat (fiche OMRAN « Statuts Documents Achats »)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Le statut créé, tel que la liste le rend."),
            @ApiResponse(responseCode = "409", description = "Code déjà pris (sans casse ni espaces de bord).",
                    content = @Content)})
    public StatutAchatConfigDto creerStatut(@RequestBody StatutAchatEcritureDto corps) {
        return service.creerStatut(corps);
    }

    @PutMapping("/statuts/{id}")
    @Operation(summary = "Remplace la fiche entière d'un statut d'achat, listes et documents compris")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Le statut à jour, tel que la liste le rend."),
            @ApiResponse(responseCode = "404", description = "Statut inconnu.", content = @Content),
            @ApiResponse(responseCode = "409", description = "Code déjà pris par un autre statut.", content = @Content)})
    public StatutAchatConfigDto modifierStatut(@Parameter(description = ID_STATUT) @PathVariable UUID id,
                                               @RequestBody StatutAchatEcritureDto corps) {
        return service.modifierStatut(id, corps);
    }

    @DeleteMapping("/statuts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprime un statut d'achat inutilisé, avec ses documents à joindre")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Statut supprimé."),
            @ApiResponse(responseCode = "404", description = "Statut inconnu.", content = @Content),
            @ApiResponse(responseCode = "409", description = "Statut cité par des pièces, transitions ou historiques ; "
                    + "le message donne les trois nombres.", content = @Content)})
    public void supprimerStatut(@Parameter(description = ID_STATUT) @PathVariable UUID id) {
        service.supprimerStatut(id);
    }

    @PostMapping("/transitions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crée une transition de statut d'achat dans une gestion, avec ses rôles")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "La transition créée, telle que la liste la rend."),
            @ApiResponse(responseCode = "409", description = "Même départ et même arrivée déjà dans cette gestion.",
                    content = @Content)})
    public TransitionAchatDto creerTransition(@RequestBody TransitionAchatEcritureDto corps) {
        return service.creerTransition(corps);
    }

    @PutMapping("/transitions/{id}")
    @Operation(summary = "Remplace une transition de statut d'achat, rôles compris")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "La transition à jour, telle que la liste la rend."),
            @ApiResponse(responseCode = "404", description = "Transition inconnue.", content = @Content),
            @ApiResponse(responseCode = "409", description = "Même départ et même arrivée déjà dans cette gestion.",
                    content = @Content)})
    public TransitionAchatDto modifierTransition(@Parameter(description = ID_TRANSITION) @PathVariable UUID id,
                                                 @RequestBody TransitionAchatEcritureDto corps) {
        return service.modifierTransition(id, corps);
    }

    @DeleteMapping("/transitions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprime une transition et ses liens de rôles ; l'historique des pièces est gardé")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Transition supprimée."),
            @ApiResponse(responseCode = "404", description = "Transition inconnue.", content = @Content)})
    public void supprimerTransition(@Parameter(description = ID_TRANSITION) @PathVariable UUID id) {
        service.supprimerTransition(id);
    }
}
