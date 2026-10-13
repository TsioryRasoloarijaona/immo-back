package com.tsix.immocrm.services.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Déclencheur du résumé à 09:00 et 14:00, heure de Casablanca ; désactivé sous la suite de tests. */
@Component
@ConditionalOnProperty(name = "app.notifications.resume.actif", havingValue = "true", matchIfMissing = true)
public class ResumeDemiJournee {

    private final ResumeNotificationsService resumes;

    public ResumeDemiJournee(ResumeNotificationsService resumes) {
        this.resumes = resumes;
    }

    @Scheduled(cron = "0 0 9,14 * * *", zone = "Africa/Casablanca")
    public void declencher() {
        resumes.envoyerResumes();
    }
}
