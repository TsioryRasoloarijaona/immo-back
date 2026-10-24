package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.patrimoine.ProjetDetailDto;
import com.tsix.immocrm.dto.patrimoine.ProjetEcritureDto;
import com.tsix.immocrm.dto.patrimoine.ProjetListItemDto;
import com.tsix.immocrm.dto.patrimoine.TrancheListItemDto;
import com.tsix.immocrm.dto.patrimoine.ZoneProjetDto;
import com.tsix.immocrm.dto.patrimoine.ZoneProjetEcritureDto;
import com.tsix.immocrm.services.patrimoine.ProjetService;
import com.tsix.immocrm.services.patrimoine.fiche.FichePdf;
import com.tsix.immocrm.services.patrimoine.fiche.FichesPatrimoine;
import com.tsix.immocrm.web.export.ColonneFixe;
import com.tsix.immocrm.web.export.DemandeExport;
import com.tsix.immocrm.web.export.ExportListe;
import com.tsix.immocrm.web.export.LignesExport;
import io.swagger.v3.oas.annotations.Operation;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.domain.TypeExport.DATE;
import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;
import static com.tsix.immocrm.web.export.ColonneFixe.cachee;
import static com.tsix.immocrm.web.export.ColonneFixe.texte;

@Tag(name = "Programmes", description = "Programmes immobiliers (CfgProject), en lecture et en écriture (Administrateur).")
@RestController
@RequestMapping("/api/projets")
public class ProjetController {

    private static final String ADMIN = "hasRole('Administrateur')";
    private static final String PAS_ADMIN = "L'appelant n'a pas le rôle Administrateur.";
    private static final String INTROUVABLE = "Programme introuvable ou de son entité inactive.";
    private static final String ID_PROGRAMME = "Id du programme (CfgProject.PKey).";
    private static final String ID_ZONE = "Id de la zone (CfgProjectZone.PKey).";
    private static final String ZONE_INTROUVABLE = "Programme ou zone introuvable.";
    private static final String CODE_ZONE_PRIS = "Code déjà pris sous le même parent du programme.";

    private static final String RECHERCHE = "Recherche sans casse dans le code, le libellé, la ville et l'entité.";
    /** Colonnes de {@code ProjetsList.tsx}, code en premier. */
    private static final List<ColonneFixe> COLONNES_EXPORT = ColonneFixe.suite(List.of(
            texte("code", "Code"), texte("description", "Description"), texte("entiteCode", "Entité"),
            texte("ville", "Ville"), cachee("descriptionLongue", "Description détaillée"),
            cachee("dateDebutTravaux", "Date lancement des travaux", DATE), cachee("dateFinTravaux", "Date fin des travaux", DATE),
            cachee("dateDebutCommercialisation", "Date début de commercialisation", DATE),
            cachee("dateFinCommercialisation", "Date fin de commercialisation", DATE), cachee("adresse", "Adresse")),
            ColonneFixe.CONTACTS,
            List.of(cachee("villeSignature", "Courrier fait à"), cachee("signataireLibelle", "Courrier signataire"),
                    cachee("representantLegalLibelle", "Représentant légal"),
                    cachee("responsableCommercialLibelle", "Représentant commercial"),
                    cachee("responsableTechniqueLibelle", "Représentant technique")),
            ColonneFixe.AUDIT);

    private final ProjetService projetService;
    private final ExportListe export;
    private final FichePdf fichePdf;

    public ProjetController(ProjetService projetService, ExportListe export, FichePdf fichePdf) {
        this.projetService = projetService;
        this.export = export;
        this.fichePdf = fichePdf;
    }

    @GetMapping
    @Operation(summary = "Liste paginée des programmes des entités actives (colonnes de la liste OMRAN)")
    public PageDto<ProjetListItemDto> findAll(
            @Parameter(description = RECHERCHE)
            @RequestParam(required = false) String search,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "userCreateDateTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(projetService.findAll(search, pageable));
    }

    @GetMapping("/export")
    @Operation(summary = "Export xlsx ou pdf de la liste des programmes", description = ExportListe.DESCRIPTION)
    @ApiResponse(responseCode = "400", description = ExportListe.REFUS, content = @Content)
    public ResponseEntity<byte[]> exporter(
            @ParameterObject @ModelAttribute DemandeExport demande,
            @Parameter(description = RECHERCHE) @RequestParam(required = false) String search,
            @Parameter(hidden = true) @SortDefault(sort = "userCreateDateTime", direction = Sort.Direction.DESC) Sort sort) {
        return export.reponse(demande, "projets", "Programmes", ColonneFixe.choisir(demande.colonnes(), COLONNES_EXPORT),
                () -> LignesExport.toutes(sort, page -> projetService.findAll(search, page)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un programme")
    @ApiResponse(responseCode = "404", description = INTROUVABLE)
    public ProjetDetailDto findById(@Parameter(description = ID_PROGRAMME) @PathVariable UUID id) {
        return projetService.findById(id);
    }

    @GetMapping("/{id}/fiche.pdf")
    @Operation(summary = "Fiche PDF du programme (inline), mêmes droit et visibilité que le détail")
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    public ResponseEntity<byte[]> fichePdf(@Parameter(description = ID_PROGRAMME) @PathVariable UUID id) {
        return fichePdf.reponse(FichesPatrimoine.projet(projetService.findById(id)));
    }

    @GetMapping("/defauts-entite/{entiteId}")
    @Operation(summary = "Valeurs par défaut d'un programme tirées de l'entité (code et description compris)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Corps de création pré-rempli."),
            @ApiResponse(responseCode = "404", description = "Entité introuvable ou inactive.", content = @Content)
    })
    public ProjetEcritureDto defautsEntite(
            @Parameter(description = "Id de l'entité (CfgCompany.PKey).") @PathVariable UUID entiteId) {
        return projetService.defautsEntite(entiteId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ADMIN)
    @Operation(summary = "Supprime un programme sans tranche ni article")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Programme supprimé avec ses habilitations, zones, pièces et notes."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Une tranche, un article ou une autre table référence le programme.")
    })
    public void supprimer(@Parameter(description = ID_PROGRAMME) @PathVariable UUID id) {
        projetService.supprimerProjet(id);
    }

    @GetMapping("/{id}/zones")
    @Operation(summary = "Zones du programme : arbre rendu à plat (parentId), trié par Sorting")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Zones, tableau JSON."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public List<ZoneProjetDto> zones(@Parameter(description = ID_PROGRAMME) @PathVariable UUID id) {
        return projetService.zones(id);
    }

    @PostMapping("/{id}/zones")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMIN)
    @Operation(summary = "Ajoute une zone au programme")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Zone créée."),
            @ApiResponse(responseCode = "400", description = "Code ou description manquant."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = "Programme ou zone parente introuvable."),
            @ApiResponse(responseCode = "409", description = CODE_ZONE_PRIS)
    })
    public ZoneProjetDto creerZone(@Parameter(description = ID_PROGRAMME) @PathVariable UUID id,
                                   @RequestBody ZoneProjetEcritureDto saisie) {
        return projetService.creerZone(id, saisie);
    }

    @PatchMapping("/{id}/zones/{zoneId}")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Modifie une zone (absent ou null = inchangé)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Zone modifiée."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = ZONE_INTROUVABLE),
            @ApiResponse(responseCode = "409", description = CODE_ZONE_PRIS)
    })
    public ZoneProjetDto modifierZone(@Parameter(description = ID_PROGRAMME) @PathVariable UUID id,
                                      @Parameter(description = ID_ZONE) @PathVariable UUID zoneId,
                                      @RequestBody ZoneProjetEcritureDto saisie) {
        return projetService.modifierZone(id, zoneId, saisie);
    }

    @DeleteMapping("/{id}/zones/{zoneId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ADMIN)
    @Operation(summary = "Supprime une zone sans sous-zone ni référence")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Zone supprimée."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN),
            @ApiResponse(responseCode = "404", description = ZONE_INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "La zone a des sous-zones ou est référencée (SAV, GRH).")
    })
    public void supprimerZone(@Parameter(description = ID_PROGRAMME) @PathVariable UUID id,
                              @Parameter(description = ID_ZONE) @PathVariable UUID zoneId) {
        projetService.supprimerZone(id, zoneId);
    }

    @GetMapping("/{id}/tranches")
    @Operation(summary = "Tranches d'un programme")
    @ApiResponse(responseCode = "404", description = INTROUVABLE)
    public PageDto<TrancheListItemDto> findTranches(
            @Parameter(description = "Id du programme (CfgProject.PKey).") @PathVariable UUID id,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "userCreateDateTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(projetService.findTranches(id, pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Crée un programme")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Programme créé."),
            @ApiResponse(responseCode = "400", description = "Champ obligatoire manquant, entité ou pays inconnu, dates incohérentes."),
            @ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur."),
            @ApiResponse(responseCode = "409", description = "Code déjà pris par un autre programme.")
    })
    public ProjetDetailDto creer(@RequestBody ProjetEcritureDto saisie) {
        return projetService.creerProjet(saisie);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Modifie un programme (champ absent ou null = inchangé)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Programme modifié."),
            @ApiResponse(responseCode = "400", description = "Champ vidé obligatoire, entité ou pays inconnu, dates incohérentes."),
            @ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur."),
            @ApiResponse(responseCode = "404", description = "Programme introuvable."),
            @ApiResponse(responseCode = "409", description = "Code déjà pris par un autre programme.")
    })
    public ProjetDetailDto modifier(@Parameter(description = "Id du programme (CfgProject.PKey).") @PathVariable UUID id,
                                    @RequestBody ProjetEcritureDto saisie) {
        return projetService.modifierProjet(id, saisie);
    }
}
