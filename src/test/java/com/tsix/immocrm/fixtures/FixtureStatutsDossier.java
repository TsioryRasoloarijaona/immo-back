package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Statuts de dossier de vente ({@code StrStatus} de l'entité ComFolder), créés s'ils manquent :
 * la création d'un dossier exige RESERVATION (500 sinon), comme OMRAN. Drapeaux et délai de
 * production : RESERVATION {@code Delay} 7, sorties {@code BoolField002}/{@code 003}, les autres
 * « transforme le prospect en client » ({@code BoolField011}).
 */
@Component
public class FixtureStatutsDossier {

    public static final int DELAI_RESERVATION = 7;

    private final StrStatusRepository strStatusRepository;

    public FixtureStatutsDossier(StrStatusRepository strStatusRepository) {
        this.strStatusRepository = strStatusRepository;
    }

    public StrStatus reservation() {
        return statut(StatutDossier.RESERVATION);
    }

    /** Statut de ce code, créé avec les drapeaux de production s'il n'existe pas encore. */
    public StrStatus statut(StatutDossier statut) {
        return Optional.ofNullable(strStatusRepository.findByStrConfigEntityIdAndCode(
                        StatutDossier.CONFIG_ENTITY_ID, statut.getCode()))
                .orElseGet(() -> strStatusRepository.save(nouveau(statut)));
    }

    private static StrStatus nouveau(StatutDossier statut) {
        StrStatus s = new StrStatus();
        s.setId(UUID.randomUUID());
        s.setCode(statut.getCode());
        s.setDescription(statut.getCode());
        s.setStrConfigEntityId(StatutDossier.CONFIG_ENTITY_ID);
        s.setAnnulation(statut == StatutDossier.ANNULATION);
        s.setResiliation(statut == StatutDossier.DESISTEMENT);
        s.setTransformeProspectEnClient(!statut.isSortie() && statut != StatutDossier.RESERVATION);
        s.setDelai(statut == StatutDossier.RESERVATION ? DELAI_RESERVATION : 0);
        return s;
    }
}
