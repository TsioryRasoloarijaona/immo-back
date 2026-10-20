package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.faisabilite.ValeursFaisabilite;
import com.tsix.immocrm.domain.faisabilite.VerifFaisabilite;
import com.tsix.immocrm.domain.faisabilite.VerifFaisabilite.Surface;
import com.tsix.immocrm.dto.patrimoine.faisabilite.LigneTrancheEtudeDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.LigneUniteEtudeDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.VentilationEtudeDto;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.SpecificFunction;
import com.tsix.immocrm.entities.stk.StkFSBaseUnit;
import com.tsix.immocrm.entities.stk.StkFSBaseUnitStkFeasibilityStudy;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudy;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudyCfgTranche;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudyCfgTrancheStkBaseUnit;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import com.tsix.immocrm.entities.LigneOmran;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Grilles d'une étude (unités de base, tranches, ventilations) et leur Verif (FEATURE-patrimoine-faisabilite.md §2.4,
 * §2.5, §8 bis.8) : la saisie est contrôlée en entier sur l'état résultant avant qu'une ligne ne change.
 */
@Component
class GrillesEtude {

    private static final String ETUDE = "etude";
    private static final String LIGNES = "SELECT l FROM StkFSBaseUnitStkFeasibilityStudy l "
            + "WHERE l.feasibilityStudy.id = :etude ORDER BY l.sorting, l.id";
    private static final String TRANCHES = "SELECT t FROM StkFeasibilityStudyCfgTranche t "
            + "WHERE t.feasibilityStudy.id = :etude ORDER BY t.sorting, t.code";
    private static final String VENTILATIONS = "SELECT v FROM StkFeasibilityStudyCfgTrancheStkBaseUnit v "
            + "WHERE v.feasibilityStudyId = :etude";
    private static final String TRANCHES_SOCIETE = "SELECT t FROM CfgTranche t JOIN t.cfgProject pr JOIN pr.cfgCompany soc "
            + "WHERE soc.id = :societe AND soc.isActive = true";

    private final EntityManager em;
    private final TracePatrimoine trace;

    GrillesEtude(EntityManager em, TracePatrimoine trace) {
        this.em = em;
        this.trace = trace;
    }

    // ------------------------------------------------------------------ lecture

    List<StkFSBaseUnitStkFeasibilityStudy> lignes(UUID etudeId) {
        return em.createQuery(LIGNES, StkFSBaseUnitStkFeasibilityStudy.class).setParameter(ETUDE, etudeId).getResultList();
    }

    List<StkFeasibilityStudyCfgTranche> tranches(UUID etudeId) {
        return em.createQuery(TRANCHES, StkFeasibilityStudyCfgTranche.class).setParameter(ETUDE, etudeId).getResultList();
    }

    List<StkFeasibilityStudyCfgTrancheStkBaseUnit> ventilations(UUID etudeId) {
        return em.createQuery(VENTILATIONS, StkFeasibilityStudyCfgTrancheStkBaseUnit.class).setParameter(ETUDE, etudeId)
                .getResultList();
    }

    /** Tranches visibles de la société, par code (liste de choix et contrôle des lignes). */
    List<CfgTranche> tranchesDeLaSociete(UUID societeId) {
        return em.createQuery(TRANCHES_SOCIETE + " ORDER BY t.code", CfgTranche.class).setParameter("societe", societeId)
                .getResultList();
    }

    List<LigneUniteEtudeDto> unitesDto(UUID etudeId) {
        return lignes(etudeId).stream().map(l -> new LigneUniteEtudeDto(l.getId(), l.getSorting(),
                Champs.de(l.getStkFSBaseUnit(), StkFSBaseUnit::getId), Champs.de(l.getStkFSBaseUnit(), StkFSBaseUnit::getCode),
                Champs.de(l.getStkFSBaseUnit(), StkFSBaseUnit::getDescription), l.getQuantity())).toList();
    }

    List<LigneTrancheEtudeDto> tranchesDto(UUID etudeId) {
        Map<UUID, List<VentilationEtudeDto>> parTranche = ventilations(etudeId).stream().collect(Collectors.groupingBy(
                v -> v.getTranche().getId(), Collectors.mapping(v -> new VentilationEtudeDto(v.getId(),
                        Champs.de(v.getLigneUnite(), StkFSBaseUnitStkFeasibilityStudy::getId), v.getQuantity(), v.getWeight()),
                        Collectors.toList())));
        return tranches(etudeId).stream().map(t -> new LigneTrancheEtudeDto(t.getId(), t.getCode(), t.getSorting(),
                Champs.de(t.getCfgTranche(), CfgTranche::getId), Champs.de(t.getCfgTranche(), CfgTranche::getDescription),
                t.getWeight(), t.getWorkStartDate(), t.getWorkEndDate(), t.getMarketingStartDate(), t.getMarketingEndDate(),
                t.getEndDateConcretisation(), t.getStartDateFinalisation(), t.getDeliveryDate(), t.getOccupancyPermitsDate(),
                parTranche.getOrDefault(t.getId(), List.of()))).toList();
    }

    // ------------------------------------------------------------------ Verif

    /** Verif sur les grilles enregistrées et les valeurs {@code v} de l'étude (PATCH, Bloquer). */
    List<String> erreursEnBase(UUID etudeId, ValeursFaisabilite v) {
        List<StkFeasibilityStudyCfgTranche> tranches = tranches(etudeId);
        return erreurs(v, lignes(etudeId), tranches.stream().map(StkFeasibilityStudyCfgTranche::getWeight).toList(),
                ventile(ventilations(etudeId)));
    }

    /** Verif de l'aperçu : grilles du corps seulement ; une ventilation désigne sa ligne par l'id saisi (provisoire). */
    List<String> erreursSaisies(ValeursFaisabilite v, List<LigneUniteEtudeDto> unites, List<LigneTrancheEtudeDto> tranches) {
        List<StkFSBaseUnitStkFeasibilityStudy> lignes = Objects.requireNonNullElse(unites, List.<LigneUniteEtudeDto>of())
                .stream().map(u -> ligneProvisoire(u, Objects.requireNonNullElseGet(u.id(), UUID::randomUUID))).toList();
        List<LigneTrancheEtudeDto> saisies = Objects.requireNonNullElse(tranches, List.of());
        Map<UUID, BigDecimal> ventile = new HashMap<>();
        saisies.forEach(t -> Objects.requireNonNullElse(t.unites(), List.<VentilationEtudeDto>of()).stream()
                .filter(x -> x.uniteEtudeId() != null)
                .forEach(x -> ventile.merge(x.uniteEtudeId(), z(x.quantite()), BigDecimal::add)));
        return erreurs(v, lignes, saisies.stream().map(LigneTrancheEtudeDto::poids).toList(), ventile);
    }

    static void exigerSansErreur(List<String> erreurs) {
        if (!erreurs.isEmpty()) {
            throw new InvalidRequestException(erreurs.get(0));
        }
    }

    private static List<String> erreurs(ValeursFaisabilite v, List<StkFSBaseUnitStkFeasibilityStudy> lignes,
                                        List<BigDecimal> poids, Map<UUID, BigDecimal> ventile) {
        return VerifFaisabilite.erreurs(v, lignes.stream().map(GrillesEtude::unite).toList(), poids, ventile);
    }

    /** Surfaces comptées : unité de surface portant une fonction spécifique non « total » ({@code VerifBaseUnits}). */
    private static VerifFaisabilite.Unite unite(StkFSBaseUnitStkFeasibilityStudy l) {
        StkFSBaseUnit u = l.getStkFSBaseUnit();
        SpecificFunction f = u.getSpecificFunction();
        Set<Surface> surfaces = EnumSet.noneOf(Surface.class);
        if (Boolean.TRUE.equals(u.getIsArea()) && f != null && !Boolean.TRUE.equals(f.getIsTotal())) {
            ajouterSi(surfaces, Surface.VENDABLE, f.getSaleAbleArea());
            ajouterSi(surfaces, Surface.BRUTE, f.getAreaBrut());
            ajouterSi(surfaces, Surface.NETTE, f.getAreaNet());
            ajouterSi(surfaces, Surface.EMPRISE, f.getAreaCos());
            ajouterSi(surfaces, Surface.COUVERTE, f.getCoveredArea());
        }
        return new VerifFaisabilite.Unite(l.getId(), u.getCode(), l.getQuantity(), surfaces);
    }

    private static void ajouterSi(Set<Surface> surfaces, Surface surface, Boolean drapeau) {
        if (Boolean.TRUE.equals(drapeau)) {
            surfaces.add(surface);
        }
    }

    private static Map<UUID, BigDecimal> ventile(List<StkFeasibilityStudyCfgTrancheStkBaseUnit> ventilations) {
        return ventilations.stream().filter(x -> x.getLigneUnite() != null).collect(Collectors.toMap(
                x -> x.getLigneUnite().getId(), x -> z(x.getQuantity()), BigDecimal::add));
    }

    // ------------------------------------------------------------------ PUT des unités

    /**
     * Liste complète (convention P1b) ; une ligne retirée part avec ses ventilations ({@code BeforeUpdate} l.3284-3287).
     * Verif sur l'état résultant, avant toute écriture.
     */
    void remplacerUnites(StkFeasibilityStudy etude, List<LigneUniteEtudeDto> saisies, ValeursFaisabilite v) {
        Map<UUID, StkFSBaseUnitStkFeasibilityStudy> existantes = parId(lignes(etude.getId()),
                StkFSBaseUnitStkFeasibilityStudy::getId);
        List<StkFSBaseUnitStkFeasibilityStudy> resultat = saisies.stream().map(s -> {
            if (s.id() != null && !existantes.containsKey(s.id())) {
                throw new InvalidRequestException("id : ligne d'unité inconnue dans cette étude (%s).".formatted(s.id()));
            }
            return ligneProvisoire(s, s.id() != null ? s.id() : UUID.randomUUID());
        }).toList();
        Set<UUID> gardees = resultat.stream().map(StkFSBaseUnitStkFeasibilityStudy::getId).collect(Collectors.toSet());
        List<StkFeasibilityStudyCfgTrancheStkBaseUnit> ventilations = ventilations(etude.getId());
        exigerSansErreur(erreurs(v, resultat, tranches(etude.getId()).stream().map(StkFeasibilityStudyCfgTranche::getWeight)
                .toList(), ventile(ventilations.stream()
                .filter(x -> x.getLigneUnite() != null && gardees.contains(x.getLigneUnite().getId())).toList())));

        ventilations.stream().filter(x -> x.getLigneUnite() == null || !gardees.contains(x.getLigneUnite().getId()))
                .forEach(em::remove);
        existantes.values().stream().filter(l -> !gardees.contains(l.getId())).forEach(em::remove);
        em.flush();
        resultat.forEach(l -> {
            StkFSBaseUnitStkFeasibilityStudy cible = existantes.get(l.getId());
            if (cible == null) {
                l.setFeasibilityStudy(etude);
                em.persist(l);
            } else {
                cible.setSorting(l.getSorting());
                cible.setStkFSBaseUnit(l.getStkFSBaseUnit());
                cible.setQuantity(l.getQuantity());
            }
        });
    }

    /** Ligne non attachée à la session : la saisie contrôlée ; {@code uniteBaseId} inconnu → 400. */
    private StkFSBaseUnitStkFeasibilityStudy ligneProvisoire(LigneUniteEtudeDto s, UUID id) {
        if (s.uniteBaseId() == null) {
            throw new InvalidRequestException("uniteBaseId est obligatoire.");
        }
        StkFSBaseUnitStkFeasibilityStudy l = new StkFSBaseUnitStkFeasibilityStudy();
        l.setId(id);
        l.setSorting(s.sorting());
        l.setQuantity(s.quantite());
        l.setStkFSBaseUnit(ReferencesPatrimoine.trouver(i -> Optional.ofNullable(em.find(StkFSBaseUnit.class, i)),
                s.uniteBaseId(), "uniteBaseId"));
        return l;
    }

    // ------------------------------------------------------------------ PUT des tranches

    /** Tranche saisie et ses ventilations, résolues et contrôlées. */
    private record TrancheSaisie(LigneTrancheEtudeDto saisie, CfgTranche tranche,
                                 List<Map.Entry<StkFSBaseUnitStkFeasibilityStudy, VentilationEtudeDto>> ventilations) {
    }

    /** Liste complète ; les ventilations des tranches gardées sont recréées. Verif avant toute écriture. */
    void remplacerTranches(StkFeasibilityStudy etude, List<LigneTrancheEtudeDto> saisies, ValeursFaisabilite v) {
        UUID etudeId = etude.getId();
        Map<UUID, StkFeasibilityStudyCfgTranche> existantes = parId(tranches(etudeId), StkFeasibilityStudyCfgTranche::getId);
        List<StkFSBaseUnitStkFeasibilityStudy> lignes = lignes(etudeId);
        Map<UUID, StkFSBaseUnitStkFeasibilityStudy> lignesParId = parId(lignes, StkFSBaseUnitStkFeasibilityStudy::getId);
        Set<UUID> tranchesSociete = tranchesDeLaSociete(etude.getCfgCompany().getId()).stream().map(CfgTranche::getId)
                .collect(Collectors.toSet());
        Set<String> codes = new HashSet<>();
        List<TrancheSaisie> resultat = saisies.stream()
                .map(s -> controler(s, existantes, lignesParId, tranchesSociete, codes)).toList();
        Map<UUID, BigDecimal> ventile = new HashMap<>();
        resultat.forEach(t -> t.ventilations().forEach(x -> ventile.merge(x.getKey().getId(), z(x.getValue().quantite()),
                BigDecimal::add)));
        exigerSansErreur(erreurs(v, lignes, saisies.stream().map(LigneTrancheEtudeDto::poids).toList(), ventile));

        ventilations(etudeId).forEach(em::remove);
        Set<UUID> gardees = saisies.stream().map(LigneTrancheEtudeDto::id).filter(Objects::nonNull).collect(Collectors.toSet());
        existantes.values().stream().filter(t -> !gardees.contains(t.getId())).forEach(em::remove);
        em.flush();
        TracePatrimoine.Trace t = trace.maintenant();
        resultat.forEach(r -> ecrireTranche(etude, r, existantes.get(r.saisie().id()), t));
    }

    private TrancheSaisie controler(LigneTrancheEtudeDto s, Map<UUID, StkFeasibilityStudyCfgTranche> existantes,
                                    Map<UUID, StkFSBaseUnitStkFeasibilityStudy> lignes, Set<UUID> tranchesSociete,
                                    Set<String> codes) {
        if (s.id() != null && !existantes.containsKey(s.id())) {
            throw new InvalidRequestException("id : tranche inconnue dans cette étude (%s).".formatted(s.id()));
        }
        String code = SaisiePatrimoine.obligatoire(s.code(), null, "Le code de la tranche");
        if (s.sorting() == null) {
            throw new InvalidRequestException("sorting est obligatoire sur chaque tranche.");
        }
        if (!codes.add(code.toLowerCase(Locale.ROOT))) {
            throw new ConflictException("Le code de tranche %s est en double dans l'étude.".formatted(code));
        }
        CfgTranche tranche = null;
        if (s.trancheId() != null) {
            if (!tranchesSociete.contains(s.trancheId())) {
                throw new InvalidRequestException("trancheId : tranche inconnue ou d'une autre société (%s)."
                        .formatted(s.trancheId()));
            }
            tranche = em.find(CfgTranche.class, s.trancheId());
        }
        List<Map.Entry<StkFSBaseUnitStkFeasibilityStudy, VentilationEtudeDto>> ventilations =
                Objects.requireNonNullElse(s.unites(), List.<VentilationEtudeDto>of()).stream().map(x -> {
                    StkFSBaseUnitStkFeasibilityStudy ligne = x.uniteEtudeId() == null ? null : lignes.get(x.uniteEtudeId());
                    if (ligne == null) {
                        throw new InvalidRequestException("unites.uniteEtudeId : ligne d'unité inconnue dans cette étude ("
                                + x.uniteEtudeId() + ").");
                    }
                    return Map.entry(ligne, x);
                }).toList();
        return new TrancheSaisie(s, tranche, ventilations);
    }

    private void ecrireTranche(StkFeasibilityStudy etude, TrancheSaisie r, StkFeasibilityStudyCfgTranche existante,
                               TracePatrimoine.Trace t) {
        LigneTrancheEtudeDto s = r.saisie();
        StkFeasibilityStudyCfgTranche cible = existante != null ? existante : new StkFeasibilityStudyCfgTranche();
        cible.setCode(s.code().trim());
        cible.setSorting(s.sorting());
        cible.setCfgTranche(r.tranche());
        cible.setWeight(s.poids());
        cible.setWorkStartDate(s.debutTravaux());
        cible.setWorkEndDate(s.finTravaux());
        cible.setMarketingStartDate(s.debutCommercialisation());
        cible.setMarketingEndDate(s.finCommercialisation());
        cible.setEndDateConcretisation(s.finConcretisation());
        cible.setStartDateFinalisation(s.debutFinalisation());
        cible.setDeliveryDate(s.dateLivraison());
        cible.setOccupancyPermitsDate(s.datePermisHabiter());
        tracer(cible, t, existante == null);
        if (existante == null) {
            cible.setId(UUID.randomUUID());
            cible.setFeasibilityStudy(etude);
            em.persist(cible);
        }
        r.ventilations().forEach(x -> {
            StkFeasibilityStudyCfgTrancheStkBaseUnit ventilation = new StkFeasibilityStudyCfgTrancheStkBaseUnit();
            ventilation.setId(UUID.randomUUID());
            ventilation.setFeasibilityStudyId(etude.getId());
            ventilation.setTranche(cible);
            ventilation.setLigneUnite(x.getKey());
            ventilation.setQuantity(x.getValue().quantite());
            ventilation.setWeight(x.getValue().poids());
            tracer(ventilation, t, true);
            em.persist(ventilation);
        });
    }

    private static void tracer(LigneOmran ligne, TracePatrimoine.Trace t, boolean creation) {
        if (creation) {
            ligne.setUserCreate(t.auteur());
            ligne.setUserCreateDateTime(t.le());
        }
        ligne.setUserUpdate(t.auteur());
        ligne.setUserUpdateDateTime(t.le());
    }

    // ------------------------------------------------------------------ suppression de l'étude

    /** Enfants d'abord : chaque ligne passe par une entité (audit et rapatriement), l'historique compris (§8 ter.8). */
    void supprimerTout(UUID etudeId) {
        ventilations(etudeId).forEach(em::remove);
        tranches(etudeId).forEach(em::remove);
        lignes(etudeId).forEach(em::remove);
        em.createQuery("SELECT h FROM StkFeasibilityStudyStatusHistory h WHERE h.feasibilityStudyId = :etude", Object.class)
                .setParameter(ETUDE, etudeId).getResultList().forEach(em::remove);
        em.flush();
    }

    private static <T> Map<UUID, T> parId(List<T> lignes, Function<T, UUID> id) {
        return lignes.stream().collect(Collectors.toMap(id, l -> l, (a, b) -> a, HashMap::new));
    }

    private static BigDecimal z(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
