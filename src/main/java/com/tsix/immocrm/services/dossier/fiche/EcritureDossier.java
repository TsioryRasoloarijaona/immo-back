package com.tsix.immocrm.services.dossier.fiche;

import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Chargement d'un dossier pour une action de la fiche et signature GEN0016 ({@code UserUpdate*}) : toute
 * écriture de ce lot sur un dossier passe par ici. Le périmètre est déjà vérifié par {@code PerimetreInterceptor}.
 */
@Component
public class EcritureDossier {

    private final ComFolderRepository folderRepository;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    public EcritureDossier(ComFolderRepository folderRepository, UtilisateurAffiche utilisateurAffiche,
                           Clock clock) {
        this.folderRepository = folderRepository;
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
    }

    public ComFolder charger(UUID dossierId) {
        return folderRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable: " + dossierId));
    }

    /** Signe le dossier au nom de l'appelant et l'enregistre. */
    public ComFolder signer(ComFolder dossier) {
        dossier.setUserUpdate(utilisateurAffiche.courant().nom());
        dossier.setUserUpdateDateTime(LocalDateTime.now(clock));
        return folderRepository.save(dossier);
    }

    public UtilisateurAffiche.Auteur auteur() {
        return utilisateurAffiche.courant();
    }

    public LocalDateTime maintenant() {
        return LocalDateTime.now(clock);
    }
}
