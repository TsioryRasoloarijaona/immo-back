package com.tsix.immocrm.services.referentiel;

import com.tsix.immocrm.domain.ClassementArticle;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.repositories.stk.StkBuildingRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Listes déroulantes des formulaires patrimoine, filtrées par {@link ClassementArticle} (FEATURE-patrimoine-ecriture.md §6). */
@Service
@Transactional(readOnly = true)
public class ReferentielPatrimoineService {

    private static final Comparator<ReferentielItemDto> PAR_CODE =
            Comparator.comparing(r -> Objects.toString(r.code(), ""));

    private final StkBuildingRepository buildingRepository;
    private final StkItemCategoryRepository categoryRepository;
    private final StkItemTypeRepository typeRepository;
    private final StkVocationRepository vocationRepository;

    public ReferentielPatrimoineService(StkBuildingRepository buildingRepository,
                                        StkItemCategoryRepository categoryRepository,
                                        StkItemTypeRepository typeRepository,
                                        StkVocationRepository vocationRepository) {
        this.buildingRepository = buildingRepository;
        this.categoryRepository = categoryRepository;
        this.typeRepository = typeRepository;
        this.vocationRepository = vocationRepository;
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
