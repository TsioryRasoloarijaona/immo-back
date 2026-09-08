package com.tsix.immocrm.services.kpi;

import com.tsix.immocrm.domain.InteractionKpi;
import com.tsix.immocrm.dto.kpi.PerformanceCommercialDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * {@code GET /api/kpi/commerciaux[/moi]} — performance commerciale sur une période
 * ({@code instructions/FEATURE-kpi-performance-commerciaux.md}).
 *
 * <p><b>Ce service porte lui-même les deux restrictions de visibilité, et c'est le point de
 * vigilance de la feature.</b> {@code /api/kpi} n'appartient pas aux collections scopées de
 * {@code PerimetreInterceptor} — qui ne connaît que {@code dossiers}, {@code prospections},
 * {@code pre-prospections} et {@code encaissements} — donc rien n'est filtré en amont. Chaque
 * agrégat existe en deux variantes, avec et sans périmètre, et toutes portent le filtre d'entité
 * juridique. Un compteur resté global exposerait ici les chiffres d'entités que le reste de
 * l'application masque. Même avertissement que {@code DashboardService}, pour la même raison.
 *
 * <p><b>Deux attributions distinctes, assumées.</b> L'activité (appels, visites, interactions) est
 * comptée pour <i>l'auteur</i> de la saisie ; le portefeuille (prospections, dossiers) pour le
 * <i>commercial assigné</i>. Elles coïncident dans 86 % des cas réels, et l'écart est précisément
 * ce qu'un responsable veut voir. La clé commune est le commercial ({@code CfgTier}), jamais le
 * compte : un employé porte parfois deux comptes, dont les chiffres doivent s'additionner.
 */
@Service
@Transactional(readOnly = true)
public class PerformanceCommercialService {

    /** Fenêtre par défaut quand l'appelant ne précise pas de période. */
    private static final int JOURS_PAR_DEFAUT = 30;

    private final ComActionMessageRepository messageRepository;
    private final ComProspectionRepository prospectionRepository;
    private final ComFolderRepository folderRepository;
    private final CfgTierRepository tierRepository;
    private final PerimetreCommercialService perimetreService;

    public PerformanceCommercialService(ComActionMessageRepository messageRepository,
                                        ComProspectionRepository prospectionRepository,
                                        ComFolderRepository folderRepository,
                                        CfgTierRepository tierRepository,
                                        PerimetreCommercialService perimetreService) {
        this.messageRepository = messageRepository;
        this.prospectionRepository = prospectionRepository;
        this.folderRepository = folderRepository;
        this.tierRepository = tierRepository;
        this.perimetreService = perimetreService;
    }

    /**
     * Tous les commerciaux ayant au moins une ligne sur la période, les plus performants d'abord
     * (dossiers créés, puis appels passés). Ceux sans aucune activité sont omis : sur les 75
     * comptes de la base réelle, 60 sont désactivés, et les lister à zéro noierait les 23 qui
     * travaillent.
     */
    public List<PerformanceCommercialDto> findAll(LocalDate du, LocalDate au) {
        Periode periode = Periode.resoudre(du, au);
        Comparator<PerformanceCommercialDto> parPerformance =
                Comparator.comparingLong(PerformanceCommercialDto::dossiersCrees)
                        .thenComparingLong(PerformanceCommercialDto::appelsPasses)
                        .reversed();
        return assembler(periode).values().stream().sorted(parPerformance).toList();
    }

    /**
     * Les chiffres du commercial rattaché au compte connecté.
     *
     * <p>Renvoie <b>200 avec des compteurs à zéro</b> quand il n'a rien fait sur la période : un
     * écran vide est une information, une erreur n'en est pas une. Le 404 est réservé au cas
     * structurel où le compte n'est rattaché à aucun employé ({@code AdmUser.CfgEmployeeId} nul,
     * 16 comptes sur 75) — il n'y a alors pas de commercial dont parler.
     */
    public PerformanceCommercialDto findMoi(AuthenticatedUser principal, LocalDate du, LocalDate au) {
        UUID commercialId = principal != null ? principal.employeeId() : null;
        if (commercialId == null) {
            throw new ResourceNotFoundException(
                    "Ce compte n'est rattaché à aucun employé : aucune performance commerciale à afficher.");
        }
        Periode periode = Periode.resoudre(du, au);
        PerformanceCommercialDto trouve = assembler(periode).get(commercialId);
        return trouve != null ? trouve : vide(commercialId, periode);
    }

    // ---------------------------------------------------------------- assemblage

    /**
     * Les trois projections, fusionnées par commercial. L'ordre d'insertion suit les interactions
     * puis les prospections puis les dossiers ; le tri définitif est appliqué par l'appelant.
     */
    private Map<UUID, PerformanceCommercialDto> assembler(Periode periode) {
        Optional<List<UUID>> perimetre = perimetreService.perimetreCourant();
        Map<UUID, Accumulateur> parCommercial = new LinkedHashMap<>();

        for (Object[] ligne : activite(periode, perimetre)) {
            Accumulateur acc = parCommercial.computeIfAbsent((UUID) ligne[0], id -> new Accumulateur());
            acc.appelsPasses = nombre(ligne[1]);
            acc.appelsRecus = nombre(ligne[2]);
            acc.visitesRealisees = nombre(ligne[3]);
            acc.visitesDecrochees = nombre(ligne[4]);
            acc.rdvObtenus = nombre(ligne[5]);
            acc.interactionsTotal = nombre(ligne[6]);
            acc.prospectionsTraitees = nombre(ligne[7]);
        }

        for (Object[] ligne : prospections(periode, perimetre)) {
            Accumulateur acc = parCommercial.computeIfAbsent((UUID) ligne[0], id -> new Accumulateur());
            acc.prospectionsAssignees++;
            if (Boolean.TRUE.equals(ligne[2])) {
                acc.prospectionsCloturees++;
            }
            LocalDateTime creation = (LocalDateTime) ligne[1];
            LocalDateTime premiereInteraction = (LocalDateTime) ligne[3];
            if (premiereInteraction != null) {
                acc.prospectionsPrisesEnCharge++;
                // Une interaction antérieure à la prospection existe en base (saisie rétroactive) ;
                // un délai négatif n'a pas de sens et fausserait la médiane, on le ramène à zéro.
                if (creation != null) {
                    long heures = Duration.between(creation, premiereInteraction).toHours();
                    acc.delaisHeures.add(Math.max(0L, heures));
                }
            }
        }

        for (Object[] ligne : dossiers(periode, perimetre)) {
            Accumulateur acc = parCommercial.computeIfAbsent((UUID) ligne[0], id -> new Accumulateur());
            acc.dossiersCrees++;
            if (ligne[1] != null) {
                acc.montantTotal = acc.montantTotal.add((BigDecimal) ligne[1]);
            }
        }

        Map<UUID, String> noms = nomsDe(parCommercial.keySet());
        Map<UUID, PerformanceCommercialDto> resultat = new LinkedHashMap<>();
        parCommercial.forEach((id, acc) -> resultat.put(id, acc.versDto(id, noms.get(id), periode)));
        return resultat;
    }

    private List<Object[]> activite(Periode periode, Optional<List<UUID>> perimetre) {
        return perimetre
                .map(projetIds -> messageRepository.agregerActiviteParCommercialDansPerimetre(
                        periode.debut(), periode.fin(),
                        InteractionKpi.TYPE_APPEL_SORTANT, InteractionKpi.TYPE_APPEL_ENTRANT,
                        InteractionKpi.TYPE_VISITE, InteractionKpi.RESULTAT_VISITE_DECROCHEE,
                        InteractionKpi.RESULTAT_RDV_PRIS, projetIds))
                .orElseGet(() -> messageRepository.agregerActiviteParCommercial(
                        periode.debut(), periode.fin(),
                        InteractionKpi.TYPE_APPEL_SORTANT, InteractionKpi.TYPE_APPEL_ENTRANT,
                        InteractionKpi.TYPE_VISITE, InteractionKpi.RESULTAT_VISITE_DECROCHEE,
                        InteractionKpi.RESULTAT_RDV_PRIS));
    }

    private List<Object[]> prospections(Periode periode, Optional<List<UUID>> perimetre) {
        return perimetre
                .map(projetIds -> prospectionRepository.projeterPourKpiDansPerimetre(
                        periode.debut(), periode.fin(), projetIds))
                .orElseGet(() -> prospectionRepository.projeterPourKpi(periode.debut(), periode.fin()));
    }

    private List<Object[]> dossiers(Periode periode, Optional<List<UUID>> perimetre) {
        return perimetre
                .map(projetIds -> folderRepository.projeterPourKpiDansPerimetre(
                        periode.debut(), periode.fin(), projetIds))
                .orElseGet(() -> folderRepository.projeterPourKpi(periode.debut(), periode.fin()));
    }

    private Map<UUID, String> nomsDe(Iterable<UUID> ids) {
        List<UUID> liste = new ArrayList<>();
        ids.forEach(liste::add);
        if (liste.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> noms = new LinkedHashMap<>();
        for (CfgTier tier : tierRepository.findAllById(liste)) {
            noms.put(tier.getId(), nomAffiche(tier));
        }
        return noms;
    }

    /** {@code Description}, à défaut « prénom nom », à défaut le code — jamais une chaîne vide. */
    private static String nomAffiche(CfgTier tier) {
        if (StringUtils.hasText(tier.getDescription())) {
            return tier.getDescription().trim();
        }
        String complet = ((tier.getFirstName() != null ? tier.getFirstName() : "") + " "
                + (tier.getLastName() != null ? tier.getLastName() : "")).trim();
        if (StringUtils.hasText(complet)) {
            return complet;
        }
        return StringUtils.hasText(tier.getCode()) ? tier.getCode() : null;
    }

    private PerformanceCommercialDto vide(UUID commercialId, Periode periode) {
        Map<UUID, String> noms = nomsDe(List.of(commercialId));
        return new Accumulateur().versDto(commercialId, noms.get(commercialId), periode);
    }

    private static long nombre(Object valeur) {
        return valeur instanceof Number n ? n.longValue() : 0L;
    }

    // ---------------------------------------------------------------- période

    /**
     * Bornes effectives de la période.
     *
     * <p>{@code au} est inclusive <b>jusqu'à la fin de la journée</b> : la borner à minuit
     * exclurait toute l'activité du jour demandé, ce qui rendrait faux le cas le plus courant —
     * « du 1er au aujourd'hui ».
     */
    private record Periode(LocalDate du, LocalDate au, LocalDateTime debut, LocalDateTime fin) {

        static Periode resoudre(LocalDate du, LocalDate au) {
            LocalDate fin = au != null ? au : LocalDate.now();
            LocalDate debut = du != null ? du : fin.minusDays(JOURS_PAR_DEFAUT - 1L);
            if (debut.isAfter(fin)) {
                throw new InvalidRequestException(
                        "La date de début (" + debut + ") est postérieure à la date de fin (" + fin + ").");
            }
            return new Periode(debut, fin, debut.atStartOfDay(), fin.atTime(LocalTime.MAX));
        }
    }

    // ---------------------------------------------------------------- accumulateur

    /** État mutable d'un commercial pendant la fusion des trois projections. */
    private static final class Accumulateur {
        private long appelsPasses;
        private long appelsRecus;
        private long visitesRealisees;
        private long visitesDecrochees;
        private long rdvObtenus;
        private long interactionsTotal;
        private long prospectionsTraitees;
        private long prospectionsAssignees;
        private long prospectionsCloturees;
        private long prospectionsPrisesEnCharge;
        private final List<Long> delaisHeures = new ArrayList<>();
        private long dossiersCrees;
        private BigDecimal montantTotal = BigDecimal.ZERO;

        PerformanceCommercialDto versDto(UUID commercialId, String nom, Periode periode) {
            return new PerformanceCommercialDto(
                    commercialId, nom, periode.du(), periode.au(),
                    appelsPasses, appelsRecus, visitesRealisees, visitesDecrochees, rdvObtenus,
                    interactionsTotal, prospectionsTraitees,
                    prospectionsAssignees, prospectionsCloturees, prospectionsPrisesEnCharge,
                    ratio(prospectionsPrisesEnCharge, prospectionsAssignees),
                    mediane(delaisHeures),
                    dossiersCrees,
                    montantTotal.setScale(2, RoundingMode.HALF_UP),
                    dossiersCrees > 0
                            ? montantTotal.divide(BigDecimal.valueOf(dossiersCrees), 2, RoundingMode.HALF_UP)
                            : null,
                    ratio(rdvObtenus, appelsPasses),
                    ratio(dossiersCrees, prospectionsAssignees));
        }

        /**
         * {@code null} — jamais zéro — quand le dénominateur est nul : « aucun appel passé » et
         * « des appels passés, aucun rendez-vous » sont deux situations opposées, et un 0 les
         * confondrait au détriment du premier.
         */
        private static BigDecimal ratio(long numerateur, long denominateur) {
            if (denominateur <= 0) {
                return null;
            }
            return BigDecimal.valueOf(numerateur)
                    .divide(BigDecimal.valueOf(denominateur), 3, RoundingMode.HALF_UP);
        }

        /**
         * Médiane et non moyenne : quelques leads repris des mois plus tard suffisent à tirer une
         * moyenne loin du comportement habituel (109 h de moyenne sur la base réelle). Sur un
         * effectif pair, la borne basse des deux valeurs centrales — pas leur demi-somme : un
         * délai est un fait observé, en inventer un qui n'a jamais eu lieu n'apporte rien.
         */
        private static Long mediane(List<Long> valeurs) {
            if (valeurs.isEmpty()) {
                return null;
            }
            List<Long> triees = new ArrayList<>(valeurs);
            Collections.sort(triees);
            return triees.get((triees.size() - 1) / 2);
        }
    }
}
