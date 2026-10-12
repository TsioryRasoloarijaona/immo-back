package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.domain.TypeActionRecente;
import com.tsix.immocrm.entities.str.StrHistory;
import com.tsix.immocrm.repositories.str.StrHistoryRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/** Ligne {@code StrHistory} d'une prospection, partagée par {@link ProspectionService} et l'écran « Mon équipe ». */
@Component
public class HistoriqueProspection {

    private static final int LONGUEUR_MAX = 255;

    private final StrHistoryRepository historyRepository;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    public HistoriqueProspection(StrHistoryRepository historyRepository, UtilisateurAffiche utilisateurAffiche,
                                 Clock clock) {
        this.historyRepository = historyRepository;
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
    }

    /** Sans utilisateur authentifié (formulaire public, import), auteur NULL : pas une erreur. */
    @Transactional
    public void enregistrerHistorique(UUID prospectionId, TypeActionRecente type, String suite) {
        UtilisateurAffiche.Auteur auteur = utilisateurAffiche.courant();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        String description = type.description(suite);
        StrHistory historique = new StrHistory();
        historique.setId(UUID.randomUUID());
        historique.setDocDate(maintenant);
        historique.setUserDate(maintenant);
        historique.setObjectId(prospectionId);
        historique.setDescription(description.length() > LONGUEUR_MAX ? description.substring(0, LONGUEUR_MAX) : description);
        historique.setAdmUserId(auteur.compteId());
        historique.setAdmUserDescription(auteur.nom());
        historyRepository.save(historique);
    }
}
