package com.tsix.immocrm.exceptions;

import com.tsix.immocrm.dto.prospection.DossierLieDto;

/** L'offre a déjà donné un dossier de vente → 409 avec {@code dossierExistant} (FEATURE-offre-vers-dossier.md B1). */
public class OffreDejaTransformeeException extends ConflictException {

    private final transient DossierLieDto dossierExistant;

    public OffreDejaTransformeeException(String codeOffre, DossierLieDto dossier) {
        super("L'offre " + codeOffre + " a déjà donné le dossier " + dossier.code() + " (" + dossier.id()
                + ") : ouvrez-le plutôt que d'en créer un second.");
        this.dossierExistant = dossier;
    }

    public DossierLieDto getDossierExistant() {
        return dossierExistant;
    }
}
