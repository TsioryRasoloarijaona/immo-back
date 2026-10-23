package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.faisabilite.ValeursFaisabilite;
import com.tsix.immocrm.dto.patrimoine.faisabilite.EtudeFaisabiliteDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.EtudeFaisabiliteListeDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.GrilleEtudeDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.LigneTrancheEtudeDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.LigneUniteEtudeDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.SaisieEtudeDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.services.patrimoine.EtudeFaisabiliteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Études de faisabilité")
@RestController
@RequestMapping("/api/etudes-faisabilite")
@PreAuthorize(EtudeFaisabiliteController.LECTURE)
public class EtudeFaisabiliteController {

    static final String LECTURE = "hasAnyRole('Administrateur', 'Direction Générale')";
    static final String ADMIN = "hasRole('Administrateur')";
    static final String ID_ETUDE = "Id de l'étude (StkFeasibilityStudy.PKey).";
    private static final String OK = "Étude enregistrée, fiche à jour.";
    static final String PAS_LECTEUR = "Rôle requis : Administrateur ou Direction Générale ; clé d'API : hors portée.";
    static final String PAS_ADMIN = "Rôle requis : Administrateur ; clé d'API : hors portée.";
    static final String INTROUVABLE = "Étude inconnue, ou de société inactive.";
    static final String BLOQUEE = "Étude bloquée (« Étude bloquée »).";
    private static final String VERIF = "Champ invalide, référence inconnue ou message Verif OMRAN (detail).";

    private final EtudeFaisabiliteService service;

    public EtudeFaisabiliteController(EtudeFaisabiliteService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Liste des études des sociétés actives, par code")
    @ApiResponse(responseCode = "403", description = PAS_LECTEUR)
    public List<EtudeFaisabiliteListeDto> lister(
            @Parameter(description = "Société (CfgCompany.PKey).") @RequestParam(required = false) UUID societeId,
            @Parameter(description = "Statut (StkFeasibilityStudyStatus.PKey).") @RequestParam(required = false) UUID statutId,
            @Parameter(description = "Sous-chaîne du code ou du libellé, casse ignorée.") @RequestParam(required = false)
            String search) {
        return service.lister(societeId, statutId, search);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fiche d'une étude avec ses grilles")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fiche."),
            @ApiResponse(responseCode = "403", description = PAS_LECTEUR),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public EtudeFaisabiliteDto fiche(@Parameter(description = ID_ETUDE) @PathVariable UUID id) {
        return service.fiche(id);
    }

    @PostMapping("/calcul")
    @Operation(summary = "Aperçu du calcul (bloc A, phases, Verif) sans rien écrire")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Valeurs calculées et messages Verif."),
            @ApiResponse(responseCode = "403", description = PAS_LECTEUR)
    })
    public ValeursFaisabilite calculer(@RequestBody SaisieEtudeDto saisie) {
        return service.calculerApercu(saisie);
    }

    @GetMapping("/choix/terrains")
    @Operation(summary = "Terrains choisissables : articles de nature TER visibles, par code")
    @ApiResponse(responseCode = "403", description = PAS_LECTEUR)
    public List<ReferentielItemDto> terrains() {
        return service.terrains();
    }

    @GetMapping("/choix/tranches")
    @Operation(summary = "Tranches du patrimoine d'une société active, par code")
    @ApiResponse(responseCode = "403", description = PAS_LECTEUR)
    public List<ReferentielItemDto> tranches(
            @Parameter(description = "Société de l'étude (CfgCompany.PKey) ; inactive ou absente = liste vide.")
            @RequestParam(required = false) UUID societeId) {
        return service.tranches(societeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMIN)
    @Operation(summary = "Crée une étude au statut « En étude »")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Étude créée."),
            @ApiResponse(responseCode = "400", description = VERIF),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "409", description = "Code déjà pris par une autre étude.")
    })
    public EtudeFaisabiliteDto creer(@RequestBody SaisieEtudeDto saisie) {
        return service.creerEtude(saisie);
    }

    @PatchMapping("/{id}")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Modifie une étude ; bloc A et phases recalculés, Verif sur les grilles enregistrées")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = OK),
            @ApiResponse(responseCode = "400", description = VERIF),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Code déjà pris, ou " + BLOQUEE)
    })
    public EtudeFaisabiliteDto modifier(@Parameter(description = ID_ETUDE) @PathVariable UUID id,
                                        @RequestBody SaisieEtudeDto saisie) {
        return service.modifierEtude(id, saisie);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ADMIN)
    @Operation(summary = "Supprime une étude sans prévision ni financement, avec ses grilles et son historique")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Étude supprimée."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Prévisions ou financements présents, ou " + BLOQUEE)
    })
    public void supprimer(@Parameter(description = ID_ETUDE) @PathVariable UUID id) {
        service.supprimerEtude(id);
    }

    @PutMapping("/{id}/unites")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Remplace les unités de base de l'étude ; une unité retirée perd ses ventilations")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Unités enregistrées, par ordre."),
            @ApiResponse(responseCode = "400", description = VERIF),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Unité retirée utilisée par une prévision, ou " + BLOQUEE)
    })
    public List<LigneUniteEtudeDto> remplacerUnites(@Parameter(description = ID_ETUDE) @PathVariable UUID id,
                                                    @RequestBody GrilleEtudeDto<LigneUniteEtudeDto> grille) {
        return service.modifierUnites(id, grille.lignes());
    }

    @PutMapping("/{id}/tranches")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Remplace les tranches de l'étude et leurs ventilations")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tranches enregistrées, par ordre."),
            @ApiResponse(responseCode = "400", description = VERIF),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Code en double, ou " + BLOQUEE)
    })
    public List<LigneTrancheEtudeDto> remplacerTranches(@Parameter(description = ID_ETUDE) @PathVariable UUID id,
                                                        @RequestBody GrilleEtudeDto<LigneTrancheEtudeDto> grille) {
        return service.modifierTranches(id, grille.lignes());
    }

    @PostMapping("/{id}/bloquer")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Bloque l'étude après Verif (Bloquer = enregistrer)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = OK),
            @ApiResponse(responseCode = "400", description = "Message Verif OMRAN (detail) : l'étude reste débloquée."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Étude déjà bloquée.")
    })
    public EtudeFaisabiliteDto bloquer(@Parameter(description = ID_ETUDE) @PathVariable UUID id) {
        return service.modifierBlocage(id, true);
    }

    @PostMapping("/{id}/debloquer")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Débloque l'étude")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = OK),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public EtudeFaisabiliteDto debloquer(@Parameter(description = ID_ETUDE) @PathVariable UUID id) {
        return service.modifierBlocage(id, false);
    }
}
