package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.repositories.adm.AdmSequenceRepository;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Suppression d'une entité juridique (FEATURE-entites-juridiques.md §2) : 409 tant qu'une clé étrangère NO_ACTION la
 * référence ; sinon séquences, habilitations et répartition supprimées explicitement (les bases de test n'ont pas les
 * CASCADE de production), le reste des CASCADE par la base.
 */
@Service
public class SuppressionEntiteService {

    private final CfgCompanyRepository repository;
    private final EntiteJuridiqueVisible visible;
    private final ReferencesBloquantes referencesBloquantes;
    private final AdmSequenceRepository sequenceRepository;
    private final AdmUserHabilitationRepository habilitationRepository;
    private final RepartitionEntite repartition;

    public SuppressionEntiteService(CfgCompanyRepository repository, EntiteJuridiqueVisible visible,
                                    ReferencesBloquantes referencesBloquantes, AdmSequenceRepository sequenceRepository,
                                    AdmUserHabilitationRepository habilitationRepository,
                                    RepartitionEntite repartition) {
        this.repository = repository;
        this.visible = visible;
        this.referencesBloquantes = referencesBloquantes;
        this.sequenceRepository = sequenceRepository;
        this.habilitationRepository = habilitationRepository;
        this.repartition = repartition;
    }

    @Transactional
    public void supprimer(UUID id) {
        CfgCompany entite = visible.trouver(id);
        List<String> bloquantes = referencesBloquantes.compter(ReferencesBloquantes.ENTITE, id);
        if (!bloquantes.isEmpty()) {
            throw new ConflictException("L'entité « %s » ne peut pas être supprimée : elle est référencée par %s."
                    .formatted(entite.getCode(), String.join(", ", bloquantes)));
        }
        sequenceRepository.deleteAll(sequenceRepository.findByCfgCompanyId(id));
        habilitationRepository.deleteAll(habilitationRepository.findByCfgCompanyId(id));
        repartition.supprimerTout(id);
        repository.delete(entite);
    }
}
