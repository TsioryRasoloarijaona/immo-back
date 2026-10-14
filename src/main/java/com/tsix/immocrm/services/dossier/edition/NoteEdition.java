package com.tsix.immocrm.services.dossier.edition;

import com.tsix.immocrm.domain.ActionsDossier;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageCategory;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.repositories.com.ActionDossierRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageCategoryRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageTypeRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.dossier.fiche.EcritureDossier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Note du dossier recopiée en action système de type « Note » ({@code Sale.cs:MigrateNotes}) : seulement si la
 * note est non vide et a changé ; catégorie selon le statut comme toute action du dossier.
 */
@Component
class NoteEdition {

    static final String CODE_TYPE_NOTE = "Note";

    private final ActionDossierRepository actionRepository;
    private final ComActionMessageTypeRepository typeRepository;
    private final ComActionMessageCategoryRepository categoryRepository;
    private final EcritureDossier ecriture;

    NoteEdition(ActionDossierRepository actionRepository, ComActionMessageTypeRepository typeRepository,
                ComActionMessageCategoryRepository categoryRepository, EcritureDossier ecriture) {
        this.actionRepository = actionRepository;
        this.typeRepository = typeRepository;
        this.categoryRepository = categoryRepository;
        this.ecriture = ecriture;
    }

    /** Vrai si la note a changé ; {@code null} = vider (aucune action, comme OMRAN pour une note vide). */
    boolean appliquer(ComFolder dossier, String note) {
        if (Objects.equals(note, dossier.getNotes())) {
            return false;
        }
        dossier.setNotes(note);
        if (note != null) {
            actionRepository.save(action(dossier, note));
        }
        return true;
    }

    private ComActionMessage action(ComFolder dossier, String note) {
        ComActionMessageType type = typeRepository.findFirstByCode(CODE_TYPE_NOTE)
                .orElseThrow(() -> new ConflictException("Le type d'action « Note » est absent du référentiel."));
        StrStatus statut = dossier.getStrStatus();
        UtilisateurAffiche.Auteur auteur = ecriture.auteur();
        LocalDateTime maintenant = ecriture.maintenant();

        ComActionMessage m = new ComActionMessage();
        m.setId(UUID.randomUUID());
        m.setComFolder(dossier);
        m.setComProspection(dossier.getComProspection());
        m.setCfgTier(dossier.getCfgCustomer());
        m.setCfgEmployee(dossier.getCfgCommercial());
        m.setStkItem(dossier.getStkItem());
        m.setStrFolderStatusId(statut != null ? statut.getId() : null);
        m.setComActionMessageType(type);
        m.setComActionMessageCategory(categorie(statut));
        m.setDocDate(maintenant);
        m.setAlertMessage(note);
        m.setDescription(type.getDescription());
        m.setAdmUser(auteur.compte());
        m.setUserName(auteur.nom());
        m.setUserCreate(auteur.nom());
        m.setUserCreateDateTime(maintenant);
        m.setIsIn(false);
        m.setIsSystem(true);
        m.setIsUnpaid(false);
        m.setIsNotRead(false);
        m.setWithoutRevival(true);
        return m;
    }

    /** Statut inconnu ou catégorie absente du référentiel (base de test) : l'action reste enregistrable. */
    private ComActionMessageCategory categorie(StrStatus statut) {
        if (statut == null || statut.getCode() == null) {
            return null;
        }
        try {
            UUID id = ActionsDossier.categoriePourStatut(StatutDossier.fromCode(statut.getCode()));
            return categoryRepository.findById(id).orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
