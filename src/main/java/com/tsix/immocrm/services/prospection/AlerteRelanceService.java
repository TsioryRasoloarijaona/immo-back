package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.entities.adm.AdmAlertData;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.repositories.adm.AdmAlertDataRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Cycle de vie d'une alerte de relance, tel que la trace d'audit d'OMRAN le montre : c'est la
 * prochaine interaction <b>porteuse d'une alerte</b> qui éteint la précédente, jamais le temps.
 * Voir {@code instructions/FEATURE-alerte-admalertdata.md}.
 */
@Service
public class AlerteRelanceService {

    private static final int LONGUEUR_MESSAGE = 1000;

    private final ComActionMessageRepository messageRepository;
    private final AdmAlertDataRepository alerteRepository;

    public AlerteRelanceService(ComActionMessageRepository messageRepository,
                                AdmAlertDataRepository alerteRepository) {
        this.messageRepository = messageRepository;
        this.alerteRepository = alerteRepository;
    }

    /**
     * Désarme les relances précédentes de la prospection et pose la nouvelle. À appeler
     * <b>après</b> le {@code save}, et seulement si {@code AlertValidityDate} est renseignée.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public AdmAlertData poserAlerte(ComActionMessage interaction) {
        if (interaction.getAlertValidityDate() == null || interaction.getComProspection() == null) {
            throw new IllegalArgumentException("poserAlerte exige une interaction de prospection datée d'une alerte");
        }
        UUID prospectionId = interaction.getComProspection().getId();
        return remplacerAlerte(interaction,
                messageRepository.findByComProspectionIdAndAlertValidityDateIsNotNullAndComDocumentIsNull(prospectionId),
                alerteRepository.alertesActivesDeLaProspection(prospectionId));
    }

    /**
     * GEN0005 sur une fiche article : la nouvelle alerte désarme celles des autres actions propres de l'article
     * (FEATURE-patrimoine-audit-actions.md §3). Même séquence que {@link #poserAlerte}.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public AdmAlertData poserAlerteArticle(ComActionMessage action) {
        if (action.getAlertValidityDate() == null || action.getStkItem() == null) {
            throw new IllegalArgumentException("poserAlerteArticle exige une action d'article datée d'une alerte");
        }
        UUID articleId = action.getStkItem().getId();
        return remplacerAlerte(action, messageRepository.relancesDeLArticle(articleId),
                alerteRepository.alertesActivesDeLArticle(articleId));
    }

    private AdmAlertData remplacerAlerte(ComActionMessage interaction, List<ComActionMessage> precedentes,
                                         List<AdmAlertData> actives) {
        // 1. WithoutRevival = 1 sur les autres relances — le prédicat exact du légataire. Entités
        //    une à une, pas de @Modifying JPQL : il échapperait au collecteur Hibernate qui
        //    rapatrie les écritures dans la copie de lecture.
        for (ComActionMessage m : precedentes) {
            if (!m.getId().equals(interaction.getId()) && !Boolean.TRUE.equals(m.getIsUnpaid())) {
                m.setWithoutRevival(true);
            }
        }
        messageRepository.saveAll(precedentes);

        // 2. Active = 0 sur leurs alertes. La nouvelle n'a pas encore de ligne : rien à exclure.
        desactiver(actives);

        // 3. La nouvelle alerte, au format constant des lignes OMRAN.
        AdmAlertData alerte = new AdmAlertData();
        UUID id = UUID.randomUUID();
        alerte.setId(id);
        alerte.setCode(id.toString().toLowerCase(Locale.ROOT));
        alerte.setDescription(AdmAlertData.DESCRIPTION_ALERTE);
        alerte.setMessage(tronquer(interaction.getAlertMessage()));
        alerte.setGroupping(AdmAlertData.GROUPE_PROSPECTION);
        alerte.setAlertDate(interaction.getAlertValidityDate());
        alerte.setRelatedKey(interaction.getId());
        alerte.setRightKey(AdmAlertData.DROIT_COMMUNICATION);
        alerte.setActive(true);
        return alerteRepository.save(alerte);
    }

    /**
     * Vente et archivage : les deux premiers ordres du légataire sans le troisième. L'UPDATE de
     * masse suffit pour les messages, que personne ne relit dans la transaction.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void desarmerToutes(UUID prospectionId) {
        messageRepository.desarmerRelancesDeLaProspection(prospectionId);
        desactiver(alerteRepository.alertesActivesDeLaProspection(prospectionId));
    }

    private void desactiver(List<AdmAlertData> actives) {
        for (AdmAlertData a : actives) {
            a.setActive(false);
        }
        alerteRepository.saveAll(actives);
    }

    private static String tronquer(String message) {
        if (message == null || message.length() <= LONGUEUR_MESSAGE) {
            return message;
        }
        return message.substring(0, LONGUEUR_MESSAGE);
    }
}
