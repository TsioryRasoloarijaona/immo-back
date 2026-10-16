package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.domain.CategorieModele;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Catalogue de chaque catégorie, par code ; une catégorie sans catalogue n'est pas proposée. */
@Component
public class CataloguesModeles {

    private final Map<CategorieModele, CatalogueFiche> parCategorie = new EnumMap<>(CategorieModele.class);

    public CataloguesModeles(List<CatalogueFiche> catalogues) {
        catalogues.forEach(c -> parCategorie.put(c.categorie(), c));
    }

    public List<CategorieModele> categories() {
        return List.copyOf(parCategorie.keySet());
    }

    /** 400 si le code ne désigne aucune catégorie disponible. */
    public CatalogueFiche de(String categorie) {
        return CategorieModele.depuisCode(categorie)
                .map(parCategorie::get)
                .orElseThrow(() -> new InvalidRequestException("Catégorie inconnue : %s".formatted(categorie)));
    }
}
