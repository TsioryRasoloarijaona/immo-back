package com.tsix.immocrm.services.notification;

import com.tsix.immocrm.domain.RolesExemptesPerimetre;
import com.tsix.immocrm.domain.TypeNotification;
import com.tsix.immocrm.dto.notification.EvenementNotificationDto;
import com.tsix.immocrm.dto.notification.NonLuesDto;
import com.tsix.immocrm.dto.notification.NotificationDto;
import com.tsix.immocrm.dto.notification.NotificationsDto;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppNotification;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.app.AppNotificationRepository;
import com.tsix.immocrm.services.equipe.EquipeCommercialeService;
import com.tsix.immocrm.services.equipe.MembreEquipe;
import com.tsix.immocrm.services.equipe.RolesEquipe;
import com.tsix.immocrm.services.notification.LeadsAttribues.Lead;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Notifications de la cloche (FEATURE-repartition-notifications.md §3.3). Lectures et marquages : compte appelant
 * seulement. Créations : appelées après commit de l'écriture métier ; l'acteur ne se notifie jamais lui-même.
 */
@Service
@Transactional(readOnly = true)
public class NotificationService {

    static final int TAILLE_MAX = 100;
    private static final String COMPTE_INCONNU = "Compte applicatif non identifiable.";

    private final AppNotificationRepository notificationRepository;
    private final AdmUserRepository userRepository;
    private final EquipeCommercialeService equipeService;
    private final Clock clock;

    public NotificationService(AppNotificationRepository notificationRepository, AdmUserRepository userRepository,
                               EquipeCommercialeService equipeService, Clock clock) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.equipeService = equipeService;
        this.clock = clock;
    }

    /** Une notification à écrire pour un compte. */
    public record NouvelleNotification(UUID admUserId, TypeNotification type, String titre, String message,
                                       @Nullable String lien) {
    }

    // ---------------------------------------------------------------- lectures du compte appelant

    public NotificationsDto lister(int page, int taille) {
        if (page < 0 || taille < 1 || taille > TAILLE_MAX) {
            throw new InvalidRequestException("page ≥ 0 et 1 ≤ size ≤ " + TAILLE_MAX);
        }
        UUID compte = appelant();
        Page<AppNotification> lues = notificationRepository.findByAdmUserIdOrderByCreeLeDescIdAsc(compte,
                PageRequest.of(page, taille));
        return new NotificationsDto(lues.getContent().stream().map(NotificationService::versDto).toList(),
                page, taille, lues.getTotalElements(), notificationRepository.countByAdmUserIdAndLueLeIsNull(compte));
    }

    /** 404 si la notification n'existe pas ou n'est pas au compte appelant (jamais 403 : rien à révéler). */
    @Transactional
    public NonLuesDto marquerLue(UUID id) {
        UUID compte = appelant();
        AppNotification n = notificationRepository.findByIdAndAdmUserId(id, compte)
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable: " + id));
        if (n.getLueLe() == null) {
            n.setLueLe(LocalDateTime.now(clock));
            notificationRepository.save(n);
        }
        return new NonLuesDto(notificationRepository.countByAdmUserIdAndLueLeIsNull(compte));
    }

    @Transactional
    public NonLuesDto marquerToutLu() {
        UUID compte = appelant();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        List<AppNotification> nonLues = notificationRepository.findByAdmUserIdAndLueLeIsNull(compte);
        nonLues.forEach(n -> n.setLueLe(maintenant));
        notificationRepository.saveAll(nonLues);
        return new NonLuesDto(0);
    }

    /** 403 sans compte applicatif (clé d'API, principal de test) : une notification est toujours à un compte. */
    public static UUID appelant() {
        return PerimetreCommercialService.admUserIdCourant().orElseThrow(() -> new ForbiddenException(COMPTE_INCONNU));
    }

    // ---------------------------------------------------------------- créations

    /** Écrit les notifications ; renvoie les évènements à pousser sur le flux une fois la transaction validée. */
    @Transactional
    public List<EvenementNotificationDto> creer(List<NouvelleNotification> nouvelles) {
        LocalDateTime maintenant = LocalDateTime.now(clock);
        List<AppNotification> ecrites = new ArrayList<>();
        for (NouvelleNotification n : nouvelles) {
            AppNotification ligne = new AppNotification();
            ligne.setId(UUID.randomUUID());
            ligne.setAdmUser(userRepository.getReferenceById(n.admUserId()));
            ligne.setType(n.type().name());
            ligne.setTitre(tronquer(n.titre(), 200));
            ligne.setMessage(tronquer(n.message(), 1000));
            ligne.setLien(n.lien());
            ligne.setCreeLe(maintenant);
            ecrites.add(notificationRepository.save(ligne));
        }
        notificationRepository.flush();
        Map<UUID, Long> nonLues = new LinkedHashMap<>();
        return ecrites.stream()
                .map(e -> {
                    UUID compte = e.getAdmUser().getId();
                    long total = nonLues.computeIfAbsent(compte, notificationRepository::countByAdmUserIdAndLueLeIsNull);
                    return new EvenementNotificationDto(compte, versDto(e), total);
                })
                .toList();
    }

    /** {@code LEADS_ATTRIBUES} regroupées par compte, et {@code LEADS_A_DISTRIBUER} aux chefs du projet. */
    @Transactional
    public List<EvenementNotificationDto> notifierAttributions(LeadsAttribues evenement) {
        List<NouvelleNotification> nouvelles = new ArrayList<>(attribues(evenement));
        nouvelles.addAll(aDistribuer(evenement));
        return nouvelles.isEmpty() ? List.of() : creer(nouvelles);
    }

    private List<NouvelleNotification> attribues(LeadsAttribues evenement) {
        Map<UUID, List<Lead>> parCommercial = evenement.attribues().stream()
                .filter(l -> l.commercialId() != null)
                .collect(Collectors.groupingBy(Lead::commercialId, LinkedHashMap::new, Collectors.toList()));
        if (parCommercial.isEmpty()) {
            return List.of();
        }
        List<NouvelleNotification> nouvelles = new ArrayList<>();
        for (AdmUser compte : userRepository.findByCfgEmployeeIdIn(parCommercial.keySet())) {
            if (Boolean.TRUE.equals(compte.getDisabled()) || compte.getId().equals(evenement.acteurId())) {
                continue;
            }
            List<Lead> leads = parCommercial.get(compte.getCfgEmployee().getId());
            nouvelles.add(MessagesNotification.leadsAttribues(compte.getId(), leads));
        }
        return nouvelles;
    }

    /** Projet : chefs habilités ; sans projet : exemptés. Un message par compte, avec son nombre de leads. */
    private List<NouvelleNotification> aDistribuer(LeadsAttribues evenement) {
        if (evenement.aDistribuer().isEmpty()) {
            return List.of();
        }
        List<MembreEquipe> chefs = equipeService.comptesActifs(roles -> roles.contains(RolesEquipe.CHEF));
        List<MembreEquipe> exemptes = equipeService.comptesActifs(RolesExemptesPerimetre::exempte);
        Map<UUID, Integer> parCompte = new LinkedHashMap<>();
        for (Lead lead : evenement.aDistribuer()) {
            List<MembreEquipe> destinataires = lead.projetId() == null ? exemptes
                    : chefs.stream().filter(c -> c.projetIds().contains(lead.projetId())).toList();
            destinataires.stream()
                    .map(MembreEquipe::admUserId)
                    .filter(id -> !id.equals(evenement.acteurId()))
                    .forEach(id -> parCompte.merge(id, 1, Integer::sum));
        }
        return parCompte.entrySet().stream()
                .map(e -> MessagesNotification.leadsADistribuer(e.getKey(), e.getValue()))
                .toList();
    }

    static NotificationDto versDto(AppNotification n) {
        return new NotificationDto(n.getId(), TypeNotification.valueOf(n.getType()), n.getTitre(), n.getMessage(),
                n.getLien(), n.getCreeLe(), n.getLueLe());
    }

    private static String tronquer(String texte, int max) {
        return texte.length() > max ? texte.substring(0, max - 1) + "…" : texte;
    }
}
