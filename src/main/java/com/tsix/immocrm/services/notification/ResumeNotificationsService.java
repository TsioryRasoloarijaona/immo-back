package com.tsix.immocrm.services.notification;

import com.tsix.immocrm.domain.TypeNotification;
import com.tsix.immocrm.dto.notification.EvenementNotificationDto;
import com.tsix.immocrm.dto.parcours.JourneeDto;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.equipe.EquipeCommercialeService;
import com.tsix.immocrm.services.equipe.MembreEquipe;
import com.tsix.immocrm.services.equipe.RolesEquipe;
import com.tsix.immocrm.services.notification.NotificationService.NouvelleNotification;
import com.tsix.immocrm.services.parcours.JourneeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Résumé de la demi-journée : pour chaque commercial actif, les quatre totaux de {@code GET /api/journee}, calculés
 * par le même service sous son identité (même périmètre). Sans transaction propre : chaque lecture a la sienne.
 */
@Service
public class ResumeNotificationsService {

    static final String TITRE = "Votre point de la demi-journée";

    private static final Logger log = LoggerFactory.getLogger(ResumeNotificationsService.class);

    private final EquipeCommercialeService equipeService;
    private final JourneeService journeeService;
    private final NotificationService notificationService;
    private final FluxNotifications flux;

    public ResumeNotificationsService(EquipeCommercialeService equipeService, JourneeService journeeService,
                                      NotificationService notificationService, FluxNotifications flux) {
        this.equipeService = equipeService;
        this.journeeService = journeeService;
        this.notificationService = notificationService;
        this.flux = flux;
    }

    /** Une notification par compte qui a quelque chose ; rien pour un compte dont tout est à zéro. */
    public List<EvenementNotificationDto> envoyerResumes() {
        List<NouvelleNotification> nouvelles = new ArrayList<>();
        for (MembreEquipe compte : equipeService.comptesActifs(RolesEquipe::estCommercialOuChef)) {
            if (compte.employeId() == null) {
                continue;
            }
            try {
                resume(compte).ifPresent(nouvelles::add);
            } catch (RuntimeException e) {
                log.error("Résumé non calculé pour le compte {}.", compte.code(), e);
            }
        }
        if (nouvelles.isEmpty()) {
            return List.of();
        }
        List<EvenementNotificationDto> evenements = notificationService.creer(nouvelles);
        flux.diffuser(evenements);
        return evenements;
    }

    private Optional<NouvelleNotification> resume(MembreEquipe compte) {
        JourneeDto journee = sousIdentite(compte);
        String message = MessagesNotification.resume(journee.nouveauxLeads().total(),
                journee.relancesDuJour().total(), journee.visitesDuJour().total(), journee.enRetard().total());
        return Optional.ofNullable(message)
                .map(m -> new NouvelleNotification(compte.admUserId(), TypeNotification.RESUME_DEMI_JOURNEE,
                        TITRE, m, "/journee"));
    }

    /** {@code GET /api/journee} lit le compte appelant : on le pose le temps du calcul, puis on rend l'ancien. */
    private JourneeDto sousIdentite(MembreEquipe compte) {
        SecurityContext precedent = SecurityContextHolder.getContext();
        SecurityContext contexte = SecurityContextHolder.createEmptyContext();
        AuthenticatedUser principal = new AuthenticatedUser(compte.admUserId(), compte.code(), compte.employeId(),
                false, true, 0);
        contexte.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        SecurityContextHolder.setContext(contexte);
        try {
            return journeeService.journee(null);
        } finally {
            SecurityContextHolder.setContext(precedent);
        }
    }
}
