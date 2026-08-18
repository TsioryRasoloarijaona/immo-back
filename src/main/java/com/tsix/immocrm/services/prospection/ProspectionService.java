package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.dto.prospect.CreerProspectRequest;
import com.tsix.immocrm.dto.prospection.ArchiverProspectionRequest;
import com.tsix.immocrm.dto.prospection.ChangerEtapeRequest;
import com.tsix.immocrm.dto.prospection.CreerInteractionRequest;
import com.tsix.immocrm.dto.prospection.InteractionDto;
import com.tsix.immocrm.dto.prospection.ProspectionDetailDto;
import com.tsix.immocrm.dto.prospection.ProspectionListItemDto;
import com.tsix.immocrm.dto.prospection.TransformerProspectionRequest;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionClosingReason;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComProspectionClosingReasonRepository;
import com.tsix.immocrm.repositories.com.ComProspectionKindRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.services.prospect.ProspectService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProspectionService {

    private final ComProspectionRepository prospectionRepository;
    private final ProspectService prospectService;
    private final CfgTierRepository tierRepository;
    private final ComProspectionKindRepository prospectionKindRepository;
    private final ComProspectionClosingReasonRepository closingReasonRepository;
    private final ComActionMessageRepository actionMessageRepository;

    public ProspectionService(ComProspectionRepository prospectionRepository, ProspectService prospectService,
                               CfgTierRepository tierRepository, ComProspectionKindRepository prospectionKindRepository,
                               ComProspectionClosingReasonRepository closingReasonRepository,
                               ComActionMessageRepository actionMessageRepository) {
        this.prospectionRepository = prospectionRepository;
        this.prospectService = prospectService;
        this.tierRepository = tierRepository;
        this.prospectionKindRepository = prospectionKindRepository;
        this.closingReasonRepository = closingReasonRepository;
        this.actionMessageRepository = actionMessageRepository;
    }

    /**
     * {@code GET /api/prospections}. Excludes pré-prospections (CfgProspectId IS NULL AND
     * CfgCommercialId IS NULL) — those are exposed separately by {@link #findPreProspections()}
     * to avoid the two endpoints returning overlapping rows.
     *
     * @param etape      {@code ComProspectionKind.code} filter ("Nature")
     * @param origine    {@code ComProspectionOrigin.code} filter
     * @param commercial {@code CfgTier.id} (commercial) filter
     * @param search     free-text match on code/prospect name/temp contact fields
     * @param ouvertes   when true, only non-closed (IsClosed = false) prospections
     */
    public List<ProspectionListItemDto> findAll(String etape, String origine, UUID commercial, String search, Boolean ouvertes) {
        Specification<ComProspection> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.or(
                    cb.isNotNull(root.get("cfgProspect")),
                    cb.isNotNull(root.get("cfgCommercial"))
            ));
            if (StringUtils.hasText(etape)) {
                predicates.add(cb.equal(cb.lower(root.get("comProspectionKind").get("code")), etape.toLowerCase()));
            }
            if (StringUtils.hasText(origine)) {
                predicates.add(cb.equal(cb.lower(root.get("comProspectionOrigin").get("code")), origine.toLowerCase()));
            }
            if (commercial != null) {
                predicates.add(cb.equal(root.get("cfgCommercial").get("id"), commercial));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("tempDescription")), pattern),
                        cb.like(cb.lower(root.get("tempTel")), pattern),
                        cb.like(cb.lower(root.get("tempEMail")), pattern)
                ));
            }
            if (ouvertes != null && ouvertes) {
                predicates.add(cb.or(cb.isFalse(root.get("isClosed")), cb.isNull(root.get("isClosed"))));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return prospectionRepository.findAll(spec).stream().map(ProspectionService::toListItem).toList();
    }

    /** {@code GET /api/pre-prospections}: CfgProspectId IS NULL AND CfgCommercialId IS NULL. */
    public List<ProspectionListItemDto> findPreProspections() {
        return prospectionRepository.findByCfgProspectIsNullAndCfgCommercialIsNull().stream()
                .map(ProspectionService::toListItem)
                .toList();
    }

    public ProspectionDetailDto findById(UUID id) {
        return prospectionRepository.findById(id)
                .map(ProspectionService::toDetail)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + id));
    }

    /**
     * {@code POST /api/pre-prospections/{id}/creer-prospect}: creates a {@code CfgTier} and
     * links it as the prospection's {@code CfgProspectId}. Only valid on a pré-prospection
     * (CfgProspectId IS NULL) — 400 if the prospection already has a prospect.
     */
    @Transactional
    public ProspectionDetailDto creerProspect(UUID prospectionId, CreerProspectRequest request) {
        ComProspection prospection = prospectionRepository.findById(prospectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + prospectionId));

        if (prospection.getCfgProspect() != null) {
            throw new InvalidRequestException("Cette prospection a déjà un prospect lié: " + prospectionId);
        }

        CfgTier tier = prospectService.createEntity(request);
        prospection.setCfgProspect(tier);
        return toDetail(prospectionRepository.save(prospection));
    }

    /**
     * {@code POST /api/prospections/{id}/transformer}: pré-prospection → prospection, by
     * assigning the commercial. Requires a prospect to already be linked (via
     * {@link #creerProspect}) — 400 otherwise.
     */
    @Transactional
    public ProspectionDetailDto transformer(UUID id, TransformerProspectionRequest request) {
        ComProspection prospection = prospectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + id));

        if (prospection.getCfgProspect() == null) {
            throw new InvalidRequestException(
                    "Un prospect doit être créé (POST /api/pre-prospections/{id}/creer-prospect) avant de transformer: " + id);
        }
        if (request.commercialId() == null) {
            throw new InvalidRequestException("commercialId est requis");
        }

        CfgTier commercial = tierRepository.findById(request.commercialId())
                .orElseThrow(() -> new InvalidRequestException("Commercial (CfgTier) introuvable: " + request.commercialId()));

        prospection.setCfgCommercial(commercial);
        return toDetail(prospectionRepository.save(prospection));
    }

    /**
     * {@code PATCH /api/prospections/{id}/etape}: changes {@code ComProspectionKindId}
     * ("Nature"). Only membership is validated — no transition table.
     */
    @Transactional
    public ProspectionDetailDto changerEtape(UUID id, ChangerEtapeRequest request) {
        ComProspection prospection = prospectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + id));

        if (request.etapeId() == null) {
            throw new InvalidRequestException("etapeId est requis");
        }
        ComProspectionKind kind = prospectionKindRepository.findById(request.etapeId())
                .orElseThrow(() -> new InvalidRequestException("ComProspectionKind introuvable: " + request.etapeId()));

        prospection.setComProspectionKind(kind);
        if (request.note() != null) {
            prospection.setNotes(request.note());
        }
        return toDetail(prospectionRepository.save(prospection));
    }

    /** {@code PATCH /api/prospections/{id}/archiver}: closes the prospection with a reason. */
    @Transactional
    public ProspectionDetailDto archiver(UUID id, ArchiverProspectionRequest request) {
        ComProspection prospection = prospectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + id));

        if (request.raisonId() == null && !StringUtils.hasText(request.raisonTexte())) {
            throw new InvalidRequestException("raisonId ou raisonTexte est requis");
        }

        if (request.raisonId() != null) {
            ComProspectionClosingReason reason = closingReasonRepository.findById(request.raisonId())
                    .orElseThrow(() -> new InvalidRequestException("ComProspectionClosingReason introuvable: " + request.raisonId()));
            prospection.setComProspectionClosingReason(reason);
        }
        if (StringUtils.hasText(request.raisonTexte())) {
            prospection.setClosingReasonText(request.raisonTexte());
        }
        prospection.setIsClosed(true);
        prospection.setClosingDate(LocalDateTime.now());

        return toDetail(prospectionRepository.save(prospection));
    }

    /** {@code GET /api/prospections/{id}/interactions}: journal, most recent first. */
    public List<InteractionDto> findInteractions(UUID prospectionId) {
        if (!prospectionRepository.existsById(prospectionId)) {
            throw new ResourceNotFoundException("Prospection introuvable: " + prospectionId);
        }
        return actionMessageRepository.findByComProspectionIdOrderByDocDateDesc(prospectionId).stream()
                .map(ProspectionService::toInteractionDto)
                .toList();
    }

    /** {@code POST /api/prospections/{id}/interactions}. */
    @Transactional
    public InteractionDto addInteraction(UUID prospectionId, CreerInteractionRequest request) {
        ComProspection prospection = prospectionRepository.findById(prospectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + prospectionId));

        if (!StringUtils.hasText(request.description())) {
            throw new InvalidRequestException("description est requise");
        }

        ComActionMessage message = new ComActionMessage();
        message.setId(UUID.randomUUID());
        message.setComProspection(prospection);
        message.setDocDate(LocalDateTime.now());
        message.setDescription(request.description());
        message.setIsIn(request.entrante() != null ? request.entrante() : true);

        return toInteractionDto(actionMessageRepository.save(message));
    }

    private static InteractionDto toInteractionDto(ComActionMessage m) {
        return new InteractionDto(
                m.getId(),
                m.getDocDate(),
                m.getDescription(),
                m.getIsIn(),
                m.getCfgEmployee() != null ? m.getCfgEmployee().getId() : null,
                m.getCfgEmployee() != null
                        ? ((m.getCfgEmployee().getFirstName() != null ? m.getCfgEmployee().getFirstName() + " " : "")
                        + (m.getCfgEmployee().getLastName() != null ? m.getCfgEmployee().getLastName() : "")).trim()
                        : null
        );
    }

    private static ProspectionListItemDto toListItem(ComProspection p) {
        return new ProspectionListItemDto(
                p.getId(),
                p.getCode(),
                p.getDocDate(),
                p.getCfgProspect() != null ? p.getCfgProspect().getId() : null,
                nomTier(p.getCfgProspect()),
                p.getCfgCommercial() != null ? p.getCfgCommercial().getId() : null,
                nomTier(p.getCfgCommercial()),
                p.getComProspectionOrigin() != null ? p.getComProspectionOrigin().getCode() : null,
                p.getComProspectionKind() != null ? p.getComProspectionKind().getCode() : null,
                p.getIsClosed(),
                p.getPrice(),
                p.getArea()
        );
    }

    private static ProspectionDetailDto toDetail(ComProspection p) {
        return new ProspectionDetailDto(
                p.getId(),
                p.getCode(),
                p.getDocDate(),
                p.getCfgProspect() != null ? p.getCfgProspect().getId() : null,
                nomTier(p.getCfgProspect()),
                p.getCfgCommercial() != null ? p.getCfgCommercial().getId() : null,
                nomTier(p.getCfgCommercial()),
                p.getCfgProposer() != null ? p.getCfgProposer().getId() : null,
                nomTier(p.getCfgProposer()),
                p.getComProspectionOrigin() != null ? p.getComProspectionOrigin().getId() : null,
                p.getComProspectionOrigin() != null ? p.getComProspectionOrigin().getDescription() : null,
                p.getComProspectionOriginSupport() != null ? p.getComProspectionOriginSupport().getId() : null,
                p.getComProspectionOriginSupport() != null ? p.getComProspectionOriginSupport().getDescription() : null,
                p.getComProspectionKind() != null ? p.getComProspectionKind().getId() : null,
                p.getComProspectionKind() != null ? p.getComProspectionKind().getDescription() : null,
                p.getNotes(),
                p.getArea(),
                p.getAreaTo(),
                p.getPrice(),
                p.getPriceTo(),
                p.getAdvanceAmount(),
                p.getFundingOwnBase(),
                p.getFundingCredit(),
                p.getFundingOther(),
                p.getIsClosed(),
                p.getClosingDate(),
                p.getComProspectionClosingReason() != null ? p.getComProspectionClosingReason().getId() : null,
                p.getComProspectionClosingReason() != null ? p.getComProspectionClosingReason().getDescription() : null,
                p.getClosingReasonText(),
                p.getIsOldCustomer(),
                p.getStkHierarchy() != null ? p.getStkHierarchy().getId() : null,
                p.getStkItem() != null ? p.getStkItem().getId() : null
        );
    }

    private static String nomTier(com.tsix.immocrm.entities.cfg.CfgTier tier) {
        if (tier == null) return null;
        if (StringUtils.hasText(tier.getSocialReason())) return tier.getSocialReason();
        String prenom = tier.getFirstName() != null ? tier.getFirstName() : "";
        String nom = tier.getLastName() != null ? tier.getLastName() : "";
        return (prenom + " " + nom).trim();
    }
}
