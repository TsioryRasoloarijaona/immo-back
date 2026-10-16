package com.tsix.immocrm.services.equipe;

import com.tsix.immocrm.domain.InteractionKpi;
import com.tsix.immocrm.domain.RolesExemptesPerimetre;
import com.tsix.immocrm.dto.equipe.AffectationsEquipeDto;
import com.tsix.immocrm.dto.equipe.AffectationsEquipeDto.MembreAffectationDto;
import com.tsix.immocrm.dto.equipe.CommercialEquipeDto;
import com.tsix.immocrm.dto.equipe.EntonnoirDto;
import com.tsix.immocrm.dto.equipe.LeadActiviteDto;
import com.tsix.immocrm.dto.kpi.PerformanceCommercialDto;
import com.tsix.immocrm.dto.perimetre.ProjetPerimetreDto;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserRole;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.com.ComProspectionEquipeRepository;
import com.tsix.immocrm.repositories.com.ComProspectionEquipeRepository.ActiviteLeadLigne;
import com.tsix.immocrm.repositories.com.ComProspectionEquipeRepository.LeadCohorte;
import com.tsix.immocrm.repositories.com.ComProspectionEquipeRepository.PortefeuilleCommercial;
import com.tsix.immocrm.services.kpi.PerformanceCommercialService;
import com.tsix.immocrm.services.parcours.LignesParcours;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Lectures de l'écran « Mon équipe » ; {@link #perimetre()} est l'unique calcul de l'équipe, partagé avec
 * {@link EquipeEcrituresService} (FEATURE-equipe-commerciale.md §2).
 */
@Service
@Transactional(readOnly = true)
public class EquipeCommercialeService {

    // IN () est invalide : liste jamais lue quand tous = true, mais jamais vide.
    private static final List<UUID> AUCUN_PROJET = List.of(new UUID(0L, 0L));

    private final AdmUserRoleRepository userRoleRepository;
    private final AdmUserHabilitationRepository habilitationRepository;
    private final CfgProjectRepository projectRepository;
    private final ComProspectionEquipeRepository equipeRepository;
    private final PerformanceCommercialService performanceService;
    private final Clock clock;

    public EquipeCommercialeService(AdmUserRoleRepository userRoleRepository,
                                    AdmUserHabilitationRepository habilitationRepository,
                                    CfgProjectRepository projectRepository,
                                    ComProspectionEquipeRepository equipeRepository,
                                    PerformanceCommercialService performanceService,
                                    Clock clock) {
        this.userRoleRepository = userRoleRepository;
        this.habilitationRepository = habilitationRepository;
        this.projectRepository = projectRepository;
        this.equipeRepository = equipeRepository;
        this.performanceService = performanceService;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- périmètre

    /**
     * Rôles lus dans {@code AdmUserRole} (pas le jeton) ; projets limités aux sociétés actives. Un chef sans projet
     * a une équipe vide — jamais « tout », contrairement au périmètre de prospection.
     */
    public PerimetreEquipe perimetre() {
        UUID appelant = PerimetreCommercialService.admUserIdCourant()
                .orElseThrow(() -> new ForbiddenException("Compte applicatif non identifiable."));

        ComptesEtRoles lus = comptesEtRoles();
        Map<UUID, Set<String>> roles = lus.roles();
        boolean exempte = RolesExemptesPerimetre.exempte(roles.getOrDefault(appelant, Set.of()));

        List<CfgProject> actifs = projectRepository.findDesEntitesActives();
        Set<UUID> projetIds = new LinkedHashSet<>();
        Set<UUID> affectesAppelant = exempte ? Set.of() : new HashSet<>(habilitationRepository.findProjetIdsByAdmUserId(appelant));
        actifs.stream()
                .filter(p -> exempte || affectesAppelant.contains(p.getId()))
                .forEach(p -> projetIds.add(p.getId()));
        List<ProjetPerimetreDto> projets = actifs.stream()
                .filter(p -> projetIds.contains(p.getId()))
                .map(p -> new ProjetPerimetreDto(p.getId(), p.getCode(), p.getDescription()))
                .sorted(Comparator.comparing(p -> Objects.toString(p.code(), "")))
                .toList();

        List<MembreEquipe> candidats = candidats(lus.comptes(), roles, RolesEquipe::estMembrePossible);
        List<MembreEquipe> membres = candidats.stream()
                .filter(m -> exempte || m.projetIds().stream().anyMatch(projetIds::contains))
                .toList();
        // Un commercial sans restriction n'est à aucun chef : seul l'administrateur le restreint.
        List<MembreEquipe> horsEquipe = exempte ? List.of() : candidats.stream()
                .filter(m -> !membres.contains(m) && !m.estProtege() && !m.sansRestriction())
                .toList();
        return new PerimetreEquipe(appelant, exempte, projets, projetIds, membres, horsEquipe);
    }

    /**
     * Comptes actifs Commercial / Commercial Aide avec leurs projets, sans égard à l'appelant : même liste que
     * les membres possibles de {@link #perimetre()} (répartition automatique, FEATURE-repartition-notifications.md §3.1).
     */
    public List<MembreEquipe> membresActifs() {
        return comptesActifs(RolesEquipe::estMembrePossible);
    }

    /** Comptes actifs dont les rôles ({@code AdmUserRole}) satisfont {@code roles}, avec leurs projets, triés par nom. */
    public List<MembreEquipe> comptesActifs(Predicate<Set<String>> roles) {
        ComptesEtRoles lus = comptesEtRoles();
        return candidats(lus.comptes(), lus.roles(), roles);
    }

    private record ComptesEtRoles(Map<UUID, AdmUser> comptes, Map<UUID, Set<String>> roles) {
    }

    private ComptesEtRoles comptesEtRoles() {
        Map<UUID, AdmUser> comptes = new HashMap<>();
        Map<UUID, Set<String>> roles = new HashMap<>();
        for (AdmUserRole ur : userRoleRepository.findAllAvecCompteEtRole()) {
            comptes.put(ur.getAdmUser().getId(), ur.getAdmUser());
            if (ur.getAdmRole().getCode() != null) {
                roles.computeIfAbsent(ur.getAdmUser().getId(), k -> new HashSet<>()).add(ur.getAdmRole().getCode());
            }
        }
        return new ComptesEtRoles(comptes, roles);
    }

    /** Comptes actifs portant un rôle retenu par {@code retenus}, triés par nom. */
    private List<MembreEquipe> candidats(Map<UUID, AdmUser> comptes, Map<UUID, Set<String>> roles,
                                         Predicate<Set<String>> retenus) {
        List<AdmUser> actifs = comptes.values().stream()
                .filter(u -> !Boolean.TRUE.equals(u.getDisabled()))
                .filter(u -> retenus.test(roles.getOrDefault(u.getId(), Set.of())))
                .toList();
        if (actifs.isEmpty()) {
            return List.of();
        }
        Map<UUID, Set<UUID>> projetsParCompte = new HashMap<>();
        for (Object[] ligne : habilitationRepository.findProjetIdsByAdmUserIdIn(actifs.stream().map(AdmUser::getId).toList())) {
            projetsParCompte.computeIfAbsent((UUID) ligne[0], k -> new HashSet<>()).add((UUID) ligne[1]);
        }
        return actifs.stream()
                .map(u -> new MembreEquipe(u.getId(), u.getCode(), nom(u),
                        u.getCfgEmployee() != null ? u.getCfgEmployee().getId() : null,
                        Set.copyOf(roles.getOrDefault(u.getId(), Set.of())),
                        Set.copyOf(projetsParCompte.getOrDefault(u.getId(), Set.of()))))
                .sorted(Comparator.comparing(m -> Objects.toString(m.nom(), "")))
                .toList();
    }

    static String nom(AdmUser u) {
        return StringUtils.hasText(u.getDescription()) ? u.getDescription().trim() : u.getCode();
    }

    // ---------------------------------------------------------------- lectures

    public EntonnoirDto entonnoir(@Nullable LocalDate du, @Nullable LocalDate au, @Nullable UUID projet) {
        PeriodeEquipe periode = PeriodeEquipe.resoudre(du, au, LocalDate.now(clock));
        List<UUID> projets = perimetre().projetsFiltres(projet);
        List<LeadCohorte> leads = List.of();
        if (projets == null || !projets.isEmpty()) {
            leads = equipeRepository.cohorte(periode.debut(), periode.fin(), InteractionKpi.TYPE_VISITE,
                    projets == null, pourRequete(projets));
        }
        EntonnoirCohorte.Resultat resultat = EntonnoirCohorte.calculer(leads);
        return new EntonnoirDto(periode.du(), periode.au(), projet, resultat.etapes(), resultat.delaiMedianJours());
    }

    public List<CommercialEquipeDto> commerciaux(@Nullable LocalDate du, @Nullable LocalDate au, @Nullable UUID projet) {
        // 400 avant tout : une équipe vide ne doit pas masquer une période invalide.
        PeriodeEquipe.resoudre(du, au, LocalDate.now(clock));
        PerimetreEquipe perimetre = perimetre();
        List<UUID> projets = perimetre.projetsFiltres(projet);
        List<MembreEquipe> membres = perimetre.membres().stream()
                .filter(m -> projet == null || m.peutRecevoir(projet))
                .toList();
        if (membres.isEmpty() || (projets != null && projets.isEmpty())) {
            return List.of();
        }
        Map<UUID, PortefeuilleCommercial> portefeuilles = portefeuilles(membres, projets);
        List<PerformanceCommercialDto> performances = performanceService.pourMembres(
                membres.stream().map(m -> new PerformanceCommercialService.Membre(m.employeId(), m.admUserId())).toList(),
                du, au, projets);
        Map<UUID, ProjetPerimetreDto> projetsParId = perimetre.projets().stream()
                .collect(Collectors.toMap(ProjetPerimetreDto::id, Function.identity()));

        List<CommercialEquipeDto> lignes = new ArrayList<>();
        for (int i = 0; i < membres.size(); i++) {
            MembreEquipe m = membres.get(i);
            PortefeuilleCommercial pf = m.employeId() != null ? portefeuilles.get(m.employeId()) : null;
            lignes.add(new CommercialEquipeDto(m.admUserId(), m.code(), m.nom(), m.employeId(),
                    projetsVisibles(m, projetsParId),
                    pf != null ? valeur(pf.getOuvertes()) : 0L,
                    pf != null ? valeur(pf.getSansAction()) : 0L,
                    pf != null ? valeur(pf.getRelancesEnRetard()) : 0L,
                    performances.get(i)));
        }
        return lignes;
    }

    private Map<UUID, PortefeuilleCommercial> portefeuilles(List<MembreEquipe> membres, @Nullable List<UUID> projets) {
        List<UUID> employes = membres.stream().map(MembreEquipe::employeId).filter(Objects::nonNull).distinct().toList();
        if (employes.isEmpty()) {
            return Map.of();
        }
        LocalDateTime maintenant = LocalDateTime.now(clock);
        Map<UUID, PortefeuilleCommercial> parEmploye = new HashMap<>();
        for (PortefeuilleCommercial pf : equipeRepository.portefeuilles(employes, ActiviteLead.seuil(maintenant),
                maintenant.toLocalDate().atStartOfDay(), projets == null, pourRequete(projets))) {
            parEmploye.put(pf.getCommercialId(), pf);
        }
        return parEmploye;
    }

    /** Activité des leads demandés ; un id hors du périmètre d'équipe ou inconnu est omis, sans erreur. */
    public List<LeadActiviteDto> activiteLeads(List<UUID> ids) {
        PerimetreEquipe perimetre = perimetre();
        List<UUID> projets = perimetre.projetsFiltres(null);
        List<UUID> membres = perimetre.membres().stream()
                .map(MembreEquipe::employeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        return equipeRepository.activite(List.copyOf(new LinkedHashSet<>(ids)), projets == null, pourRequete(projets),
                        membres.isEmpty() ? AUCUN_PROJET : membres).stream()
                .map(l -> versActivite(l, maintenant))
                .toList();
    }

    private static LeadActiviteDto versActivite(ActiviteLeadLigne l, LocalDateTime maintenant) {
        LocalDateTime reference = ActiviteLead.reference(l.getDerniereAction(), l.getDateLead());
        long nombreActions = valeur(l.getNombreActions());
        return new LeadActiviteDto(l.getId(), (int) nombreActions, l.getDerniereAction(),
                ActiviteLead.joursSansAction(reference, maintenant), ActiviteLead.endormi(reference, maintenant),
                LignesParcours.nonTraitee(nombreActions, l.getEtapeCode()));
    }

    private static List<ProjetPerimetreDto> projetsVisibles(MembreEquipe m, Map<UUID, ProjetPerimetreDto> projetsParId) {
        return m.projetIds().stream()
                .map(projetsParId::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(p -> Objects.toString(p.code(), "")))
                .toList();
    }

    /** {@code null} (sans restriction) ⇒ la liste factice : jamais lue, mais {@code IN ()} serait invalide. */
    private static List<UUID> pourRequete(@Nullable List<UUID> projets) {
        return projets == null ? AUCUN_PROJET : projets;
    }

    private static long valeur(Integer n) {
        return n != null ? n : 0L;
    }

    public AffectationsEquipeDto affectations() {
        PerimetreEquipe perimetre = perimetre();
        return new AffectationsEquipeDto(perimetre.projets(),
                perimetre.membres().stream().map(m -> versAffectation(m, perimetre)).toList(),
                perimetre.horsEquipe().stream().map(m -> versAffectation(m, perimetre)).toList());
    }

    /** Projets limités au périmètre de l'appelant : un chef ne voit pas les affectations d'un autre chef. */
    static MembreAffectationDto versAffectation(MembreEquipe m, PerimetreEquipe perimetre) {
        List<UUID> projetIds = perimetre.projets().stream()
                .map(ProjetPerimetreDto::id)
                .filter(m.projetIds()::contains)
                .toList();
        return new MembreAffectationDto(m.admUserId(), m.code(), m.nom(), m.employeId(), projetIds,
                m.sansRestriction(), perimetre.exempte() || !m.estProtege());
    }
}
