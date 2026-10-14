package com.tsix.immocrm.services.dossier.workflow;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.str.StrIntervener;
import com.tsix.immocrm.repositories.str.StrIntervenerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Statuts de dossier qu'un utilisateur voit dans la liste ({@code StrStatus.GetStatusFilterQueryClauseForListing}) :
 * ceux dont un intervenant ({@code StrStatusIntervener}) correspond à son rôle ou à lui-même. Vide = aucune
 * restriction (Administrateur, ou aucun intervenant de statut paramétré). À appliquer par la liste des dossiers.
 */
@Service
@Transactional(readOnly = true)
public class StatutsVisibles {

    private final StrIntervenerRepository intervenerRepository;
    private final IntervenantCourant intervenantCourant;

    public StatutsVisibles(StrIntervenerRepository intervenerRepository, IntervenantCourant intervenantCourant) {
        this.intervenerRepository = intervenerRepository;
        this.intervenantCourant = intervenantCourant;
    }

    public Optional<Set<UUID>> pourUtilisateurCourant() {
        return pour(intervenantCourant.courant());
    }

    public Optional<Set<UUID>> pour(Intervenant utilisateur) {
        if (utilisateur.administrateur()) {
            return Optional.empty();
        }
        List<Object[]> lignes = intervenerRepository.deStatuts(StatutDossier.CONFIG_ENTITY_ID);
        if (lignes.isEmpty()) {
            return Optional.empty();
        }
        Set<UUID> visibles = new HashSet<>();
        for (Object[] ligne : lignes) {
            if (utilisateur.correspond((StrIntervener) ligne[1])) {
                visibles.add((UUID) ligne[0]);
            }
        }
        return Optional.of(Set.copyOf(visibles));
    }
}
