package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.ClassementArticle;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkBuilding;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.exceptions.ChampInvalideException;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.stk.StkBuildingRepository;
import com.tsix.immocrm.repositories.stk.StkFloorRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkKindRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.str.StrEntityRepository;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Références d'un article citées dans le corps d'une écriture : introuvable ou hors classement = 400 sur le champ
 * concerné. Les deux constantes d'OMRAN (nature Bâtiment, type de document StkItem) sont lues par code, jamais en dur.
 */
@Component
class ReferencesArticle {

    private static final String CODE_STR_ENTITY_ARTICLE = "StkItem";
    private static final String INTROUVABLE = "%s introuvable : %s";
    private static final String CHAMP_VOCATION = "vocationId";

    private final CfgTrancheRepository trancheRepository;
    private final StkVocationRepository vocationRepository;
    private final StkItemCategoryRepository categoryRepository;
    private final StkItemTypeRepository typeRepository;
    private final StkBuildingRepository buildingRepository;
    private final StkFloorRepository floorRepository;
    private final StkKindRepository kindRepository;
    private final StrEntityRepository strEntityRepository;

    @SuppressWarnings("java:S107") // un dépôt par référentiel d'un article, regroupés ici pour alléger le service
    ReferencesArticle(CfgTrancheRepository trancheRepository, StkVocationRepository vocationRepository,
                      StkItemCategoryRepository categoryRepository, StkItemTypeRepository typeRepository,
                      StkBuildingRepository buildingRepository, StkFloorRepository floorRepository,
                      StkKindRepository kindRepository, StrEntityRepository strEntityRepository) {
        this.trancheRepository = trancheRepository;
        this.vocationRepository = vocationRepository;
        this.categoryRepository = categoryRepository;
        this.typeRepository = typeRepository;
        this.buildingRepository = buildingRepository;
        this.floorRepository = floorRepository;
        this.kindRepository = kindRepository;
        this.strEntityRepository = strEntityRepository;
    }

    CfgTranche tranche(UUID id) {
        return trancheRepository.findById(id)
                .orElseThrow(() -> new ChampInvalideException("trancheId", INTROUVABLE.formatted("Tranche", id)));
    }

    StkVocation vocation(UUID id) {
        StkVocation vocation = vocationRepository.findById(id)
                .orElseThrow(() -> new ChampInvalideException(CHAMP_VOCATION, INTROUVABLE.formatted("Vocation", id)));
        if (!ClassementArticle.vocationEligible(vocation.getCode())) {
            throw new ChampInvalideException(CHAMP_VOCATION, "La vocation « Projet » est réservée aux terrains.");
        }
        return vocation;
    }

    StkVocation vocationParDefaut() {
        return vocationRepository.findFirstByCode(ClassementArticle.VOCATION_VENTE)
                .orElseThrow(() -> new ChampInvalideException(CHAMP_VOCATION,
                        "Aucune vocation VENTE paramétrée : choisissez une vocation."));
    }

    StkItemCategory categorie(UUID id, boolean pourSousArticle) {
        StkItemCategory categorie = categoryRepository.findById(id)
                .orElseThrow(() -> new ChampInvalideException("categorieId", INTROUVABLE.formatted("Catégorie", id)));
        if (!ClassementArticle.classementEligible(Champs.de(categorie.getStkKind(), StkKind::getCode),
                categorie.getForUnderItem(), pourSousArticle)) {
            throw new ChampInvalideException("categorieId", messageClassement("catégorie", pourSousArticle));
        }
        return categorie;
    }

    StkItemType type(UUID id, boolean pourSousArticle) {
        StkItemType type = typeRepository.findById(id)
                .orElseThrow(() -> new ChampInvalideException("typeId", INTROUVABLE.formatted("Type", id)));
        if (!ClassementArticle.classementEligible(Champs.de(type.getStkKind(), StkKind::getCode),
                type.getForUnderItem(), pourSousArticle)) {
            throw new ChampInvalideException("typeId", messageClassement("type", pourSousArticle));
        }
        return type;
    }

    // Formulation de l'aide OMRAN (page ARTICLES).
    private static String messageClassement(String quoi, boolean pourSousArticle) {
        return pourSousArticle
                ? "La %s d'un sous-article doit avoir la nature \"Bâtiment\" et être paramétrée \"Pour les sous articles\"."
                        .formatted(quoi)
                : "La %s doit avoir la nature \"Bâtiment\" et ne pas être paramétrée \"Pour les sous articles\"."
                        .formatted(quoi);
    }

    StkBuilding immeuble(UUID id) {
        return buildingRepository.findById(id)
                .orElseThrow(() -> new ChampInvalideException("immeubleId", INTROUVABLE.formatted("Immeuble", id)));
    }

    StkFloor etage(UUID id) {
        return floorRepository.findById(id)
                .orElseThrow(() -> new ChampInvalideException("etageId", INTROUVABLE.formatted("Étage", id)));
    }

    StkKind natureBatiment() {
        return kindRepository.findFirstByCode(ClassementArticle.NATURE_BATIMENT)
                .orElseThrow(() -> new IllegalStateException("Nature StkKind « BAT » absente de la base."));
    }

    StrEntity typeDocumentArticle() {
        return strEntityRepository.findByCodeIn(List.of(CODE_STR_ENTITY_ARTICLE)).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("StrEntity « StkItem » absente de la base."));
    }
}
