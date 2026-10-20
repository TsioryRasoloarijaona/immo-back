package com.tsix.immocrm.controller.referentiel;

import com.tsix.immocrm.domain.ColonneSuiviAction;
import com.tsix.immocrm.domain.StatutArticle;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.patrimoine.ArticleListItemDto;
import com.tsix.immocrm.dto.referentiel.OrigineProspectionDto;
import com.tsix.immocrm.dto.referentiel.PaysDto;
import com.tsix.immocrm.dto.referentiel.ProfilCategorieDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.dto.referentiel.ResultatInteractionDto;
import com.tsix.immocrm.dto.referentiel.TitreDto;
import com.tsix.immocrm.dto.referentiel.TypeReglementDto;
import com.tsix.immocrm.dto.referentiel.VilleDto;
import com.tsix.immocrm.services.referentiel.ReferentielService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.tsix.immocrm.domain.ColonneDossier;
import com.tsix.immocrm.domain.ColonneEncaissement;
import com.tsix.immocrm.domain.ColonneImpaye;
import com.tsix.immocrm.domain.ColonneProspect;
import com.tsix.immocrm.domain.ColonneProspection;
import com.tsix.immocrm.dto.referentiel.ColonneDto;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Référentiels")
@RestController
@RequestMapping("/api/referentiels")
public class ReferentielController {

    private static final String POUR_SOUS_ARTICLE = "Renseigné : nature Bâtiment et « pour les sous-articles » égal "
            + "à la valeur. Absent = toutes.";
    private static final String NATURE = "Code de la nature (StkKind), ex. TER : ne garde que ses références ; "
            + "remplace Bâtiment si pourSousArticle est renseigné. Code inconnu = 400. Absent = toutes.";

    private final ReferentielService referentielService;

    public ReferentielController(ReferentielService referentielService) {
        this.referentielService = referentielService;
    }

    @GetMapping("/roles")
    @Operation(summary = "Liste des rôles applicatifs (AdmRole)")
    public List<ReferentielItemDto> getRoles() {
        return referentielService.roles();
    }

    @GetMapping("/etapes-prospection")
    public List<ReferentielItemDto> getEtapesProspection() {
        return referentielService.etapesProspection();
    }

    @GetMapping("/origines-prospection")
    public List<OrigineProspectionDto> getOriginesProspection() {
        return referentielService.originesProspection();
    }

    @GetMapping("/origines-prospection/{origineId}/supports")
    public List<ReferentielItemDto> getSupportsForOrigine(@PathVariable UUID origineId) {
        return referentielService.supportsForOrigine(origineId);
    }

    @GetMapping("/raisons-cloture")
    public List<ReferentielItemDto> getRaisonsCloture() {
        return referentielService.raisonsCloture();
    }

    @GetMapping("/types-contact-prospection")
    public List<ReferentielItemDto> getTypesContactProspection() {
        return referentielService.typesContactProspection();
    }

    @GetMapping("/resultats-interaction")
    @Operation(summary = "Résultats possibles pour une interaction (dropdown \"Résultat\")")
    public List<ResultatInteractionDto> getResultatsInteraction() {
        return referentielService.resultatsInteraction();
    }

    @GetMapping("/actions-commerciales")
    public List<ReferentielItemDto> getActionsCommerciales() {
        return referentielService.actionsCommerciales();
    }

    @GetMapping("/vocations")
    public List<ReferentielItemDto> getVocations(
            @Parameter(description = "true : sans la vocation PROJET (formulaire d'article). Absent = toutes.")
            @RequestParam(defaultValue = "false") boolean pourArticle) {
        return referentielService.vocations(pourArticle);
    }

    @GetMapping("/natures")
    @Operation(summary = "Dropdown \"Nature\" pour le filtre natureId des endpoints Article")
    public List<ReferentielItemDto> getNatures() {
        return referentielService.natures();
    }

    @GetMapping("/etages")
    @Operation(summary = "Dropdown \"Étage\" (StkFloor), trié par Sorting puis code")
    public List<ReferentielItemDto> getEtages() {
        return referentielService.etages();
    }

    @GetMapping("/tier-types")
    public List<ReferentielItemDto> getTierTypes() {
        return referentielService.tierTypes();
    }

    @GetMapping("/tier-types/{typeId}/kinds")
    public List<ReferentielItemDto> getTierKindsForType(@PathVariable UUID typeId) {
        return referentielService.tierKindsForType(typeId);
    }

    @GetMapping("/types-employe")
    @Operation(summary = "Types d'employé (CfgTierKind)")
    public List<ReferentielItemDto> getTypesEmploye() {
        return referentielService.typesEmploye();
    }

    @GetMapping("/titres")
    public List<TitreDto> getTitres() {
        return referentielService.titres();
    }

    @GetMapping("/sexes")
    public List<ReferentielItemDto> getSexes() {
        return referentielService.sexes();
    }

    @GetMapping("/pays")
    public List<PaysDto> getPays() {
        return referentielService.pays();
    }

    @GetMapping("/nationalites")
    public List<ReferentielItemDto> getNationalites() {
        return referentielService.nationalites();
    }

    @GetMapping("/situations-familiales")
    public List<ReferentielItemDto> getSituationsFamiliales() {
        return referentielService.situationsFamiliales();
    }

    @GetMapping("/types-residence")
    public List<ReferentielItemDto> getTypesResidence() {
        return referentielService.typesResidence();
    }

    @GetMapping("/regimes-matrimoniaux")
    public List<ReferentielItemDto> getRegimesMatrimoniaux() {
        return referentielService.regimesMatrimoniaux();
    }

    @GetMapping("/types-piece-identite")
    public List<ReferentielItemDto> getTypesPieceIdentite() {
        return referentielService.typesPieceIdentite();
    }

    @GetMapping("/types-piece-jointe")
    @Operation(summary = "Types de pièce jointe disponibles")
    public List<ReferentielItemDto> getTypesPieceJointe() {
        return referentielService.typesPieceJointe();
    }

    @GetMapping("/professions")
    public List<ReferentielItemDto> getProfessions() {
        return referentielService.professions();
    }

    @GetMapping("/salaires")
    public List<ReferentielItemDto> getSalaires() {
        return referentielService.salaires();
    }

    @GetMapping("/categories-bien")
    public List<ReferentielItemDto> getCategoriesBien(
            @Parameter(description = POUR_SOUS_ARTICLE) @RequestParam(required = false) Boolean pourSousArticle,
            @Parameter(description = NATURE, example = "TER") @RequestParam(required = false) String nature) {
        return referentielService.categoriesBien(pourSousArticle, nature);
    }

    @GetMapping("/types-bien")
    public List<ReferentielItemDto> getTypesBien(
            @Parameter(description = POUR_SOUS_ARTICLE) @RequestParam(required = false) Boolean pourSousArticle,
            @Parameter(description = NATURE, example = "TER") @RequestParam(required = false) String nature) {
        return referentielService.typesBien(pourSousArticle, nature);
    }

    @GetMapping("/types-relation-contact")
    public List<ReferentielItemDto> getTypesRelationContact() {
        return referentielService.typesRelationContact();
    }

    @GetMapping("/villes")
    public PageDto<VilleDto> getVilles(@RequestParam(required = false) UUID paysId,
                                        @RequestParam(required = false) String search,
                                        @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(referentielService.villes(paysId, search, pageable));
    }

    @GetMapping("/modes-paiement")
    public List<ReferentielItemDto> getModesPaiement() {
        return referentielService.modesPaiement();
    }

    @GetMapping("/types-reglement")
    @Operation(summary = "Dropdown \"Type\" pour l'ajout d'un règlement")
    public List<TypeReglementDto> getTypesReglement() {
        return referentielService.typesReglement();
    }

    @GetMapping("/banques")
    @Operation(summary = "Dropdown \"Banque\" pour l'ajout d'un règlement")
    public List<ReferentielItemDto> getBanques() {
        return referentielService.banques();
    }

    @GetMapping("/entites")
    @Operation(summary = "Dropdown \"Entité\" pour GET /api/encaissements")
    public List<ReferentielItemDto> getEntites() {
        return referentielService.entites();
    }

    @GetMapping("/comptes")
    @Operation(
            summary = "Dropdown \"Compte\" (encaissements, pointage d'un règlement)",
            description = "Comptes bancaires (StlAccount) triés par libellé ; filtrés sur la société si societeId est donné."
    )
    public List<ReferentielItemDto> getComptes(
            @Parameter(description = "Société du règlement à pointer ; absent = tous les comptes.")
            @RequestParam(required = false) UUID societeId) {
        return referentielService.comptes(societeId);
    }

    @GetMapping("/depots")
    @Operation(
            summary = "Dropdown \"Dépôt\" pour GET /api/encaissements",
            description = "Catégories de dépôt (StlDeposit) — 3 lignes en données réelles : "
                    + "FOURNISSEUR/CLIENT/BANQUE."
    )
    public List<ReferentielItemDto> getDepots() {
        return referentielService.depots();
    }

    @GetMapping("/categories-echeance")
    public List<ReferentielItemDto> getCategoriesEcheance() {
        return referentielService.categoriesEcheance();
    }

    @GetMapping("/profils-prospection")
    public List<ProfilCategorieDto> getProfilsProspection() {
        return referentielService.profilsProspection();
    }

    @GetMapping("/projets")
    @Operation(summary = "Dropdown \"Projet\"")
    public List<ReferentielItemDto> getProjets() {
        return referentielService.projets();
    }

    @GetMapping("/tranches")
    @Operation(summary = "Dropdown \"Tranche\"")
    public List<ReferentielItemDto> getTranches(
            @Parameter(description = "Id du projet (CfgProject.id) pour restreindre les tranches. Absent = toutes.")
            @RequestParam(required = false) UUID projetId) {
        return referentielService.tranches(projetId);
    }

    @GetMapping("/articles")
    @Operation(summary = "Dropdown \"Article\" d'une tranche")
    public List<ArticleListItemDto> getArticles(
            @Parameter(description = "Id de la tranche (CfgTranche.id). Obligatoire.")
            @RequestParam UUID trancheId,
            @Parameter(description = "Filtre sur la disponibilité. Absent = DISPONIBLE.")
            @RequestParam(required = false) StatutArticle statut,
            @Parameter(description = "Id(s) de StkItemCategory. Répétable, correspondance OR. Absent = toutes.")
            @RequestParam(required = false) List<UUID> categorieId,
            @Parameter(description = "Id(s) de StkItemType. Répétable, correspondance OR. Absent = tous.")
            @RequestParam(required = false) List<UUID> typeId) {
        return referentielService.articles(trancheId, statut, categorieId, typeId);
    }

    @GetMapping("/sous-articles")
    @Operation(summary = "Dropdown \"Sous-article\" d'un article")
    public List<ArticleListItemDto> getSousArticles(
            @Parameter(description = "Id de l'article parent (StkItem.id). Obligatoire.")
            @RequestParam UUID articleId,
            @Parameter(description = "Id(s) de StkItemCategory. Répétable, correspondance OR. Absent = toutes.")
            @RequestParam(required = false) List<UUID> categorieId,
            @Parameter(description = "Id(s) de StkItemType. Répétable, correspondance OR. Absent = tous.")
            @RequestParam(required = false) List<UUID> typeId,
            @Parameter(description = "Filtre sur la disponibilité. Absent = DISPONIBLE.")
            @RequestParam(required = false) StatutArticle statut) {
        return referentielService.sousArticles(articleId, statut, categorieId, typeId);
    }

    @GetMapping("/colonnes/prospections")
    @Operation(summary = "Catalogue des colonnes de la liste des prospections")
    public List<ColonneDto> getColonnesProspections() {
        return Arrays.stream(ColonneProspection.values())
                .map(c -> ColonneDto.de(c.nom(), c.libelle(), c.groupe().name(), c.type().name(),
                        c.parDefaut(), c.chemin()))
                .toList();
    }

    @GetMapping("/colonnes/prospects")
    @Operation(summary = "Catalogue des colonnes de la liste des prospects")
    public List<ColonneDto> getColonnesProspects() {
        return Arrays.stream(ColonneProspect.values())
                .map(c -> ColonneDto.de(c.nom(), c.libelle(), c.groupe().name(), c.type().name(),
                        c.parDefaut(), c.chemin()))
                .toList();
    }

    @GetMapping("/colonnes/dossiers")
    @Operation(summary = "Catalogue des colonnes de la liste des dossiers de vente")
    public List<ColonneDto> getColonnesDossiers() {
        return Arrays.stream(ColonneDossier.values())
                .map(c -> ColonneDto.de(c.nom(), c.libelle(), c.groupe().name(), c.type().name(),
                        c.parDefaut(), c.chemin()))
                .toList();
    }

    @GetMapping("/colonnes/encaissements")
    @Operation(summary = "Catalogue des colonnes de la liste des encaissements")
    public List<ColonneDto> getColonnesEncaissements() {
        return Arrays.stream(ColonneEncaissement.values())
                .map(c -> ColonneDto.de(c.nom(), c.libelle(), c.groupe().name(), c.type().name(),
                        c.parDefaut(), c.chemin()))
                .toList();
    }

    @GetMapping("/colonnes/impayes")
    @Operation(summary = "Catalogue des colonnes de la liste des impayés (aucune n'est groupable)")
    public List<ColonneDto> getColonnesImpayes() {
        return Arrays.stream(ColonneImpaye.values())
                .map(c -> ColonneDto.de(c.nom(), c.libelle(), c.groupe().name(), c.type().name(),
                        c.parDefaut(), c.chemin()))
                .toList();
    }

    @GetMapping("/colonnes/suivi-actions")
    @Operation(summary = "Catalogue des colonnes du suivi des actions")
    public List<ColonneDto> getColonnesSuiviActions() {
        return Arrays.stream(ColonneSuiviAction.values())
                .map(c -> ColonneDto.de(c.nom(), c.libelle(), c.groupe().name(), c.type().name(),
                        c.parDefaut(), c.chemin()))
                .toList();
    }
}
