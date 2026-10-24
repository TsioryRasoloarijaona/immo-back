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
import com.tsix.immocrm.web.export.ColonneFixe;
import com.tsix.immocrm.web.export.DemandeExport;
import com.tsix.immocrm.web.export.ExportListe;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
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

import static com.tsix.immocrm.domain.TypeExport.BOOLEEN;
import static com.tsix.immocrm.domain.TypeExport.DATE;
import static com.tsix.immocrm.domain.TypeExport.MONTANT;
import static com.tsix.immocrm.domain.TypeExport.NOMBRE;
import static com.tsix.immocrm.web.export.ColonneFixe.de;
import static com.tsix.immocrm.web.export.ColonneFixe.texte;

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

    private static final String SOCIETE = "Société (CfgCompany.PKey).";
    private static final String STATUT = "Statut (StkFeasibilityStudyStatus.PKey).";
    private static final String RECHERCHE = "Sous-chaîne du code ou du libellé, casse ignorée.";

    /** Colonnes de {@code EtudesFaisabiliteList.tsx} ; taux en fraction, comme la liste. */
    private static final List<ColonneFixe> COLONNES_EXPORT = List.of(texte("code", "Code"),
            texte("description", "Description"), de("dateDocument", "Date", DATE), texte("societeLibelle", "Société"),
            texte("terrainLibelle", "Terrain"), texte("statutLibelle", "Statut"), de("bloquee", "Bloquée", BOOLEEN),
            de("superficie", "Superficie", NOMBRE), de("coutTotalTerrain", "Coût total du terrain", MONTANT),
            de("coutTotal", "Charge TTC", MONTANT), de("chiffreAffaires", "Chiffre d'affaires TTC", MONTANT),
            de("montantAutofinancement", "Fonds propres TTC", MONTANT), de("margeNette", "Marge bénéficiaire nette", MONTANT),
            de("montantConcretisation", "Montant concrétisation TTC", MONTANT),
            de("montantFinalisation", "Montant finalisation TTC", MONTANT),
            de("pourcentageConcretisation", "Taux de concrétisation", NOMBRE),
            de("rentabiliteCoutProjet", "Retour sur coût projet", NOMBRE),
            de("rentabiliteChiffreAffaires", "Retour sur chiffre d'affaires", NOMBRE),
            de("rentabiliteFondsPropres", "Retour sur fonds propres", NOMBRE));

    private final EtudeFaisabiliteService service;
    private final ExportListe export;

    public EtudeFaisabiliteController(EtudeFaisabiliteService service, ExportListe export) {
        this.service = service;
        this.export = export;
    }

    @GetMapping
    @Operation(summary = "Liste des études des sociétés actives, par code")
    @ApiResponse(responseCode = "403", description = PAS_LECTEUR)
    public List<EtudeFaisabiliteListeDto> lister(
            @Parameter(description = SOCIETE) @RequestParam(required = false) UUID societeId,
            @Parameter(description = STATUT) @RequestParam(required = false) UUID statutId,
            @Parameter(description = RECHERCHE) @RequestParam(required = false)
            String search) {
        return service.lister(societeId, statutId, search);
    }

    @GetMapping("/export")
    @Operation(summary = "Export xlsx ou pdf de la liste des études, mêmes droits que la liste", description = ExportListe.DESCRIPTION)
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = ExportListe.REFUS, content = @Content),
            @ApiResponse(responseCode = "403", description = PAS_LECTEUR, content = @Content)
    })
    public ResponseEntity<StreamingResponseBody> exporter(
            @ParameterObject @ModelAttribute DemandeExport demande,
            @Parameter(description = SOCIETE) @RequestParam(required = false) UUID societeId,
            @Parameter(description = STATUT) @RequestParam(required = false) UUID statutId,
            @Parameter(description = RECHERCHE) @RequestParam(required = false) String search) {
        return export.reponse(demande, "etudes-faisabilite", "Études de faisabilité",
                ColonneFixe.choisir(demande.colonnes(), COLONNES_EXPORT),
                () -> service.lister(societeId, statutId, search));
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
