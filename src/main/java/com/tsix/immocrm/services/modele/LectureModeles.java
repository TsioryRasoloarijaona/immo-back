package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.entities.app.AppModeleDocument;
import com.tsix.immocrm.entities.app.AppModeleDocumentVersion;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.app.AppModeleDocumentRepository;
import com.tsix.immocrm.repositories.app.AppModeleDocumentVersionRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** Lectures partagées par l'administration et la génération ; s'exécute dans la transaction de l'appelant. */
@Component
public class LectureModeles {

    private final AppModeleDocumentRepository modeleRepository;
    private final AppModeleDocumentVersionRepository versionRepository;

    public LectureModeles(AppModeleDocumentRepository modeleRepository,
                          AppModeleDocumentVersionRepository versionRepository) {
        this.modeleRepository = modeleRepository;
        this.versionRepository = versionRepository;
    }

    public AppModeleDocument trouver(UUID id) {
        return modeleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Modèle de document introuvable: " + id));
    }

    /** Pour imprimer : un modèle inactif est introuvable (404), comme absent du menu. */
    public AppModeleDocument trouverActif(UUID id) {
        AppModeleDocument modele = trouver(id);
        if (!Boolean.TRUE.equals(modele.getActif())) {
            throw new ResourceNotFoundException("Modèle de document introuvable: " + id);
        }
        return modele;
    }

    public Optional<AppModeleDocument> actifParCode(String categorie, String code) {
        return modeleRepository.findByCategorieAndCode(categorie, code).filter(m -> Boolean.TRUE.equals(m.getActif()));
    }

    public Optional<AppModeleDocumentVersion> versionCourante(AppModeleDocument modele) {
        return Optional.ofNullable(modele.getVersionCouranteId()).flatMap(versionRepository::findById);
    }
}
