package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.domain.CategorieModele;

import java.util.UUID;

/** Source des champs d'une catégorie : son catalogue et la lecture d'une fiche, périmètre contrôlé. */
public interface CatalogueFiche {

    CategorieModele categorie();

    DescriptionChamps catalogue();

    /** 404 si la fiche est introuvable ou hors du périmètre de l'appelant. */
    FicheLue lire(UUID ficheId);

    record FicheLue(String code, ValeursFiche valeurs) {
    }
}
