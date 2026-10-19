package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.entities.stk.StkBuilding;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.exceptions.ConfigurationManquanteException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.stk.StkBuildingRepository;
import com.tsix.immocrm.repositories.stk.StkFloorRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkKindRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.str.StrEntityRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.services.patrimoine.ArticleService.NATURE_BATIMENT;
import static com.tsix.immocrm.services.patrimoine.ArticleService.VOCATION_PROJET;

/** Références d'un article saisies par l'utilisateur ou posées par le serveur (FEATURE-patrimoine-ecriture.md §5.2, §5.3). */
@Component
record ReferencesArticle(StkVocationRepository vocations, StkItemCategoryRepository categories,
                         StkItemTypeRepository types, StkBuildingRepository immeubles, StkFloorRepository etages,
                         StkKindRepository natures, StrEntityRepository entites) {

    private static final String VOCATION_VENTE = "VENTE";
    private static final String CODE_ENTITE_ARTICLE = "StkItem";

    StkVocation vocation(UUID id) {
        if (id == null) {
            return vocations.findFirstByCode(VOCATION_VENTE)
                    .orElseThrow(() -> new ConfigurationManquanteException("Vocation VENTE absente de StkVocation."));
        }
        StkVocation vocation = vocations.findById(id)
                .orElseThrow(() -> new InvalidRequestException("Vocation inconnue : " + id));
        if (VOCATION_PROJET.equals(vocation.getCode())) {
            throw new InvalidRequestException("La vocation PROJET ne s'applique pas à un article.");
        }
        return vocation;
    }

    StkItemCategory categorie(UUID id, boolean pourSousArticle) {
        StkItemCategory categorie = categories.findById(requis(id, "La catégorie"))
                .orElseThrow(() -> new InvalidRequestException("Catégorie inconnue : " + id));
        verifierClassement(categorie.getStkKind(), categorie.getForUnderItem(), pourSousArticle, "La catégorie");
        return categorie;
    }

    StkItemType type(UUID id, boolean pourSousArticle) {
        StkItemType type = types.findById(requis(id, "Le type"))
                .orElseThrow(() -> new InvalidRequestException("Type inconnu : " + id));
        verifierClassement(type.getStkKind(), type.getForUnderItem(), pourSousArticle, "Le type");
        return type;
    }

    StkBuilding immeuble(UUID id) {
        return immeubles.findById(requis(id, "L'immeuble"))
                .orElseThrow(() -> new InvalidRequestException("Immeuble inconnu : " + id));
    }

    StkFloor etage(UUID id) {
        return etages.findById(requis(id, "L'étage"))
                .orElseThrow(() -> new InvalidRequestException("Étage inconnu : " + id));
    }

    StkKind natureBatiment() {
        return natures.findFirstByCode(NATURE_BATIMENT)
                .orElseThrow(() -> new ConfigurationManquanteException("Nature BAT absente de StkKind."));
    }

    /** Lue en base, jamais en dur : sans elle l'article est invisible dans OMRAN. */
    UUID entiteArticle() {
        return entites.findByCodeIn(List.of(CODE_ENTITE_ARTICLE)).stream().findFirst().map(StrEntity::getId)
                .orElseThrow(() -> new ConfigurationManquanteException("StrEntity StkItem absente."));
    }

    private static UUID requis(UUID id, String libelle) {
        if (id == null) {
            throw new InvalidRequestException(libelle + " est obligatoire.");
        }
        return id;
    }

    /** Aide OMRAN ARTICLES : nature « Bâtiment », paramétrée ou non « Pour les sous articles » selon le lot. */
    private static void verifierClassement(StkKind nature, Boolean pourSousArticles, boolean attendu, String libelle) {
        boolean batiment = nature != null && NATURE_BATIMENT.equals(nature.getCode());
        if (!batiment || Boolean.TRUE.equals(pourSousArticles) != attendu) {
            throw new InvalidRequestException(attendu
                    ? libelle + " doit avoir la nature Bâtiment et être paramétrée pour les sous-articles."
                    : libelle + " doit avoir la nature Bâtiment et ne pas être paramétrée pour les sous-articles.");
        }
    }
}
