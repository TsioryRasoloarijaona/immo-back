package com.tsix.immocrm.services.parcours;

import com.tsix.immocrm.domain.EcheanceRelance;
import com.tsix.immocrm.dto.parcours.BlocLeadsDto;
import com.tsix.immocrm.dto.parcours.BlocRelancesDto;
import com.tsix.immocrm.dto.parcours.JourneeDto;
import com.tsix.immocrm.dto.parcours.LeadJourneeDto;
import com.tsix.immocrm.dto.parcours.RelanceJourneeDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.services.dashboard.RelanceDashboardService;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
import com.tsix.immocrm.services.prospection.ReglesInteraction;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** {@code GET /api/journee} : quatre blocs, chacun total + 20 lignes (FEATURE-parcours-commercial.md §3). */
@Service
@Transactional(readOnly = true)
public class JourneeService {

    static final int LIGNES_PAR_BLOC = 20;
    private static final String ALERT_VALIDITY_DATE = "alertValidityDate";

    private final ComProspectionRepository prospectionRepository;
    private final ComActionMessageRepository messageRepository;
    private final CfgTierRepository tierRepository;
    private final PerimetreCommercialService perimetreService;
    private final PorteeParcours portee;
    private final Clock clock;

    public JourneeService(ComProspectionRepository prospectionRepository, ComActionMessageRepository messageRepository,
                          CfgTierRepository tierRepository, PerimetreCommercialService perimetreService,
                          PorteeParcours portee, Clock clock) {
        this.prospectionRepository = prospectionRepository;
        this.messageRepository = messageRepository;
        this.tierRepository = tierRepository;
        this.perimetreService = perimetreService;
        this.portee = portee;
        this.clock = clock;
    }

    public JourneeDto journee(@Nullable UUID commercialId) {
        UUID commercial = portee.commercialLu(commercialId);
        LocalDate aujourdhui = LocalDate.now(clock);
        return new JourneeDto(aujourdhui, commercial, prenom(commercial),
                nouveauxLeads(commercial, aujourdhui),
                relances(EcheanceRelance.AUJOURDHUI, commercial, aujourdhui, false),
                relances(EcheanceRelance.AUJOURDHUI, commercial, aujourdhui, true),
                relances(EcheanceRelance.RETARD, commercial, aujourdhui, null));
    }

    private @Nullable String prenom(@Nullable UUID commercial) {
        if (commercial == null) {
            return null;
        }
        return tierRepository.findById(commercial)
                .map(t -> StringUtils.hasText(t.getFirstName()) ? t.getFirstName().trim() : TierNomFormatter.nom(t))
                .orElse(null);
    }

    // ---------------------------------------------------------------- nouveaux leads

    private BlocLeadsDto nouveauxLeads(@Nullable UUID commercial, LocalDate aujourdhui) {
        Specification<ComProspection> spec = perimetreService.filtreProspections()
                .and(LignesParcours.aTrier(commercial, aujourdhui.minusMonths(LignesParcours.MOIS_RECENTS)))
                .and(LignesParcours.nonTraitee());
        Page<ComProspection> page = prospectionRepository.findAll(spec, PageRequest.of(0, LIGNES_PAR_BLOC,
                Sort.by(Sort.Order.desc(LignesParcours.DOC_DATE), Sort.Order.asc("id"))));
        return new BlocLeadsDto(page.getTotalElements(),
                page.getContent().stream().map(p -> toLead(p, aujourdhui)).toList());
    }

    private static LeadJourneeDto toLead(ComProspection p, LocalDate aujourdhui) {
        return new LeadJourneeDto(p.getId(), p.getCode(), LignesParcours.nom(p), LignesParcours.telephone(p),
                LignesParcours.indicatif(p), LignesParcours.projetCode(p), LignesParcours.trancheCode(p),
                p.getDocDate(), ageJours(p, aujourdhui), p.getNotes(),
                Champs.de(p.getComProspectionOrigin(), ComProspectionOrigin::getCode));
    }

    static long ageJours(ComProspection p, LocalDate aujourdhui) {
        return p.getDocDate() == null ? 0L
                : Math.max(0L, ChronoUnit.DAYS.between(p.getDocDate().toLocalDate(), aujourdhui));
    }

    // ---------------------------------------------------------------- relances

    /** {@code visites} : vrai = résultats « visite / RDV », faux = les autres, nul = tous (retard). */
    private BlocRelancesDto relances(EcheanceRelance echeance, @Nullable UUID commercial, LocalDate aujourdhui,
                                     @Nullable Boolean visites) {
        Specification<ComActionMessage> spec = perimetreService.filtreInteractions()
                .and(RelanceDashboardService.relancesEchues(echeance, aujourdhui))
                .and(ouverteDuCommercial(commercial))
                .and(intention(visites));
        Page<ComActionMessage> page = messageRepository.findAll(spec, PageRequest.of(0, LIGNES_PAR_BLOC,
                Sort.by(Sort.Order.asc(ALERT_VALIDITY_DATE), Sort.Order.asc("id"))));
        return new BlocRelancesDto(page.getTotalElements(),
                page.getContent().stream().map(m -> toRelance(m, aujourdhui)).toList());
    }

    /** Relance non impayée d'une prospection ouverte dont le commercial est {@code commercial} (§2). */
    private static Specification<ComActionMessage> ouverteDuCommercial(@Nullable UUID commercial) {
        return (root, query, cb) -> {
            if (commercial == null) {
                return cb.disjunction();
            }
            Join<ComActionMessage, ComProspection> prospection = root.join(LignesParcours.COM_PROSPECTION, JoinType.INNER);
            Join<ComProspection, CfgTier> titulaire = prospection.join(LignesParcours.CFG_COMMERCIAL, JoinType.INNER);
            return cb.and(
                    cb.equal(titulaire.get("id"), commercial),
                    cb.or(cb.isNull(prospection.get(LignesParcours.IS_CLOSED)),
                            cb.isFalse(prospection.get(LignesParcours.IS_CLOSED))),
                    cb.or(cb.isNull(root.get("isUnpaid")), cb.isFalse(root.get("isUnpaid"))));
        };
    }

    private static Specification<ComActionMessage> intention(@Nullable Boolean visites) {
        return (root, query, cb) -> {
            if (visites == null) {
                return cb.conjunction();
            }
            Join<ComActionMessage, ComActionMessageSubCategory> resultat =
                    root.join("comActionMessageSubCategory", JoinType.LEFT);
            var estVisite = resultat.get("code").in(ReglesInteraction.RESULTATS_AVEC_DATE_PLANIFIEE);
            return Boolean.TRUE.equals(visites)
                    ? estVisite
                    : cb.or(cb.isNull(resultat.get("code")), cb.not(estVisite));
        };
    }

    private static RelanceJourneeDto toRelance(ComActionMessage m, LocalDate aujourdhui) {
        ComProspection p = m.getComProspection();
        LocalDate echeance = m.getAlertValidityDate().toLocalDate();
        return new RelanceJourneeDto(m.getId(), p.getId(), DiscriminantPreProspection.estPreProspection(p),
                LignesParcours.nom(p), LignesParcours.telephone(p), LignesParcours.indicatif(p),
                LignesParcours.projetCode(p), LignesParcours.trancheCode(p),
                Champs.de(p.getComProspectionKind(), ComProspectionKind::getCode),
                m.getAlertValidityDate(), Math.max(0L, ChronoUnit.DAYS.between(echeance, aujourdhui)),
                m.getAlertMessage(),
                Champs.de(m.getComActionMessageSubCategory(), ComActionMessageSubCategory::getDescription));
    }
}
