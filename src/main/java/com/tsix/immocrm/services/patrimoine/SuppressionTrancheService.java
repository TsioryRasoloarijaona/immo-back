package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileRepository;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Suppression d'une tranche (FEATURE-patrimoine-tranche-onglets.md §4) : 409 tant qu'une clé NO_ACTION la référence ;
 * sinon, dans la même transaction, ses terrains (NO_ACTION, retirés comme OMRAN), ses lignes enfants mappées (CASCADE en
 * production, supprimées ici pour l'audit, la copie de lecture et les bases sans clé étrangère), PJ et notes.
 */
@Service
public class SuppressionTrancheService {

    private final TrancheVisible trancheVisible;
    private final CfgTrancheRepository trancheRepository;
    private final ReferencesBloquantes referencesBloquantes;
    private final EnfantsTranche enfants;
    private final AdmUserHabilitationRepository habilitationRepository;
    private final AdmAttachedFileRepository pieceJointeRepository;

    SuppressionTrancheService(TrancheVisible trancheVisible, CfgTrancheRepository trancheRepository,
                              ReferencesBloquantes referencesBloquantes, EnfantsTranche enfants,
                              AdmUserHabilitationRepository habilitationRepository,
                              AdmAttachedFileRepository pieceJointeRepository) {
        this.trancheVisible = trancheVisible;
        this.trancheRepository = trancheRepository;
        this.referencesBloquantes = referencesBloquantes;
        this.enfants = enfants;
        this.habilitationRepository = habilitationRepository;
        this.pieceJointeRepository = pieceJointeRepository;
    }

    @Transactional
    public void supprimer(UUID id) {
        CfgTranche tranche = trancheVisible.trouver(id);
        List<String> bloquantes = referencesBloquantes.compter(ReferencesBloquantes.TRANCHE, id);
        if (!bloquantes.isEmpty()) {
            throw new ConflictException("La tranche « %s » ne peut pas être supprimée : elle est référencée par %s."
                    .formatted(tranche.getCode(), String.join(", ", bloquantes)));
        }
        enfants.terrains().deleteAll(enfants.terrains().findByCfgTrancheIdOrderBySortingAsc(id));
        enfants.etages().deleteAll(enfants.etages().findByCfgTrancheId(id));
        enfants.planchers().deleteAll(enfants.planchers().findByCfgTrancheId(id));
        enfants.frais().deleteAll(enfants.frais().findByCfgTrancheIdOrderBySortingAsc(id));
        enfants.seuils().deleteAll(enfants.seuils().findByCfgTrancheId(id));
        habilitationRepository.deleteAll(habilitationRepository.findByCfgTrancheId(id));
        pieceJointeRepository.deleteAll(pieceJointeRepository.findByCategoryId(id));
        referencesBloquantes.supprimerMessages(id);
        trancheRepository.delete(tranche);
    }
}
