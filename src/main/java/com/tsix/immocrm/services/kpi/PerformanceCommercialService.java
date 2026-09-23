package com.tsix.immocrm.services.kpi;

import com.tsix.immocrm.domain.InteractionKpi;
import com.tsix.immocrm.dto.kpi.ActionParTypeDto;
import com.tsix.immocrm.dto.kpi.PerformanceCommercialDto;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import org.jspecify.annotations.Nullable;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
 * <p><b>Deux attributions distinctes, assumées.</b> L'activité (toutes les actions) est comptée
 * pour <i>l'auteur</i> de la saisie ; le portefeuille (prospections, dossiers) pour le
 * <i>commercial assigné</i>. Elles coïncident dans 86 % des cas réels, et l'écart est précisément
 * ce qu'un responsable veut voir. La clé commune est le commercial ({@code CfgTier}), jamais le
 * compte : un employé porte parfois deux comptes, dont les chiffres doivent s'additionner.
 *
 * <p><b>L'auteur d'une action suit la règle du « Suivi des actions »</b>
 * ({@code FEATURE-suivi-actions.md} §3bis) : son compte ({@code AdmUserId}) quand il est
 * renseigné, <b>sinon le nom écrit sur l'action</b> ({@code UserName}), résolu vers le compte qui
 * le porte — seulement s'il est seul à le porter (« admin » est porté par deux comptes : jamais
 * deviné). Les actions « Système » ne sont à personne. Un compte sans employé (16 sur 75) forme sa
 * propre ligne, identifiée par {@code utilisateurId} : ses actions n'étaient comptées nulle part.
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
    private final AdmUserRepository userRepository;
    private final PerimetreCommercialService perimetreService;

    /** {@code UserName} des actions générées — exclues, cf. {@code SuiviActionService}. */
    static final String UTILISATEUR_SYSTEME = "Système";

    /** Libellé du groupe des actions sans type dans {@code actionsParType}. */
    static final String SANS_TYPE = "(sans type)";

    public PerformanceCommercialService(ComActionMessageRepository messageRepository,
                                        ComProspectionRepository prospectionRepository,
                                        ComFolderRepository folderRepository,
                                        CfgTierRepository tierRepository,
                                        AdmUserRepository userRepository,
                                        PerimetreCommercialService perimetreService) {
        this.messageRepository = messageRepository;
        this.prospectionRepository = prospectionRepository;
        this.folderRepository = folderRepository;
        this.tierRepository = tierRepository;
        this.userRepository = userRepository;
        this.perimetreService = perimetreService;
    }

    /**
     * Clé d'une ligne du KPI : le commercial quand le compte en a un, sinon le compte lui-même.
     * Exactement un des deux est renseigné.
     */
    private record Cle(UUID commercialId, UUID utilisateurId) {

        static Cle commercial(UUID id) {
            return new Cle(id, null);
        }

        static Cle compte(UUID id) {
            return new Cle(null, id);
        }
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
     * Les chiffres du compte connecté : ceux de son commercial s'il est rattaché à un employé,
     * sinon ceux du compte lui-même — un compte sans employé a désormais une activité propre.
     *
     * <p>Renvoie <b>200 avec des compteurs à zéro</b> quand il n'a rien fait sur la période : un
     * écran vide est une information, une erreur n'en est pas une. Le 404 est réservé à un appel
     * sans compte applicatif identifiable.
     */
    public PerformanceCommercialDto findMoi(AuthenticatedUser principal, LocalDate du, LocalDate au) {
        if (principal == null || (principal.employeeId() == null && principal.admUserId() == null)) {
            throw new ResourceNotFoundException("Aucun compte identifiable : aucune performance à afficher.");
        }
        Cle cle = principal.employeeId() != null
                ? Cle.commercial(principal.employeeId())
                : Cle.compte(principal.admUserId());
        Periode periode = Periode.resoudre(du, au);
        PerformanceCommercialDto trouve = assembler(periode).get(cle);
        return trouve != null ? trouve : vide(cle, periode);
    }

    // ---------------------------------------------------------------- assemblage

    /**
     * Les trois projections, fusionnées par commercial. L'ordre d'insertion suit les interactions
     * puis les prospections puis les dossiers ; le tri définitif est appliqué par l'appelant.
     */
    private Map<Cle, PerformanceCommercialDto> assembler(Periode periode) {
        // L'Optional du service de périmètre est résolu ici, une fois : « pas de restriction »
        // se transporte ensuite comme un null explicite, jamais comme un Optional en paramètre.
        List<UUID> perimetre = perimetreService.perimetreCourant().orElse(null);
        Map<Cle, Accumulateur> parCommercial = new LinkedHashMap<>();
        Comptes comptes = comptes();

        for (Object[] ligne : activite(periode, perimetre)) {
            Cle cle = auteur(ligne, comptes);
            if (cle == null) {
                continue;
            }
            Accumulateur acc = parCommercial.computeIfAbsent(cle, id -> new Accumulateur());
            String typeCode = (String) ligne[3];
            String typeLibelle = (String) ligne[4];
            String resultat = (String) ligne[5];
            if (InteractionKpi.TYPE_APPEL_SORTANT.equals(typeCode)) acc.appelsPasses++;
            if (InteractionKpi.TYPE_APPEL_ENTRANT.equals(typeCode)) acc.appelsRecus++;
            if (InteractionKpi.TYPE_VISITE.equals(typeCode)) acc.visitesRealisees++;
            if (InteractionKpi.RESULTAT_VISITE_DECROCHEE.equals(resultat)) acc.visitesDecrochees++;
            if (InteractionKpi.RESULTAT_RDV_PRIS.equals(resultat)) acc.rdvObtenus++;
            acc.interactionsTotal++;
            acc.parType.merge(StringUtils.hasText(typeLibelle) ? typeLibelle : SANS_TYPE, 1L, Long::sum);
            if (ligne[6] != null) {
                acc.prospections.add((UUID) ligne[6]);
            }
        }

        for (Object[] ligne : prospections(periode, perimetre)) {
            Accumulateur acc = parCommercial.computeIfAbsent(Cle.commercial((UUID) ligne[0]), id -> new Accumulateur());
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
            Accumulateur acc = parCommercial.computeIfAbsent(Cle.commercial((UUID) ligne[0]), id -> new Accumulateur());
            acc.dossiersCrees++;
            if (ligne[1] != null) {
                acc.montantTotal = acc.montantTotal.add((BigDecimal) ligne[1]);
            }
        }

        Map<UUID, String> noms = nomsDe(parCommercial.keySet().stream()
                .map(Cle::commercialId).filter(Objects::nonNull).toList());
        Map<Cle, PerformanceCommercialDto> resultat = new LinkedHashMap<>();
        parCommercial.forEach((cle, acc) -> resultat.put(cle, acc.versDto(cle,
                cle.commercialId() != null ? noms.get(cle.commercialId()) : comptes.nom(cle.utilisateurId()),
                periode)));
        return resultat;
    }

    /**
     * L'auteur d'une ligne d'activité (colonnes de
     * {@code ComActionMessageRepository#projeterActionsPourKpi}) : le compte s'il est renseigné,
     * sinon le compte seul à porter le nom écrit sur l'action ; puis son employé s'il en a un.
     * {@code null} si personne ne se résout — l'action n'est comptée pour personne.
     */
    private static Cle auteur(Object[] ligne, Comptes comptes) {
        UUID compte = (UUID) ligne[0];
        UUID employe = (UUID) ligne[1];
        if (compte == null) {
            Comptes.Compte parNom = comptes.parNomUnique((String) ligne[2]);
            if (parNom == null) {
                return null;
            }
            compte = parNom.id();
            employe = parNom.employeId();
        }
        return employe != null ? Cle.commercial(employe) : Cle.compte(compte);
    }

    private List<Object[]> activite(Periode periode, @Nullable List<UUID> perimetre) {
        if (perimetre == null) {
            return messageRepository.projeterActionsPourKpi(
                    periode.debut(), periode.fin(), UTILISATEUR_SYSTEME);
        }
        return messageRepository.projeterActionsPourKpiDansPerimetre(
                periode.debut(), periode.fin(), UTILISATEUR_SYSTEME, perimetre);
    }

    /**
     * Les comptes, pour résoudre un auteur par son nom et nommer une ligne sans employé. 75 lignes
     * en base réelle : lues une fois par calcul.
     */
    private Comptes comptes() {
        Map<UUID, Comptes.Compte> parId = new LinkedHashMap<>();
        Map<String, List<Comptes.Compte>> parNom = new HashMap<>();
        for (AdmUser u : userRepository.findAll()) {
            Comptes.Compte c = new Comptes.Compte(u.getId(),
                    u.getCfgEmployee() != null ? u.getCfgEmployee().getId() : null,
                    StringUtils.hasText(u.getDescription()) ? u.getDescription().trim() : u.getCode());
            parId.put(c.id(), c);
            if (StringUtils.hasText(u.getDescription())) {
                parNom.computeIfAbsent(u.getDescription().trim(), n -> new ArrayList<>()).add(c);
            }
        }
        return new Comptes(parId, parNom);
    }

    private record Comptes(Map<UUID, Compte> parId, Map<String, List<Compte>> parNom) {

        record Compte(UUID id, UUID employeId, String nom) {
        }

        /** Le compte seul à porter ce nom, ou {@code null} — un nom partagé n'est jamais deviné. */
        Compte parNomUnique(String nom) {
            if (!StringUtils.hasText(nom)) {
                return null;
            }
            List<Compte> candidats = parNom.get(nom.trim());
            return candidats != null && candidats.size() == 1 ? candidats.getFirst() : null;
        }

        String nom(UUID id) {
            Compte c = parId.get(id);
            return c != null ? c.nom() : null;
        }
    }

    private List<Object[]> prospections(Periode periode, @Nullable List<UUID> perimetre) {
        if (perimetre == null) {
            return prospectionRepository.projeterPourKpi(periode.debut(), periode.fin());
        }
        return prospectionRepository.projeterPourKpiDansPerimetre(
                periode.debut(), periode.fin(), perimetre);
    }

    private List<Object[]> dossiers(Periode periode, @Nullable List<UUID> perimetre) {
        if (perimetre == null) {
            return folderRepository.projeterPourKpi(periode.debut(), periode.fin());
        }
        return folderRepository.projeterPourKpiDansPerimetre(
                periode.debut(), periode.fin(), perimetre);
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

    private PerformanceCommercialDto vide(Cle cle, Periode periode) {
        String nom = cle.commercialId() != null
                ? nomsDe(List.of(cle.commercialId())).get(cle.commercialId())
                : comptes().nom(cle.utilisateurId());
        return new Accumulateur().versDto(cle, nom, periode);
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
        private final Map<String, Long> parType = new HashMap<>();
        private final Set<UUID> prospections = new HashSet<>();
        private long prospectionsAssignees;
        private long prospectionsCloturees;
        private long prospectionsPrisesEnCharge;
        private final List<Long> delaisHeures = new ArrayList<>();
        private long dossiersCrees;
        private BigDecimal montantTotal = BigDecimal.ZERO;

        PerformanceCommercialDto versDto(Cle cle, String nom, Periode periode) {
            // Du plus au moins fréquent ; à égalité, par libellé — un ordre stable d'un appel à l'autre.
            List<ActionParTypeDto> actionsParType = parType.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                            .thenComparing(Map.Entry.comparingByKey()))
                    .map(e -> new ActionParTypeDto(e.getKey(), e.getValue()))
                    .toList();
            return new PerformanceCommercialDto(
                    cle.commercialId(), cle.utilisateurId(), nom, periode.du(), periode.au(),
                    appelsPasses, appelsRecus, visitesRealisees, visitesDecrochees, rdvObtenus,
                    interactionsTotal, actionsParType, prospections.size(),
                    prospectionsAssignees, prospectionsCloturees, prospectionsPrisesEnCharge,
                    ratio(prospectionsPrisesEnCharge, prospectionsAssignees),
                    mediane(delaisHeures),
                    dossiersCrees,
                    montantTotal.setScale(2, RoundingMode.HALF_UP),
                    dossiersCrees > 0
                            ? montantTotal.divide(BigDecimal.valueOf(dossiersCrees), 2, RoundingMode.HALF_UP)
                            : null,
                    ratio(rdvObtenus, appelsPasses),
                    ratio(rdvObtenus, interactionsTotal),
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
