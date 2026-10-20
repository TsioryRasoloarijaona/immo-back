package com.tsix.immocrm.controller.desistement;

import com.tsix.immocrm.dto.desistement.CreerDesistementRequest;
import com.tsix.immocrm.dto.desistement.DesistementDto;
import com.tsix.immocrm.dto.desistement.DesistementsPageDto;
import com.tsix.immocrm.dto.desistement.FiltresDesistements;
import com.tsix.immocrm.dto.desistement.ModifierDesistementRequest;
import com.tsix.immocrm.dto.desistement.RembourserDesistementRequest;
import com.tsix.immocrm.dto.desistement.SimulationDesistementDto;
import com.tsix.immocrm.dto.desistement.ValiderDesistementRequest;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.desistement.DesistementService;
import com.tsix.immocrm.services.desistement.ValidationDesistement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Désistements")
@RestController
@RequestMapping("/api")
public class DesistementController {

    private static final String ECRITURE = "hasAnyRole('Administrateur', 'Direction Générale', 'Commercial Chef')";
    private static final String DOSSIER = "Identifiant du dossier (ComFolder.PKey).";
    private static final String DESISTEMENT = "Identifiant du désistement (ComSaleWithdrawalCancellation.PKey).";
    private static final String INTROUVABLE = "Désistement introuvable, ou dossier hors périmètre.";
    private static final String ROLES = "Réservé à Administrateur, Direction Générale, Commercial Chef.";

    private final DesistementService service;
    private final ValidationDesistement validation;

    public DesistementController(DesistementService service, ValidationDesistement validation) {
        this.service = service;
        this.validation = validation;
    }

    @GetMapping("/desistements")
    @Operation(summary = "Liste paginée des désistements, au périmètre, avec les totaux du filtre")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de désistements et totaux."),
            @ApiResponse(responseCode = "400", description = "Paramètre invalide.", content = @Content)
    })
    public DesistementsPageDto lister(@ParameterObject @ModelAttribute FiltresDesistements filtres,
                                      @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return service.lister(filtres, pageable);
    }

    @GetMapping("/desistements/{id}")
    @Operation(summary = "Fiche d'un désistement")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Le désistement."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public DesistementDto detail(@Parameter(description = DESISTEMENT) @PathVariable UUID id) {
        return service.detail(id);
    }

    @PatchMapping("/desistements/{id}")
    @PreAuthorize(ECRITURE)
    @Operation(summary = "Modifie un désistement (brouillon : tout ; validé : paiement, dates, motif, observations)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Le désistement modifié."),
            @ApiResponse(responseCode = "400", description = "Motif, type, banque ou contact inconnu ; remise hors [0, 1] ; pénalité négative.",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = ROLES, content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content),
            @ApiResponse(responseCode = "409", description = "Désistement légalisé, ou remise/pénalité après validation.",
                    content = @Content)
    })
    public DesistementDto modifier(@Parameter(description = DESISTEMENT) @PathVariable UUID id,
                                   @RequestBody ModifierDesistementRequest request) {
        return service.modifier(id, request);
    }

    @DeleteMapping("/desistements/{id}")
    @PreAuthorize(ECRITURE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprime un désistement non validé")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Supprimé."),
            @ApiResponse(responseCode = "403", description = ROLES, content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content),
            @ApiResponse(responseCode = "409", description = "Désistement déjà validé.", content = @Content)
    })
    public void supprimer(@Parameter(description = DESISTEMENT) @PathVariable UUID id) {
        service.supprimer(id);
    }

    @PostMapping("/desistements/{id}/validation")
    @PreAuthorize(ECRITURE)
    @Operation(summary = "Valide un désistement : dossier en DESISTEMENT, pièces de frais et de remboursement")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Le désistement validé."),
            @ApiResponse(responseCode = "400", description = "Date de validation absente, ou date de légalisation absente "
                    + "avec une pénalité non nulle.", content = @Content),
            @ApiResponse(responseCode = "403", description = ROLES, content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content),
            @ApiResponse(responseCode = "409", content = @Content)
    })
    public DesistementDto valider(@Parameter(description = DESISTEMENT) @PathVariable UUID id,
                                  @RequestBody ValiderDesistementRequest request) {
        return validation.valider(id, request);
    }

    @PostMapping("/desistements/{id}/remboursement")
    @PreAuthorize(ECRITURE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Enregistre un remboursement (décaissement) sur la pièce de résiliation du désistement")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Décaissement enregistré ; le désistement à jour."),
            @ApiResponse(responseCode = "400", description = "Montant, date ou type absent ou invalide.", content = @Content),
            @ApiResponse(responseCode = "403", description = ROLES, content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content),
            @ApiResponse(responseCode = "409", description = "Désistement non validé, rien à rembourser, ou montant au-delà "
                    + "du reste à rembourser.", content = @Content)
    })
    public DesistementDto rembourser(@Parameter(description = DESISTEMENT) @PathVariable UUID id,
                                     @RequestBody RembourserDesistementRequest request,
                                     @AuthenticationPrincipal AuthenticatedUser principal) {
        return validation.rembourser(id, request, principal);
    }

    @GetMapping("/dossiers/{id}/desistement")
    @Operation(summary = "Dernier désistement du dossier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Le désistement le plus récent du dossier."),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable, hors périmètre, ou sans désistement.",
                    content = @Content)
    })
    public DesistementDto duDossier(@Parameter(description = DOSSIER) @PathVariable UUID id) {
        return service.duDossier(id);
    }

    @GetMapping("/dossiers/{id}/desistement/simulation")
    @Operation(summary = "Simule les montants d'un désistement du dossier, sans rien écrire")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pénalité, reste frais, à rembourser."),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable ou hors périmètre.", content = @Content),
            @ApiResponse(responseCode = "409", description = "Statut du dossier sans « Peut effectuer un désistement ».",
                    content = @Content)
    })
    public SimulationDesistementDto simuler(@Parameter(description = DOSSIER) @PathVariable UUID id) {
        return service.simuler(id);
    }

    @PostMapping("/dossiers/{id}/desistement")
    @PreAuthorize(ECRITURE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crée un désistement (brouillon) sur le dossier")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Le désistement créé, montants calculés."),
            @ApiResponse(responseCode = "400", description = "Date de demande absente, ou motif inconnu.", content = @Content),
            @ApiResponse(responseCode = "403", description = ROLES, content = @Content),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable ou hors périmètre.", content = @Content),
            @ApiResponse(responseCode = "409", description = "Statut non éligible, ou désistement non légalisé existant.",
                    content = @Content)
    })
    public DesistementDto creer(@Parameter(description = DOSSIER) @PathVariable UUID id,
                                @RequestBody CreerDesistementRequest request) {
        return service.creer(id, request);
    }

    @GetMapping("/referentiels/motifs-desistement")
    @Operation(summary = "Motifs de désistement (ComSaleWithdrawalReason), par ordre d'affichage")
    @ApiResponse(responseCode = "200", description = "Liste, vide si aucun motif.")
    public List<ReferentielItemDto> motifs() {
        return service.motifs();
    }
}
