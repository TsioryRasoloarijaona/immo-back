package com.tsix.immocrm.services.dossier.workflow;

import com.tsix.immocrm.domain.ActionsDossier;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmAlertData;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageCategory;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.adm.AlerteStatutDossierRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageCategoryRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageTypeRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Alertes d'un changement de statut, comme {@code Sale.cs:BeforeUpdate} : extinction des relances de la prospection
 * ({@code ActionMessage.ManageAlertForComFolder}, impayés épargnés — GEN0005 §4.5), puis l'alerte système « l'état …
 * est expiré » datée de la fin de validité du nouveau statut ({@code ManageActionsForComFolder}).
 */
@Component
public class AlertesStatutDossier {

    /** {@code ComActionMessageType} « Note » (PKey de production). */
    static final UUID TYPE_NOTE = UUID.fromString("8EE7C999-F031-445A-B92F-0F75B090B7E6");
    static final String SYSTEME = "Système";
    private static final int SANS_ORDRE = -1;

    private final ComActionMessageRepository messageRepository;
    private final ComActionMessageTypeRepository typeRepository;
    private final ComActionMessageCategoryRepository categoryRepository;
    private final AlerteStatutDossierRepository alerteRepository;
    private final Clock clock;

    public AlertesStatutDossier(ComActionMessageRepository messageRepository,
                                ComActionMessageTypeRepository typeRepository,
                                ComActionMessageCategoryRepository categoryRepository,
                                AlerteStatutDossierRepository alerteRepository, Clock clock) {
        this.messageRepository = messageRepository;
        this.typeRepository = typeRepository;
        this.categoryRepository = categoryRepository;
        this.alerteRepository = alerteRepository;
        this.clock = clock;
    }

    /** Relances désarmées ({@code WithoutRevival}) puis alertes éteintes jusqu'à l'ordre du nouveau statut. */
    void eteindre(ComFolder dossier, StrStatus nouveau) {
        if (dossier.getComProspection() == null) {
            return;
        }
        UUID prospection = dossier.getComProspection().getId();
        List<ComActionMessage> relances = messageRepository
                .findByComProspectionIdAndAlertValidityDateIsNotNullAndComDocumentIsNull(prospection);
        for (ComActionMessage relance : relances) {
            // Prédicat SQL d'OMRAN « IsUnpaid=0 » : un IsUnpaid NULL n'est pas touché.
            if (Boolean.FALSE.equals(relance.getIsUnpaid())) {
                relance.setWithoutRevival(true);
            }
        }
        messageRepository.saveAll(relances);
        int ordre = nouveau.getSorting() != null ? nouveau.getSorting() : SANS_ORDRE;
        List<AdmAlertData> alertes = alerteRepository.aEteindre(prospection, ordre);
        alertes.forEach(a -> a.setActive(false));
        alerteRepository.saveAll(alertes);
    }

    /** L'action système et son alerte, posées quand la date de validité du statut a changé. */
    void alerteExpiration(ComFolder dossier, StrStatus statut, StatutDossier connu) {
        LocalDateTime maintenant = LocalDateTime.now(clock);
        ComActionMessageCategory categorie = connu != null
                ? categoryRepository.findById(ActionsDossier.categoriePourStatut(connu)).orElse(null)
                : null;
        ComActionMessage action = new ComActionMessage();
        action.setId(UUID.randomUUID());
        action.setDocDate(maintenant);
        action.setComProspection(dossier.getComProspection());
        action.setCfgTier(dossier.getCfgCustomer());
        action.setComFolder(dossier);
        action.setCfgEmployee(dossier.getCfgCommercial());
        action.setStkItem(dossier.getStkItem());
        action.setComActionMessageType(typeRepository.findById(TYPE_NOTE).orElse(null));
        action.setComActionMessageCategory(categorie);
        action.setStrFolderStatusId(statut.getId());
        action.setAlertValidityDate(dossier.getStatusValidityDate());
        action.setAlertMessage("l'état " + statut.getDescription() + " du dossier de vente N° " + dossier.getCode()
                + " est expiré");
        action.setIsSystem(true);
        action.setUserName(SYSTEME);
        action.setUserCreate(SYSTEME);
        action.setUserCreateDateTime(maintenant);
        action.setIsIn(false);
        action.setIsUnpaid(false);
        action.setWithoutRevival(false);
        ComActionMessage enregistree = messageRepository.save(action);

        AdmAlertData alerte = new AdmAlertData();
        UUID id = UUID.randomUUID();
        alerte.setId(id);
        alerte.setCode(id.toString().toLowerCase(Locale.ROOT));
        alerte.setDescription(AdmAlertData.DESCRIPTION_ALERTE);
        alerte.setMessage(enregistree.getAlertMessage());
        alerte.setGroupping(categorie != null ? categorie.getCode() : null);
        alerte.setAlertDate(enregistree.getAlertValidityDate());
        alerte.setRelatedKey(enregistree.getId());
        alerte.setRightKey(categorie != null ? categorie.getId().toString().toLowerCase(Locale.ROOT) : null);
        alerte.setActive(true);
        alerteRepository.save(alerte);
    }
}
