package com.tsix.immocrm.services.prospect;

import com.tsix.immocrm.dto.prospect.CreerProspectRequest;
import com.tsix.immocrm.dto.prospect.ModifierProspectRequest;
import com.tsix.immocrm.dto.prospect.ProspectDetailDto;
import com.tsix.immocrm.dto.prospect.ProspectListItemDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierKind;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.CfgTierKindRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProspectService {

    private final CfgTierRepository tierRepository;
    private final CfgTierTypeRepository tierTypeRepository;
    private final CfgTierKindRepository tierKindRepository;

    public ProspectService(CfgTierRepository tierRepository,
                            CfgTierTypeRepository tierTypeRepository,
                            CfgTierKindRepository tierKindRepository) {
        this.tierRepository = tierRepository;
        this.tierTypeRepository = tierTypeRepository;
        this.tierKindRepository = tierKindRepository;
    }

    /**
     * @param search free-text match on code/nom/prenom/raisonSociale/email
     * @param type   {@code CfgTierType.code} to filter by
     */
    public List<ProspectListItemDto> findAll(String search, String type) {
        Specification<CfgTier> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("isProspect")));
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern),
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("socialReason")), pattern),
                        cb.like(cb.lower(root.get("eMail")), pattern)
                ));
            }
            if (StringUtils.hasText(type)) {
                predicates.add(cb.equal(cb.lower(root.get("cfgTierType").get("code")), type.toLowerCase()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return tierRepository.findAll(spec).stream().map(ProspectService::toListItem).toList();
    }

    public ProspectDetailDto findById(UUID id) {
        return tierRepository.findById(id)
                .map(ProspectService::toDetail)
                .orElseThrow(() -> new ResourceNotFoundException("Prospect introuvable: " + id));
    }

    @Transactional
    public ProspectDetailDto create(CreerProspectRequest request) {
        return toDetail(createEntity(request));
    }

    /**
     * Same creation logic as {@link #create}, but returns the persisted entity — used by
     * {@link com.tsix.immocrm.services.prospection.ProspectionService} to link a newly
     * created prospect back onto a pré-prospection (POST /api/pre-prospections/{id}/creer-prospect).
     */
    @Transactional
    public CfgTier createEntity(CreerProspectRequest request) {
        if (!StringUtils.hasText(request.nom()) && !StringUtils.hasText(request.raisonSociale())) {
            throw new InvalidRequestException("nom ou raisonSociale est requis");
        }

        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setIsProspect(true);
        applyFields(tier, request.prenom(), request.nom(), request.raisonSociale(), request.telephone(),
                request.mobile(), request.email(), request.adresse(), request.ville(), request.codePostal(),
                request.dateNaissance(), request.typeId(), request.kindId(), request.commercialId());

        return tierRepository.save(tier);
    }

    @Transactional
    public ProspectDetailDto update(UUID id, ModifierProspectRequest request) {
        CfgTier tier = tierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prospect introuvable: " + id));

        applyFields(tier, request.prenom(), request.nom(), request.raisonSociale(), request.telephone(),
                request.mobile(), request.email(), request.adresse(), request.ville(), request.codePostal(),
                request.dateNaissance(), request.typeId(), request.kindId(), request.commercialId());

        return toDetail(tierRepository.save(tier));
    }

    private void applyFields(CfgTier tier, String prenom, String nom, String raisonSociale, String telephone,
                              String mobile, String email, String adresse, String ville, String codePostal,
                              java.time.LocalDateTime dateNaissance, UUID typeId, UUID kindId, UUID commercialId) {
        if (prenom != null) tier.setFirstName(prenom);
        if (nom != null) tier.setLastName(nom);
        if (raisonSociale != null) tier.setSocialReason(raisonSociale);
        if (telephone != null) tier.setPhone(telephone);
        if (mobile != null) tier.setMobile(mobile);
        if (email != null) tier.setEMail(email);
        if (adresse != null) tier.setAddress(adresse);
        if (ville != null) tier.setCity(ville);
        if (codePostal != null) tier.setZipCode(codePostal);
        if (dateNaissance != null) tier.setBirthDate(dateNaissance);
        if (typeId != null) {
            CfgTierType type = tierTypeRepository.findById(typeId)
                    .orElseThrow(() -> new InvalidRequestException("CfgTierType introuvable: " + typeId));
            tier.setCfgTierType(type);
        }
        if (kindId != null) {
            CfgTierKind kind = tierKindRepository.findById(kindId)
                    .orElseThrow(() -> new InvalidRequestException("CfgTierKind introuvable: " + kindId));
            tier.setCfgTierKind(kind);
        }
        if (commercialId != null) {
            CfgTier commercial = tierRepository.findById(commercialId)
                    .orElseThrow(() -> new InvalidRequestException("Commercial (CfgTier) introuvable: " + commercialId));
            tier.setCfgCommercial(commercial);
        }
    }

    private static ProspectListItemDto toListItem(CfgTier tier) {
        return new ProspectListItemDto(
                tier.getId(),
                tier.getCode(),
                tier.getFirstName(),
                tier.getLastName(),
                tier.getSocialReason(),
                tier.getPhone(),
                tier.getMobile(),
                tier.getEMail(),
                tier.getCity(),
                tier.getCfgTierType() != null ? tier.getCfgTierType().getCode() : null
        );
    }

    private static ProspectDetailDto toDetail(CfgTier tier) {
        return new ProspectDetailDto(
                tier.getId(),
                tier.getCode(),
                tier.getFirstName(),
                tier.getLastName(),
                tier.getSocialReason(),
                tier.getPhone(),
                tier.getPhone1(),
                tier.getPhone2(),
                tier.getMobile(),
                tier.getFax(),
                tier.getEMail(),
                tier.getWebSite(),
                tier.getAddress(),
                tier.getAddress2(),
                tier.getAddress3(),
                tier.getZipCode(),
                tier.getCity(),
                tier.getBirthDate(),
                tier.getBirthPlace(),
                tier.getNbrChildren(),
                tier.getQuality(),
                tier.getNotes(),
                tier.getIce(),
                tier.getIsOldCustomer(),
                tier.getCfgTierType() != null ? tier.getCfgTierType().getId() : null,
                tier.getCfgTierType() != null ? tier.getCfgTierType().getDescription() : null,
                tier.getCfgTierKind() != null ? tier.getCfgTierKind().getId() : null,
                tier.getCfgTierKind() != null ? tier.getCfgTierKind().getDescription() : null,
                tier.getCfgCommercial() != null ? tier.getCfgCommercial().getId() : null,
                tier.getCfgCommercial() != null
                        ? ((tier.getCfgCommercial().getFirstName() != null ? tier.getCfgCommercial().getFirstName() + " " : "")
                        + (tier.getCfgCommercial().getLastName() != null ? tier.getCfgCommercial().getLastName() : "")).trim()
                        : null
        );
    }
}
