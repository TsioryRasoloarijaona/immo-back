package com.tsix.immocrm.services.administration.tiers;

import com.tsix.immocrm.dto.administration.tiers.LienTiersDto;
import com.tsix.immocrm.dto.administration.tiers.LienTiersRequest;
import com.tsix.immocrm.entities.cfg.CfgRelationType;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierContact;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.CfgTierContactRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.services.patrimoine.TracePatrimoine;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Liens de {@code CfgTierContact}, tout type de tiers : ces routes ne filtrent pas par famille. */
@Service
public class LiensTiersService {

    private final EntityManager em;
    private final CfgTierRepository tiers;
    private final CfgTierContactRepository liens;
    private final TracePatrimoine traces;

    public LiensTiersService(EntityManager em, CfgTierRepository tiers, CfgTierContactRepository liens,
                             TracePatrimoine traces) {
        this.em = em;
        this.tiers = tiers;
        this.liens = liens;
        this.traces = traces;
    }

    @Transactional(readOnly = true)
    public List<LienTiersDto> lister(UUID tiersId) {
        trouver(tiersId);
        return liens.findByCfgTierIdOrderByUserCreateDateTimeDesc(tiersId).stream().map(LiensTiersService::dto).toList();
    }

    @Transactional
    public LienTiersDto creer(UUID tiersId, LienTiersRequest corps) {
        CfgTier tier = trouver(tiersId);
        CfgTier contact = corps.contactId() == null ? null : em.find(CfgTier.class, corps.contactId());
        if (contact == null) {
            throw new InvalidRequestException("contactId : référence inconnue.");
        }
        CfgRelationType relation = relation(corps.relationTypeId());
        if (liens.existsByCfgTierIdAndCfgContactId(tiersId, contact.getId())) {
            throw new ConflictException("Ce contact est déjà lié à ce tiers.");
        }
        TracePatrimoine.Trace trace = traces.maintenant();
        CfgTierContact lien = new CfgTierContact();
        lien.setId(UUID.randomUUID());
        lien.setCfgTier(tier);
        lien.setCfgContact(contact);
        lien.setCfgRelationType(relation);
        lien.setUserCreate(trace.auteur());
        lien.setUserCreateDateTime(trace.le());
        em.persist(lien);
        return dto(lien);
    }

    @Transactional
    public void supprimer(UUID tiersId, UUID lienId) {
        liens.delete(liens.findByIdAndCfgTierId(lienId, tiersId)
                .orElseThrow(() -> new ResourceNotFoundException("Lien introuvable : " + lienId)));
    }

    private CfgTier trouver(UUID id) {
        return tiers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tiers introuvable : " + id));
    }

    private CfgRelationType relation(UUID id) {
        if (id == null) {
            return null;
        }
        CfgRelationType relation = em.find(CfgRelationType.class, id);
        if (relation == null) {
            throw new InvalidRequestException("relationTypeId : référence inconnue.");
        }
        return relation;
    }

    private static LienTiersDto dto(CfgTierContact l) {
        CfgRelationType relation = l.getCfgRelationType();
        CfgTier contact = l.getCfgContact();
        return new LienTiersDto(l.getId(), contact == null ? null : contact.getId(),
                contact == null ? null : contact.getDescription(), relation == null ? null : relation.getId(),
                relation == null ? null : relation.getDescription());
    }
}
