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
 * Cycle de vie d'une alerte de relance, <b>tel que le logiciel de bureau le pratique</b>.
 *
 * <p>Relevé dans sa trace d'audit SQL ({@code C:\backup\*.sqlaudit}, 11 occurrences entre le
 * 11/09 et le 26/09/2026, toutes identiques) et rejoué en direct le 26/09 sur la prospection
 * {@code 26/05941} : trois notes saisies à 14:33 (alerte au 26/09), 14:35 (sans alerte) et 14:36
 * (alerte au 27/09). Voir {@code instructions/FEATURE-alerte-admalertdata.md}.
 *
 * <h2>La règle, en une phrase</h2>
 *
 * <p><b>C'est la prochaine interaction porteuse d'une date d'alerte qui éteint la précédente.</b>
 * Ni la date de l'alerte, ni une interaction sans alerte n'y changent rien : la note de 14:35 a
 * laissé l'alerte de 14:33 active, celle de 14:36 l'a éteinte. La base de production le confirme
 * à l'échelle — 63 alertes restent actives malgré une action postérieure sans alerte, 8
 * seulement quand l'action postérieure en portait une. Aucun processus n'éteint une alerte par
 * le temps : 3 666 alertes échues sans action derrière sont toujours affichées.
 *
 * <h2>Les trois ordres du légataire, dans l'ordre</h2>
 *
 * <ol>
 *   <li>{@code UPDATE ComActionMessage SET WithoutRevival = 1 WHERE AlertValidityDate IS NOT NULL
 *       AND ComDocumentId IS NULL AND ComProspectionId = … AND IsUnpaid = 0 AND PKey <> <nouvelle>}</li>
 *   <li>{@code UPDATE AdmAlertData SET Active = 0} sur les alertes des interactions de la
 *       prospection sans statut de dossier ni échéance ;</li>
 *   <li>{@code INSERT INTO AdmAlertData} pour la nouvelle interaction, {@code Active = 1}.</li>
 * </ol>
 *
 * <p>Les deux drapeaux vont ensemble : {@code Active = 0} et {@code WithoutRevival = 1}
 * coïncident sur 33 416 des 33 530 alertes inactives de la production. Et c'est bien
 * {@code AdmAlertData} que l'écran lit — {@code ComSaleProspectionView.LastActiveAlertDate} se
 * calcule sur {@code Active = 1}. Une interaction à alerte sans ligne ici est invisible du
 * logiciel de bureau : c'était le cas des 12 écrites par cette API avant ce service.
 *
 * <h2>Pourquoi des entités et pas des {@code UPDATE} de masse</h2>
 *
 * <p>Un {@code @Modifying} JPQL échappe au collecteur Hibernate qui rapatrie chaque écriture dans
 * la copie de lecture avec la transaction. Modifier les lignes une à une coûte quelques
 * requêtes (une prospection porte rarement plus d'une alerte active) et rend l'extinction
 * visible immédiatement, sans attendre le cycle de synchronisation. Le désarmement global
 * (vente, archivage) garde le {@code UPDATE} historique pour les messages — personne ne relit
 * {@code WithoutRevival} dans la transaction — mais passe par les entités pour les alertes.
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
     * Une interaction porteuse d'une date d'alerte vient d'être enregistrée : désarme les
     * relances précédentes de sa prospection et pose la sienne. À appeler <b>après</b> le
     * {@code save} de l'interaction et seulement si {@code AlertValidityDate} est renseignée —
     * une interaction sans alerte ne touche à rien, comme dans le logiciel de bureau.
     *
     * @return la ligne {@code AdmAlertData} créée, active.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public AdmAlertData poserAlerte(ComActionMessage interaction) {
        if (interaction.getAlertValidityDate() == null || interaction.getComProspection() == null) {
            throw new IllegalArgumentException("poserAlerte exige une interaction de prospection datée d'une alerte");
        }
        UUID prospectionId = interaction.getComProspection().getId();

        // 1. WithoutRevival = 1 sur les autres relances de la prospection — le prédicat exact du
        //    légataire, la nouvelle interaction exclue par sa clé.
        List<ComActionMessage> precedentes = messageRepository
                .findByComProspectionIdAndAlertValidityDateIsNotNullAndComDocumentIsNull(prospectionId);
        for (ComActionMessage m : precedentes) {
            if (!m.getId().equals(interaction.getId()) && !Boolean.TRUE.equals(m.getIsUnpaid())) {
                m.setWithoutRevival(true);
            }
        }
        messageRepository.saveAll(precedentes);

        // 2. Active = 0 sur leurs alertes. La nouvelle n'a pas encore de ligne : rien à exclure.
        desactiverAlertes(prospectionId);

        // 3. La nouvelle alerte, au format constant relevé sur 39 599 lignes.
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
     * Désarme <b>toutes</b> les relances d'une prospection — transformation en vente et
     * archivage, où le légataire envoie les deux premiers ordres sans le troisième.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void desarmerToutes(UUID prospectionId) {
        messageRepository.desarmerRelancesDeLaProspection(prospectionId);
        desactiverAlertes(prospectionId);
    }

    private void desactiverAlertes(UUID prospectionId) {
        List<AdmAlertData> actives = alerteRepository.alertesActivesDeLaProspection(prospectionId);
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
