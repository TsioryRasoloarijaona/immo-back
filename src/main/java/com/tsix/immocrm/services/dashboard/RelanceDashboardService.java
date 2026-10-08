package com.tsix.immocrm.services.dashboard;

import com.tsix.immocrm.domain.EcheanceRelance;
import com.tsix.immocrm.domain.PorteeRelance;
import com.tsix.immocrm.dto.dashboard.NouveauPreProspectDto;
import com.tsix.immocrm.dto.dashboard.RelanceProspectionDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
import com.tsix.immocrm.services.prospection.ProspectionService;
import com.tsix.immocrm.services.tranche.AppartenanceTranche;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
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
import java.util.UUID;

/**
 * Relances et nouveaux pré-prospects du tableau de bord ({@code FEATURE-dashboard-relances.md}). La
 * relance vit sur l'interaction ({@code ComActionMessage.AlertValidityDate}) : la colonne homonyme de
 * {@code ComProspection} est vide (§1). Une relance par prospection, pas de dédoublonnage.
 */
@Service
public class RelanceDashboardService {

    private final ComActionMessageRepository messageRepository;
    private final ComProspectionRepository prospectionRepository;
    private final PerimetreCommercialService perimetreService;
    private final Clock clock;

    public RelanceDashboardService(ComActionMessageRepository messageRepository,
                                   ComProspectionRepository prospectionRepository,
                                   PerimetreCommercialService perimetreService,
                                   Clock clock) {
        this.messageRepository = messageRepository;
        this.prospectionRepository = prospectionRepository;
        this.perimetreService = perimetreService;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- relances

    @Transactional(readOnly = true)
    public Page<RelanceProspectionDto> relances(EcheanceRelance echeance,
                                                PorteeRelance portee,
                                                @Nullable AuthenticatedUser principal,
                                                @Nullable UUID trancheId,
                                                Pageable pageable) {
        LocalDate aujourdhui = LocalDate.now(clock);
        UUID moi = principal != null ? principal.employeeId() : null;

        Specification<ComActionMessage> spec = perimetreService.filtreInteractions()
                .and(relanceVivante())
                .and(echue(echeance, aujourdhui))
                .and(echeance.limiteeAuxProspectionsDuMois()
                        ? prospectionDuMois(aujourdhui)
                        : (root, query, cb) -> cb.conjunction())
                .and(filtreDePortee(portee, moi))
                .and(prospectionDeLaTranche(trancheId));

        return messageRepository.findAll(spec, trierParEcheance(pageable))
                .map(message -> toDto(message, aujourdhui));
    }

    /**
     * {@code RETARD} : la population de la liste des prospections (qualifiée, ouverte, {@code DocDate}
     * du mois), pour que les deux écrans tombent d'accord. « Ouverte » parce que le légataire laisse une
     * alerte vivante sur une prospection archivée (§3 bis).
     */
    private static Specification<ComActionMessage> prospectionDuMois(LocalDate aujourdhui) {
        LocalDate premier = EcheanceRelance.debutDuMois(aujourdhui);
        LocalDateTime debut = premier.atStartOfDay();
        LocalDateTime finExclue = premier.plusMonths(1).atStartOfDay();
        return (root, query, cb) -> {
            Join<ComActionMessage, ComProspection> prospection = root.join("comProspection", JoinType.INNER);
            return cb.and(
                    cb.greaterThanOrEqualTo(prospection.get("docDate"), debut),
                    cb.lessThan(prospection.get("docDate"), finExclue),
                    cb.or(cb.isNull(prospection.get("isClosed")), cb.isFalse(prospection.get("isClosed"))),
                    DiscriminantPreProspection.nEstPasPreProspection(prospection, query, cb));
        };
    }

    private static Specification<ComActionMessage> prospectionDeLaTranche(@Nullable UUID trancheId) {
        if (trancheId == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> AppartenanceTranche.prospection(
                root.join("comProspection", JoinType.INNER), List.of(trancheId));
    }

    /**
     * Le prédicat du légataire mot pour mot : {@code WithoutRevival} = désarmée à la vente (le gros du
     * volume), {@code ComDocument} = rappel de paiement, pas relance commerciale (§1).
     */
    private static Specification<ComActionMessage> relanceVivante() {
        return (root, query, cb) -> cb.and(
                cb.isNotNull(root.get("alertValidityDate")),
                cb.or(cb.isNull(root.get("withoutRevival")), cb.isFalse(root.get("withoutRevival"))),
                cb.isNull(root.get("comDocument")),
                cb.isNotNull(root.get("comProspection")));
    }

    /**
     * Intervalle d'instants, pas égalité de date : le légataire écrit minuit, cette API une heure (§2).
     * {@code RETARD} n'a pas de borne basse ici, {@link #prospectionDuMois} le borne.
     */
    private static Specification<ComActionMessage> echue(EcheanceRelance echeance, LocalDate aujourdhui) {
        LocalDate premierJour = echeance.debut(aujourdhui);
        LocalDateTime finExclue = echeance.fin(aujourdhui).plusDays(1).atStartOfDay();
        return (root, query, cb) -> {
            Predicate avantLaFin = cb.lessThan(root.get("alertValidityDate"), finExclue);
            if (premierJour == null) {
                return avantLaFin;
            }
            return cb.and(
                    cb.greaterThanOrEqualTo(root.get("alertValidityDate"), premierJour.atStartOfDay()),
                    avantLaFin);
        };
    }

    /**
     * {@code MOI} inclut les non affectées : les pré-prospections n'ont jamais de commercial, sinon
     * personne ne les verrait (§4). Sans employé ({@code moi} nul), les non affectées seules, pas de 404.
     */
    private static Specification<ComActionMessage> filtreDePortee(PorteeRelance portee, @Nullable UUID moi) {
        if (portee == PorteeRelance.EQUIPE) {
            return (root, query, cb) -> cb.conjunction();
        }
        return miennesOuNonAffectees(moi);
    }

    private static Specification<ComActionMessage> miennesOuNonAffectees(@Nullable UUID moi) {
        return (root, query, cb) -> {
            Join<ComActionMessage, ComProspection> prospection = root.join("comProspection", JoinType.INNER);
            Join<ComProspection, CfgTier> commercial = prospection.join("cfgCommercial", JoinType.LEFT);
            Join<ComActionMessage, CfgTier> employe = root.join("cfgEmployee", JoinType.LEFT);

            Predicate nonAffectee = cb.and(cb.isNull(commercial.get("id")), cb.isNull(employe.get("id")));
            if (moi == null) {
                return nonAffectee;
            }
            // Commercial de la prospection d'abord, auteur de l'interaction ensuite (§4).
            Predicate mienne = cb.equal(cb.coalesce(commercial.get("id"), employe.get("id")), moi);
            return cb.or(mienne, nonAffectee);
        };
    }

    /** Tri imposé ; l'{@code id} départage les ex æquo, nombreux puisque les dates légataires sont à minuit. */
    private static Pageable trierParEcheance(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Order.asc("alertValidityDate"), Sort.Order.asc("id")));
    }

    private static RelanceProspectionDto toDto(ComActionMessage message, LocalDate aujourdhui) {
        ComProspection prospection = message.getComProspection();
        CfgTier prospect = prospection.getCfgProspect();
        CfgTier commercialProspection = prospection.getCfgCommercial();
        CfgTier employeInteraction = message.getCfgEmployee();

        // Même ordre que miennesOuNonAffectees, sinon une ligne servie sous MOI s'afficherait au nom
        // de quelqu'un d'autre.
        CfgTier commercial = commercialProspection != null ? commercialProspection : employeInteraction;

        LocalDate dateRelance = message.getAlertValidityDate().toLocalDate();

        return new RelanceProspectionDto(
                message.getId(),
                prospection.getId(),
                DiscriminantPreProspection.estPreProspection(prospection),
                ProspectionService.nomProspect(prospect, prospection),
                ProspectionService.telephoneProspect(prospect, prospection),
                dateRelance,
                ChronoUnit.DAYS.between(dateRelance, aujourdhui),
                message.getAlertMessage(),
                commercial != null ? commercial.getId() : null,
                TierNomFormatter.nom(commercial),
                commercial == null,
                message.getDocDate());
    }

    // ---------------------------------------------------------------- nouveaux pré-prospects

    /** Pas de {@code portee} : un lead du jour n'a pas de commercial. Seul le périmètre s'applique. */
    @Transactional(readOnly = true)
    public Page<NouveauPreProspectDto> nouveauxPreProspects(@Nullable LocalDate jour, Pageable pageable) {
        LocalDate cible = jour != null ? jour : LocalDate.now(clock);
        LocalDateTime debut = cible.atStartOfDay();
        LocalDateTime finExclue = cible.plusDays(1).atStartOfDay();

        Specification<ComProspection> spec = perimetreService.filtreProspections()
                // Discriminant de GET /api/pre-prospections, partagé à l'identique.
                .and(DiscriminantPreProspection.estPreProspection())
                .and((root, query, cb) -> cb.and(
                        cb.greaterThanOrEqualTo(root.get("userCreateDateTime"), debut),
                        cb.lessThan(root.get("userCreateDateTime"), finExclue)));

        Pageable trie = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Order.desc("userCreateDateTime"), Sort.Order.asc("id")));

        return prospectionRepository.findAll(spec, trie).map(RelanceDashboardService::toPreProspectDto);
    }

    private static NouveauPreProspectDto toPreProspectDto(ComProspection p) {
        return new NouveauPreProspectDto(
                p.getId(),
                p.getCode(),
                p.getTempDescription(),
                p.getTempTel(),
                p.getTempEMail(),
                p.getUserCreateDateTime());
    }

}
