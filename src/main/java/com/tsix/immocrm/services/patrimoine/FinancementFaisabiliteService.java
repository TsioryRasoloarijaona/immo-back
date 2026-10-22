package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.faisabilite.FinancementFaisabilite;
import com.tsix.immocrm.domain.faisabilite.FinancementFaisabilite.Echeance;
import com.tsix.immocrm.dto.patrimoine.faisabilite.EcheancierDemandeDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.FinancementFaisabiliteDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.RemboursementFaisabiliteDto;
import com.tsix.immocrm.entities.stk.FSFunding;
import com.tsix.immocrm.entities.stk.FSReimbursement;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudy;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.stk.StkFeasibilityStudyRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tsix.immocrm.domain.faisabilite.BilanFaisabilite.r;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.positif;

/** Crédits et remboursements d'une étude (FEATURE-patrimoine-faisabilite-workflow.md §1) ; écriture = recalcul §3. */
@Service
@Transactional(readOnly = true)
public class FinancementFaisabiliteService {

    private final StkFeasibilityStudyRepository etudes;
    private final RecalculFaisabiliteService recalcul;
    private final TracePatrimoine trace;
    private final EntityManager em;

    public FinancementFaisabiliteService(StkFeasibilityStudyRepository etudes, RecalculFaisabiliteService recalcul,
                                         TracePatrimoine trace, EntityManager em) {
        this.etudes = etudes;
        this.recalcul = recalcul;
        this.trace = trace;
        this.em = em;
    }

    public List<FinancementFaisabiliteDto> lister(UUID etudeId) {
        etudes.exigerVisible(etudeId);
        return dtos(etudeId);
    }

    /** Patron P1b imbriqué : tout est contrôlé avant la première écriture. */
    @Transactional
    public List<FinancementFaisabiliteDto> modifierFinancements(UUID etudeId, List<FinancementFaisabiliteDto> credits) {
        StkFeasibilityStudy e = FicheEtude.modifiable(etudes, etudeId);
        List<FinancementFaisabiliteDto> saisis = Objects.requireNonNullElse(credits, List.of());
        Map<FSFunding, List<FSReimbursement>> existants = recalcul.financements(etudeId);
        Map<UUID, FSFunding> parId = existants.keySet().stream().collect(Collectors.toMap(FSFunding::getId, Function.identity()));
        saisis.forEach(c -> valider(c, parId, existants));
        Set<UUID> gardes = saisis.stream().map(FinancementFaisabiliteDto::id).filter(Objects::nonNull).collect(Collectors.toSet());
        existants.forEach((credit, remboursements) -> {
            if (!gardes.contains(credit.getId())) {
                remboursements.forEach(em::remove);
                em.remove(credit);
            }
        });
        em.flush();
        TracePatrimoine.Trace t = trace.maintenant();
        saisis.forEach(c -> ecrire(etudeId, c, parId.get(c.id()), existants, t));
        em.flush();
        recalcul.recalculer(e);
        return dtos(etudeId);
    }

    /** Bouton « Générer » : aucune écriture. */
    public List<RemboursementFaisabiliteDto> echeancier(UUID etudeId, EcheancierDemandeDto d) {
        etudes.exigerVisible(etudeId);
        exigerDate(d.dateDeblocage(), "dateDeblocage");
        if (d.nombreRemboursements() == null || d.nombreRemboursements() <= 0) {
            throw new InvalidRequestException("nombreRemboursements : au moins un remboursement.");
        }
        BigDecimal montant = Objects.requireNonNullElse(positif(d.montant(), "montant"), BigDecimal.ZERO);
        return FinancementFaisabilite.echeancier(d.dateDeblocage(), montant, d.nombreRemboursements(), d.periodiciteMois(),
                d.periodeGraceMois(), Objects.requireNonNullElse(positif(d.tauxCharge(), "tauxCharge"), BigDecimal.ZERO))
                .stream().map(x -> dto(null, x)).toList();
    }

    // ------------------------------------------------------------------ contrôles

    private static void valider(FinancementFaisabiliteDto c, Map<UUID, FSFunding> parId,
                                Map<FSFunding, List<FSReimbursement>> existants) {
        if (c.id() != null && !parId.containsKey(c.id())) {
            throw new InvalidRequestException("id : crédit inconnu dans cette étude (%s).".formatted(c.id()));
        }
        exigerDate(c.dateDeblocage(), "dateDeblocage");
        positif(c.montant(), "montant");
        SaisiePatrimoine.positif(c.nombreRemboursements(), "nombreRemboursements");
        positif(c.tauxCharge(), "tauxCharge");
        Set<UUID> connus = c.id() == null ? Set.of() : existants.get(parId.get(c.id())).stream()
                .map(FSReimbursement::getId).collect(Collectors.toSet());
        Objects.requireNonNullElse(c.remboursements(), List.<RemboursementFaisabiliteDto>of()).forEach(x -> {
            if (x.id() != null && !connus.contains(x.id())) {
                throw new InvalidRequestException("remboursements.id : remboursement inconnu de ce crédit (%s).".formatted(x.id()));
            }
            exigerDate(x.date(), "remboursements.date");
            positif(x.montantBase(), "remboursements.montantBase");
            positif(x.tauxInteret(), "remboursements.tauxInteret");
            positif(x.montantInterets(), "remboursements.montantInterets");
        });
    }

    private static void exigerDate(LocalDateTime date, String champ) {
        if (date == null) {
            throw new InvalidRequestException(champ + " est obligatoire.");
        }
    }

    // ------------------------------------------------------------------ écriture

    private void ecrire(UUID etudeId, FinancementFaisabiliteDto c, FSFunding existant,
                        Map<FSFunding, List<FSReimbursement>> existants, TracePatrimoine.Trace t) {
        FSFunding credit = existant != null ? existant : new FSFunding();
        boolean nouveau = existant == null;
        LignesTracees.appliquer(credit, f -> Arrays.asList(f.getDescription(), f.getPaymentDate(), f.getAmount(),
                f.getMonthsCount(), f.getMonthsStep(), f.getGracePeriod(), f.getChargeRatio()), f -> {
            f.setFeasibilityStudyId(etudeId);
            f.setDescription(Objects.requireNonNullElse(c.description(), ""));
            f.setPaymentDate(c.dateDeblocage());
            f.setAmount(r(Objects.requireNonNullElse(c.montant(), BigDecimal.ZERO)));
            f.setMonthsCount(Objects.requireNonNullElse(c.nombreRemboursements(), 0));
            f.setMonthsStep(FinancementFaisabilite.pas(c.periodiciteMois()));
            f.setGracePeriod(Objects.requireNonNullElse(c.periodeGraceMois(), 0));
            f.setChargeRatio(r(Objects.requireNonNullElse(c.tauxCharge(), BigDecimal.ZERO)));
        }, t);
        if (nouveau) {
            em.persist(credit);
        }
        List<FSReimbursement> anciens = nouveau ? List.of() : existants.get(existant);
        Map<UUID, FSReimbursement> parId = anciens.stream().collect(Collectors.toMap(FSReimbursement::getId, Function.identity()));
        List<RemboursementFaisabiliteDto> saisis = Objects.requireNonNullElse(c.remboursements(), List.of());
        Set<UUID> gardes = saisis.stream().map(RemboursementFaisabiliteDto::id).filter(Objects::nonNull).collect(Collectors.toSet());
        anciens.stream().filter(x -> !gardes.contains(x.getId())).forEach(em::remove);
        saisis.forEach(x -> ecrire(credit.getId(), x, parId.get(x.id()), t));
    }

    /** Taux gagnant ; {@code montantARegler} toujours recalculé. */
    private void ecrire(UUID creditId, RemboursementFaisabiliteDto x, FSReimbursement existant, TracePatrimoine.Trace t) {
        FSReimbursement remboursement = existant != null ? existant : new FSReimbursement();
        Echeance e = FinancementFaisabilite.echeance(x.date(), x.montantBase(), x.tauxInteret(), x.montantInterets());
        LignesTracees.appliquer(remboursement, l -> Arrays.asList(l.getPaymentDate(), l.getAmountBase(), l.getInterestRate(),
                l.getAmountInterest(), l.getAmountToPay()), l -> {
            l.setFsFundingId(creditId);
            l.setPaymentDate(e.date());
            l.setAmountBase(r(e.base()));
            l.setInterestRate(r(e.taux()));
            l.setAmountInterest(r(e.interets()));
            l.setAmountToPay(r(e.aRegler()));
        }, t);
        if (existant == null) {
            em.persist(remboursement);
        }
    }

    // ------------------------------------------------------------------ lecture

    private List<FinancementFaisabiliteDto> dtos(UUID etudeId) {
        return recalcul.financements(etudeId).entrySet().stream().map(c -> {
            FSFunding f = c.getKey();
            List<RemboursementFaisabiliteDto> remboursements = c.getValue().stream().map(x -> dto(x.getId(),
                    new Echeance(x.getPaymentDate(), x.getAmountBase(), x.getInterestRate(), x.getAmountInterest(),
                            x.getAmountToPay()))).toList();
            BigDecimal bases = somme(remboursements, RemboursementFaisabiliteDto::montantBase);
            return new FinancementFaisabiliteDto(f.getId(), f.getDescription(), f.getPaymentDate(), f.getAmount(),
                    f.getMonthsCount(), f.getMonthsStep(), f.getGracePeriod(), f.getChargeRatio(),
                    somme(remboursements, RemboursementFaisabiliteDto::montantARegler),
                    somme(remboursements, RemboursementFaisabiliteDto::montantInterets),
                    bases.subtract(Objects.requireNonNullElse(f.getAmount(), BigDecimal.ZERO)), remboursements);
        }).toList();
    }

    private static RemboursementFaisabiliteDto dto(UUID id, Echeance e) {
        return new RemboursementFaisabiliteDto(id, e.date(), r(e.base()), r(e.taux()), r(e.interets()), r(e.aRegler()));
    }

    private static BigDecimal somme(List<RemboursementFaisabiliteDto> lignes, Function<RemboursementFaisabiliteDto, BigDecimal> champ) {
        return lignes.stream().map(champ).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
