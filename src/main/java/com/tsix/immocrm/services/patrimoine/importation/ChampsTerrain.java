package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.entities.adm.AdmCity;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.ChampImport;
import com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.Portee;
import com.tsix.immocrm.utils.Champs;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.NATURE;
import static com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.PIVOT;
import static com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.VOCATION;
import static com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.champ;
import static com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.code;
import static com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.controle;

/**
 * En-têtes du format {@code STOCK_TERRAINS} (FEATURE-patrimoine-import-export.md §2.2) → champ de
 * {@code TerrainEcritureDto}. Pays, ville, catégorie et type sont lus en texte puis résolus par {@link ImportTerrains}.
 */
final class ChampsTerrain {

    static final String PAYS_ID = "paysId";
    static final String VILLE_ID = "villeId";
    static final String CATEGORIE_ID = "categorieId";
    static final String TYPE_ID = "typeId";

    /** Colonnes communes avec les articles, reprises telles quelles de {@link ChampsArticle}. */
    private static final Set<String> REPRISES = Set.of("Code", "Description", "Orientation", "Catégorie de charge",
            "Adresse", "Notes", "Superficie Prov. (m²)", "Superficie Defi. (m²)", "TVA", "N° Titre foncier",
            "Intitulé du Titre foncier", "Date du Titre foncier");

    static final List<ChampImport> CHAMPS = Stream.concat(Stream.of(
                    controle(PIVOT, StkItem::getCode, Portee.TOUS),
                    controle(NATURE, StkItem::getStkKind, Portee.TOUS),
                    code("Entité", "entiteId", CfgCompany.class, true, StkItem::getCfgCompany),
                    champ("Pays", PAYS_ID, TypeCellule.TEXTE, i -> Champs.de(i.getAdmCountry(), AdmCountry::getCode)),
                    champ("Ville", VILLE_ID, TypeCellule.TEXTE, i -> Champs.de(i.getAdmCity(), AdmCity::getCode)),
                    code(VOCATION, "vocationId", StkVocation.class, true, StkItem::getStkVocation),
                    champ("Catégorie", CATEGORIE_ID, TypeCellule.TEXTE,
                            i -> Champs.de(i.getStkItemCategory(), StkItemCategory::getCode)),
                    champ("Type", TYPE_ID, TypeCellule.TEXTE, i -> Champs.de(i.getStkItemType(), StkItemType::getCode))),
            ChampsArticle.CHAMPS.stream().filter(c -> c.rang() == 0 && REPRISES.contains(c.entete()))).toList();

    private ChampsTerrain() {
    }
}
