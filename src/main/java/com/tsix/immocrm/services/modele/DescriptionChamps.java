package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.domain.CategorieModele;
import com.tsix.immocrm.domain.TypeChamp;
import com.tsix.immocrm.dto.modele.CatalogueChampsDto;

import java.util.Optional;

/** Ce que la validation et la génération lisent d'un catalogue, sans dépendre du type de sa fiche source. */
public interface DescriptionChamps {

    boolean estListe(String cle);

    /** Liste qui déclare ce champ, vide pour un champ simple ou inconnu. */
    Optional<String> listeDuChamp(String cle);

    /** Type d'un champ visible depuis {@code listeCourante} (ses champs, puis les champs simples). */
    Optional<TypeChamp> type(String cle, String listeCourante);

    CatalogueChampsDto versDto(CategorieModele categorie);
}
