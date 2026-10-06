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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * {@code GET /api/dashboard/relances} et {@code GET /api/dashboard/nouveaux-pre-prospects}
 * ({@code instructions/FEATURE-dashboard-relances.md}).
 *
 * <h2>Ce qu'est une relance, et ce qui la disqualifie</h2>
 *
 * <p>Une relance est portée par une <b>interaction</b> ({@code ComActionMessage.AlertValidityDate}),
 * jamais par la prospection : la colonne homonyme de {@code ComProspection} est morte en base
 * réelle (0 ligne). Trois conditions la disqualifient, et elles portent l'essentiel du volume :
 *
 * <ul>
 *   <li>{@code WithoutRevival = 1} — désarmée. <b>86 % des 39 335 alertes le sont</b> : c'est le
 *       drapeau que pose {@code desarmerRelancesDeLaProspection} quand la prospection est vendue.
 *       L'oublier multiplierait le volume par dix et ressortirait des prospections conclues.</li>
 *   <li>{@code ComDocumentId} non nul — ce n'est pas une relance commerciale mais un rappel de
 *       paiement rattaché à une échéance.</li>
 *   <li>{@code ComProspectionId} nul — l'interaction ne vise aucune prospection.</li>
 * </ul>
 *
 * <p>Il reste 3 765 relances vivantes, sur 3 765 prospections distinctes — une chacune. Aucune
 * déduplication n'est donc nécessaire ici.
 *
 * <h2>{@code RETARD} : les prospections du mois</h2>
 *
 * <p>Depuis le 28/09/2026, {@code echeance=RETARD} sert les relances échues des prospections
 * <b>qualifiées, ouvertes et créées dans le mois civil en cours</b> — la population de la liste
 * des prospections, pour que les deux écrans affichent le même nombre. L'ancienne fenêtre
 * glissante de 30 jours sur la date de l'alerte comptait aussi les prospections du mois
 * précédent et les pré-prospections. {@code AUJOURDHUI} est inchangé.
 */
@Service
public class RelanceDashboardService {

    private final ComActionMessageRepository messageRepository;
    private final ComProspectionRepository prospectionRepository;
    private final PerimetreCommercialService perimetreService;

    public RelanceDashboardService(ComActionMessageRepository messageRepository,
                                   ComProspectionRepository prospectionRepository,
                                   PerimetreCommercialService perimetreService) {
        this.messageRepository = messageRepository;
        this.prospectionRepository = prospectionRepository;
        this.perimetreService = perimetreService;
    }

    // ---------------------------------------------------------------- relances

    @Transactional(readOnly = true)
    public Page<RelanceProspectionDto> relances(EcheanceRelance echeance,
                                                PorteeRelance portee,
                                                @Nullable AuthenticatedUser principal,
                                                @Nullable UUID trancheId,
                                                Pageable pageable) {
        LocalDate aujourdhui = LocalDate.now();
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
     * La population de la liste des prospections : <b>qualifiée, ouverte, créée dans le mois
     * civil en cours</b>. C'est ce qui fait tomber le tableau de bord d'accord avec la liste.
     *
     * <ul>
     *   <li><b>Créée ce mois</b> — {@code ComProspection.DocDate} du 1er du mois 00:00 inclus au
     *       1er du mois suivant exclu. C'est la date que filtre la liste, pas
     *       {@code UserCreateDateTime}.</li>
     *   <li><b>Qualifiée</b> — une pré-prospection n'apparaît dans aucune liste de prospections :
     *       la compter ici fabriquait un chiffre que l'écran ne pouvait pas détailler.</li>
     *   <li><b>Ouverte</b> — le légataire laisse poser une alerte sur une prospection déjà
     *       archivée et ne l'éteint jamais (460 cas nettoyés le 27/09/2026). Tant que cette API
     *       ne le refuse pas elle-même, le filtre protège le compteur.</li>
     * </ul>
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

    /**
     * Filtre par tranche ({@code instructions/FEATURE-dashboard-filtre-tranche.md}) : la relance
     * est retenue si sa prospection vise la tranche — le prédicat de
     * {@code GET /api/prospections?trancheId=…}, partagé ({@link AppartenanceTranche#prospection}).
     * Neutre sans tranche.
     */
    private static Specification<ComActionMessage> prospectionDeLaTranche(@Nullable UUID trancheId) {
        if (trancheId == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> AppartenanceTranche.prospection(
                root.join("comProspection", JoinType.INNER), List.of(trancheId));
    }

    /**
     * Le prédicat du logiciel de bureau, mot pour mot — voir la javadoc de classe pour ce que
     * chaque terme retire.
     */
    private static Specification<ComActionMessage> relanceVivante() {
        return (root, query, cb) -> cb.and(
                cb.isNotNull(root.get("alertValidityDate")),
                cb.or(cb.isNull(root.get("withoutRevival")), cb.isFalse(root.get("withoutRevival"))),
                cb.isNull(root.get("comDocument")),
                cb.isNotNull(root.get("comProspection")));
    }

    /**
     * Fenêtre sur la date de l'alerte, bornes incluses. {@code RETARD} n'a pas de borne basse :
     * c'est la date de la prospection qui le borne, voir {@link #prospectionDuMois}.
     *
     * <p>La comparaison porte sur un <b>intervalle d'instants</b> et non sur une égalité de date :
     * {@code AlertValidityDate} est un {@code datetime} dont les 3 765 valeurs armées sont à
     * minuit, mais une relance posée par cette API à 14 h doit compter pour son jour. Borne haute
     * exclusive au lendemain 00:00 — {@code 23:59:59.999} compte donc bien pour le jour même.
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
     * {@code portee=MOI} : les relances dont le commercial résolu est l'appelant, <b>plus les non
     * affectées</b>.
     *
     * <p>Le second terme n'est pas une tolérance. 108 relances sur 3 765 ne désignent personne, ni
     * par la prospection ni par l'interaction, et les 21 posées sur des pré-prospections en font
     * toutes partie — une pré-prospection est <em>définie</em> par l'absence de commercial. Les
     * filtrer sous {@code MOI} les rendrait invisibles à tout le monde, or c'est le canal des leads
     * injectés automatiquement.
     *
     * <p>Un compte non rattaché à un employé ({@code moi} nul) ne peut posséder aucune relance : il
     * ne reçoit alors que les non affectées, sans erreur. Servir un 404 comme le fait
     * {@code /api/kpi/commerciaux/moi} viderait une tuile de tableau de bord au lieu de l'informer.
     */
    private static Specification<ComActionMessage> filtreDePortee(PorteeRelance portee, @Nullable UUID moi) {
        if (portee == PorteeRelance.EQUIPE) {
            // Neutre plutôt que null : Specification.and(null) est licite mais rend la chaîne
            // d'appel illisible dès qu'on en enchaîne plusieurs (même motif que ProspectionService).
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
            // coalesce = « l'affectation de la prospection d'abord, l'auteur de l'interaction
            // ensuite » — les deux concordent à 98,8 % quand elles coexistent.
            Predicate mienne = cb.equal(cb.coalesce(commercial.get("id"), employe.get("id")), moi);
            return cb.or(mienne, nonAffectee);
        };
    }

    /**
     * Tri imposé ici et non laissé à l'appelant : la pagination doit être déterministe, et
     * l'échéance la plus ancienne est le premier travail à faire. L'{@code id} départage les
     * ex æquo, nombreux puisque toutes les dates légataires sont à minuit.
     */
    private static Pageable trierParEcheance(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Order.asc("alertValidityDate"), Sort.Order.asc("id")));
    }

    private static RelanceProspectionDto toDto(ComActionMessage message, LocalDate aujourdhui) {
        ComProspection prospection = message.getComProspection();
        CfgTier prospect = prospection.getCfgProspect();
        CfgTier commercialProspection = prospection.getCfgCommercial();
        CfgTier employeInteraction = message.getCfgEmployee();

        // Même ordre de résolution que le prédicat de portée — les deux doivent rester d'accord,
        // sans quoi une ligne servie sous MOI pourrait s'afficher au nom de quelqu'un d'autre.
        CfgTier commercial = commercialProspection != null ? commercialProspection : employeInteraction;

        LocalDate dateRelance = message.getAlertValidityDate().toLocalDate();

        return new RelanceProspectionDto(
                message.getId(),
                prospection.getId(),
                DiscriminantPreProspection.estPreProspection(prospection),
                TierNomFormatter.nom(prospect),
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

    /**
     * Les leads créés dans la journée.
     *
     * <p>Pas de {@code portee} ici : un lead du jour n'a pas encore de commercial, {@code MOI}
     * n'aurait aucun sens. Seul le périmètre par projet s'applique, comme partout.
     *
     * <p>Une page vide est le cas courant — les leads arrivent par rafales, week-end compris.
     */
    @Transactional(readOnly = true)
    public Page<NouveauPreProspectDto> nouveauxPreProspects(@Nullable LocalDate jour, Pageable pageable) {
        LocalDate cible = jour != null ? jour : LocalDate.now();
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
