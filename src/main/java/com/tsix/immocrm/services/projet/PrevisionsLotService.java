package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.config.audit.EcouteurAuditOmran;
import com.tsix.immocrm.domain.projet.CalculLot;
import com.tsix.immocrm.domain.projet.CourbePrevisions;
import com.tsix.immocrm.domain.projet.MontantsLot;
import com.tsix.immocrm.dto.projet.GeneralLotDto;
import com.tsix.immocrm.dto.projet.PointSaisiDto;
import com.tsix.immocrm.dto.projet.PointSaisieDto;
import com.tsix.immocrm.dto.projet.PrevisionsLotDto;
import com.tsix.immocrm.dto.projet.PrevisionsSaisieLotDto;
import com.tsix.immocrm.dto.projet.PrevisionsSaisieRequest;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.droits.DroitsOmran;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.tsix.immocrm.services.projet.DecomptesProjetService.jour;
import static com.tsix.immocrm.services.projet.FacturesDecomptes.date;
import static com.tsix.immocrm.services.projet.FacturesDecomptes.montant;
import static com.tsix.immocrm.services.projet.FacturesDecomptes.uuid;
import static java.math.BigDecimal.ZERO;

/** Saisie et enregistrement des prévisions d'un lot (FEATURE-lot-previsions.md §4.2-4.3, R1-R7, R10). */
@Service
public class PrevisionsLotService {

    private static final String DE_BASE = " de base";
    private static final String FIN_AVANT_DEBUT = "La date fin des prévisions%s doit être supérieure ou égale à la date début !";
    private static final String DEUX_LIGNES = "Vous devez saisir au moins deux lignes de prévisions avec obligatoirement"
            + " un pourcentage d'avancement allant de 0% et 100%";
    // Textes OMRAN gardés tels quels, fautes comprises (Simulation.Tresor/Market.cs:Verif).
    private static final String APRES_FIN = "La date fin des prévisions%s doit inférieure ou égale à la date fin";
    private static final String AVANT_DEBUT = "La date début des prévisions%s doit être supérieure ou égale à la date fin";
    private static final String DECROISSANT = "Veuillez vérifier l'avancement dans la prevision du date %s !";
    private static final DateTimeFormatter JJ_MM_AAAA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String MAJ_PREVISIONS = "UPDATE PrjMarket SET StartDate = :debut, Delay = :duree, EndDate = :fin"
            + " WHERE PKey = :lot";
    private static final String MAJ_DATES_BASE = "UPDATE PrjMarket SET StartDateManual = :debut, DelayManual = :duree,"
            + " EndDateManual = :fin WHERE PKey = :lot";
    private static final String MAJ_MONTANT_BASE = "UPDATE PrjMarket SET EstimatedPriceHTManual = :ht,"
            + " EstimatedVatAmountManual = :tva, EstimatedPriceTTCManual = :ttc WHERE PKey = :lot";

    private final LotsProjetService lots;
    private final DroitsOmran droits;
    private final MontantsListeLots montants;
    private final EcouteurAuditOmran audit;
    private final PrevisionsPaiementLot previsions;
    private final SousFenetresLotService sousFenetres;
    private final EntityManager em;

    PrevisionsLotService(LotsProjetService lots, DroitsOmran droits, MontantsListeLots montants, EcouteurAuditOmran audit,
                         PrevisionsPaiementLot previsions, SousFenetresLotService sousFenetres, EntityManager em) {
        this.lots = lots;
        this.droits = droits;
        this.montants = montants;
        this.audit = audit;
        this.previsions = previsions;
        this.sousFenetres = sousFenetres;
        this.em = em;
    }

    /** Champs « de base » réellement changés par l'envoi (R2) : absent, identique ou valeur par défaut du front = inchangé. */
    private record Changements(boolean dates, boolean montant, boolean points) {
        boolean un() {
            return dates || montant || points;
        }
    }

    @Transactional(readOnly = true)
    public PrevisionsSaisieLotDto saisie(UUID id) {
        PrjMarket m = lots.visible(id);
        DroitsPrevisionsLot d = DroitsPrevisionsLot.de(droits).exigerSaisie();
        CalculLot calcul = calcul(m);
        GeneralLotDto g = calcul.general();
        LocalDate debut = jour(m.getStartDate());
        LocalDate debutBase = jour(m.getStartDateManual());
        return new PrevisionsSaisieLotDto(id, m.getDescription(), true, d.base(), debut, m.getDelay(), fin(debut, m.getDelay()),
                debutBase, m.getDelayManual(), fin(debutBase, m.getDelayManual()), m.getEstimatedPriceHTManual(),
                m.getEstimatedVatAmountManual(), m.getEstimatedPriceTTCManual(), MontantsLot.pourcentage(m.getDefaultVat()),
                g.estimationConfidentielle().ht(), g.estimationConfidentielle().tva(), g.estimationConfidentielle().ttc(),
                g.marcheAjuste().ht(), g.marcheAjuste().tva(), g.marcheAjuste().ttc(), calcul.montantsPrevisions().montant(),
                calcul.avancementFacture(), points(TablePoints.PREVISION, id), points(TablePoints.BASE, id));
    }

    /** Ordre : 404, 403 (R1 puis R2), 400 (R3, R4), puis écriture, recalcul (R8) et une ligne d'audit (R10). */
    @Transactional
    public PrevisionsLotDto enregistrer(UUID id, PrevisionsSaisieRequest corps) {
        PrjMarket m = lots.visible(id);
        DroitsPrevisionsLot d = DroitsPrevisionsLot.de(droits).exigerSaisie();
        PrevisionsSaisieRequest r = corps != null ? corps : new PrevisionsSaisieRequest(null, null, null, null, null, null, null);
        Changements c = changements(m, r);
        d.exigerBase(c.un());
        Periode previsionsLot = periode(r.debut(), r.duree(), "", "debut", "duree");
        Periode base = periode(r.debutBase(), r.dureeBase(), DE_BASE, "debutBase", "dureeBase");
        verifierPoints(r.points(), previsionsLot, "");
        verifierPoints(r.pointsBase(), base, DE_BASE);
        ecrire(MAJ_PREVISIONS, id, previsionsLot);
        if (c.dates()) {
            ecrire(MAJ_DATES_BASE, id, base);
        }
        BigDecimal ht = r.estimationBaseHT();
        if (c.montant() && ht != null) {
            BigDecimal tva = ht.multiply(Objects.requireNonNullElse(m.getDefaultVat(), ZERO));
            em.createNativeQuery(MAJ_MONTANT_BASE).setParameter("ht", ht).setParameter("tva", tva)
                    .setParameter("ttc", ht.add(tva)).setParameter("lot", id).executeUpdate();
        }
        em.refresh(m);
        if (r.points() != null) {
            remplacer(TablePoints.PREVISION, id, r.points(), previsionsLot, calcul(m).montantsPrevisions().montant());
        }
        // Points de base réécrits dès qu'un champ de base change : leurs temps et montants en dépendent.
        if (r.pointsBase() != null && c.un()) {
            remplacer(TablePoints.BASE, id, r.pointsBase(), base, Objects.requireNonNullElse(m.getEstimatedPriceTTCManual(), ZERO));
        }
        em.persist(audit.resume(m, id, "Prévisions enregistrées"));
        previsions.recalculer(id);
        return sousFenetres.previsions(id);
    }

    private Changements changements(PrjMarket m, PrevisionsSaisieRequest r) {
        LocalDate debutBase = jour(m.getStartDateManual());
        // Base vide : le début envoyé seul est le défaut du front (même périmé après un changement de début), pas une saisie.
        boolean debutChange = r.debutBase() != null && debutBase != null && !r.debutBase().equals(debutBase);
        boolean dureeChange = change(entier(r.dureeBase()), entier(m.getDelayManual()));
        boolean montantChange = change(r.estimationBaseHT(), m.getEstimatedPriceHTManual());
        boolean pointsChanges = r.pointsBase() != null && !memesPoints(trier(r.pointsBase()), points(TablePoints.BASE, m.getId()));
        return new Changements(debutChange || dureeChange, montantChange, pointsChanges);
    }

    /** Envoyé, différent du stocké, et pas le zéro que le front met à la place d'une valeur stockée nulle. */
    private static boolean change(BigDecimal envoye, BigDecimal stocke) {
        if (envoye == null) {
            return false;
        }
        return stocke == null ? envoye.signum() != 0 : envoye.compareTo(stocke) != 0;
    }

    private static BigDecimal entier(Integer valeur) {
        return valeur == null ? null : BigDecimal.valueOf(valeur);
    }

    private static boolean memesPoints(List<PointSaisieDto> envoyes, List<PointSaisiDto> stockes) {
        if (envoyes.size() != stockes.size()) {
            return false;
        }
        for (int i = 0; i < envoyes.size(); i++) {
            PointSaisieDto e = envoyes.get(i);
            PointSaisiDto s = stockes.get(i);
            if (!Objects.equals(e.date(), s.date()) || e.avancement() == null || s.avancement() == null
                    || e.avancement().compareTo(s.avancement()) != 0) {
                return false;
            }
        }
        return true;
    }

    /** Début et durée validés (R3) ; fin = début + durée. */
    private record Periode(LocalDate debut, int duree) {
        LocalDate fin() {
            return debut.plusDays(duree);
        }
    }

    private static Periode periode(LocalDate debut, Integer duree, String variante, String champDebut, String champDuree) {
        if (debut == null) {
            throw new InvalidRequestException(champDebut + " : obligatoire");
        }
        if (duree == null) {
            throw new InvalidRequestException(champDuree + " : obligatoire");
        }
        if (duree < 0) {
            throw new InvalidRequestException(FIN_AVANT_DEBUT.formatted(variante));
        }
        return new Periode(debut, duree);
    }

    private static void verifierPoints(List<PointSaisieDto> points, Periode periode, String variante) {
        if (points == null || points.isEmpty()) {
            return;
        }
        if (points.size() < 2) {
            throw new InvalidRequestException(DEUX_LIGNES);
        }
        if (points.stream().anyMatch(p -> p == null || p.date() == null || p.avancement() == null)) {
            throw new InvalidRequestException("points : date et avancement obligatoires");
        }
        BigDecimal precedent = ZERO;
        for (PointSaisieDto p : trier(points)) {
            precedent = verifierPoint(p, precedent, periode, variante);
        }
    }

    private static BigDecimal verifierPoint(PointSaisieDto p, BigDecimal precedent, Periode periode, String variante) {
        if (p.date().isAfter(periode.fin())) {
            throw new InvalidRequestException(APRES_FIN.formatted(variante));
        }
        if (p.date().isBefore(periode.debut())) {
            throw new InvalidRequestException(AVANT_DEBUT.formatted(variante));
        }
        if (p.avancement().signum() < 0 || p.avancement().compareTo(BigDecimal.ONE) > 0) {
            throw new InvalidRequestException("avancement : fraction entre 0 et 1");
        }
        if (p.avancement().compareTo(precedent) < 0) {
            throw new InvalidRequestException(DECROISSANT.formatted(JJ_MM_AAAA.format(p.date())));
        }
        return p.avancement();
    }

    private static List<PointSaisieDto> trier(List<PointSaisieDto> points) {
        return points.stream().sorted(Comparator.comparing(PointSaisieDto::date)).toList();
    }

    private void ecrire(String sql, UUID lot, Periode p) {
        em.createNativeQuery(sql).setParameter("debut", p.debut().atStartOfDay()).setParameter("duree", p.duree())
                .setParameter("fin", p.fin().atStartOfDay()).setParameter("lot", lot).executeUpdate();
    }

    /** R5 / R6 : points effacés puis réinsérés par date ; paiement = avancement − précédent, montant = paiement × prix. */
    private void remplacer(TablePoints table, UUID lot, List<PointSaisieDto> points, Periode periode, BigDecimal prix) {
        em.createNativeQuery(table.effacer).setParameter("lot", lot).executeUpdate();
        BigDecimal precedent = ZERO;
        for (PointSaisieDto p : trier(points)) {
            BigDecimal paiement = p.avancement().subtract(precedent);
            em.createNativeQuery(table.inserer).setParameter("id", UUID.randomUUID()).setParameter("lot", lot)
                    .setParameter("date", p.date().atStartOfDay()).setParameter("avancement", p.avancement())
                    .setParameter("temps", CourbePrevisions.temps(p.date(), periode.debut(), periode.duree()).setScale(16, RoundingMode.HALF_UP))
                    .setParameter("paiement", paiement).setParameter("montant", paiement.multiply(prix).setScale(6, RoundingMode.HALF_UP))
                    .executeUpdate();
            precedent = p.avancement();
        }
    }

    private List<PointSaisiDto> points(TablePoints table, UUID lot) {
        List<?> brutes = em.createNativeQuery(table.lire).setParameter("lot", lot).getResultList();
        return brutes.stream().map(Object[].class::cast).map(r -> new PointSaisiDto(uuid(r[0]), date(r[1]), montant(r[2]),
                montant(r[3]), montant(r[4]), montant(r[5]))).toList();
    }

    private CalculLot calcul(PrjMarket m) {
        return Objects.requireNonNull(montants.calculer(List.of(m)).get(m.getId()));
    }

    private static LocalDate fin(LocalDate debut, Integer duree) {
        if (debut == null) {
            return null;
        }
        return debut.plusDays(Objects.requireNonNullElse(duree, 0));
    }
}
