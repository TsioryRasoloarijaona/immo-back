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
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.services.prospect.ProspectService;
import com.tsix.immocrm.services.prospect.ProspectSubResourcesService;
import com.tsix.immocrm.utils.PhoneNumberUtils;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
    private final PerimetreCommercialService perimetreService;

    public HotlineService(CfgTierRepository tierRepository,
                           ComProspectionRepository prospectionRepository,
                           ComActionMessageRepository actionMessageRepository,
                           ProspectService prospectService,
                           ProspectSubResourcesService subResourcesService,
                           PerimetreCommercialService perimetreService) {
        this.perimetreService = perimetreService;
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
        // Périmètre commercial : le filtre est appliqué DANS la requête, avant le plafond de 50 —
        // filtrer la page déjà chargée reviendrait à évincer les lignes du périmètre au profit de
        // celles qui l'ont saturée (voir §9ter.2 et la doc du repository).
        Page<ComProspection> correspondances = perimetreService.perimetreCourant()
                .map(projetIds -> prospectionRepository.findByNormalizedTempTelDansPerimetre(
                        normalise, projetIds, preProspectionsPage))
                .orElseGet(() -> prospectionRepository.findByNormalizedTempTel(normalise, preProspectionsPage));

        List<ProspectionListItemDto> appelsSansFiche = correspondances.stream()
                // A TempTel match can, on messy legacy data, already belong to a prospection
                // that's since been linked to one of the tiers above — don't show it twice.
                .filter(p -> p.getCfgProspect() == null || !tierIds.contains(p.getCfgProspect().getId()))
                .map(HotlineService::toProspectionListItem)
                .toList();

        return new HotlineResultDto(telephoneBrut, normalise, !fiches.isEmpty(), fiches, appelsSansFiche);
    }

    /**
     * Chaque bloc de la fiche est plafonné à {@link #HISTORIQUE_LIMIT} lignes. Le périmètre
     * commercial doit donc être appliqué <b>dans chaque requête</b>, jamais sur le résultat :
     * filtrer après coup une page déjà plafonnée évince les lignes du périmètre (celles hors
     * périmètre ayant consommé la page) au lieu d'évincer celles hors périmètre — un tiers avec 50
     * interactions récentes sur un autre projet et une seule sur le mien renvoyait une liste vide.
     * Les prospections / dossiers / règlements passent par {@code ProspectSubResourcesService}, déjà
     * filtré par {@code Specification} ; les interactions sont filtrées ici par
     * {@code filtreInteractions()}, qui résout le projet par la prospection <i>puis</i> par le
     * dossier.
     */
    private HotlineFicheDto toFiche(UUID tierId) {
        Pageable recentSorted = PageRequest.of(0, HISTORIQUE_LIMIT, Sort.by(Sort.Direction.DESC, "docDate"));
        Pageable recent = PageRequest.of(0, HISTORIQUE_LIMIT);

        Specification<ComActionMessage> interactionsDuTier = Specification
                .<ComActionMessage>where((root, query, cb) -> cb.equal(root.get("cfgTier").get("id"), tierId))
                .and(perimetreService.filtreInteractions());

        return new HotlineFicheDto(
                prospectService.findById(tierId),
                subResourcesService.prospections(tierId, recentSorted).getContent(),
                subResourcesService.dossiers(tierId, recentSorted).getContent(),
                subResourcesService.reglements(tierId, recent).getContent(),
                actionMessageRepository.findAll(interactionsDuTier, recentSorted).stream()
                        .map(HotlineService::toInteractionDto).toList(),
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
                TierNomFormatter.nom(p.getCfgProspect()),
                p.getCfgCommercial() != null ? p.getCfgCommercial().getId() : null,
                TierNomFormatter.nom(p.getCfgCommercial()),
                p.getComProspectionOrigin() != null ? p.getComProspectionOrigin().getCode() : null,
                p.getComProspectionKind() != null ? p.getComProspectionKind().getCode() : null,
                p.getIsClosed(),
                p.getPrice(),
                p.getArea(),
                p.getStkHierarchy() != null ? p.getStkHierarchy().getId() : null,
                p.getStkHierarchy() != null ? p.getStkHierarchy().getCode() : null,
                p.getStkHierarchy() != null ? p.getStkHierarchy().getDescription() : null
        );
    }

    private static InteractionDto toInteractionDto(ComActionMessage m) {
        return new InteractionDto(
                m.getId(),
                m.getDocDate(),
                // AlertMessage porte le texte du commercial ; Description n'est qu'un libellé
                // « date + auteur ». Voir instructions/FIX-interaction-note.md.
                m.getAlertMessage(),
                m.getDescription(),
                m.getIsIn(),
                m.getCfgEmployee() != null ? m.getCfgEmployee().getId() : null,
                TierNomFormatter.nom(m.getCfgEmployee()),
                m.getComActionMessageType() != null ? m.getComActionMessageType().getId() : null,
                m.getComActionMessageType() != null ? m.getComActionMessageType().getDescription() : null,
                m.getComActionMessageSubCategory() != null ? m.getComActionMessageSubCategory().getId() : null,
                m.getComActionMessageSubCategory() != null ? m.getComActionMessageSubCategory().getDescription() : null,
                m.getAlertValidityDate(),
                m.getAdmEvent() != null ? m.getAdmEvent().getStartHour() : null
        );
    }
}
