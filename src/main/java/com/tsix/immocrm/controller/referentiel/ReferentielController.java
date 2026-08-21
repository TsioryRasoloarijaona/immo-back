package com.tsix.immocrm.controller.referentiel;

import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.referentiel.OrigineProspectionDto;
import com.tsix.immocrm.dto.referentiel.PaysDto;
import com.tsix.immocrm.dto.referentiel.ProfilCategorieDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.dto.referentiel.ResultatInteractionDto;
import com.tsix.immocrm.dto.referentiel.TitreDto;
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

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

/** GAP-02 — read-only lookup tables backing the Prospect/Prospection qualification forms. */
@RestController
@RequestMapping("/api/referentiels")
public class ReferentielController {

    private final ReferentielService referentielService;

    public ReferentielController(ReferentielService referentielService) {
        this.referentielService = referentielService;
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
    @Operation(
            summary = "Résultats possibles pour une interaction (dropdown \"Résultat\")",
            description = "Liste les résultats utilisables comme `resultatId` dans "
                    + "`POST /api/prospections/{id}/interactions` (ex: \"Rendez vous pris\", "
                    + "\"Hors Budget\", \"Client injoignable ou Boite vocale\"...). Chaque entrée "
                    + "indique via `necessiteDateRdv`/`alerteRecommandee` comment adapter le "
                    + "formulaire, sans avoir à coder les libellés en dur côté front."
    )
    public List<ResultatInteractionDto> getResultatsInteraction() {
        return referentielService.resultatsInteraction();
    }

    @GetMapping("/actions-commerciales")
    public List<ReferentielItemDto> getActionsCommerciales() {
        return referentielService.actionsCommerciales();
    }

    @GetMapping("/vocations")
    public List<ReferentielItemDto> getVocations() {
        return referentielService.vocations();
    }

    @GetMapping("/tier-types")
    public List<ReferentielItemDto> getTierTypes() {
        return referentielService.tierTypes();
    }

    @GetMapping("/tier-types/{typeId}/kinds")
    public List<ReferentielItemDto> getTierKindsForType(@PathVariable UUID typeId) {
        return referentielService.tierKindsForType(typeId);
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

    @GetMapping("/professions")
    public List<ReferentielItemDto> getProfessions() {
        return referentielService.professions();
    }

    @GetMapping("/salaires")
    public List<ReferentielItemDto> getSalaires() {
        return referentielService.salaires();
    }

    @GetMapping("/categories-bien")
    public List<ReferentielItemDto> getCategoriesBien() {
        return referentielService.categoriesBien();
    }

    @GetMapping("/types-bien")
    public List<ReferentielItemDto> getTypesBien() {
        return referentielService.typesBien();
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

    @GetMapping("/categories-echeance")
    public List<ReferentielItemDto> getCategoriesEcheance() {
        return referentielService.categoriesEcheance();
    }

    @GetMapping("/profils-prospection")
    public List<ProfilCategorieDto> getProfilsProspection() {
        return referentielService.profilsProspection();
    }
}
