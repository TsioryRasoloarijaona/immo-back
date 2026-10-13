package com.tsix.immocrm.services.equipe;

import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.equipe.ADistribuerDto;
import com.tsix.immocrm.dto.equipe.FiltreADistribuer;
import com.tsix.immocrm.dto.equipe.LigneADistribuerDto;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.services.parcours.LignesParcours;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
import com.tsix.immocrm.services.prospection.ProspectionService;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Carte « À distribuer » de « Mon équipe » : liste, compteur et lot de la répartition par filtre partagent
 * {@link #specification} — une seule définition (FEATURE-a-distribuer-jamais-traitees.md §2).
 */
@Service
@Transactional(readOnly = true)
public class ADistribuerService {

    static final int TAILLE_PAR_DEFAUT = 100;
    static final int TAILLE_MAX = 500;
    static final int PLAFOND_REPARTITION = 5_000;
    private static final Sort TRI = Sort.by("docDate").ascending().and(Sort.by("id").ascending());

    private final EquipeCommercialeService equipeService;
    private final ComProspectionRepository prospectionRepository;
    private final Clock clock;

    public ADistribuerService(EquipeCommercialeService equipeService, ComProspectionRepository prospectionRepository,
                              Clock clock) {
        this.equipeService = equipeService;
        this.prospectionRepository = prospectionRepository;
        this.clock = clock;
    }

    public PageDto<LigneADistribuerDto> lignes(FiltreADistribuer filtre, int page, int size) {
        if (page < 0 || size < 1) {
            throw new InvalidRequestException("`page` doit être ≥ 0 et `size` ≥ 1.");
        }
        Pageable pageable = PageRequest.of(page, Math.min(size, TAILLE_MAX), TRI);
        Specification<ComProspection> spec = specification(filtre);
        if (spec == null) {
            return PageDto.of(Page.empty(pageable));
        }
        Page<ComProspection> lues = prospectionRepository.findAll(spec, pageable);
        Set<UUID> traitees = Boolean.TRUE.equals(filtre.toutes()) ? dejaTraitees(lues.getContent()) : Set.of();
        List<LigneADistribuerDto> contenu = lues.getContent().stream()
                .map(p -> versLigne(p, traitees.contains(p.getId())))
                .toList();
        return PageDto.of(new PageImpl<>(contenu, pageable, lues.getTotalElements()));
    }

    /** Le total est celui de {@link #lignes} pour le même filtre, par construction. */
    public ADistribuerDto compter(FiltreADistribuer filtre) {
        Specification<ComProspection> spec = specification(filtre);
        if (spec == null) {
            return new ADistribuerDto(0, null, null);
        }
        Page<ComProspection> premiere = prospectionRepository.findAll(spec, PageRequest.of(0, 1, TRI));
        if (premiere.isEmpty()) {
            return new ADistribuerDto(0, null, null);
        }
        LocalDateTime plusAncienne = premiere.getContent().getFirst().getDocDate();
        Long age = plusAncienne != null
                ? Math.max(0L, ChronoUnit.DAYS.between(plusAncienne.toLocalDate(), LocalDate.now(clock)))
                : null;
        return new ADistribuerDto(premiere.getTotalElements(), plusAncienne, age);
    }

    /** Toutes les lignes du filtre (toutes pages) pour la répartition ; 400 au-delà du plafond. */
    public List<ComProspection> toutesLesLignes(FiltreADistribuer filtre) {
        Specification<ComProspection> spec = specification(filtre);
        if (spec == null) {
            return List.of();
        }
        long nombre = prospectionRepository.count(spec);
        if (nombre > PLAFOND_REPARTITION) {
            throw new InvalidRequestException(("%d pré-prospections correspondent au filtre : la répartition en accepte "
                    + "%d au plus. Découpez la période.").formatted(nombre, PLAFOND_REPARTITION));
        }
        return prospectionRepository.findAll(spec, TRI);
    }

    /**
     * {@code null} = rien à lire (chef sans projet). Période invalide ⇒ 400, projet hors périmètre ⇒ 403, avant
     * toute lecture.
     */
    private @Nullable Specification<ComProspection> specification(FiltreADistribuer filtre) {
        PeriodeEquipe periode = PeriodeEquipe.resoudre(filtre.du(), filtre.au(), LocalDate.now(clock));
        List<UUID> projets = equipeService.perimetre().projetsFiltres(filtre.projet());
        if (projets != null && projets.isEmpty()) {
            return null;
        }
        Specification<ComProspection> spec = DiscriminantPreProspection.estPreProspection()
                .and(ouvertesSansCommercial(periode))
                .and(projetsActifs(projets));
        return Boolean.TRUE.equals(filtre.toutes()) ? spec : spec.and(LignesParcours.sansInteraction());
    }

    private static Specification<ComProspection> ouvertesSansCommercial(PeriodeEquipe periode) {
        return (root, query, cb) -> cb.and(
                cb.isNull(root.get("cfgCommercial")),
                cb.or(cb.isNull(root.get("isClosed")), cb.isFalse(root.get("isClosed"))),
                cb.between(root.get("docDate"), periode.debut(), periode.fin()));
    }

    /** Société active ou sans société ; {@code projets} nul = tous les projets, lignes sans tranche comprises. */
    private static Specification<ComProspection> projetsActifs(@Nullable List<UUID> projets) {
        return (root, query, cb) -> {
            Join<ComProspection, CfgTranche> tranche = root.join("stkHierarchy", JoinType.LEFT);
            Join<CfgTranche, CfgProject> projet = tranche.join("cfgProject", JoinType.LEFT);
            Join<CfgProject, CfgCompany> societe = projet.join("cfgCompany", JoinType.LEFT);
            var active = cb.or(cb.isTrue(societe.get("isActive")), cb.isNull(societe.get("id")));
            return projets == null ? active : cb.and(active, projet.get("id").in(projets));
        };
    }

    /** Lignes de la page qui ont au moins une action non système : complément de {@code sansInteraction}. */
    private Set<UUID> dejaTraitees(List<ComProspection> page) {
        if (page.isEmpty()) {
            return Set.of();
        }
        Set<UUID> ids = page.stream().map(ComProspection::getId).collect(Collectors.toSet());
        Specification<ComProspection> parmiLaPage = (root, query, cb) -> root.get("id").in(ids);
        Set<UUID> jamais = prospectionRepository.findAll(parmiLaPage.and(LignesParcours.sansInteraction())).stream()
                .map(ComProspection::getId)
                .collect(Collectors.toSet());
        return ids.stream().filter(id -> !jamais.contains(id)).collect(Collectors.toSet());
    }

    private static LigneADistribuerDto versLigne(ComProspection p, boolean dejaTraitee) {
        return new LigneADistribuerDto(p.getId(), p.getCode(), ProspectionService.nomProspect(p.getCfgProspect(), p),
                Champs.de(p.getStkHierarchy(), CfgTranche::getCode),
                Champs.de(p.getStkHierarchy(), CfgTranche::getCfgProject, CfgProject::getId),
                p.getDocDate(), dejaTraitee);
    }
}
