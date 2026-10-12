package com.tsix.immocrm.services.referentiel;

import com.tsix.immocrm.domain.ClassementArticle;
import com.tsix.immocrm.domain.TypeTiersPatrimoine;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.repositories.stk.StkBuildingRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.services.patrimoine.ReferentielsTranche;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Listes déroulantes des formulaires patrimoine, filtrées par {@link ClassementArticle} (FEATURE-patrimoine-ecriture.md
 * §6) ; listes des fiches programme et tranche (FEATURE-patrimoine-tranche-fiche.md §2).
 */
@Service
@Transactional(readOnly = true)
public class ReferentielPatrimoineService {

    private static final Comparator<ReferentielItemDto> PAR_CODE =
            Comparator.comparing(r -> Objects.toString(r.code(), ""));


    private final StkBuildingRepository buildingRepository;
    private final StkItemCategoryRepository categoryRepository;
    private final StkItemTypeRepository typeRepository;
    private final StkVocationRepository vocationRepository;
    private final ReferentielsTranche referentielsTranche;

    public ReferentielPatrimoineService(StkBuildingRepository buildingRepository,
                                        StkItemCategoryRepository categoryRepository,
                                        StkItemTypeRepository typeRepository,
                                        StkVocationRepository vocationRepository,
                                        ReferentielsTranche referentielsTranche) {
        this.buildingRepository = buildingRepository;
        this.categoryRepository = categoryRepository;
        this.typeRepository = typeRepository;
        this.vocationRepository = vocationRepository;
        this.referentielsTranche = referentielsTranche;
    }

    public List<ReferentielItemDto> standings() {
        return referentielsTranche.standings().findAllByOrderByCodeAsc().stream()
                .map(s -> new ReferentielItemDto(s.getId(), s.getCode(), s.getDescription(), null))
                .toList();
    }

    public List<ReferentielItemDto> niveauxTerrain() {
        return referentielsTranche.niveaux().findAllByOrderByCodeAsc().stream()
                .map(n -> new ReferentielItemDto(n.getId(), n.getCode(), n.getDescription(), null))
                .toList();
    }

    public List<ReferentielItemDto> rubriquesTranche() {
        return referentielsTranche.rubriques().findAllByOrderBySortingAscCodeAsc().stream()
                .map(r -> new ReferentielItemDto(r.getId(), r.getCode(), r.getDescription(), r.getSorting()))
                .toList();
    }

    public List<ReferentielItemDto> personnels() {
        return tiersDuType(TypeTiersPatrimoine.PERSONNEL.code());
    }

    public List<ReferentielItemDto> tiersAutrui() {
        return tiersDuType(TypeTiersPatrimoine.FOURNISSEUR.code());
    }

    public List<ReferentielItemDto> plansComptables() {
        return referentielsTranche.comptes().findAllByOrderByCodeAsc().stream()
                .map(a -> new ReferentielItemDto(a.getId(), a.getCode(), a.getDescription(), null))
                .toList();
    }

    public List<ReferentielItemDto> projetsFacturation(UUID entiteId) {
        return referentielsTranche.projetsChantier().pourFacturation(entiteId).stream()
                .map(p -> new ReferentielItemDto(p.getId(), p.getCode(), p.getDescription(), null))
                .toList();
    }

    private List<ReferentielItemDto> tiersDuType(String codeType) {
        return referentielsTranche.tiers().findByCfgTierType_CodeOrderByDescriptionAsc(codeType).stream()
                .map(t -> new ReferentielItemDto(t.getId(), t.getCode(), t.getDescription(), null))
                .toList();
    }

    public List<ReferentielItemDto> immeubles() {
        return buildingRepository.findAllByOrderBySortingAscCodeAsc().stream()
                .map(b -> new ReferentielItemDto(b.getId(), b.getCode(), b.getDescription(), b.getSorting()))
                .toList();
    }

    public List<ReferentielItemDto> categories(boolean pourSousArticle) {
        return categoryRepository.findAll().stream()
                .filter(c -> ClassementArticle.classementEligible(
                        Champs.de(c.getStkKind(), StkKind::getCode), c.getForUnderItem(), pourSousArticle))
                .map(ReferentielPatrimoineService::versItem)
                .sorted(PAR_CODE)
                .toList();
    }

    public List<ReferentielItemDto> types(boolean pourSousArticle) {
        return typeRepository.findAll().stream()
                .filter(t -> ClassementArticle.classementEligible(
                        Champs.de(t.getStkKind(), StkKind::getCode), t.getForUnderItem(), pourSousArticle))
                .map(ReferentielPatrimoineService::versItem)
                .sorted(PAR_CODE)
                .toList();
    }

    public List<ReferentielItemDto> vocationsPourArticle() {
        return vocationRepository.findAll().stream()
                .filter(v -> ClassementArticle.vocationEligible(v.getCode()))
                .map(v -> new ReferentielItemDto(v.getId(), v.getCode(), v.getDescription(), null))
                .toList();
    }

    private static ReferentielItemDto versItem(StkItemCategory c) {
        return new ReferentielItemDto(c.getId(), c.getCode(), c.getDescription(), null);
    }

    private static ReferentielItemDto versItem(StkItemType t) {
        return new ReferentielItemDto(t.getId(), t.getCode(), t.getDescription(), null);
    }
}
