package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.patrimoine.EntiteFinalisationDto;
import com.tsix.immocrm.dto.patrimoine.LigneFinalisationDto;
import com.tsix.immocrm.dto.patrimoine.LignesTrancheDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.CfgTrancheFolderEntity;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.cfg.TrancheVisibleRepository;
import com.tsix.immocrm.services.patrimoine.ReconciliationLignes.Onglet;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Onglet Finalisations de la tranche (FEATURE-patrimoine-tranche-regles.md §1, §11.7) : une ligne par type de dossier. */
@Service
@Transactional(readOnly = true)
public class FinalisationsTrancheService {

    private static final String LIGNES = "SELECT l FROM CfgTrancheFolderEntity l LEFT JOIN l.strFolderEntity e "
            + "WHERE l.cfgTranche.id = :id ORDER BY e.code";
    private static final String ENTITE = "entiteDossierId";
    private static final Onglet<CfgTrancheFolderEntity, LigneFinalisationDto> ONGLET = new Onglet<>(LIGNES,
            CfgTrancheFolderEntity.class, CfgTrancheFolderEntity::new, LigneFinalisationDto::entiteDossierId, ENTITE, true);
    /** Types de dossier de la liste déroulante OMRAN. */
    private static final List<String> ENTITES = List.of("ComFolder", "PrmFolderSale", "RomFolderSale");

    private final TrancheVisibleRepository tranches;
    private final ReconciliationLignes reconciliation;
    private final EntityManager em;

    public FinalisationsTrancheService(TrancheVisibleRepository tranches, ReconciliationLignes reconciliation,
                                       EntityManager em) {
        this.tranches = tranches;
        this.reconciliation = reconciliation;
        this.em = em;
    }

    public LignesTrancheDto<LigneFinalisationDto> finalisations(UUID trancheId) {
        return lire(tranches.exigerVisible(trancheId));
    }

    public List<EntiteFinalisationDto> entitesFinalisation() {
        return em.createQuery("SELECT e FROM StrEntity e WHERE e.code IN :codes ORDER BY e.code", StrEntity.class)
                .setParameter("codes", ENTITES).getResultList().stream()
                .map(e -> new EntiteFinalisationDto(e.getId(), e.getCode(), e.getDescription(), statuts(e.getId())))
                .toList();
    }

    @Transactional
    public LignesTrancheDto<LigneFinalisationDto> enregistrerFinalisations(UUID trancheId,
                                                                           LignesTrancheDto<LigneFinalisationDto> corps) {
        CfgTranche tranche = tranches.exigerVisible(trancheId);
        reconciliation.reconcilier(tranche, corps, ONGLET, (e, l, p) -> {
            StrEntity entite = reconciliation.reference(StrEntity.class, l.entiteDossierId(), p.champ(ENTITE));
            String champ1 = p.champ("statutFinalisation1Id");
            if (l.statutFinalisation1Id() == null) {
                throw new InvalidRequestException(champ1 + " est obligatoire.");
            }
            e.setStrFolderEntity(entite);
            e.setStrStatusDelivery1(statut(l.statutFinalisation1Id(), entite, champ1));
            e.setStrStatusDelivery2(statut(l.statutFinalisation2Id(), entite, p.champ("statutFinalisation2Id")));
        });
        return lire(tranche);
    }

    /** Statut facultatif ; renseigné, il appartient au type de dossier choisi. */
    private StrStatus statut(UUID id, StrEntity entite, String champ) {
        StrStatus statut = reconciliation.referenceOuNull(StrStatus.class, id, champ);
        if (statut != null && !entite.getId().equals(statut.getStrConfigEntityId())) {
            throw new InvalidRequestException(champ + " : statut d'un autre type de dossier.");
        }
        return statut;
    }

    private List<ReferentielItemDto> statuts(UUID entiteId) {
        return em.createQuery("SELECT s FROM StrStatus s WHERE s.strConfigEntityId = :id ORDER BY s.sorting, s.code",
                        StrStatus.class).setParameter("id", entiteId).getResultList().stream()
                .map(s -> new ReferentielItemDto(s.getId(), s.getCode(), s.getDescription(), s.getSorting())).toList();
    }

    private LignesTrancheDto<LigneFinalisationDto> lire(CfgTranche tranche) {
        return new LignesTrancheDto<>(reconciliation.lignes(tranche, LIGNES, CfgTrancheFolderEntity.class).stream()
                .map(l -> new LigneFinalisationDto(l.getId(), Champs.de(l.getStrFolderEntity(), StrEntity::getId),
                        Champs.de(l.getStrStatusDelivery1(), StrStatus::getId),
                        Champs.de(l.getStrStatusDelivery2(), StrStatus::getId)))
                .toList());
    }
}
