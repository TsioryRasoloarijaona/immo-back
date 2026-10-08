package com.tsix.immocrm.services.kpi;

import com.tsix.immocrm.domain.FuseauMetier;
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
import java.time.Clock;
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
 * {@code /api/kpi} échappe à {@code PerimetreInterceptor} : périmètre et entité appliqués ici, à chaque
 * agrégat ({@code FEATURE-kpi-performance-commerciaux.md} §5). Activité à l'auteur, portefeuille au
 * commercial assigné, regroupés par employé et non par compte (§3 ; auteur : {@code FEATURE-suivi-actions.md}).
 */
@Service
@Transactional(readOnly = true)
public class PerformanceCommercialService {

    private static final int JOURS_PAR_DEFAUT = 30;

    private final ComActionMessageRepository messageRepository;
    private final ComProspectionRepository prospectionRepository;
    private final ComFolderRepository folderRepository;
    private final CfgTierRepository tierRepository;
    private final AdmUserRepository userRepository;
    private final PerimetreCommercialService perimetreService;
    private final Clock clock;

    /** {@code UserName} des actions générées — exclues, cf. {@code SuiviActionService}. */
    static final String UTILISATEUR_SYSTEME = "Système";

    static final String SANS_TYPE = "(sans type)";

    public PerformanceCommercialService(ComActionMessageRepository messageRepository,
                                        ComProspectionRepository prospectionRepository,
                                        ComFolderRepository folderRepository,
                                        CfgTierRepository tierRepository,
                                        AdmUserRepository userRepository,
                                        PerimetreCommercialService perimetreService,
                                        Clock clock) {
        this.messageRepository = messageRepository;
        this.prospectionRepository = prospectionRepository;
        this.folderRepository = folderRepository;
        this.tierRepository = tierRepository;
        this.userRepository = userRepository;
        this.perimetreService = perimetreService;
        this.clock = clock;
    }

    /** Le commercial quand le compte en a un, sinon le compte : exactement un des deux renseigné. */
    private record Cle(UUID commercialId, UUID utilisateurId) {

        static Cle commercial(UUID id) {
            return new Cle(id, null);
        }

        static Cle compte(UUID id) {
            return new Cle(null, id);
        }
    }

    /** Sans activité sur la période ⇒ omis : la plupart des comptes sont désactivés (§6). */
    public List<PerformanceCommercialDto> findAll(LocalDate du, LocalDate au) {
        Periode periode = Periode.resoudre(du, au, LocalDate.now(clock));
        Comparator<PerformanceCommercialDto> parPerformance =
                Comparator.comparingLong(PerformanceCommercialDto::dossiersCrees)
                        .thenComparingLong(PerformanceCommercialDto::appelsPasses)
                        .reversed();
        return assembler(periode).values().stream().sorted(parPerformance).toList();
    }

    /** Rien sur la période ⇒ 200 à zéro ; 404 seulement sans compte identifiable (§6). */
    public PerformanceCommercialDto findMoi(AuthenticatedUser principal, LocalDate du, LocalDate au) {
        if (principal == null || (principal.employeeId() == null && principal.admUserId() == null)) {
            throw new ResourceNotFoundException("Aucun compte identifiable : aucune performance à afficher.");
        }
        Cle cle = principal.employeeId() != null
                ? Cle.commercial(principal.employeeId())
                : Cle.compte(principal.admUserId());
        Periode periode = Periode.resoudre(du, au, LocalDate.now(clock));
        PerformanceCommercialDto trouve = assembler(periode).get(cle);
        return trouve != null ? trouve : vide(cle, periode);
    }

    // ---------------------------------------------------------------- assemblage

    private Map<Cle, PerformanceCommercialDto> assembler(Periode periode) {
        // null = pas de restriction.
        List<UUID> perimetre = perimetreService.perimetreCourant().orElse(null);
        Map<Cle, Accumulateur> parCommercial = new LinkedHashMap<>();
        Comptes comptes = comptes();

        for (Object[] ligne : activite(periode, perimetre)) {
            Cle cle = auteur(ligne, comptes);
            if (cle != null) {
                cumulerInteraction(parCommercial.computeIfAbsent(cle, id -> new Accumulateur()), ligne);
            }
        }

        for (Object[] ligne : prospections(periode, perimetre)) {
            cumulerProspection(
                    parCommercial.computeIfAbsent(Cle.commercial((UUID) ligne[0]), id -> new Accumulateur()), ligne);
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

    private static void cumulerInteraction(Accumulateur acc, Object[] ligne) {
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

    private static void cumulerProspection(Accumulateur acc, Object[] ligne) {
        acc.prospectionsAssignees++;
        if (Boolean.TRUE.equals(ligne[2])) {
            acc.prospectionsCloturees++;
        }
        LocalDateTime creation = (LocalDateTime) ligne[1];
        LocalDateTime premiereInteraction = (LocalDateTime) ligne[3];
        if (premiereInteraction == null) {
            return;
        }
        acc.prospectionsPrisesEnCharge++;
        // Saisie rétroactive : une interaction peut précéder la prospection ; délai ramené à 0.
        if (creation != null) {
            long heures = heuresEcoulees(creation, premiereInteraction);
            acc.delaisHeures.add(Math.max(0L, heures));
        }
    }

    /** Heures réellement écoulées entre deux heures murales marocaines (changements d'heure du Ramadan compris). */
    static long heuresEcoulees(LocalDateTime debut, LocalDateTime fin) {
        return Duration.between(debut.atZone(FuseauMetier.ZONE), fin.atZone(FuseauMetier.ZONE)).toHours();
    }

    /**
     * Le compte, sinon le compte <b>seul</b> à porter le nom écrit sur l'action (un nom partagé n'est
     * jamais deviné), puis son employé. {@code null} ⇒ l'action n'est comptée pour personne.
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

    /** Table de quelques dizaines de lignes : lue en entier, une fois par calcul. */
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

    /** {@code au} inclus jusqu'à la fin de la journée : à minuit, « jusqu'à aujourd'hui » perdrait le jour. */
    private record Periode(LocalDate du, LocalDate au, LocalDateTime debut, LocalDateTime fin) {

        static Periode resoudre(LocalDate du, LocalDate au, LocalDate aujourdhui) {
            LocalDate fin = au != null ? au : aujourdhui;
            LocalDate debut = du != null ? du : fin.minusDays(JOURS_PAR_DEFAUT - 1L);
            if (debut.isAfter(fin)) {
                throw new InvalidRequestException(
                        "La date de début (" + debut + ") est postérieure à la date de fin (" + fin + ").");
            }
            return new Periode(debut, fin, debut.atStartOfDay(), fin.atTime(LocalTime.MAX));
        }
    }

    // ---------------------------------------------------------------- accumulateur

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

        /** Dénominateur nul ⇒ {@code null}, pas 0 : « aucun appel » n'est pas « aucun rendez-vous ». */
        private static BigDecimal ratio(long numerateur, long denominateur) {
            if (denominateur <= 0) {
                return null;
            }
            return BigDecimal.valueOf(numerateur)
                    .divide(BigDecimal.valueOf(denominateur), 3, RoundingMode.HALF_UP);
        }

        /**
         * Médiane : quelques leads repris des mois plus tard tirent la moyenne (§4). Effectif pair ⇒
         * la valeur centrale basse, pas la demi-somme : un délai qui n'a jamais eu lieu n'apporte rien.
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
