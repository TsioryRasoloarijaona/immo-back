package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.foncier.SurfacesTerrain;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.ApercuCalculDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.ExecutionTransitionDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.FiltresProspectionsTerrain;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.HistoriqueStatutProspectionDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.ProspectionTerrainDetailDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.ProspectionTerrainEcritureDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.ProspectionTerrainListeDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.TerrainCreeDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.TransformationTerrainDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.TransitionProspectionTerrainDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.ZoneTerrainDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.ZonesTerrainEcritureDto;
import com.tsix.immocrm.services.patrimoine.prospectionterrain.ProspectionTerrainService;
import com.tsix.immocrm.services.patrimoine.terrain.TransformationProspectionTerrain;
import com.tsix.immocrm.services.patrimoine.fiche.FichePdf;
import com.tsix.immocrm.services.patrimoine.fiche.FichesPatrimoine;
import com.tsix.immocrm.web.export.ColonneFixe;
import com.tsix.immocrm.web.export.DemandeExport;
import com.tsix.immocrm.web.export.ExportListe;
import com.tsix.immocrm.web.export.LignesExport;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.patrimoine.ArticleController.ADMINISTRATEUR;
import static com.tsix.immocrm.controller.patrimoine.ArticleController.REPONSE_403;
import static com.tsix.immocrm.domain.TypeExport.BOOLEEN;
import static com.tsix.immocrm.domain.TypeExport.DATE;
import static com.tsix.immocrm.domain.TypeExport.MONTANT;
import static com.tsix.immocrm.domain.TypeExport.NOMBRE;
import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;
import static com.tsix.immocrm.web.export.ColonneFixe.cachee;
import static com.tsix.immocrm.web.export.ColonneFixe.de;
import static com.tsix.immocrm.web.export.ColonneFixe.texte;

@Tag(name = "Prospections terrain")
@RestController
@RequestMapping("/api/prospections-terrain")
public class ProspectionTerrainController {

    private static final String ID = "Id de la prospection terrain (LndLand.PKey).";
    private static final String INTROUVABLE = "Prospection terrain inconnue.";
    private static final String SAISIE_INVALIDE = "Saisie refusée : detail nomme le champ (voir la description).";
    private static final String LECTURE_SEULE = "Statut courant en lecture seule.";

    /** Colonnes de {@code lib/colonnesProspectionsTerrain.tsx}, même ordre. */
    private static final List<ColonneFixe> COLONNES_EXPORT = ColonneFixe.suite(List.of(
            texte("code", "Code"), de("date", "Date", DATE), texte("description", "Description"),
            texte("categorie", "Catégorie"), texte("type", "Type"), texte("niveau", "Niveau"), texte("vocation", "Vocation"),
            texte("ville", "Ville"), texte("statut", "Statut"), de("dateValiditeStatut", "Validité du statut", DATE),
            de("superficie", "Superficie", NOMBRE), de("superficieNette", "Superficie nette", NOMBRE),
            de("prixPropose", "Prix proposé", MONTANT), de("estArchivee", "Archivée", BOOLEEN),
            de("estTransformee", "Transformée", BOOLEEN), de("nbEtudesFaisabilite", "Nbr. étude de fais.", NOMBRE),
            cachee("pays", "Pays"), cachee("courtier", "Courtier"), cachee("adresse", "Adresse"),
            cachee("proprietaires", "Propriétaires"), cachee("estAmenage", "Aménagé", BOOLEEN), cachee("notes", "Notes"),
            cachee("anomalies", "Anomalies"), cachee("superficieEquipements", "Superficie des voies d'aménagement", NOMBRE),
            cachee("coefEquipements", "Coefficient d'équipements", NOMBRE), cachee("cos", "COS", NOMBRE),
            cachee("superficieCos", "Emprise au sol", NOMBRE), cachee("cuf", "CUF", NOMBRE),
            cachee("superficieCuf", "Plancher couvert", NOMBRE), cachee("coefVendable", "Coefficient vendable", NOMBRE),
            cachee("superficieVendable", "Plancher vendable", NOMBRE),
            cachee("arreteAlignement", "Arrêté d'alignement", NOMBRE), cachee("hauteurMax", "Hauteur maximale", NOMBRE),
            cachee("prixM2", "Prix du m² du propriétaire", MONTANT), cachee("prix", "Prix total du propriétaire", MONTANT),
            cachee("dateEnregistrement", "Date d'enregistrement", DATE),
            cachee("numeroEnregistrement", "Numéro d'enregistrement"), cachee("lieuEnregistrement", "Lieu d'enregistrement"),
            cachee("cpfImmatriculation", "C.P.F. immatriculation"),
            cachee("dateImmatriculation", "Date d'immatriculation", DATE)), ColonneFixe.AUDIT);

    private final ProspectionTerrainService service;
    private final TransformationProspectionTerrain transformation;
    private final ExportListe export;
    private final FichePdf fichePdf;

    public ProspectionTerrainController(ProspectionTerrainService service,
                                        TransformationProspectionTerrain transformation, ExportListe export,
                                        FichePdf fichePdf) {
        this.service = service;
        this.transformation = transformation;
        this.export = export;
        this.fichePdf = fichePdf;
    }

    @GetMapping
    @Operation(summary = "Liste des prospections terrain, par code croissant par défaut")
    @ApiResponse(responseCode = "200", description = "Page de prospections terrain.")
    public PageDto<ProspectionTerrainListeDto> lister(
            @ParameterObject @ModelAttribute FiltresProspectionsTerrain filtres,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "code", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageDto.of(service.lister(filtres, pageable));
    }

    @GetMapping("/export")
    @Operation(summary = "Export xlsx ou pdf de la liste des prospections terrain", description = ExportListe.DESCRIPTION)
    @ApiResponse(responseCode = "400", description = ExportListe.REFUS, content = @Content)
    public ResponseEntity<byte[]> exporter(
            @ParameterObject @ModelAttribute FiltresProspectionsTerrain filtres,
            @ParameterObject @ModelAttribute DemandeExport demande,
            @Parameter(hidden = true) @SortDefault(sort = "code", direction = Sort.Direction.ASC) Sort sort) {
        return export.reponse(demande, "prospections-terrain", "Prospections terrain",
                ColonneFixe.choisir(demande.colonnes(), COLONNES_EXPORT),
                () -> LignesExport.toutes(sort, page -> service.lister(filtres, page)));
    }

    @GetMapping("/{id}/fiche.pdf")
    @Operation(summary = "Fiche PDF de la prospection terrain (inline), mêmes droit et visibilité que le détail")
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    public ResponseEntity<byte[]> fichePdf(@Parameter(description = ID) @PathVariable UUID id) {
        return fichePdf.reponse(FichesPatrimoine.prospectionTerrain(service.lire(id)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fiche d'une prospection terrain, zones et terrain issu compris")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fiche de la prospection."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public ProspectionTerrainDetailDto lire(@Parameter(description = ID) @PathVariable UUID id) {
        return service.lire(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Crée une prospection terrain au premier statut « Nouveau » de l'entité LndLand")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Prospection créée."),
            @ApiResponse(responseCode = "400", description = SAISIE_INVALIDE),
            @ApiResponse(responseCode = "403", description = REPONSE_403),
            @ApiResponse(responseCode = "409", description = "Code déjà pris par une prospection terrain."),
            @ApiResponse(responseCode = "500", description = "Entité LndLand ou statut « Nouveau » absent : rien n'est écrit.")
    })
    public ProspectionTerrainDetailDto creer(@RequestBody ProspectionTerrainEcritureDto saisie) {
        return service.creer(saisie);
    }

    @PatchMapping("/{id}")
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Modifie une prospection terrain (champ absent ou null = inchangé)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prospection modifiée, surfaces recalculées."),
            @ApiResponse(responseCode = "400", description = SAISIE_INVALIDE),
            @ApiResponse(responseCode = "403", description = REPONSE_403),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Code déjà pris, ou statut courant en lecture seule.")
    })
    public ProspectionTerrainDetailDto modifier(@Parameter(description = ID) @PathVariable UUID id,
                                                @RequestBody ProspectionTerrainEcritureDto saisie) {
        return service.modifier(id, saisie);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Supprime une prospection terrain avec ses zones, son historique, ses pièces jointes et notes")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Prospection supprimée."),
            @ApiResponse(responseCode = "403", description = REPONSE_403),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Statut sans suppression, terrain ou étude liés, autre référence.")
    })
    public void supprimer(@Parameter(description = ID) @PathVariable UUID id) {
        service.supprimer(id);
    }

    @PutMapping("/{id}/zones")
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Remplace la grille des zones d'un terrain brut")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Zones enregistrées, recalculées."),
            @ApiResponse(responseCode = "400", description = SAISIE_INVALIDE),
            @ApiResponse(responseCode = "403", description = REPONSE_403),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = LECTURE_SEULE)
    })
    public List<ZoneTerrainDto> modifierZones(@Parameter(description = ID) @PathVariable UUID id,
                                              @RequestBody ZonesTerrainEcritureDto saisie) {
        return service.modifierZones(id, saisie);
    }

    @PostMapping("/apercu-calcul")
    @Operation(summary = "Recalcule les surfaces et le prix sans rien écrire")
    @ApiResponse(responseCode = "200", description = "Toutes les grandeurs recalculées.")
    public SurfacesTerrain apercu(@RequestBody ApercuCalculDto saisie) {
        return service.apercu(saisie);
    }

    @GetMapping("/{id}/transitions")
    @Operation(summary = "Transitions sortantes du statut courant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transitions, par ordre (peut être vide)."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public List<TransitionProspectionTerrainDto> transitions(@Parameter(description = ID) @PathVariable UUID id) {
        return service.transitions(id);
    }

    @PostMapping("/{id}/transition")
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Exécute une transition : statut, historique, archivage, validité")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fiche au nouveau statut."),
            @ApiResponse(responseCode = "400", description = "Commentaire obligatoire absent."),
            @ApiResponse(responseCode = "403", description = REPONSE_403),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Transition non permise (transitionsAutorisees) ou second utilisateur.")
    })
    public ProspectionTerrainDetailDto executerTransition(@Parameter(description = ID) @PathVariable UUID id,
                                                          @RequestBody ExecutionTransitionDto saisie) {
        return service.executerTransition(id, saisie);
    }

    @GetMapping("/{id}/historique")
    @Operation(summary = "Historique des statuts (StrHistory)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lignes, la plus ancienne d'abord."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public List<HistoriqueStatutProspectionDto> historique(@Parameter(description = ID) @PathVariable UUID id) {
        return service.historique(id);
    }

    @PostMapping("/{id}/transformer-en-terrain")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Crée le terrain de la prospection, puis l'archive et la marque transformée")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Terrain créé."),
            @ApiResponse(responseCode = "400", description = SAISIE_INVALIDE),
            @ApiResponse(responseCode = "403", description = REPONSE_403),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Déjà transformée (terrainId), statut non transformable, code pris."),
            @ApiResponse(responseCode = "500", description = "Configuration LandProject absente : rien n'est écrit.")
    })
    public TerrainCreeDto transformerEnTerrain(@Parameter(description = ID) @PathVariable UUID id,
                                               @RequestBody TransformationTerrainDto saisie) {
        return transformation.transformer(id, saisie);
    }
}
