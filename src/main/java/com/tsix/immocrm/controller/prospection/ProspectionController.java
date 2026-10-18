package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.dto.common.GroupeDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.common.PageGroupesDto;
import com.tsix.immocrm.dto.common.TotauxListeDto;
import com.tsix.immocrm.dto.prospect.ContactProspectDto;
import com.tsix.immocrm.dto.prospect.CreerContactProspectRequest;
import com.tsix.immocrm.dto.prospect.HistoriqueDto;
import com.tsix.immocrm.dto.prospection.AjouterAutreTrancheRequest;
import com.tsix.immocrm.dto.prospection.AjouterCategorieRequest;
import com.tsix.immocrm.dto.prospection.AjouterProfilRequest;
import com.tsix.immocrm.dto.prospection.AjouterTypeBienRequest;
import com.tsix.immocrm.dto.prospection.AjouterVilleRequest;
import com.tsix.immocrm.dto.prospection.ArchiverProspectionRequest;
import com.tsix.immocrm.dto.prospection.ChangerEtapeRequest;
import com.tsix.immocrm.dto.prospection.CreerDevisRequest;
import com.tsix.immocrm.dto.prospection.CreerInteractionRequest;
import com.tsix.immocrm.dto.prospection.CreerProspectionRequest;
import com.tsix.immocrm.dto.prospection.DevisDto;
import com.tsix.immocrm.dto.prospection.FiltresAvancesProspection;
import com.tsix.immocrm.dto.prospection.InteractionDto;
import com.tsix.immocrm.dto.prospection.ModifierProspectionRequest;
import com.tsix.immocrm.dto.prospection.ModifierTranchePrincipaleRequest;
import com.tsix.immocrm.dto.prospection.NdaData;
import com.tsix.immocrm.dto.prospection.OffreDevisData;
import com.tsix.immocrm.dto.prospection.ProspectionFicheData;
import com.tsix.immocrm.dto.prospection.ProspectionAutreTrancheDto;
import com.tsix.immocrm.dto.prospection.ProspectionCategorieDto;
import com.tsix.immocrm.dto.prospection.ProspectionDetailDto;
import com.tsix.immocrm.dto.prospection.ProspectionListItemDto;
import com.tsix.immocrm.dto.prospection.ProspectionProfilDto;
import com.tsix.immocrm.dto.prospection.ProspectionTypeBienDto;
import com.tsix.immocrm.dto.prospection.ProspectionVilleDto;
import com.tsix.immocrm.dto.prospection.EnvoyerOffrePrixRequest;
import com.tsix.immocrm.dto.prospection.ResultatEnvoiOffreDto;
import com.tsix.immocrm.dto.prospection.ResultatEnvoiVisiteDto;
import com.tsix.immocrm.dto.prospection.TransformerProspectionRequest;
import com.tsix.immocrm.services.prospection.ConfirmationVisiteService;
import com.tsix.immocrm.services.prospection.OffrePrixService;
import com.tsix.immocrm.services.prospection.DevisService;
import com.tsix.immocrm.services.prospection.FiltresListeProspection;
import com.tsix.immocrm.services.prospection.NdaService;
import com.tsix.immocrm.services.prospection.OffreDevisDocumentService;
import com.tsix.immocrm.services.prospect.ProspectSubResourcesService;
import com.tsix.immocrm.services.prospection.ProspectionQualificationService;
import com.tsix.immocrm.services.prospection.ProspectionService;
import com.tsix.immocrm.services.prospection.ProspectionFicheService;
import com.tsix.immocrm.services.prospection.document.NdaPdfRenderer;
import com.tsix.immocrm.services.prospection.document.OffreDevisPdfRenderer;
import com.tsix.immocrm.services.prospection.document.ProspectionFichePdfRenderer;
import com.tsix.immocrm.domain.ColonneProspection;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.domain.TriGroupe;
import com.tsix.immocrm.web.Groupements;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Prospections")
@RestController
@RequestMapping("/api/prospections")
public class ProspectionController {

    private final ProspectionService prospectionService;
    private final ProspectionQualificationService qualificationService;
    private final DevisService devisService;
    private final OffreDevisDocumentService offreDevisDocumentService;
    private final OffreDevisPdfRenderer offreDevisPdfRenderer;
    private final NdaService ndaService;
    private final NdaPdfRenderer ndaPdfRenderer;
    private final ProspectionFicheService prospectionFicheService;
    private final ProspectionFichePdfRenderer prospectionFichePdfRenderer;
    private final ConfirmationVisiteService confirmationVisiteService;
    private final OffrePrixService offrePrixService;
    private final ProspectSubResourcesService subResourcesService;

    public ProspectionController(ProspectionService prospectionService,
                                  ProspectionQualificationService qualificationService,
                                  DevisService devisService,
                                  OffreDevisDocumentService offreDevisDocumentService,
                                  OffreDevisPdfRenderer offreDevisPdfRenderer,
                                  NdaService ndaService,
                                  NdaPdfRenderer ndaPdfRenderer,
                                  ProspectionFicheService prospectionFicheService,
                                  ProspectionFichePdfRenderer prospectionFichePdfRenderer,
                                  ConfirmationVisiteService confirmationVisiteService,
                                  OffrePrixService offrePrixService,
                                  ProspectSubResourcesService subResourcesService) {
        this.confirmationVisiteService = confirmationVisiteService;
        this.offrePrixService = offrePrixService;
        this.prospectionService = prospectionService;
        this.qualificationService = qualificationService;
        this.devisService = devisService;
        this.offreDevisDocumentService = offreDevisDocumentService;
        this.offreDevisPdfRenderer = offreDevisPdfRenderer;
        this.ndaService = ndaService;
        this.ndaPdfRenderer = ndaPdfRenderer;
        this.prospectionFicheService = prospectionFicheService;
        this.prospectionFichePdfRenderer = prospectionFichePdfRenderer;
        this.subResourcesService = subResourcesService;
    }

    @GetMapping
    @ApiResponse(responseCode = "400",
            description = "Un nom passé à `colonnes` ne figure pas au catalogue. Le corps "
                    + "(ProblemDetail) porte `colonne` et `colonnesValides`.")
    @Operation(
            summary = "Liste paginée des prospections"
    )
    public PageDto<ProspectionListItemDto> findAll(
            @Parameter(example = "3 QUALIFIÉ")
            @RequestParam(required = false) List<String> etape,
            @Parameter(description = "Code(s) ComProspectionOrigin à filtrer. Répéter le "
                    + "paramètre pour plusieurs origines.", example = "FACEBOOK")
            @RequestParam(required = false) List<String> origine,
            @Parameter(description = "Id(s) du commercial (CfgTier.id, cfgCommercial). Répéter "
                    + "le paramètre pour plusieurs commerciaux.")
            @RequestParam(required = false) List<UUID> commercial,
            @Parameter(description = "Id(s) de tranche visée (CfgTranche.id, "
                    + "ComProspection.stkHierarchy). Répéter le paramètre pour plusieurs tranches.")
            @RequestParam(required = false) List<UUID> trancheId,
            @Parameter(example = "2026-01-01")
            @RequestParam(required = false) String dateDebut,
            @Parameter(example = "2026-12-31")
            @RequestParam(required = false) String dateFin,
            @RequestParam(required = false) String search,
            @Parameter(example = "true")
            @RequestParam(required = false) Boolean ouvertes,
            @Parameter(example = "code,prospectNom,joursInactivite,nbInteractions")
            @RequestParam(required = false) List<String> colonnes,
            @Parameter(example = "etape")
            @RequestParam(required = false) String groupeColonne,
            @RequestParam(required = false) String groupeValeur,
            @Parameter(description = "Granularité si `groupeColonne` est une date. Doit être "
                    + "identique à celle passée à `/groupes`. Défaut : MOIS.")
            @RequestParam(required = false) Granularite granularite,
            @ModelAttribute FiltresAvancesProspection filtres,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        // Résolu ici pour que le 400 parte avant toute requête. L'omission des clés non demandées
        // est appliquée à la sérialisation par SelectionColonnesPropertyFilter.
        Set<ColonneProspection> selection = ColonneProspection.resoudre(colonnes);
        ColonneProspection groupe = Groupements.colonneDeDepliage(
                groupeColonne, groupeValeur, ColonneProspection::resoudreGroupement);
        return PageDto.of(prospectionService.findAll(new FiltresListeProspection(etape, origine, commercial,
                        trancheId, false, dateDebut, dateFin, search, ouvertes), filtres, selection, groupe, groupeValeur,
                Groupements.granularite(groupe, granularite), pageable));
    }

    @GetMapping("/groupes")
    @Operation(
            summary = "Liste des prospections repliée en groupes, avec effectifs et totaux",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Groupes, paginés comme une liste"),
                    @ApiResponse(responseCode = "400", content = @Content)
            })
    public PageGroupesDto grouper(
            @Parameter(description = "Colonne de groupement. **Requis.** Un seul niveau.",
                    example = "etape", required = true)
            @RequestParam String groupBy,
            @Parameter(example = "prix,prix:MAX")
            @RequestParam(required = false) List<String> agregats,
            @Parameter(example = "MOIS")
            @RequestParam(required = false) Granularite granularite,
            @Parameter(description = "LIBELLE (croissant, défaut) ou NOMBRE (effectif décroissant).")
            @RequestParam(required = false) TriGroupe tri,
            @Parameter(example = "false")
            @RequestParam(defaultValue = "false") boolean inclureSansValeur,
            @RequestParam(required = false) List<String> etape,
            @RequestParam(required = false) List<String> origine,
            @RequestParam(required = false) List<UUID> commercial,
            @RequestParam(required = false) List<UUID> trancheId,
            @Parameter(example = "2026-01-01")
            @RequestParam(required = false) String dateDebut,
            @Parameter(example = "2026-12-31")
            @RequestParam(required = false) String dateFin,
            @RequestParam(required = false) String search,
            @Parameter(example = "true")
            @RequestParam(required = false) Boolean ouvertes,
            @ModelAttribute FiltresAvancesProspection filtres,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        ColonneProspection groupe = ColonneProspection.resoudreGroupement(groupBy);
        return prospectionService.grouperProspections(new FiltresListeProspection(etape, origine, commercial,
                        trancheId, false, dateDebut, dateFin, search, ouvertes), filtres, groupe,
                Groupements.granularite(groupe, granularite),
                ColonneProspection.resoudreAgregats(agregats),
                tri != null ? tri : TriGroupe.DEFAUT, inclureSansValeur, pageable).versPage();
    }

    @GetMapping("/totaux-colonnes")
    @Operation(
            summary = "Résumés (Σ) de la liste des prospections sur tout le filtre, pas sur une page",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Effectif du filtre et un résumé par jeton demandé"),
                    @ApiResponse(responseCode = "400", description = "Jeton d'`agregats` inconnu, colonne non "
                            + "agrégeable ou fonction refusée pour son type.", content = @Content)
            })
    public TotauxListeDto totauxColonnes(
            @Parameter(example = "prix:AVG")
            @RequestParam(required = false) List<String> agregats,
            @RequestParam(required = false) List<String> etape,
            @RequestParam(required = false) List<String> origine,
            @RequestParam(required = false) List<UUID> commercial,
            @RequestParam(required = false) List<UUID> trancheId,
            @Parameter(example = "2026-01-01")
            @RequestParam(required = false) String dateDebut,
            @Parameter(example = "2026-12-31")
            @RequestParam(required = false) String dateFin,
            @RequestParam(required = false) String search,
            @Parameter(example = "true")
            @RequestParam(required = false) Boolean ouvertes,
            @ModelAttribute FiltresAvancesProspection filtres) {
        return prospectionService.totauxProspections(new FiltresListeProspection(etape, origine, commercial,
                        trancheId, false, dateDebut, dateFin, search, ouvertes), filtres,
                ColonneProspection.resoudreAgregats(agregats));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Détail d'une prospection (ou d'une pré-prospection)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prospection trouvée."),
            @ApiResponse(responseCode = "404")
    })
    public ProspectionDetailDto findById(
            @Parameter(description = "Identifiant de la prospection (ComProspection.PKey).")
            @PathVariable UUID id) {
        return prospectionService.findById(id);
    }

    @PostMapping
    @Operation(
            summary = "Crée une prospection qualifiée pour un prospect existant"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Prospection créée."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "403"),
            @ApiResponse(responseCode = "409")
    })
    public ResponseEntity<ProspectionDetailDto> creerProspection(@RequestBody CreerProspectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prospectionService.creerProspection(request));
    }

    @GetMapping("/{id}/historique")
    @Operation(
            summary = "Historique complet du parcours d'une prospection"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Prospection introuvable.")
    })
    public PageDto<HistoriqueDto> historique(@PathVariable UUID id,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(prospectionService.historique(id, pageable));
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Modifie les champs de qualification d'une prospection"
    )
    public ProspectionDetailDto modifierProspection(@PathVariable UUID id, @RequestBody ModifierProspectionRequest request) {
        return prospectionService.modifierProspection(id, request);
    }

    @PatchMapping("/{id}/tranche-principale")
    @Operation(
            summary = "Remplace la tranche/l'article principal"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tranche/article principal mis à jour."),
            @ApiResponse(responseCode = "400", description = "`trancheId` manquant, tranche/article "
                    + "introuvable, ou l'article fourni n'appartient pas à la tranche fournie."),
            @ApiResponse(responseCode = "403", description = "Le projet de la tranche cible n'est "
                    + "pas dans le périmètre commercial de l'appelant."),
            @ApiResponse(responseCode = "404", description = "Aucune prospection avec cet id, ou "
                    + "prospection hors du périmètre commercial de l'appelant."),
            @ApiResponse(responseCode = "409")
    })
    public ProspectionDetailDto modifierTranchePrincipale(@PathVariable UUID id,
            @RequestBody ModifierTranchePrincipaleRequest request) {
        return prospectionService.modifierTranchePrincipale(id, request);
    }

    @PostMapping("/{id}/transformer")
    @Operation(
            summary = "Transforme une pré-prospection en prospection (commercial + tranche)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prospection transformée : commercial "
                    + "affecté et tranche garantie."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "403", description = "Le projet de la tranche fournie n'est "
                    + "pas dans le périmètre commercial de l'appelant."),
            @ApiResponse(responseCode = "404", description = "Aucune prospection avec cet id."),
            @ApiResponse(responseCode = "409")
    })
    public ProspectionDetailDto transformer(@PathVariable UUID id, @RequestBody TransformerProspectionRequest request) {
        return prospectionService.transformer(id, request);
    }

    @PatchMapping("/{id}/etape")
    public ProspectionDetailDto changerEtape(@PathVariable UUID id, @RequestBody ChangerEtapeRequest request) {
        return prospectionService.changerEtape(id, request);
    }

    @PatchMapping("/{id}/archiver")
    @Operation(
            summary = "Archive une prospection (clôture avec motif)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prospection archivée (`fermee = true`)."),
            @ApiResponse(responseCode = "400", description = "Ni `raisonId` ni `raisonTexte` fourni, "
                    + "ou `raisonId` inconnu."),
            @ApiResponse(responseCode = "404", description = "Aucune prospection avec cet id, ou "
                    + "prospection hors du périmètre commercial de l'appelant.")
    })
    public ProspectionDetailDto archiver(@PathVariable UUID id, @RequestBody ArchiverProspectionRequest request) {
        return prospectionService.archiver(id, request);
    }

    @PatchMapping("/{id}/desarchiver")
    @Operation(
            summary = "Désarchive une prospection (la rouvre dans le pipeline)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prospection rouverte (`fermee = false`, "
                    + "date et motif de clôture toujours renseignés)."),
            @ApiResponse(responseCode = "404", description = "Aucune prospection avec cet id, ou "
                    + "prospection hors du périmètre commercial de l'appelant."),
            @ApiResponse(responseCode = "409", description = "La prospection n'est pas archivée "
                    + "(`fermee` false ou absent) — rien à rouvrir.")
    })
    public ProspectionDetailDto desarchiver(
            @Parameter(description = "Id de la prospection archivée.") @PathVariable UUID id) {
        return prospectionService.desarchiver(id);
    }

    @GetMapping("/{id}/interactions")
    @Operation(
            summary = "Journal des interactions d'une prospection"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page d'interactions (peut être vide)."),
            @ApiResponse(responseCode = "404", description = "Aucune prospection avec cet id.")
    })
    public PageDto<InteractionDto> findInteractions(
            @Parameter(description = "Id de la prospection.") @PathVariable UUID id,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(prospectionService.findInteractions(id, pageable));
    }

    @PostMapping("/{id}/interactions")
    @Operation(
            summary = "Ajoute une interaction à une prospection"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Interaction créée."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "404", description = "Aucune prospection avec cet id.")
    })
    public ResponseEntity<InteractionDto> addInteraction(
            @Parameter(description = "Id de la prospection.") @PathVariable UUID id,
            @RequestBody CreerInteractionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prospectionService.addInteraction(id, request));
    }

    @PostMapping("/{id}/interactions/{interactionId}/email-visite")
    @Operation(
            summary = "Renvoie au prospect la confirmation de sa visite (email + WhatsApp)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tentative effectuée — lire `envoye` "
                    + "et `motif` pour en connaître l'issue."),
            @ApiResponse(responseCode = "404"),
            @ApiResponse(responseCode = "409", description = "Cette interaction ne planifie aucun "
                    + "rendez-vous : il n'y a rien à confirmer.")
    })
    public ResultatEnvoiVisiteDto renvoyerEmailVisite(
            @Parameter(description = "Id de la prospection.") @PathVariable UUID id,
            @PathVariable UUID interactionId) {
        return confirmationVisiteService.renvoyer(id, interactionId);
    }

    @PostMapping("/{id}/offre-prix")
    @Operation(
            summary = "Établit l'offre de prix et l'envoie au prospect (email + WhatsApp)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offre établie et envoi tenté — lire "
                    + "`envoye` et le `motif` de chaque canal pour en connaître l'issue."),
            @ApiResponse(responseCode = "404", description = "Aucune prospection avec cet id, ou "
                    + "prospection hors du périmètre commercial de l'appelant."),
            @ApiResponse(responseCode = "409")
    })
    public ResultatEnvoiOffreDto envoyerOffrePrix(
            @Parameter(description = "Id de la prospection.") @PathVariable UUID id,
            @RequestBody(required = false) EnvoyerOffrePrixRequest request) {
        return offrePrixService.envoyer(id, request);
    }

    // -- Sous-ressources de qualification --

    @GetMapping("/{id}/villes")
    public List<ProspectionVilleDto> getVilles(@PathVariable UUID id) {
        return qualificationService.villes(id);
    }

    @PostMapping("/{id}/villes")
    public ProspectionVilleDto ajouterVille(@PathVariable UUID id, @RequestBody AjouterVilleRequest request) {
        return qualificationService.ajouterVille(id, request);
    }

    @DeleteMapping("/{id}/villes/{itemId}")
    public ResponseEntity<Void> retirerVille(@PathVariable UUID id, @PathVariable UUID itemId) {
        qualificationService.retirerVille(id, itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/categories")
    public List<ProspectionCategorieDto> getCategories(@PathVariable UUID id) {
        return qualificationService.categories(id);
    }

    @PostMapping("/{id}/categories")
    public ProspectionCategorieDto ajouterCategorie(@PathVariable UUID id, @RequestBody AjouterCategorieRequest request) {
        return qualificationService.ajouterCategorie(id, request);
    }

    @DeleteMapping("/{id}/categories/{itemId}")
    public ResponseEntity<Void> retirerCategorie(@PathVariable UUID id, @PathVariable UUID itemId) {
        qualificationService.retirerCategorie(id, itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/types-bien")
    public List<ProspectionTypeBienDto> getTypesBien(@PathVariable UUID id) {
        return qualificationService.typesBien(id);
    }

    @PostMapping("/{id}/types-bien")
    public ProspectionTypeBienDto ajouterTypeBien(@PathVariable UUID id, @RequestBody AjouterTypeBienRequest request) {
        return qualificationService.ajouterTypeBien(id, request);
    }

    @DeleteMapping("/{id}/types-bien/{itemId}")
    public ResponseEntity<Void> retirerTypeBien(@PathVariable UUID id, @PathVariable UUID itemId) {
        qualificationService.retirerTypeBien(id, itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/profils")
    public List<ProspectionProfilDto> getProfils(@PathVariable UUID id) {
        return qualificationService.profils(id);
    }

    @PostMapping("/{id}/profils")
    public ProspectionProfilDto ajouterProfil(@PathVariable UUID id, @RequestBody AjouterProfilRequest request) {
        return qualificationService.ajouterProfil(id, request);
    }

    @DeleteMapping("/{id}/profils/{itemId}")
    public ResponseEntity<Void> retirerProfil(@PathVariable UUID id, @PathVariable UUID itemId) {
        qualificationService.retirerProfil(id, itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/autres-tranches")
    @Operation(
            summary = "Liste les \"autres tranches\" (intérêts secondaires)"
    )
    public List<ProspectionAutreTrancheDto> getAutresTranches(@PathVariable UUID id) {
        return qualificationService.autresTranches(id);
    }

    @PostMapping("/{id}/autres-tranches")
    @Operation(
            summary = "Ajoute un intérêt secondaire sur une autre tranche"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Intérêt secondaire ajouté."),
            @ApiResponse(responseCode = "400", description = "`trancheId` manquant/introuvable, ou "
                    + "l'article fourni n'appartient pas à la tranche fournie."),
            @ApiResponse(responseCode = "404", description = "Aucune prospection avec cet id.")
    })
    public ProspectionAutreTrancheDto ajouterAutreTranche(@PathVariable UUID id, @RequestBody AjouterAutreTrancheRequest request) {
        return qualificationService.ajouterAutreTranche(id, request);
    }

    @DeleteMapping("/{id}/autres-tranches/{itemId}")
    public ResponseEntity<Void> retirerAutreTranche(@PathVariable UUID id, @PathVariable UUID itemId) {
        qualificationService.retirerAutreTranche(id, itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/contacts")
    @Operation(
            summary = "Contacts du prospect de la prospection"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des contacts (éventuellement vide)."),
            @ApiResponse(responseCode = "404", description = "Aucune prospection avec cet id.")
    })
    public List<ContactProspectDto> getContacts(@PathVariable UUID id) {
        return subResourcesService.contactsDeProspection(id);
    }

    @PostMapping("/{id}/contacts")
    @Operation(
            summary = "Ajoute un contact au prospect de la prospection"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contact créé/lié et rattaché."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "404", description = "Aucune prospection avec cet id.")
    })
    public ContactProspectDto ajouterContact(@PathVariable UUID id, @RequestBody CreerContactProspectRequest request) {
        return subResourcesService.ajouterContactAProspection(id, request);
    }

    // -- Devis --

    @GetMapping("/{id}/devis")
    @Operation(
            summary = "Liste les offres (devis) d'une prospection"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page d'offres (peut être vide)."),
            @ApiResponse(responseCode = "404", description = "Aucune prospection avec cet id.")
    })
    public PageDto<DevisDto> getDevis(@PathVariable UUID id,
                                       @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(devisService.findByProspection(id, pageable));
    }

    @GetMapping("/{id}/devis/{devisId}")
    @Operation(
            summary = "Détail d'une offre (devis)",
            description = "Détail complet d'une offre, y compris le HT/TTC de l'onglet "
                    + "\"Montants\" (`montants`)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "L'offre."),
            @ApiResponse(responseCode = "404", description = "Aucune offre avec cet id sur cette prospection.")
    })
    public DevisDto getDevisById(@PathVariable UUID id, @PathVariable UUID devisId) {
        return devisService.findById(id, devisId);
    }

    @PostMapping("/{id}/devis")
    @Operation(
            summary = "Crée une offre (devis) sur une prospection"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offre créée."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "404", description = "Aucune prospection avec cet id.")
    })
    public DevisDto creerDevis(@PathVariable UUID id, @RequestBody CreerDevisRequest request) {
        return devisService.creerDevis(id, request);
    }

    @GetMapping("/{id}/devis/{devisId}/document")
    @Operation(
            summary = "Génère la \"Fiche Offre vente\" (PDF) d'une offre"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Le PDF de la fiche offre vente."),
            @ApiResponse(responseCode = "404", description = "Aucune offre avec cet id sur cette prospection.")
    })
    public ResponseEntity<byte[]> getDevisDocument(@PathVariable UUID id, @PathVariable UUID devisId) {
        OffreDevisData data = offreDevisDocumentService.collect(id, devisId);
        byte[] pdf = offreDevisPdfRenderer.render(data);
        String nomFichier = "offre-" + (data.code() != null ? data.code().replace('/', '-') : devisId) + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(nomFichier).build().toString())
                .body(pdf);
    }

    @GetMapping("/{id}/documents/nda")
    @Operation(
            summary = "Génère la \"NDA AYAD PRIVATE\" (PDF) du prospect de la prospection"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF généré."),
            @ApiResponse(responseCode = "404", description = "Prospection introuvable.")
    })
    public ResponseEntity<byte[]> getNda(@PathVariable UUID id) {
        NdaData data = ndaService.collect(id);
        byte[] pdf = ndaPdfRenderer.render(data);
        String nomFichier = "nda-" + id + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(nomFichier).build().toString())
                .body(pdf);
    }

    @GetMapping("/{id}/documents/fiche")
    @Operation(
            summary = "Génère la \"Fiche Prospection\" (PDF)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF généré."),
            @ApiResponse(responseCode = "404", description = "Prospection introuvable.")
    })
    public ResponseEntity<byte[]> getFiche(@PathVariable UUID id) {
        ProspectionFicheData data = prospectionFicheService.collect(id);
        byte[] pdf = prospectionFichePdfRenderer.render(data);
        String nomFichier = "fiche-prospection-" + (data.code() != null ? data.code().replace('/', '-') : id) + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(nomFichier).build().toString())
                .body(pdf);
    }
}
