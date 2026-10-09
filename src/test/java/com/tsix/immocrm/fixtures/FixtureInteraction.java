package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.com.ComActionMessageCategory;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.repositories.com.ComActionMessageCategoryRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageTypeRepository;
import com.tsix.immocrm.services.prospection.ContexteInteraction;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Référentiel minimal d'une interaction (FEATURE-lot4 §4) : l'action est obligatoire, une Note
 * dispense du résultat. Créer-si-absent, comme {@link FixtureReferentielStk}.
 */
@Component
public class FixtureInteraction {

    private static final UUID TYPE_NOTE = UUID.fromString("5E1A0C3B-7D44-4F0A-9E2B-0C1D2E3F4A51");
    private static final UUID TYPE_TELEPHONE = UUID.fromString("5E1A0C3B-7D44-4F0A-9E2B-0C1D2E3F4A52");

    private final ComActionMessageTypeRepository typeRepository;
    private final ComActionMessageCategoryRepository categoryRepository;

    public FixtureInteraction(ComActionMessageTypeRepository typeRepository,
                              ComActionMessageCategoryRepository categoryRepository) {
        this.typeRepository = typeRepository;
        this.categoryRepository = categoryRepository;
    }

    /** Action de code {@code Note} : message obligatoire, résultat facultatif. */
    public ComActionMessageType typeNote() {
        return type(TYPE_NOTE, "Note");
    }

    /** Action autre qu'une note : résultat obligatoire. */
    public ComActionMessageType typeTelephone() {
        return type(TYPE_TELEPHONE, "Téléphone Sortant");
    }

    /** Catégorie « Prospection » de l'écran parent, à son identifiant légataire. */
    public ComActionMessageCategory categorieProspection() {
        return categoryRepository.findById(ContexteInteraction.CATEGORIE_PROSPECTION).orElseGet(() -> {
            ComActionMessageCategory categorie = new ComActionMessageCategory();
            categorie.setId(ContexteInteraction.CATEGORIE_PROSPECTION);
            categorie.setCode("Prospection");
            categorie.setDescription("Prospection");
            return categoryRepository.save(categorie);
        });
    }

    private ComActionMessageType type(UUID id, String code) {
        return typeRepository.findById(id).orElseGet(() -> {
            ComActionMessageType type = new ComActionMessageType();
            type.setId(id);
            type.setCode(code);
            type.setDescription(code);
            type.setIsSystem(true);
            return typeRepository.save(type);
        });
    }
}
