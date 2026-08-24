package com.tsix.immocrm.services.hotline;

import com.tsix.immocrm.dto.hotline.HotlineFicheDto;
import com.tsix.immocrm.dto.hotline.HotlineResultDto;
import com.tsix.immocrm.dto.prospection.InteractionDto;
import com.tsix.immocrm.dto.prospection.ProspectionListItemDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.services.prospect.ProspectService;
import com.tsix.immocrm.services.prospect.ProspectSubResourcesService;
import com.tsix.immocrm.utils.PhoneNumberUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Backs {@code GET /api/hotline/recherche} — the "who is calling?" lookup a hotline agent runs
 * the instant a caller gives their phone number: are they already known, and if so, what is
 * their entire history (general info, prospections, dossiers, règlements, interactions,
 * statut history)? See {@link HotlineResultDto} for the full response shape.
 */
@Service
@Transactional(readOnly = true)
public class HotlineService {

    /**
     * Cap applied to every history list embedded in a {@link HotlineFicheDto} (prospections,
     * dossiers, règlements, interactions, historique) — this endpoint is meant for an instant,
     * single-round-trip overview while someone is on the phone, not exhaustive pagination. Once
     * a tier is identified via {@code tier.id}, the dedicated {@code /api/prospects/{id}/...}
     * endpoints give full, paginated access to any of these lists.
     */
    static final int HISTORIQUE_LIMIT = 50;

    private final CfgTierRepository tierRepository;
    private final ComProspectionRepository prospectionRepository;
    private final ComActionMessageRepository actionMessageRepository;
    private final ProspectService prospectService;
    private final ProspectSubResourcesService subResourcesService;

    public HotlineService(CfgTierRepository tierRepository,
                           ComProspectionRepository prospectionRepository,
                           ComActionMessageRepository actionMessageRepository,
                           ProspectService prospectService,
                           ProspectSubResourcesService subResourcesService) {
        this.tierRepository = tierRepository;
        this.prospectionRepository = prospectionRepository;
        this.actionMessageRepository = actionMessageRepository;
        this.prospectService = prospectService;
        this.subResourcesService = subResourcesService;
    }

    /** {@code GET /api/hotline/recherche?telephone=...} — see {@link HotlineResultDto}. */
    public HotlineResultDto rechercherParTelephone(String telephoneBrut) {
        if (!StringUtils.hasText(telephoneBrut)) {
            throw new InvalidRequestException("Le paramètre 'telephone' est requis");
        }
        String normalise = PhoneNumberUtils.normalize(telephoneBrut);
        if (normalise.isEmpty()) {
            throw new InvalidRequestException("Le paramètre 'telephone' ne contient aucun chiffre exploitable");
        }

        List<CfgTier> tiers = tierRepository.findByNormalizedPhone(normalise);
        List<HotlineFicheDto> fiches = tiers.stream().map(t -> toFiche(t.getId())).toList();

        Set<UUID> tierIds = tiers.stream().map(CfgTier::getId).collect(Collectors.toSet());
        Pageable preProspectionsPage = PageRequest.of(0, HISTORIQUE_LIMIT);
        List<ProspectionListItemDto> appelsSansFiche = prospectionRepository
                .findByNormalizedTempTel(normalise, preProspectionsPage)
                .stream()
                // A TempTel match can, on messy legacy data, already belong to a prospection
                // that's since been linked to one of the tiers above — don't show it twice.
                .filter(p -> p.getCfgProspect() == null || !tierIds.contains(p.getCfgProspect().getId()))
                .map(HotlineService::toProspectionListItem)
                .toList();

        return new HotlineResultDto(telephoneBrut, normalise, !fiches.isEmpty(), fiches, appelsSansFiche);
    }

    private HotlineFicheDto toFiche(UUID tierId) {
        Pageable recentSorted = PageRequest.of(0, HISTORIQUE_LIMIT, Sort.by(Sort.Direction.DESC, "docDate"));
        Pageable recent = PageRequest.of(0, HISTORIQUE_LIMIT);
        return new HotlineFicheDto(
                prospectService.findById(tierId),
                subResourcesService.prospections(tierId, recentSorted).getContent(),
                subResourcesService.dossiers(tierId, recentSorted).getContent(),
                subResourcesService.reglements(tierId, recent).getContent(),
                actionMessageRepository.findByCfgTierIdOrderByDocDateDesc(tierId, recent)
                        .map(HotlineService::toInteractionDto).getContent(),
                subResourcesService.historique(tierId, recent).getContent()
        );
    }

    private static ProspectionListItemDto toProspectionListItem(ComProspection p) {
        return new ProspectionListItemDto(
                p.getId(),
                p.getCode(),
                p.getDocDate(),
                p.getTempDescription(),
                p.getCfgProspect() != null ? p.getCfgProspect().getId() : null,
                nomTier(p.getCfgProspect()),
                p.getCfgCommercial() != null ? p.getCfgCommercial().getId() : null,
                nomTier(p.getCfgCommercial()),
                p.getComProspectionOrigin() != null ? p.getComProspectionOrigin().getCode() : null,
                p.getComProspectionKind() != null ? p.getComProspectionKind().getCode() : null,
                p.getIsClosed(),
                p.getPrice(),
                p.getArea(),
                p.getStkHierarchy() != null ? p.getStkHierarchy().getId() : null
        );
    }

    private static InteractionDto toInteractionDto(ComActionMessage m) {
        return new InteractionDto(
                m.getId(),
                m.getDocDate(),
                m.getDescription(),
                m.getIsIn(),
                m.getCfgEmployee() != null ? m.getCfgEmployee().getId() : null,
                nomTier(m.getCfgEmployee()),
                m.getComActionMessageType() != null ? m.getComActionMessageType().getId() : null,
                m.getComActionMessageType() != null ? m.getComActionMessageType().getDescription() : null,
                m.getComActionMessageSubCategory() != null ? m.getComActionMessageSubCategory().getId() : null,
                m.getComActionMessageSubCategory() != null ? m.getComActionMessageSubCategory().getDescription() : null,
                m.getAlertValidityDate(),
                m.getAdmEvent() != null ? m.getAdmEvent().getStartHour() : null
        );
    }

    private static String nomTier(CfgTier tier) {
        if (tier == null) return null;
        if (StringUtils.hasText(tier.getSocialReason())) return tier.getSocialReason();
        String prenom = tier.getFirstName() != null ? tier.getFirstName() : "";
        String nom = tier.getLastName() != null ? tier.getLastName() : "";
        return (prenom + " " + nom).trim();
    }
}
