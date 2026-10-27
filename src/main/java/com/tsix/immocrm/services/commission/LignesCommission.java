package com.tsix.immocrm.services.commission;

import com.tsix.immocrm.domain.commission.TypeCommission;
import com.tsix.immocrm.dto.commission.LigneCommissionCalculeeDto;
import com.tsix.immocrm.entities.com.ComCommissionLine;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.com.ComCommissionLineRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Montant et suppression d'une ligne (R-X, C4, C5) : une ligne liée à un document à payer est figée. */
@Component
public class LignesCommission {

    private final CommissionsVisibles visibles;
    private final ComCommissionLineRepository lignes;
    private final VueLignesCommission vue;
    private final UtilisateurAffiche utilisateur;
    private final Clock clock;

    public LignesCommission(CommissionsVisibles visibles, ComCommissionLineRepository lignes, VueLignesCommission vue,
                            UtilisateurAffiche utilisateur, Clock clock) {
        this.visibles = visibles;
        this.lignes = lignes;
        this.vue = vue;
        this.utilisateur = utilisateur;
        this.clock = clock;
    }

    @Transactional
    public LigneCommissionCalculeeDto modifierMontant(UUID ligneId, BigDecimal montant) {
        ComCommissionLine l = visibles.ligne(ligneId);
        if (l.getBuyDocumentId() != null) {
            throw new ConflictException("vous ne pouvez pas modifier le montant d'une commission qui a un document à régler!");
        }
        boolean negatif = TypeCommission.negatif(l.getCommissionType());
        if (montant.signum() == 0 || (montant.signum() < 0) != negatif) {
            throw new InvalidRequestException("Le montant d'une commission de %s doit être %s."
                    .formatted(l.getCommissionType(), negatif ? "négatif" : "positif"));
        }
        l.setCommissionAmount(montant);
        l.setUserUpdate(utilisateur.courant().nom());
        l.setUserUpdateDateTime(LocalDateTime.now(clock));
        return vue.dtos(List.of(l)).get(0);
    }

    /** Tout ou rien : une ligne inconnue (404) ou documentée (409) et rien n'est supprimé. */
    @Transactional
    public void supprimer(List<UUID> ligneIds) {
        List<ComCommissionLine> aSupprimer = visibles.lignes(ligneIds);
        if (aSupprimer.stream().anyMatch(l -> l.getBuyDocumentId() != null)) {
            throw new ConflictException("Vous ne pouvez pas supprimer une ligne associée à un document !");
        }
        lignes.deleteAll(aSupprimer);
    }
}
